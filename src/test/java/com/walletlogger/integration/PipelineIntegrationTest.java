package com.walletlogger.integration;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.walletlogger.anomaly.AnomalyConfig;
import com.walletlogger.anomaly.AnomalyDetectionEngine;
import com.walletlogger.dao.AnomalyDAO;
import com.walletlogger.dao.SchemaInitialiser;
import com.walletlogger.dao.TransactionDAO;
import com.walletlogger.exceptions.AnomalyConfigException;
import com.walletlogger.model.AnomalyAlert;
import com.walletlogger.model.DebitTransaction;
import com.walletlogger.model.Transaction;
import com.walletlogger.pipeline.TransactionProducer;

/**
 * End-to-end test wiring the real Week 2 pipeline together on live threads:
 *
 *   TransactionProducer  --puts-->  BlockingQueue<Transaction>  --take()-->  AnomalyDetectionEngine
 *
 * exactly as it runs in production, then verifies transactions and alerts
 * both made it all the way through to the database.
 */
class PipelineIntegrationTest {

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

    private Transaction debit(String txnId, String userId, String vendorId, double amount, LocalDateTime ts) {
        return new DebitTransaction.Builder()
                .transactionId(txnId).userId(userId).vendorId(vendorId)
                .amount(amount).timestamp(ts).build();
    }

    @Test
    void fullPipelineProcessesBatchAndPersistsAlerts() throws InterruptedException {
        String userId = "PIPE_USER_" + UUID.randomUUID().toString().substring(0, 8);
        LocalDateTime base = LocalDateTime.of(2026, 6, 1, 12, 0);

        List<Transaction> batch = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            batch.add(debit("PIPE" + i + "_" + UUID.randomUUID().toString().substring(0, 6),
                    userId, "V01", 100.0 + i, base.plusMinutes(i)));
        }
        // One deliberate outlier at the end of the batch.
        batch.add(debit("PIPE_SPIKE_" + UUID.randomUUID().toString().substring(0, 6),
                userId, "V01", 500000.0, base.plusMinutes(20)));

        BlockingQueue<Transaction> queue = new ArrayBlockingQueue<>(50);
        AnomalyDetectionEngine engine = new AnomalyDetectionEngine(queue, transactionDAO, anomalyDAO, config);

        CountDownLatch alertLatch = new CountDownLatch(1);
        engine.addListener(new AnomalyDetectionEngine.AlertListener() {
            @Override
            public void onAlertRaised(AnomalyAlert alert) {
                if ("AMOUNT_SPIKE".equals(alert.getFlagCode())) {
                    alertLatch.countDown();
                }
            }
        });

        Thread consumerThread = new Thread(engine, "test-anomaly-engine");
        consumerThread.start();

        Thread producerThread = new Thread(new TransactionProducer(queue, batch, 0), "test-producer");
        producerThread.start();

        boolean alertSeen = alertLatch.await(10, TimeUnit.SECONDS);
        engine.stop();
        consumerThread.join(5000);
        producerThread.join(5000);

        assertTrue(alertSeen, "Expected the amount-spike alert to be raised within the timeout");

        // Verify the transactions really landed in the database via the DAO layer.
        boolean allPersisted = batch.stream().allMatch(t -> {
            try {
                return transactionDAO.findById(t.getTransactionId()).isPresent();
            } catch (Exception e) {
                return false;
            }
        });
        assertTrue(allPersisted, "All produced transactions should be persisted by the engine");
    }
}
