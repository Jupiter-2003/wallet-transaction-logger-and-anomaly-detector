package com.walletlogger.exceptions;

/**
 * Thrown when a Transaction object fails validation —
 * e.g. negative amount, null userId, unrecognised vendorId.
 */
public class InvalidTransactionException extends Exception {

    public InvalidTransactionException(String message) {
        super(message);
    }

    public InvalidTransactionException(String message, Throwable cause) {
        super(message, cause);
    }
}
