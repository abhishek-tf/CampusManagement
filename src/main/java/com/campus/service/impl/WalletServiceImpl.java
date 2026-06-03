// Author: Hemanth (JDBC conversion)
package com.campus.service.impl;

import com.campus.config.AppConfig;
import com.campus.constants.ErrorMessages;
import com.campus.dto.WalletDTO;
import com.campus.entity.Wallet;
import com.campus.exception.*;
import com.campus.repository.interfaces.IWalletRepository;
import com.campus.service.interfaces.IWalletService;
import com.campus.util.DBConnection;
import com.campus.util.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Business logic for wallet operations, persisted to MySQL via JDBC.
 *
 * <p>DESIGN: this service owns every transaction boundary. Each money movement
 * (top-up, withdraw, transfer) runs inside a single JDBC transaction that both
 * updates the wallet balance AND records the matching {@code transaction} row
 * (schema.sql: every money movement is one transaction row) - committing together
 * or rolling back entirely. SQL is delegated to {@link IWalletRepository}.</p>
 *
 * <p>MONEY: all arithmetic uses {@link BigDecimal} to avoid floating-point error.</p>
 */
public class WalletServiceImpl implements IWalletService {

    private static final Logger log = LoggerFactory.getLogger(WalletServiceImpl.class);

    // Transaction-type codes accepted by the schema's transaction.txn_type CHECK.
    private static final String TXN_DEPOSIT = "DEPOSIT";
    private static final String TXN_WITHDRAW = "WITHDRAW";
    private static final String TXN_TRANSFER = "TRANSFER";

    private final IWalletRepository walletRepository;

    public WalletServiceImpl(IWalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Override
    public WalletDTO createWallet(String studentId) throws CampusPaymentException {
        if (!ValidationUtil.isValidStudentId(studentId)) {
            throw new InvalidInputException("Invalid student id for wallet creation");
        }

        return executeInTransaction(conn -> {
            // Enforce "one wallet per student" (schema UNIQUE) with a clear business
            // error instead of surfacing a duplicate-key SQL failure.
            if (walletRepository.findByStudentId(conn, studentId).isPresent()) {
                throw new CampusPaymentException(
                        "Wallet already exists for student " + studentId, "WALLET_EXISTS");
            }

            Wallet wallet = Wallet.builder()
                    .studentId(studentId)
                    .balance(BigDecimal.ZERO)
                    .dailyTransferUsed(BigDecimal.ZERO)
                    .transferResetDate(LocalDate.now())
                    .maxBalanceCap(AppConfig.WALLET_MAX_BALANCE)
                    .dailyTransferLimit(AppConfig.DAILY_TRANSFER_LIMIT)
                    .build();
            walletRepository.save(conn, wallet);

            log.info("Wallet created: walletId={} studentId={}", wallet.getWalletId(), studentId);
            return WalletDTO.from(wallet);
        });
    }

    @Override
    public void topupWallet(String studentId, BigDecimal amount) throws CampusPaymentException {
        validateAmount(amount, "top-up");

        executeInTransaction(conn -> {
            Wallet wallet = requireWallet(conn, studentId);

            BigDecimal newBalance = wallet.getBalance().add(amount);
            if (newBalance.compareTo(wallet.getMaxBalanceCap()) > 0) {
                log.warn("Top-up rejected (cap exceeded): studentId={} amount={}", studentId, amount);
                throw new MaxBalanceExceededException(
                        "Top-up would exceed wallet balance cap of " + wallet.getMaxBalanceCap());
            }

            wallet.setBalance(newBalance);
            walletRepository.update(conn, wallet);
            walletRepository.insertTransaction(conn, wallet.getWalletId(), TXN_DEPOSIT, amount);

            log.info("Top-up success: studentId={} amount={} newBalance={}", studentId, amount, newBalance);
            return null;
        });
    }

    @Override
    public void withdrawFromWallet(String studentId, BigDecimal amount) throws CampusPaymentException {
        validateAmount(amount, "withdrawal");

        executeInTransaction(conn -> {
            Wallet wallet = requireWallet(conn, studentId);

            if (wallet.getBalance().compareTo(amount) < 0) {
                log.warn("Withdrawal rejected (insufficient funds): studentId={} amount={} balance={}",
                        studentId, amount, wallet.getBalance());
                throw new InsufficientBalanceException("Insufficient balance for withdrawal");
            }

            wallet.setBalance(wallet.getBalance().subtract(amount));
            walletRepository.update(conn, wallet);
            walletRepository.insertTransaction(conn, wallet.getWalletId(), TXN_WITHDRAW, amount);

            log.info("Withdrawal success: studentId={} amount={} newBalance={}",
                    studentId, amount, wallet.getBalance());
            return null;
        });
    }

    @Override
    public void transferMoney(String fromStudentId, String toStudentId, BigDecimal amount)
            throws CampusPaymentException {
        validateTransferAmount(amount);

        // Self-transfer is meaningless and the schema forbids it (CHECK from <> to).
        if (fromStudentId != null && fromStudentId.equals(toStudentId)) {
            throw new InvalidAmountException("Cannot transfer to the same wallet");
        }

        executeInTransaction(conn -> {
            // Lock both wallets for the duration of the transfer.
            Wallet sender = walletRepository.findByStudentIdForUpdate(conn, fromStudentId)
                    .orElseThrow(() -> new WalletNotFoundException("Sender wallet not found"));
            Wallet receiver = walletRepository.findByStudentIdForUpdate(conn, toStudentId)
                    .orElseThrow(() -> new WalletNotFoundException("Recipient wallet not found"));

            // 1) Sender must have the funds.
            if (sender.getBalance().compareTo(amount) < 0) {
                log.warn("Transfer rejected (insufficient funds): from={} amount={}", fromStudentId, amount);
                throw new InsufficientBalanceException("Insufficient balance for transfer");
            }

            // 2) Roll the daily counter over if we've crossed into a new calendar day.
            resetDailyCounterIfNewDay(sender);

            // 3) Enforce the sender's daily transfer limit.
            BigDecimal usedAfter = sender.getDailyTransferUsed().add(amount);
            if (usedAfter.compareTo(sender.getDailyTransferLimit()) > 0) {
                log.warn("Transfer rejected (daily limit): from={} usedAfter={} limit={}",
                        fromStudentId, usedAfter, sender.getDailyTransferLimit());
                throw new DailyLimitExceededException("Daily transfer limit exceeded");
            }

            // 4) The credit must not overflow the receiver's balance cap.
            BigDecimal receiverNewBalance = receiver.getBalance().add(amount);
            if (receiverNewBalance.compareTo(receiver.getMaxBalanceCap()) > 0) {
                log.warn("Transfer rejected (receiver cap): to={} amount={}", toStudentId, amount);
                throw new MaxBalanceExceededException("Transfer would exceed recipient's balance cap");
            }

            // Apply the movement to both wallets and record it atomically:
            // debit + credit + transaction + transfer_transaction commit together.
            sender.setBalance(sender.getBalance().subtract(amount));
            sender.setDailyTransferUsed(usedAfter);
            receiver.setBalance(receiverNewBalance);

            walletRepository.update(conn, sender);
            walletRepository.update(conn, receiver);
            long txnId = walletRepository.insertTransaction(conn, sender.getWalletId(), TXN_TRANSFER, amount);
            walletRepository.insertTransferDetail(conn, txnId, fromStudentId, toStudentId);

            log.info("Transfer success: from={} to={} amount={} txnId={}",
                    fromStudentId, toStudentId, amount, txnId);
            return null;
        });
    }

    @Override
    public BigDecimal getBalance(String studentId) throws CampusPaymentException {
        return executeInTransaction(conn -> requireWallet(conn, studentId).getBalance());
    }

    @Override
    public WalletDTO getWalletDetails(String studentId) throws CampusPaymentException {
        return executeInTransaction(conn -> WalletDTO.from(requireWallet(conn, studentId)));
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
            log.error("Database error during wallet operation", e);
            throw new CampusPaymentException(ErrorMessages.DATABASE_ERROR, e);
        }
    }

    private Wallet requireWallet(Connection conn, String studentId)
            throws SQLException, CampusPaymentException {
        return walletRepository.findByStudentIdForUpdate(conn, studentId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for student " + studentId));
    }

    private void validateAmount(BigDecimal amount, String operation) throws InvalidAmountException {
        if (!ValidationUtil.isValidAmount(amount)) {
            throw new InvalidAmountException("Invalid amount for " + operation);
        }
    }

    private void validateTransferAmount(BigDecimal amount) throws InvalidAmountException {
        validateAmount(amount, "transfer");
        if (amount.compareTo(AppConfig.MIN_TRANSFER_AMOUNT) < 0
                || amount.compareTo(AppConfig.MAX_TRANSFER_AMOUNT) > 0) {
            throw new InvalidAmountException(
                    "Transfer amount must be between " + AppConfig.MIN_TRANSFER_AMOUNT
                            + " and " + AppConfig.MAX_TRANSFER_AMOUNT);
        }
    }

    private void resetDailyCounterIfNewDay(Wallet wallet) {
        LocalDate today = LocalDate.now();
        if (wallet.getTransferResetDate() == null || wallet.getTransferResetDate().isBefore(today)) {
            wallet.setDailyTransferUsed(BigDecimal.ZERO);
            wallet.setTransferResetDate(today);
        }
    }
}
