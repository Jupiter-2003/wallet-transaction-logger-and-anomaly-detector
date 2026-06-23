package com.walletlogger.exceptions;

/**
 * Thrown when the anomaly detection config file is missing,
 * malformed, or contains invalid threshold values.
 */
public class AnomalyConfigException extends Exception {

    public AnomalyConfigException(String message) {
        super(message);
    }

    public AnomalyConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
