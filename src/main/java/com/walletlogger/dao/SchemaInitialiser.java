package com.walletlogger.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Creates the database tables on first run if they don't exist yet.
 * Call SchemaInitialiser.initialise() once at application startup
 * before any DAO is used.
 */
public class SchemaInitialiser {

    private SchemaInitialiser() {}   // utility class — no instances

    public static void initialise() throws SQLException {
        DatabaseConnectionPool pool = DatabaseConnectionPool.getInstance();
        Connection conn = pool.getConnection();

        try (Statement stmt = conn.createStatement()) {

            // ── transactions table ────────────────────────────────────────────
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS transactions (
                    transaction_id TEXT PRIMARY KEY,
                    user_id        TEXT NOT NULL,
                    vendor_id      TEXT NOT NULL,
                    amount         REAL NOT NULL,
                    timestamp      TEXT NOT NULL,
                    type           TEXT NOT NULL,
                    flagged        INTEGER NOT NULL DEFAULT 0,
                    flag_reason    TEXT
                )
            """);

            // ── anomaly_log table ─────────────────────────────────────────────
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS anomaly_log (
                    alert_id        TEXT PRIMARY KEY,
                    transaction_id  TEXT NOT NULL,
                    user_id         TEXT NOT NULL,
                    flag_code       TEXT NOT NULL,
                    description     TEXT NOT NULL,
                    severity_score  INTEGER NOT NULL,
                    raised_at       TEXT NOT NULL,
                    FOREIGN KEY (transaction_id) REFERENCES transactions(transaction_id)
                )
            """);

            System.out.println("Schema initialised successfully.");

        } finally {
            pool.releaseConnection(conn);
        }
    }
}
