package com.walletlogger.gui;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Cell renderers shared by the transaction and alert tables: zebra striping
 * with a tinted background for flagged rows, right-aligned currency amounts,
 * and colored "pill" badges for the flagged/severity columns.
 */
public final class TableRenderers {

    private TableRenderers() {}

    public static final int TXN_FLAGGED_COLUMN = 6;
    public static final int TXN_AMOUNT_COLUMN = 5;
    public static final int ALERT_SEVERITY_COLUMN = 1;

    /** Base striped renderer: alternating row colors, tinted red for flagged transactions. */
    public static class StripedRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                         boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setFont(Theme.FONT_TABLE);

            boolean flagged = false;
            if (table.getColumnCount() > TXN_FLAGGED_COLUMN) {
                Object flagVal = table.getModel().getValueAt(table.convertRowIndexToModel(row), TXN_FLAGGED_COLUMN);
                flagged = "YES".equals(flagVal);
            }

            if (!isSelected) {
                if (flagged) {
                    setBackground(row % 2 == 0 ? Theme.DANGER_LIGHT : new Color(0xFDE8E8));
                } else {
                    setBackground(row % 2 == 0 ? Color.WHITE : Theme.STRIPE);
                }
                setForeground(Theme.TEXT_PRIMARY);
            } else {
                setBackground(Theme.PRIMARY_LIGHT);
                setForeground(Theme.PRIMARY_DARK);
            }
            setHorizontalAlignment(SwingConstants.LEFT);
            return c;
        }
    }

    /** Right-aligned, monospaced-ish rendering for the amount column, still striped/tinted. */
    public static class AmountRenderer extends StripedRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                         boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(SwingConstants.RIGHT);
            setText("\u20B9 " + value);
            return c;
        }
    }

    /** Renders the transaction "Flagged" column as a red pill, or blank when not flagged. */
    public static class FlagBadgeRenderer extends DefaultTableCellRenderer {
        private final PillLabel pill = new PillLabel();

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                         boolean hasFocus, int row, int column) {
            boolean flagged = "YES".equals(value);
            Color rowBg = isSelected ? Theme.PRIMARY_LIGHT
                    : (flagged ? (row % 2 == 0 ? Theme.DANGER_LIGHT : new Color(0xFDE8E8))
                               : (row % 2 == 0 ? Color.WHITE : Theme.STRIPE));

            if (!flagged) {
                DefaultTableCellRenderer blank = new DefaultTableCellRenderer();
                blank.setText("");
                blank.setBackground(rowBg);
                return blank;
            }

            pill.setText("FLAGGED");
            pill.setColors(Theme.DANGER, Color.WHITE);
            pill.setOpaque(false);

            javax.swing.JPanel wrapper = new javax.swing.JPanel(new java.awt.BorderLayout());
            wrapper.setBackground(rowBg);
            wrapper.add(pill, java.awt.BorderLayout.CENTER);
            return wrapper;
        }
    }

    /** Renders the alert severity column (1–5) as a colored pill with a text label. */
    public static class SeverityBadgeRenderer extends DefaultTableCellRenderer {
        private final PillLabel pill = new PillLabel();

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                         boolean hasFocus, int row, int column) {
            int score = value instanceof Integer ? (Integer) value : Integer.parseInt(String.valueOf(value));
            String label = switch (score) {
                case 5 -> "CRITICAL";
                case 4 -> "HIGH";
                case 3 -> "MEDIUM";
                default -> "LOW";
            };

            pill.setText(label + " (" + score + ")");
            pill.setColors(Theme.severityLightColor(score), Theme.severityColor(score));
            pill.setOpaque(false);

            Color rowBg = isSelected ? Theme.PRIMARY_LIGHT : (row % 2 == 0 ? Color.WHITE : Theme.STRIPE);
            javax.swing.JPanel wrapper = new javax.swing.JPanel(new java.awt.BorderLayout());
            wrapper.setBackground(rowBg);
            wrapper.add(pill, java.awt.BorderLayout.CENTER);
            return wrapper;
        }
    }

    /** Plain striped renderer for the other alert-table columns (no special coloring). */
    public static class AlertStripedRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                         boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setFont(Theme.FONT_TABLE);
            if (!isSelected) {
                setBackground(row % 2 == 0 ? Color.WHITE : Theme.STRIPE);
                setForeground(Theme.TEXT_PRIMARY);
            } else {
                setBackground(Theme.PRIMARY_LIGHT);
                setForeground(Theme.PRIMARY_DARK);
            }
            return c;
        }
    }
}
