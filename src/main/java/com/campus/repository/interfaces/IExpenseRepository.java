package com.campus.repository.interfaces;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import com.campus.entity.ExpenseGroup;
import com.campus.entity.GroupExpense;
import com.campus.entity.GroupMember;

/**
 * Persistence operations for expense groups, members and bills, plus the
 * minimal wallet/transaction writes needed to settle a split atomically.
 *
 * <p>Every method takes the active {@link Connection} so the service layer
 * controls the transaction boundary (commit/rollback). Implementations perform
 * SQL only - no validation or business rules.</p>
 */
public interface IExpenseRepository {

    // --- expense_group ----------------------------------------------------

    /** Inserts a group and returns its generated {@code group_id}. */
    long saveGroup(Connection conn, ExpenseGroup group) throws SQLException;

    Optional<ExpenseGroup> findGroupById(Connection conn, long groupId) throws SQLException;

    // --- group_member -----------------------------------------------------

    void addMember(Connection conn, GroupMember member) throws SQLException;

    boolean isMember(Connection conn, long groupId, String studentId) throws SQLException;

    List<GroupMember> findMembersByGroupId(Connection conn, long groupId) throws SQLException;

    // --- group_expense ----------------------------------------------------

    /** Inserts a bill and returns its generated {@code expense_id}. */
    long saveExpense(Connection conn, GroupExpense expense) throws SQLException;

    Optional<GroupExpense> findExpenseById(Connection conn, long expenseId) throws SQLException;

    List<GroupExpense> findExpensesByGroupId(Connection conn, long groupId) throws SQLException;

    // --- wallet / transaction (used only to settle a split atomically) ----

    /** Locks and returns a student's wallet row for update, if it exists. */
    Optional<WalletRow> findWalletForUpdate(Connection conn, String studentId) throws SQLException;

    /**
     * Records a wallet TRANSFER: inserts the {@code transaction} row and its
     * {@code transfer_transaction} detail, returning the generated {@code txn_id}.
     */
    long insertTransferTransaction(Connection conn, long fromWalletId,
                                   String fromStudentId, String toStudentId,
                                   BigDecimal amount) throws SQLException;

    /** Applies a signed delta to a wallet's balance. */
    void adjustWalletBalance(Connection conn, long walletId, BigDecimal delta) throws SQLException;

    /** Lightweight view of a wallet row needed during settlement. */
    record WalletRow(long walletId, BigDecimal balance) {
    }
}
