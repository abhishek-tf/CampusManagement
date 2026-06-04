package com.campus.service.impl;

import com.campus.constants.ErrorMessages;
import com.campus.entity.CampusPayment;
import com.campus.entity.Wallet;
import com.campus.enums.PaymentCategory;
import com.campus.exception.CampusPaymentException;
import com.campus.exception.InsufficientBalanceException;
import com.campus.exception.InvalidAmountException;
import com.campus.exception.InvalidInputException;
import com.campus.exception.WalletNotFoundException;
import com.campus.repository.interfaces.IPaymentRepository;
import com.campus.repository.interfaces.IWalletRepository;
import com.campus.service.interfaces.IPaymentService;
import com.campus.service.interfaces.PaymentProcessor;
import com.campus.util.Logger;
import com.campus.util.Tx;
import com.campus.util.ValidationUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Business logic for campus payments, persisted to MySQL via JDBC.
 *
 * <p>A payment is a single atomic transaction (via {@link Tx}): it debits the
 * wallet, writes the parent {@code transaction} row (txn_type = PAYMENT) and the
 * {@code campus_payment} detail - all committing together or rolling back.</p>
 *
 * <p>The per-category processing behaviour is modelled with the
 * {@link PaymentProcessor} functional interface and registered once in an
 * {@link EnumMap}; dispatch is a single map lookup (open for extension - add a
 * category, register a lambda - with no switch to edit).</p>
 */
public class PaymentServiceImpl implements IPaymentService {

    private static final String TXN_PAYMENT = "PAYMENT";

    private final IPaymentRepository paymentRepository;
    private final IWalletRepository walletRepository;
    private final Map<PaymentCategory, PaymentProcessor> processors;

    public PaymentServiceImpl(IPaymentRepository paymentRepository,
                              IWalletRepository walletRepository) {
        this.paymentRepository = paymentRepository;
        this.walletRepository = walletRepository;
        this.processors = buildProcessors();
    }

    /** Registers one payment-processing strategy (a lambda) per category. */
    private Map<PaymentCategory, PaymentProcessor> buildProcessors() {
        Map<PaymentCategory, PaymentProcessor> map = new EnumMap<>(PaymentCategory.class);
        for (PaymentCategory category : PaymentCategory.values()) {
            map.put(category, (conn, wallet, amount) -> recordPayment(conn, wallet, category, amount));
        }
        return map;
    }

    @Override
    public void processPayment(String studentId, String category, BigDecimal amount)
            throws CampusPaymentException {
        if (!ValidationUtil.isValidStudentId(studentId)) {
            throw new InvalidInputException("Invalid student id for payment");
        }
        if (!ValidationUtil.isValidAmount(amount)) {
            throw new InvalidAmountException("Invalid payment amount");
        }
        PaymentCategory paymentCategory = parseCategory(category);
        PaymentProcessor processor = processors.get(paymentCategory);

        Tx.inTransaction(conn -> {
            Wallet wallet = walletRepository.findByStudentIdForUpdate(conn, studentId)
                    .orElseThrow(() -> new WalletNotFoundException(
                            "Wallet not found for student " + studentId));

            if (wallet.getBalance().compareTo(amount) < 0) {
                Logger.paymentFailure("studentId=" + studentId + " category=" + paymentCategory
                        + " amount=" + amount + " (insufficient funds)");
                throw new InsufficientBalanceException("Insufficient balance for payment");
            }

            long txnId = processor.process(conn, wallet, amount);
            Logger.audit("Payment: studentId=" + studentId + " category=" + paymentCategory
                    + " amount=" + amount + " txnId=" + txnId);
            return null;
        });
    }

    @Override
    public List<CampusPayment> getPaymentHistory(String studentId) throws CampusPaymentException {
        if (!ValidationUtil.isValidStudentId(studentId)) {
            throw new InvalidInputException("Invalid student id");
        }
        return Tx.inTransaction(conn -> paymentRepository.findByStudentId(conn, studentId));
    }

    /**
     * The work shared by every category strategy: debit the (locked) wallet,
     * record the PAYMENT transaction, then the campus_payment detail. Runs inside
     * the caller's transaction so the whole payment is atomic.
     */
    private long recordPayment(Connection conn, Wallet wallet, PaymentCategory category, BigDecimal amount)
            throws CampusPaymentException {
        try {
            wallet.setBalance(wallet.getBalance().subtract(amount));
            walletRepository.update(conn, wallet);
            long txnId = walletRepository.insertTransaction(conn, wallet.getWalletId(), TXN_PAYMENT, amount);

            CampusPayment payment = CampusPayment.builder()
                    .txnId(txnId)
                    .studentId(wallet.getStudentId())
                    .category(category)
                    .amount(amount)
                    .build();
            paymentRepository.save(conn, payment);
            return txnId;
        } catch (SQLException e) {
            throw new CampusPaymentException(ErrorMessages.DATABASE_ERROR, e);
        }
    }

    /** Translates the raw category string into the enum, or a clear business error. */
    private PaymentCategory parseCategory(String category) throws CampusPaymentException {
        if (category == null) {
            throw new InvalidInputException("Payment category is required");
        }
        try {
            return PaymentCategory.valueOf(category.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidInputException("Unknown payment category: " + category);
        }
    }
}
