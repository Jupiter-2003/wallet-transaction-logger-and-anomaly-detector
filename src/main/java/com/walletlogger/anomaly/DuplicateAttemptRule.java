package com.walletlogger.anomaly;

import java.time.temporal.ChronoUnit;
import java.util.Optional;

import com.walletlogger.model.Transaction;
import com.walletlogger.model.UserProfile;

/**
 * Flags a transaction that looks like an accidental or malicious replay:
 * same vendor, same amount, arriving very shortly after the previous one
 * from the same user (e.g. a double-tap "pay" button, or a retried
 * request that wasn't actually deduplicated upstream).
 */
public class DuplicateAttemptRule implements AnomalyRule {

    private static final double AMOUNT_EPSILON = 0.01;

    @Override
    public String getCode() {
        return "DUPLICATE_ATTEMPT";
    }

    @Override
    public Optional<Verdict> evaluate(Transaction transaction, UserProfile profile, AnomalyConfig config) {
        if (profile.getLastVendorId() == null || profile.getLastTransactionTime() == null) {
            return Optional.empty();
        }

        boolean sameVendor = profile.getLastVendorId().equals(transaction.getVendorId());
        boolean sameAmount = Math.abs(profile.getLastAmount() - transaction.getAmount()) < AMOUNT_EPSILON;

        if (!sameVendor || !sameAmount) {
            return Optional.empty();
        }

        long secondsSinceLast = ChronoUnit.SECONDS.between(
                profile.getLastTransactionTime(), transaction.getTimestamp());

        if (secondsSinceLast < 0 || secondsSinceLast > config.getDuplicateWindowSeconds()) {
            return Optional.empty();
        }

        String description = String.format(
                "Same vendor (%s) and amount (₹%.2f) as %ds earlier",
                transaction.getVendorId(), transaction.getAmount(), secondsSinceLast);

        return Optional.of(new Verdict(description, 3));
    }
}
