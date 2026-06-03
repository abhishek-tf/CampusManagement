package com.campus.exception;

/**
 * Thrown when registering a student whose email already exists.
 *
 * WHY  : The `student` table has UNIQUE(email); a duplicate is a business
 *        rule violation, not an infrastructure failure, so it gets its own
 *        checked exception instead of a generic CampusPaymentException.
 * HOW  : Extends CampusPaymentException and stamps a stable error code so
 *        callers/logs can distinguish it without string matching.
 * USED BY : StudentServiceImpl.registerStudent (raised),
 *           StudentMenu (caught -> shown to the user).
 */
public class DuplicateStudentException extends CampusPaymentException {
    public DuplicateStudentException(String message) {
        super(message, "DUPLICATE_STUDENT");
    }
}
