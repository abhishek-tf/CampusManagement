package com.campus.repository.interfaces;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

import com.campus.entity.Wallet;

/**
 * Persistence operations for the {@code wallet} table, plus the {@code transaction}
 * / {@code transfer_transaction} writes that every wallet money-movement must
 * record (schema.sql: "every money movement is one row in transaction").
 *
 * <p>Every method takes the active {@link Connection} so the service layer owns
 * the transaction boundary (commit/rollback). Implementations perform SQL only -
 * no validation, no balance maths, no business rules.</p>
 */
public interface IWalletRepository {

    /** Inserts a wallet row and returns its generated {@code wallet_id}. */
    long save(Connection conn, Wallet wallet) throws SQLException;

    Optional<Wallet> findByStudentId(Connection conn, String studentId) throws SQLException;

    /** Same as {@link #findByStudentId} but locks the row ({@code FOR UPDATE}). */
    Optional<Wallet> findByStudentIdForUpdate(Connection conn, String studentId) throws SQLException;

    /** Persists the mutable wallet state (balance, daily counter, updated_at). */
    void update(Connection conn, Wallet wallet) throws SQLException;

    /**
     * Records a money movement in the {@code transaction} table and returns the
     * generated {@code txn_id}.
     *
     * @param txnType one of DEPOSIT | WITHDRAW | TRANSFER | PAYMENT (schema CHECK)
     */
    long insertTransaction(Connection conn, long walletId, String txnType,
                           BigDecimal amount) throws SQLException;

    /** Records the {@code transfer_transaction} detail for a TRANSFER txn. */
    void insertTransferDetail(Connection conn, long txnId, String fromStudentId,
                              String toStudentId) throws SQLException;
}
