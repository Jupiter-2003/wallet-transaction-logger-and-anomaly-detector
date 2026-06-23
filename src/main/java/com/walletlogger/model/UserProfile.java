package com.walletlogger.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Holds per-user statistics maintained by the anomaly detector.
 * Updated after every transaction using Welford's online algorithm
 * so we always have a live running mean and standard deviation
 * without storing the full transaction history in memory.
 *
 * Serializable so the detector's entire state (a map of these)
 * can be saved to disk and restored on restart.
 */
public class UserProfile implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String userId;

    // Welford's algorithm state
    private int    count;       // number of transactions seen so far
    private double mean;        // running mean of transaction amounts
    private double M2;          // running sum of squared deviations (for variance)

    // For HIGH_VELOCITY and DORMANT_SPIKE rules
    private LocalDateTime lastTransactionTime;

    // For DUPLICATE_ATTEMPT rule
    private String lastVendorId;
    private double lastAmount;

    public UserProfile(String userId) {
        this.userId  = userId;
        this.count   = 0;
        this.mean    = 0.0;
        this.M2      = 0.0;
        this.lastTransactionTime = null;
        this.lastVendorId = null;
        this.lastAmount   = -1;
    }

    /**
     * Update running statistics with the new transaction amount.
     * Uses Welford's online algorithm — O(1) per update, numerically stable.
     */
    public void update(double amount, String vendorId, LocalDateTime timestamp) {
        count++;
        double delta = amount - mean;
        mean += delta / count;
        double delta2 = amount - mean;
        M2 += delta * delta2;

        this.lastAmount          = amount;
        this.lastVendorId        = vendorId;
        this.lastTransactionTime = timestamp;
    }

    /**
     * Population standard deviation derived from Welford state.
     * Returns 0 if fewer than 2 transactions have been seen.
     */
    public double getStdDev() {
        if (count < 2) return 0.0;
        return Math.sqrt(M2 / count);
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public String getUserId()                   { return userId; }
    public int getCount()                        { return count; }
    public double getMean()                      { return mean; }
    public LocalDateTime getLastTransactionTime(){ return lastTransactionTime; }
    public String getLastVendorId()              { return lastVendorId; }
    public double getLastAmount()                { return lastAmount; }

    @Override
    public String toString() {
        return String.format("UserProfile[%s | txns=%d | mean=%.2f | stddev=%.2f]",
                userId, count, mean, getStdDev());
    }
}
