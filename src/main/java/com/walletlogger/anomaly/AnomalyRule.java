package com.walletlogger.anomaly;

import java.util.Optional;

import com.walletlogger.model.Transaction;
import com.walletlogger.model.UserProfile;

/**
 * Strategy interface for a single anomaly-detection rule.
 *
 * Each rule inspects one incoming transaction against the user's running
 * profile and decides, in isolation, whether it looks anomalous. The
 * {@link AnomalyDetectionEngine} runs every registered rule against every
 * transaction and collects whatever fires — this keeps each rule small,
 * independently testable, and trivial to add to or remove from the pipeline.
 *
 * Implementations must be side-effect free with respect to the profile:
 * the engine updates {@link UserProfile} state itself, once, after all
 * rules have been evaluated for a given transaction.
 */
public interface AnomalyRule {

    /** Short, stable machine-readable code, e.g. "AMOUNT_SPIKE". Stored in anomaly_log. */
    String getCode();

    /**
     * Evaluates the rule.
     *
     * @param transaction the incoming transaction (profile has NOT yet been updated with it)
     * @param profile     the user's current running profile (pre-transaction state)
     * @param config      tunable thresholds
     * @return a description + severity if the rule fires, otherwise empty
     */
    Optional<Verdict> evaluate(Transaction transaction, UserProfile profile, AnomalyConfig config);

    /**
     * The outcome of a rule firing: a human-readable explanation and a
     * 1–5 severity score (5 = most urgent) used to order alerts in the
     * engine's output PriorityQueue.
     */
    record Verdict(String description, int severityScore) {}
}
