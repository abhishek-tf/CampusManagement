package com.campus.repository.impl;

import com.campus.entity.Wallet;
import com.campus.repository.interfaces.IWalletRepository;
import java.util.*;

public class WalletRepositoryImpl implements IWalletRepository {
    private final Map<Long, Wallet> store = new HashMap<>();
    private static long idCounter = 1;

    @Override
    public void save(Wallet wallet) {
        if (wallet.getWalletId() == null) {
            wallet.setWalletId(idCounter++);
        }
        store.put(wallet.getWalletId(), wallet);
    }

    @Override
    public Optional<Wallet> findById(Long walletId) {
        return Optional.ofNullable(store.get(walletId));
    }

    @Override
    public Optional<Wallet> findByStudentId(Long studentId) {
        return store.values().stream()
                .filter(w -> w.getStudentId().equals(studentId))
                .findFirst();
    }

    @Override
    public List<Wallet> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void update(Wallet wallet) {
        if (store.containsKey(wallet.getWalletId())) {
            store.put(wallet.getWalletId(), wallet);
        }
    }
}
