package com.walletlogger.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.JTableHeader;

import com.walletlogger.anomaly.AnomalyConfig;
import com.walletlogger.anomaly.AnomalyDetectionEngine;
import com.walletlogger.anomaly.ProfileStore;
import com.walletlogger.dao.AnomalyDAO;
import com.walletlogger.dao.DatabaseConnectionPool;
import com.walletlogger.dao.SchemaInitialiser;
import com.walletlogger.dao.TransactionDAO;
import com.walletlogger.model.AnomalyAlert;
import com.walletlogger.model.Transaction;
import com.walletlogger.model.TransactionType;
import com.walletlogger.model.UserProfile;

/**
 * Week 3 deliverable: the Swing desktop UI for the wallet logger.
 *
 * Wires together everything built in Weeks 1–2: a background
 * {@link AnomalyDetectionEngine} consumes from a shared {@link BlockingQueue},
 * a {@link CsvIngestWorker} (a {@link SwingWorker}) feeds it without ever
 * freezing the window, and the engine's listener callbacks stream live
 * updates onto the three tabs (Live Feed / All Transactions / Anomaly Alerts)
 * via {@code SwingUtilities.invokeLater}, since those callbacks fire on the
 * engine's own background thread.
 */
public class MainFrame extends JFrame implements AnomalyDetectionEngine.AlertListener {

    private static final String DB_FILE_PATH = "wallet.db";
    private static final String PROFILE_STORE_PATH = "data/user_profiles.ser";
    private static final String SAMPLE_CSV_PATH = "src/main/resources/sample_transactions.csv";

    private BlockingQueue<Transaction> queue = new LinkedBlockingQueue<>();
    private TransactionDAO transactionDAO;
    private AnomalyDAO anomalyDAO;
    private AnomalyDetectionEngine engine;
    private Thread engineThread;

    private final TransactionTableModel liveFeedModel = new TransactionTableModel(300);
    private final TransactionTableModel allTransactionsModel = new TransactionTableModel();
    private final AlertTableModel alertModel = new AlertTableModel();

    private final JLabel statusDot = new JLabel("\u25CF");
    private final JLabel statusLabel = new JLabel("Ready.");
    private final StatCard processedCard = new StatCard("Processed", "0", Theme.PRIMARY);
    private final StatCard alertsCard = new StatCard("Alerts Raised", "0", Theme.DANGER);
    private final StatCard flaggedRateCard = new StatCard("Flagged Rate", "0%", Theme.WARNING);
    private final StatCard usersCard = new StatCard("Active Users", "0", Theme.SUCCESS);

    private int processedCount = 0;
    private int alertCount = 0;

    public MainFrame() throws Exception {
        super("Smart Wallet Transaction Logger — Real-Time Anomaly Detection");

        initializePipeline();

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                shutdownAndExit();
            }
        });

        getContentPane().setBackground(Theme.BACKGROUND);
        setJMenuBar(buildMenuBar());
        setLayout(new BorderLayout());

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Theme.BACKGROUND);
        top.add(buildHeaderPanel(), BorderLayout.NORTH);
        top.add(buildToolBar(), BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(Theme.FONT_TABLE_HEADER);
        tabs.addTab("  Live Feed  ", buildLiveFeedPanel());
        tabs.addTab("  All Transactions  ", buildAllTransactionsPanel());
        tabs.addTab("  Anomaly Alerts  ", buildAlertsPanel());

        JPanel center = new JPanel(new BorderLayout());
        center.setBorder(new EmptyBorder(0, 16, 16, 16));
        center.setBackground(Theme.BACKGROUND);
        center.add(tabs, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        add(buildStatusBar(), BorderLayout.SOUTH);

        setSize(1180, 720);
        setLocationRelativeTo(null);
    }

    private void initializePipeline() throws Exception {
        SchemaInitialiser.initialise();
        this.transactionDAO = new TransactionDAO();
        this.anomalyDAO = new AnomalyDAO();
        AnomalyConfig config = AnomalyConfig.loadDefault();
        Map<String, UserProfile> restoredProfiles = ProfileStore.load(PROFILE_STORE_PATH);

        this.queue = new LinkedBlockingQueue<>();
        this.engine = new AnomalyDetectionEngine(queue, transactionDAO, anomalyDAO, config, restoredProfiles);
        engine.addListener(this);
        this.engineThread = new Thread(engine, "anomaly-detection-engine");
        engineThread.setDaemon(true);
        engineThread.start();
    }

    // ── Header (branding + live dashboard cards) ─────────────────────────────

    private JPanel buildHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.PRIMARY);
        header.setBorder(new EmptyBorder(18, 24, 18, 24));

        JLabel title = new JLabel("Smart Wallet Transaction Logger");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("Real-time anomaly detection over a live transaction pipeline");
        subtitle.setFont(Theme.FONT_SUBTITLE);
        subtitle.setForeground(new Color(0xE0E7FF));

        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new java.awt.GridLayout(2, 1));
        titleBlock.setOpaque(false);
        titleBlock.add(title);
        titleBlock.add(subtitle);

        JPanel cards = new JPanel(new GridLayout(1, 4, 12, 0));
        cards.setOpaque(false);
        cards.add(processedCard);
        cards.add(alertsCard);
        cards.add(flaggedRateCard);
        cards.add(usersCard);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(14, 0, 0, 0));
        wrapper.add(cards, BorderLayout.CENTER);

        JPanel outer = new JPanel(new BorderLayout());
        outer.setOpaque(false);
        outer.add(titleBlock, BorderLayout.NORTH);
        outer.add(wrapper, BorderLayout.CENTER);

        header.add(outer, BorderLayout.CENTER);
        return header;
    }

    private void updateDashboardCards() {
        processedCard.setValue(String.valueOf(processedCount));
        alertsCard.setValue(String.valueOf(alertCount));
        int rate = processedCount == 0 ? 0 : (int) Math.round(100.0 * alertCount / processedCount);
        flaggedRateCard.setValue(rate + "%");
        usersCard.setValue(String.valueOf(engine.getProfiles().size()));
    }

    // ── Toolbar (primary actions) ─────────────────────────────────────────────

    private JToolBar buildToolBar() {
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        bar.setBackground(Theme.CARD_BACKGROUND);
        bar.setBorder(new EmptyBorder(10, 20, 10, 20));

        JButton loadSample = primaryButton("Load Sample CSV", Theme.PRIMARY);
        loadSample.addActionListener(e -> startCsvIngest(SAMPLE_CSV_PATH));

        JButton loadCustom = secondaryButton("Load CSV...");
        loadCustom.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser(".");
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                startCsvIngest(chooser.getSelectedFile().getAbsolutePath());
            }
        });

        JButton simulate = secondaryButton("Simulate Transaction");
        simulate.addActionListener(e -> simulateSingleTransaction());

        bar.add(loadSample);
        bar.add(javax.swing.Box.createHorizontalStrut(8));
        bar.add(loadCustom);
        bar.add(javax.swing.Box.createHorizontalStrut(8));
        bar.add(simulate);
        return bar;
    }

    private JButton primaryButton(String text, Color color) {
        JButton b = new JButton(text);
        b.setBackground(color);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setFont(Theme.FONT_TABLE_HEADER);
        b.setBorder(new EmptyBorder(8, 16, 8, 16));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JButton secondaryButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(Theme.CARD_BACKGROUND);
        b.setForeground(Theme.TEXT_PRIMARY);
        b.setFocusPainted(false);
        b.setFont(Theme.FONT_TABLE_HEADER);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER, 1, true),
                new EmptyBorder(7, 15, 7, 15)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    // ── Menu bar ──────────────────────────────────────────────────────────────

    private JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");

        JMenuItem loadSample = new JMenuItem("Load Sample CSV");
        loadSample.addActionListener(e -> startCsvIngest(SAMPLE_CSV_PATH));

        JMenuItem loadCustom = new JMenuItem("Load CSV...");
        loadCustom.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser(".");
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                startCsvIngest(chooser.getSelectedFile().getAbsolutePath());
            }
        });

        JMenuItem simulate = new JMenuItem("Simulate Single Transaction...");
        simulate.addActionListener(e -> simulateSingleTransaction());

        JMenuItem resetData = new JMenuItem("Reset All Data...");
        resetData.addActionListener(e -> resetAllData());

        JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(e -> shutdownAndExit());

        fileMenu.add(loadSample);
        fileMenu.add(loadCustom);
        fileMenu.addSeparator();
        fileMenu.add(simulate);
        fileMenu.addSeparator();
        fileMenu.add(resetData);
        fileMenu.addSeparator();
        fileMenu.add(exit);
        menuBar.add(fileMenu);
        return menuBar;
    }

    // ── Live Feed tab ─────────────────────────────────────────────────────────

    private JPanel buildLiveFeedPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Theme.CARD_BACKGROUND);
        panel.setBorder(BorderFactory.createLineBorder(Theme.BORDER));

        JTable table = new JTable(liveFeedModel);
        styleTable(table, true);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JLabel hint = new JLabel("  Streams transactions live as the anomaly engine processes them — use the toolbar above to feed the pipeline.");
        hint.setFont(Theme.FONT_LABEL);
        hint.setForeground(Theme.TEXT_MUTED);
        hint.setBorder(new EmptyBorder(8, 4, 8, 4));
        panel.add(hint, BorderLayout.NORTH);
        return panel;
    }

    // ── All Transactions tab ──────────────────────────────────────────────────

    private JPanel buildAllTransactionsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Theme.CARD_BACKGROUND);
        panel.setBorder(BorderFactory.createLineBorder(Theme.BORDER));

        JTextField userIdField = new JTextField(8);
        JTextField minAmountField = new JTextField(6);
        JTextField maxAmountField = new JTextField(6);
        JComboBox<String> typeBox = new JComboBox<>(new String[]{"ANY", "DEBIT", "CREDIT", "REFUND"});
        JCheckBox flaggedOnlyBox = new JCheckBox("Flagged only");
        JButton searchButton = secondaryButton("Search");
        JButton refreshButton = secondaryButton("Show All");

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        filterPanel.setBackground(Theme.CARD_BACKGROUND);
        filterPanel.add(filterLabel("User ID"));
        filterPanel.add(userIdField);
        filterPanel.add(filterLabel("Min ₹"));
        filterPanel.add(minAmountField);
        filterPanel.add(filterLabel("Max ₹"));
        filterPanel.add(maxAmountField);
        filterPanel.add(filterLabel("Type"));
        filterPanel.add(typeBox);
        filterPanel.add(flaggedOnlyBox);
        filterPanel.add(searchButton);
        filterPanel.add(refreshButton);

        JTable table = new JTable(allTransactionsModel);
        styleTable(table, true);

        searchButton.addActionListener(e -> {
            String userId = userIdField.getText().isBlank() ? null : userIdField.getText().trim();
            Double minAmount = parseOrNull(minAmountField.getText());
            Double maxAmount = parseOrNull(maxAmountField.getText());
            String typeStr = (String) typeBox.getSelectedItem();
            TransactionType type = "ANY".equals(typeStr) ? null : TransactionType.valueOf(typeStr);
            Boolean flaggedOnly = flaggedOnlyBox.isSelected() ? Boolean.TRUE : null;
            runFilteredQuery(userId, minAmount, maxAmount, type, flaggedOnly);
        });

        refreshButton.addActionListener(e -> runFilteredQuery(null, null, null, null, null));

        panel.add(filterPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JLabel filterLabel(String text) {
        JLabel label = new JLabel(text + ":");
        label.setFont(Theme.FONT_LABEL);
        label.setForeground(Theme.TEXT_MUTED);
        return label;
    }

    private Double parseOrNull(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Runs the filtered DB query off the EDT via a SwingWorker so the UI never freezes. */
    private void runFilteredQuery(String userId, Double minAmount, Double maxAmount,
                                   TransactionType type, Boolean flaggedOnly) {
        statusLabel.setText("Querying transactions...");
        new SwingWorker<List<Transaction>, Void>() {
            @Override
            protected List<Transaction> doInBackground() throws Exception {
                return transactionDAO.findWithFilters(userId, null, null, minAmount, maxAmount, type, flaggedOnly);
            }

            @Override
            protected void done() {
                try {
                    List<Transaction> results = get();
                    allTransactionsModel.setAll(results);
                    statusLabel.setText("Loaded " + results.size() + " transactions.");
                } catch (Exception ex) {
                    statusLabel.setText("Query failed: " + ex.getMessage());
                }
            }
        }.execute();
    }

    // ── Anomaly Alerts tab ────────────────────────────────────────────────────

    private JPanel buildAlertsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Theme.CARD_BACKGROUND);
        panel.setBorder(BorderFactory.createLineBorder(Theme.BORDER));

        JTextField userIdField = new JTextField(10);
        JButton searchButton = secondaryButton("Search by User");
        JButton refreshButton = secondaryButton("Show All");

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        filterPanel.setBackground(Theme.CARD_BACKGROUND);
        filterPanel.add(filterLabel("User ID"));
        filterPanel.add(userIdField);
        filterPanel.add(searchButton);
        filterPanel.add(refreshButton);

        JTable table = new JTable(alertModel);
        styleTable(table, false);
        table.getColumnModel().getColumn(TableRenderers.ALERT_SEVERITY_COLUMN)
                .setCellRenderer(new TableRenderers.SeverityBadgeRenderer());
        table.getColumnModel().getColumn(TableRenderers.ALERT_SEVERITY_COLUMN).setPreferredWidth(120);

        searchButton.addActionListener(e -> {
            String userId = userIdField.getText().trim();
            runAlertQuery(userId.isBlank() ? null : userId);
        });
        refreshButton.addActionListener(e -> runAlertQuery(null));

        panel.add(filterPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private void runAlertQuery(String userId) {
        statusLabel.setText("Querying anomaly alerts...");
        new SwingWorker<List<AnomalyAlert>, Void>() {
            @Override
            protected List<AnomalyAlert> doInBackground() throws Exception {
                return userId == null ? anomalyDAO.findAll() : anomalyDAO.findByUserId(userId);
            }

            @Override
            protected void done() {
                try {
                    List<AnomalyAlert> results = get();
                    alertModel.setAll(results);
                    statusLabel.setText("Loaded " + results.size() + " alerts.");
                } catch (Exception ex) {
                    statusLabel.setText("Query failed: " + ex.getMessage());
                }
            }
        }.execute();
    }

    // ── Shared table styling ──────────────────────────────────────────────────

    private void styleTable(JTable table, boolean isTransactionTable) {
        table.setRowHeight(28);
        table.setShowGrid(false);
        table.setIntercellSpacing(new java.awt.Dimension(0, 0));
        table.setSelectionBackground(Theme.PRIMARY_LIGHT);
        table.setSelectionForeground(Theme.PRIMARY_DARK);
        table.setFont(Theme.FONT_TABLE);

        JTableHeader header = table.getTableHeader();
        header.setFont(Theme.FONT_TABLE_HEADER);
        header.setBackground(Theme.BACKGROUND);
        header.setForeground(Theme.TEXT_MUTED);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));
        ((javax.swing.table.DefaultTableCellRenderer) header.getDefaultRenderer())
                .setHorizontalAlignment(SwingConstants.LEFT);

        if (isTransactionTable) {
            for (int i = 0; i < table.getColumnCount(); i++) {
                if (i == TableRenderers.TXN_FLAGGED_COLUMN) {
                    table.getColumnModel().getColumn(i).setCellRenderer(new TableRenderers.FlagBadgeRenderer());
                } else if (i == TableRenderers.TXN_AMOUNT_COLUMN) {
                    table.getColumnModel().getColumn(i).setCellRenderer(new TableRenderers.AmountRenderer());
                } else {
                    table.getColumnModel().getColumn(i).setCellRenderer(new TableRenderers.StripedRenderer());
                }
            }
        } else {
            for (int i = 0; i < table.getColumnCount(); i++) {
                if (i != TableRenderers.ALERT_SEVERITY_COLUMN) {
                    table.getColumnModel().getColumn(i).setCellRenderer(new TableRenderers.AlertStripedRenderer());
                }
            }
        }
    }

    // ── Status bar ────────────────────────────────────────────────────────────

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Theme.CARD_BACKGROUND);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER),
                new EmptyBorder(8, 20, 8, 20)));

        statusDot.setForeground(Theme.SUCCESS);
        statusLabel.setFont(Theme.FONT_LABEL);
        statusLabel.setForeground(Theme.TEXT_MUTED);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.setOpaque(false);
        left.add(statusDot);
        left.add(statusLabel);

        bar.add(left, BorderLayout.WEST);
        return bar;
    }

    // ── CSV ingestion (background, via SwingWorker) ──────────────────────────

    private void startCsvIngest(String path) {
        statusLabel.setText("Loading " + path + "...");
        CsvIngestWorker worker = new CsvIngestWorker(path, queue, 150, new CsvIngestWorker.StatusListener() {
            @Override
            public void onStatusUpdate(String message) {
                statusLabel.setText(message);
            }

            @Override
            public void onFinished(int totalSubmitted, Exception error) {
                if (error != null) {
                    statusLabel.setText("CSV load failed: " + error.getMessage());
                } else {
                    statusLabel.setText("Finished submitting " + totalSubmitted + " transactions from " + path);
                }
            }
        });
        worker.execute();
    }

    // ── Manual single-transaction simulation ─────────────────────────────────

    private void simulateSingleTransaction() {
        SimulateTransactionDialog dialog = new SimulateTransactionDialog(this);
        dialog.setVisible(true);
        Transaction t = dialog.getResult();
        if (t == null) return;

        queue.offer(t);
        statusLabel.setText("Submitted simulated transaction " + t.getTransactionId());
    }

    // ── Reset all data ────────────────────────────────────────────────────────

    private void resetAllData() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "This will permanently delete all stored transactions, anomaly alerts, and "
                        + "learned user profiles, then start fresh. This cannot be undone.\n\nContinue?",
                "Reset All Data", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        statusLabel.setText("Resetting all data...");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                engine.stop();
                engineThread.join(2000);
                DatabaseConnectionPool.shutdownAndReset();

                new File(DB_FILE_PATH).delete();
                File profileFile = new File(PROFILE_STORE_PATH);
                profileFile.delete();

                initializePipeline();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    liveFeedModel.clear();
                    allTransactionsModel.clear();
                    alertModel.clear();
                    processedCount = 0;
                    alertCount = 0;
                    updateDashboardCards();
                    statusLabel.setText("All data reset — starting fresh.");
                } catch (Exception ex) {
                    statusLabel.setText("Reset failed: " + ex.getMessage());
                }
            }
        }.execute();
    }

    // ── AnomalyDetectionEngine.AlertListener callbacks ────────────────────────
    // These fire on the engine's background thread, so every UI mutation is
    // marshalled onto the Event Dispatch Thread via invokeLater.

    @Override
    public void onAlertRaised(AnomalyAlert alert) {
        SwingUtilities.invokeLater(() -> {
            alertModel.addAlert(alert);
            alertCount++;
            updateDashboardCards();
        });
    }

    @Override
    public void onTransactionProcessed(Transaction transaction) {
        SwingUtilities.invokeLater(() -> {
            liveFeedModel.addTransaction(transaction);
            processedCount++;
            updateDashboardCards();
        });
    }

    // ── Shutdown ──────────────────────────────────────────────────────────────

    private void shutdownAndExit() {
        try {
            engine.stop();
            engineThread.join(2000);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        try {
            ProfileStore.save(engine.getProfiles(), PROFILE_STORE_PATH);
        } catch (Exception e) {
            System.err.println("Failed to save profile store: " + e.getMessage());
        }
        try {
            DatabaseConnectionPool.getInstance().shutdown();
        } catch (Exception ignored) {
            // best effort on shutdown
        }
        dispose();
        System.exit(0);
    }
}
