package com.campus.exception;

public class CampusPaymentException extends Exception {
    private String errorCode;
    private long timestamp;

    public CampusPaymentException(String message) {
        super(message);
        this.timestamp = System.currentTimeMillis();
    }

    public CampusPaymentException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.timestamp = System.currentTimeMillis();
    }

    public CampusPaymentException(String message, Throwable cause) {
        super(message, cause);
        this.timestamp = System.currentTimeMillis();
    }

    public String getErrorCode() {
        return errorCode;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
