package com.walletlogger.anomaly;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.walletlogger.exceptions.AnomalyConfigException;
import com.walletlogger.model.DebitTransaction;
import com.walletlogger.model.Transaction;
import com.walletlogger.model.UserProfile;

class AnomalyRulesTest {

    private AnomalyConfig config;

    @BeforeEach
    void setup() throws AnomalyConfigException {
        config = AnomalyConfig.loadDefault();
    }

    private Transaction debit(String txnId, String userId, String vendorId, double amount, LocalDateTime ts) {
        return new DebitTransaction.Builder()
                .transactionId(txnId).userId(userId).vendorId(vendorId)
                .amount(amount).timestamp(ts).build();
    }

    // ── AMOUNT_SPIKE ──────────────────────────────────────────────────────────

    @Test
    void amountSpikeFiresOnLargeOutlier() {
        UserProfile profile = new UserProfile("U01");
        LocalDateTime base = LocalDateTime.of(2026, 6, 1, 12, 0);
        // Build up a stable history around ~100
        for (int i = 0; i < 10; i++) {
            profile.update(100.0 + (i % 2), "V01", base.plusMinutes(i));
        }

        Transaction spike = debit("TXN_SPIKE", "U01", "V01", 100000.0, base.plusMinutes(11));
        Optional<AnomalyRule.Verdict> verdict = new AmountSpikeRule().evaluate(spike, profile, config);

        assertTrue(verdict.isPresent());
    }

    @Test
    void amountSpikeDoesNotFireWithoutEnoughHistory() {
        UserProfile profile = new UserProfile("U02");
        profile.update(50.0, "V01", LocalDateTime.now());

        Transaction t = debit("TXN1", "U02", "V01", 5000.0, LocalDateTime.now());
        Optional<AnomalyRule.Verdict> verdict = new AmountSpikeRule().evaluate(t, profile, config);

        assertFalse(verdict.isPresent());
    }

    // ── HIGH_VELOCITY ─────────────────────────────────────────────────────────

    @Test
    void highVelocityFiresOnRapidBurst() {
        UserProfile profile = new UserProfile("U03");
        LocalDateTime base = LocalDateTime.of(2026, 6, 1, 12, 0, 0);
        profile.update(100.0, "V01", base);
        profile.update(100.0, "V01", base.plusSeconds(5));
        profile.update(100.0, "V01", base.plusSeconds(10));

        Transaction burstTxn = debit("TXN_BURST", "U03", "V01", 100.0, base.plusSeconds(15));
        Optional<AnomalyRule.Verdict> verdict = new HighVelocityRule().evaluate(burstTxn, profile, config);

        assertTrue(verdict.isPresent());
    }

    @Test
    void highVelocityDoesNotFireForSpreadOutTransactions() {
        UserProfile profile = new UserProfile("U04");
        LocalDateTime base = LocalDateTime.of(2026, 6, 1, 12, 0, 0);
        profile.update(100.0, "V01", base);

        Transaction t = debit("TXN2", "U04", "V01", 100.0, base.plusHours(2));
        Optional<AnomalyRule.Verdict> verdict = new HighVelocityRule().evaluate(t, profile, config);

        assertFalse(verdict.isPresent());
    }

    // ── DORMANT_SPIKE ─────────────────────────────────────────────────────────

    @Test
    void dormantSpikeFiresAfterLongSilenceWithBigAmount() {
        UserProfile profile = new UserProfile("U05");
        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 10, 0);
        for (int i = 0; i < 5; i++) {
            profile.update(200.0, "V01", base.plusDays(i));
        }

        Transaction bigLateTxn = debit("TXN_DORMANT", "U05", "V01", 5000.0, base.plusDays(60));
        Optional<AnomalyRule.Verdict> verdict = new DormantSpikeRule().evaluate(bigLateTxn, profile, config);

        assertTrue(verdict.isPresent());
    }

    @Test
    void dormantSpikeDoesNotFireForFreshUser() {
        UserProfile profile = new UserProfile("U06");
        Transaction t = debit("TXN3", "U06", "V01", 5000.0, LocalDateTime.now());
        Optional<AnomalyRule.Verdict> verdict = new DormantSpikeRule().evaluate(t, profile, config);
        assertFalse(verdict.isPresent());
    }

    // ── DUPLICATE_ATTEMPT ─────────────────────────────────────────────────────

    @Test
    void duplicateAttemptFiresOnQuickRepeat() {
        UserProfile profile = new UserProfile("U07");
        LocalDateTime base = LocalDateTime.of(2026, 6, 1, 12, 0, 0);
        profile.update(499.0, "V02", base);

        Transaction repeat = debit("TXN_DUP", "U07", "V02", 499.0, base.plusSeconds(10));
        Optional<AnomalyRule.Verdict> verdict = new DuplicateAttemptRule().evaluate(repeat, profile, config);

        assertTrue(verdict.isPresent());
    }

    @Test
    void duplicateAttemptDoesNotFireForDifferentVendor() {
        UserProfile profile = new UserProfile("U08");
        LocalDateTime base = LocalDateTime.of(2026, 6, 1, 12, 0, 0);
        profile.update(499.0, "V02", base);

        Transaction different = debit("TXN4", "U08", "V03", 499.0, base.plusSeconds(10));
        Optional<AnomalyRule.Verdict> verdict = new DuplicateAttemptRule().evaluate(different, profile, config);

        assertFalse(verdict.isPresent());
    }

    // ── UNUSUAL_HOUR ──────────────────────────────────────────────────────────

    @Test
    void unusualHourFiresLateAtNight() {
        UserProfile profile = new UserProfile("U09");
        Transaction lateNight = debit("TXN5", "U09", "V01", 100.0,
                LocalDateTime.of(2026, 6, 1, 2, 30));

        Optional<AnomalyRule.Verdict> verdict = new UnusualHourRule().evaluate(lateNight, profile, config);
        assertTrue(verdict.isPresent());
    }

    @Test
    void unusualHourDoesNotFireDuringTheDay() {
        UserProfile profile = new UserProfile("U10");
        Transaction daytime = debit("TXN6", "U10", "V01", 100.0,
                LocalDateTime.of(2026, 6, 1, 14, 0));

        Optional<AnomalyRule.Verdict> verdict = new UnusualHourRule().evaluate(daytime, profile, config);
        assertFalse(verdict.isPresent());
    }
}
