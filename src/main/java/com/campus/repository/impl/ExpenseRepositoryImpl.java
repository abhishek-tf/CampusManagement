package com.campus.repository.impl;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.campus.entity.ExpenseGroup;
import com.campus.entity.GroupExpense;
import com.campus.entity.GroupMember;
import com.campus.enums.SplitType;
import com.campus.repository.interfaces.IExpenseRepository;

/**
 * JDBC implementation of {@link IExpenseRepository}. SQL only - all validation
 * and transaction control live in the service layer.
 */
public class ExpenseRepositoryImpl implements IExpenseRepository {

    // --- expense_group ----------------------------------------------------

    @Override
    public long saveGroup(Connection conn, ExpenseGroup group) throws SQLException {
        String sql = "INSERT INTO expense_group (group_name, created_by) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, group.getGroupName());
            ps.setString(2, group.getCreatedBy());
            ps.executeUpdate();
            return generatedId(ps);
        }
    }

    @Override
    public Optional<ExpenseGroup> findGroupById(Connection conn, long groupId) throws SQLException {
        String sql = "SELECT group_id, group_name, created_by, created_at "
                + "FROM expense_group WHERE group_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapGroup(rs)) : Optional.empty();
            }
        }
    }

    // --- group_member -----------------------------------------------------

    @Override
    public void addMember(Connection conn, GroupMember member) throws SQLException {
        String sql = "INSERT INTO group_member (group_id, student_id) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, member.getGroupId());
            ps.setString(2, member.getStudentId());
            ps.executeUpdate();
        }
    }

    @Override
    public boolean isMember(Connection conn, long groupId, String studentId) throws SQLException {
        String sql = "SELECT 1 FROM group_member WHERE group_id = ? AND student_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, groupId);
            ps.setString(2, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public List<GroupMember> findMembersByGroupId(Connection conn, long groupId) throws SQLException {
        String sql = "SELECT group_id, student_id, joined_at "
                + "FROM group_member WHERE group_id = ? ORDER BY joined_at";
        List<GroupMember> members = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    members.add(GroupMember.builder()
                            .groupId(rs.getLong("group_id"))
                            .studentId(rs.getString("student_id"))
                            .joinedAt(toLocalDateTime(rs.getTimestamp("joined_at")))
                            .build());
                }
            }
        }
        return members;
    }

    // --- group_expense ----------------------------------------------------

    @Override
    public long saveExpense(Connection conn, GroupExpense expense) throws SQLException {
        String sql = "INSERT INTO group_expense "
                + "(group_id, paid_by, description, total_amount, split_type) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, expense.getGroupId());
            ps.setString(2, expense.getPaidBy());
            ps.setString(3, expense.getDescription());
            ps.setBigDecimal(4, expense.getTotalAmount());
            ps.setString(5, expense.getSplitType().name());
            ps.executeUpdate();
            return generatedId(ps);
        }
    }

    @Override
    public Optional<GroupExpense> findExpenseById(Connection conn, long expenseId) throws SQLException {
        String sql = "SELECT expense_id, group_id, paid_by, description, total_amount, "
                + "split_type, created_at FROM group_expense WHERE expense_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, expenseId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapExpense(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public List<GroupExpense> findExpensesByGroupId(Connection conn, long groupId) throws SQLException {
        String sql = "SELECT expense_id, group_id, paid_by, description, total_amount, "
                + "split_type, created_at FROM group_expense WHERE group_id = ? ORDER BY expense_id";
        List<GroupExpense> expenses = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    expenses.add(mapExpense(rs));
                }
            }
        }
        return expenses;
    }

    // --- wallet / transaction (settlement support) ------------------------

    @Override
    public Optional<WalletRow> findWalletForUpdate(Connection conn, String studentId) throws SQLException {
        String sql = "SELECT wallet_id, balance FROM wallet WHERE student_id = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new WalletRow(rs.getLong("wallet_id"),
                            rs.getBigDecimal("balance")));
                }
                return Optional.empty();
            }
        }
    }

    @Override
    public long insertTransferTransaction(Connection conn, long fromWalletId,
                                          String fromStudentId, String toStudentId,
                                          BigDecimal amount) throws SQLException {
        String txnSql = "INSERT INTO transaction (wallet_id, txn_type, amount, status) "
                + "VALUES (?, 'TRANSFER', ?, 'SUCCESS')";
        long txnId;
        try (PreparedStatement ps = conn.prepareStatement(txnSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, fromWalletId);
            ps.setBigDecimal(2, amount);
            ps.executeUpdate();
            txnId = generatedId(ps);
        }

        String transferSql = "INSERT INTO transfer_transaction "
                + "(txn_id, from_student_id, to_student_id) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(transferSql)) {
            ps.setLong(1, txnId);
            ps.setString(2, fromStudentId);
            ps.setString(3, toStudentId);
            ps.executeUpdate();
        }
        return txnId;
    }

    @Override
    public void adjustWalletBalance(Connection conn, long walletId, BigDecimal delta) throws SQLException {
        String sql = "UPDATE wallet SET balance = balance + ? WHERE wallet_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, delta);
            ps.setLong(2, walletId);
            ps.executeUpdate();
        }
    }

    // --- row mappers / helpers -------------------------------------------

    private ExpenseGroup mapGroup(ResultSet rs) throws SQLException {
        return ExpenseGroup.builder()
                .groupId(rs.getLong("group_id"))
                .groupName(rs.getString("group_name"))
                .createdBy(rs.getString("created_by"))
                .createdAt(toLocalDateTime(rs.getTimestamp("created_at")))
                .build();
    }

    private GroupExpense mapExpense(ResultSet rs) throws SQLException {
        return GroupExpense.builder()
                .expenseId(rs.getLong("expense_id"))
                .groupId(rs.getLong("group_id"))
                .paidBy(rs.getString("paid_by"))
                .description(rs.getString("description"))
                .totalAmount(rs.getBigDecimal("total_amount"))
                .splitType(SplitType.valueOf(rs.getString("split_type")))
                .createdAt(toLocalDateTime(rs.getTimestamp("created_at")))
                .build();
    }

    private long generatedId(PreparedStatement ps) throws SQLException {
        try (ResultSet keys = ps.getGeneratedKeys()) {
            if (keys.next()) {
                return keys.getLong(1);
            }
            throw new SQLException("No generated key returned by insert");
        }
    }

    private java.time.LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
