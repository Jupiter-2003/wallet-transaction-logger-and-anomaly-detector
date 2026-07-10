package com.walletlogger.anomaly;

import java.time.temporal.ChronoUnit;
import java.util.Optional;

import com.walletlogger.model.Transaction;
import com.walletlogger.model.UserProfile;

/**
 * Flags a large transaction that suddenly appears from a user who has
 * been inactive for a long stretch — a classic pattern for a dormant
 * account being taken over and drained.
 */
public class DormantSpikeRule implements AnomalyRule {

    @Override
    public String getCode() {
        return "DORMANT_SPIKE";
    }

    @Override
    public Optional<Verdict> evaluate(Transaction transaction, UserProfile profile, AnomalyConfig config) {
        if (profile.getLastTransactionTime() == null || profile.getCount() == 0) {
            return Optional.empty(); // no history yet — nothing to be "dormant" relative to
        }

        long daysSinceLast = ChronoUnit.DAYS.between(
                profile.getLastTransactionTime(), transaction.getTimestamp());

        if (daysSinceLast < config.getDormantDaysThreshold()) {
            return Optional.empty();
        }

        double comparisonBaseline = profile.getMean() > 0 ? profile.getMean() : profile.getLastAmount();
        if (comparisonBaseline <= 0
                || transaction.getAmount() < comparisonBaseline * config.getDormantAmountMultiplier()) {
            return Optional.empty();
        }

        String description = String.format(
                "₹%.2f after %d dormant days (last activity avg ₹%.2f)",
                transaction.getAmount(), daysSinceLast, comparisonBaseline);

        return Optional.of(new Verdict(description, 5));
    }
}
