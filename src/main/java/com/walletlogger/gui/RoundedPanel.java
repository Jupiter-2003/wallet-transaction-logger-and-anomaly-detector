package com.walletlogger.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * A JPanel that paints itself as a rounded, flat-shadowed card instead of a
 * plain rectangle — the basic building block for the dashboard stat cards
 * and any other "card" styled surface in the GUI.
 */
public class RoundedPanel extends JPanel {

    private final int cornerRadius;
    private Color backgroundColorOverride;

    public RoundedPanel(int cornerRadius) {
        this.cornerRadius = cornerRadius;
        setOpaque(false);
        setBorder(new EmptyBorder(14, 16, 14, 16));
    }

    public void setCardBackground(Color color) {
        this.backgroundColorOverride = color;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color bg = backgroundColorOverride != null ? backgroundColorOverride : Theme.CARD_BACKGROUND;
        g2.setColor(bg);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius);

        g2.setColor(Theme.BORDER);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius);

        g2.dispose();
        super.paintComponent(g);
    }
}
