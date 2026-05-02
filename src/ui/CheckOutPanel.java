package ui;

import dao.BookingDAO;
import dao.DatabaseManager;
import model.Booking;
import util.Theme;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class CheckOutPanel extends JPanel {

    private final BookingDAO bookingDAO = new BookingDAO();

    private JTextField        searchField;
    private JPanel            detailsCard;
    private JLabel            lblPlate, lblSlot, lblOwner, lblContact,
                              lblCheckIn, lblCheckOut, lblDuration,
                              lblBaseAmt, lblTaxAmt, lblTotalAmt, lblVType;
    private JComboBox<String> payMethodCombo;
    private JButton           checkOutBtn;

    private JTable            activeTable;
    private DefaultTableModel activeModel;
    private JLabel            lblActiveCount, lblTotalRevToday;

    private Booking activeBooking;
    private String  cachedVType   = "car";
    private String  cachedOwner   = "-";
    private String  cachedContact = "-";

    private static final Color CLR_RED      = new Color(0xEF4444);
    private static final Color CLR_RED_DARK = new Color(0x991B1B);
    private static final Color CLR_GREEN    = new Color(0x10B981);
    private static final Color CLR_AMBER    = new Color(0xF59E0B);
    private static final Color CLR_STRIPE   = new Color(0xF8FAFC);
    private static final Color CLR_NAVY     = new Color(0x1E3A5F);
    private static final Color CLR_SLATE    = new Color(0x64748B);

    private Timer liveUpdateTimer;

    public CheckOutPanel() {
        setLayout(new BorderLayout(0, 0));
        setOpaque(true);
        setBackground(Theme.BG_WHITE);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        buildUI();

        liveUpdateTimer = new Timer(30000, e -> {
            if (activeBooking != null && detailsCard.isVisible()) updateTimeAndFee();
        });
        liveUpdateTimer.start();
    }

    private void buildUI() {
        add(buildHeader(), BorderLayout.NORTH);
        JPanel body = new JPanel(new GridLayout(1, 2, 20, 0));
        body.setOpaque(false);
        body.add(buildLeftColumn());
        body.add(buildRightColumn());
        add(body, BorderLayout.CENTER);
        refreshRight();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
        JPanel stats = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        stats.setOpaque(false);
        lblActiveCount   = statBadge("Active: 0",    CLR_AMBER);
        lblTotalRevToday = statBadge("Today: Rs. 0", CLR_GREEN);
        stats.add(lblActiveCount);
        stats.add(lblTotalRevToday);
        header.add(stats, BorderLayout.EAST);
        return header;
    }

    private JPanel buildLeftColumn() {
        JPanel col = new JPanel(new BorderLayout(0, 14));
        col.setOpaque(false);
        col.add(buildSearchCard(), BorderLayout.NORTH);
        detailsCard = buildDetailsCard();
        detailsCard.setVisible(false);
        col.add(detailsCard, BorderLayout.CENTER);
        return col;
    }

    private JPanel buildSearchCard() {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.BORDER, 2),
            BorderFactory.createEmptyBorder(16, 18, 16, 18)));

        JPanel cardHead = new JPanel(new BorderLayout());
        cardHead.setOpaque(false);
        cardHead.add(Theme.label(" Find Vehicle by Plate",
                Theme.FONT_SUBTITLE, Theme.TEXT_DARK), BorderLayout.WEST);
        card.add(cardHead, BorderLayout.NORTH);

        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);

        JLabel prefix = new JLabel("  PLATE  ");
        prefix.setFont(new Font("Segoe UI", Font.BOLD, 11));
        prefix.setForeground(Color.WHITE);
        prefix.setBackground(Theme.SIDEBAR_START);
        prefix.setOpaque(true);
        prefix.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));

        searchField = new JTextField();
        searchField.setFont(new Font("Segoe UI", Font.BOLD, 15));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0xCBD5E1), 1),
            BorderFactory.createEmptyBorder(6, 12, 6, 12)));
        searchField.setBackground(Color.WHITE);
        searchField.setText("e.g. ABC-1234");
        searchField.setForeground(Theme.TEXT_MUTED);
        searchField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) {
                searchField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Theme.ACCENT, 1),
                    BorderFactory.createEmptyBorder(6, 12, 6, 12)));
                if ("e.g. ABC-1234".equals(searchField.getText())) {
                    searchField.setText(""); searchField.setForeground(Theme.TEXT_DARK);
                }
            }
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                searchField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0xCBD5E1), 1),
                    BorderFactory.createEmptyBorder(6, 12, 6, 12)));
                if (searchField.getText().isEmpty()) {
                    searchField.setText("e.g. ABC-1234"); searchField.setForeground(Theme.TEXT_MUTED);
                }
            }
        });
        searchField.addActionListener(e -> searchBooking());

        JPanel fieldWrap = new JPanel(new BorderLayout());
        fieldWrap.setOpaque(false);
        fieldWrap.add(prefix, BorderLayout.WEST);
        fieldWrap.add(searchField, BorderLayout.CENTER);

        JButton searchBtn = buildSearchButton();
        searchBtn.addActionListener(e -> searchBooking());
        row.add(fieldWrap, BorderLayout.CENTER);
        row.add(searchBtn, BorderLayout.EAST);

        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        south.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        JLabel hint = new JLabel("  Press Enter or click Search -- case-insensitive");
        hint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        hint.setForeground(Theme.TEXT_MUTED);
        JLabel rates = new JLabel(String.format(" Car Rs.%.0f/hr  |  Bike Rs.%.0f/hr ", 
            util.Settings.getDouble("carRatePerHour", 100), 
            util.Settings.getDouble("bikeRatePerHour", 50)));
        rates.setFont(new Font("Segoe UI", Font.BOLD, 10));
        rates.setForeground(Color.WHITE);
        rates.setBackground(CLR_SLATE);
        rates.setOpaque(true);
        rates.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        south.add(hint, BorderLayout.WEST);
        south.add(rates, BorderLayout.EAST);

        card.add(row, BorderLayout.CENTER);
        card.add(south, BorderLayout.SOUTH);
        return card;
    }

    private JButton buildSearchButton() {
        JButton btn = new JButton("Search") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, new Color(0x0EA5E9), getWidth(), 0, new Color(0x0369A1)));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setContentAreaFilled(false); btn.setOpaque(false);
        btn.setBorderPainted(false);     btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(Color.WHITE);
        btn.setPreferredSize(new Dimension(110, 44));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JPanel buildDetailsCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));

        JPanel cardHdr = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, new Color(0x1E3A5F), getWidth(), 0, new Color(0x0F172A)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(34, 211, 238, 100));
                g2.fillRect(0, getHeight()-1, getWidth(), 1);
                g2.dispose();
            }
        };
        cardHdr.setOpaque(false);
        cardHdr.setBorder(BorderFactory.createEmptyBorder(9, 16, 9, 16));
        cardHdr.add(Theme.label("  Vehicle Details & Payment",
                new Font("Segoe UI", Font.BOLD, 13), Color.WHITE), BorderLayout.WEST);
        card.add(cardHdr, BorderLayout.NORTH);

        lblPlate   = dvBold("-"); lblVType   = dv("-");
        lblSlot    = dv("-");     lblOwner   = dvBold("-");
        lblContact = dv("-");
        lblCheckIn  = dv("-");    lblCheckOut = dv("-"); lblDuration = dvBold("-");
        lblBaseAmt  = dvBold("-"); lblBaseAmt.setForeground(CLR_GREEN);
        lblTaxAmt   = dv("-");    lblTaxAmt.setForeground(CLR_AMBER);
        lblTotalAmt = new JLabel("-");
        lblTotalAmt.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTotalAmt.setForeground(CLR_RED);

        JPanel twoColWrapper = new JPanel(new GridLayout(1, 2, 1, 0));
        twoColWrapper.setBackground(new Color(0xE2E8F0));

        JPanel leftPanel = new JPanel();
        leftPanel.setBackground(Color.WHITE);
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 12));

        leftPanel.add(sectionBadge("VEHICLE INFORMATION"));
        leftPanel.add(Box.createVerticalStrut(8));
        leftPanel.add(infoRow("Plate No", lblPlate));
        leftPanel.add(thinDivider());
        leftPanel.add(infoRow("Type",     lblVType));
        leftPanel.add(thinDivider());
        leftPanel.add(infoRow("Slot",     lblSlot));
        leftPanel.add(thinDivider());
        leftPanel.add(infoRow("Owner",    lblOwner));
        leftPanel.add(thinDivider());
        leftPanel.add(infoRow("Contact",  lblContact));
        leftPanel.add(Box.createVerticalGlue());

        JPanel rightPanel = new JPanel();
        rightPanel.setBackground(Color.WHITE);
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 14));

        rightPanel.add(sectionBadge("PARKING DURATION"));
        rightPanel.add(Box.createVerticalStrut(8));
        rightPanel.add(infoRow("Check-In",  lblCheckIn));
        rightPanel.add(thinDivider());
        rightPanel.add(infoRow("Check-Out", lblCheckOut));
        rightPanel.add(thinDivider());
        rightPanel.add(infoRow("Duration",  lblDuration));
        rightPanel.add(Box.createVerticalStrut(10));

        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(0xCBD5E1));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        rightPanel.add(sep);
        rightPanel.add(Box.createVerticalStrut(10));

        rightPanel.add(sectionBadge("FEE BREAKDOWN"));
        rightPanel.add(Box.createVerticalStrut(8));
        rightPanel.add(infoRow("Base Amt",  lblBaseAmt));
        rightPanel.add(thinDivider());
        rightPanel.add(infoRow(String.format("Tax (%.0f%%)", util.Settings.getDouble("taxPercentage", 10)), lblTaxAmt));
        rightPanel.add(Box.createVerticalStrut(8));

        JPanel totalBox = new JPanel(new BorderLayout(0, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(0xFFF1F2));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.setColor(new Color(0xFCA5A5));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 6, 6);
                g2.dispose();
            }
        };
        totalBox.setOpaque(false);
        totalBox.setBorder(BorderFactory.createEmptyBorder(7, 10, 7, 10));
        totalBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        totalBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel tLbl = new JLabel("TOTAL DUE");
        tLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        tLbl.setForeground(CLR_RED_DARK);
        totalBox.add(tLbl, BorderLayout.WEST);
        totalBox.add(lblTotalAmt, BorderLayout.EAST);
        rightPanel.add(totalBox);
        rightPanel.add(Box.createVerticalGlue());

        twoColWrapper.add(leftPanel);
        twoColWrapper.add(rightPanel);
        card.add(twoColWrapper, BorderLayout.CENTER);

        JPanel south = new JPanel();
        south.setBackground(new Color(0xF8FAFC));
        south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));
        south.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER),
            BorderFactory.createEmptyBorder(10, 16, 12, 16)));

        JPanel payRow = new JPanel(new BorderLayout(12, 0));
        payRow.setOpaque(false);
        payRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        payRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel payLbl = new JLabel("Payment Method:");
        payLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        payLbl.setForeground(Theme.TEXT_DARK);
        payRow.add(payLbl, BorderLayout.WEST);

        payMethodCombo = new JComboBox<>(new String[]{"Cash", "Card", "JazzCash"});
        payMethodCombo.setFont(new Font("Segoe UI", Font.BOLD, 13));
        payMethodCombo.setBackground(Color.WHITE);
        payMethodCombo.setPreferredSize(new Dimension(160, 36));
        payMethodCombo.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object val,
                    int idx, boolean sel, boolean foc) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, val, idx, sel, foc);
                if (!sel) l.setForeground(switch (String.valueOf(val)) {
                    case "Cash"     -> CLR_GREEN;
                    case "Card"     -> new Color(0x2563EB);
                    case "JazzCash" -> CLR_AMBER;
                    default         -> Theme.TEXT_DARK;
                });
                l.setFont(new Font("Segoe UI", Font.BOLD, 13));
                return l;
            }
        });
        payRow.add(payMethodCombo, BorderLayout.EAST);
        south.add(payRow);
        south.add(Box.createVerticalStrut(8));

        checkOutBtn = new JButton("Complete Check-Out") {
            @Override protected void paintComponent(Graphics gr) {
                Graphics2D g2 = (Graphics2D) gr.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color c1 = getModel().isPressed() ? CLR_RED_DARK : CLR_RED;
                g2.setPaint(new GradientPaint(0, 0, c1, getWidth(), 0, CLR_RED_DARK));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(gr);
            }
        };
        checkOutBtn.setContentAreaFilled(false); checkOutBtn.setOpaque(false);
        checkOutBtn.setBorderPainted(false);     checkOutBtn.setFocusPainted(false);
        checkOutBtn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        checkOutBtn.setForeground(Color.WHITE);
        checkOutBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        checkOutBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        checkOutBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        checkOutBtn.addActionListener(e -> handleCheckOut());
        south.add(checkOutBtn);

        card.add(south, BorderLayout.SOUTH);
        return card;
    }

    private JLabel sectionBadge(String text) {
        JLabel l = new JLabel("  " + text + "  ");
        l.setFont(new Font("Segoe UI", Font.BOLD, 10));
        l.setForeground(Color.WHITE);
        l.setBackground(CLR_SLATE);
        l.setOpaque(true);
        l.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JPanel infoRow(String key, JLabel valLabel) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        row.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
        JLabel kl = new JLabel(key);
        kl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        kl.setForeground(Theme.TEXT_GRAY);
        kl.setPreferredSize(new Dimension(68, 22));
        row.add(kl, BorderLayout.WEST);
        valLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        row.add(valLabel, BorderLayout.CENTER);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        return row;
    }

    private Component thinDivider() {
        JSeparator s = new JSeparator();
        s.setForeground(new Color(0xF1F5F9));
        s.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return s;
    }

    private JPanel buildRightColumn() {
        JPanel col = new JPanel();
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setOpaque(false);

        JPanel tableCard = buildActiveTableCard();
        tableCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        tableCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 420));
        tableCard.setPreferredSize(new Dimension(tableCard.getPreferredSize().width, 340));

        JPanel guideCard = buildGuideCard();
        guideCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        guideCard.setPreferredSize(new Dimension(guideCard.getPreferredSize().width, 205));
        guideCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 205));
        guideCard.setMinimumSize(new Dimension(100, 205));

        col.add(tableCard);
        col.add(Box.createVerticalStrut(14));
        col.add(guideCard);
        return col;
    }

    private JPanel buildActiveTableCard() {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 4, 0, 0, Theme.OCC_RED),
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER, 1),
                BorderFactory.createEmptyBorder(14, 16, 8, 16))));

        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setOpaque(true);
        hdr.setBackground(new Color(0x1E3A5F));
        hdr.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        hdr.add(Theme.label("Currently Parked Vehicles",
                new Font("Segoe UI", Font.BOLD, 13), Color.WHITE), BorderLayout.WEST);
        
        JPanel tableWrap = new JPanel(new BorderLayout(0, 0));
        tableWrap.setOpaque(false);
        tableWrap.add(hdr, BorderLayout.NORTH);
        tableWrap.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(34, 211, 238, 150)));
        card.add(tableWrap, BorderLayout.NORTH);

        String[] cols = {"Plate", "Type", "Slot", "Check-In", "Duration", "Est. Fee"};
        activeModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        activeTable = new JTable(activeModel);
        styleTable(activeTable);

        TableColumnModel cm = activeTable.getColumnModel();
        cm.getColumn(0).setPreferredWidth(90);
        cm.getColumn(1).setPreferredWidth(60);
        cm.getColumn(2).setPreferredWidth(60);
        cm.getColumn(3).setPreferredWidth(110);
        cm.getColumn(4).setPreferredWidth(100);
        cm.getColumn(5).setPreferredWidth(80);

        activeTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = activeTable.getSelectedRow();
                if (row >= 0 && e.getClickCount() == 2) {
                    String plate = (String) activeModel.getValueAt(row, 0);
                    searchField.setText(plate);
                    searchField.setForeground(Theme.TEXT_DARK);
                    searchBooking();
                }
            }
        });

        JScrollPane sp = new JScrollPane(activeTable);
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));
        sp.getViewport().setBackground(Color.WHITE);
        card.add(sp, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(4, 0, 2, 0));
        JLabel dblHint = new JLabel("Double-click any row to auto-fill plate");
        dblHint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        dblHint.setForeground(Theme.TEXT_MUTED);
        JButton refreshBtn = new JButton("Refresh") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, new Color(0x0EA5E9), 0, getHeight(), new Color(0x0369A1)));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        refreshBtn.setContentAreaFilled(false); refreshBtn.setOpaque(false);
        refreshBtn.setBorderPainted(false);     refreshBtn.setFocusPainted(false);
        refreshBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.setPreferredSize(new Dimension(90, 30));
        refreshBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refreshBtn.addActionListener(e -> refreshRight());
        footer.add(dblHint, BorderLayout.WEST);
        footer.add(refreshBtn, BorderLayout.EAST);
        card.add(footer, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildGuideCard() {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.BORDER, 1),
            BorderFactory.createEmptyBorder(12, 16, 12, 16)));
        card.add(Theme.label("Checkout Guide", Theme.FONT_SUBTITLE, Theme.TEXT_DARK),
                BorderLayout.NORTH);

        JPanel steps = new JPanel();
        steps.setOpaque(false);
        steps.setLayout(new BoxLayout(steps, BoxLayout.Y_AXIS));
        String[][] guide = {
            {"1", "Enter plate number (or double-click table row)"},
            {"2", "Click Search to load parking details"},
            {"3", "Verify owner name, duration & fee"},
            {"4", "Select payment method"},
            {"5", "Click Complete Check-Out"},
            {"6", "Receipt will appear -- billed per exact minute"},
        };
        for (String[] g : guide) {
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
            row.setOpaque(false);
            JLabel num = new JLabel(g[0]) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setPaint(new GradientPaint(0, 0, new Color(0x0EA5E9), 0, getHeight(), new Color(0x0369A1)));
                    g2.fillOval(0, 0, getWidth(), getHeight());
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            num.setFont(new Font("Segoe UI", Font.BOLD, 10));
            num.setForeground(Color.WHITE);
            num.setPreferredSize(new Dimension(22, 22));
            num.setHorizontalAlignment(SwingConstants.CENTER);
            num.setOpaque(false);
            
            JLabel txt = new JLabel(g[1]);
            txt.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            txt.setForeground(Theme.TEXT_DARK);
            row.add(num); row.add(txt);
            steps.add(row);
        }
        card.add(steps, BorderLayout.CENTER);
        return card;
    }

    private void searchBooking() {
        String raw = searchField.getText().trim();
        if (raw.isEmpty() || "e.g. ABC-1234".equals(raw)) { error("Plate number enter karein."); return; }
        String plate = raw.toUpperCase();
        activeBooking = bookingDAO.findActiveByPlate(plate);
        if (activeBooking == null) {
            detailsCard.setVisible(false);
            revalidate(); repaint();
            JOptionPane.showMessageDialog(this,
                "<html>Plate <b>" + plate + "</b> ke liye koi active vehicle nahi mili.</html>",
                "Not Found", JOptionPane.WARNING_MESSAGE);
            return;
        }
        populateDetails();
        detailsCard.setVisible(true);
        revalidate(); repaint();
    }

    private void populateDetails() {
        lblPlate.setText(activeBooking.getVehiclePlate());
        String sn = activeBooking.getSlotNumber();
        lblSlot.setText(sn + "  (Row " + (sn.length() > 0 ? sn.charAt(0) : '-') + ")");

        cachedOwner = "-"; cachedContact = "-"; cachedVType = "car";
        try {
            Connection c = DatabaseManager.getInstance().getConnection();
            try (PreparedStatement ps = c.prepareStatement(
                     "SELECT owner_name, contact_number FROM bookings WHERE booking_id=?")) {
                ps.setInt(1, activeBooking.getBookingId());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    cachedOwner   = nvl(rs.getString("owner_name"));
                    cachedContact = nvl(rs.getString("contact_number"));
                }
            }
        } catch (Exception ignored) {}

        try {
            Connection c = DatabaseManager.getInstance().getConnection();
            try (PreparedStatement ps = c.prepareStatement(
                     "SELECT vehicle_type FROM parking_slots WHERE slot_id=?")) {
                ps.setInt(1, activeBooking.getSlotId());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) cachedVType = nvl(rs.getString("vehicle_type"), "car");
            }
        } catch (Exception ignored) {}

        lblOwner.setText(cachedOwner);
        lblContact.setText(cachedContact);
        lblVType.setText(cap(cachedVType));
        updateTimeAndFee();
    }

    private void updateTimeAndFee() {
        if (activeBooking == null) return;
        LocalDateTime checkIn = activeBooking.getCheckIn();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy  HH:mm");
        lblCheckIn.setText(checkIn != null ? checkIn.format(fmt) : "-");
        lblCheckOut.setText(LocalDateTime.now().format(fmt) + "  (now)");

        long mins = checkIn != null ? ChronoUnit.MINUTES.between(checkIn, LocalDateTime.now()) : 0;
        FeeResult fee = calcFee(mins, cachedVType);
        long h = mins / 60, m = mins % 60;
        lblDuration.setText(String.format("%dh %02dm (%d min)", h, m, mins));
        lblBaseAmt.setText(String.format("Rs. %.2f", fee.base));
        lblTaxAmt.setText(String.format("Rs. %.2f", fee.tax));
        lblTotalAmt.setText(String.format("Rs. %.2f", fee.total));
    }

    private static class FeeResult {
        final double base, tax, total;
        FeeResult(double b, double t, double tot) { base=b; tax=t; total=tot; }
    }

    private FeeResult calcFee(long totalMinutes, String vType) {
        double rate = vType.equalsIgnoreCase("bike") 
            ? util.Settings.getDouble("bikeRatePerHour", 50) 
            : util.Settings.getDouble("carRatePerHour", 100);
        double taxRate = util.Settings.getDouble("taxPercentage", 10) / 100.0;
        
        double base = (totalMinutes / 60.0) * rate;
        double tax  = base * taxRate;
        return new FeeResult(base, tax, base + tax);
    }

    private void handleCheckOut() {
        if (activeBooking == null) return;
        String payMethod = (String) payMethodCombo.getSelectedItem();
        LocalDateTime checkIn  = activeBooking.getCheckIn();
        LocalDateTime checkOut = LocalDateTime.now();
        long mins = checkIn != null ? ChronoUnit.MINUTES.between(checkIn, checkOut) : 0;
        FeeResult fee = calcFee(mins, cachedVType);

        int confirm = JOptionPane.showConfirmDialog(this,
            "<html>Vehicle: <b>" + activeBooking.getVehiclePlate() + "</b><br>" +
            "Duration: <b>" + (mins/60) + " hr " + (mins%60) + " min</b><br>" +
            "Total Due: <b>Rs. " + String.format("%.2f", fee.total) + "</b><br>" +
            "Payment: <b>" + payMethod + "</b><br><br>Proceed with checkout?</html>",
            "Confirm Check-Out", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        bookingDAO.checkoutBooking(activeBooking.getBookingId(),
                mins/60.0, fee.base, fee.tax, 0, fee.total, payMethod);
        bookingDAO.updateSlotStatus(activeBooking.getSlotId(), "free");

        showReceiptDialog(activeBooking, mins, fee, payMethod, checkOut);

        detailsCard.setVisible(false);
        activeBooking = null;
        searchField.setText("e.g. ABC-1234");
        searchField.setForeground(Theme.TEXT_MUTED);
        refreshRight();
        Window win = SwingUtilities.getWindowAncestor(this);
        if (win instanceof AdminDashboard d) d.refreshData();
        else if (win instanceof EmployeeDashboard d) d.refreshData();
    }

    private void showReceiptDialog(Booking b, long totalMins, FeeResult fee,
                                   String payMethod, LocalDateTime checkOut) {

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
        long h = totalMins / 60, m = totalMins % 60;
        String confCode = b.getConfCode() != null ? b.getConfCode() : "---";
        double rate = "bike".equalsIgnoreCase(cachedVType) 
            ? util.Settings.getDouble("bikeRatePerHour", 50) 
            : util.Settings.getDouble("carRatePerHour", 100);

        String receiptText = buildReceiptText(b, confCode, checkOut, fmt,
                                               h, m, totalMins, rate, fee, payMethod);

        JDialog dialog = new JDialog(
            SwingUtilities.getWindowAncestor(this),
            "Receipt -- " + confCode,
            java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(0, 8));
        root.setBackground(Color.WHITE);
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextArea ta = new JTextArea(receiptText);
        ta.setFont(new Font("Courier New", Font.PLAIN, 12));
        ta.setBackground(Color.WHITE);
        ta.setForeground(new Color(0x1E293B));
        ta.setEditable(false);
        ta.setFocusable(false);
        ta.setMargin(new Insets(8, 12, 8, 12));

        root.add(ta, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnPanel.setOpaque(false);
        btnPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        JButton printBtn = new JButton("Print");
        JButton closeBtn = new JButton("OK");
        printBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        closeBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        printBtn.setPreferredSize(new Dimension(100, 30));
        closeBtn.setPreferredSize(new Dimension(80, 30));

        printBtn.addActionListener(e -> {
            try { ta.print(); } catch (Exception ex) { /* ignore */ }
        });
        closeBtn.addActionListener(e -> dialog.dispose());

        btnPanel.add(printBtn);
        btnPanel.add(closeBtn);
        root.add(btnPanel, BorderLayout.SOUTH);

        dialog.setContentPane(root);
        dialog.pack();
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private String buildReceiptText(Booking b, String confCode, LocalDateTime checkOut,
                                    DateTimeFormatter fmt, long h, long m, long totalMins,
                                    double rate, FeeResult fee, String payMethod) {
        final int W = 42;
        final String LINE  = "=".repeat(W);
        final String DASH  = "-".repeat(W);

        String slot = b.getSlotNumber() != null ? b.getSlotNumber() : "-";
        String slotRow = slot.length() > 0 ? String.valueOf(slot.charAt(0)) : "-";

        StringBuilder sb = new StringBuilder();
        sb.append(LINE).append("\n");
        sb.append(center("PARKNOVA SYSTEM", W)).append("\n");
        sb.append(center("Parking Receipt", W)).append("\n");
        sb.append(LINE).append("\n");
        sb.append(lr("Ref  :", confCode, W)).append("\n");
        sb.append(DASH).append("\n");
        sb.append(lr("Plate   :", b.getVehiclePlate(), W)).append("\n");
        sb.append(lr("Type    :", cap(cachedVType), W)).append("\n");
        sb.append(lr("Slot    :", slot, W)).append("\n");
        sb.append(lr("Row     :", slotRow, W)).append("\n");
        sb.append(lr("Owner   :", cachedOwner, W)).append("\n");
        sb.append(lr("Contact :", cachedContact, W)).append("\n");
        sb.append(DASH).append("\n");
        String ciStr = b.getCheckIn() != null ? b.getCheckIn().format(fmt) : "---";
        sb.append(lr("Check-In :", ciStr, W)).append("\n");
        sb.append(lr("Check-Out:", checkOut.format(fmt), W)).append("\n");
        sb.append(lr("Duration :", String.format("%d hr  %02d min", h, m), W)).append("\n");
        sb.append(DASH).append("\n");
        sb.append(lr("Rate     :", String.format("Rs. %.0f / hr", rate), W)).append("\n");
        sb.append(lr("Base Fee :", String.format("Rs. %7.2f", fee.base), W)).append("\n");
        sb.append(lr(String.format("Tax(%.0f%%) :", util.Settings.getDouble("taxPercentage", 10)), String.format("Rs. %7.2f", fee.tax), W)).append("\n");
        sb.append(LINE).append("\n");
        sb.append(lr("TOTAL    :", String.format("Rs. %7.2f", fee.total), W)).append("\n");
        sb.append(LINE).append("\n");
        sb.append(lr("Payment  :", payMethod.toUpperCase(), W)).append("\n");
        sb.append(LINE).append("\n");
        sb.append(center("** THANK YOU **", W)).append("\n");
        sb.append(center("Drive safely!", W)).append("\n");
        sb.append(LINE).append("\n");
        return sb.toString();
    }

    private String center(String text, int width) {
        if (text.length() >= width) return text;
        int pad = (width - text.length()) / 2;
        return " ".repeat(pad) + text;
    }

    private String lr(String left, String right, int width) {
        int spaces = width - left.length() - right.length();
        if (spaces < 1) spaces = 1;
        return left + " ".repeat(spaces) + right;
    }

    public void refreshRight() {
        if (activeModel == null) return;
        activeModel.setRowCount(0);
        List<Booking> active = bookingDAO.getActiveBookings();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM  HH:mm");
        for (Booking bk : active) {
            long mins = bk.getCheckIn() != null
                    ? ChronoUnit.MINUTES.between(bk.getCheckIn(), LocalDateTime.now()) : 0;
            String vt = "car";
            try {
                Connection c = DatabaseManager.getInstance().getConnection();
                try (PreparedStatement ps = c.prepareStatement(
                         "SELECT vehicle_type FROM parking_slots WHERE slot_id=?")) {
                    ps.setInt(1, bk.getSlotId());
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) vt = nvl(rs.getString("vehicle_type"), "car");
                }
            } catch (Exception ignored) {}
            FeeResult est = calcFee(mins, vt);
            long hh = mins/60, mm = mins%60;
            activeModel.addRow(new Object[]{
                bk.getVehiclePlate(), cap(vt), bk.getSlotNumber(),
                bk.getCheckIn() != null ? bk.getCheckIn().format(fmt) : "-",
                String.format("%dh %02dm", hh, mm),
                String.format("Rs. %.0f", est.total)
            });
        }
        lblActiveCount.setText("Active: " + active.size());
        try {
            double rev = bookingDAO.getTodayRevenue();
            lblTotalRevToday.setText("Today: Rs. " + String.format("%.0f", rev));
        } catch (Exception ignored) {}
    }

    public void refreshData() {
        refreshRight();
        if (activeBooking != null) searchBooking();
    }

    private void styleTable(JTable t) {
        t.setRowHeight(32);
        t.setFont(Theme.FONT_BODY);
        t.setGridColor(Theme.DIVIDER);
        t.setSelectionBackground(new Color(0x0EA5E9));
        t.setSelectionForeground(Color.WHITE);
        t.setShowGrid(true);

        JTableHeader h = t.getTableHeader();
        h.setOpaque(false);
        h.setPreferredSize(new Dimension(0, 36));
        h.setBorder(BorderFactory.createEmptyBorder());
        h.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tbl, Object val,
                    boolean sel, boolean foc, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(tbl, val, sel, foc, row, col);
                l.setText("  " + val.toString().toUpperCase());
                l.setForeground(Color.WHITE);
                l.setFont(new Font("Segoe UI", Font.BOLD, 11));
                l.setHorizontalAlignment(SwingConstants.LEFT);
                l.setOpaque(false);
                return l;
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, CLR_NAVY, getWidth(), 0, new Color(0x0F172A)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(34, 211, 238, 120));
                g2.fillRect(0, getHeight()-1, getWidth(), 1);
                g2.dispose();
                super.paintComponent(g);
            }
        });

        DefaultTableCellRenderer base = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tbl, Object val,
                    boolean isSel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(tbl, val, isSel, foc, row, col);
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                setBackground(isSel ? t.getSelectionBackground() : (row % 2 == 0 ? CLR_STRIPE : Color.WHITE));
                setForeground(isSel ? Color.WHITE : Theme.TEXT_DARK);
                setFont(new Font("Segoe UI", col == 0 ? Font.BOLD : Font.PLAIN, 12));
                setOpaque(true);
                return this;
            }
        };
        for (int i = 0; i < t.getColumnCount(); i++)
            t.getColumnModel().getColumn(i).setCellRenderer(base);
    }

    private JLabel statBadge(String text, Color bg) {
        JLabel l = new JLabel(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Glow Background
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                // Sleek Border
                g2.setColor(new Color(255, 255, 255, 120));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);
                // Inner Highlight
                g2.setColor(new Color(255, 255, 255, 40));
                g2.drawLine(5, 2, getWidth()-5, 2);
                super.paintComponent(g);
                g2.dispose();
            }
        };
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(Color.WHITE);
        l.setBackground(bg);
        l.setOpaque(false);
        l.setBorder(BorderFactory.createEmptyBorder(5, 18, 5, 18));
        return l;
    }

    private JLabel dv(String t) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        l.setForeground(Theme.TEXT_DARK);
        return l;
    }

    private JLabel dvBold(String t) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("Segoe UI", Font.BOLD, 13));
        l.setForeground(Theme.TEXT_DARK);
        return l;
    }

    private void error(String m) {
        JOptionPane.showMessageDialog(this, m, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private String nvl(String s)             { return s != null ? s : "-"; }
    private String nvl(String s, String def) { return (s != null && !s.isEmpty()) ? s : def; }
    private String cap(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}