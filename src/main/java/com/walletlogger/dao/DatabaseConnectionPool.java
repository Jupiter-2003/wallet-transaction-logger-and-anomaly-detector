package com.walletlogger.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Thread-safe Singleton connection pool for SQLite.
 *
 * Why Singleton? We want exactly one pool shared across all DAOs
 * so we don't accidentally open hundreds of connections.
 *
 * Why BlockingQueue? If all connections are busy, a requesting
 * thread simply waits rather than crashing — safe under concurrent load.
 *
 * Switch to PostgreSQL later by changing DB_URL and loading
 * the postgres driver instead (zero other changes needed).
 */
public class DatabaseConnectionPool {

    private static final String DB_URL    = "jdbc:sqlite:wallet.db";
    private static final int    POOL_SIZE = 5;

    // The single instance — volatile ensures visibility across threads
    private static volatile DatabaseConnectionPool instance;

    private final BlockingQueue<Connection> pool;

    // ── Private constructor (Singleton) ───────────────────────────────────────

    private DatabaseConnectionPool() throws SQLException {
        pool = new ArrayBlockingQueue<>(POOL_SIZE);
        for (int i = 0; i < POOL_SIZE; i++) {
            pool.offer(createConnection());
        }
    }

    private Connection createConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL);
        conn.setAutoCommit(true);
        return conn;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Double-checked locking — safe and efficient for multithreaded init.
     */
    public static DatabaseConnectionPool getInstance() throws SQLException {
        if (instance == null) {
            synchronized (DatabaseConnectionPool.class) {
                if (instance == null) {
                    instance = new DatabaseConnectionPool();
                }
            }
        }
        return instance;
    }

    /**
     * Borrow a connection from the pool.
     * Blocks if all connections are currently in use.
     */
    public Connection getConnection() throws SQLException {
        try {
            return pool.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrupted while waiting for a DB connection", e);
        }
    }

    /**
     * Return a connection back to the pool after use.
     * DAOs call this in their finally blocks (or via try-with-resources wrapper).
     */
    public void releaseConnection(Connection conn) {
        if (conn != null) {
            pool.offer(conn);
        }
    }

    /**
     * Shut down the pool cleanly — call this on application exit.
     */
    public void shutdown() {
        for (Connection conn : pool) {
            try {
                conn.close();
            } catch (SQLException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
        pool.clear();
    }
}
