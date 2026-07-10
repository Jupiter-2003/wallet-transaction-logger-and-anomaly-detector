package com.walletlogger.anomaly;

import java.util.Optional;

import com.walletlogger.model.Transaction;
import com.walletlogger.model.UserProfile;

/**
 * Flags a transaction that arrives as part of an unusually rapid burst
 * of activity from the same user — e.g. a script hammering the wallet
 * API, or a stolen-credential fraud attempt trying many payments fast.
 */
public class HighVelocityRule implements AnomalyRule {

    @Override
    public String getCode() {
        return "HIGH_VELOCITY";
    }

    @Override
    public Optional<Verdict> evaluate(Transaction transaction, UserProfile profile, AnomalyConfig config) {
        long recentCount = profile.countRecentWithinSeconds(
                config.getVelocityWindowSeconds(), transaction.getTimestamp());

        // recentCount only reflects transactions BEFORE this one; +1 accounts for it.
        if (recentCount + 1 < config.getVelocityMaxCount()) {
            return Optional.empty();
        }

        String description = String.format(
                "%d transactions within %ds window (limit %d)",
                recentCount + 1, config.getVelocityWindowSeconds(), config.getVelocityMaxCount());

        return Optional.of(new Verdict(description, 4));
    }
}
