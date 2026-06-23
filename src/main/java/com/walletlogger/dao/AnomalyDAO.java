package com.walletlogger.dao;

import com.walletlogger.exceptions.TransactionPersistenceException;
import com.walletlogger.model.AnomalyAlert;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of GenericDAO for AnomalyAlert objects.
 */
public class AnomalyDAO implements GenericDAO<AnomalyAlert, String> {

    private final DatabaseConnectionPool pool;

    public AnomalyDAO() throws SQLException {
        this.pool = DatabaseConnectionPool.getInstance();
    }

    // ── INSERT ────────────────────────────────────────────────────────────────

    @Override
    public void insert(AnomalyAlert alert) throws TransactionPersistenceException {
        String sql = """
            INSERT INTO anomaly_log
                (alert_id, transaction_id, user_id, flag_code, description, severity_score, raised_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        Connection conn = null;
        try {
            conn = pool.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, alert.getAlertId());
                ps.setString(2, alert.getTransactionId());
                ps.setString(3, alert.getUserId());
                ps.setString(4, alert.getFlagCode());
                ps.setString(5, alert.getDescription());
                ps.setInt   (6, alert.getSeverityScore());
                ps.setString(7, alert.getRaisedAt().toString());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new TransactionPersistenceException(
                    "Failed to insert anomaly alert: " + alert.getAlertId(), e);
        } finally {
            pool.releaseConnection(conn);
        }
    }

    // ── FIND BY ID ────────────────────────────────────────────────────────────

    @Override
    public Optional<AnomalyAlert> findById(String id) throws TransactionPersistenceException {
        String sql = "SELECT * FROM anomaly_log WHERE alert_id = ?";

        Connection conn = null;
        try {
            conn = pool.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new TransactionPersistenceException(
                    "Failed to find alert by id: " + id, e);
        } finally {
            pool.releaseConnection(conn);
        }
        return Optional.empty();
    }

    // ── FIND ALL ──────────────────────────────────────────────────────────────

    @Override
    public List<AnomalyAlert> findAll() throws TransactionPersistenceException {
        String sql = "SELECT * FROM anomaly_log ORDER BY raised_at DESC";
        List<AnomalyAlert> results = new ArrayList<>();

        Connection conn = null;
        try {
            conn = pool.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new TransactionPersistenceException("Failed to fetch all alerts", e);
        } finally {
            pool.releaseConnection(conn);
        }
        return results;
    }

    // ── FIND BY USER ──────────────────────────────────────────────────────────

    public List<AnomalyAlert> findByUserId(String userId) throws TransactionPersistenceException {
        String sql = "SELECT * FROM anomaly_log WHERE user_id = ? ORDER BY raised_at DESC";
        List<AnomalyAlert> results = new ArrayList<>();

        Connection conn = null;
        try {
            conn = pool.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) results.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new TransactionPersistenceException(
                    "Failed to fetch alerts for user: " + userId, e);
        } finally {
            pool.releaseConnection(conn);
        }
        return results;
    }

    // ── UPDATE / DELETE ───────────────────────────────────────────────────────

    @Override
    public void update(AnomalyAlert alert) throws TransactionPersistenceException {
        // Alerts are immutable once raised — nothing to update
        throw new UnsupportedOperationException("Anomaly alerts cannot be updated.");
    }

    @Override
    public void delete(String id) throws TransactionPersistenceException {
        String sql = "DELETE FROM anomaly_log WHERE alert_id = ?";

        Connection conn = null;
        try {
            conn = pool.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, id);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new TransactionPersistenceException(
                    "Failed to delete alert: " + id, e);
        } finally {
            pool.releaseConnection(conn);
        }
    }

    // ── ROW MAPPER ────────────────────────────────────────────────────────────

    private AnomalyAlert mapRow(ResultSet rs) throws SQLException {
        return new AnomalyAlert(
                rs.getString("alert_id"),
                rs.getString("transaction_id"),
                rs.getString("user_id"),
                rs.getString("flag_code"),
                rs.getString("description"),
                rs.getInt   ("severity_score"),
                LocalDateTime.parse(rs.getString("raised_at"))
        );
    }
}
