package com.walletlogger.gui;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import com.walletlogger.model.AnomalyAlert;

/**
 * Backs the "Anomaly Alerts" table. New alerts are inserted at the top,
 * most-severe-first is left to the caller (typically alerts already arrive
 * pre-sorted by severity from the engine's PriorityQueue drain).
 */
public class AlertTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {
            "Raised At", "Severity", "Flag", "Transaction ID", "User", "Description"
    };
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final List<AnomalyAlert> rows = new ArrayList<>();

    public void addAlert(AnomalyAlert alert) {
        rows.add(0, alert);
        fireTableDataChanged();
    }

    public void setAll(List<AnomalyAlert> alerts) {
        rows.clear();
        rows.addAll(alerts);
        fireTableDataChanged();
    }

    public void clear() {
        rows.clear();
        fireTableDataChanged();
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
        AnomalyAlert a = rows.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> a.getRaisedAt().format(TIME_FMT);
            case 1 -> a.getSeverityScore();
            case 2 -> a.getFlagCode();
            case 3 -> a.getTransactionId();
            case 4 -> a.getUserId();
            case 5 -> a.getDescription();
            default -> "";
        };
    }
}
