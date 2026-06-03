package com.campus.repository.interfaces;

import com.campus.entity.Transaction;
import java.util.List;
import java.util.Optional;

public interface ITransactionRepository {
    void save(Transaction transaction);
    Optional<Transaction> findById(Long txnId);
    List<Transaction> findByWalletId(Long walletId);
    List<Transaction> findAll();
}
