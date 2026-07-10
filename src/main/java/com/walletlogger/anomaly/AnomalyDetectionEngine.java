package com.walletlogger.anomaly;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import com.walletlogger.dao.AnomalyDAO;
import com.walletlogger.dao.TransactionDAO;
import com.walletlogger.model.AnomalyAlert;
import com.walletlogger.model.DebitTransaction;
import com.walletlogger.model.Transaction;
import com.walletlogger.model.UserProfile;

/**
 * The heart of Week 2: a single-threaded consumer that drains a
 * {@link BlockingQueue} of incoming transactions, runs every registered
 * {@link AnomalyRule} against each one, persists both the transaction and
 * any resulting {@link AnomalyAlert}s, and keeps each user's
 * {@link UserProfile} statistics up to date.
 *
 * Designed to run on its own thread (e.g. {@code new Thread(engine).start()}
 * or wrapped by a GUI {@code SwingWorker}) while producers (CSV replay, a
 * live feed simulator, or a future REST endpoint) push transactions onto
 * the same queue from other threads. Shutdown is via the classic
 * "poison pill" pattern: pushing {@link #POISON_PILL} onto the queue tells
 * the engine to stop after processing everything queued ahead of it.
 */
public class AnomalyDetectionEngine implements Runnable {

    /** Sentinel value signalling "no more transactions are coming". */
    public static final Transaction POISON_PILL = new DebitTransaction.Builder()
            .transactionId("__POISON_PILL__")
            .userId("__SYSTEM__")
            .vendorId("__SYSTEM__")
            .amount(0.0)
            .timestamp(LocalDateTime.MIN)
            .build();

    private final BlockingQueue<Transaction> inputQueue;
    private final TransactionDAO transactionDAO;
    private final AnomalyDAO anomalyDAO;
    private final AnomalyConfig config;
    private final Map<String, UserProfile> profiles;
    private final List<AnomalyRule> rules;
    private final List<AlertListener> listeners = new CopyOnWriteArrayList<>();

    private volatile boolean running = true;

    public AnomalyDetectionEngine(BlockingQueue<Transaction> inputQueue,
                                   TransactionDAO transactionDAO,
                                   AnomalyDAO anomalyDAO,
                                   AnomalyConfig config) {
        this(inputQueue, transactionDAO, anomalyDAO, config, new ConcurrentHashMap<>());
    }

    public AnomalyDetectionEngine(BlockingQueue<Transaction> inputQueue,
                                   TransactionDAO transactionDAO,
                                   AnomalyDAO anomalyDAO,
                                   AnomalyConfig config,
                                   Map<String, UserProfile> initialProfiles) {
        this.inputQueue     = inputQueue;
        this.transactionDAO = transactionDAO;
        this.anomalyDAO     = anomalyDAO;
        this.config         = config;
        this.profiles       = initialProfiles;
        this.rules = List.of(
                new AmountSpikeRule(),
                new HighVelocityRule(),
                new DormantSpikeRule(),
                new DuplicateAttemptRule(),
                new UnusualHourRule()
        );
    }

    // ── Listener registration (used by the Swing GUI to react live) ─────────────

    public void addListener(AlertListener listener) {
        listeners.add(listener);
    }

    public void removeListener(AlertListener listener) {
        listeners.remove(listener);
    }

    /** Callback interface for observers (typically the GUI) that want live updates. */
    public interface AlertListener {
        void onAlertRaised(AnomalyAlert alert);
        default void onTransactionProcessed(Transaction transaction) {}
    }

    // ── Main consumer loop ───────────────────────────────────────────────────────

    @Override
    public void run() {
        while (running) {
            try {
                Transaction t = inputQueue.take();
                if (t == POISON_PILL || "__POISON_PILL__".equals(t.getTransactionId())) {
                    break;
                }
                process(t);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /** Requests a graceful stop; safe to call from another thread. */
    public void stop() {
        running = false;
        inputQueue.offer(POISON_PILL);
    }

    // ── Per-transaction processing ───────────────────────────────────────────────

    /**
     * Evaluates all rules against one transaction, persists the outcome,
     * and updates the user's running profile. Package-visible (not private)
     * so unit tests can exercise it synchronously without needing a live
     * background thread.
     */
    void process(Transaction t) {
        UserProfile profile = profiles.computeIfAbsent(t.getUserId(), UserProfile::new);

        PriorityQueue<AnomalyAlert> firedAlerts = new PriorityQueue<>();
        for (AnomalyRule rule : rules) {
            Optional<AnomalyRule.Verdict> verdict = rule.evaluate(t, profile, config);
            verdict.ifPresent(v -> firedAlerts.add(new AnomalyAlert(
                    UUID.randomUUID().toString(),
                    t.getTransactionId(),
                    t.getUserId(),
                    rule.getCode(),
                    v.description(),
                    v.severityScore(),
                    LocalDateTime.now()
            )));
        }

        if (!firedAlerts.isEmpty()) {
            t.setFlagged(true);
            t.setFlagReason(firedAlerts.stream()
                    .map(AnomalyAlert::getFlagCode)
                    .collect(Collectors.joining(",")));
        }

        try {
            transactionDAO.insert(t);
        } catch (Exception e) {
            System.err.println("Failed to persist transaction " + t.getTransactionId() + ": " + e.getMessage());
        }

        // Drain in severity order (highest first) — persist and notify listeners.
        while (!firedAlerts.isEmpty()) {
            AnomalyAlert alert = firedAlerts.poll();
            try {
                anomalyDAO.insert(alert);
            } catch (Exception e) {
                System.err.println("Failed to persist alert " + alert.getAlertId() + ": " + e.getMessage());
            }
            for (AlertListener l : listeners) {
                l.onAlertRaised(alert);
            }
        }

        // Profile is updated last, so rules always see PRE-transaction state.
        profile.update(t.getAmount(), t.getVendorId(), t.getTimestamp());

        for (AlertListener l : listeners) {
            l.onTransactionProcessed(t);
        }
    }

    /** Read-only view of current per-user profiles — used when saving via {@link ProfileStore}. */
    public Map<String, UserProfile> getProfiles() {
        return profiles;
    }
}
