package com.campus.exception;

/**
 * Unchecked wrapper for low-level JDBC {@link java.sql.SQLException}s thrown by the
 * repository layer.
 *
 * <p>WHAT: A RuntimeException the repositories throw instead of leaking SQLException.
 * WHY:  An UNCHECKED type keeps SQL plumbing out of repository and service method
 *       signatures — repositories don't need {@code throws SQLException} on every method, and
 *       unrelated callers (Fraud, Wallet, Student services) don't have to declare it. The
 *       service layer still catches it where it manages a transaction, to translate it into a
 *       business exception (PaymentProcessingException) and trigger rollback. Using a
 *       business (checked) exception here would force throws-clauses across the whole codebase.
 * HOW:  Always constructed with the originating SQLException as the cause so the stack trace
 *       and SQL state are retained for diagnostics.</p>
 */
public class DataAccessException extends RuntimeException {
    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
