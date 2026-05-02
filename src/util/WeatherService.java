package util;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Fetches real-time weather using Windows Location Services (GPS/WiFi-based).
 * Shows a mobile-style "Enable Location" dialog if location is turned off.
 */
public class WeatherService {

    public static String fetchWeather(java.awt.Component parent) {
        try {
            double[] coords = getWindowsLocation();
            if (coords != null) {
                String city = reverseGeocode(coords[0], coords[1]);
                String weather = fetchWeatherData(coords[0], coords[1]);
                return weather + " | " + city;
            } else {
                // Location OFF — show dialog on EDT
                final boolean[] opened = {false};
                try {
                    SwingUtilities.invokeAndWait(() -> opened[0] = showLocationDialog(parent));
                } catch (Exception ignored) {}
                if (opened[0]) {
                    Thread.sleep(6000);
                    coords = getWindowsLocation();
                    if (coords != null) {
                        String city = reverseGeocode(coords[0], coords[1]);
                        String weather = fetchWeatherData(coords[0], coords[1]);
                        return weather + " | " + city;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Weather failed: " + e.getMessage());
        }
        return fetchByIP();
    }

    /** Converts coordinates to city name using OpenStreetMap Nominatim */
    private static String reverseGeocode(double lat, double lon) {
        try {
            URL url = new URL("https://nominatim.openstreetmap.org/reverse?format=json&lat="
                    + lat + "&lon=" + lon + "&zoom=10&addressdetails=1");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setRequestProperty("User-Agent", "SmartEPark/1.0");

            if (conn.getResponseCode() == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();
                String json = sb.toString();

                // Parse city/town from JSON response
                String city = extractJsonValue(json, "city");
                if (city == null) city = extractJsonValue(json, "town");
                if (city == null) city = extractJsonValue(json, "county");
                if (city == null) city = extractJsonValue(json, "state");
                
                String country = extractJsonValue(json, "country_code");
                if (country != null) country = country.toUpperCase();

                if (city != null) {
                    return city + (country != null ? ", " + country : "");
                }
            }
        } catch (Exception e) {
            System.err.println("Geocode failed: " + e.getMessage());
        }
        return "Unknown Location";
    }

    /** Simple JSON value extractor (no library needed) */
    private static String extractJsonValue(String json, String key) {
        String search = "\"" + key + "\":\"";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        int start = idx + search.length();
        int end = json.indexOf("\"", start);
        if (end < 0) return null;
        return json.substring(start, end);
    }

    /** Fetches just the weather condition and temperature */
    private static String fetchWeatherData(double lat, double lon) {
        try {
            URL url = new URL("https://wttr.in/" + lat + "," + lon + "?format=%C,+%t");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            if (conn.getResponseCode() == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String resp = br.readLine();
                br.close();
                if (resp != null && !resp.isBlank()) return resp.trim();
            }
        } catch (Exception e) { System.err.println("Weather data: " + e.getMessage()); }
        return "Weather unavailable";
    }

    /** Shows premium mobile-style dialog asking user to enable location */
    private static boolean showLocationDialog(java.awt.Component parent) {
        final boolean[] result = {false};

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(parent),
                "Location Permission", true);
        dialog.setUndecorated(true);
        dialog.setSize(340, 280);
        dialog.setLocationRelativeTo(parent);
        dialog.setBackground(new Color(0, 0, 0, 0));

        JPanel card = new JPanel(new BorderLayout(0, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0x0F172A));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 20, 20));
                g2.setColor(new Color(14, 165, 233, 180));
                g2.fillRect(0, 0, getWidth(), 2);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(28, 28, 24, 28));

        // Icon area — glowing location pin
        JPanel iconPanel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(14, 165, 233, 40));
                g2.fillOval(getWidth()/2 - 32, 4, 64, 64);
                g2.setColor(new Color(14, 165, 233, 100));
                g2.fillOval(getWidth()/2 - 24, 12, 48, 48);
                g2.setColor(new Color(0x0EA5E9));
                g2.fillOval(getWidth()/2 - 8, 24, 16, 16);
                int[] xs = {getWidth()/2 - 8, getWidth()/2 + 8, getWidth()/2};
                int[] ys = {36, 36, 52};
                g2.fillPolygon(xs, ys, 3);
                g2.dispose();
            }
        };
        iconPanel.setOpaque(false);
        iconPanel.setPreferredSize(new Dimension(340, 75));
        card.add(iconPanel, BorderLayout.NORTH);

        // Text
        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setBorder(BorderFactory.createEmptyBorder(12, 0, 20, 0));

        JLabel title = new JLabel("Location is Turned Off", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel msg = new JLabel("<html><center>Enable Location Services to show<br>accurate local weather on your dashboard.</center></html>",
                SwingConstants.CENTER);
        msg.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        msg.setForeground(new Color(0x94A3B8));
        msg.setAlignmentX(Component.CENTER_ALIGNMENT);

        textPanel.add(title);
        textPanel.add(Box.createVerticalStrut(8));
        textPanel.add(msg);
        card.add(textPanel, BorderLayout.CENTER);

        // Buttons
        JPanel btnRow = new JPanel(new GridLayout(1, 2, 10, 0));
        btnRow.setOpaque(false);

        JButton skipBtn = new JButton("Skip") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0x1E3A5F));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        skipBtn.setContentAreaFilled(false); skipBtn.setBorderPainted(false);
        skipBtn.setFocusPainted(false); skipBtn.setOpaque(false);
        skipBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        skipBtn.setForeground(new Color(0x94A3B8));
        skipBtn.setPreferredSize(new Dimension(0, 42));
        skipBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        skipBtn.addActionListener(e -> { result[0] = false; dialog.dispose(); });

        JButton openBtn = new JButton("Open Settings") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, new Color(0x0EA5E9), 0, getHeight(), new Color(0x0369A1)));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        openBtn.setContentAreaFilled(false); openBtn.setBorderPainted(false);
        openBtn.setFocusPainted(false); openBtn.setOpaque(false);
        openBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        openBtn.setForeground(Color.WHITE);
        openBtn.setPreferredSize(new Dimension(0, 42));
        openBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        openBtn.addActionListener(e -> {
            result[0] = true;
            try { Runtime.getRuntime().exec("cmd /c start ms-settings:privacy-location"); }
            catch (Exception ex) { ex.printStackTrace(); }
            dialog.dispose();
        });

        btnRow.add(skipBtn);
        btnRow.add(openBtn);
        card.add(btnRow, BorderLayout.SOUTH);

        dialog.setContentPane(card);
        dialog.setVisible(true);
        return result[0];
    }

    /** Uses PowerShell + Windows Location API for exact GPS coordinates */
    private static double[] getWindowsLocation() throws Exception {
        String psScript =
            "Add-Type -AssemblyName System.Device;" +
            "$w = New-Object System.Device.Location.GeoCoordinateWatcher;" +
            "$w.Start(); Start-Sleep -Seconds 2;" +
            "$l = $w.Position.Location;" +
            "if (-not $l.IsUnknown) { Write-Output ($l.Latitude.ToString() + ',' + $l.Longitude.ToString()) }" +
            "else { Write-Output 'UNKNOWN' }; $w.Stop();";

        ProcessBuilder pb = new ProcessBuilder(
            "powershell.exe", "-NoProfile", "-NonInteractive",
            "-ExecutionPolicy", "Bypass", "-Command", psScript);
        pb.redirectErrorStream(true);
        Process proc = pb.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.contains(",") && !line.equals("UNKNOWN")) {
                String[] p = line.split(",");
                if (p.length == 2) {
                    return new double[]{Double.parseDouble(p[0].trim()), Double.parseDouble(p[1].trim())};
                }
            }
        }
        proc.waitFor();
        return null;
    }

    /** Fallback: IP-based weather */
    private static String fetchByIP() {
        try {
            URL url = new URL("https://wttr.in/?format=%C,+%t+|+%l");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(3000); conn.setReadTimeout(3000);
            if (conn.getResponseCode() == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String resp = br.readLine(); br.close();
                if (resp != null && !resp.isBlank()) return resp.trim();
            }
        } catch (Exception e) { System.err.println("IP weather: " + e.getMessage()); }
        return "Weather unavailable";
    }
}
