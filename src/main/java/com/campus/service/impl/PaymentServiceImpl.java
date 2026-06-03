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
import com.campus.util.DBConnection;
import com.campus.util.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Business logic for campus payments, persisted to MySQL via JDBC.
 *
 * <p>A payment is a single atomic transaction: it debits the wallet, writes the
 * parent {@code transaction} row (txn_type = PAYMENT) and the {@code campus_payment}
 * detail - all committing together or rolling back entirely.</p>
 *
 * <p>It depends on {@link IWalletRepository} for the wallet/transaction writes and
 * {@link IPaymentRepository} for the payment detail (the {@code wallet} and
 * {@code transaction} tables are owned by the wallet repository).</p>
 */
public class PaymentServiceImpl implements IPaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private static final String TXN_PAYMENT = "PAYMENT";

    private final IPaymentRepository paymentRepository;
    private final IWalletRepository walletRepository;

    public PaymentServiceImpl(IPaymentRepository paymentRepository,
                              IWalletRepository walletRepository) {
        this.paymentRepository = paymentRepository;
        this.walletRepository = walletRepository;
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

        executeInTransaction(conn -> {
            Wallet wallet = walletRepository.findByStudentIdForUpdate(conn, studentId)
                    .orElseThrow(() -> new WalletNotFoundException(
                            "Wallet not found for student " + studentId));

            if (wallet.getBalance().compareTo(amount) < 0) {
                log.warn("Payment rejected (insufficient funds): studentId={} amount={} balance={}",
                        studentId, amount, wallet.getBalance());
                throw new InsufficientBalanceException("Insufficient balance for payment");
            }

            // Debit the wallet, record the PAYMENT transaction, then the payment detail.
            wallet.setBalance(wallet.getBalance().subtract(amount));
            walletRepository.update(conn, wallet);
            long txnId = walletRepository.insertTransaction(conn, wallet.getWalletId(), TXN_PAYMENT, amount);

            CampusPayment payment = CampusPayment.builder()
                    .txnId(txnId)
                    .studentId(studentId)
                    .category(paymentCategory)
                    .amount(amount)
                    .build();
            paymentRepository.save(conn, payment);

            log.info("Payment success: studentId={} category={} amount={} txnId={}",
                    studentId, paymentCategory, amount, txnId);
            return null;
        });
    }

    @Override
    public List<CampusPayment> getPaymentHistory(String studentId) throws CampusPaymentException {
        if (!ValidationUtil.isValidStudentId(studentId)) {
            throw new InvalidInputException("Invalid student id");
        }
        return executeInTransaction(conn -> paymentRepository.findByStudentId(conn, studentId));
    }

    // ----------------------------------------------------------------------
    // Transaction template & helpers
    // ----------------------------------------------------------------------

    @FunctionalInterface
    private interface ConnectionWork<T> {
        T apply(Connection conn) throws SQLException, CampusPaymentException;
    }

    /** Runs {@code work} in one JDBC transaction: commit on success, rollback on failure. */
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
            throw e;
        } catch (SQLException e) {
            log.error("Database error during payment operation", e);
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
