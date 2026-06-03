package com.campus.repository.impl;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.campus.entity.Wallet;
import com.campus.repository.interfaces.IWalletRepository;

/**
 * JDBC implementation of {@link IWalletRepository} against the MySQL {@code wallet},
 * {@code transaction} and {@code transfer_transaction} tables.
 *
 * <p>SQL only - all validation and transaction control live in the service layer.
 * Every method uses the {@link Connection} passed in so multiple writes can share
 * one transaction (atomic transfers).</p>
 */
public class WalletRepositoryImpl implements IWalletRepository {

    private static final String COLUMNS =
            "wallet_id, student_id, balance, daily_transfer_used, transfer_reset_date, "
            + "max_balance_cap, daily_transfer_limit, updated_at";

    @Override
    public long save(Connection conn, Wallet wallet) throws SQLException {
        String sql = "INSERT INTO wallet "
                + "(student_id, balance, daily_transfer_used, transfer_reset_date, "
                + "max_balance_cap, daily_transfer_limit) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, wallet.getStudentId());
            ps.setBigDecimal(2, wallet.getBalance());
            ps.setBigDecimal(3, wallet.getDailyTransferUsed());
            ps.setDate(4, toSqlDate(wallet.getTransferResetDate()));
            ps.setBigDecimal(5, wallet.getMaxBalanceCap());
            ps.setBigDecimal(6, wallet.getDailyTransferLimit());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    wallet.setWalletId(id);
                    return id;
                }
                throw new SQLException("No generated wallet_id returned");
            }
        }
    }

    @Override
    public Optional<Wallet> findByStudentId(Connection conn, String studentId) throws SQLException {
        return findByStudentId(conn, studentId, false);
    }

    @Override
    public Optional<Wallet> findByStudentIdForUpdate(Connection conn, String studentId) throws SQLException {
        return findByStudentId(conn, studentId, true);
    }

    private Optional<Wallet> findByStudentId(Connection conn, String studentId, boolean forUpdate)
            throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM wallet WHERE student_id = ?"
                + (forUpdate ? " FOR UPDATE" : "");
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public List<Wallet> findAll(Connection conn) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM wallet ORDER BY wallet_id";
        List<Wallet> wallets = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                wallets.add(mapRow(rs));
            }
        }
        return wallets;
    }

    @Override
    public void update(Connection conn, Wallet wallet) throws SQLException {
        // student_id and the caps are immutable; only the spendable state changes.
        // updated_at is maintained by the DB (ON UPDATE CURRENT_TIMESTAMP).
        String sql = "UPDATE wallet SET balance = ?, daily_transfer_used = ?, "
                + "transfer_reset_date = ? WHERE wallet_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, wallet.getBalance());
            ps.setBigDecimal(2, wallet.getDailyTransferUsed());
            ps.setDate(3, toSqlDate(wallet.getTransferResetDate()));
            ps.setLong(4, wallet.getWalletId());
            ps.executeUpdate();
        }
    }

    @Override
    public long insertTransaction(Connection conn, long walletId, String txnType,
                                  BigDecimal amount) throws SQLException {
        String sql = "INSERT INTO transaction (wallet_id, txn_type, amount, status) "
                + "VALUES (?, ?, ?, 'SUCCESS')";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, walletId);
            ps.setString(2, txnType);
            ps.setBigDecimal(3, amount);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
                throw new SQLException("No generated txn_id returned");
            }
        }
    }

    @Override
    public void insertTransferDetail(Connection conn, long txnId, String fromStudentId,
                                     String toStudentId) throws SQLException {
        String sql = "INSERT INTO transfer_transaction (txn_id, from_student_id, to_student_id) "
                + "VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, txnId);
            ps.setString(2, fromStudentId);
            ps.setString(3, toStudentId);
            ps.executeUpdate();
        }
    }

    // --- row mapper / helpers ---------------------------------------------

    private Wallet mapRow(ResultSet rs) throws SQLException {
        Date resetDate = rs.getDate("transfer_reset_date");
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        return Wallet.builder()
                .walletId(rs.getLong("wallet_id"))
                .studentId(rs.getString("student_id"))
                .balance(rs.getBigDecimal("balance"))
                .dailyTransferUsed(rs.getBigDecimal("daily_transfer_used"))
                .transferResetDate(resetDate == null ? null : resetDate.toLocalDate())
                .maxBalanceCap(rs.getBigDecimal("max_balance_cap"))
                .dailyTransferLimit(rs.getBigDecimal("daily_transfer_limit"))
                .updatedAt(updatedAt == null ? null : updatedAt.toLocalDateTime())
                .build();
    }

    private Date toSqlDate(LocalDate date) {
        return date == null ? null : Date.valueOf(date);
    }
}
