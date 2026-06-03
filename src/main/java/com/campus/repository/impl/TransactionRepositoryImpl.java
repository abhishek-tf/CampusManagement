package com.campus.repository.impl;

import com.campus.entity.TransactionHistory;
import com.campus.repository.interfaces.ITransactionRepository;
import java.util.*;
import java.util.stream.Collectors;

public class TransactionRepositoryImpl implements ITransactionRepository {
    private final Map<Long, TransactionHistory> store = new HashMap<>();
    private static long idCounter = 1;

    @Override
    public void save(TransactionHistory transaction) {
        if (transaction.getTransactionId() == null) {
            transaction.setTransactionId(idCounter++);
        }
        store.put(transaction.getTransactionId(), transaction);
    }

    @Override
    public Optional<TransactionHistory> findById(Long transactionId) {
        return Optional.ofNullable(store.get(transactionId));
    }

    @Override
    public List<TransactionHistory> findByStudentId(Long studentId) {
        return store.values().stream()
                .filter(t -> t.getStudentId().equals(studentId))
                .collect(Collectors.toList());
    }

    @Override
    public List<TransactionHistory> findAll() {
        return new ArrayList<>(store.values());
    }
}
