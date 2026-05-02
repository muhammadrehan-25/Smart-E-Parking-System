package main;

import dao.DatabaseManager;
import ui.SplashScreen;
import javax.swing.*;


public class Main {
    public static void main(String[] args) {
        // Set system look-and-feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) { }

        // Initialize database (creates tables + seed data)
        DatabaseManager.getInstance();

        // Launch splash screen on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            SplashScreen splash = new SplashScreen();
            splash.showAndLoad();
        });
    }
}
