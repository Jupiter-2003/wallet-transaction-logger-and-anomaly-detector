package com.walletlogger.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

/**
 * A single dashboard "stat card" — a big number with a label underneath and
 * a colored accent dot, used in the header dashboard row (Processed,
 * Alerts Raised, Flagged Rate, Active Users).
 */
public class StatCard extends RoundedPanel {

    private final JLabel valueLabel;
    private final JLabel titleLabel;

    public StatCard(String title, String initialValue, Color accentColor) {
        super(14);
        setLayout(new BorderLayout(4, 2));
        setPreferredSize(new Dimension(190, 78));

        JLabel dot = new JLabel("\u25CF"); // ●
        dot.setForeground(accentColor);

        titleLabel = new JLabel(title.toUpperCase());
        titleLabel.setFont(Theme.FONT_LABEL);
        titleLabel.setForeground(Theme.TEXT_MUTED);

        var topRow = new javax.swing.JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        topRow.setOpaque(false);
        topRow.add(dot);
        topRow.add(titleLabel);

        valueLabel = new JLabel(initialValue);
        valueLabel.setFont(Theme.FONT_STAT);
        valueLabel.setForeground(Theme.TEXT_PRIMARY);
        valueLabel.setHorizontalAlignment(SwingConstants.LEFT);

        add(topRow, BorderLayout.NORTH);
        add(valueLabel, BorderLayout.CENTER);
    }

    public void setValue(String value) {
        valueLabel.setText(value);
    }
}
