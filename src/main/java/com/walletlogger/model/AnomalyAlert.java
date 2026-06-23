package com.walletlogger.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Represents an anomaly alert raised against a specific transaction.
 * Stored in the anomaly_log table via AnomalyDAO.
 */
public class AnomalyAlert implements Serializable, Comparable<AnomalyAlert> {

    private static final long serialVersionUID = 1L;

    private final String alertId;
    private final String transactionId;
    private final String userId;
    private final String flagCode;       // e.g. AMOUNT_SPIKE, HIGH_VELOCITY
    private final String description;   // human-readable explanation
    private final int severityScore;    // higher = more urgent; used by PriorityQueue
    private final LocalDateTime raisedAt;

    public AnomalyAlert(String alertId,
                        String transactionId,
                        String userId,
                        String flagCode,
                        String description,
                        int severityScore,
                        LocalDateTime raisedAt) {
        this.alertId       = alertId;
        this.transactionId = transactionId;
        this.userId        = userId;
        this.flagCode      = flagCode;
        this.description   = description;
        this.severityScore = severityScore;
        this.raisedAt      = raisedAt;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public String getAlertId()       { return alertId; }
    public String getTransactionId() { return transactionId; }
    public String getUserId()        { return userId; }
    public String getFlagCode()      { return flagCode; }
    public String getDescription()   { return description; }
    public int getSeverityScore()    { return severityScore; }
    public LocalDateTime getRaisedAt() { return raisedAt; }

    /**
     * Natural ordering by severity descending — so a PriorityQueue
     * always surfaces the most urgent alert first.
     */
    @Override
    public int compareTo(AnomalyAlert other) {
        return Integer.compare(other.severityScore, this.severityScore);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s on TXN %s — %s (severity: %d)",
                raisedAt, flagCode, transactionId, description, severityScore);
    }
}
