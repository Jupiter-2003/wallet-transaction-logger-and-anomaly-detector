package com.walletlogger.dao;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.walletlogger.exceptions.TransactionPersistenceException;
import com.walletlogger.model.AnomalyAlert;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AnomalyDAOTest {

    private static AnomalyDAO dao;

    @BeforeAll
    static void setup() throws SQLException {
        SchemaInitialiser.initialise();
        dao = new AnomalyDAO();
    }

    private AnomalyAlert buildAlert(String alertId, String txnId, String userId, String flag) {
        return new AnomalyAlert(alertId, txnId, userId, flag,
                "Test alert for " + flag, 3, LocalDateTime.now());
    }

    @Test
    @Order(1)
    void testInsertAndFindById() throws TransactionPersistenceException {
        AnomalyAlert alert = buildAlert("ALR001", "TXN001", "U01", "AMOUNT_SPIKE");
        dao.insert(alert);

        Optional<AnomalyAlert> found = dao.findById("ALR001");
        assertTrue(found.isPresent());
        assertEquals("AMOUNT_SPIKE", found.get().getFlagCode());
        assertEquals("U01", found.get().getUserId());
    }

    @Test
    @Order(2)
    void testFindAll() throws TransactionPersistenceException {
        dao.insert(buildAlert("ALR002", "TXN002", "U02", "HIGH_VELOCITY"));
        dao.insert(buildAlert("ALR003", "TXN003", "U01", "UNUSUAL_HOUR"));

        List<AnomalyAlert> all = dao.findAll();
        assertTrue(all.size() >= 3);
    }

    @Test
    @Order(3)
    void testFindByUserId() throws TransactionPersistenceException {
        List<AnomalyAlert> u01Alerts = dao.findByUserId("U01");
        assertTrue(u01Alerts.stream().allMatch(a -> a.getUserId().equals("U01")));
        assertTrue(u01Alerts.size() >= 2);
    }

    @Test
    @Order(4)
    void testDelete() throws TransactionPersistenceException {
        dao.delete("ALR001");
        assertFalse(dao.findById("ALR001").isPresent());
    }

    @Test
    @Order(5)
    void testUpdateThrowsUnsupported() {
        AnomalyAlert alert = buildAlert("ALR999", "TXN999", "U99", "TEST");
        assertThrows(UnsupportedOperationException.class, () -> dao.update(alert));
    }
}
