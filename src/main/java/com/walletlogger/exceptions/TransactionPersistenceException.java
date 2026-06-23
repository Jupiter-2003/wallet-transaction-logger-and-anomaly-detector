package com.walletlogger.exceptions;

/**
 * Thrown when a JDBC operation (insert, update, query) on the
 * transactions or anomaly_log table fails.
 * Wraps the underlying SQLException so callers don't need to
 * import java.sql everywhere.
 */
public class TransactionPersistenceException extends Exception {

    public TransactionPersistenceException(String message) {
        super(message);
    }

    public TransactionPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
