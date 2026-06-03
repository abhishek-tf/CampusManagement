package com.campus.repository.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.campus.entity.CampusPayment;
import com.campus.enums.PaymentCategory;
import com.campus.repository.interfaces.IPaymentRepository;

/**
 * JDBC implementation of {@link IPaymentRepository} against the MySQL
 * {@code campus_payment} table. SQL only - validation and transaction control
 * live in the service layer.
 */
public class PaymentRepositoryImpl implements IPaymentRepository {

    private static final String COLUMNS =
            "payment_id, txn_id, student_id, category, amount, paid_at";

    @Override
    public long save(Connection conn, CampusPayment payment) throws SQLException {
        String sql = "INSERT INTO campus_payment (txn_id, student_id, category, amount) "
                + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, payment.getTxnId());
            ps.setString(2, payment.getStudentId());
            ps.setString(3, payment.getCategory().name());
            ps.setBigDecimal(4, payment.getAmount());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    payment.setPaymentId(id);
                    return id;
                }
                throw new SQLException("No generated payment_id returned");
            }
        }
    }

    @Override
    public Optional<CampusPayment> findById(Connection conn, long paymentId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM campus_payment WHERE payment_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, paymentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public List<CampusPayment> findByStudentId(Connection conn, String studentId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM campus_payment "
                + "WHERE student_id = ? ORDER BY paid_at DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);
            return mapList(ps);
        }
    }

    @Override
    public List<CampusPayment> findAll(Connection conn) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM campus_payment ORDER BY payment_id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            return mapList(ps);
        }
    }

    // --- row mappers ------------------------------------------------------

    private List<CampusPayment> mapList(PreparedStatement ps) throws SQLException {
        List<CampusPayment> payments = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                payments.add(mapRow(rs));
            }
        }
        return payments;
    }

    private CampusPayment mapRow(ResultSet rs) throws SQLException {
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
