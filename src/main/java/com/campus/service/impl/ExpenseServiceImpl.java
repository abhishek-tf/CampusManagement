package com.campus.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.campus.constants.ErrorMessages;
import com.campus.entity.ExpenseGroup;
import com.campus.entity.ExpenseSplits;
import com.campus.entity.GroupExpense;
import com.campus.entity.GroupMember;
import com.campus.enums.SettlementStatus;
import com.campus.enums.SplitType;
import com.campus.exception.CampusPaymentException;
import com.campus.exception.InsufficientBalanceException;
import com.campus.exception.InvalidAmountException;
import com.campus.exception.StudentNotFoundException;
import com.campus.repository.interfaces.IExpenseRepository;
import com.campus.repository.interfaces.IExpenseRepository.WalletRow;
import com.campus.repository.interfaces.ISplitRepository;
import com.campus.service.interfaces.IExpenseService;
import com.campus.util.DBConnection;

/**
 * Business logic for Splitwise-style expense sharing.
 *
 * <p>This service owns every transaction boundary: each public operation runs
 * inside a single JDBC transaction that either commits as a whole or rolls back
 * entirely. SQL is delegated to the repositories.</p>
 */
public class ExpenseServiceImpl implements IExpenseService {

    private static final Logger log = LoggerFactory.getLogger(ExpenseServiceImpl.class);

    private static final BigDecimal ONE_CENT = new BigDecimal("0.01");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final IExpenseRepository expenseRepository;
    private final ISplitRepository splitRepository;

    public ExpenseServiceImpl(IExpenseRepository expenseRepository,
                              ISplitRepository splitRepository) {
        this.expenseRepository = expenseRepository;
        this.splitRepository = splitRepository;
    }

    @Override
    public long createGroup(String groupName, String createdBy) throws CampusPaymentException {
        requireText(groupName, "Group name is required");
        requireStudentId(createdBy);

        return executeInTransaction(conn -> {
            ExpenseGroup group = ExpenseGroup.builder()
                    .groupName(groupName.trim())
                    .createdBy(createdBy)
                    .build();
            long groupId = expenseRepository.saveGroup(conn, group);

            // The creator is implicitly the first member of the group.
            expenseRepository.addMember(conn, GroupMember.builder()
                    .groupId(groupId)
                    .studentId(createdBy)
                    .build());

            log.info("Created expense group {} ('{}') by student {}", groupId, groupName, createdBy);
            return groupId;
        });
    }

    @Override
    public void addMember(long groupId, String studentId) throws CampusPaymentException {
        requireStudentId(studentId);

        executeInTransaction(conn -> {
            requireGroup(conn, groupId);
            if (expenseRepository.isMember(conn, groupId, studentId)) {
                throw new CampusPaymentException("Student is already a member of this group");
            }
            expenseRepository.addMember(conn, GroupMember.builder()
                    .groupId(groupId)
                    .studentId(studentId)
                    .build());
            log.info("Added student {} to group {}", studentId, groupId);
            return null;
        });
    }

    @Override
    public long addExpense(long groupId, String paidBy, String description,
                           BigDecimal totalAmount, SplitType splitType,
                           Map<String, BigDecimal> shares) throws CampusPaymentException {
        requireStudentId(paidBy);
        requirePositive(totalAmount);
        if (splitType == null) {
            throw new CampusPaymentException(ErrorMessages.INVALID_INPUT);
        }
        BigDecimal total = totalAmount.setScale(2, RoundingMode.HALF_UP);

        return executeInTransaction(conn -> {
            requireGroup(conn, groupId);
            List<GroupMember> members = expenseRepository.findMembersByGroupId(conn, groupId);
            Set<String> memberIds = members.stream()
                    .map(GroupMember::getStudentId)
                    .collect(Collectors.toSet());
            if (!memberIds.contains(paidBy)) {
                throw new CampusPaymentException(ErrorMessages.NOT_GROUP_MEMBER);
            }

            List<SplitLine> lines = computeSplitLines(total, splitType, members, memberIds, shares);

            long expenseId = expenseRepository.saveExpense(conn, GroupExpense.builder()
                    .groupId(groupId)
                    .paidBy(paidBy)
                    .description(description)
                    .totalAmount(total)
                    .splitType(splitType)
                    .build());

            persistSplits(conn, expenseId, paidBy, lines);

            log.info("Recorded expense {} of {} in group {} paid by {} ({} split, {} participants)",
                    expenseId, total, groupId, paidBy, splitType, lines.size());
            return expenseId;
        });
    }

    @Override
    public List<ExpenseSplits> getPendingDues(String studentId) throws CampusPaymentException {
        requireStudentId(studentId);
        return executeInTransaction(conn -> splitRepository.findPendingByDebtor(conn, studentId));
    }

    @Override
    public void settleSplit(long splitId) throws CampusPaymentException {
        executeInTransaction(conn -> {
            ExpenseSplits split = splitRepository.findById(conn, splitId)
                    .orElseThrow(() -> new CampusPaymentException(ErrorMessages.SPLIT_NOT_FOUND));
            if (split.getStatus() == SettlementStatus.SETTLED) {
                throw new CampusPaymentException(ErrorMessages.ALREADY_SETTLED);
            }

            GroupExpense expense = expenseRepository.findExpenseById(conn, split.getExpenseId())
                    .orElseThrow(() -> new CampusPaymentException(ErrorMessages.EXPENSE_NOT_FOUND));

            String debtor = split.getDebtorId();
            String creditor = expense.getPaidBy();
            if (debtor.equals(creditor)) {
                throw new CampusPaymentException("Cannot settle a split owed to oneself");
            }
            BigDecimal amount = split.getShareAmount();

            // Lock both wallets, verify funds, then move money + record it atomically.
            WalletRow debtorWallet = expenseRepository.findWalletForUpdate(conn, debtor)
                    .orElseThrow(() -> new StudentNotFoundException(
                            "Wallet not found for debtor " + debtor));
            WalletRow creditorWallet = expenseRepository.findWalletForUpdate(conn, creditor)
                    .orElseThrow(() -> new StudentNotFoundException(
                            "Wallet not found for payer " + creditor));

            if (debtorWallet.balance().compareTo(amount) < 0) {
                throw new InsufficientBalanceException(
                        ErrorMessages.INSUFFICIENT_BALANCE + " to settle split " + splitId);
            }

            long txnId = expenseRepository.insertTransferTransaction(
                    conn, debtorWallet.walletId(), debtor, creditor, amount);
            expenseRepository.adjustWalletBalance(conn, debtorWallet.walletId(), amount.negate());
            expenseRepository.adjustWalletBalance(conn, creditorWallet.walletId(), amount);
            splitRepository.markSettled(conn, splitId, txnId, LocalDateTime.now());

            log.info("Settled split {}: {} paid {} to {} (txn {})",
                    splitId, debtor, amount, creditor, txnId);
            return null;
        });
    }

    @Override
    public Map<String, BigDecimal> getGroupNetBalances(long groupId) throws CampusPaymentException {
        return executeInTransaction(conn -> {
            requireGroup(conn, groupId);
            List<GroupMember> members = expenseRepository.findMembersByGroupId(conn, groupId);
            List<GroupExpense> expenses = expenseRepository.findExpensesByGroupId(conn, groupId);
            List<ExpenseSplits> splits = splitRepository.findByGroupId(conn, groupId);

            // Total fronted by each payer and total share consumed by each debtor.
            Map<String, BigDecimal> paid = expenses.stream().collect(Collectors.groupingBy(
                    GroupExpense::getPaidBy,
                    Collectors.reducing(BigDecimal.ZERO, GroupExpense::getTotalAmount, BigDecimal::add)));
            Map<String, BigDecimal> owed = splits.stream().collect(Collectors.groupingBy(
                    ExpenseSplits::getDebtorId,
                    Collectors.reducing(BigDecimal.ZERO, ExpenseSplits::getShareAmount, BigDecimal::add)));

            Map<String, BigDecimal> netBalances = new LinkedHashMap<>();
            for (GroupMember member : members) {
                String id = member.getStudentId();
                BigDecimal net = paid.getOrDefault(id, BigDecimal.ZERO)
                        .subtract(owed.getOrDefault(id, BigDecimal.ZERO));
                netBalances.put(id, net.setScale(2, RoundingMode.HALF_UP));
            }
            return netBalances;
        });
    }

    // --- split computation ------------------------------------------------

    /** One participant's computed share before it is persisted. */
    private record SplitLine(String debtorId, BigDecimal amount, BigDecimal percent) {
    }

    private List<SplitLine> computeSplitLines(BigDecimal total, SplitType splitType,
                                              List<GroupMember> members, Set<String> memberIds,
                                              Map<String, BigDecimal> shares)
            throws CampusPaymentException {
        return switch (splitType) {
            case EQUAL -> splitEqually(total, members);
            case EXACT -> splitByExactAmounts(total, memberIds, shares);
            case PERCENT -> splitByPercentages(total, memberIds, shares);
        };
    }

    private List<SplitLine> splitEqually(BigDecimal total, List<GroupMember> members) {
        int count = members.size();
        BigDecimal base = total.divide(BigDecimal.valueOf(count), 2, RoundingMode.DOWN);
        // Spread the rounding remainder one cent at a time over the first members.
        int extraCents = total.subtract(base.multiply(BigDecimal.valueOf(count)))
                .movePointRight(2).intValue();

        List<SplitLine> lines = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            BigDecimal amount = i < extraCents ? base.add(ONE_CENT) : base;
            lines.add(new SplitLine(members.get(i).getStudentId(), amount, null));
        }
        return lines;
    }

    private List<SplitLine> splitByExactAmounts(BigDecimal total, Set<String> memberIds,
                                                Map<String, BigDecimal> shares)
            throws CampusPaymentException {
        requireShares(shares);
        List<SplitLine> lines = new ArrayList<>();
        BigDecimal sum = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> entry : shares.entrySet()) {
            requireParticipant(memberIds, entry.getKey());
            BigDecimal amount = entry.getValue().setScale(2, RoundingMode.HALF_UP);
            sum = sum.add(amount);
            lines.add(new SplitLine(entry.getKey(), amount, null));
        }
        if (sum.compareTo(total) != 0) {
            throw new InvalidAmountException(ErrorMessages.INVALID_SPLIT);
        }
        return lines;
    }

    private List<SplitLine> splitByPercentages(BigDecimal total, Set<String> memberIds,
                                               Map<String, BigDecimal> shares)
            throws CampusPaymentException {
        requireShares(shares);
        List<Map.Entry<String, BigDecimal>> entries = new ArrayList<>(shares.entrySet());

        BigDecimal percentSum = entries.stream()
                .map(Map.Entry::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (percentSum.compareTo(HUNDRED) != 0) {
            throw new InvalidAmountException("Split percentages must add up to 100");
        }

        List<SplitLine> lines = new ArrayList<>();
        BigDecimal allocated = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> entry : entries) {
            requireParticipant(memberIds, entry.getKey());
            BigDecimal percent = entry.getValue();
            BigDecimal amount = total.multiply(percent)
                    .divide(HUNDRED, 2, RoundingMode.DOWN);
            allocated = allocated.add(amount);
            lines.add(new SplitLine(entry.getKey(), amount, percent));
        }

        // Push any rounding remainder onto the last participant so shares sum to total.
        BigDecimal remainder = total.subtract(allocated);
        if (remainder.signum() != 0 && !lines.isEmpty()) {
            SplitLine last = lines.get(lines.size() - 1);
            lines.set(lines.size() - 1,
                    new SplitLine(last.debtorId(), last.amount().add(remainder), last.percent()));
        }
        return lines;
    }

    private void persistSplits(Connection conn, long expenseId, String paidBy,
                               List<SplitLine> lines) throws SQLException {
        LocalDateTime now = LocalDateTime.now();
        for (SplitLine line : lines) {
            boolean isPayer = line.debtorId().equals(paidBy);
            splitRepository.saveSplit(conn, ExpenseSplits.builder()
                    .expenseId(expenseId)
                    .debtorId(line.debtorId())
                    .shareAmount(line.amount())
                    .sharePercent(line.percent())
                    // The payer already covered the bill, so their own share is
                    // recorded as settled; everyone else owes a PENDING due.
                    .status(isPayer ? SettlementStatus.SETTLED : SettlementStatus.PENDING)
                    .settledAt(isPayer ? now : null)
                    .settledTxnId(null)
                    .build());
        }
    }

    // --- transaction template & validation --------------------------------

    @FunctionalInterface
    private interface ConnectionWork<T> {
        T apply(Connection conn) throws SQLException, CampusPaymentException;
    }

    /**
     * Runs {@code work} inside a single JDBC transaction. Commits on success,
     * rolls back on any failure, and translates SQL errors into a
     * {@link CampusPaymentException} after logging them.
     */
    private <T> T executeInTransaction(ConnectionWork<T> work) throws CampusPaymentException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                T result = work.apply(conn);
                conn.commit();
                return result;
            } catch (SQLException | CampusPaymentException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        } catch (CampusPaymentException e) {
            log.error("Expense operation failed: {}", e.getMessage());
            throw e;
        } catch (SQLException e) {
            log.error("Database error during expense operation", e);
            throw new CampusPaymentException(ErrorMessages.DATABASE_ERROR, e);
        }
    }

    private void requireGroup(Connection conn, long groupId) throws SQLException, CampusPaymentException {
        if (expenseRepository.findGroupById(conn, groupId).isEmpty()) {
            throw new CampusPaymentException(ErrorMessages.GROUP_NOT_FOUND);
        }
    }

    private void requireParticipant(Set<String> memberIds, String studentId)
            throws CampusPaymentException {
        requireStudentId(studentId);
        if (!memberIds.contains(studentId)) {
            throw new CampusPaymentException(ErrorMessages.NOT_GROUP_MEMBER + ": " + studentId);
        }
    }

    private void requireShares(Map<String, BigDecimal> shares) throws CampusPaymentException {
        if (shares == null || shares.isEmpty()) {
            throw new CampusPaymentException("Share details are required for this split type");
        }
    }

    private void requireStudentId(String studentId) throws CampusPaymentException {
        if (studentId == null || studentId.isBlank()) {
            throw new CampusPaymentException(ErrorMessages.INVALID_INPUT);
        }
    }

    private void requireText(String value, String message) throws CampusPaymentException {
        if (value == null || value.isBlank()) {
            throw new CampusPaymentException(message);
        }
    }

    private void requirePositive(BigDecimal amount) throws CampusPaymentException {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException(ErrorMessages.INVALID_AMOUNT);
        }
    }
}
