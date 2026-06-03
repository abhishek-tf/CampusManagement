package com.campus.repository.impl;

import com.campus.constants.ErrorMessages;
import com.campus.dto.report.SpendRecord;
import com.campus.repository.interfaces.IReportRepository;
import com.campus.util.DBConnection;
import com.campus.util.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC reporting repository. Performs only the join (transaction -> wallet ->
 * student) and returns raw spend rows; the service does the Stream aggregation.
 * "Spend" excludes DEPOSIT (inflow) and counts only SUCCESS rows.
 */
public class ReportRepositoryImpl implements IReportRepository {

    private static final String SPEND_RECORDS_SQL =
            "SELECT s.student_id, s.name, s.department, t.amount, t.created_at " +
            "FROM transaction t " +
            "JOIN wallet w  ON t.wallet_id = w.wallet_id " +
            "JOIN student s ON w.student_id = s.student_id " +
            "WHERE t.status = 'SUCCESS' " +
            "  AND t.txn_type IN ('PAYMENT', 'WITHDRAW', 'TRANSFER')";

    @Override
    public List<SpendRecord> findSpendRecords() {
        List<SpendRecord> records = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SPEND_RECORDS_SQL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Timestamp createdAt = rs.getTimestamp("created_at");
                records.add(new SpendRecord(
                        rs.getString("student_id"),
                        rs.getString("name"),
                        rs.getString("department"),
                        rs.getBigDecimal("amount"),
                        createdAt != null ? createdAt.toLocalDateTime() : null));
            }
            return records;
        } catch (SQLException e) {
            Logger.error(ErrorMessages.DATABASE_ERROR + " while loading spend records", e);
            throw new RuntimeException(ErrorMessages.DATABASE_ERROR, e);
        }
    }
}
