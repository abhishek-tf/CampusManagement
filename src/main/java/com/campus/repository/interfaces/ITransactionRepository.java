package com.campus.repository.interfaces;

import com.campus.entity.TransactionHistory;
import com.campus.enums.TransactionStatus;
import java.sql.Connection;
import java.util.Optional;
import java.util.List;

/**
 * Persistence contract for the {@code transaction} audit table.
 *
 * <p>WHAT: SQL operations over transaction rows.
 * WHY:  Interface-based for DIP/testability; SQL-only (no business logic) for SRP.
 * HOW:  Provides both standalone reads and transaction-enlisted writes (Connection params) so
 *       a PAYMENT row can be created inside the service's atomic unit of work.</p>
 */
public interface ITransactionRepository {

    void save(TransactionHistory transaction);

    Optional<TransactionHistory> findById(Long transactionId);

    /**
     * Transactions for a student, resolved via the owning wallet (transaction has no
     * student_id column). studentId is String to match student.student_id VARCHAR(20).
     */
    List<TransactionHistory> findByStudentId(String studentId);

    List<TransactionHistory> findAll();

    /**
     * Inserts a transaction row within the caller's transaction and returns the generated txn_id.
     * WHY return the id: the campus_payment row must reference this txn_id (the 1:1 link), so the
     * service needs it immediately, within the same transaction.
     */
    long insert(Connection conn, TransactionHistory transaction);

    /** Updates status / failure reason within the caller's transaction. */
    void updateStatus(Connection conn, long txnId, TransactionStatus status, String failureReason);
}