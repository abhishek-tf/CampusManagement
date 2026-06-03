package com.campus.repository.interfaces;

import com.campus.entity.TransactionHistory;
import java.util.Optional;
import java.util.List;

public interface ITransactionRepository {
    void save(TransactionHistory transaction);
    Optional<TransactionHistory> findById(Long transactionId);
    List<TransactionHistory> findByStudentId(Long studentId);
    List<TransactionHistory> findAll();
}
