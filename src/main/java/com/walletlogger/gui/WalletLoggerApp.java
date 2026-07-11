package com.walletlogger.gui;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatLightLaf;

/**
 * launches the Swing desktop application.
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
