package com.walletlogger.anomaly;

import java.util.Optional;

import com.walletlogger.model.Transaction;
import com.walletlogger.model.UserProfile;

/**
 * Flags a transaction that occurs outside normal waking hours
 * (configurable, default 06:00–23:00). Low severity by itself —
 * mostly useful in combination with other rules — but cheap to check
 * and a common signal in real fraud-detection systems.
 */
public class UnusualHourRule implements AnomalyRule {

    @Override
    public String getCode() {
        return "UNUSUAL_HOUR";
    }

    @Override
    public Optional<Verdict> evaluate(Transaction transaction, UserProfile profile, AnomalyConfig config) {
        int hour = transaction.getTimestamp().getHour();
        boolean withinNormalHours = hour >= config.getUnusualHourStart() && hour < config.getUnusualHourEnd();

        if (withinNormalHours) {
            return Optional.empty();
        }

        String description = String.format(
                "Transaction at %02d:00, outside normal hours [%02d:00–%02d:00)",
                hour, config.getUnusualHourStart(), config.getUnusualHourEnd());

        return Optional.of(new Verdict(description, 2));
    }
}
