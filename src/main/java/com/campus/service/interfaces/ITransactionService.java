package com.campus.service.interfaces;

import com.campus.entity.Transaction;
import com.campus.exception.CampusPaymentException;
import java.math.BigDecimal;
import java.util.List;

public interface ITransactionService {

    /**
     * Records a money movement against a wallet (writes one transaction row).
     *
     * @param walletId the wallet the transaction belongs to
     * @param txnType  DEPOSIT | WITHDRAW | TRANSFER | PAYMENT
     * @param amount   positive amount
     * @return the saved transaction, with its generated id populated
     */
    Transaction recordTransaction(Long walletId, String txnType, BigDecimal amount)
            throws CampusPaymentException;

    /** Full transaction history for a wallet, newest first. */
    List<Transaction> getWalletHistory(Long walletId) throws CampusPaymentException;
}
