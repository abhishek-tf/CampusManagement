package com.campus.exception;

public class DailyLimitExceededException extends CampusPaymentException {
    public DailyLimitExceededException(String message) {
        super(message, "DAILY_LIMIT_EXCEEDED");
    }
}
