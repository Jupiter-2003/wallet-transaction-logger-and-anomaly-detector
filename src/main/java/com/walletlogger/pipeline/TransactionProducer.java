package com.walletlogger.pipeline;

import java.util.List;
import java.util.concurrent.BlockingQueue;

import com.walletlogger.anomaly.AnomalyDetectionEngine;
import com.walletlogger.model.Transaction;

/**
 * Producer side of the Week 2 BlockingQueue pipeline.
 *
 * Pushes a batch of transactions onto the shared queue, optionally with a
 * small delay between each one to simulate a real-time feed (useful for
 * watching the GUI update live rather than seeing the whole CSV land at
 * once). Always finishes by pushing the engine's poison pill so the
 * consumer thread can shut down cleanly once everything has been consumed.
 *
 * Intended to run on its own thread, e.g.:
 * <pre>
 *   new Thread(new TransactionProducer(queue, transactions, 200)).start();
 * </pre>
 */
public class TransactionProducer implements Runnable {

    private final BlockingQueue<Transaction> outputQueue;
    private final List<Transaction> transactions;
    private final long delayMillisBetweenItems;

    /** No delay — pushes everything as fast as the queue accepts it. */
    public TransactionProducer(BlockingQueue<Transaction> outputQueue, List<Transaction> transactions) {
        this(outputQueue, transactions, 0);
    }

    public TransactionProducer(BlockingQueue<Transaction> outputQueue,
                                List<Transaction> transactions,
                                long delayMillisBetweenItems) {
        this.outputQueue = outputQueue;
        this.transactions = transactions;
        this.delayMillisBetweenItems = delayMillisBetweenItems;
    }

    @Override
    public void run() {
        try {
            for (Transaction t : transactions) {
                outputQueue.put(t);
                if (delayMillisBetweenItems > 0) {
                    Thread.sleep(delayMillisBetweenItems);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            // Signal the consumer that no more items are coming.
            outputQueue.offer(AnomalyDetectionEngine.POISON_PILL);
        }
    }
}
