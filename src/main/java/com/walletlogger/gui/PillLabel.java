package com.walletlogger.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

/**
 * A JLabel that paints a solid rounded-rectangle background behind its text,
 * used to render status "pills" (e.g. severity level, flagged indicator)
 * inside table cells.
 */
public class PillLabel extends JLabel {

    private Color pillColor = Theme.PRIMARY_LIGHT;
    private Color textColor = Theme.PRIMARY;

    public PillLabel() {
        setHorizontalAlignment(SwingConstants.CENTER);
        setOpaque(false);
        setBorder(new EmptyBorder(2, 10, 2, 10));
        setFont(Theme.FONT_LABEL);
    }

    public void setColors(Color pillColor, Color textColor) {
        this.pillColor = pillColor;
        this.textColor = textColor;
        setForeground(textColor);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(pillColor);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
        g2.dispose();
        super.paintComponent(g);
    }
}
