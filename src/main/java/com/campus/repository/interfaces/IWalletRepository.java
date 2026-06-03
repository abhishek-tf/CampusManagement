package com.campus.repository.interfaces;

import com.campus.entity.Wallet;
import java.sql.Connection;
import java.util.Optional;
import java.util.List;

/**
 * Persistence contract for the {@code wallet} table.
 *
 * <p>WHAT: SQL operations over wallets, including transaction-aware variants for payments.
 * WHY:  Interface for DIP; SQL-only for SRP (balance arithmetic and limit rules belong to the
 *       service, not here).
 * HOW:  The two Connection-taking methods exist specifically so a payment can lock the wallet
 *       and debit it inside the service's open transaction.</p>
 */
public interface IWalletRepository {

    void save(Wallet wallet);
    Optional<Wallet> findById(Long walletId);
    Optional<Wallet> findByStudentId(String studentId);
    List<Wallet> findAll();
    void update(Wallet wallet);

    // --- transactional variants: participate in a caller-managed JDBC transaction ---

    /**
     * Locks and reads the wallet for a student within the caller's transaction
     * (SELECT ... FOR UPDATE).
     * WHY FOR UPDATE: prevents a concurrent payment/transfer from reading the same balance and
     * double-spending — the row stays locked until the caller commits or rolls back.
     */
    Optional<Wallet> findByStudentId(Connection conn, String studentId);

    /** Sets the wallet balance within the caller's transaction (the debit step of a payment). */
    void updateBalance(Connection conn, Long walletId, java.math.BigDecimal newBalance);
}