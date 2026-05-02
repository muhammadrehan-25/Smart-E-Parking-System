package ui;

import dao.BookingDAO;
import model.ParkingSlot;
import util.Theme;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CheckInPanel extends JPanel {
    private final BookingDAO bookingDAO = new BookingDAO();
    private final int operatorId;

    private JLabel totalSlotsLabel, availableSlotsLabel;
    private JLabel availableBikeLabel, availableCarLabel;
    private JTextField plateField, ownerField, contactField;
    private JComboBox<String> vehicleTypeCombo;

    public CheckInPanel() { this(1); }

    public CheckInPanel(int operatorId) {
        this.operatorId = operatorId;
        setLayout(new BorderLayout(0, 20));
        setOpaque(true);
        setBackground(Theme.BG_WHITE);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        buildUI();
        refreshData();
    }

    private void buildUI() {
        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        gbc.insets = new Insets(0, 15, 0, 15);

        // Slots Cards on the left (Weight 0.4)
        gbc.gridx = 0; 
        gbc.weightx = 0.4; 
        content.add(buildStatusColumn(), gbc);

        // Form Card on the right (Reduced Weight 0.45)
        gbc.gridx = 1; 
        gbc.weightx = 0.45; 
        // Adding a wrapper to center/narrow the form even more
        JPanel formWrap = new JPanel(new BorderLayout());
        formWrap.setOpaque(false);
        formWrap.add(buildFormCard(), BorderLayout.CENTER);
        content.add(formWrap, gbc);

        // Spacer on the far right (Weight 0.15)
        gbc.gridx = 2;
        gbc.weightx = 0.15;
        content.add(Box.createGlue(), gbc);

        add(content, BorderLayout.CENTER);
    }

    private JPanel buildStatusColumn() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        totalSlotsLabel     = new JLabel("0");
        availableSlotsLabel = new JLabel("0");
        availableBikeLabel  = new JLabel("0");
        availableCarLabel   = new JLabel("0");

        JPanel c1 = miniStatCard("Total Slots",     totalSlotsLabel,     Theme.ACCENT_BLUE);
        JPanel c2 = miniStatCard("Available Slots", availableSlotsLabel, Theme.FREE_GREEN);
        JPanel c3 = miniStatCard("Bike Slots Free", availableBikeLabel,  Theme.AMBER);
        JPanel c4 = miniStatCard("Car Slots Free",  availableCarLabel,   Theme.ACC_PURPLE);

        // Reduced height to fit 4 cards without cutting
        Dimension cardSize = new Dimension(Integer.MAX_VALUE, 135);
        for (JPanel c : new JPanel[]{c1, c2, c3, c4}) {
            c.setMaximumSize(cardSize);
            c.setPreferredSize(cardSize);
            c.setMinimumSize(new Dimension(0, 110));
        }

        p.add(Box.createVerticalGlue());
        p.add(c1);
        p.add(Box.createVerticalStrut(12));
        p.add(c2);
        p.add(Box.createVerticalStrut(12));
        p.add(c3);
        p.add(Box.createVerticalStrut(12));
        p.add(c4);
        p.add(Box.createVerticalGlue());
        
        return p;
    }

    private JPanel buildFormCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Theme.BG_CARD);
        card.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 2));

        JPanel cardHeader = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, new Color(0x1E3A5F), getWidth(), 0, new Color(0x0F172A)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(34, 211, 238, 120));
                g2.fillRect(0, getHeight()-1, getWidth(), 1);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        cardHeader.setOpaque(false);
        cardHeader.setBorder(BorderFactory.createEmptyBorder(14, 24, 14, 24));
        cardHeader.add(Theme.label("Enter Vehicle Details", new Font("Segoe UI", Font.BOLD, 16), Color.WHITE), BorderLayout.WEST);
        card.add(cardHeader, BorderLayout.NORTH);

        JPanel formBody = new JPanel(new GridBagLayout());
        formBody.setBackground(Theme.BG_CARD);
        formBody.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(6, 0, 6, 0);
        g.gridx = 0; g.weightx = 1.0;

        g.gridy = 0; formBody.add(fieldLabel("Vehicle Plate Number"), g);
        g.gridy = 1; plateField = styledField("e.g. ABC-1234"); formBody.add(plateField, g);
        g.gridy = 2; formBody.add(fieldLabel("Owner Name"), g);
        g.gridy = 3; ownerField = styledField("e.g. Ahmed Khan"); formBody.add(ownerField, g);
        g.gridy = 4; formBody.add(fieldLabel("Contact Number"), g);
        g.gridy = 5; contactField = styledField("e.g. 0300-1234567"); formBody.add(contactField, g);
        g.gridy = 6; formBody.add(fieldLabel("Vehicle Type"), g);
        g.gridy = 7;
        vehicleTypeCombo = new JComboBox<>(new String[]{"Car", "Bike"});
        vehicleTypeCombo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        vehicleTypeCombo.setPreferredSize(new Dimension(0, 42));
        vehicleTypeCombo.setBackground(Color.WHITE);
        formBody.add(vehicleTypeCombo, g);

        // Enter key listeners
        java.awt.event.KeyAdapter enter = new java.awt.event.KeyAdapter() {
            @Override public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) handleCheckIn();
            }
        };
        plateField.addKeyListener(enter);
        ownerField.addKeyListener(enter);
        contactField.addKeyListener(enter);
        vehicleTypeCombo.addKeyListener(enter);

        g.gridy = 8; g.insets = new Insets(24, 0, 8, 0);
        JButton btn = gradientBtn("Allow Entry", new Color(0x0EA5E9), new Color(0x0369A1));
        btn.addActionListener(e -> handleCheckIn());
        formBody.add(btn, g);

        card.add(formBody, BorderLayout.CENTER);
        return card;
    }

    private JPanel miniStatCard(String title, JLabel val, Color accent) {
        Color hoverBg    = blendWithWhite(accent, 0.10f);
        Color thinBorder = blendWithWhite(accent, 0.35f);
        Border border = BorderFactory.createCompoundBorder(
            new LeftThickBorder(accent, thinBorder, 4),
            BorderFactory.createEmptyBorder(14, 20, 14, 20)
        );

        JPanel card = new JPanel(new BorderLayout(0, 12)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 12, 12);
                g2.setColor(new Color(0xE2E8F0));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setBackground(Color.WHITE);
        card.setOpaque(false);
        card.setBorder(border);

        JLabel t = new JLabel(title, SwingConstants.CENTER);
        t.setFont(new Font("Segoe UI", Font.BOLD, 12));
        t.setForeground(new Color(0x64748B));

        val.setFont(new Font("Segoe UI", Font.BOLD, 48));
        val.setForeground(accent);
        val.setHorizontalAlignment(SwingConstants.CENTER);

        card.add(t,   BorderLayout.NORTH);
        card.add(val, BorderLayout.CENTER);

        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { card.setBackground(hoverBg);     card.repaint(); }
            @Override public void mouseExited (MouseEvent e) { card.setBackground(Color.WHITE); card.repaint(); }
        });

        return card;
    }

    private void handleCheckIn() {
        String plate   = getFieldText(plateField,   "e.g. ABC-1234");
        String owner   = getFieldText(ownerField,   "e.g. Ahmed Khan");
        String contact = getFieldText(contactField, "e.g. 0300-1234567");
        String vType   = ((String) vehicleTypeCombo.getSelectedItem()).toLowerCase();

        if (plate.isEmpty())   { showError("Please enter vehicle plate number."); return; }
        if (owner.isEmpty())   { showError("Please enter owner name.");           return; }
        if (contact.isEmpty()) { showError("Please enter contact number.");       return; }

        if (bookingDAO.findActiveByPlate(plate.toUpperCase()) != null) {
            showError("This vehicle is already inside the parking!"); return;
        }

        ParkingSlot slot = bookingDAO.findFreeSlotByVehicleType(vType);
        if (slot == null) { showParkingFull(vType); return; }

        int vId = bookingDAO.saveVehicleIfNew(plate.toUpperCase(), vType, 1);
        int bId = bookingDAO.createBookingWithDetails(vId, slot.getSlotId(), operatorId,
                plate.toUpperCase(), slot.getSlotNumber(), owner, contact);

        if (bId > 0) {
            String conf = "EP" + String.format("%04d", bId);
            showThermalReceipt(plate.toUpperCase(), owner, contact,
                    vType, slot.getSlotNumber(), conf);
            clearForm();
            refreshData();
            Window win = SwingUtilities.getWindowAncestor(this);
            if (win instanceof AdminDashboard d) d.refreshData();
            else if (win instanceof EmployeeDashboard d) d.refreshData();
        } else {
            showError("Entry failed. Please try again.");
        }
    }

    private void showThermalReceipt(String plate, String owner, String contact,
                                    String vType, String slotNum, String conf) {
        String now = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"));
        String row = slotNum.length() > 0 ? slotNum.substring(0, 1) : "-";
        String receipt = buildReceiptText(plate, owner, contact, vType, slotNum, row, conf, now);

        JTextArea ta = new JTextArea(receipt);
        ta.setFont(new Font("Courier New", Font.PLAIN, 12));
        ta.setEditable(false);
        ta.setBackground(new Color(0xFFFEF5));
        ta.setForeground(new Color(0x1a1a1a));
        ta.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        ta.setLineWrap(false);

        JScrollPane scroll = new JScrollPane(ta);
        scroll.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, new Color(0xD0C8A0)));
        scroll.setPreferredSize(new Dimension(370, 360));
        scroll.getViewport().setBackground(new Color(0xFFFEF5));

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(new Color(0xFFFEF5));
        wrap.add(scroll, BorderLayout.CENTER);

        JButton btnPrint = new JButton("🖨  Print");
        btnPrint.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnPrint.setForeground(new Color(0x2563eb));
        btnPrint.setBackground(Color.WHITE);
        btnPrint.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x2563eb), 1, true),
            BorderFactory.createEmptyBorder(6, 16, 6, 16)));
        btnPrint.setFocusPainted(false);
        btnPrint.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JButton btnOK = new JButton("OK") {
            @Override protected void paintComponent(Graphics gr) {
                Graphics2D g2 = (Graphics2D) gr.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0x0f1d35));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();
                super.paintComponent(gr);
            }
        };
        btnOK.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnOK.setForeground(Color.WHITE);
        btnOK.setContentAreaFilled(false);
        btnOK.setOpaque(false);
        btnOK.setBorderPainted(false);
        btnOK.setBorder(BorderFactory.createEmptyBorder(7, 26, 7, 26));
        btnOK.setFocusPainted(false);
        btnOK.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        btnPanel.setBackground(new Color(0xFFFEF5));
        btnPanel.add(btnPrint);
        btnPanel.add(btnOK);
        wrap.add(btnPanel, BorderLayout.SOUTH);

        JDialog dialog = new JDialog(
            (Frame) SwingUtilities.getWindowAncestor(this),
            "Entry — " + conf, true);
        dialog.setContentPane(wrap);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.getRootPane().setDefaultButton(btnOK);

        btnOK.addActionListener(e -> dialog.dispose());
        btnPrint.addActionListener(e -> {
            try { ta.print(); } catch (Exception ex) { /* ignore */ }
        });

        dialog.setVisible(true);
    }

    private String buildReceiptText(String plate, String owner, String contact,
                                    String vType, String slot, String row,
                                    String conf, String checkIn) {
        int W = 38;
        String LINE = "=".repeat(W);
        String DASH = "-".repeat(W);

        return LINE + "\n"
             + center("PARKNOVA SYSTEM", W) + "\n"
             + center("Parking Receipt", W) + "\n"
             + LINE + "\n"
             + col("Ref :", conf, W) + "\n"
             + DASH + "\n"
             + col("Plate  :", plate.toUpperCase(), W) + "\n"
             + col("Type   :", capitalize(vType), W) + "\n"
             + col("Slot   :", slot, W) + "\n"
             + col("Row    :", row, W) + "\n"
             + col("Owner  :", owner, W) + "\n"
             + col("Contact:", contact, W) + "\n"
             + DASH + "\n"
             + col("Check-In :", checkIn, W) + "\n"
             + col("Check-Out:", "Pending", W) + "\n"
             + col("Duration :", "—", W) + "\n"
             + DASH + "\n"
             + col("Confirm  :", conf, W) + "\n"
             + LINE + "\n"
             + center("STATUS: ENTRY ALLOWED", W) + "\n"
             + LINE + "\n"
             + center("** THANK YOU **", W) + "\n"
             + center("Drive safely!", W) + "\n"
             + LINE;
    }

    private void showParkingFull(String vType) {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setBackground(new Color(0xFFF3CD));
        p.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));
        JLabel ico = new JLabel("PARKING FULL", SwingConstants.CENTER);
        ico.setFont(new Font("Segoe UI", Font.BOLD, 18));
        ico.setForeground(new Color(0xC0392B));
        JLabel msg = new JLabel("<html><center>No " + vType.toUpperCase() +
            " slots available right now.<br>Vehicle cannot enter. Please try later.</center></html>",
            SwingConstants.CENTER);
        msg.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        p.add(ico, BorderLayout.NORTH);
        p.add(msg, BorderLayout.CENTER);
        JOptionPane.showMessageDialog(this, p, "No Slots Available", JOptionPane.WARNING_MESSAGE);
    }

    public void refreshData() {
        try {
            java.util.List<ParkingSlot> all = bookingDAO.getAllSlots();
            int total    = all.size();
            int free     = (int) all.stream().filter(ParkingSlot::isFree).count();
            int freeCar  = (int) all.stream().filter(s -> s.isFree() && "car".equals(s.getVehicleType())).count();
            int freeBike = (int) all.stream().filter(s -> s.isFree() && "bike".equals(s.getVehicleType())).count();
            totalSlotsLabel.setText(String.valueOf(total));
            availableSlotsLabel.setText(String.valueOf(free));
            availableCarLabel.setText(String.valueOf(freeCar));
            availableBikeLabel.setText(String.valueOf(freeBike));
        } catch (Exception ex) {
            System.err.println("CheckIn refresh: " + ex.getMessage());
        }
    }

    private Color blendWithWhite(Color c, float ratio) {
        int r = (int)(255 + (c.getRed()   - 255) * ratio);
        int g = (int)(255 + (c.getGreen() - 255) * ratio);
        int b = (int)(255 + (c.getBlue()  - 255) * ratio);
        return new Color(Math.max(0,Math.min(255,r)), Math.max(0,Math.min(255,g)), Math.max(0,Math.min(255,b)));
    }

    private String col(String key, String val, int w) {
        int gap = w - key.length() - val.length();
        return key + " ".repeat(Math.max(1, gap)) + val;
    }

    private String center(String s, int w) {
        if (s.length() >= w) return s;
        int pad = (w - s.length()) / 2;
        return " ".repeat(pad) + s;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private JLabel fieldLabel(String t) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("Segoe UI", Font.BOLD, 13));
        l.setForeground(Theme.TEXT_DARK);
        return l;
    }

    private JTextField styledField(String ph) {
        JTextField f = new JTextField();
        f.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        f.setPreferredSize(new Dimension(0, 42));
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));
        f.setBackground(Color.WHITE);
        f.setText(ph);
        f.setForeground(Theme.TEXT_MUTED);
        f.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) {
                f.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0x0EA5E9)),
                    BorderFactory.createEmptyBorder(6, 12, 6, 12)));
                if (f.getText().equals(ph)) { f.setText(""); f.setForeground(Theme.TEXT_DARK); }
            }
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                f.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Theme.BORDER),
                    BorderFactory.createEmptyBorder(6, 12, 6, 12)));
                if (f.getText().isEmpty()) { f.setText(ph); f.setForeground(Theme.TEXT_MUTED); }
            }
        });
        return f;
    }

    private JButton gradientBtn(String label, Color c1, Color c2) {
        JButton b = new JButton(label) {
            @Override protected void paintComponent(Graphics gr) {
                Graphics2D g2 = (Graphics2D) gr.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, c1, 0, getHeight(), c2));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(gr);
            }
        };
        b.setContentAreaFilled(false); b.setOpaque(false);
        b.setBorderPainted(false); b.setFocusPainted(false);
        b.setFont(new Font("Segoe UI", Font.BOLD, 15));
        b.setForeground(Color.WHITE);
        b.setPreferredSize(new Dimension(0, 48));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private String getFieldText(JTextField f, String ph) {
        String t = f.getText().trim();
        return t.equals(ph) ? "" : t;
    }

    private void clearForm() {
        setPlaceholder(plateField,   "e.g. ABC-1234");
        setPlaceholder(ownerField,   "e.g. Ahmed Khan");
        setPlaceholder(contactField, "e.g. 0300-1234567");
        vehicleTypeCombo.setSelectedIndex(0);
    }

    private void setPlaceholder(JTextField f, String ph) {
        f.setText(ph);
        f.setForeground(Theme.TEXT_MUTED);
    }

    private void showError(String m) {
        JOptionPane.showMessageDialog(this, m, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private static class LeftThickBorder implements Border {
        private final Color leftColor, otherColor;
        private final int leftThick;

        LeftThickBorder(Color leftColor, Color otherColor, int leftThick) {
            this.leftColor  = leftColor;
            this.otherColor = otherColor;
            this.leftThick  = leftThick;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(leftColor);
            g2.fillRect(x, y, leftThick, h);
            g2.setColor(otherColor);
            g2.fillRect(x + leftThick, y, w - leftThick, 1);
            g2.fillRect(x + leftThick, y + h - 1, w - leftThick, 1);
            g2.fillRect(x + w - 1, y, 1, h);
            g2.dispose();
        }

        @Override public Insets getBorderInsets(Component c) { return new Insets(1, leftThick, 1, 1); }
        @Override public boolean isBorderOpaque() { return true; }
    }
}