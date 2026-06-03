package com.campus.repository.interfaces;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import com.campus.entity.CampusPayment;

/**
 * Persistence operations for the {@code campus_payment} table.
 *
 * <p>Every method takes the active {@link Connection} so the service can record
 * the payment in the same transaction as the wallet debit and the parent
 * {@code transaction} row. Implementations perform SQL only.</p>
 */
public interface IPaymentRepository {

    /** Inserts a payment (its {@code txnId} must already be set) and returns the generated {@code payment_id}. */
    long save(Connection conn, CampusPayment payment) throws SQLException;

    Optional<CampusPayment> findById(Connection conn, long paymentId) throws SQLException;

    List<CampusPayment> findByStudentId(Connection conn, String studentId) throws SQLException;

    List<CampusPayment> findAll(Connection conn) throws SQLException;
}
