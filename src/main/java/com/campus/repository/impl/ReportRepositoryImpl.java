package com.campus.repository.impl;

import com.campus.constants.ErrorMessages;
import com.campus.dto.report.DepartmentSpendReport;
import com.campus.dto.report.MonthlySummary;
import com.campus.dto.report.SpenderReport;
import com.campus.repository.interfaces.IReportRepository;
import com.campus.util.DBConnection;
import com.campus.util.Logger;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC reporting repository. All aggregation happens in SQL via GROUP BY and
 * joins; "spend" excludes DEPOSIT (inflow) and counts only SUCCESS rows.
 */
public class ReportRepositoryImpl implements IReportRepository {

    /** Shared predicate so the spend definition lives in exactly one place. */
    private static final String SPEND_FILTER =
            " t.status = 'SUCCESS' AND t.txn_type IN ('PAYMENT', 'WITHDRAW', 'TRANSFER') ";

    private static final String TOTAL_SPEND_SQL =
            "SELECT COALESCE(SUM(t.amount), 0) AS total " +
            "FROM transaction t WHERE" + SPEND_FILTER;

    private static final String TOP_SPENDERS_SQL =
            "SELECT s.student_id, s.name, s.department, " +
            "       SUM(t.amount) AS total_spent, COUNT(*) AS txn_count " +
            "FROM transaction t " +
            "JOIN wallet w  ON t.wallet_id = w.wallet_id " +
            "JOIN student s ON w.student_id = s.student_id " +
            "WHERE" + SPEND_FILTER +
            "GROUP BY s.student_id, s.name, s.department " +
            "ORDER BY total_spent DESC " +
            "LIMIT ?";

    private static final String DEPARTMENT_SQL =
            "SELECT s.department, SUM(t.amount) AS total_spent, COUNT(*) AS txn_count " +
            "FROM transaction t " +
            "JOIN wallet w  ON t.wallet_id = w.wallet_id " +
            "JOIN student s ON w.student_id = s.student_id " +
            "WHERE" + SPEND_FILTER +
            "GROUP BY s.department " +
            "ORDER BY total_spent DESC";

    private static final String MONTHLY_SQL =
            "SELECT YEAR(t.created_at) AS yr, MONTH(t.created_at) AS mon, " +
            "       SUM(t.amount) AS total_spent, COUNT(*) AS txn_count " +
            "FROM transaction t " +
            "WHERE" + SPEND_FILTER +
            "GROUP BY YEAR(t.created_at), MONTH(t.created_at) " +
            "ORDER BY yr DESC, mon DESC";

    @Override
    public BigDecimal totalSpend() {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(TOTAL_SPEND_SQL);
             ResultSet rs = ps.executeQuery()) {

            return rs.next() ? rs.getBigDecimal("total") : BigDecimal.ZERO;
        } catch (SQLException e) {
            throw dbError("computing total spend", e);
        }
    }

    @Override
    public List<SpenderReport> topSpenders(int limit) {
        List<SpenderReport> reports = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(TOP_SPENDERS_SQL)) {

            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reports.add(new SpenderReport(
                            rs.getString("student_id"),
                            rs.getString("name"),
                            rs.getString("department"),
                            rs.getBigDecimal("total_spent"),
                            rs.getLong("txn_count")));
                }
            }
            return reports;
        } catch (SQLException e) {
            throw dbError("computing top spenders", e);
        }
    }

    @Override
    public List<DepartmentSpendReport> departmentWiseSpend() {
        List<DepartmentSpendReport> reports = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(DEPARTMENT_SQL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                reports.add(new DepartmentSpendReport(
                        rs.getString("department"),
                        rs.getBigDecimal("total_spent"),
                        rs.getLong("txn_count")));
            }
            return reports;
        } catch (SQLException e) {
            throw dbError("computing department-wise spend", e);
        }
    }

    @Override
    public List<MonthlySummary> monthlySummaries() {
        List<MonthlySummary> summaries = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(MONTHLY_SQL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                summaries.add(new MonthlySummary(
                        rs.getInt("yr"),
                        rs.getInt("mon"),
                        rs.getBigDecimal("total_spent"),
                        rs.getLong("txn_count")));
            }
            return summaries;
        } catch (SQLException e) {
            throw dbError("computing monthly summaries", e);
        }
    }

    private RuntimeException dbError(String action, SQLException e) {
        Logger.error(ErrorMessages.DATABASE_ERROR + " while " + action, e);
        return new RuntimeException(ErrorMessages.DATABASE_ERROR, e);
    }
}
