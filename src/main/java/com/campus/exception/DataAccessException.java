package com.campus.exception;

/**
 * Wraps low-level SQLExceptions thrown by the repository layer.
 *
 * WHY  : A DB failure (connection dropped, constraint error) is an
 *        infrastructure problem, NOT a business case — so it is unchecked
 *        and does not pollute every repository signature with `throws SQLException`.
 *        The original SQLException is preserved as the cause for diagnostics.
 * HOW  : Extends RuntimeException; the repository logs the technical detail
 *        via Logger and rethrows this with a user-neutral message
 *        (ErrorMessages.DATABASE_ERROR).
 * USED BY : StudentRepositoryImpl (raised on any SQLException),
 *           AppConfig / callers (propagated; surfaced as "Database error").
 */
public class DataAccessException extends RuntimeException {
    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
