package com.campus.service.interfaces;

import com.campus.exception.CampusPaymentException;
import java.math.BigDecimal;

public interface IWalletService {
    void topupWallet(Long studentId, BigDecimal amount) throws CampusPaymentException;
    void withdrawFromWallet(Long studentId, BigDecimal amount) throws CampusPaymentException;
    void transferMoney(Long fromStudentId, Long toStudentId, BigDecimal amount) throws CampusPaymentException;
    BigDecimal getBalance(Long studentId) throws CampusPaymentException;
}
