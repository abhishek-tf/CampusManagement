package com.campus.service.impl;

import com.campus.entity.Wallet;
import com.campus.exception.*;
import com.campus.repository.interfaces.IWalletRepository;
import com.campus.service.interfaces.IWalletService;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WalletServiceImpl implements IWalletService {
    private final IWalletRepository walletRepository;

    public WalletServiceImpl(IWalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Override
    public void topupWallet(Long studentId, BigDecimal amount) throws CampusPaymentException {
        if (studentId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Invalid amount for topup");
        }

        Wallet wallet = walletRepository.findByStudentId(studentId)
                .orElseThrow(() -> new StudentNotFoundException("Student wallet not found"));

        wallet.setBalance(wallet.getBalance().add(amount));
        wallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.update(wallet);
    }

    @Override
    public void withdrawFromWallet(Long studentId, BigDecimal amount) throws CampusPaymentException {
        if (studentId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Invalid amount for withdrawal");
        }

        Wallet wallet = walletRepository.findByStudentId(studentId)
                .orElseThrow(() -> new StudentNotFoundException("Student wallet not found"));

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient balance for withdrawal");
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        wallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.update(wallet);
    }

    @Override
    public void transferMoney(Long fromStudentId, Long toStudentId, BigDecimal amount) throws CampusPaymentException {
        if (fromStudentId == null || toStudentId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Invalid transfer amount");
        }

        if (fromStudentId.equals(toStudentId)) {
            throw new InvalidAmountException("Cannot transfer to same account");
        }

        Wallet fromWallet = walletRepository.findByStudentId(fromStudentId)
                .orElseThrow(() -> new StudentNotFoundException("Sender wallet not found"));
        
        Wallet toWallet = walletRepository.findByStudentId(toStudentId)
                .orElseThrow(() -> new StudentNotFoundException("Recipient wallet not found"));

        if (fromWallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient balance for transfer");
        }

        if (fromWallet.getDailyTransferSpent().add(amount).compareTo(fromWallet.getDailyTransferLimit()) > 0) {
            throw new DailyLimitExceededException("Daily transfer limit exceeded");
        }

        fromWallet.setBalance(fromWallet.getBalance().subtract(amount));
        fromWallet.setDailyTransferSpent(fromWallet.getDailyTransferSpent().add(amount));
        
        toWallet.setBalance(toWallet.getBalance().add(amount));
        
        fromWallet.setUpdatedAt(LocalDateTime.now());
        toWallet.setUpdatedAt(LocalDateTime.now());

        walletRepository.update(fromWallet);
        walletRepository.update(toWallet);
    }

    @Override
    public BigDecimal getBalance(Long studentId) throws CampusPaymentException {
        Wallet wallet = walletRepository.findByStudentId(studentId)
                .orElseThrow(() -> new StudentNotFoundException("Student wallet not found"));
        return wallet.getBalance();
    }
}
