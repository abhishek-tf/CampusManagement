package com.campus.service.impl;

import com.campus.entity.FraudFlag;
import com.campus.entity.Transaction;
import com.campus.entity.Wallet;
import com.campus.enums.TransactionStatus;
import com.campus.exception.CampusPaymentException;
import com.campus.exception.FraudDetectedException;
import com.campus.repository.interfaces.IFraudRepository;
import com.campus.repository.interfaces.ITransactionRepository;
import com.campus.repository.interfaces.IWalletRepository;
import com.campus.service.interfaces.IFraudDetectionService;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public class FraudDetectionServiceImpl implements IFraudDetectionService {
    private final IFraudRepository fraudRepository;
    private final ITransactionRepository transactionRepository;
    private final IWalletRepository walletRepository;
    private static final int FRAUD_THRESHOLD = 10;
    private static final int TIME_WINDOW_MINUTES = 5;

    public FraudDetectionServiceImpl(IFraudRepository fraudRepository,
                                   ITransactionRepository transactionRepository,
                                   IWalletRepository walletRepository) {
        this.fraudRepository = fraudRepository;
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
    }

    @Override
    public void detectAndFlagFraud(Long studentId) throws CampusPaymentException {
        if (studentId == null) {
            throw new CampusPaymentException("Invalid student ID");
        }

        List<Transaction> recentTransactions = walletRepository.findByStudentId(studentId)
                .map(Wallet::getWalletId)
                .map(transactionRepository::findByWalletId)
                .orElse(Collections.emptyList());
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(TIME_WINDOW_MINUTES);

        long suspiciousCount = recentTransactions.stream()
                .filter(t -> t.getCreatedAt().isAfter(fiveMinutesAgo))
                .count();

        if (suspiciousCount >= FRAUD_THRESHOLD) {
            FraudFlag fraudFlag = FraudFlag.builder()
                    .studentId(studentId)
                    .reason("Multiple transactions detected in short timeframe")
                    .suspiciousTransactionCount((int) suspiciousCount)
                    .status(TransactionStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .flaggedAt(LocalDateTime.now())
                    .build();

            fraudRepository.save(fraudFlag);
            throw new FraudDetectedException("Fraudulent activity detected");
        }
    }

    @Override
    public boolean isFraudulent(Long studentId) {
        if (studentId == null) {
            return false;
        }

        return fraudRepository.findByStudentId(studentId)
                .map(f -> f.getStatus() == TransactionStatus.PENDING)
                .orElse(false);
    }
}
