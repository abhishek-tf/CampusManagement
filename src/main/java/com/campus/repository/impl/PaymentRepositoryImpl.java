package com.campus.repository.impl;

import com.campus.config.AppConfig;
import com.campus.entity.CampusPayment;
import com.campus.enums.PaymentCategory;
import com.campus.exception.DataAccessException;
import com.campus.repository.interfaces.IPaymentRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC repository for the {@code campus_payment} table.
 *
 * <p>WHAT: Translates between CampusPayment objects and campus_payment rows using plain JDBC.
 * WHY:  Contains ONLY SQL — no business rules — so it has a single responsibility (persistence).
 * HOW:  Read methods open their own short-lived connection; the write method (insert) reuses
 *       the caller's transactional connection so payments stay atomic.</p>
 */
public class PaymentRepositoryImpl implements IPaymentRepository {

    // WHAT: The column list shared by the SELECTs.
    // WHY:  Declared once to keep the read queries consistent and avoid duplication (DRY).
    private static final String COLUMNS = "payment_id, txn_id, student_id, category, amount, paid_at";

    @Override
    public Optional<CampusPayment> findById(Long paymentId) {
        String sql = "SELECT " + COLUMNS + " FROM campus_payment WHERE payment_id = ?";
        // WHAT: try-with-resources for Connection + PreparedStatement.
        // WHY:  Guarantees both are closed even on exception, preventing connection leaks.
        // HOW:  AppConfig.getConnection() hands out a fresh JDBC connection from the configured URL.
        try (Connection conn = AppConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            // WHAT: Bind the id as a parameter rather than concatenating it into the SQL.
            // WHY:  PreparedStatement parameterisation prevents SQL injection and lets the driver
            //       reuse the parsed statement; chosen over Statement string-building for safety.
            ps.setLong(1, paymentId);
            try (ResultSet rs = ps.executeQuery()) {
                // WHAT: Return the single row if present, else Optional.empty().
                // WHY:  Optional makes "no such payment" explicit at the type level (no nulls).
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            // WHY: Wrap the checked SQLException in an unchecked DataAccessException so callers
            //      aren't forced to declare throws SQLException; the cause is preserved.
            throw new DataAccessException("Failed to find payment " + paymentId, e);
        }
    }

    @Override
    public List<CampusPayment> findByStudentId(String studentId) {
        // WHY ORDER BY paid_at DESC: payment history is shown most-recent-first to the user.
        String sql = "SELECT " + COLUMNS + " FROM campus_payment WHERE student_id = ? ORDER BY paid_at DESC";
        List<CampusPayment> list = new ArrayList<>();
        try (Connection conn = AppConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                // WHAT: Iterate the result set, mapping each row to an entity.
                // WHY:  rs.next() advances row-by-row; a while loop collects all matches.
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find payments for student " + studentId, e);
        }
    }

    @Override
    public List<CampusPayment> findAll() {
        String sql = "SELECT " + COLUMNS + " FROM campus_payment";
        List<CampusPayment> list = new ArrayList<>();
        try (Connection conn = AppConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to list payments", e);
        }
    }

    @Override
    public void insert(Connection conn, CampusPayment payment) {
        // WHAT: Insert only the writable columns; payment_id and paid_at are omitted.
        // WHY:  payment_id is AUTO_INCREMENT and paid_at has a DB default — letting the database
        //       own them keeps the entity and schema authoritative for generated values.
        String sql = "INSERT INTO campus_payment (txn_id, student_id, category, amount) VALUES (?, ?, ?, ?)";
        // WHAT: NOTE there is NO try-with-resources on the Connection here.
        // WHY:  This method uses the caller-supplied transactional connection; closing it would
        //       end the service's transaction prematurely. Only the PreparedStatement is closed.
        // HOW:  RETURN_GENERATED_KEYS lets us read back the new payment_id.
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, payment.getTxnId());
            ps.setString(2, payment.getStudentId());
            // WHY .name(): persist the enum's stable NAME, which matches the DB CHECK values.
            ps.setString(3, payment.getCategory().name());
            ps.setBigDecimal(4, payment.getAmount());
            ps.executeUpdate();
            // WHAT: Read the auto-generated payment_id back onto the entity.
            // WHY:  So the caller has the persisted identity without an extra SELECT.
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    payment.setPaymentId(keys.getLong(1));
                }
            }
        } catch (SQLException e) {
            // WHY: Surface as DataAccessException so the service's transaction catch can roll back.
            throw new DataAccessException("Failed to insert campus payment for student " + payment.getStudentId(), e);
        }
    }

    /**
     * WHAT: Builds a CampusPayment from the current ResultSet row.
     * WHY:  Centralises row->entity mapping so every query produces entities consistently (DRY).
     * HOW:  Reads each column by name; converts the SQL category string back to the enum and the
     *       SQL timestamp to LocalDateTime (null-safe).
     */
    private CampusPayment map(ResultSet rs) throws SQLException {
        Timestamp paidAt = rs.getTimestamp("paid_at");
        return CampusPayment.builder()
                .paymentId(rs.getLong("payment_id"))
                .txnId(rs.getLong("txn_id"))
                .studentId(rs.getString("student_id"))
                .category(PaymentCategory.valueOf(rs.getString("category")))
                .amount(rs.getBigDecimal("amount"))
                .paidAt(paidAt == null ? null : paidAt.toLocalDateTime())
                .build();
    }
}