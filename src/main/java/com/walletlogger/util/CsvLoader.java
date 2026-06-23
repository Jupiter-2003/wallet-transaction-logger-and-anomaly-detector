package com.walletlogger.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import com.walletlogger.exceptions.InvalidTransactionException;
import com.walletlogger.model.CreditTransaction;
import com.walletlogger.model.DebitTransaction;
import com.walletlogger.model.RefundTransaction;
import com.walletlogger.model.Transaction;
import com.walletlogger.model.TransactionType;

/**
 * Loads historical transactions from a CSV file into a List<Transaction>.
 *
 * Expected CSV format (with header row):
 *   transaction_id,user_id,vendor_id,amount,timestamp,type
 *
 * Example row:
 *   TXN001,U01,V03,450.00,2026-06-01T10:30:00,DEBIT
 *
 * Uses BufferedReader + try-with-resources so the file is always closed,
 * even if an exception occurs mid-read.
 */
public class CsvLoader {

    private CsvLoader() {}   // utility class

    /**
     * Parses the CSV at the given file path and returns a list of Transaction objects.
     * Rows that fail validation are skipped and logged to stderr.
     */
    public static List<Transaction> load(String filePath) throws IOException {
        List<Transaction> transactions = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {
                // Skip header
                if (firstLine) { firstLine = false; continue; }
                // Skip blank lines
                if (line.isBlank()) continue;

                try {
                    transactions.add(parseLine(line));
                } catch (InvalidTransactionException e) {
                    System.err.println("Skipping invalid CSV row: " + e.getMessage());
                }
            }
        }

        System.out.println("CSV loaded: " + transactions.size() + " transactions from " + filePath);
        return transactions;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static Transaction parseLine(String line) throws InvalidTransactionException {
        String[] parts = line.split(",");
        if (parts.length != 6) {
            throw new InvalidTransactionException(
                    "Expected 6 columns, got " + parts.length + " in: " + line);
        }

        String txnId    = parts[0].trim();
        String userId   = parts[1].trim();
        String vendorId = parts[2].trim();
        String amtStr   = parts[3].trim();
        String tsStr    = parts[4].trim();
        String typeStr  = parts[5].trim();

        if (txnId.isBlank() || userId.isBlank() || vendorId.isBlank()) {
            throw new InvalidTransactionException("Blank required field in: " + line);
        }

        double amount;
        try {
            amount = Double.parseDouble(amtStr);
        } catch (NumberFormatException e) {
            throw new InvalidTransactionException("Invalid amount '" + amtStr + "' in: " + line);
        }
        if (amount < 0) {
            throw new InvalidTransactionException("Negative amount in: " + line);
        }

        LocalDateTime timestamp;
        try {
            timestamp = LocalDateTime.parse(tsStr);
        } catch (DateTimeParseException e) {
            throw new InvalidTransactionException("Invalid timestamp '" + tsStr + "' in: " + line);
        }

        TransactionType type;
        try {
            type = TransactionType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidTransactionException("Unknown type '" + typeStr + "' in: " + line);
        }

        Transaction.Builder<?> builder = switch (type) {
            case DEBIT  -> new DebitTransaction.Builder();
            case CREDIT -> new CreditTransaction.Builder();
            case REFUND -> new RefundTransaction.Builder();
        };

        return builder
                .transactionId(txnId)
                .userId(userId)
                .vendorId(vendorId)
                .amount(amount)
                .timestamp(timestamp)
                .type(type)
                .build();
    }
}
