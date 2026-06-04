package com.campus.enums;

public enum SettlementStatus {
    PENDING("Settlement pending"),
    SETTLED("Fully settled");

    private final String description;

    SettlementStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
