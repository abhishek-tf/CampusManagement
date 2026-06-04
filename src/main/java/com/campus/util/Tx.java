package com.campus.util;

import java.sql.Connection;
import java.sql.SQLException;

import com.campus.constants.ErrorMessages;
import com.campus.exception.CampusPaymentException;

/**
 * Runs a unit of work inside a single JDBC transaction.
 *
 * <p>WHAT: opens a connection, turns auto-commit off, executes the work, then
 * commits on success or rolls back on any failure (atomic unit of work).
 * WHY:  centralises the commit/rollback/close boilerplate so every service does
 *       not repeat it (DRY) and transaction management is demonstrably correct
 *       in one place. Business failures ({@link CampusPaymentException}) propagate
 *       after rollback; raw {@link SQLException}s are logged and wrapped so the
 *       data-access concern never leaks into business signatures.</p>
 */
public final class Tx {

    private Tx() {
    }

    /** A unit of work that runs against the supplied (transaction-enlisted) connection. */
    @FunctionalInterface
    public interface Work<T> {
        T run(Connection connection) throws SQLException, CampusPaymentException;
    }

    /**
     * Executes {@code work} in one transaction.
     *
     * @return whatever the work returns (use {@code null} for void work)
     * @throws CampusPaymentException on a business failure or a wrapped SQL error
     */
    public static <T> T inTransaction(Work<T> work) throws CampusPaymentException {
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                T result = work.run(connection);
                connection.commit();
                return result;
            } catch (SQLException | CampusPaymentException | RuntimeException e) {
                connection.rollback();
                Logger.rollback("Transaction rolled back: " + e.getMessage());
                throw e;
            }
        } catch (CampusPaymentException e) {
            throw e;
        } catch (SQLException e) {
            Logger.error(ErrorMessages.DATABASE_ERROR, e);
            throw new CampusPaymentException(ErrorMessages.DATABASE_ERROR, e);
        }
    }
}
