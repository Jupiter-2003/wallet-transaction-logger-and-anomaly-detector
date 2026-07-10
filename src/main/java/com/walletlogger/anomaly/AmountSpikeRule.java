package com.walletlogger.anomaly;

import java.util.Optional;

import com.walletlogger.model.Transaction;
import com.walletlogger.model.UserProfile;

/**
 * Flags a transaction whose amount is an outlier relative to the user's
 * own historical spending, using a z-score computed from Welford's
 * running mean/variance (see {@link UserProfile}).
 *
 * Needs a minimum sample size first so a user's 2nd-ever transaction
 * doesn't get flagged just for being different from their 1st.
 */
public class AmountSpikeRule implements AnomalyRule {

    @Override
    public String getCode() {
        return "AMOUNT_SPIKE";
    }

    @Override
    public Optional<Verdict> evaluate(Transaction transaction, UserProfile profile, AnomalyConfig config) {
        if (profile.getCount() < config.getSpikeMinSamples()) {
            return Optional.empty();
        }

        double stdDev = profile.getStdDev();
        if (stdDev <= 0.0) {
            return Optional.empty(); // no variance to compare against yet
        }

        double zScore = (transaction.getAmount() - profile.getMean()) / stdDev;
        if (zScore <= config.getSpikeZScoreThreshold()) {
            return Optional.empty();
        }

        int severity = zScore >= config.getSpikeZScoreThreshold() * 2 ? 5 : 4;
        String description = String.format(
                "Amount ₹%.2f is %.1fσ above user's mean of ₹%.2f (stddev ₹%.2f)",
                transaction.getAmount(), zScore, profile.getMean(), stdDev);

        return Optional.of(new Verdict(description, severity));
    }
}
