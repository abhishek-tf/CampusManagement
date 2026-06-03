// Author: Hemanth
package com.campus.service.interfaces;

import com.campus.dto.WalletDTO;
import com.campus.exception.CampusPaymentException;

import java.math.BigDecimal;

/**
 * Business contract for wallet operations.
 *
 * WHY an interface (CLAUDE.md - Dependency Inversion & Interface Segregation):
 *   - Callers (menu layer, other services) depend on this abstraction, never on
 *     WalletServiceImpl, so the implementation can be swapped or mocked.
 *   - It is intentionally small and wallet-focused (no payment/expense methods),
 *     honouring Interface Segregation.
 *
 * WHAT it offers: create a wallet, inspect balance/details, and the three money
 *   movements that affect a wallet's balance (top-up, withdraw, transfer).
 *
 * HOW errors are reported: every method declares {@link CampusPaymentException}
 *   (the checked supertype). Concrete failures are its subtypes
 *   (InvalidAmountException, InsufficientBalanceException, etc.), so callers can
 *   catch broadly or narrowly. No business method ever returns an error code or
 *   null to signal failure.
 *
 * SCOPE NOTE: recording transaction / transfer_transaction rows and running
 *   fraud detection are owned by the transaction and fraud modules. This service
 *   deliberately limits itself to wallet balance + limit state.
 */
public interface IWalletService {

    /**
     * Creates a new wallet for a student with default caps from AppConfig.
     *
     * @param studentId owner of the wallet
     * @return the created wallet as a DTO (including its generated id)
     * @throws CampusPaymentException if the id is invalid
     */
    WalletDTO createWallet(Long studentId) throws CampusPaymentException;

    /**
     * Credits money into a wallet (e.g. cash/online top-up).
     *
     * @throws CampusPaymentException if the amount is invalid, the wallet is
     *         missing, or the credit would exceed the wallet's balance cap
     */
    void topupWallet(Long studentId, BigDecimal amount) throws CampusPaymentException;

    /**
     * Debits money from a wallet.
     *
     * @throws CampusPaymentException if the amount is invalid, the wallet is
     *         missing, or the balance is insufficient
     */
    void withdrawFromWallet(Long studentId, BigDecimal amount) throws CampusPaymentException;

    /**
     * Moves money between two wallets, enforcing the sender's daily transfer
     * limit and the receiver's balance cap.
     *
     * @throws CampusPaymentException if the amount/limit/balance rules fail or a
     *         wallet is missing
     */
    void transferMoney(Long fromStudentId, Long toStudentId, BigDecimal amount) throws CampusPaymentException;

    /**
     * @return the current balance of the student's wallet
     * @throws CampusPaymentException if the wallet is missing
     */
    BigDecimal getBalance(Long studentId) throws CampusPaymentException;

    /**
     * @return a read-only snapshot of the student's wallet
     * @throws CampusPaymentException if the wallet is missing
     */
    WalletDTO getWalletDetails(Long studentId) throws CampusPaymentException;
}
