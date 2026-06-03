package com.campus.service.interfaces;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.campus.entity.ExpenseSplits;
import com.campus.enums.SplitType;
import com.campus.exception.CampusPaymentException;

/**
 * Splitwise-style expense sharing: groups, bills split fairly among members,
 * pending dues tracking, atomic settlement and stream-based reports.
 */
public interface IExpenseService {

    /**
     * Creates a group and registers the creator as its first member.
     *
     * @return the generated group id
     */
    long createGroup(String groupName, String createdBy) throws CampusPaymentException;

    /** Adds a student to an existing group. */
    void addMember(long groupId, String studentId) throws CampusPaymentException;

    /**
     * Records a bill paid by one member and splits it among participants.
     *
     * <p>For {@link SplitType#EQUAL} the bill is divided across all current group
     * members and {@code shares} is ignored. For {@link SplitType#EXACT} and
     * {@link SplitType#PERCENT}, {@code shares} maps each participant's student id
     * to their amount / percentage. The payer's own share is recorded as already
     * settled; every other participant gets a PENDING due owed to the payer.</p>
     *
     * @return the generated expense id
     */
    long addExpense(long groupId, String paidBy, String description,
                    BigDecimal totalAmount, SplitType splitType,
                    Map<String, BigDecimal> shares) throws CampusPaymentException;

    /** All outstanding (PENDING) dues owed by a student. */
    List<ExpenseSplits> getPendingDues(String studentId) throws CampusPaymentException;

    /**
     * Settles one split atomically: moves the share from the debtor's wallet to
     * the payer's wallet, records the transaction and marks the split SETTLED.
     * The whole operation commits together or rolls back entirely.
     */
    void settleSplit(long splitId) throws CampusPaymentException;

    /**
     * Net balance per group member: total paid minus total share consumed.
     * Positive means the member is owed money; negative means they owe.
     */
    Map<String, BigDecimal> getGroupNetBalances(long groupId) throws CampusPaymentException;
}
