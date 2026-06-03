package com.campus.exception;

public class InsufficientBalanceException extends CampusPaymentException {
    public InsufficientBalanceException(String message) {
        super(message, "INSUFFICIENT_BALANCE");
    }
}
