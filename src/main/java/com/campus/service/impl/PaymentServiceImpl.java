package com.campus.service.impl;

import com.campus.config.AppConfig;
import com.campus.constants.ErrorMessages;
import com.campus.entity.CampusPayment;
import com.campus.entity.TransactionHistory;
import com.campus.entity.Wallet;
import com.campus.enums.PaymentCategory;
import com.campus.enums.TransactionStatus;
import com.campus.enums.TransactionType;
import com.campus.exception.*;
import com.campus.repository.interfaces.IPaymentRepository;
import com.campus.repository.interfaces.IStudentRepository;
import com.campus.repository.interfaces.ITransactionRepository;
import com.campus.repository.interfaces.IWalletRepository;
import com.campus.service.interfaces.IPaymentService;
import com.campus.service.interfaces.PaymentProcessor;
import com.campus.util.Logger;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Campus payment business logic — the heart of the module.
 *
 * <p>WHAT: Orchestrates the payment flow: validate student → validate wallet → validate amount
 *       → validate balance → begin transaction → insert transaction → insert campus_payment →
 *       update wallet → commit, all atomically with rollback on failure.
 *
 * WHY this class holds the business rules: per layered architecture, the service is the single
 *       place for validation, transaction boundaries and orchestration; repositories stay
 *       SQL-only and entities stay data-only. This keeps each layer with one responsibility.
 *
 * WHY transaction management lives here (not in repositories): only the service knows that the
 *       transaction insert, campus_payment insert and wallet debit form ONE business operation,
 *       so it owns the connection, commit and rollback that make them atomic.
 *
 * HOW dependencies arrive: constructor injection of interfaces (Dependency Inversion) makes the
 *       collaborators explicit, final, and easily substitutable for testing.</p>
 */
public class PaymentServiceImpl implements IPaymentService {

    private final IPaymentRepository paymentRepository;
    private final ITransactionRepository transactionRepository;
    private final IWalletRepository walletRepository;
    private final IStudentRepository studentRepository;

    // WHAT: Category -> payment-processing strategy (a PaymentProcessor lambda).
    // WHY EnumMap: keys are PaymentCategory enums; EnumMap is the fastest, most compact Map for
    //      enum keys (array-backed by ordinal). It also makes the dispatch table explicit.
    // WHY at all: lets processPayment() select behaviour by a map lookup instead of a switch-case,
    //      which is open for extension (register a new category's lambda) and closed for
    //      modification (no existing method is edited to add a category).
    private final Map<PaymentCategory, PaymentProcessor> processors = new EnumMap<>(PaymentCategory.class);

    public PaymentServiceImpl(IPaymentRepository paymentRepository,
                              ITransactionRepository transactionRepository,
                              IWalletRepository walletRepository,
                              IStudentRepository studentRepository) {
        this.paymentRepository = paymentRepository;
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
        this.studentRepository = studentRepository;
        registerProcessors();
    }

    /**
     * WHAT: Registers one PaymentProcessor lambda per category in the EnumMap.
     * WHY:  Every category is processed by the same standard flow (there are no per-category
     *       amount rules in the schema/requirements), so a loop registers identical behaviour —
     *       honest and DRY, rather than fabricating differences.
     * HOW:  Each lambda is a PaymentProcessor (functional interface) capturing `category`; it
     *       delegates to recordPayment(). This is the "functional interface models payment
     *       behaviour + lambda dispatch, no switch" design.
     */
    private void registerProcessors() {
        for (PaymentCategory category : PaymentCategory.values()) {
            processors.put(category, (conn, wallet, amount) -> recordPayment(conn, wallet, category, amount));
        }
    }

    @Override
    public void processPayment(String studentId, String category, BigDecimal amount) throws CampusPaymentException {
        // WHAT: Convert the raw category string to the enum (fail fast on an unknown category).
        // WHY:  Validating input before any DB work avoids opening a connection for a request that
        //       can never succeed; an unknown category is a processing error, not a balance issue.
        PaymentCategory paymentCategory = parseCategory(category);

        // STEP 1 — Validate the student exists.
        // WHAT: Look the student up via the (reused) student repository.
        // WHY:  Business rule: only real students can pay. Done before the wallet lookup so a bad
        //       id is rejected with the most specific error (StudentNotFound) and no wallet lock
        //       is taken. Reuses the existing repository rather than duplicating a lookup.
        studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException(ErrorMessages.STUDENT_NOT_FOUND + ": " + studentId));

        // WHAT: Acquire a JDBC connection for the atomic part of the flow.
        // WHY try-with-resources: guarantees the connection is returned/closed even on error.
        try (Connection conn = AppConfig.getConnection()) {

            // WHAT: Turn OFF auto-commit to begin a single transaction.
            // WHY:  The transaction insert, campus_payment insert and wallet debit must be ONE
            //       atomic unit — with auto-commit on, each statement would commit independently,
            //       allowing a partial payment (e.g. money taken but no payment recorded).
            conn.setAutoCommit(false);
            try {
                // STEP 2 — Validate the wallet exists (and lock it).
                // WHAT: SELECT ... FOR UPDATE via the transactional repository method.
                // WHY:  No wallet => cannot pay (WalletNotFound). The lock prevents concurrent
                //       debits from overspending the same balance until we commit/rollback.
                Wallet wallet = walletRepository.findByStudentId(conn, studentId)
                        .orElseThrow(() -> new WalletNotFoundException(
                                ErrorMessages.WALLET_NOT_FOUND + " for student " + studentId));

                // STEP 3 — Validate the amount.
                // WHY:  Reject zero/negative/null amounts (InvalidAmountException) — a core money rule.
                validateAmount(amount);

                // STEP 4 — Validate sufficient balance.
                // WHAT: compareTo(amount) < 0 means balance is strictly less than the amount.
                // WHY:  Enforces non-negative balances; chosen over subtract-then-check because
                //       compareTo expresses the rule directly without computing an intermediate value.
                if (wallet.getBalance().compareTo(amount) < 0) {
                    throw new InsufficientBalanceException(
                            ErrorMessages.INSUFFICIENT_BALANCE + " for payment of " + amount);
                }

                // STEPS 5-8 — Execute the payment via the functional-interface strategy.
                // WHAT: Look up this category's PaymentProcessor and run it.
                // WHY:  Dispatch by EnumMap lookup (no switch). The processor performs the
                //       transaction insert + campus_payment insert + wallet debit, all on `conn`
                //       so they remain inside this transaction.
                processors.get(paymentCategory).process(conn, wallet, amount);

                // STEP 9 — Commit.
                // WHAT: Make all three writes permanent atomically.
                // WHY:  Reached only when every step succeeded; this is the single commit point.
                conn.commit();

            } catch (InsufficientBalanceException e) {
                // WHAT: Undo anything done in this transaction.
                // WHY:  Defensive — guarantees no partial state even though the check precedes the
                //       writes. Then we audit the rejected attempt and rethrow the business error.
                conn.rollback();
                Logger.rollback("Payment rolled back (insufficient balance) for student " + studentId);
                recordFailure(studentId, amount, e.getMessage());
                throw e;
            } catch (DataAccessException e) {
                // WHAT: A persistence failure occurred mid-flow (e.g. an INSERT failed).
                // WHY rollback: discard the partial work so the DB is left consistent.
                // WHY translate: callers should see a business PaymentProcessingException, not a
                //      low-level data error; the cause is preserved for diagnostics.
                conn.rollback();
                Logger.rollback("Payment rolled back (data error) for student " + studentId + ": " + e.getMessage());
                recordFailure(studentId, amount, ErrorMessages.PAYMENT_PROCESSING_FAILED);
                throw new PaymentProcessingException(ErrorMessages.PAYMENT_PROCESSING_FAILED, e);
            }
        } catch (SQLException e) {
            // WHAT: Failure to obtain the connection or to commit/rollback itself.
            // WHY:  Logged as a SEVERE payment failure and surfaced as a processing exception, so
            //       infrastructure problems are visible and uniformly typed for the caller.
            Logger.paymentFailure("Connection/transaction error for student " + studentId + ": " + e.getMessage());
            throw new PaymentProcessingException(ErrorMessages.PAYMENT_PROCESSING_FAILED, e);
        }
    }

    /**
     * Standard payment execution shared by every category's processor.
     *
     * <p>WHAT: Creates the PAYMENT transaction, the linked campus_payment, and debits the wallet,
     *       all on the caller's connection; returns the new transaction id.
     * WHY one shared method: the steps are identical for every category, so centralising them
     *       avoids duplication across the five lambdas (DRY) and keeps the order/audit consistent.
     * HOW:  Insert transaction first to obtain txn_id, then insert the campus_payment that
     *       references it (satisfying the FK/1:1 link), then debit, then write the audit log.</p>
     */
    private long recordPayment(Connection conn, Wallet wallet, PaymentCategory category, BigDecimal amount) {
        // WHAT: Build and insert the master transaction row (type PAYMENT, status SUCCESS).
        // WHY:  Every money movement must exist in the transaction audit backbone first; its
        //       generated id is the anchor the campus_payment will reference.
        TransactionHistory txn = TransactionHistory.builder()
                .walletId(wallet.getWalletId())
                .txnType(TransactionType.PAYMENT)
                .amount(amount)
                .status(TransactionStatus.SUCCESS)
                .build();
        long txnId = transactionRepository.insert(conn, txn);

        // WHAT: Build and insert the campus_payment subtype row, linked by txnId.
        // WHY:  Records the payment-specific detail (category, payer) against its transaction,
        //       completing the supertype/subtype pair in one transaction.
        CampusPayment payment = CampusPayment.builder()
                .txnId(txnId)
                .studentId(wallet.getStudentId())
                .category(category)
                .amount(amount)
                .build();
        paymentRepository.insert(conn, payment);

        // WHAT: Debit the wallet by the amount.
        // WHY subtract here (service), persist in repo: the service owns the arithmetic
        //      (business rule); BigDecimal.subtract gives exact money math.
        walletRepository.updateBalance(conn, wallet.getWalletId(), wallet.getBalance().subtract(amount));

        // WHAT: Write a success audit entry.
        // WHY:  Audit logging gives a traceable record of every successful money movement (who,
        //       what category, how much, which txn) for compliance and debugging — independent of
        //       the DB rows, so the trail survives even if a row is later examined out of context.
        Logger.audit(String.format("PAYMENT SUCCESS | student=%s category=%s amount=%s txnId=%d",
                wallet.getStudentId(), category, amount, txnId));
        return txnId;
    }

    @Override
    public List<CampusPayment> getPaymentHistory(String studentId) throws CampusPaymentException {
        // WHAT: Guard against a missing/blank id before querying.
        // WHY:  A blank id can't identify a student; fail with the specific business exception.
        if (studentId == null || studentId.isBlank()) {
            throw new StudentNotFoundException(ErrorMessages.STUDENT_NOT_FOUND);
        }
        // WHY: a read-only history lookup needs no transaction, so it delegates straight to the repo.
        return paymentRepository.findByStudentId(studentId);
    }

    // --- validation helpers ---

    /**
     * WHAT: Reject null, zero or negative amounts.
     * WHY:  Core money rule (and mirrors the DB chk_pay_amount > 0); throwing the specific
     *       InvalidAmountException lets callers distinguish bad input from other failures.
     * HOW:  compareTo(ZERO) <= 0 is true for zero and negatives.
     */
    private void validateAmount(BigDecimal amount) throws InvalidAmountException {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException(ErrorMessages.INVALID_AMOUNT);
        }
    }

    /**
     * WHAT: Parse the category string into the PaymentCategory enum.
     * WHY:  Converts unsafe external input into a checked value; an unrecognised category is a
     *       PaymentProcessingException (it cannot be processed), distinct from amount/balance errors.
     * HOW:  trim()/toUpperCase() tolerate stray whitespace/case from the menu; valueOf throws
     *       IllegalArgumentException for unknown names, which we translate to the business exception.
     */
    private PaymentCategory parseCategory(String category) throws PaymentProcessingException {
        if (category == null) {
            throw new PaymentProcessingException(ErrorMessages.UNKNOWN_PAYMENT_CATEGORY);
        }
        try {
            return PaymentCategory.valueOf(category.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new PaymentProcessingException(ErrorMessages.UNKNOWN_PAYMENT_CATEGORY + ": " + category);
        }
    }

    /**
     * Best-effort audit of a failed payment.
     *
     * <p>WHAT: Logs the failure and writes a FAILED transaction row in its OWN connection.
     * WHY:  The main transaction has already rolled back (so any SUCCESS row is gone); recording a
     *       separate FAILED row preserves evidence that an attempt happened and why — valuable for
     *       fraud/ops review. It uses a fresh connection precisely because the original was rolled back.
     * HOW:  Failures here are swallowed (logged as SEVERE) so that auditing a failure can never
     *       mask or override the original business exception being thrown to the caller.</p>
     */
    private void recordFailure(String studentId, BigDecimal amount, String reason) {
        // WHY: a SEVERE payment-failure log entry makes every rejected payment visible in the log.
        Logger.paymentFailure(String.format("student=%s amount=%s reason=%s", studentId, amount, reason));
        try (Connection conn = AppConfig.getConnection()) {
            conn.setAutoCommit(false);
            walletRepository.findByStudentId(conn, studentId).ifPresent(wallet -> {
                TransactionHistory failed = TransactionHistory.builder()
                        .walletId(wallet.getWalletId())
                        .txnType(TransactionType.PAYMENT)
                        .amount(amount)
                        .status(TransactionStatus.FAILED)
                        .failureReason(reason)
                        .build();
                transactionRepository.insert(conn, failed);
            });
            conn.commit();
        } catch (SQLException | DataAccessException e) {
            // WHY: never let an audit-write problem propagate over the real business error.
            Logger.severe("Could not record failed payment for student " + studentId + ": " + e.getMessage());
        }
    }
}