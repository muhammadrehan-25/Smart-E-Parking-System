package ui;
import dao.BookingDAO;
import model.*;
import util.FileManager;
import util.Theme;
import util.WeatherService;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.List;

public class AdminDashboard extends JFrame {
    private final Admin admin;
    private final BookingDAO bookingDAO = new BookingDAO();
    private JLabel revenueLabel, occupiedLabel, bookingsLabel, usersLabel;
    private JTable bookingsTable;
    private DefaultTableModel tableModel;
    private CardLayout cardLayout;
    private JPanel mainContentPanel;
    private JPanel topBarPanel;
    private JLabel topBarTitle;
    private RevenueBarChart revenueChart;
    private OccupancyDonutChart donutChart;
    private JLabel weatherLabel, greetingLabel, notificationBadge;
    private JPanel activityFeedPanel, alertsPanel;

    public AdminDashboard(Admin admin) {
        this.admin = admin;
        setTitle("ParkNova – Admin Control Panel");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1280, 800);
        setMinimumSize(new Dimension(1000, 700));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(0, 0));
        buildUI();
        setupShortcuts();
        refreshData();
        updateWeather();
        startAutoRefresh();
    }

    private void startAutoRefresh() {
        new javax.swing.Timer(30000, e -> refreshData()).start();
    }

    private void updateWeather() {
        AdminDashboard self = this;
        new Thread(() -> {
            try {
                // fetchWeather will show dialog on EDT internally if needed
                String w = WeatherService.fetchWeather(self);
                SwingUtilities.invokeLater(() -> {
                    if (weatherLabel != null) weatherLabel.setText(w);
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    if (weatherLabel != null) weatherLabel.setText("Weather unavailable");
                });
            }
        }).start();
    }

    private void setupShortcuts() {
        // Ctrl + F for Fullscreen toggle
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK), "toggleFullscreen");
        getRootPane().getActionMap().put("toggleFullscreen", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                if (getExtendedState() == JFrame.MAXIMIZED_BOTH) {
                    setExtendedState(JFrame.NORMAL);
                } else {
                    setExtendedState(JFrame.MAXIMIZED_BOTH);
                }
            }
        });
    }

    private void buildUI() {
        cardLayout = new CardLayout();
        mainContentPanel = new JPanel(cardLayout);
        mainContentPanel.setBackground(Theme.BG_WHITE);

        // Build panels
        mainContentPanel.add(buildDashboardPanel(), "Dashboard");

        JPanel mapWrap = Theme.contentPanel();
        // Removed redundant "Slot Status" title
        mapWrap.add(new ParkingMapPanel(admin.getUserId()), BorderLayout.CENTER);
        mainContentPanel.add(mapWrap, "Slot Status");

        mainContentPanel.add(new AllBookingsPanel(), "All Bookings");
        mainContentPanel.add(new ManageUsersPanel(), "Manage Users");
        mainContentPanel.add(new CheckInPanel(admin.getUserId()), "Check In");
        mainContentPanel.add(new CheckOutPanel(), "Check Out");
        mainContentPanel.add(new SettingsPanel(), "Settings");
        mainContentPanel.add(new SystemLogsPanel(), "System Logs");

        // Topbar
        JPanel topWrapper = new JPanel(new BorderLayout());
        topWrapper.setBackground(Theme.BG_WHITE);
        topBarPanel = buildTopBar();
        topWrapper.add(topBarPanel, BorderLayout.NORTH);
        topWrapper.add(mainContentPanel, BorderLayout.CENTER);

        add(buildSidebar(), BorderLayout.WEST);
        add(topWrapper, BorderLayout.CENTER);
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Base Gradient
                g2.setPaint(new GradientPaint(0, 0, new Color(0x0F172A), getWidth(), 0, new Color(0x020817)));
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Top-Left Cyan Glow
                float[] dist = {0.0f, 1.0f};
                Color[] colors = {new Color(34, 211, 238, 30), new Color(34, 211, 238, 0)};
                g2.setPaint(new RadialGradientPaint(new Point2D.Float(0, 0), getWidth() / 2f, dist, colors));
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Subtle bottom highlight
                g2.setColor(new Color(34, 211, 238, 80));
                g2.fillRect(0, getHeight() - 1, getWidth(), 1);
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0x1E3A5F)),
            BorderFactory.createEmptyBorder(14, 28, 14, 28)));

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setOpaque(false);
        topBarTitle = Theme.label("Dashboard", new Font("Segoe UI", Font.BOLD, 22), Theme.TEXT_WHITE);
        JLabel dateLbl = Theme.label("", Theme.FONT_SMALL, new Color(0xA0B4C8));
        leftPanel.add(topBarTitle);
        leftPanel.add(Box.createVerticalStrut(2));
        leftPanel.add(dateLbl);
        bar.add(leftPanel, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 0));
        right.setOpaque(false);

        JLabel clockLbl = Theme.label("", new Font("Segoe UI", Font.BOLD, 20), Theme.ACCENT);
        JLabel userLbl  = Theme.label(admin.getFullName() + " (" + admin.getUsername() + ")",
                                      Theme.FONT_BODY, Theme.TEXT_WHITE);

        JPanel clockPanel = new JPanel(new BorderLayout());
        clockPanel.setOpaque(false);
        clockPanel.add(clockLbl, BorderLayout.CENTER);
        
        weatherLabel = Theme.label("Sunny, 39°C | Karachi", new Font("Segoe UI", Font.BOLD, 13), new Color(0x94A3B8));
        clockPanel.add(weatherLabel, BorderLayout.SOUTH);

        notificationBadge = Theme.label("0 Alerts", new Font("Segoe UI", Font.BOLD, 10), Color.WHITE);
        notificationBadge.setBackground(new Color(0xEF4444));
        notificationBadge.setOpaque(true);
        notificationBadge.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        notificationBadge.setVisible(false);
        
        right.add(notificationBadge);
        right.add(clockPanel);
        right.add(new JSeparator(SwingConstants.VERTICAL) {{
            setPreferredSize(new Dimension(1, 28));
            setForeground(new Color(0x2E4A6F));
        }});
        right.add(userLbl);
        bar.add(right, BorderLayout.EAST);

        javax.swing.Timer clock = new javax.swing.Timer(1000, e -> {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            clockLbl.setText(now.format(java.time.format.DateTimeFormatter.ofPattern("hh:mm:ss a")));
            dateLbl.setText(now.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy")));
        });
        clock.setInitialDelay(0);
        clock.start();

        return bar;
    }

    private JPanel buildSidebar() {
        JPanel side = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Base Gradient (Deep Navy to Black)
                g2.setPaint(new GradientPaint(0, 0, new Color(0x0F172A), 0, getHeight(), new Color(0x020817)));
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Top-Left Cyan Glow
                float[] dist = {0.0f, 1.0f};
                Color[] colors = {new Color(34, 211, 238, 25), new Color(34, 211, 238, 0)};
                g2.setPaint(new RadialGradientPaint(new Point2D.Float(0, 0), getWidth() * 1.5f, dist, colors));
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Subtle glowing line on the right edge
                g2.setColor(new Color(34, 211, 238, 60));
                g2.drawLine(getWidth()-1, 0, getWidth()-1, getHeight());
                g2.dispose();
            }
        };
        side.setOpaque(false);
        side.setPreferredSize(new Dimension(230, 0));
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(0x1E3A5F)));

        // Logo area
        JPanel logoArea = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Theme.drawShieldLogo((Graphics2D)g, getWidth()/2, 45, 75);
            }
        };
        logoArea.setOpaque(false);
        logoArea.setPreferredSize(new Dimension(230, 80));
        logoArea.setMaximumSize(new Dimension(230, 80));
        logoArea.setAlignmentX(Component.CENTER_ALIGNMENT);
        side.add(logoArea);

        JLabel appTitle = Theme.label("ParkNova", new Font("Segoe UI", Font.BOLD, 15), Color.WHITE);
        appTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        side.add(appTitle);
        side.add(Box.createVerticalStrut(10));

        JLabel appSub = Theme.label("SYSTEM ADMINISTRATION", new Font("Segoe UI", Font.BOLD, 9), Theme.ACCENT_BLUE);
        appSub.setAlignmentX(Component.CENTER_ALIGNMENT);
        side.add(appSub);
        side.add(Box.createVerticalStrut(15));

        // Separator
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(0x1E3A5F));
        sep.setMaximumSize(new Dimension(200, 1));
        sep.setAlignmentX(Component.CENTER_ALIGNMENT);
        side.add(sep);
        side.add(Box.createVerticalStrut(8));

        // Nav items
        Object[][] navItems = {
            {"", "Dashboard"}, {"", "Slot Status"}, {"", "All Bookings"},
            {"", "Manage Users"}, {"", "Check In"}, {"", "Check Out"}, {"", "Settings"}, {"", "System Logs"}
        };

        java.util.List<JButton> navBtns = new java.util.ArrayList<>();

        for (Object[] item : navItems) {
            String icon = (String) item[0];
            String name = (String) item[1];
            JButton btn = Theme.sidebarButton(icon, name);
            navBtns.add(btn);
            btn.addActionListener(e -> {
                cardLayout.show(mainContentPanel, name);
                topBarTitle.setText(name);
                // Update active state
                navBtns.forEach(b -> Theme.setNavActive(b, false));
                Theme.setNavActive(btn, true);
                if (name.equals("Dashboard")) refreshData();
                for (Component c : mainContentPanel.getComponents()) {
                    if (c.isVisible()) {
                        if (c instanceof AllBookingsPanel p) p.refreshData();
                        else if (c instanceof CheckInPanel p) p.refreshData();
                        else if (c instanceof CheckOutPanel p) p.refreshData();
                    }
                }
            });
            side.add(btn);
        }
        // Mark Dashboard as initially active
        if (!navBtns.isEmpty()) Theme.setNavActive(navBtns.get(0), true);

        side.add(Box.createVerticalGlue());

        side.add(Box.createVerticalStrut(8));
        JSeparator sep2 = new JSeparator();
        sep2.setForeground(new Color(0x1E3A5F));
        sep2.setMaximumSize(new Dimension(200, 1));
        sep2.setAlignmentX(Component.CENTER_ALIGNMENT);
        side.add(sep2);

        // User info
        JPanel userInfo = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 14));
        userInfo.setOpaque(false);
        userInfo.setAlignmentX(Component.CENTER_ALIGNMENT);
        userInfo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));

        JLabel avatar = new JLabel("A");
        avatar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        avatar.setForeground(Theme.TEXT_DARK);
        avatar.setBackground(Theme.ACCENT);
        avatar.setOpaque(true);
        avatar.setPreferredSize(new Dimension(30, 30));
        avatar.setHorizontalAlignment(SwingConstants.CENTER);
        userInfo.add(avatar);

        JPanel names = new JPanel();
        names.setOpaque(false);
        names.setLayout(new BoxLayout(names, BoxLayout.Y_AXIS));
        names.add(Theme.label(admin.getFullName(), Theme.FONT_SMALL, Theme.TEXT_WHITE));
        names.add(Theme.label("Administrator", Theme.FONT_SMALL, new Color(0x64748B)));
        userInfo.add(names);
        side.add(userInfo);

        // Logout
        JButton logout = new JButton("  Logout") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(0xDC2626),
                                                      0, getHeight(), new Color(0x991B1B));
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        logout.setContentAreaFilled(false);
        logout.setHorizontalAlignment(SwingConstants.CENTER);
        logout.setFont(Theme.FONT_BODY);
        logout.setForeground(Color.WHITE);
        logout.setBorderPainted(false);
        logout.setFocusPainted(false);
        logout.setOpaque(false);
        logout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logout.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        logout.setAlignmentX(Component.CENTER_ALIGNMENT);
        logout.setMaximumSize(new Dimension(210, 36));
        logout.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { logout.setForeground(new Color(0xFECACA)); }
            public void mouseExited(java.awt.event.MouseEvent e)  { logout.setForeground(Color.WHITE); }
        });
        logout.addActionListener(e -> { 
            new dao.LogDAO().log(admin.getUserId(), admin.getUsername(), "Logout", "Admin logged out");
            dispose(); 
            new LoginScreen().setVisible(true); 
        });
        side.add(Box.createVerticalStrut(10));
        side.add(logout);
        side.add(Box.createVerticalStrut(14));

        return side;
    }

    // ── Dashboard main panel ──────────────────────────────────────────────────
    private JPanel buildDashboardPanel() {
        JPanel main = new JPanel(new BorderLayout(0, 20));
        main.setOpaque(false);
        main.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // Stats row — full width, original size
        JPanel statsRow = new JPanel(new GridLayout(1, 4, 16, 0));
        statsRow.setOpaque(false);
        statsRow.setPreferredSize(new Dimension(0, 130));

        revenueLabel  = new JLabel("Rs. 0");
        occupiedLabel = new JLabel("0 / 75");
        bookingsLabel = new JLabel("0");
        usersLabel    = new JLabel("3");

        statsRow.add(Theme.statCard("", "Today's Revenue",  revenueLabel,  Theme.ACCENT));
        statsRow.add(Theme.statCard("", "Slots Occupied",   occupiedLabel, Theme.OCC_RED));
        statsRow.add(Theme.statCard("", "Today's Bookings", bookingsLabel, Theme.AMBER));
        statsRow.add(Theme.statCard("", "Active Users",     usersLabel,    Theme.ACC_PURPLE));
        main.add(statsRow, BorderLayout.NORTH);

        // Bottom row — 3 equal cards: Revenue | Donut | Activity Feed
        JPanel bottomRow = new JPanel(new GridLayout(1, 3, 20, 0));
        bottomRow.setOpaque(false);

        JPanel revCard = buildCard("Daily Revenue Trends");
        revenueChart = new RevenueBarChart();
        revCard.add(revenueChart, BorderLayout.CENTER);
        bottomRow.add(revCard);

        JPanel occCard = buildCard("Occupancy Breakdown");
        donutChart = new OccupancyDonutChart();
        occCard.add(donutChart, BorderLayout.CENTER);
        bottomRow.add(occCard);

        // ── Activity Feed Scroll Pane ──
        activityFeedPanel = new JPanel();
        activityFeedPanel.setOpaque(false);
        activityFeedPanel.setLayout(new BoxLayout(activityFeedPanel, BoxLayout.Y_AXIS));
        JScrollPane feedScroll = new JScrollPane(activityFeedPanel);
        feedScroll.setBorder(null);
        feedScroll.setOpaque(false);
        feedScroll.getViewport().setOpaque(false);

        // ── Alerts Scroll Pane ──
        alertsPanel = new JPanel();
        alertsPanel.setOpaque(false);
        alertsPanel.setLayout(new BoxLayout(alertsPanel, BoxLayout.Y_AXIS));
        JScrollPane alertsScroll = new JScrollPane(alertsPanel);
        alertsScroll.setBorder(null);
        alertsScroll.setOpaque(false);
        alertsScroll.getViewport().setOpaque(false);
        
        // ── Tabbed View ──
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 11));
        tabs.addTab("LIVE FEED", feedScroll);
        tabs.addTab("ALERTS", alertsScroll);
        
        JPanel rightCol = buildCard("Updates & Alerts");
        rightCol.add(tabs, BorderLayout.CENTER);
        bottomRow.add(rightCol);

        main.add(bottomRow, BorderLayout.CENTER);

        return main;
    }

    private void buildTable() {
        String[] cols = {"Conf #", "Vehicle", "Slot", "Check-In", "Status", "Amount"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        bookingsTable = new JTable(tableModel);
        styleTable(bookingsTable);
    }

    public void refreshData() {
        try {
            double rev      = bookingDAO.getTodayRevenue();
            int    todayCnt = bookingDAO.getTodayCount();
            List<model.ParkingSlot> all = bookingDAO.getAllSlots();
            int carOcc = 0, bikeOcc = 0;
            if (all != null) {
                for (model.ParkingSlot s : all) {
                    if (!s.isFree()) {
                        if ("car".equals(s.getVehicleType())) carOcc++;
                        else bikeOcc++;
                    }
                }
            }
            int total = (all != null) ? all.size() : 0;
            int active = (int) new dao.UserDAO().getAllUsers().stream().filter(u -> u.isActive()).count();

            revenueLabel.setText("Rs. " + String.format("%.0f", rev));
            occupiedLabel.setText((carOcc + bikeOcc) + " / " + total);
            bookingsLabel.setText(String.valueOf(todayCnt));
            if (usersLabel != null) usersLabel.setText(String.valueOf(active));

            if (revenueChart != null) revenueChart.setData(bookingDAO.getDailyRevenueLast7Days());
            if (donutChart != null) donutChart.setData(carOcc, bikeOcc, total - (carOcc + bikeOcc));

            // Refresh Activity Feed
            if (activityFeedPanel != null) {
                activityFeedPanel.removeAll();
                List<Booking> recent = bookingDAO.getRecentActivity(8);
                for (Booking b : recent) {
                    activityFeedPanel.add(buildActivityItem(b));
                    activityFeedPanel.add(Box.createVerticalStrut(10));
                }
                activityFeedPanel.revalidate();
                activityFeedPanel.repaint();
            }
            
            // Refresh Alerts
            if (alertsPanel != null) {
                alertsPanel.removeAll();
                List<Booking> activeBookings = bookingDAO.getActiveBookings();
                int alertCount = 0;
                for (Booking b : activeBookings) {
                    if (b.computeHours() > 24) {
                        alertsPanel.add(buildAlertItem(b));
                        alertsPanel.add(Box.createVerticalStrut(8));
                        alertCount++;
                    }
                }
                if (alertCount == 0) {
                    JLabel none = Theme.label("No active alerts", Theme.FONT_SMALL, Theme.TEXT_MUTED);
                    none.setAlignmentX(Component.CENTER_ALIGNMENT);
                    alertsPanel.add(none);
                }
                notificationBadge.setText(alertCount + " Alerts");
                notificationBadge.setVisible(alertCount > 0);
                alertsPanel.revalidate();
                alertsPanel.repaint();
            }

        } catch (Exception ex) {
            System.err.println("refreshData error: " + ex.getMessage());
        }
    }

    private JPanel buildActivityItem(Booking b) {
        boolean isOut = b.getStatus() == Booking.BookingStatus.COMPLETED;
        JPanel p = new JPanel(new BorderLayout(12, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0xF8FAFC));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(isOut ? new Color(0x10B981) : new Color(0x0EA5E9));
                g2.fillRect(0, 4, 3, getHeight() - 8);
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));

        JLabel icon = new JLabel(isOut ? "OUT" : "IN");
        icon.setFont(new Font("Segoe UI", Font.BOLD, 10));
        icon.setForeground(isOut ? new Color(0x10B981) : new Color(0x0EA5E9));
        p.add(icon, BorderLayout.WEST);

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        
        JLabel title = Theme.label(b.getVehiclePlate() + " • " + b.getSlotNumber(), 
                                   new Font("Segoe UI", Font.BOLD, 13), Theme.TEXT_DARK);
        JLabel desc = Theme.label((isOut ? "Checked Out" : "Checked In") + " • " + b.getVehicleType().toUpperCase(),
                                  new Font("Segoe UI", Font.PLAIN, 11), Theme.TEXT_MUTED);
        
        info.add(title);
        info.add(desc);
        p.add(info, BorderLayout.CENTER);

        String time = isOut ? b.getCheckOut().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                            : b.getCheckIn().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        JLabel timeLbl = Theme.label(time, new Font("Segoe UI", Font.BOLD, 11), new Color(0x94A3B8));
        p.add(timeLbl, BorderLayout.EAST);

        return p;
    }

    private JPanel buildAlertItem(Booking b) {
        JPanel p = new JPanel(new BorderLayout(12, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0xFFF1F2));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(new Color(0xE11D48));
                g2.fillRect(0, 4, 3, getHeight() - 8);
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));

        JLabel icon = new JLabel("!");
        icon.setFont(new Font("Segoe UI", Font.BOLD, 18));
        icon.setForeground(new Color(0xE11D48));
        p.add(icon, BorderLayout.WEST);

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        
        JLabel title = Theme.label(b.getVehiclePlate() + " OVERTIME", 
                                   new Font("Segoe UI", Font.BOLD, 13), new Color(0x9F1239));
        JLabel desc = Theme.label("Parked for " + String.format("%.1f", b.computeHours()) + " hours",
                                  new Font("Segoe UI", Font.PLAIN, 11), Theme.TEXT_MUTED);
        
        info.add(title);
        info.add(desc);
        p.add(info, BorderLayout.CENTER);

        return p;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private JPanel buildCard(String title) {
        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBackground(Theme.BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.BORDER, 1),
            BorderFactory.createEmptyBorder(16, 16, 12, 16)));
        card.add(Theme.label(title, Theme.FONT_SUBTITLE, Theme.TEXT_DARK), BorderLayout.NORTH);
        return card;
    }

    private void styleTable(JTable t) {
        t.setBackground(Theme.BG_CARD);
        t.setForeground(Theme.TEXT_DARK);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        t.setRowHeight(36);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setFillsViewportHeight(true);

        // ── Gradient Header ──────────────────────────────────────────────────
        JTableHeader h = t.getTableHeader();
        h.setPreferredSize(new Dimension(0, 44));
        h.setReorderingAllowed(false);
        h.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                l.setForeground(Color.WHITE);
                l.setFont(new Font("Segoe UI", Font.BOLD, 13));
                l.setHorizontalAlignment(SwingConstants.LEFT);
                l.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));
                l.setOpaque(false);
                return l;
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, Theme.SIDEBAR_START, getWidth(), 0, new Color(0x0D2040));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        });
        h.setReorderingAllowed(false);

        // ── Alternating row renderer ───────────────────────────────────────────
        Color rowEven = new Color(0xF0F8FF);   // alice-blue — very light
        Color rowOdd  = new Color(0xE6F3FB);   // matches BG_CARD
        Color rowSel  = new Color(0x00D4AA);   // teal accent
        Color confCol = new Color(0x0369A1);   // steel-blue for Conf #

        DefaultTableCellRenderer baseRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                if (isSelected) {
                    setBackground(rowSel);
                    setForeground(Color.WHITE);
                } else {
                    setBackground(row % 2 == 0 ? rowEven : rowOdd);
                    setForeground(column == 0 ? confCol : Theme.TEXT_DARK);
                    if (column == 0) setFont(new Font("Segoe UI", Font.BOLD, 13));
                    else             setFont(new Font("Segoe UI", Font.PLAIN, 13));
                }
                setOpaque(true);
                return this;
            }
        };
        // Apply base renderer to all columns
        for (int i = 0; i < t.getColumnCount(); i++) {
            t.getColumnModel().getColumn(i).setCellRenderer(baseRenderer);
        }

        // ── Status column (col 4) — coloured badges ────────────────────────────
        t.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val,
                    boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(tbl, val, sel, foc, row, col);
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                setFont(new Font("Segoe UI", Font.BOLD, 12));
                if (sel) {
                    setBackground(rowSel);
                    setForeground(Color.WHITE);
                } else {
                    setBackground(row % 2 == 0 ? rowEven : rowOdd);
                    String s = String.valueOf(val).toLowerCase();
                    if      (s.contains("active"))    setForeground(new Color(0xD97706)); // amber-600
                    else if (s.contains("completed")) setForeground(new Color(0x059669)); // green-600
                    else if (s.contains("cancel"))    setForeground(new Color(0xDC2626)); // red-600
                    else                              setForeground(Theme.TEXT_DARK);
                }
                setOpaque(true);
                return this;
            }
        });

        // ── Selection colours ─────────────────────────────────────────────────
        t.setSelectionBackground(rowSel);
        t.setSelectionForeground(Color.WHITE);
    }

    private void styleScrollPane(JScrollPane sp) {
        sp.getViewport().setBackground(Theme.BG_CARD);
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));
    }

    private ImageIcon loadSidebarLogo() {
        try {
            java.io.File f = new java.io.File("resources/logo.png");
            if (f.exists()) {
                Image img = new ImageIcon(f.getAbsolutePath()).getImage()
                    .getScaledInstance(-1, 54, Image.SCALE_SMOOTH);
                return new ImageIcon(img);
            }
        } catch (Exception e) { /* ignore */ }
        return null;
    }

    private ImageIcon loadSmallLogo() {
        try {
            java.io.File f = new java.io.File("resources/logo.png");
            if (f.exists()) {
                Image img = new ImageIcon(f.getAbsolutePath()).getImage()
                    .getScaledInstance(32, -1, Image.SCALE_SMOOTH);
                return new ImageIcon(img);
            }
        } catch (Exception e) { /* ignore */ }
        return null;
    }

    // ── Revenue Bar Chart ─────────────────────────────────────────────────────
    /** 7-day daily revenue bar chart drawn with Java2D — no external library. */
    private class RevenueBarChart extends JPanel {
        private java.util.LinkedHashMap<String, Double> data = new java.util.LinkedHashMap<>();

        RevenueBarChart() {
            setOpaque(false);
            setPreferredSize(new Dimension(0, 180));
        }

        void setData(java.util.LinkedHashMap<String, Double> d) { data = d; repaint(); }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (data == null || data.isEmpty()) return;
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            int padL = 28, padR = 8, padT = 14, padB = 42;
            int chartW = w - padL - padR;
            int chartH = h - padT - padB;

            // Background
            g2.setColor(new Color(0xF8FAFF));
            g2.fillRoundRect(0, 0, w - 1, h - 1, 10, 10);

            // Y-axis grid lines
            double maxVal = data.values().stream().max(Double::compareTo).orElse(1.0);
            if (maxVal == 0) maxVal = 500;
            int gridLines = 4;
            g2.setStroke(new java.awt.BasicStroke(0.5f,
                    java.awt.BasicStroke.CAP_BUTT, java.awt.BasicStroke.JOIN_MITER,
                    1, new float[]{4}, 0));
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            for (int i = 0; i <= gridLines; i++) {
                int y = padT + (int)(chartH * (1.0 - (double)i / gridLines));
                g2.setColor(new Color(0xDDE4EF));
                g2.drawLine(padL, y, w - padR, y);
                g2.setColor(Theme.TEXT_GRAY);
                double val = maxVal * i / gridLines;
                String lbl = val >= 1000 ? String.format("%.0fK", val/1000) : String.format("%.0f", val);
                g2.drawString(lbl, padL - g2.getFontMetrics().stringWidth(lbl) - 4, y + 4);
            }
            g2.setStroke(new java.awt.BasicStroke(1f));

            // Bars
            String[] keys = data.keySet().toArray(new String[0]);
            int n = keys.length;
            int barSlot = chartW / n;
            int barW    = (int)(barSlot * 0.55);
            int barOff  = (barSlot - barW) / 2;

            Color barColor  = new Color(0x0EA5E9);
            Color barToday  = Theme.ACCENT;

            for (int i = 0; i < n; i++) {
                double val     = data.get(keys[i]);
                int barH       = (int)((val / maxVal) * chartH);
                int x          = padL + i * barSlot + barOff;
                int y          = padT + chartH - barH;
                boolean isLast = (i == n - 1);

                // Bar gradient
                GradientPaint gp = new GradientPaint(x, y, isLast ? barToday : barColor,
                        x, padT + chartH, isLast ? barToday.darker() : barColor.darker());
                g2.setPaint(gp);
                g2.fillRoundRect(x, barH > 0 ? y : y - 1, barW, Math.max(barH, 2), 4, 4);

                // Value label on bar
                if (barH > 16) {
                    String rv = val >= 1000 ? String.format("%.0fK", val/1000) : String.format("%.0f", val);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                    g2.setColor(Color.WHITE);
                    int lw = g2.getFontMetrics().stringWidth(rv);
                    g2.drawString(rv, x + (barW - lw) / 2, y + 13);
                }

                // Day label — split into 2 lines (day + date)
                g2.setFont(new Font("Segoe UI", Font.BOLD, 8));
                g2.setColor(isLast ? barToday : Theme.TEXT_DARK);
                String[] parts = keys[i].split(" ");
                String dayPart = parts.length > 0 ? parts[0] : keys[i];
                String datePart = parts.length > 1 ? parts[1] : "";
                int dw1 = g2.getFontMetrics().stringWidth(dayPart);
                int dw2 = g2.getFontMetrics().stringWidth(datePart);
                g2.drawString(dayPart, x + (barW - dw1) / 2, padT + chartH + 12);
                g2.setColor(isLast ? barToday : Theme.TEXT_GRAY);
                g2.drawString(datePart, x + (barW - dw2) / 2, padT + chartH + 24);
            }

            // Axis line
            g2.setColor(Theme.BORDER);
            g2.setStroke(new java.awt.BasicStroke(1.5f));
            g2.drawLine(padL, padT, padL, padT + chartH);
            g2.drawLine(padL, padT + chartH, w - padR, padT + chartH);
        }
    }

    // ── Occupancy Donut Chart ────────────────────────────────────────────────
    private class OccupancyDonutChart extends JPanel {
        private int car, bike, free;

        OccupancyDonutChart() { setOpaque(false); setPreferredSize(new Dimension(0, 180)); }

        void setData(int c, int b, int f) { this.car = c; this.bike = b; this.free = f; repaint(); }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int size = Math.min(getWidth(), getHeight()) - 40;
            int x = (getWidth() - size) / 2;
            int y = (getHeight() - size) / 2;

            int total = car + bike + free;
            if (total == 0) total = 1;

            double carAngle = (car * 360.0) / total;
            double bikeAngle = (bike * 360.0) / total;
            double freeAngle = (free * 360.0) / total;

            int start = 90;
            // Free
            g2.setColor(new Color(0xE2E8F0));
            g2.fillArc(x, y, size, size, start, (int)-freeAngle);
            start -= freeAngle;
            // Car
            g2.setPaint(new GradientPaint(x, y, new Color(0x0EA5E9), x+size, y+size, new Color(0x0369A1)));
            g2.fillArc(x, y, size, size, start, (int)-carAngle);
            start -= carAngle;
            // Bike
            g2.setPaint(new GradientPaint(x, y, new Color(0x8B5CF6), x+size, y+size, new Color(0x5B21B6)));
            g2.fillArc(x, y, size, size, start, (int)-bikeAngle);

            // Center hole (Donut effect)
            g2.setColor(Color.WHITE);
            int hole = (int)(size * 0.6);
            g2.fillOval(x + (size-hole)/2, y + (size-hole)/2, hole, hole);

            // Text in middle
            int occ = car + bike;
            int pct = (occ * 100) / total;
            g2.setFont(new Font("Segoe UI", Font.BOLD, 18));
            g2.setColor(Theme.TEXT_DARK);
            String s = pct + "%";
            int sw = g2.getFontMetrics().stringWidth(s);
            g2.drawString(s, x + size/2 - sw/2, y + size/2 + 7);

            // Legend
            g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
            drawLegend(g2, 10, getHeight()-30, new Color(0x0EA5E9), "Car: " + car);
            drawLegend(g2, 80, getHeight()-30, new Color(0x8B5CF6), "Bike: " + bike);
            drawLegend(g2, 150, getHeight()-30, new Color(0xE2E8F0), "Free: " + free);

            g2.dispose();
        }
        
        private void drawLegend(Graphics2D g2, int x, int y, Color c, String t) {
            g2.setColor(c);
            g2.fillOval(x, y-7, 8, 8);
            g2.setColor(Theme.TEXT_MUTED);
            g2.drawString(t, x + 12, y);
        }
    }
}
