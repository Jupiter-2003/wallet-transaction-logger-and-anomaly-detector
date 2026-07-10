package com.walletlogger.gui;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import com.walletlogger.model.Transaction;

/**
 * Backs both the "Live Feed" and "All Transactions" tables. Newly arriving
 * transactions are inserted at the top so the most recent activity is
 * always visible without scrolling.
 */
public class TransactionTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {
            "Time", "Transaction ID", "User", "Vendor", "Type", "Amount (₹)", "Flagged", "Reason"
    };
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final List<Transaction> rows = new ArrayList<>();
    private final int maxRows;

    public TransactionTableModel() {
        this(Integer.MAX_VALUE);
    }

    /** @param maxRows caps memory/UI growth for a long-running live feed; 0 or negative means unbounded. */
    public TransactionTableModel(int maxRows) {
        this.maxRows = maxRows <= 0 ? Integer.MAX_VALUE : maxRows;
    }

    public void addTransaction(Transaction t) {
        rows.add(0, t);
        while (rows.size() > maxRows) {
            rows.remove(rows.size() - 1);
        }
        fireTableDataChanged();
    }

    public void setAll(List<Transaction> transactions) {
        rows.clear();
        rows.addAll(transactions);
        fireTableDataChanged();
    }

    public void clear() {
        rows.clear();
        fireTableDataChanged();
    }

    public Transaction getTransactionAt(int row) {
        return rows.get(row);
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Transaction t = rows.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> t.getTimestamp().format(TIME_FMT);
            case 1 -> t.getTransactionId();
            case 2 -> t.getUserId();
            case 3 -> t.getVendorId();
            case 4 -> t.getType().name();
            case 5 -> String.format("%.2f", t.getAmount());
            case 6 -> t.isFlagged() ? "YES" : "";
            case 7 -> t.getFlagReason() == null ? "" : t.getFlagReason();
            default -> "";
        };
    }
}
