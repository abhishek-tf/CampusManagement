package com.campus.service.interfaces;

import com.campus.exception.CampusPaymentException;

public interface IFraudDetectionService {
    void detectAndFlagFraud(Long studentId) throws CampusPaymentException;
    boolean isFraudulent(Long studentId);
}
