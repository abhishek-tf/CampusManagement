package com.campus.service.interfaces;

import com.campus.exception.CampusPaymentException;
import java.math.BigDecimal;

public interface IPaymentService {
    void processPayment(Long studentId, String category, BigDecimal amount) throws CampusPaymentException;
}
