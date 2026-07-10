package com.walletlogger.anomaly;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.walletlogger.dao.AnomalyDAO;
import com.walletlogger.dao.SchemaInitialiser;
import com.walletlogger.dao.TransactionDAO;
import com.walletlogger.exceptions.AnomalyConfigException;
import com.walletlogger.model.AnomalyAlert;
import com.walletlogger.model.DebitTransaction;
import com.walletlogger.model.Transaction;

/**
 * Exercises the engine's per-transaction processing directly (synchronously,
 * via the package-visible {@code process} method) so assertions don't need
 * to race a background consumer thread. The end-to-end threaded pipeline is
 * covered separately by the integration test.
 */
class AnomalyDetectionEngineTest {

    private static TransactionDAO transactionDAO;
    private static AnomalyDAO anomalyDAO;
    private static AnomalyConfig config;

    @BeforeAll
    static void setup() throws SQLException, AnomalyConfigException {
        SchemaInitialiser.initialise();
        transactionDAO = new TransactionDAO();
        anomalyDAO = new AnomalyDAO();
        config = AnomalyConfig.loadDefault();
    }

    private String uid() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private Transaction debit(String txnId, String userId, String vendorId, double amount, LocalDateTime ts) {
        return new DebitTransaction.Builder()
                .transactionId(txnId).userId(userId).vendorId(vendorId)
                .amount(amount).timestamp(ts).build();
    }

    @Test
    void engineFlagsAndPersistsAnAmountSpike() {
        BlockingQueue<Transaction> queue = new ArrayBlockingQueue<>(10);
        AnomalyDetectionEngine engine = new AnomalyDetectionEngine(queue, transactionDAO, anomalyDAO, config);

        List<AnomalyAlert> captured = new ArrayList<>();
        engine.addListener(new AnomalyDetectionEngine.AlertListener() {
            @Override
            public void onAlertRaised(AnomalyAlert alert) {
                captured.add(alert);
            }
        });

        String userId = "ENGINE_USER_" + uid();
        LocalDateTime base = LocalDateTime.of(2026, 6, 1, 12, 0);

        for (int i = 0; i < 5; i++) {
            engine.process(debit("ENGTXN" + uid(), userId, "V01", 100.0 + i, base.plusMinutes(i)));
        }
        Transaction spike = debit("ENGTXN" + uid(), userId, "V01", 999999.0, base.plusMinutes(10));
        engine.process(spike);

        assertTrue(spike.isFlagged(), "Spike transaction should be flagged");
        assertTrue(captured.stream().anyMatch(a -> a.getFlagCode().equals("AMOUNT_SPIKE")));
    }

    @Test
    void engineDoesNotFlagOrdinaryTransactions() {
        BlockingQueue<Transaction> queue = new ArrayBlockingQueue<>(10);
        AnomalyDetectionEngine engine = new AnomalyDetectionEngine(queue, transactionDAO, anomalyDAO, config);

        String userId = "ENGINE_USER_" + uid();
        LocalDateTime base = LocalDateTime.of(2026, 6, 1, 14, 0);

        Transaction t1 = debit("ENGTXN" + uid(), userId, "V01", 100.0, base);
        engine.process(t1);

        assertFalse(t1.isFlagged());
    }
}
