package com.walletlogger.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.walletlogger.exceptions.TransactionPersistenceException;
import com.walletlogger.model.CreditTransaction;
import com.walletlogger.model.DebitTransaction;
import com.walletlogger.model.RefundTransaction;
import com.walletlogger.model.Transaction;
import com.walletlogger.model.TransactionType;

/**
 * JDBC implementation of GenericDAO for Transaction objects.
 *
 * Key practices:
 *  - PreparedStatement for every query (no string concatenation = no SQL injection)
 *  - try-with-resources on every PreparedStatement so nothing leaks
 *  - Connection is borrowed from the pool and always released in finally
 *  - SQLExceptions are wrapped in TransactionPersistenceException so
 *    callers don't need to know about JDBC
 */
public class TransactionDAO implements GenericDAO<Transaction, String> {

    private final DatabaseConnectionPool pool;

    public TransactionDAO() throws SQLException {
        this.pool = DatabaseConnectionPool.getInstance();
    }

    // ── INSERT ────────────────────────────────────────────────────────────────

    @Override
    public void insert(Transaction t) throws TransactionPersistenceException {
        String sql = """
            INSERT INTO transactions
                (transaction_id, user_id, vendor_id, amount, timestamp, type, flagged, flag_reason)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        Connection conn = null;
        try {
            conn = pool.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, t.getTransactionId());
                ps.setString(2, t.getUserId());
                ps.setString(3, t.getVendorId());
                ps.setDouble(4, t.getAmount());
                ps.setString(5, t.getTimestamp().toString());
                ps.setString(6, t.getType().name());
                ps.setInt   (7, t.isFlagged() ? 1 : 0);
                ps.setString(8, t.getFlagReason());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new TransactionPersistenceException(
                    "Failed to insert transaction: " + t.getTransactionId(), e);
        } finally {
            pool.releaseConnection(conn);
        }
    }

    // ── FIND BY ID ────────────────────────────────────────────────────────────

    @Override
    public Optional<Transaction> findById(String id) throws TransactionPersistenceException {
        String sql = "SELECT * FROM transactions WHERE transaction_id = ?";

        Connection conn = null;
        try {
            conn = pool.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            throw new TransactionPersistenceException(
                    "Failed to find transaction by id: " + id, e);
        } finally {
            pool.releaseConnection(conn);
        }
        return Optional.empty();
    }

    // ── FIND ALL ──────────────────────────────────────────────────────────────

    @Override
    public List<Transaction> findAll() throws TransactionPersistenceException {
        String sql = "SELECT * FROM transactions ORDER BY timestamp DESC";
        List<Transaction> results = new ArrayList<>();

        Connection conn = null;
        try {
            conn = pool.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new TransactionPersistenceException("Failed to fetch all transactions", e);
        } finally {
            pool.releaseConnection(conn);
        }
        return results;
    }

    // ── FIND WITH FILTERS (for admin query panel) ─────────────────────────────

    /**
     * Flexible filtered query used by the admin GUI.
     * Any parameter can be null to mean "no filter on this field".
     */
    public List<Transaction> findWithFilters(String userId,
                                             LocalDateTime from,
                                             LocalDateTime to,
                                             Double minAmount,
                                             Double maxAmount,
                                             TransactionType type,
                                             Boolean flaggedOnly)
            throws TransactionPersistenceException {

        StringBuilder sql = new StringBuilder("SELECT * FROM transactions WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (userId != null && !userId.isBlank()) {
            sql.append(" AND user_id = ?");
            params.add(userId);
        }
        if (from != null) {
            sql.append(" AND timestamp >= ?");
            params.add(from.toString());
        }
        if (to != null) {
            sql.append(" AND timestamp <= ?");
            params.add(to.toString());
        }
        if (minAmount != null) {
            sql.append(" AND amount >= ?");
            params.add(minAmount);
        }
        if (maxAmount != null) {
            sql.append(" AND amount <= ?");
            params.add(maxAmount);
        }
        if (type != null) {
            sql.append(" AND type = ?");
            params.add(type.name());
        }
        if (Boolean.TRUE.equals(flaggedOnly)) {
            sql.append(" AND flagged = 1");
        }
        sql.append(" ORDER BY timestamp DESC");

        List<Transaction> results = new ArrayList<>();
        Connection conn = null;
        try {
            conn = pool.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                for (int i = 0; i < params.size(); i++) {
                    ps.setObject(i + 1, params.get(i));
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        results.add(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            throw new TransactionPersistenceException("Filtered query failed", e);
        } finally {
            pool.releaseConnection(conn);
        }
        return results;
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    @Override
    public void update(Transaction t) throws TransactionPersistenceException {
        String sql = """
            UPDATE transactions
               SET flagged = ?, flag_reason = ?
             WHERE transaction_id = ?
        """;

        Connection conn = null;
        try {
            conn = pool.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt   (1, t.isFlagged() ? 1 : 0);
                ps.setString(2, t.getFlagReason());
                ps.setString(3, t.getTransactionId());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new TransactionPersistenceException(
                    "Failed to update transaction: " + t.getTransactionId(), e);
        } finally {
            pool.releaseConnection(conn);
        }
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Override
    public void delete(String id) throws TransactionPersistenceException {
        String sql = "DELETE FROM transactions WHERE transaction_id = ?";

        Connection conn = null;
        try {
            conn = pool.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, id);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new TransactionPersistenceException(
                    "Failed to delete transaction: " + id, e);
        } finally {
            pool.releaseConnection(conn);
        }
    }

    // ── ROW MAPPER ────────────────────────────────────────────────────────────

    /**
     * Maps a ResultSet row to the appropriate Transaction subtype
     * based on the stored type column.
     */
    private Transaction mapRow(ResultSet rs) throws SQLException {
        String typeStr = rs.getString("type");
        TransactionType type = TransactionType.valueOf(typeStr);

        Transaction.Builder<?> builder = switch (type) {
            case DEBIT  -> new DebitTransaction.Builder();
            case CREDIT -> new CreditTransaction.Builder();
            case REFUND -> new RefundTransaction.Builder();
        };

        Transaction t = builder
                .transactionId(rs.getString("transaction_id"))
                .userId       (rs.getString("user_id"))
                .vendorId     (rs.getString("vendor_id"))
                .amount       (rs.getDouble("amount"))
                .timestamp    (LocalDateTime.parse(rs.getString("timestamp")))
                .type         (type)
                .build();

        t.setFlagged    (rs.getInt("flagged") == 1);
        t.setFlagReason (rs.getString("flag_reason"));
        return t;
    }
}
