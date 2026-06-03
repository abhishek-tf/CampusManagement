package com.campus.service.impl;

import com.campus.constants.ErrorMessages;
import com.campus.entity.Transaction;
import com.campus.enums.TransactionStatus;
import com.campus.enums.TransactionType;
import com.campus.exception.CampusPaymentException;
import com.campus.exception.InvalidAmountException;
import com.campus.repository.interfaces.ITransactionRepository;
import com.campus.service.interfaces.ITransactionService;
import com.campus.util.Logger;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Transaction-tracking business logic: validates input, then persists/reads
 * transaction rows through {@link ITransactionRepository}.
 */
public class TransactionServiceImpl implements ITransactionService {

    private final ITransactionRepository transactionRepository;

    public TransactionServiceImpl(ITransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public Transaction recordTransaction(Long walletId, String txnType, BigDecimal amount)
            throws CampusPaymentException {

        if (walletId == null) {
            throw new CampusPaymentException(ErrorMessages.INVALID_INPUT);
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException(ErrorMessages.INVALID_AMOUNT);
        }
        TransactionType type = parseType(txnType);

        Transaction transaction = new Transaction(
                null,
                walletId,
                type.name(),
                amount.doubleValue(),
                TransactionStatus.SUCCESS.name(),
                null,
                LocalDateTime.now());

        transactionRepository.save(transaction);
        Logger.info("Recorded " + type.name() + " of " + amount
                + " for wallet " + walletId + " (txn #" + transaction.getTxnId() + ")");
        return transaction;
    }

    @Override
    public List<Transaction> getWalletHistory(Long walletId) throws CampusPaymentException {
        if (walletId == null) {
            throw new CampusPaymentException(ErrorMessages.INVALID_INPUT);
        }
        return transactionRepository.findByWalletId(walletId);
    }

    private TransactionType parseType(String txnType) throws CampusPaymentException {
        try {
            return TransactionType.valueOf(txnType.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new CampusPaymentException(
                    ErrorMessages.INVALID_INPUT + ": txn type must be one of "
                            + "DEPOSIT, WITHDRAW, TRANSFER, PAYMENT");
        }
    }
}
