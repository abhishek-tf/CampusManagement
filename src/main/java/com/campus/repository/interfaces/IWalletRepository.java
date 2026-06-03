package com.campus.repository.interfaces;

import com.campus.entity.Wallet;
import java.util.Optional;
import java.util.List;

public interface IWalletRepository {
    void save(Wallet wallet);
    Optional<Wallet> findById(Long walletId);
    Optional<Wallet> findByStudentId(Long studentId);
    List<Wallet> findAll();
    void update(Wallet wallet);
}
