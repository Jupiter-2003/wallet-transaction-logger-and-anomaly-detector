package com.walletlogger.gui;

import java.awt.Color;
import java.awt.Font;

/**
 * Central palette and typography for the Swing GUI so every panel/renderer
 * pulls from the same set of colors instead of hardcoding ad-hoc values.
 */
public final class Theme {

    private Theme() {}

    public static final Color PRIMARY        = new Color(0x4F46E5); // indigo
    public static final Color PRIMARY_DARK    = new Color(0x3730A3);
    public static final Color PRIMARY_LIGHT   = new Color(0xEEF2FF);

    public static final Color SUCCESS         = new Color(0x059669); // emerald
    public static final Color SUCCESS_LIGHT   = new Color(0xECFDF5);

    public static final Color WARNING         = new Color(0xD97706); // amber
    public static final Color WARNING_LIGHT   = new Color(0xFFFBEB);

    public static final Color DANGER          = new Color(0xDC2626); // red
    public static final Color DANGER_LIGHT    = new Color(0xFEF2F2);

    public static final Color TEXT_PRIMARY    = new Color(0x111827);
    public static final Color TEXT_MUTED      = new Color(0x6B7280);

    public static final Color BACKGROUND      = new Color(0xF3F4F6);
    public static final Color CARD_BACKGROUND = Color.WHITE;
    public static final Color BORDER          = new Color(0xE5E7EB);
    public static final Color STRIPE          = new Color(0xF9FAFB);

    public static final Font FONT_TITLE    = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_STAT     = new Font("Segoe UI", Font.BOLD, 26);
    public static final Font FONT_LABEL    = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_TABLE    = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_TABLE_HEADER = new Font("Segoe UI", Font.BOLD, 12);

    /** Maps a 1–5 rule severity score to a color for badges. */
    public static Color severityColor(int score) {
        if (score >= 5) return DANGER;
        if (score >= 4) return new Color(0xEA580C); // orange
        if (score >= 3) return WARNING;
        return new Color(0x2563EB); // blue for low severity
    }

    public static Color severityLightColor(int score) {
        if (score >= 5) return DANGER_LIGHT;
        if (score >= 4) return new Color(0xFFF7ED);
        if (score >= 3) return WARNING_LIGHT;
        return new Color(0xEFF6FF);
    }
}
