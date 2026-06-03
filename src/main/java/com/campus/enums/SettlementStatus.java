package com.campus.enums;

public enum SettlementStatus {
    PENDING("Settlement pending"),
    PARTIAL("Partially settled"),
    SETTLED("Fully settled"),
    DISPUTED("Under dispute"),
    CANCELLED("Cancelled");

    private final String description;

    SettlementStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
