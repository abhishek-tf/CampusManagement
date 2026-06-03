package com.campus.repository.impl;

import com.campus.constants.ErrorMessages;
import com.campus.entity.Transaction;
import com.campus.repository.interfaces.ITransactionRepository;
import com.campus.util.DBConnection;
import com.campus.util.Logger;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC repository for the {@code transaction} table.
 *
 * <p>Pure data access only — no business rules. Each method opens a fresh
 * connection via {@link DBConnection} and closes it through try-with-resources.</p>
 */
public class TransactionRepositoryImpl implements ITransactionRepository {

    private static final String INSERT_SQL =
            "INSERT INTO transaction (wallet_id, txn_type, amount, status, failure_reason, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SELECT_BY_ID =
            "SELECT * FROM transaction WHERE txn_id = ?";

    private static final String SELECT_BY_WALLET =
            "SELECT * FROM transaction WHERE wallet_id = ? ORDER BY created_at DESC, txn_id DESC";

    private static final String SELECT_ALL =
            "SELECT * FROM transaction ORDER BY created_at DESC, txn_id DESC";

    @Override
    public void save(Transaction transaction) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, transaction.getWalletId());
            ps.setString(2, transaction.getTxnType());
            ps.setBigDecimal(3, BigDecimal.valueOf(transaction.getAmount()));
            ps.setString(4, transaction.getStatus());
            ps.setString(5, transaction.getFailureReason());
            ps.setTimestamp(6, Timestamp.valueOf(transaction.getCreatedAt()));
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    transaction.setTxnId(keys.getLong(1));
                }
            }
        } catch (SQLException e) {
            Logger.error(ErrorMessages.DATABASE_ERROR + " while saving transaction", e);
            throw new RuntimeException(ErrorMessages.DATABASE_ERROR, e);
        }
    }

    @Override
    public Optional<Transaction> findById(Long txnId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_ID)) {

            ps.setLong(1, txnId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            Logger.error(ErrorMessages.DATABASE_ERROR + " while finding transaction " + txnId, e);
            throw new RuntimeException(ErrorMessages.DATABASE_ERROR, e);
        }
    }

    @Override
    public List<Transaction> findByWalletId(Long walletId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_WALLET)) {

            ps.setLong(1, walletId);
            return mapRows(ps);
        } catch (SQLException e) {
            Logger.error(ErrorMessages.DATABASE_ERROR + " while listing wallet " + walletId, e);
            throw new RuntimeException(ErrorMessages.DATABASE_ERROR, e);
        }
    }

    @Override
    public List<Transaction> findAll() {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ALL)) {

            return mapRows(ps);
        } catch (SQLException e) {
            Logger.error(ErrorMessages.DATABASE_ERROR + " while listing transactions", e);
            throw new RuntimeException(ErrorMessages.DATABASE_ERROR, e);
        }
    }

    private List<Transaction> mapRows(PreparedStatement ps) throws SQLException {
        List<Transaction> transactions = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                transactions.add(mapRow(rs));
            }
        }
        return transactions;
    }

    private Transaction mapRow(ResultSet rs) throws SQLException {
        Transaction transaction = new Transaction();
        transaction.setTxnId(rs.getLong("txn_id"));
        transaction.setWalletId(rs.getLong("wallet_id"));
        transaction.setTxnType(rs.getString("txn_type"));
        transaction.setAmount(rs.getDouble("amount"));
        transaction.setStatus(rs.getString("status"));
        transaction.setFailureReason(rs.getString("failure_reason"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        transaction.setCreatedAt(createdAt != null ? createdAt.toLocalDateTime() : null);
        return transaction;
    }
}
