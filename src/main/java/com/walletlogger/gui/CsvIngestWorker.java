package com.walletlogger.gui;

import java.util.List;
import java.util.concurrent.BlockingQueue;

import javax.swing.SwingWorker;

import com.walletlogger.model.Transaction;
import com.walletlogger.util.CsvLoader;

/**
 * Loads a CSV file off the Event Dispatch Thread and feeds each transaction
 * into the shared {@link BlockingQueue} at a configurable pace, so the GUI
 * stays responsive (no frozen window) while the file is read and the "live
 * feed" is simulated. Status text is streamed back to the EDT via
 * {@link #publish} so the label updates progressively rather than jumping
 * straight to "done".
 *
 * Deliberately does NOT push the engine's poison pill when finished — the
 * anomaly engine keeps running so the user can load another file, or use
 * "Simulate one transaction", without restarting anything.
 */
public class CsvIngestWorker extends SwingWorker<Integer, String> {

    private final String csvPath;
    private final BlockingQueue<Transaction> outputQueue;
    private final long delayMillisBetweenItems;
    private final StatusListener statusListener;

    public interface StatusListener {
        void onStatusUpdate(String message);
        void onFinished(int totalSubmitted, Exception error);
    }

    public CsvIngestWorker(String csvPath,
                            BlockingQueue<Transaction> outputQueue,
                            long delayMillisBetweenItems,
                            StatusListener statusListener) {
        this.csvPath = csvPath;
        this.outputQueue = outputQueue;
        this.delayMillisBetweenItems = delayMillisBetweenItems;
        this.statusListener = statusListener;
    }

    @Override
    protected Integer doInBackground() throws Exception {
        List<Transaction> transactions = CsvLoader.load(csvPath);
        publish("Loaded " + transactions.size() + " rows from " + csvPath + ", streaming into pipeline...");

        int submitted = 0;
        for (Transaction t : transactions) {
            outputQueue.put(t);
            submitted++;
            if (submitted % 5 == 0 || submitted == transactions.size()) {
                publish("Submitted " + submitted + "/" + transactions.size() + " transactions...");
            }
            if (delayMillisBetweenItems > 0) {
                Thread.sleep(delayMillisBetweenItems);
            }
        }
        return submitted;
    }

    @Override
    protected void process(List<String> chunks) {
        if (!chunks.isEmpty() && statusListener != null) {
            statusListener.onStatusUpdate(chunks.get(chunks.size() - 1));
        }
    }

    @Override
    protected void done() {
        if (statusListener == null) return;
        try {
            int total = get();
            statusListener.onFinished(total, null);
        } catch (Exception e) {
            statusListener.onFinished(0, e);
        }
    }
}
