// Author: Hemanth
package com.campus.service.impl;

import com.campus.config.AppConfig;
import com.campus.dto.WalletDTO;
import com.campus.entity.Wallet;
import com.campus.exception.*;
import com.campus.repository.interfaces.IWalletRepository;
import com.campus.service.interfaces.IWalletService;
import com.campus.util.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Business logic for wallet operations (CLAUDE.md: all rules live in the service
 * layer, never in entities or repositories).
 *
 * DESIGN (SOLID):
 *   - SRP: this class only orchestrates wallet rules. It does not talk to the
 *     database directly, validate emails, or detect fraud.
 *   - DIP: it depends on the {@link IWalletRepository} abstraction, injected via
 *     the constructor — never on a concrete repository. (The persistence
 *     implementation is owned by a separate piece of work.)
 *
 * MONEY: all arithmetic uses {@link BigDecimal} to avoid floating-point rounding.
 */
public class WalletServiceImpl implements IWalletService {

    // SLF4J logger — the configured logging framework (logback). CLAUDE.md
    // forbids System.out in services; structured logging gives us an audit trail
    // of every successful and rejected money movement.
    private static final Logger log = LoggerFactory.getLogger(WalletServiceImpl.class);

    private final IWalletRepository walletRepository;

    /**
     * Constructor injection of the repository dependency (DIP). The caller (e.g.
     * AppConfig) decides which implementation to supply.
     */
    public WalletServiceImpl(IWalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Override
    public WalletDTO createWallet(Long studentId) throws CampusPaymentException {
        // WHY validate first: never hit persistence with a bad id (CLAUDE.md -
        // validation happens before repository calls).
        if (!ValidationUtil.isValidStudentId(studentId)) {
            throw new InvalidAmountException("Invalid student id for wallet creation");
        }

        // Enforce "one wallet per student" (schema: UNIQUE student_id) early so the
        // user gets a clear business error instead of a duplicate-key failure.
        if (walletRepository.findByStudentId(studentId).isPresent()) {
            throw new CampusPaymentException(
                    "Wallet already exists for student " + studentId, "WALLET_EXISTS");
        }

        // Build a fresh wallet seeded with the platform default caps/limits.
        Wallet wallet = Wallet.builder()
                .studentId(studentId)
                .balance(BigDecimal.ZERO)
                .dailyTransferUsed(BigDecimal.ZERO)
                .transferResetDate(LocalDate.now())
                .maxBalanceCap(AppConfig.WALLET_MAX_BALANCE)
                .dailyTransferLimit(AppConfig.DAILY_TRANSFER_LIMIT)
                .updatedAt(LocalDateTime.now())
                .build();

        walletRepository.save(wallet); // repository sets the generated walletId
        log.info("Wallet created: walletId={} studentId={}", wallet.getWalletId(), studentId);
        return WalletDTO.from(wallet);
    }

    @Override
    public void topupWallet(Long studentId, BigDecimal amount) throws CampusPaymentException {
        validateAmount(amount, "top-up");
        Wallet wallet = requireWallet(studentId);

        // A top-up must not push the balance over the wallet's cap.
        BigDecimal newBalance = wallet.getBalance().add(amount);
        if (newBalance.compareTo(wallet.getMaxBalanceCap()) > 0) {
            log.warn("Top-up rejected (cap exceeded): studentId={} amount={}", studentId, amount);
            throw new MaxBalanceExceededException(
                    "Top-up would exceed wallet balance cap of " + wallet.getMaxBalanceCap());
        }

        wallet.setBalance(newBalance);
        wallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.update(wallet);
        log.info("Top-up success: studentId={} amount={} newBalance={}", studentId, amount, newBalance);
    }

    @Override
    public void withdrawFromWallet(Long studentId, BigDecimal amount) throws CampusPaymentException {
        validateAmount(amount, "withdrawal");
        Wallet wallet = requireWallet(studentId);

        // Cannot withdraw more than is available (schema also has balance >= 0).
        if (wallet.getBalance().compareTo(amount) < 0) {
            log.warn("Withdrawal rejected (insufficient funds): studentId={} amount={} balance={}",
                    studentId, amount, wallet.getBalance());
            throw new InsufficientBalanceException("Insufficient balance for withdrawal");
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        wallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.update(wallet);
        log.info("Withdrawal success: studentId={} amount={} newBalance={}",
                studentId, amount, wallet.getBalance());
    }

    @Override
    public void transferMoney(Long fromStudentId, Long toStudentId, BigDecimal amount)
            throws CampusPaymentException {
        validateTransferAmount(amount);

        // Self-transfer is meaningless and the schema's transfer_transaction table
        // forbids it (CHECK from <> to); reject before doing any work.
        if (fromStudentId != null && fromStudentId.equals(toStudentId)) {
            throw new InvalidAmountException("Cannot transfer to the same wallet");
        }

        Wallet sender = requireWallet(fromStudentId, "Sender wallet not found");
        Wallet receiver = requireWallet(toStudentId, "Recipient wallet not found");

        // 1) Sender must have the funds.
        if (sender.getBalance().compareTo(amount) < 0) {
            log.warn("Transfer rejected (insufficient funds): from={} amount={}", fromStudentId, amount);
            throw new InsufficientBalanceException("Insufficient balance for transfer");
        }

        // 2) Roll the daily counter over if we've crossed into a new calendar day.
        //    (Done inline with LocalDate — DateTimeUtil works on LocalDateTime, but
        //    transfer_reset_date is a DATE, so a LocalDate compare maps the column exactly.)
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

        // Apply the movement to both in-memory wallets.
        LocalDateTime now = LocalDateTime.now();
        sender.setBalance(sender.getBalance().subtract(amount));
        sender.setDailyTransferUsed(usedAfter);
        sender.setUpdatedAt(now);

        receiver.setBalance(receiverNewBalance);
        receiver.setUpdatedAt(now);

        // NOTE: both updates should commit atomically. Transaction boundaries are a
        // persistence concern owned by the repository implementation; the service
        // simply hands it the two final states.
        walletRepository.update(sender);
        walletRepository.update(receiver);
        log.info("Transfer success: from={} to={} amount={}", fromStudentId, toStudentId, amount);
    }

    @Override
    public BigDecimal getBalance(Long studentId) throws CampusPaymentException {
        return requireWallet(studentId).getBalance();
    }

    @Override
    public WalletDTO getWalletDetails(Long studentId) throws CampusPaymentException {
        return WalletDTO.from(requireWallet(studentId));
    }

    // ----------------------------------------------------------------------
    // Private helpers — kept here to keep public methods short and DRY.
    // ----------------------------------------------------------------------

    /** Loads a wallet or fails with a clear, typed error. */
    private Wallet requireWallet(Long studentId) throws CampusPaymentException {
        return requireWallet(studentId, "Wallet not found for student " + studentId);
    }

    private Wallet requireWallet(Long studentId, String message) throws CampusPaymentException {
        return walletRepository.findByStudentId(studentId)
                .orElseThrow(() -> new WalletNotFoundException(message));
    }

    /** Rejects null / non-positive amounts (shared by top-up and withdrawal). */
    private void validateAmount(BigDecimal amount, String operation) throws InvalidAmountException {
        if (!ValidationUtil.isValidAmount(amount)) {
            throw new InvalidAmountException("Invalid amount for " + operation);
        }
    }

    /** Transfers have both a positive-amount rule and configured min/max bounds. */
    private void validateTransferAmount(BigDecimal amount) throws InvalidAmountException {
        validateAmount(amount, "transfer");
        if (amount.compareTo(AppConfig.MIN_TRANSFER_AMOUNT) < 0
                || amount.compareTo(AppConfig.MAX_TRANSFER_AMOUNT) > 0) {
            throw new InvalidAmountException(
                    "Transfer amount must be between " + AppConfig.MIN_TRANSFER_AMOUNT
                            + " and " + AppConfig.MAX_TRANSFER_AMOUNT);
        }
    }

    /**
     * Resets the daily transfer counter when the stored reset date is null or in
     * the past, so a fresh limit applies each calendar day without scanning the
     * transaction history.
     */
    private void resetDailyCounterIfNewDay(Wallet wallet) {
        LocalDate today = LocalDate.now();
        if (wallet.getTransferResetDate() == null || wallet.getTransferResetDate().isBefore(today)) {
            wallet.setDailyTransferUsed(BigDecimal.ZERO);
            wallet.setTransferResetDate(today);
        }
    }
}
