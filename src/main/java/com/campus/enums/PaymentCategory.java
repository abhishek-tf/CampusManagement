package com.campus.enums;

public enum PaymentCategory {
    CANTEEN("Canteen & Food"),
    LIBRARY_FINE("Library Fine"),
    HOSTEL_FEE("Hostel Fee"),
    TUITION("Tuition Fee"),
    LAB_FEE("Lab Fee"),
    EXAM("Exam Fee"),
    SPORTS("Sports & Activities"),
    MISC("Miscellaneous");

    private final String description;

    PaymentCategory(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
