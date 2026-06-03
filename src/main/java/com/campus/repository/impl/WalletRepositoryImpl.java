package com.campus.repository.impl;

import com.campus.config.AppConfig;
import com.campus.entity.Wallet;
import com.campus.exception.DataAccessException;
import com.campus.repository.interfaces.IWalletRepository;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC repository for the {@code wallet} table. SQL only — no business rules.
 *
 * <p>WHAT: Reads/writes wallet rows; provides a locking read and a balance update for payments.
 * WHY:  Balance validation and limit logic deliberately live in the service, not here, so this
 *       class has a single reason to change (the persistence/SQL).
 * HOW:  The transactional methods reuse the caller's Connection so the lock and debit are part of
 *       the same atomic payment.</p>
 */
public class WalletRepositoryImpl implements IWalletRepository {

    // WHY: shared column list for all reads (DRY); ordered to mirror the schema for readability.
    private static final String COLUMNS =
            "wallet_id, student_id, balance, daily_transfer_used, transfer_reset_date, "
            + "max_balance_cap, daily_transfer_limit, updated_at";

    @Override
    public void save(Wallet wallet) {
        // WHAT: Insert a new wallet's core columns.
        // WHY:  max_balance_cap, transfer_reset_date and updated_at are omitted so their schema
        //       defaults apply — the database stays the source of truth for those values.
        String sql = "INSERT INTO wallet (student_id, balance, daily_transfer_used, daily_transfer_limit) "
                + "VALUES (?, ?, ?, ?)";
        try (Connection conn = AppConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, wallet.getStudentId());
            // WHY nvl(...): the columns are NOT NULL; coalescing a null amount to ZERO avoids a
            //     constraint violation if the caller left a money field unset.
            ps.setBigDecimal(2, nvl(wallet.getBalance()));
            ps.setBigDecimal(3, nvl(wallet.getDailyTransferSpent()));
            ps.setBigDecimal(4, nvl(wallet.getDailyTransferLimit()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    wallet.setWalletId(keys.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save wallet for student " + wallet.getStudentId(), e);
        }
    }

    @Override
    public Optional<Wallet> findById(Long walletId) {
        String sql = "SELECT " + COLUMNS + " FROM wallet WHERE wallet_id = ?";
        try (Connection conn = AppConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, walletId);
            return single(ps);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find wallet " + walletId, e);
        }
    }

    @Override
    public Optional<Wallet> findByStudentId(String studentId) {
        // WHAT: Non-locking read of a student's wallet (used outside a transaction, e.g. balance display).
        String sql = "SELECT " + COLUMNS + " FROM wallet WHERE student_id = ?";
        try (Connection conn = AppConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);
            return single(ps);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find wallet for student " + studentId, e);
        }
    }

    @Override
    public List<Wallet> findAll() {
        String sql = "SELECT " + COLUMNS + " FROM wallet";
        List<Wallet> wallets = new ArrayList<>();
        try (Connection conn = AppConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                wallets.add(map(rs));
            }
            return wallets;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to list wallets", e);
        }
    }

    @Override
    public void update(Wallet wallet) {
        String sql = "UPDATE wallet SET balance = ?, daily_transfer_used = ?, daily_transfer_limit = ?, "
                + "transfer_reset_date = ? WHERE wallet_id = ?";
        try (Connection conn = AppConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, nvl(wallet.getBalance()));
            ps.setBigDecimal(2, nvl(wallet.getDailyTransferSpent()));
            ps.setBigDecimal(3, nvl(wallet.getDailyTransferLimit()));
            // WHY toDate(...): convert LocalDate -> java.sql.Date for the DATE column, null-safely.
            ps.setDate(4, toDate(wallet.getTransferResetDate()));
            ps.setLong(5, wallet.getWalletId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update wallet " + wallet.getWalletId(), e);
        }
    }

    // --- transactional variants ---

    @Override
    public Optional<Wallet> findByStudentId(Connection conn, String studentId) {
        // WHAT: Read the wallet AND lock the row for the duration of the caller's transaction.
        // WHY FOR UPDATE: serialises concurrent debits on the same wallet so two simultaneous
        //     payments cannot both read the old balance and overspend (prevents lost updates).
        // HOW:  Reuses the caller's connection (no try-with-resources on it) so the lock is held
        //       until that connection commits/rolls back.
        String sql = "SELECT " + COLUMNS + " FROM wallet WHERE student_id = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);
            return single(ps);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to lock wallet for student " + studentId, e);
        }
    }

    @Override
    public void updateBalance(Connection conn, Long walletId, BigDecimal newBalance) {
        // WHAT: The debit step — write the new balance within the caller's transaction.
        // WHY:  The service computes newBalance (business rule); the repository only persists it,
        //       keeping arithmetic out of the data layer.
        String sql = "UPDATE wallet SET balance = ? WHERE wallet_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, newBalance);
            ps.setLong(2, walletId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update balance for wallet " + walletId, e);
        }
    }

    // --- mapping helpers ---

    /**
     * WHAT: Executes the query and returns the first row (if any) as a Wallet.
     * WHY:  Shared by the by-id and by-student reads to avoid duplicating ResultSet handling (DRY).
     */
    private Optional<Wallet> single(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            return rs.next() ? Optional.of(map(rs)) : Optional.empty();
        }
    }

    /**
     * WHAT: Maps the current row to a Wallet.
     * WHY:  One conversion point for all wallet reads.
     * HOW:  DATE -> LocalDate and DATETIME -> LocalDateTime conversions are null-safe.
     */
    private Wallet map(ResultSet rs) throws SQLException {
        Date resetDate = rs.getDate("transfer_reset_date");
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        return Wallet.builder()
                .walletId(rs.getLong("wallet_id"))
                .studentId(rs.getString("student_id"))
                .balance(rs.getBigDecimal("balance"))
                .dailyTransferSpent(rs.getBigDecimal("daily_transfer_used"))
                .transferResetDate(resetDate == null ? null : resetDate.toLocalDate())
                .maxBalanceCap(rs.getBigDecimal("max_balance_cap"))
                .dailyTransferLimit(rs.getBigDecimal("daily_transfer_limit"))
                .updatedAt(updatedAt == null ? null : updatedAt.toLocalDateTime())
                .build();
    }

    // WHAT: Null-coalesce a money value to ZERO.
    // WHY:  Wallet money columns are NOT NULL; this guards inserts/updates against nulls.
    private static BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    // WHAT: Convert LocalDate to java.sql.Date, preserving null.
    // WHY:  PreparedStatement needs java.sql.Date for a DATE column; null maps to SQL NULL.
    private static Date toDate(LocalDate value) {
        return value == null ? null : Date.valueOf(value);
    }
}