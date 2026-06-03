package com.campus.repository.interfaces;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.campus.entity.ExpenseSplits;

/**
 * Persistence operations for the {@code expense_split} table.
 *
 * <p>Every method takes the active {@link Connection} so the service layer owns
 * the transaction boundary. Implementations perform SQL only.</p>
 */
public interface ISplitRepository {

    void saveSplit(Connection conn, ExpenseSplits split) throws SQLException;

    Optional<ExpenseSplits> findById(Connection conn, long splitId) throws SQLException;

    List<ExpenseSplits> findByExpenseId(Connection conn, long expenseId) throws SQLException;

    /** All PENDING splits owed by a student, across every group. */
    List<ExpenseSplits> findPendingByDebtor(Connection conn, String debtorId) throws SQLException;

    /** All splits belonging to a group (joined through {@code group_expense}). */
    List<ExpenseSplits> findByGroupId(Connection conn, long groupId) throws SQLException;

    /** Marks a split SETTLED, linking the wallet transaction that paid it. */
    void markSettled(Connection conn, long splitId, long settledTxnId,
                     LocalDateTime settledAt) throws SQLException;
}
