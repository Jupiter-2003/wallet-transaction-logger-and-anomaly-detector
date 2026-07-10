package com.walletlogger.anomaly;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.walletlogger.model.UserProfile;

/**
 * Persists the {@link AnomalyDetectionEngine}'s in-memory
 * {@code Map<String, UserProfile>} to disk between runs using standard
 * Java serialization, so a user's running mean/stddev/velocity history
 * survives an application restart instead of resetting to zero.
 */
public final class ProfileStore {

    private ProfileStore() {}

    /**
     * Serializes the given profile map to {@code filePath}.
     */
    public static void save(Map<String, UserProfile> profiles, String filePath) throws IOException {
        File file = new File(filePath);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(new ConcurrentHashMap<>(profiles));
        }
    }

    /**
     * Deserializes a profile map from {@code filePath}. If the file doesn't
     * exist yet (first run), returns a fresh empty map rather than failing —
     * this is the normal, expected case on a clean checkout.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, UserProfile> load(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            return new ConcurrentHashMap<>();
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            Object obj = ois.readObject();
            return new ConcurrentHashMap<>((Map<String, UserProfile>) obj);
        } catch (ClassNotFoundException e) {
            throw new IOException("Profile store file is corrupt or incompatible: " + filePath, e);
        }
    }
}
