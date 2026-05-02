package util;

import java.io.*;
import java.util.Properties;

/** Centralized configuration management for SmartEPark. */
public class Settings {
    private static final String FILE_PATH = util.FileManager.BASE_DIR + "data/settings.properties";
    private static Properties props = new Properties();

    static { load(); }

    public static void load() {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            saveDefaults();
        }
        try (InputStream is = new FileInputStream(file)) {
            props.load(is);
        } catch (IOException e) {
            System.err.println("Error loading settings: " + e.getMessage());
        }
    }

    private static void saveDefaults() {
        new File(util.FileManager.BASE_DIR + "data").mkdirs();
        props.setProperty("carRatePerHour", "100");
        props.setProperty("bikeRatePerHour", "50");
        props.setProperty("taxPercentage", "10");
        props.setProperty("carSlots", "30");
        props.setProperty("bikeSlots", "20");
        try (OutputStream os = new FileOutputStream(FILE_PATH)) {
            props.store(os, "Default Settings");
        } catch (IOException ignored) {}
    }

    public static double getDouble(String key, double def) {
        try { return Double.parseDouble(props.getProperty(key)); }
        catch (Exception e) { return def; }
    }

    public static int getInt(String key, int def) {
        try { return Integer.parseInt(props.getProperty(key)); }
        catch (Exception e) { return def; }
    }

    public static String get(String key, String def) {
        return props.getProperty(key, def);
    }

    // ── Helper methods for slots ────────────────────────────
    public static int getCarSlots() { return getInt("carSlots", 30); }
    public static int getBikeSlots() { return getInt("bikeSlots", 20); }
    public static int getTotalSlots() { return getCarSlots() + getBikeSlots(); }

    public static boolean isMaintenanceMode() {
        return get("maintenanceMode", "false").equalsIgnoreCase("true");
    }

    public static void setMaintenanceMode(boolean active) {
        props.setProperty("maintenanceMode", String.valueOf(active));
        try (OutputStream os = new FileOutputStream(FILE_PATH)) {
            props.store(os, "Updated Settings");
        } catch (IOException ignored) {}
    }
}
