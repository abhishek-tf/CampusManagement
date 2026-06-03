package com.campus.exception;

public class InvalidAmountException extends CampusPaymentException {
    public InvalidAmountException(String message) {
        super(message, "INVALID_AMOUNT");
    }
}
