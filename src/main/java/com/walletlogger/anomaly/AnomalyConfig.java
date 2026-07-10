package com.walletlogger.anomaly;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.walletlogger.exceptions.AnomalyConfigException;

/**
 * Loads and validates the tunable thresholds used by the anomaly rules
 * from {@code anomaly_config.properties} on the classpath.
 *
 * Failing fast here (rather than deep inside a rule at 2am during a demo)
 * is the whole point of {@link AnomalyConfigException} — a malformed or
 * missing config should never silently fall back to made-up numbers.
 */
public final class AnomalyConfig {

    private static final String DEFAULT_RESOURCE = "/anomaly_config.properties";

    private final double spikeZScoreThreshold;
    private final int    spikeMinSamples;

    private final int  velocityMaxCount;
    private final long velocityWindowSeconds;

    private final int    dormantDaysThreshold;
    private final double dormantAmountMultiplier;

    private final long duplicateWindowSeconds;

    private final int unusualHourStart;
    private final int unusualHourEnd;

    private AnomalyConfig(Properties p) throws AnomalyConfigException {
        this.spikeZScoreThreshold    = parseDouble(p, "spike.zscore.threshold");
        this.spikeMinSamples         = parseInt(p, "spike.min.samples");
        this.velocityMaxCount        = parseInt(p, "velocity.max.count");
        this.velocityWindowSeconds   = parseInt(p, "velocity.window.seconds");
        this.dormantDaysThreshold    = parseInt(p, "dormant.days.threshold");
        this.dormantAmountMultiplier = parseDouble(p, "dormant.amount.multiplier");
        this.duplicateWindowSeconds  = parseInt(p, "duplicate.window.seconds");
        this.unusualHourStart        = parseInt(p, "unusual.hour.start");
        this.unusualHourEnd          = parseInt(p, "unusual.hour.end");

        if (spikeZScoreThreshold <= 0) {
            throw new AnomalyConfigException("spike.zscore.threshold must be positive");
        }
        if (spikeMinSamples < 1) {
            throw new AnomalyConfigException("spike.min.samples must be >= 1");
        }
        if (velocityMaxCount < 1) {
            throw new AnomalyConfigException("velocity.max.count must be >= 1");
        }
        if (velocityWindowSeconds <= 0) {
            throw new AnomalyConfigException("velocity.window.seconds must be positive");
        }
        if (dormantDaysThreshold < 1) {
            throw new AnomalyConfigException("dormant.days.threshold must be >= 1");
        }
        if (dormantAmountMultiplier <= 0) {
            throw new AnomalyConfigException("dormant.amount.multiplier must be positive");
        }
        if (duplicateWindowSeconds <= 0) {
            throw new AnomalyConfigException("duplicate.window.seconds must be positive");
        }
        if (unusualHourStart < 0 || unusualHourStart > 23 || unusualHourEnd < 0 || unusualHourEnd > 23) {
            throw new AnomalyConfigException("unusual.hour.start/end must be within [0,23]");
        }
    }

    /** Loads config from the default classpath resource. */
    public static AnomalyConfig loadDefault() throws AnomalyConfigException {
        return load(DEFAULT_RESOURCE);
    }

    /** Loads config from an arbitrary classpath resource — handy for tests. */
    public static AnomalyConfig load(String classpathResource) throws AnomalyConfigException {
        Properties props = new Properties();
        try (InputStream in = AnomalyConfig.class.getResourceAsStream(classpathResource)) {
            if (in == null) {
                throw new AnomalyConfigException(
                        "Anomaly config resource not found on classpath: " + classpathResource);
            }
            props.load(in);
        } catch (IOException e) {
            throw new AnomalyConfigException(
                    "Failed to read anomaly config: " + classpathResource, e);
        }
        return new AnomalyConfig(props);
    }

    /** Builds a config directly from an in-memory Properties object — used by unit tests. */
    public static AnomalyConfig fromProperties(Properties props) throws AnomalyConfigException {
        return new AnomalyConfig(props);
    }

    private static double parseDouble(Properties p, String key) throws AnomalyConfigException {
        String raw = requireKey(p, key);
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            throw new AnomalyConfigException("Invalid double for '" + key + "': " + raw, e);
        }
    }

    private static int parseInt(Properties p, String key) throws AnomalyConfigException {
        String raw = requireKey(p, key);
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new AnomalyConfigException("Invalid integer for '" + key + "': " + raw, e);
        }
    }

    private static String requireKey(Properties p, String key) throws AnomalyConfigException {
        String value = p.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new AnomalyConfigException("Missing required anomaly config key: " + key);
        }
        return value.trim();
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public double getSpikeZScoreThreshold()    { return spikeZScoreThreshold; }
    public int    getSpikeMinSamples()         { return spikeMinSamples; }
    public int    getVelocityMaxCount()        { return velocityMaxCount; }
    public long   getVelocityWindowSeconds()   { return velocityWindowSeconds; }
    public int    getDormantDaysThreshold()    { return dormantDaysThreshold; }
    public double getDormantAmountMultiplier() { return dormantAmountMultiplier; }
    public long   getDuplicateWindowSeconds()  { return duplicateWindowSeconds; }
    public int    getUnusualHourStart()        { return unusualHourStart; }
    public int    getUnusualHourEnd()          { return unusualHourEnd; }
}
