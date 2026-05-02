package ui;
import dao.BookingDAO;
import model.ParkingSlot;
import model.Booking;
import util.Theme;
import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Parking Map Panel
 * - Cars / Bikes separate tabs
 * - No emojis (text labels only)
 * - Only Available / Occupied (no Reserved display)
 * - Auto-refresh every 5 seconds from DB
 * - Slots wrap to window width automatically
 * - Small row badges so more slots fit per row
 */
public class ParkingMapPanel extends JPanel {

    private final BookingDAO bookingDAO = new BookingDAO();
    private final int operatorId;
    private List<ParkingSlot> allSlots  = new ArrayList<>();
    private String selectedType = "car";
    private boolean readOnly    = false;

    private JPanel  mapArea;
    private JButton btnCar, btnBike;
    private JLabel  lblCarFree, lblCarOcc, lblBikeFree, lblBikeOcc;
    private javax.swing.Timer autoRefresh;

    public ParkingMapPanel() { this(1, false); }
    public ParkingMapPanel(boolean ro) { this(1, ro); }
    public ParkingMapPanel(int operatorId) { this(operatorId, false); }

    public ParkingMapPanel(int operatorId, boolean ro) {
        this.operatorId = operatorId;
        this.readOnly = ro;
        setLayout(new BorderLayout(0, 0));
        setOpaque(true);
        setBackground(Theme.BG_WHITE);
        build();
        refresh();
        startAutoRefresh();
    }

    // ── Auto refresh every 5 seconds ─────────────────────────────────────────
    private void startAutoRefresh() {
        autoRefresh = new javax.swing.Timer(5000, e -> refresh());
        autoRefresh.setRepeats(true);
        autoRefresh.start();
    }

    public void stopAutoRefresh() {
        if (autoRefresh != null) autoRefresh.stop();
    }

    // ── Build UI ──────────────────────────────────────────────────────────────
    private void build() {

        // Top bar
        JPanel topBar = new JPanel(new BorderLayout(16, 0));
        topBar.setOpaque(false);
        topBar.setBorder(BorderFactory.createEmptyBorder(12, 18, 10, 18));

        JPanel typeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        typeRow.setOpaque(false);
        btnCar  = typeBtn("  CARS  ", "car");
        btnBike = typeBtn("  BIKES  ", "bike");
        typeRow.add(btnCar);
        typeRow.add(btnBike);
        topBar.add(typeRow, BorderLayout.WEST);

        JPanel counterRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        counterRow.setOpaque(false);
        lblCarFree  = counterBadge("Car Free: 0",  Theme.FREE_GREEN);
        lblCarOcc   = counterBadge("Car Occ: 0",   Theme.OCC_RED);
        lblBikeFree = counterBadge("Bike Free: 0", new Color(0xF59E0B));
        lblBikeOcc  = counterBadge("Bike Occ: 0",  new Color(0x7C3AED));
        counterRow.add(lblCarFree);
        counterRow.add(lblCarOcc);
        counterRow.add(lblBikeFree);
        counterRow.add(lblBikeOcc);
        topBar.add(counterRow, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        // Legend
        JPanel legendBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 24, 6));
        legendBar.setOpaque(true);
        legendBar.setBackground(new Color(0xF1F5F9));
        legendBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER));
        addLegend(legendBar, Theme.FREE_GREEN, "Available");
        addLegend(legendBar, Theme.OCC_RED,    "Occupied");
        add(legendBar, BorderLayout.SOUTH);

        // Map scroll area
        mapArea = new JPanel();
        mapArea.setOpaque(false);
        mapArea.setLayout(new BoxLayout(mapArea, BoxLayout.Y_AXIS));

        JScrollPane scroll = new JScrollPane(mapArea);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setBackground(Theme.BG_WHITE);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        selectType("car");
    }

    // ── Refresh ───────────────────────────────────────────────────────────────
    public void refresh() {
        allSlots = bookingDAO.getAllSlots();
        updateCounters();
        drawMap();
    }

    private void updateCounters() {
        long cf = allSlots.stream().filter(s -> "car".equals(s.getVehicleType())  && s.isFree()).count();
        long co = allSlots.stream().filter(s -> "car".equals(s.getVehicleType())  && !s.isFree()).count();
        long bf = allSlots.stream().filter(s -> "bike".equals(s.getVehicleType()) && s.isFree()).count();
        long bo = allSlots.stream().filter(s -> "bike".equals(s.getVehicleType()) && !s.isFree()).count();
        
        lblCarFree.setText(" Car Free: " + cf);
        lblCarOcc.setText(" Car Occ: " + co);
        lblBikeFree.setText(" Bike Free: " + bf);
        lblBikeOcc.setText(" Bike Occ: " + bo);
    }

    // ── Draw map ──────────────────────────────────────────────────────────────
    private void drawMap() {
        mapArea.removeAll();

        List<ParkingSlot> filtered = allSlots.stream()
            .filter(s -> selectedType.equals(s.getVehicleType()))
            .sorted((s1, s2) -> {
                String str1 = s1.getSlotNumber();
                String str2 = s2.getSlotNumber();
                String p1 = str1.replaceAll("[0-9]", "");
                String p2 = str2.replaceAll("[0-9]", "");
                int c = p1.compareTo(p2);
                if (c != 0) return c;
                try {
                    int n1 = Integer.parseInt(str1.replaceAll("[^0-9]", ""));
                    int n2 = Integer.parseInt(str2.replaceAll("[^0-9]", ""));
                    return Integer.compare(n1, n2);
                } catch (Exception e) {
                    return str1.compareTo(str2);
                }
            })
            .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            JPanel msg = new JPanel(new GridBagLayout());
            msg.setOpaque(false);
            msg.setPreferredSize(new Dimension(400, 200));
            JLabel lbl = new JLabel("<html><center>No " + selectedType + " slots found.<br>"
                + "Add slots in <b>Settings</b>.</center></html>");
            lbl.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            lbl.setForeground(Theme.TEXT_MUTED);
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            msg.add(lbl);
            mapArea.add(msg);
            mapArea.revalidate();
            mapArea.repaint();
            return;
        }

        boolean isBike = "bike".equals(selectedType);
        int slotW = isBike ? 60 : 74;
        int slotH = isBike ? 50 : 60;
        int gap   = 7;

        // Group by row letter
        Map<String, List<ParkingSlot>> byRow = new LinkedHashMap<>();
        for (ParkingSlot s : filtered) {
            String row = s.getSlotNumber().length() > 0
                       ? s.getSlotNumber().substring(0, 1) : "?";
            byRow.computeIfAbsent(row, k -> new ArrayList<>()).add(s);
        }

        Color[] rowColors = {
            new Color(0x1E40AF), new Color(0x065F46), new Color(0x92400E),
            new Color(0x6D28D9), new Color(0x9D174D), new Color(0x0F766E),
            new Color(0x854D0E), new Color(0x1D4ED8)
        };
        int ci = 0;

        mapArea.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        for (Map.Entry<String, List<ParkingSlot>> entry : byRow.entrySet()) {
            String rowKey           = entry.getKey();
            List<ParkingSlot> rowSl = entry.getValue();
            Color rowColor          = rowColors[ci % rowColors.length];
            ci++;

            // Row wrapper: badge on left, slots wrap on right
            JPanel rowWrapper = new JPanel(new BorderLayout(6, 0));
            rowWrapper.setOpaque(false);
            rowWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
            rowWrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

            // Row badge
            JPanel badge = new JPanel(new GridBagLayout()) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(getBackground());
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                    g2.dispose();
                }
            };
            badge.setBackground(rowColor);
            badge.setOpaque(false);
            badge.setPreferredSize(new Dimension(30, slotH));
            JLabel rl = new JLabel(rowKey);
            rl.setFont(new Font("Segoe UI", Font.BOLD, 14));
            rl.setForeground(Color.WHITE);
            badge.add(rl);

            // Slots with JustifiedWrapLayout — wraps and also spreads the gaps
            // so each row fills the available width (reduces right-side empty space).
            JPanel slotsPanel = new JPanel(new JustifiedWrapLayout(gap, gap));
            slotsPanel.setOpaque(false);
            for (ParkingSlot slot : rowSl) {
                slotsPanel.add(buildSlotCell(slot, slotW, slotH, isBike));
            }

            rowWrapper.add(badge,      BorderLayout.WEST);
            rowWrapper.add(slotsPanel, BorderLayout.CENTER);
            mapArea.add(rowWrapper);

            // Thin aisle line
            JPanel aisle = new JPanel();
            aisle.setOpaque(true);
            aisle.setBackground(new Color(0xE2E8F0));
            aisle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
            aisle.setAlignmentX(Component.LEFT_ALIGNMENT);
            mapArea.add(aisle);
        }

        // Stats footer
        long freeC = filtered.stream().filter(ParkingSlot::isFree).count();
        long occC  = filtered.size() - freeC;
        mapArea.add(Box.createVerticalStrut(6));
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        footer.setOpaque(false);
        footer.setAlignmentX(Component.LEFT_ALIGNMENT);
        footer.add(pillLabel("Total: "      + filtered.size(), new Color(0x1E293B)));
        footer.add(pillLabel("Available: "  + freeC,           Theme.FREE_GREEN));
        footer.add(pillLabel("Occupied: "   + occC,            Theme.OCC_RED));
        mapArea.add(footer);

        mapArea.revalidate();
        mapArea.repaint();
    }

    // ── Slot cell ─────────────────────────────────────────────────────────────
    private JPanel buildSlotCell(ParkingSlot slot, int w, int h, boolean isBike) {
        // RESERVED also shows as Occupied
        boolean isOccupied = !slot.isFree();

        Color bg        = isOccupied ? new Color(0xFEE2E2) : Color.WHITE;
        Color fg        = isOccupied ? Theme.OCC_RED       : Theme.FREE_GREEN;
        Color borderCol = fg;
        String statusTxt = isOccupied ? "Occupied"         : "Available";
        String typeTag   = isBike     ? "BKE"              : "CAR";

        JPanel cell = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Shadow / Glow effect
                if (getMousePosition() != null) {
                    g2.setColor(new Color(borderCol.getRed(), borderCol.getGreen(), borderCol.getBlue(), 30));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                }

                g2.setColor(getBackground());
                g2.fillRoundRect(2, 2, getWidth()-4, getHeight()-4, 10, 10);
                
                g2.setColor(new Color(borderCol.getRed(), borderCol.getGreen(), borderCol.getBlue(), 180));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(2, 2, getWidth()-4, getHeight()-4, 10, 10);
                g2.dispose();
            }
        };
        cell.setBackground(bg);
        cell.setOpaque(false);
        cell.setPreferredSize(new Dimension(w, h));
        cell.setToolTipText("<html><b>" + slot.getSlotNumber() + "</b> — " + statusTxt + "</html>");

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.gridy = 0;
        
        JLabel numLbl = new JLabel(slot.getSlotNumber());
        numLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        numLbl.setForeground(fg);
        cell.add(numLbl, gc);

        gc.gridy = 1;
        JLabel stLbl = new JLabel(statusTxt.toUpperCase());
        stLbl.setFont(new Font("Segoe UI", Font.BOLD, 8));
        stLbl.setForeground(new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 160));
        cell.add(stLbl, gc);

        if (!readOnly) {
            final Color hoverBg = isOccupied ? new Color(0xFCA5A5) : new Color(0xD1FAE5);
            cell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            cell.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (slot.isFree()) handleBook(slot);
                    else               showSlotDetails(slot);
                }
                @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                    cell.setBackground(hoverBg);
                    cell.repaint();
                }
                @Override public void mouseExited(java.awt.event.MouseEvent e) {
                    cell.setBackground(bg);
                    cell.repaint();
                }
            });
        }
        return cell;
    }

    // ── Slot Details Popup ──────────────────────────────────────────────────
    private void showSlotDetails(ParkingSlot slot) {
        Booking b = bookingDAO.getActiveBookings().stream()
            .filter(bk -> bk.getSlotNumber().equals(slot.getSlotNumber()))
            .findFirst().orElse(null);
            
        if (b == null) {
            JOptionPane.showMessageDialog(this, "No active booking found for slot " + slot.getSlotNumber());
            return;
        }

        double hrs  = b.computeHours();
        double rate = "bike".equals(selectedType) 
            ? util.Settings.getDouble("bikeRatePerHour", 50) 
            : util.Settings.getDouble("carRatePerHour", 100);
        double total = Math.max(rate, hrs * rate);
        
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        p.add(Theme.label("Vehicle: " + b.getVehiclePlate(), new Font("Segoe UI", Font.BOLD, 16), Theme.ACCENT));
        p.add(Box.createVerticalStrut(10));
        p.add(Theme.label("Check-In: " + b.getCheckIn().format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a, dd MMM")), 
                         Theme.FONT_BODY, Theme.TEXT_DARK));
        p.add(Theme.label("Duration: " + String.format("%.1f", hrs) + " hours so far", 
                         Theme.FONT_BODY, Theme.TEXT_DARK));
        p.add(Box.createVerticalStrut(10));
        p.add(Theme.label("Current Bill: Rs. " + String.format("%.0f", total), 
                         new Font("Segoe UI", Font.BOLD, 14), new Color(0x059669)));
        
        Object[] options = {"Check-Out Vehicle", "Close"};
        int choice = JOptionPane.showOptionDialog(this, p, "Slot Details - " + slot.getSlotNumber(),
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[1]);
        
        if (choice == 0) {
            handleCheckout(slot);
        }
    }

    // ── Check-In ─────────────────────────────────────────────────────────────
    private void handleBook(ParkingSlot slot) {
        JTextField pf = new JTextField(12);
        pf.setFont(Theme.FONT_BODY);
        Object[] msg = {
            Theme.label("Vehicle Plate Number:", Theme.FONT_BODY, Theme.TEXT_DARK), pf,
            Theme.label("Slot: " + slot.getSlotNumber(), Theme.FONT_SMALL, Theme.TEXT_GRAY)
        };
        if (JOptionPane.showConfirmDialog(this, msg, "Check-In",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            String plate = pf.getText().trim().toUpperCase();
            if (plate.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Plate number is required.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            int vId = bookingDAO.saveVehicleIfNew(plate, selectedType, 1);
            int bId = bookingDAO.createBooking(vId, slot.getSlotId(), operatorId, plate, slot.getSlotNumber());
            if (bId > 0) {
                JOptionPane.showMessageDialog(this,
                    "<html><b>Check-In Successful!</b><br>"
                    + "Plate: " + plate + "<br>"
                    + "Slot: " + slot.getSlotNumber() + "<br>"
                    + "Confirmation: EP" + String.format("%04d", bId) + "</html>",
                    "Check-In Done", JOptionPane.INFORMATION_MESSAGE);
                refresh();
            } else {
                JOptionPane.showMessageDialog(this, "Check-in failed. Please try again.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ── Check-Out ────────────────────────────────────────────────────────────
    private void handleCheckout(ParkingSlot slot) {
        Booking b = bookingDAO.getActiveBookings().stream()
            .filter(bk -> bk.getSlotNumber().equals(slot.getSlotNumber()))
            .findFirst().orElse(null);
        if (b == null) {
            JOptionPane.showMessageDialog(this, "No active booking found for slot "
                    + slot.getSlotNumber() + ".");
            return;
        }
        int opt = JOptionPane.showConfirmDialog(this,
            "<html>Vehicle: <b>" + b.getVehiclePlate() + "</b><br>"
            + "Slot: <b>" + slot.getSlotNumber() + "</b><br>"
            + "Confirm checkout?</html>",
            "Check-Out", JOptionPane.YES_NO_OPTION);
        if (opt == JOptionPane.YES_OPTION) {
            double hrs  = b.computeHours();
            double rate = "bike".equals(selectedType) 
                ? util.Settings.getDouble("bikeRatePerHour", 50) 
                : util.Settings.getDouble("carRatePerHour", 100);
            double base = Math.max(rate, hrs * rate);
            double tax  = base * (util.Settings.getDouble("taxPercentage", 10) / 100.0);
            bookingDAO.checkoutBooking(b.getBookingId(), hrs, base, tax, 0, base + tax, "CASH");
            bookingDAO.updateSlotStatus(slot.getSlotId(), "free");
            JOptionPane.showMessageDialog(this,
                "<html><b>Check-Out Complete!</b><br>"
                + "Duration: " + String.format("%.1f", hrs) + " hrs<br>"
                + "Total: Rs. " + String.format("%.0f", base + tax) + "</html>",
                "Check-Out Done", JOptionPane.INFORMATION_MESSAGE);
            refresh();
        }
    }

    // ── UI helpers ────────────────────────────────────────────────────────────
    /** Creates a tab button that reliably shows its background color on any LAF */
    private JButton typeBtn(String text, String type) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (getBackground() == Theme.ACCENT) {
                    // Cyber-Glow Gradient for Active
                    g2.setPaint(new GradientPaint(0, 0, new Color(0x0EA5E9), getWidth(), 0, new Color(0x0284C7)));
                } else {
                    g2.setColor(getBackground());
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

                // Text
                g2.setColor(getForeground());
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth()  - fm.stringWidth(getText())) / 2;
                int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), tx, ty);
                g2.dispose();
            }
        };
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setFocusPainted(false);
        b.setBorderPainted(true);
        b.setContentAreaFilled(false); // we paint ourselves
        b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(100, 34));
        b.addActionListener(e -> selectType(type));
        return b;
    }

    private void selectType(String type) {
        selectedType = type;
        styleTab(btnCar,  "car".equals(type));
        styleTab(btnBike, "bike".equals(type));
        drawMap();
    }

    private void styleTab(JButton b, boolean active) {
        b.setBackground(active ? Theme.ACCENT : Color.WHITE);
        b.setForeground(active ? Color.WHITE  : Theme.TEXT_DARK);
        b.setBorder(active
            ? BorderFactory.createLineBorder(Theme.ACCENT, 2)
            : BorderFactory.createLineBorder(Theme.BORDER, 1));
        b.repaint();
    }

    private JLabel counterBadge(String text, Color color) {
        JLabel l = new JLabel("  " + text + "  ") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                super.paintComponent(g);
                g2.dispose();
            }
        };
        l.setFont(new Font("Segoe UI", Font.BOLD, 11));
        l.setForeground(Color.WHITE);
        l.setBackground(color);
        l.setOpaque(false);
        l.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        return l;
    }

    private JLabel pillLabel(String text, Color color) {
        JLabel l = new JLabel("  " + text + "  ");
        l.setFont(new Font("Segoe UI", Font.BOLD, 11));
        l.setForeground(color);
        l.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color, 1),
            BorderFactory.createEmptyBorder(3, 5, 3, 5)));
        return l;
    }

    private void addLegend(JPanel p, Color c, String label) {
        JPanel dot = new JPanel();
        dot.setBackground(c);
        dot.setPreferredSize(new Dimension(13, 13));
        dot.setBorder(BorderFactory.createLineBorder(c.darker(), 1));
        p.add(dot);
        p.add(Theme.label(label, Theme.FONT_SMALL, Theme.TEXT_GRAY));
    }

    // ── JustifiedWrapLayout — wraps + justifies rows ─────────────────────────
    // Like FlowLayout wrap, but distributes extra row space into gaps.
    static class JustifiedWrapLayout extends FlowLayout {
        JustifiedWrapLayout(int hgap, int vgap) { super(FlowLayout.LEFT, hgap, vgap); }

        @Override
        public Dimension preferredLayoutSize(Container target) { return layoutSize(target, true); }

        @Override
        public Dimension minimumLayoutSize(Container target) { return layoutSize(target, false); }

        @Override
        public void layoutContainer(Container target) {
            synchronized (target.getTreeLock()) {
                Insets insets = target.getInsets();
                int hgap = getHgap(), vgap = getVgap();
                int maxWidth = target.getWidth() - (insets.left + insets.right + hgap * 2);
                if (maxWidth <= 0) {
                    super.layoutContainer(target);
                    return;
                }

                int x = insets.left + hgap;
                int y = insets.top + vgap;

                java.util.List<Component> row = new java.util.ArrayList<>();
                int rowW = 0;
                int rowH = 0;

                for (int i = 0; i < target.getComponentCount(); i++) {
                    Component c = target.getComponent(i);
                    if (!c.isVisible()) continue;

                    Dimension d = c.getPreferredSize();
                    int nextW = (row.isEmpty() ? d.width : rowW + hgap + d.width);

                    if (!row.isEmpty() && nextW > maxWidth) {
                        layoutRow(row, target, x, y, maxWidth, rowH);
                        y += rowH + vgap;
                        row.clear();
                        rowW = 0;
                        rowH = 0;
                    }

                    row.add(c);
                    rowW = (rowW == 0) ? d.width : (rowW + hgap + d.width);
                    rowH = Math.max(rowH, d.height);
                }

                if (!row.isEmpty()) {
                    // Justify the last row too (user requested "fill by itself")
                    layoutRow(row, target, x, y, maxWidth, rowH);
                }
            }
        }

        private void layoutRow(java.util.List<Component> row, Container target,
                               int startX, int y, int maxWidth, int rowH) {
            int hgap = getHgap();
            int count = row.size();
            if (count == 0) return;

            int totalCompW = 0;
            for (Component c : row) totalCompW += c.getPreferredSize().width;

            int gaps = Math.max(0, count - 1);
            int baseGapsW = gaps * hgap;
            int extra = maxWidth - (totalCompW + baseGapsW);

            int addPerGap = (gaps > 0 && extra > 0) ? (extra / gaps) : 0;
            int remainder = (gaps > 0 && extra > 0) ? (extra % gaps) : 0;

            int x = startX;
            for (int i = 0; i < count; i++) {
                Component c = row.get(i);
                Dimension d = c.getPreferredSize();
                int cy = y + (rowH - d.height) / 2;
                c.setBounds(x, cy, d.width, d.height);

                x += d.width;
                if (i < count - 1) {
                    int gap = hgap + addPerGap + (remainder-- > 0 ? 1 : 0);
                    x += gap;
                }
            }
        }

        private Dimension layoutSize(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                int targetWidth = target.getSize().width;
                if (targetWidth == 0) targetWidth = Integer.MAX_VALUE;
                int hgap = getHgap(), vgap = getVgap();
                Insets insets = target.getInsets();
                int maxWidth  = targetWidth - (insets.left + insets.right + hgap * 2);
                Dimension dim = new Dimension(0, 0);
                int rowW = 0, rowH = 0;
                for (int i = 0; i < target.getComponentCount(); i++) {
                    Component m = target.getComponent(i);
                    if (m.isVisible()) {
                        Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
                        if (rowW + d.width > maxWidth && rowW > 0) {
                            dim.width   = Math.max(dim.width, rowW);
                            dim.height += rowH + vgap;
                            rowW = 0; rowH = 0;
                        }
                        rowW += d.width + hgap;
                        rowH  = Math.max(rowH, d.height);
                    }
                }
                dim.width   = Math.max(dim.width, rowW);
                dim.height += rowH + vgap * 2 + insets.top + insets.bottom;
                return dim;
            }
        }
    }
}