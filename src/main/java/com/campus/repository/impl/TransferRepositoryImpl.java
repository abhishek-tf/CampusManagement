package com.campus.repository.impl;

import com.campus.entity.TransferTransaction;
import com.campus.repository.interfaces.ITransferRepository;
import java.util.*;
import java.util.stream.Collectors;

public class TransferRepositoryImpl implements ITransferRepository {
    private final Map<Long, TransferTransaction> store = new HashMap<>();
    private static long idCounter = 1;

    @Override
    public void save(TransferTransaction transfer) {
        if (transfer.getTransferId() == null) {
            transfer.setTransferId(idCounter++);
        }
        store.put(transfer.getTransferId(), transfer);
    }

    @Override
    public Optional<TransferTransaction> findById(Long transferId) {
        return Optional.ofNullable(store.get(transferId));
    }

    @Override
    public List<TransferTransaction> findByFromStudentId(Long studentId) {
        return store.values().stream()
                .filter(t -> t.getFromStudentId().equals(studentId))
                .collect(Collectors.toList());
    }

    @Override
    public List<TransferTransaction> findByToStudentId(Long studentId) {
        return store.values().stream()
                .filter(t -> t.getToStudentId().equals(studentId))
                .collect(Collectors.toList());
    }

    @Override
    public List<TransferTransaction> findAll() {
        return new ArrayList<>(store.values());
    }
}
