package com.walletlogger;

import com.walletlogger.dao.AnomalyDAO;
import com.walletlogger.dao.SchemaInitialiser;
import com.walletlogger.dao.TransactionDAO;
import com.walletlogger.model.Transaction;
import com.walletlogger.util.CsvLoader;

import java.util.List;

/**
 * Entry point for Week 1 smoke test.
 * Initialises the DB, loads the sample CSV, inserts all transactions,
 * then reads them back and prints them — verifying the full DAO layer works.
 */
public class Main {

    public static void main(String[] args) {
        try {
            // 1. Create tables if they don't exist
            SchemaInitialiser.initialise();

            // 2. Load transactions from CSV
            List<Transaction> transactions = CsvLoader.load(
                    "src/main/resources/sample_transactions.csv");

            // 3. Insert all into DB via TransactionDAO
            TransactionDAO txnDAO = new TransactionDAO();
            for (Transaction t : transactions) {
                txnDAO.insert(t);
            }
            System.out.println("Inserted " + transactions.size() + " transactions.");

            // 4. Read them all back and print
            List<Transaction> fromDB = txnDAO.findAll();
            System.out.println("\n── Transactions in DB ──────────────────");
            fromDB.forEach(System.out::println);

            // 5. Test findById
            txnDAO.findById("TXN001").ifPresentOrElse(
                    t -> System.out.println("\nFound by ID: " + t),
                    ()  -> System.out.println("\nTXN001 not found.")
            );

            // 6. Verify AnomalyDAO (empty for now — detection comes in Week 2)
            AnomalyDAO anomalyDAO = new AnomalyDAO();
            System.out.println("\nAnomaly log count: " + anomalyDAO.findAll().size());

            System.out.println("\nWeek 1 smoke test passed.");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
