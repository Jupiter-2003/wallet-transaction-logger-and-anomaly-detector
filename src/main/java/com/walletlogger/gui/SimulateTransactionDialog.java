package com.walletlogger.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDateTime;
import java.util.UUID;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.walletlogger.model.CreditTransaction;
import com.walletlogger.model.DebitTransaction;
import com.walletlogger.model.RefundTransaction;
import com.walletlogger.model.Transaction;
import com.walletlogger.model.TransactionType;

/**
 * A single well-laid-out form for manually injecting a transaction into the
 * live pipeline, replacing what used to be a chain of separate
 * {@code JOptionPane.showInputDialog} popups.
 */
public class SimulateTransactionDialog extends JDialog {

    private final JTextField userIdField = new JTextField("U01", 14);
    private final JTextField vendorIdField = new JTextField("V01", 14);
    private final JTextField amountField = new JTextField("100.00", 14);
    private final JComboBox<TransactionType> typeBox = new JComboBox<>(TransactionType.values());

    private Transaction result;

    public SimulateTransactionDialog(Frame owner) {
        super(owner, "Simulate a Transaction", true);
        setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        form.setBackground(Theme.CARD_BACKGROUND);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        addRow(form, gbc, 0, "User ID", userIdField);
        addRow(form, gbc, 1, "Vendor ID", vendorIdField);
        addRow(form, gbc, 2, "Amount (\u20B9)", amountField);
        addRow(form, gbc, 3, "Type", typeBox);

        JLabel hint = new JLabel("<html><i>Tip: submit the same user/vendor/amount twice quickly, "
                + "or a very large amount, to trigger an anomaly alert.</i></html>");
        hint.setFont(Theme.FONT_LABEL);
        hint.setForeground(Theme.TEXT_MUTED);
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        form.add(hint, gbc);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            result = null;
            dispose();
        });

        JButton submitButton = new JButton("Submit Transaction");
        submitButton.setBackground(Theme.PRIMARY);
        submitButton.setForeground(java.awt.Color.WHITE);
        submitButton.setFocusPainted(false);
        submitButton.addActionListener(e -> onSubmit());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        buttonPanel.setBackground(Theme.CARD_BACKGROUND);
        buttonPanel.add(cancelButton);
        buttonPanel.add(submitButton);

        add(form, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(submitButton);
        setPreferredSize(new Dimension(420, 300));
        pack();
        setLocationRelativeTo(owner);
        setResizable(false);
    }

    private void addRow(JPanel form, GridBagConstraints gbc, int row, String labelText, java.awt.Component field) {
        JLabel label = new JLabel(labelText);
        label.setFont(Theme.FONT_LABEL);
        label.setForeground(Theme.TEXT_MUTED);

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        form.add(label, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        form.add(field, gbc);
    }

    private void onSubmit() {
        String userId = userIdField.getText().trim();
        String vendorId = vendorIdField.getText().trim();
        if (userId.isEmpty() || vendorId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "User ID and Vendor ID are required.", "Missing fields",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountField.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Amount must be a number.", "Invalid amount",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        TransactionType type = (TransactionType) typeBox.getSelectedItem();
        Transaction.Builder<?> builder = switch (type) {
            case DEBIT  -> new DebitTransaction.Builder();
            case CREDIT -> new CreditTransaction.Builder();
            case REFUND -> new RefundTransaction.Builder();
        };

        this.result = builder
                .transactionId("SIM-" + UUID.randomUUID().toString().substring(0, 8))
                .userId(userId)
                .vendorId(vendorId)
                .amount(amount)
                .timestamp(LocalDateTime.now())
                .build();

        dispose();
    }

    /** @return the constructed transaction, or {@code null} if the dialog was cancelled. */
    public Transaction getResult() {
        return result;
    }
}
