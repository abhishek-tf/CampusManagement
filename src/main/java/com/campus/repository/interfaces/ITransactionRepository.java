package com.campus.repository.interfaces;

import com.campus.entity.Transaction;
import java.util.List;

/** Persistence operations for the {@code transaction} audit table. SQL only. */
public interface ITransactionRepository {
    void save(Transaction transaction);
    List<Transaction> findByWalletId(Long walletId);
}
