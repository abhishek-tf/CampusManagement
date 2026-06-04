package com.campus.enums;

public enum TransactionStatus {
    PENDING("Transaction pending"),
    SUCCESS("Transaction successful"),
    FAILED("Transaction failed");

    private final String description;

    TransactionStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
