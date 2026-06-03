package com.campus.enums;

public enum TransactionType {
    DEPOSIT("Wallet Deposit"),
    WITHDRAW("Wallet Withdrawal"),
    TRANSFER("P2P Transfer"),
    PAYMENT("Campus Payment");

    private final String description;

    TransactionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
