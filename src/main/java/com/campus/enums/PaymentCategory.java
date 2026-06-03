package com.campus.enums;

/**
 * Campus payment categories.
 *
 * <p>These map 1:1 to the {@code campus_payment.category} CHECK constraint in
 * schema.sql (the source of truth) and to the categories named in the challenge
 * brief: canteen, library fine, hackathon fee, workshop fee, hostel fee.</p>
 */
public enum PaymentCategory {
    CANTEEN("Canteen & Food"),
    LIBRARY_FINE("Library Fine"),
    HACKATHON_FEE("Hackathon Fee"),
    WORKSHOP_FEE("Workshop Fee"),
    HOSTEL_FEE("Hostel Fee");

    private final String description;

    PaymentCategory(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
