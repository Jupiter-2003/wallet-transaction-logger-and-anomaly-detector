package com.walletlogger.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Base class representing a wallet transaction.
 * Uses the Builder inner class for clean object construction.
 * Serializable so transaction objects can be passed across threads safely.
 */
public abstract class Transaction implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String transactionId;
    private final String userId;
    private final String vendorId;
    private final double amount;
    private final LocalDateTime timestamp;
    private final TransactionType type;

    // Whether this transaction has been flagged by anomaly detection
    private boolean flagged;
    private String flagReason;

    // Protected constructor — only subclasses and the Builder call this
    protected Transaction(Builder<?> builder) {
        this.transactionId = builder.transactionId;
        this.userId        = builder.userId;
        this.vendorId      = builder.vendorId;
        this.amount        = builder.amount;
        this.timestamp     = builder.timestamp;
        this.type          = builder.type;
        this.flagged       = false;
        this.flagReason    = null;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public String getTransactionId() { return transactionId; }
    public String getUserId()        { return userId; }
    public String getVendorId()      { return vendorId; }
    public double getAmount()        { return amount; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public TransactionType getType() { return type; }
    public boolean isFlagged()       { return flagged; }
    public String getFlagReason()    { return flagReason; }

    public void setFlagged(boolean flagged)       { this.flagged = flagged; }
    public void setFlagReason(String flagReason)  { this.flagReason = flagReason; }

    @Override
    public String toString() {
        return String.format("[%s] %s | User: %s | Vendor: %s | ₹%.2f | %s | Flagged: %s",
                type, transactionId, userId, vendorId, amount, timestamp, flagged);
    }

    // ── Generic Builder ───────────────────────────────────────────────────────

    /**
     * Generic Builder using the "curiously recurring template pattern" (CRTP)
     * so subclass builders can chain fluently without casting.
     *
     * Usage:
     *   Transaction t = new DebitTransaction.Builder()
     *       .transactionId("TXN001")
     *       .userId("U01")
     *       ...
     *       .build();
     */
    @SuppressWarnings("unchecked")
    public static abstract class Builder<T extends Builder<T>> {

        private String transactionId;
        private String userId;
        private String vendorId;
        private double amount;
        private LocalDateTime timestamp;
        private TransactionType type;

        public T transactionId(String transactionId) {
            this.transactionId = transactionId;
            return (T) this;
        }

        public T userId(String userId) {
            this.userId = userId;
            return (T) this;
        }

        public T vendorId(String vendorId) {
            this.vendorId = vendorId;
            return (T) this;
        }

        public T amount(double amount) {
            this.amount = amount;
            return (T) this;
        }

        public T timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return (T) this;
        }

        public T type(TransactionType type) {
            this.type = type;
            return (T) this;
        }

        public abstract Transaction build();
    }
}
