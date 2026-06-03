package com.campus.exception;

public class FraudDetectedException extends CampusPaymentException {
    public FraudDetectedException(String message) {
        super(message, "FRAUD_DETECTED");
    }
}
