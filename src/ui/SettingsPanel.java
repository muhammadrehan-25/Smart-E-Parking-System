package ui;

import dao.DatabaseManager;
import util.Theme;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.io.*;
import java.sql.*;
import java.util.Properties;

/**
 * Professional Settings Panel
 *
 * Layout (top to bottom):
 *   ┌─ Page header bar ──────────────────────────────────────────────────────┐
 *   │  "System Settings"  subtitle                                           │
 *   └────────────────────────────────────────────────────────────────────────┘
 *   ┌─ Card 1: Configuration (two columns) ──────────────────────────────────┐
 *   │  LEFT: Slot Management  │  RIGHT: Pricing Configuration                │
 *   └────────────────────────────────────────────────────────────────────────┘
 *   ┌─ Card 2: Live Slot Summary ─────────────────────────────────────────────┐
 *   │  6 stat cells (car & bike × total / free / occupied)                   │
 *   └────────────────────────────────────────────────────────────────────────┘
 *   ┌─ Footer row ────────────────────────────────────────────────────────────┐
 *   │  hint text                          [ gradient Save All Settings btn ]  │
 *   └────────────────────────────────────────────────────────────────────────┘
 */
public class SettingsPanel extends JPanel {

    // ── Data fields ───────────────────────────────────────────────────────────
    private JTextField txtCarSlots, txtBikeSlots;
    private JTextField txtCarRate, txtBikeRate, txtTax;

    private final File       settingsFile = new File(util.FileManager.BASE_DIR + "data/settings.properties");
    private       Properties props        = new Properties();

    // ── Design tokens ─────────────────────────────────────────────────────────
    // Page background
    private static final Color PAGE_BG       = new Color(0xEEF2F7);

    // Cards
    private static final Color CARD_BG       = Color.WHITE;
    private static final Color BORDER_CLR    = new Color(0xCBD5E1);

    // Section labels / hints
    private static final Color TEXT_SECTION  = new Color(0x8898AA);  // upper-case labels
    private static final Color TEXT_LABEL    = new Color(0x4A5568);  // field labels
    private static final Color TEXT_HINT     = new Color(0xA0AEC0);  // small hint

    // Accent dots
    private static final Color CAR_DOT       = new Color(0x1E40AF);
    private static final Color BIKE_DOT      = new Color(0x6D28D9);
    private static final Color PRICE_DOT     = new Color(0x0D9488);

    // Badge colours
    private static final Color CAR_BG        = new Color(0xEFF6FF);
    private static final Color CAR_FG        = new Color(0x1D4ED8);
    private static final Color BIKE_BG       = new Color(0xF5F3FF);
    private static final Color BIKE_FG       = new Color(0x5B21B6);
    private static final Color RS_BG         = new Color(0xECFDF5);
    private static final Color RS_FG         = new Color(0x065F46);
    private static final Color PCT_BG        = new Color(0xFFF7ED);
    private static final Color PCT_FG        = new Color(0x9A3412);

    // Summary stat colours
    private static final Color FREE_CLR      = new Color(0x059669);
    private static final Color OCC_CLR       = new Color(0xDC2626);

    // Gradient button stops
    private static final Color BTN_G1        = new Color(0x0EA5E9); // sky-500
    private static final Color BTN_G2        = new Color(0x0369A1); // sky-700
    private static final Color BTN_HOVER_G1  = new Color(0x0284C7);
    private static final Color BTN_HOVER_G2  = new Color(0x075985);

    // Summary strip background
    private static final Color STRIP_BG      = new Color(0xF7FAFC);

    // ── Constructor ───────────────────────────────────────────────────────────
    public SettingsPanel() {
        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(PAGE_BG);
        loadSettings();
        buildUI();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  BUILD
    // ═════════════════════════════════════════════════════════════════════════
    private void buildUI() {
        // Main content container
        JPanel outer = new JPanel();
        outer.setLayout(new BoxLayout(outer, BoxLayout.Y_AXIS));
        outer.setOpaque(false);
        outer.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Page title block
        outer.add(buildPageHeader());
        outer.add(Box.createVerticalStrut(15));

        // Card 1 — configuration
        outer.add(buildConfigCard());
        outer.add(Box.createVerticalStrut(20));

        // Card 2 — live summary
        outer.add(buildSummaryCard());
        outer.add(Box.createVerticalStrut(20));

        // Footer row (Save Button)
        outer.add(buildFooterRow());
        outer.add(Box.createVerticalStrut(20));

        // Wrap everything in a JScrollPane
        JScrollPane scroll = new JScrollPane(outer);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        
        // Ultra-Smooth Scrolling Config
        JScrollBar vertical = scroll.getVerticalScrollBar();
        vertical.setUnitIncrement(25);  // Fast & Fluid
        vertical.setBlockIncrement(100); 
        vertical.setPreferredSize(new Dimension(8, 0));
        
        add(scroll, BorderLayout.CENTER);
    }

    // ── Page header ───────────────────────────────────────────────────────────
    private JPanel buildPageHeader() {
        return new JPanel();
    }

    private JPanel buildConfigCard() {
        JPanel container = new JPanel(new GridLayout(1, 2, 24, 0));
        container.setOpaque(false);
        container.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Left Column: Slots + Management
        JPanel leftCol = new JPanel();
        leftCol.setLayout(new BoxLayout(leftCol, BoxLayout.Y_AXIS));
        leftCol.setOpaque(false);
        
        JPanel slotCard = roundedCard(16);
        slotCard.setLayout(new BorderLayout());
        slotCard.add(buildSlotSection(), BorderLayout.CENTER);
        leftCol.add(slotCard);
        
        leftCol.add(vGap(20));
        leftCol.add(buildManagementCard());
        container.add(leftCol);

        // Right Column: Pricing
        JPanel priceCard = roundedCard(16);
        priceCard.setLayout(new BorderLayout());
        priceCard.add(buildPricingSection(), BorderLayout.CENTER);
        container.add(priceCard);

        return container;
    }

    private JCheckBox chkMaintenance;
    private JPanel buildManagementCard() {
        JPanel card = roundedCard(16);
        card.setLayout(new BorderLayout());
        JPanel p = sectionPanel(16, 25);
        p.add(sectionTitle("System Management", new Color(0xEF4444)));
        p.add(vGap(14));
        
        chkMaintenance = new JCheckBox("Enable Maintenance Mode");
        chkMaintenance.setSelected(util.Settings.isMaintenanceMode());
        chkMaintenance.setFont(new Font("Segoe UI", Font.BOLD, 14));
        chkMaintenance.setForeground(new Color(0x1E293B));
        chkMaintenance.setOpaque(false);
        
        JLabel warning = htmlLabel("<span style='color:#EF4444; line-height:1.4'>"
            + "• Blocks all Employee & Customer logins.<br>"
            + "• Use only during system updates.</span>", 11);
        
        JButton btnBackup = new JButton("  Backup Database") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, new Color(0x334155), 0, getHeight(), new Color(0x0F172A)));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnBackup.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnBackup.setForeground(Color.WHITE);
        btnBackup.setContentAreaFilled(false);
        btnBackup.setBorderPainted(false);
        btnBackup.setFocusPainted(false);
        btnBackup.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnBackup.setPreferredSize(new Dimension(180, 32));
        btnBackup.addActionListener(e -> {
            String path = util.FileManager.backupDatabase();
            if (path != null) {
                JOptionPane.showMessageDialog(this, "Backup created at:\n" + path, "Backup Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Backup failed! Ensure DB is not locked.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        p.add(chkMaintenance);
        p.add(vGap(8));
        p.add(warning);
        p.add(vGap(15));
        p.add(btnBackup);
        p.add(Box.createVerticalGlue());
        card.add(p, BorderLayout.CENTER);
        return card;
    }

    // ── LEFT: Slot Management ─────────────────────────────────────────────────
    private JPanel buildSlotSection() {
        JPanel p = sectionPanel(16, 25);

        p.add(sectionTitle("Slot Management", CAR_DOT));
        p.add(vGap(14));

        int[] counts = getCurrentSlotCounts();
        txtCarSlots  = inputField(props.getProperty("carSlots",  String.valueOf(counts[0])));
        txtBikeSlots = inputField(props.getProperty("bikeSlots", String.valueOf(counts[1])));

        p.add(fieldGroup("Car slots count",  badgeInput("CAR",  CAR_BG,  CAR_FG,  txtCarSlots)));
        p.add(vGap(12));
        p.add(fieldGroup("Bike slots count", badgeInput("BIKE", BIKE_BG, BIKE_FG, txtBikeSlots)));
        p.add(vGap(14));

        JLabel hint = htmlLabel("<span style='color:#718096; line-height:1.4'>"
            + "• Occupied slots are always preserved.<br>"
            + "• Free slots are recreated when you save.</span>", 11);
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(hint);
        p.add(Box.createVerticalGlue());
        return p;
    }

    // ── RIGHT: Pricing ────────────────────────────────────────────────────────
    private JPanel buildPricingSection() {
        JPanel p = sectionPanel(16, 25);

        p.add(sectionTitle("Pricing Configuration", PRICE_DOT));
        p.add(vGap(14));

        txtCarRate  = inputField(props.getProperty("carRatePerHour",  "100"));
        txtBikeRate = inputField(props.getProperty("bikeRatePerHour", "50"));
        txtTax      = inputField(props.getProperty("taxPercentage",   "10"));

        p.add(fieldGroup("Car — Rate per hour",  badgeInput("Rs.", RS_BG,  RS_FG,  txtCarRate)));
        p.add(vGap(12));
        p.add(fieldGroup("Bike — Rate per hour", badgeInput("Rs.", RS_BG,  RS_FG,  txtBikeRate)));
        p.add(vGap(12));
        p.add(fieldGroup("Tax percentage",       badgeInput("%",   PCT_BG, PCT_FG, txtTax)));
        p.add(Box.createVerticalGlue());
        return p;
    }

    // ── Card 2: Live Summary ──────────────────────────────────────────────────
    private JPanel buildSummaryCard() {
        int[] counts = getCurrentSlotCounts();
        int[] occ    = getOccupiedCounts();
        int carFree  = Math.max(0, counts[0] - occ[0]);
        int bikeFree = Math.max(0, counts[1] - occ[1]);

        JPanel card = roundedCard(16);
        card.setLayout(new BorderLayout());

        // Card header — Navy gradient bar
        JPanel hdr = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, new Color(0x1E3A5F), getWidth(), 0, new Color(0x0F172A)));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.fillRect(0, getHeight()/2, getWidth(), getHeight()/2);
                g2.setColor(new Color(34, 211, 238, 120));
                g2.fillRect(0, getHeight()-1, getWidth(), 1);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        hdr.setOpaque(false);
        hdr.setBorder(new EmptyBorder(10, 18, 10, 18));
        JLabel hdrLbl = new JLabel("LIVE SLOT SUMMARY");
        hdrLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        hdrLbl.setForeground(Color.WHITE);
        hdr.add(hdrLbl, BorderLayout.WEST);
        card.add(hdr, BorderLayout.NORTH);

        // Divider under header
        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER_CLR);
        card.add(sep, BorderLayout.CENTER);

        // Stat cells grid
        JPanel grid = new JPanel(new GridLayout(2, 3, 1, 1));
        grid.setBackground(new Color(0xE2E8F0));
        grid.setBorder(BorderFactory.createEmptyBorder(1, 0, 0, 0));

        int carRows = (counts[0] + 9) / 10;
        char carLast = (char)('A' + Math.max(0, carRows - 1));
        String carRange = counts[0] > 0 ? " (A-" + carLast + ")" : "";
        
        char bikeStart = (char)('A' + carRows);
        int bRows = (counts[1] + 9) / 10;
        char bikeLast = (char)(bikeStart + Math.max(0, bRows - 1));
        String bikeRange = counts[1] > 0 ? " (" + bikeStart + "-" + bikeLast + ")" : "";

        grid.add(statCell("Car Total",     counts[0] + carRange, CAR_DOT,    false, true));
        grid.add(statCell("Car Free",      String.valueOf(carFree),   FREE_CLR,   false, true));
        grid.add(statCell("Car Occupied",  String.valueOf(occ[0]),    OCC_CLR,    false, false));
        grid.add(statCell("Bike Total",    counts[1] + bikeRange, BIKE_DOT,   true,  true));
        grid.add(statCell("Bike Free",     String.valueOf(bikeFree),  FREE_CLR,   true,  true));
        grid.add(statCell("Bike Occupied", String.valueOf(occ[1]),    OCC_CLR,    true,  false));

        card.add(grid, BorderLayout.SOUTH);
        return card;
    }

    // ── Footer row ────────────────────────────────────────────────────────────
    private JPanel buildFooterRow() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));

        GradientButton saveBtn = new GradientButton("Save All Settings",
            BTN_G1, BTN_G2, BTN_HOVER_G1, BTN_HOVER_G2);
        saveBtn.addActionListener(e -> saveAll());
        p.add(saveBtn);
        return p;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  GRADIENT BUTTON (inner class)
    // ═════════════════════════════════════════════════════════════════════════
    private static class GradientButton extends JButton {
        private final Color g1, g2, hg1, hg2;
        private boolean hovered = false;

        GradientButton(String text, Color g1, Color g2, Color hg1, Color hg2) {
            super(text);
            this.g1 = g1; this.g2 = g2; this.hg1 = hg1; this.hg2 = hg2;
            setFont(new Font("Segoe UI", Font.BOLD, 13));
            setForeground(Color.WHITE);
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(10, 28, 10, 28));
            addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent e) { hovered = true;  repaint(); }
                public void mouseExited (java.awt.event.MouseEvent e) { hovered = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color from = hovered ? hg1 : g1;
            Color to   = hovered ? hg2 : g2;
            GradientPaint gp = new GradientPaint(0, 0, from, getWidth(), getHeight(), to);
            g2d.setPaint(gp);
            g2d.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));

            // Subtle inner highlight line at top
            g2d.setColor(new Color(255, 255, 255, 40));
            g2d.setStroke(new BasicStroke(1f));
            g2d.drawLine(6, 1, getWidth() - 6, 1);

            g2d.dispose();
            super.paintComponent(g);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  SAVE LOGIC
    // ═════════════════════════════════════════════════════════════════════════
    private void saveAll() {
        int carTarget, bikeTarget;
        double carRate, bikeRate, tax;
        try {
            carTarget  = Integer.parseInt(txtCarSlots.getText().trim());
            bikeTarget = Integer.parseInt(txtBikeSlots.getText().trim());
            carRate    = Double.parseDouble(txtCarRate.getText().trim());
            bikeRate   = Double.parseDouble(txtBikeRate.getText().trim());
            tax        = Double.parseDouble(txtTax.getText().trim());
            if (carTarget < 0 || bikeTarget < 0 || carRate < 0 || bikeRate < 0 || tax < 0)
                throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                "Please enter valid positive numbers in all fields.",
                "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            "<html><b>Confirm Save?</b><br><br>"
            + "Car Slots: <b>" + carTarget + "</b> &nbsp;|&nbsp; Bike Slots: <b>" + bikeTarget + "</b><br>"
            + "Car Rate: <b>Rs. " + carRate + "/hr</b> &nbsp;|&nbsp; Bike Rate: <b>Rs. " + bikeRate + "/hr</b><br>"
            + "Tax: <b>" + tax + "%</b></html>",
            "Confirm Save", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            props.setProperty("carSlots",        String.valueOf(carTarget));
            props.setProperty("bikeSlots",       String.valueOf(bikeTarget));
            props.setProperty("carRatePerHour",  String.valueOf(carRate));
            props.setProperty("bikeRatePerHour", String.valueOf(bikeRate));
            props.setProperty("taxPercentage",   String.valueOf(tax));
            props.setProperty("maintenanceMode", String.valueOf(chkMaintenance.isSelected()));
            props.setProperty("ratePerHour",     String.valueOf(carRate)); // legacy
            new File(util.FileManager.BASE_DIR + "data").mkdirs();
            props.store(new FileOutputStream(settingsFile), "ParkNova Settings");
            util.Settings.load(); // Refresh global settings cache

            Connection conn = DatabaseManager.getInstance().getConnection();
            conn.createStatement().execute("PRAGMA foreign_keys = ON");
            
            // Sync Slots: This logic ensures we have exactly carTarget and bikeTarget slots
            // Dynamically calculate starting rows to avoid overlaps
            syncSlots(conn, "car",  carTarget,  'A');
            char bikeStart = (char)('A' + (carTarget + 9) / 10);
            syncSlots(conn, "bike", bikeTarget, bikeStart);

            JOptionPane.showMessageDialog(this,
                "<html><b>Settings saved successfully!</b><br><br>"
                + "Car: " + carTarget + " slots @ Rs." + carRate + "/hr<br>"
                + "Bike: " + bikeTarget + " slots @ Rs." + bikeRate + "/hr<br>"
                + "Tax: " + tax + "%</html>",
                "Saved", JOptionPane.INFORMATION_MESSAGE);

            removeAll();
            loadSettings();
            buildUI();
            revalidate();
            repaint();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Error saving settings: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  DB HELPERS
    // ═════════════════════════════════════════════════════════════════════════
    private void syncSlots(Connection conn, String type, int target, char startRow) throws SQLException {
        // High safety limit (e.g., 200 per type)
        int maxSlots = 200;
        if (target > maxSlots) target = maxSlots;

        // 1. Delete extra FREE slots of THIS TYPE if target is smaller
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT slot_id, slot_number FROM parking_slots WHERE vehicle_type=? AND status='free'")) {
            ps.setString(1, type);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String slotNum = rs.getString("slot_number");
                int slotIndex = getIndexFromSlotName(slotNum, startRow);
                if (slotIndex >= target) {
                    try (PreparedStatement del = conn.prepareStatement("DELETE FROM parking_slots WHERE slot_id=?")) {
                        del.setInt(1, rs.getInt("slot_id"));
                        del.executeUpdate();
                    }
                }
            }
        }

        // 2. Add or RE-CLAIM missing slots up to target
        for (int i = 0; i < target; i++) {
            char rowChar = (char)(startRow + (i / 10));
            int num = (i % 10) + 1;
            String name = rowChar + String.valueOf(num);
            
            // 2. Add or RE-CLAIM missing slots up to target (Robustly)
            boolean exists = false;
            try (PreparedStatement check = conn.prepareStatement("SELECT slot_id, vehicle_type, status FROM parking_slots WHERE slot_number=?")) {
                check.setString(1, name);
                try (ResultSet rs = check.executeQuery()) {
                    if (rs.next()) {
                        exists = true;
                        int existingId = rs.getInt("slot_id");
                        String existingType = rs.getString("vehicle_type");
                        String status = rs.getString("status");

                        // CLEANUP DUPLICATES: If we find multiple, delete all but the first one
                        try (PreparedStatement clean = conn.prepareStatement("DELETE FROM parking_slots WHERE slot_number=? AND slot_id != ?")) {
                            clean.setString(1, name); clean.setInt(2, existingId); clean.executeUpdate();
                        }

                        // RE-CLAIM: If it's free but wrong type (e.g. from a previous row allocation), fix it
                        if (!type.equals(existingType) && "free".equals(status)) {
                            try (PreparedStatement up = conn.prepareStatement("UPDATE parking_slots SET vehicle_type=? WHERE slot_id=?")) {
                                up.setString(1, type); up.setInt(2, existingId); up.executeUpdate();
                            }
                        }
                    }
                }
            }

            if (!exists) {
                try (PreparedStatement ins = conn.prepareStatement(
                        "INSERT INTO parking_slots (floor_number, slot_number, slot_type, vehicle_type, status) VALUES (?,?,?,?,?)")) {
                    ins.setInt(1, 1); ins.setString(2, name);
                    ins.setString(3, "standard"); ins.setString(4, type);
                    ins.setString(5, "free"); ins.executeUpdate();
                }
            }
        }
    }

    private int getIndexFromSlotName(String name, char startRow) {
        try {
            char row = name.charAt(0);
            int num = Integer.parseInt(name.substring(1));
            int rowIndex = row - startRow;
            return (rowIndex * 10) + (num - 1);
        } catch (Exception e) { return 0; }
    }

    private int getOccupied(Connection conn, String type) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM parking_slots WHERE vehicle_type=? AND status!='free'")) {
            ps.setString(1, type);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private int[] getCurrentSlotCounts() {
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            int car = 0, bike = 0;
            ResultSet r1 = conn.createStatement()
                .executeQuery("SELECT COUNT(*) FROM parking_slots WHERE vehicle_type='car'");
            if (r1.next()) car = r1.getInt(1);
            ResultSet r2 = conn.createStatement()
                .executeQuery("SELECT COUNT(*) FROM parking_slots WHERE vehicle_type='bike'");
            if (r2.next()) bike = r2.getInt(1);
            return new int[]{car, bike};
        } catch (Exception e) { return new int[]{0, 0}; }
    }

    private int[] getOccupiedCounts() {
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            return new int[]{getOccupied(conn, "car"), getOccupied(conn, "bike")};
        } catch (Exception e) { return new int[]{0, 0}; }
    }

    private void loadSettings() {
        try {
            if (settingsFile.exists()) {
                props.load(new FileInputStream(settingsFile));
            } else {
                props.setProperty("carSlots",        "20");
                props.setProperty("bikeSlots",       "20");
                props.setProperty("carRatePerHour",  "100");
                props.setProperty("bikeRatePerHour", "50");
                props.setProperty("taxPercentage",   "10");
                new File(util.FileManager.BASE_DIR + "data").mkdirs();
                props.store(new FileOutputStream(settingsFile), "ParkNova Settings");
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  UI HELPERS
    // ═════════════════════════════════════════════════════════════════════════

    /** White card with rounded border and subtle shadow-like compound border */
    private JPanel roundedCard(int radius) {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Soft shadow layer
                g2.setColor(new Color(0, 0, 0, 12));
                g2.fill(new RoundRectangle2D.Float(2, 3, getWidth()-4, getHeight()-2, radius+2, radius+2));
                // Card face
                g2.setColor(CARD_BG);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth()-1, getHeight()-2, radius, radius));
                // Border
                g2.setColor(BORDER_CLR);
                g2.setStroke(new BasicStroke(0.8f));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth()-1, getHeight()-2, radius, radius));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        p.setOpaque(false);
        p.setBackground(CARD_BG);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return p;
    }

    private JPanel sectionPanel(int vPad, int hPad) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(vPad, hPad, vPad, hPad));
        return p;
    }

    private JPanel sectionTitle(String text, Color dotColor) {
        JPanel row = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, new Color(0x1E3A5F), getWidth(), 0, new Color(0x0F172A)));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(new Color(34, 211, 238, 120));
                g2.fillRect(0, getHeight()-1, getWidth(), 1);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setBorder(new EmptyBorder(8, 14, 8, 14));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(Color.WHITE);
        row.add(lbl, BorderLayout.WEST);
        return row;
    }

    private JPanel fieldGroup(String labelText, JPanel inputWidget) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lbl = new JLabel(labelText);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(TEXT_LABEL);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(lbl);
        p.add(Box.createVerticalStrut(5));
        inputWidget.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(inputWidget);
        return p;
    }

    /** Badge prefix + text field, with modern rounded border and white background */
    private JPanel badgeInput(String badge, Color badgeBg, Color badgeFg, JTextField field) {
        JPanel p = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Background
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);
                // Border
                g2.setColor(BORDER_CLR);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        p.setPreferredSize(new Dimension(200, 42));

        JLabel badgeLbl = new JLabel(badge);
        badgeLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        badgeLbl.setForeground(badgeFg);
        badgeLbl.setBackground(badgeBg);
        badgeLbl.setOpaque(true);
        badgeLbl.setHorizontalAlignment(SwingConstants.CENTER);
        badgeLbl.setPreferredSize(new Dimension(50, 42));
        badgeLbl.setBorder(new CompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER_CLR),
            new EmptyBorder(0, 5, 0, 5)));

        field.setBorder(new EmptyBorder(0, 15, 0, 15));
        field.setBackground(new Color(0, 0, 0, 0)); // Transparent to show parent white
        field.setOpaque(false);
        field.setFont(new Font("Segoe UI", Font.BOLD, 15));
        field.setForeground(Theme.TEXT_DARK);
        field.setCaretColor(Theme.TEXT_DARK);

        p.add(badgeLbl, BorderLayout.WEST);
        p.add(field,    BorderLayout.CENTER);
        
        // Add hover effect via parent panel
        p.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                p.setCursor(new Cursor(Cursor.TEXT_CURSOR));
            }
        });
        
        return p;
    }

    private JTextField inputField(String text) {
        JTextField f = new JTextField(text);
        f.setFont(new Font("Segoe UI", Font.BOLD, 15));
        f.setForeground(Theme.TEXT_DARK);
        f.setBorder(null);
        f.setHorizontalAlignment(SwingConstants.LEFT);
        return f;
    }

    private JLabel htmlLabel(String bodyHtml, int size) {
        JLabel l = new JLabel("<html><span style='font-size:" + size + "px'>" + bodyHtml + "</span></html>");
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    /** 1-px vertical divider */
    private JPanel vDivider() {
        JPanel d = new JPanel();
        d.setBackground(BORDER_CLR);
        d.setPreferredSize(new Dimension(1, 0));
        d.setOpaque(true);
        return d;
    }

    private Component vGap(int h) { return Box.createVerticalStrut(h); }

    /**
     * Single stat cell for summary strip.
     * @param topBorder  true for second row (bike) to show a top separator
     * @param rightBorder true unless last in row
     */
    private JPanel statCell(String label, String value, Color valueColor,
                             boolean topBorder, boolean rightBorder) {
        JPanel c = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0xF8FAFC));
                g2.fillRoundRect(4, 4, getWidth()-8, getHeight()-8, 10, 10);
                g2.setColor(new Color(0xE2E8F0));
                g2.drawRoundRect(4, 4, getWidth()-8, getHeight()-8, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        c.setOpaque(false);
        c.setBorder(new EmptyBorder(14, 8, 14, 8));

        JLabel val = new JLabel(value);
        val.setFont(new Font("Segoe UI", Font.BOLD, 22));
        val.setForeground(valueColor);
        val.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lbl = new JLabel(label.toUpperCase());
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 9));
        lbl.setForeground(TEXT_HINT);
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        c.add(val);
        c.add(Box.createVerticalStrut(3));
        c.add(lbl);
        return c;
    }
}