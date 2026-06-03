package com.campus.repository.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.campus.entity.ExpenseSplits;
import com.campus.enums.SettlementStatus;
import com.campus.repository.interfaces.ISplitRepository;

/**
 * JDBC implementation of {@link ISplitRepository}. SQL only - all validation
 * and transaction control live in the service layer.
 */
public class SplitRepositoryImpl implements ISplitRepository {

    private static final String COLUMNS =
            "split_id, expense_id, debtor_id, share_amount, share_percent, "
            + "status, settled_txn_id, settled_at";

    @Override
    public void saveSplit(Connection conn, ExpenseSplits split) throws SQLException {
        String sql = "INSERT INTO expense_split "
                + "(expense_id, debtor_id, share_amount, share_percent, status, "
                + "settled_txn_id, settled_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, split.getExpenseId());
            ps.setString(2, split.getDebtorId());
            ps.setBigDecimal(3, split.getShareAmount());
            if (split.getSharePercent() != null) {
                ps.setBigDecimal(4, split.getSharePercent());
            } else {
                ps.setNull(4, java.sql.Types.DECIMAL);
            }
            ps.setString(5, split.getStatus().name());
            if (split.getSettledTxnId() != null) {
                ps.setLong(6, split.getSettledTxnId());
            } else {
                ps.setNull(6, java.sql.Types.BIGINT);
            }
            if (split.getSettledAt() != null) {
                ps.setTimestamp(7, Timestamp.valueOf(split.getSettledAt()));
            } else {
                ps.setNull(7, java.sql.Types.TIMESTAMP);
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    split.setSplitId(keys.getLong(1));
                }
            }
        }
    }

    @Override
    public Optional<ExpenseSplits> findById(Connection conn, long splitId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM expense_split WHERE split_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, splitId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapSplit(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public List<ExpenseSplits> findByExpenseId(Connection conn, long expenseId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM expense_split "
                + "WHERE expense_id = ? ORDER BY split_id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, expenseId);
            return mapList(ps);
        }
    }

    @Override
    public List<ExpenseSplits> findPendingByDebtor(Connection conn, String debtorId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM expense_split "
                + "WHERE debtor_id = ? AND status = 'PENDING' ORDER BY split_id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, debtorId);
            return mapList(ps);
        }
    }

    @Override
    public List<ExpenseSplits> findByGroupId(Connection conn, long groupId) throws SQLException {
        String sql = "SELECT es.split_id, es.expense_id, es.debtor_id, es.share_amount, "
                + "es.share_percent, es.status, es.settled_txn_id, es.settled_at "
                + "FROM expense_split es "
                + "JOIN group_expense ge ON es.expense_id = ge.expense_id "
                + "WHERE ge.group_id = ? ORDER BY es.split_id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, groupId);
            return mapList(ps);
        }
    }

    @Override
    public void markSettled(Connection conn, long splitId, long settledTxnId,
                            LocalDateTime settledAt) throws SQLException {
        String sql = "UPDATE expense_split "
                + "SET status = 'SETTLED', settled_txn_id = ?, settled_at = ? "
                + "WHERE split_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, settledTxnId);
            ps.setTimestamp(2, Timestamp.valueOf(settledAt));
            ps.setLong(3, splitId);
            ps.executeUpdate();
        }
    }

    // --- row mappers ------------------------------------------------------

    private List<ExpenseSplits> mapList(PreparedStatement ps) throws SQLException {
        List<ExpenseSplits> splits = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                splits.add(mapSplit(rs));
            }
        }
        return splits;
    }

    private ExpenseSplits mapSplit(ResultSet rs) throws SQLException {
        // Read settled_txn_id and capture wasNull() before touching any other
        // column, since wasNull() reflects only the most recent getter call.
        long settledTxnId = rs.getLong("settled_txn_id");
        Long settledTxnIdOrNull = rs.wasNull() ? null : settledTxnId;
        Timestamp settledAt = rs.getTimestamp("settled_at");
        return ExpenseSplits.builder()
                .splitId(rs.getLong("split_id"))
                .expenseId(rs.getLong("expense_id"))
                .debtorId(rs.getString("debtor_id"))
                .shareAmount(rs.getBigDecimal("share_amount"))
                .sharePercent(rs.getBigDecimal("share_percent"))
                .status(SettlementStatus.valueOf(rs.getString("status")))
                .settledTxnId(settledTxnIdOrNull)
                .settledAt(settledAt == null ? null : settledAt.toLocalDateTime())
                .build();
    }
}
