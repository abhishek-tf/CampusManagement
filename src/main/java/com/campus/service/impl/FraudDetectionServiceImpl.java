package com.campus.service.impl;

import com.campus.entity.FraudFlag;
import com.campus.entity.TransactionHistory;
import com.campus.enums.TransactionStatus;
import com.campus.exception.CampusPaymentException;
import com.campus.exception.FraudDetectedException;
import com.campus.repository.interfaces.IFraudRepository;
import com.campus.repository.interfaces.ITransactionRepository;
import com.campus.service.interfaces.IFraudDetectionService;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class FraudDetectionServiceImpl implements IFraudDetectionService {
    private final IFraudRepository fraudRepository;
    private final ITransactionRepository transactionRepository;
    private static final int FRAUD_THRESHOLD = 10;
    private static final int TIME_WINDOW_MINUTES = 5;

    public FraudDetectionServiceImpl(IFraudRepository fraudRepository, 
                                   ITransactionRepository transactionRepository) {
        this.fraudRepository = fraudRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public void detectAndFlagFraud(Long studentId) throws CampusPaymentException {
        if (studentId == null) {
            throw new CampusPaymentException("Invalid student ID");
        }

        List<TransactionHistory> recentTransactions = transactionRepository.findByStudentId(studentId);
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
