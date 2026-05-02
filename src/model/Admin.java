package model;

import interfaces.Reportable;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Admin user — full system control.
 * Demonstrates INHERITANCE from User and POLYMORPHISM (getRole override).
 */
public class Admin extends User implements Reportable {
    private static final long serialVersionUID = 1L;

    private List<String> activityLog; // COMPOSITION: ArrayList of strings

    public Admin(int userId, String username, String passwordHash,
                 String fullName, String email, String phone) {
        super(userId, username, passwordHash, fullName, email, phone);
        this.activityLog = new ArrayList<>();
    }

    // POLYMORPHISM: overrides abstract method
    @Override public String getRole()           { return "ADMIN"; }
    @Override public String getDashboardTitle() { return "Admin Control Panel"; }

    public void logAction(String action) {
        String entry = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                + " | ADMIN | " + getUsername() + " | " + action;
        activityLog.add(entry);
    }

    public List<String> getActivityLog() { return activityLog; }

    // Reportable interface implementation
    @Override
    public String generateReport() {
        StringBuilder sb = new StringBuilder("=== Admin Activity Report ===\n");
        for (String entry : activityLog) sb.append(entry).append("\n");
        return sb.toString();
    }

    @Override
    public boolean exportCSV(String filePath) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filePath))) {
            pw.println("Timestamp,Role,Username,Action");
            for (String entry : activityLog) {
                pw.println(entry.replace(" | ", ","));
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public String getSummary() {
        return "Admin: " + getFullName() + " | Total actions: " + activityLog.size();
    }
}
