package com.campus.repository.impl;

import com.campus.config.AppConfig;
import com.campus.entity.TransactionHistory;
import com.campus.enums.TransactionStatus;
import com.campus.enums.TransactionType;
import com.campus.exception.DataAccessException;
import com.campus.repository.interfaces.ITransactionRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC repository for the {@code transaction} table. SQL only — no business rules.
 *
 * <p>WHAT: Reads/writes transaction rows; supports both standalone and transaction-enlisted writes.
 * WHY:  Keeps the audit backbone's persistence isolated from business logic.
 * HOW:  Generated-key retrieval returns the new txn_id so the caller can link a campus_payment.</p>
 */
public class TransactionRepositoryImpl implements ITransactionRepository {

    // WHY: single shared column list keeps the SELECTs consistent (DRY).
    private static final String COLUMNS =
            "txn_id, wallet_id, txn_type, amount, status, failure_reason, created_at";

    @Override
    public void save(TransactionHistory transaction) {
        // WHAT: Standalone save — opens its own connection (autocommit on).
        // WHY:  For callers that just need to persist one transaction outside a larger unit of work.
        // HOW:  Delegates to insert() to avoid duplicating the INSERT SQL (DRY), then records the id.
        try (Connection conn = AppConfig.getConnection()) {
            long id = insert(conn, transaction);
            transaction.setTxnId(id);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save transaction", e);
        }
    }

    @Override
    public Optional<TransactionHistory> findById(Long transactionId) {
        String sql = "SELECT " + COLUMNS + " FROM transaction WHERE txn_id = ?";
        try (Connection conn = AppConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, transactionId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find transaction " + transactionId, e);
        }
    }

    @Override
    public List<TransactionHistory> findByStudentId(String studentId) {
        // WHAT: Resolve a student's transactions by JOINing through the wallet.
        // WHY:  The transaction table stores wallet_id, not student_id, so the student link must
        //       be made via wallet.student_id — modelling the schema's supertype/subtype design.
        String sql = "SELECT t.txn_id, t.wallet_id, t.txn_type, t.amount, t.status, t.failure_reason, t.created_at "
                + "FROM transaction t JOIN wallet w ON t.wallet_id = w.wallet_id WHERE w.student_id = ?";
        List<TransactionHistory> list = new ArrayList<>();
        try (Connection conn = AppConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            // WHY: bind studentId directly (String) — no String.valueOf conversion is needed now
            //      that student identity is consistently String across the module.
            ps.setString(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find transactions for student " + studentId, e);
        }
    }

    @Override
    public List<TransactionHistory> findAll() {
        String sql = "SELECT " + COLUMNS + " FROM transaction";
        List<TransactionHistory> list = new ArrayList<>();
        try (Connection conn = AppConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to list transactions", e);
        }
    }

    @Override
    public long insert(Connection conn, TransactionHistory txn) {
        // WHAT: Insert the writable columns; txn_id and created_at are DB-generated.
        String sql = "INSERT INTO transaction (wallet_id, txn_type, amount, status, failure_reason) "
                + "VALUES (?, ?, ?, ?, ?)";
        // WHY no try-with-resources on conn: this enlists in the caller's transaction; only the
        //     statement is closed here. RETURN_GENERATED_KEYS yields the new txn_id.
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, txn.getWalletId());
            // WHY .name(): store the enum's stable name, matching the txn_type CHECK values.
            ps.setString(2, txn.getTxnType().name());
            ps.setBigDecimal(3, txn.getAmount());
            ps.setString(4, txn.getStatus().name());
            // WHY: failure_reason is null for SUCCESS rows and a message for FAILED rows.
            ps.setString(5, txn.getFailureReason());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    // WHAT: Return the generated id so the campus_payment can reference it.
                    return keys.getLong(1);
                }
                // WHY: A missing generated key means the insert did not behave as expected;
                //      fail loudly rather than return a bogus id that would corrupt the FK link.
                throw new DataAccessException("Transaction insert returned no generated key", null);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert transaction", e);
        }
    }

    @Override
    public void updateStatus(Connection conn, long txnId, TransactionStatus status, String failureReason) {
        // WHAT: Update a transaction's status/reason within the caller's transaction.
        // WHY:  Lets a flow flip a row to FAILED (with a reason) atomically with related changes.
        String sql = "UPDATE transaction SET status = ?, failure_reason = ? WHERE txn_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setString(2, failureReason);
            ps.setLong(3, txnId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update transaction " + txnId, e);
        }
    }

    /**
     * WHAT: Maps the current ResultSet row to a TransactionHistory.
     * WHY:  One place for row->entity conversion (DRY) used by all reads.
     * HOW:  Enum columns are parsed tolerantly (see parseType/parseStatus) and the timestamp is
     *       converted null-safely.
     */
    private TransactionHistory map(ResultSet rs) throws SQLException {
        Timestamp createdAt = rs.getTimestamp("created_at");
        return TransactionHistory.builder()
                .txnId(rs.getLong("txn_id"))
                .walletId(rs.getLong("wallet_id"))
                .txnType(parseType(rs.getString("txn_type")))
                .amount(rs.getBigDecimal("amount"))
                .status(parseStatus(rs.getString("status")))
                .failureReason(rs.getString("failure_reason"))
                .createdAt(createdAt == null ? null : createdAt.toLocalDateTime())
                .build();
    }

    /**
     * WHAT: Maps a DB txn_type string to the enum, returning null if no constant matches.
     * WHY:  The transaction table may legitimately contain types this module's enum does not
     *       model (e.g. legacy DEPOSIT/WITHDRAW); tolerating them avoids throwing while reading
     *       unrelated rows, chosen over a hard valueOf() that would crash on any unknown value.
     */
    private TransactionType parseType(String value) {
        for (TransactionType t : TransactionType.values()) {
            if (t.name().equals(value)) {
                return t;
            }
        }
        return null;
    }

    // WHY: same tolerant approach for status, for the same robustness reason as parseType.
    private TransactionStatus parseStatus(String value) {
        for (TransactionStatus s : TransactionStatus.values()) {
            if (s.name().equals(value)) {
                return s;
            }
        }
        return null;
    }
}