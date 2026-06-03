package com.campus.exception;

/**
 * Thrown when caller-supplied student data fails validation
 * (missing name, malformed email, blank department, etc.).
 *
 * WHY  : Replaces the previous generic CampusPaymentException("Invalid data")
 *        so bad input is reported with a specific, catchable type and code
 *        (CLAUDE.md: never use generic exceptions for business cases).
 * HOW  : Extends CampusPaymentException with the "INVALID_INPUT" code.
 * USED BY : StudentServiceImpl (validation guards),
 *           StudentMenu (caught -> shown to the user).
 */
public class InvalidInputException extends CampusPaymentException {
    public InvalidInputException(String message) {
        super(message, "INVALID_INPUT");
    }
}
