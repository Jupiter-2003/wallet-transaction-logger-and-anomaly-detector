package com.walletlogger.gui;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatLightLaf;

/**
 * Week 3 entry point — launches the Swing desktop application.
 * (The Week 1 CLI smoke test in {@code com.walletlogger.Main} is unchanged
 * and still available for a quick non-GUI sanity check of the DAO layer.)
 */
public class WalletLoggerApp {

    public static void main(String[] args) {
        FlatLightLaf.setup();
        UIManager.put("Component.arc", 10);
        UIManager.put("Button.arc", 10);
        UIManager.put("TabbedPane.selectedBackground", java.awt.Color.WHITE);
        UIManager.put("defaultFont", Theme.FONT_TABLE);

        SwingUtilities.invokeLater(() -> {
            try {
                MainFrame frame = new MainFrame();
                frame.setVisible(true);
            } catch (Exception e) {
                System.err.println("Failed to start Wallet Logger GUI: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        });
    }
}
