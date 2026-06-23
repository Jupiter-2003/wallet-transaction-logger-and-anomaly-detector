package com.walletlogger.dao;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.walletlogger.exceptions.TransactionPersistenceException;
import com.walletlogger.model.CreditTransaction;
import com.walletlogger.model.DebitTransaction;
import com.walletlogger.model.RefundTransaction;
import com.walletlogger.model.Transaction;
import com.walletlogger.model.TransactionType;

/**
 * JUnit 5 tests for TransactionDAO.
 * Uses an in-memory SQLite DB so tests are fast and don't touch the real file.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TransactionDAOTest {

    private static TransactionDAO dao;

    @BeforeAll
    static void setup() throws SQLException {
        // Point to an in-memory SQLite DB for tests
        System.setProperty("db.url", "jdbc:sqlite::memory:");
        SchemaInitialiser.initialise();
        dao = new TransactionDAO();
    }

    private Transaction buildDebit(String id, String userId, double amount) {
        return new DebitTransaction.Builder()
                .transactionId(id)
                .userId(userId)
                .vendorId("V01")
                .amount(amount)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // ── INSERT + FIND BY ID ───────────────────────────────────────────────────

    @Test
    @Order(1)
    void testInsertAndFindById() throws TransactionPersistenceException {
        Transaction t = buildDebit("TEST001", "U01", 350.0);
        dao.insert(t);

        Optional<Transaction> found = dao.findById("TEST001");
        assertTrue(found.isPresent(), "Transaction should be found after insert");
        assertEquals("U01",   found.get().getUserId());
        assertEquals(350.0,   found.get().getAmount(), 0.001);
        assertEquals(TransactionType.DEBIT, found.get().getType());
    }

    // ── FIND ALL ──────────────────────────────────────────────────────────────

    @Test
    @Order(2)
    void testFindAll() throws TransactionPersistenceException {
        dao.insert(buildDebit("TEST002", "U02", 100.0));
        dao.insert(buildDebit("TEST003", "U03", 200.0));

        List<Transaction> all = dao.findAll();
        assertTrue(all.size() >= 3, "Should have at least 3 transactions");
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    @Test
    @Order(3)
    void testUpdate() throws TransactionPersistenceException {
        Transaction t = dao.findById("TEST001").orElseThrow();
        t.setFlagged(true);
        t.setFlagReason("AMOUNT_SPIKE");
        dao.update(t);

        Transaction updated = dao.findById("TEST001").orElseThrow();
        assertTrue(updated.isFlagged());
        assertEquals("AMOUNT_SPIKE", updated.getFlagReason());
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Test
    @Order(4)
    void testDelete() throws TransactionPersistenceException {
        dao.delete("TEST001");
        Optional<Transaction> deleted = dao.findById("TEST001");
        assertFalse(deleted.isPresent(), "Transaction should be gone after delete");
    }

    // ── FIND NOT FOUND ────────────────────────────────────────────────────────

    @Test
    @Order(5)
    void testFindByIdNotFound() throws TransactionPersistenceException {
        Optional<Transaction> result = dao.findById("NONEXISTENT");
        assertFalse(result.isPresent());
    }

    // ── FILTERED QUERY ────────────────────────────────────────────────────────

    @Test
    @Order(6)
    void testFindWithFilters() throws TransactionPersistenceException {
        dao.insert(buildDebit("TEST004", "U02", 999.0));

        List<Transaction> results = dao.findWithFilters(
                "U02", null, null, 500.0, null, null, null);
        assertTrue(results.stream().allMatch(t -> t.getUserId().equals("U02")));
        assertTrue(results.stream().allMatch(t -> t.getAmount() >= 500.0));
    }

    // ── CREDIT AND REFUND TYPES ───────────────────────────────────────────────

    @Test
    @Order(7)
    void testCreditAndRefundSubtypes() throws TransactionPersistenceException {
        Transaction credit = new CreditTransaction.Builder()
                .transactionId("TEST005").userId("U01").vendorId("V01")
                .amount(1000.0).timestamp(LocalDateTime.now()).build();

        Transaction refund = new RefundTransaction.Builder()
                .transactionId("TEST006").userId("U01").vendorId("V01")
                .amount(200.0).timestamp(LocalDateTime.now()).build();

        dao.insert(credit);
        dao.insert(refund);

        assertEquals(TransactionType.CREDIT,
                dao.findById("TEST005").orElseThrow().getType());
        assertEquals(TransactionType.REFUND,
                dao.findById("TEST006").orElseThrow().getType());
    }
}
