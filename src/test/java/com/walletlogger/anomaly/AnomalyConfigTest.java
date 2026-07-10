package com.walletlogger.anomaly;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.walletlogger.exceptions.AnomalyConfigException;

class AnomalyConfigTest {

    private Properties validProperties() {
        Properties p = new Properties();
        p.setProperty("spike.zscore.threshold", "3.0");
        p.setProperty("spike.min.samples", "3");
        p.setProperty("velocity.max.count", "4");
        p.setProperty("velocity.window.seconds", "60");
        p.setProperty("dormant.days.threshold", "30");
        p.setProperty("dormant.amount.multiplier", "2.0");
        p.setProperty("duplicate.window.seconds", "30");
        p.setProperty("unusual.hour.start", "6");
        p.setProperty("unusual.hour.end", "23");
        return p;
    }

    @Test
    void loadsDefaultResourceSuccessfully() throws AnomalyConfigException {
        AnomalyConfig config = AnomalyConfig.loadDefault();
        assertEquals(3.0, config.getSpikeZScoreThreshold(), 0.001);
        assertEquals(4, config.getVelocityMaxCount());
    }

    @Test
    void validPropertiesLoadCleanly() throws AnomalyConfigException {
        AnomalyConfig config = AnomalyConfig.fromProperties(validProperties());
        assertEquals(30, config.getDormantDaysThreshold());
        assertEquals(6, config.getUnusualHourStart());
        assertEquals(23, config.getUnusualHourEnd());
    }

    @Test
    void missingKeyThrowsConfigException() {
        Properties p = validProperties();
        p.remove("spike.zscore.threshold");
        assertThrows(AnomalyConfigException.class, () -> AnomalyConfig.fromProperties(p));
    }

    @Test
    void nonNumericValueThrowsConfigException() {
        Properties p = validProperties();
        p.setProperty("velocity.max.count", "not-a-number");
        assertThrows(AnomalyConfigException.class, () -> AnomalyConfig.fromProperties(p));
    }

    @Test
    void negativeThresholdThrowsConfigException() {
        Properties p = validProperties();
        p.setProperty("spike.zscore.threshold", "-1.0");
        assertThrows(AnomalyConfigException.class, () -> AnomalyConfig.fromProperties(p));
    }

    @Test
    void missingResourceThrowsConfigException() {
        assertThrows(AnomalyConfigException.class, () -> AnomalyConfig.load("/does_not_exist.properties"));
    }
}
