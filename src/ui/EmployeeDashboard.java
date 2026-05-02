package ui;

import dao.BookingDAO;
import model.Employee;
import util.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.List;
import model.Booking;

public class EmployeeDashboard extends JFrame {
    private final Employee employee;
    private final BookingDAO bookingDAO = new BookingDAO();
    private CardLayout cardLayout;
    private JPanel mainContentPanel;
    private JLabel topBarTitle, weatherLabel;
    // Stats for Dashboard
    private JLabel revenueLabel, occupiedLabel, bookingsLabel, usersLabel;
    private JTable recentBookingsTable;
    private javax.swing.table.DefaultTableModel tableModel;
    private RevenueBarChart revenueChart;
    private OccupancyDonutChart donutChart;
    private JPanel activityFeedPanel;

    public EmployeeDashboard(Employee employee) {
        this.employee = employee;
        setTitle("ParkNova – Operator Dashboard");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1280, 800);
        setMinimumSize(new Dimension(1000, 700));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(0, 0));
        buildUI();
        setupShortcuts();
        refreshData();
        updateWeather();
        showShiftAlert();
        startAutoRefresh();
    }

    private void startAutoRefresh() {
        new javax.swing.Timer(30000, e -> refreshData()).start();
    }

    private void updateWeather() {
        EmployeeDashboard self = this;
        new Thread(() -> {
            try {
                String w = util.WeatherService.fetchWeather(self);
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

        // 1. Dashboard Summary
        mainContentPanel.add(buildDashboardPanel(), "Dashboard");

        // 2. Slot Status
        JPanel mapPanel = Theme.contentPanel();
        // Removed redundant "Slot Status" title
        mapPanel.add(new ParkingMapPanel(employee.getUserId()), BorderLayout.CENTER);
        mainContentPanel.add(mapPanel, "Slot Status");

        // 3. Check In & Out
        mainContentPanel.add(new CheckInPanel(employee.getUserId()), "Check In");
        mainContentPanel.add(new CheckOutPanel(), "Check Out");

        // 4. Global Parking History (shows all bookings)
        mainContentPanel.add(new AllBookingsPanel(), "Parking History");

        // Topbar wrapper
        JPanel topWrapper = new JPanel(new BorderLayout());
        topWrapper.setBackground(Theme.BG_WHITE);
        topWrapper.add(buildTopBar(), BorderLayout.NORTH);
        topWrapper.add(mainContentPanel, BorderLayout.CENTER);

        add(buildSidebar(), BorderLayout.WEST);
        add(topWrapper, BorderLayout.CENTER);
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, new Color(0x0F172A), getWidth(), 0, new Color(0x020817)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                float[] dist = {0.0f, 1.0f};
                Color[] colors = {new Color(34, 211, 238, 30), new Color(34, 211, 238, 0)};
                g2.setPaint(new RadialGradientPaint(new Point2D.Float(0, 0), getWidth() / 2f, dist, colors));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(34, 211, 238, 80));
                g2.fillRect(0, getHeight() - 1, getWidth(), 1);
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(14, 28, 14, 28));

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setOpaque(false);
        topBarTitle = Theme.label("Dashboard", new Font("Segoe UI", Font.BOLD, 22), Theme.TEXT_WHITE);
        JLabel dateLbl = Theme.label("", Theme.FONT_SMALL, new Color(0xA0B4C8));
        leftPanel.add(topBarTitle);
        leftPanel.add(Box.createVerticalStrut(2));
        leftPanel.add(dateLbl);
        bar.add(leftPanel, BorderLayout.WEST);

        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.X_AXIS));
        right.setOpaque(false);

        JPanel clockGroup = new JPanel();
        clockGroup.setLayout(new BoxLayout(clockGroup, BoxLayout.Y_AXIS));
        clockGroup.setOpaque(false);
        JLabel clockLbl = Theme.label("", new Font("Segoe UI", Font.BOLD, 20), Theme.ACCENT);
        weatherLabel = Theme.label("Loading weather...", Theme.FONT_SMALL, new Color(0x94A3B8));
        clockLbl.setAlignmentX(Component.RIGHT_ALIGNMENT);
        weatherLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        clockGroup.add(clockLbl);
        clockGroup.add(weatherLabel);

        JPanel userGrp = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        userGrp.setOpaque(false);
        userGrp.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(255,255,255,20)));
        JLabel userLbl = Theme.label(employee.getFullName(), Theme.FONT_BODY, Theme.TEXT_WHITE);
        userGrp.add(userLbl);

        right.add(clockGroup);
        right.add(Box.createHorizontalStrut(20));
        right.add(userGrp);
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
        side.setPreferredSize(new Dimension(240, 0));
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(0x1E3A5F)));

        JPanel logoArea = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Theme.drawShieldLogo((Graphics2D)g, getWidth()/2, 45, 75);
            }
        };
        logoArea.setPreferredSize(new Dimension(240, 80));
        logoArea.setMaximumSize(new Dimension(240, 80));
        logoArea.setAlignmentX(Component.CENTER_ALIGNMENT);
        side.add(logoArea);

        JLabel appTitle = Theme.label("ParkNova", new Font("Segoe UI", Font.BOLD, 15), Color.WHITE);
        appTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        side.add(appTitle);
        side.add(Box.createVerticalStrut(10));

        JLabel appSub = Theme.label("OPERATOR CONSOLE", new Font("Segoe UI", Font.BOLD, 9), Theme.ACCENT_BLUE);
        appSub.setAlignmentX(Component.CENTER_ALIGNMENT);
        side.add(appSub);
        side.add(Box.createVerticalStrut(15));

        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(0x1E3A5F));
        sep.setMaximumSize(new Dimension(200, 1));
        sep.setAlignmentX(Component.CENTER_ALIGNMENT);
        side.add(sep);
        side.add(Box.createVerticalStrut(8));

        // Nav items
        Object[][] navItems = {
            {"", "Dashboard"}, {"", "Slot Status"}, {"", "Check In"}, {"", "Check Out"}, {"", "Parking History"}
        };

        java.util.List<JButton> navBtns = new java.util.ArrayList<>();

        for (Object[] item : navItems) {
            String name = (String) item[1];
            JButton btn = Theme.sidebarButton("", name);
            navBtns.add(btn);
            btn.addActionListener(e -> {
                cardLayout.show(mainContentPanel, name);
                topBarTitle.setText(name);
                navBtns.forEach(b -> Theme.setNavActive(b, false));
                Theme.setNavActive(btn, true);
                if (name.equals("Dashboard")) refreshData();
                refreshData();
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
        if (!navBtns.isEmpty()) Theme.setNavActive(navBtns.get(0), true);

        side.add(Box.createVerticalGlue());

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

        JLabel avatar = new JLabel("E");
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
        names.add(Theme.label(employee.getFullName(), Theme.FONT_SMALL, Theme.TEXT_WHITE));
        names.add(Theme.label("Operator (" + employee.getShift().toUpperCase() + ")", Theme.FONT_SMALL, new Color(0x64748B)));
        userInfo.add(names);
        side.add(userInfo);

        // Logout
        JButton logout = new JButton("  Logout") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(0xDC2626), 0, getHeight(), new Color(0x991B1B));
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
            try {
                // Calculate shift summary
                int todayCount = bookingDAO.getTodayCount(); // Simplified: using total today
                double todayRev = bookingDAO.getTodayRevenue();
                
                new dao.LogDAO().log(employee.getUserId(), employee.getUsername(), "Logout", "Shift ended");

                JOptionPane.showMessageDialog(this,
                    "<html><body style='width: 300px; padding: 10px;'>"
                    + "<h2 style='color: #1E3A5F; margin-bottom: 5px;'>Shift Summary</h2>"
                    + "<hr color='#E2E8F0'>"
                    + "<p style='font-size: 13px; color: #475569;'>"
                    + "Operator: <b>" + employee.getFullName() + "</b><br>"
                    + "Shift: <b>" + employee.getShift().toUpperCase() + "</b><br><br>"
                    + "Vehicles Handled Today: <b style='color: #0EA5E9;'>" + todayCount + "</b><br>"
                    + "Total Revenue Collected: <b style='color: #10B981;'>Rs. " + (int)todayRev + "</b><br><br>"
                    + "<i>Good job today! Take care.</i></p></body></html>",
                    "Shift Completed", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) { ex.printStackTrace(); }
            
            dispose();
            new LoginScreen().setVisible(true);
        });
        side.add(Box.createVerticalStrut(10));
        side.add(logout);
        side.add(Box.createVerticalStrut(14));

        return side;
    }

    private ImageIcon loadSidebarLogo() {
        try {
            java.io.File f = new java.io.File("resources/logo.png");
            if (f.exists()) {
                Image img = new ImageIcon(f.getAbsolutePath()).getImage().getScaledInstance(-1, 54, Image.SCALE_SMOOTH);
                return new ImageIcon(img);
            }
        } catch (Exception e) { }
        return null;
    }

        // ── Dashboard main panel ──────────────────────────────────────────────────
    private JPanel buildDashboardPanel() {
        JPanel main = new JPanel(new BorderLayout(0, 20));
        main.setOpaque(false);
        main.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // Stats row — full width
        JPanel statsRow = new JPanel(new GridLayout(1, 4, 16, 0));
        statsRow.setOpaque(false);
        statsRow.setPreferredSize(new Dimension(0, 130));

        revenueLabel  = new JLabel("Rs. 0");
        occupiedLabel = new JLabel("0 / 0");
        bookingsLabel = new JLabel("0");
        usersLabel    = new JLabel("0");

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

        JPanel feedCard = buildCard("Live Activity Feed");
        activityFeedPanel = new JPanel();
        activityFeedPanel.setOpaque(false);
        activityFeedPanel.setLayout(new BoxLayout(activityFeedPanel, BoxLayout.Y_AXIS));
        JScrollPane feedScroll = new JScrollPane(activityFeedPanel);
        feedScroll.setBorder(null);
        feedScroll.setOpaque(false);
        feedScroll.getViewport().setOpaque(false);
        
        // --- Custom Thin Scrollbar ---
        feedScroll.getVerticalScrollBar().setPreferredSize(new Dimension(5, 0));
        feedScroll.getVerticalScrollBar().setUnitIncrement(16);
        feedScroll.getVerticalScrollBar().setBackground(new Color(0,0,0,0));
        feedScroll.getVerticalScrollBar().setOpaque(false);
        
        feedCard.add(feedScroll, BorderLayout.CENTER);
        bottomRow.add(feedCard);

        main.add(bottomRow, BorderLayout.CENTER);
        return main;
    }

    private JPanel buildCard(String title) {
        JPanel card = new JPanel(new BorderLayout(0, 15));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0xE2E8F0), 1),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        JLabel lbl = Theme.label(title, new Font("Segoe UI", Font.BOLD, 16), Theme.TEXT_DARK);
        card.add(lbl, BorderLayout.NORTH);
        return card;
    }

    // ── Occupancy Donut Chart ──────────────────────────────────────────────────
    private class OccupancyDonutChart extends JPanel {
        private int car = 0, bike = 0, free = 100;
        OccupancyDonutChart() { setOpaque(false); }
        void update(int c, int b, int f) { this.car = c; this.bike = b; this.free = f; repaint(); }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int size = Math.min(getWidth(), getHeight()) - 40;
            int x = (getWidth()-size)/2, y = (getHeight()-size)/2;
            int total = car + bike + free;
            if (total == 0) total = 1;
            int a1 = (int)(360.0 * car / total), a2 = (int)(360.0 * bike / total);
            g2.setStroke(new BasicStroke(28, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(0x0EA5E9)); g2.drawArc(x, y, size, size, 90, -a1);
            g2.setColor(new Color(0x8B5CF6)); g2.drawArc(x, y, size, size, 90-a1, -a2);
            g2.setColor(new Color(0xE2E8F0)); g2.drawArc(x, y, size, size, 90-a1-a2, -(360-a1-a2));
            String pct = (int)(100.0 * (car+bike)/total) + "%";
            g2.setFont(new Font("Segoe UI", Font.BOLD, 24));
            g2.setColor(Theme.TEXT_DARK);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(pct, (getWidth()-fm.stringWidth(pct))/2, (getHeight()+fm.getAscent())/2 - 5);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
            g2.setColor(new Color(0x94A3B8));
            String sub = "Occupied";
            g2.drawString(sub, (getWidth()-g2.getFontMetrics().stringWidth(sub))/2, (getHeight()+fm.getAscent())/2 + 15);
            
            // Legend
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            int lx = x, ly = y + size + 25;
            drawLeg(g2, lx, ly, new Color(0x0EA5E9), "Car: "+car);
            drawLeg(g2, lx+60, ly, new Color(0x8B5CF6), "Bike: "+bike);
            drawLeg(g2, lx+125, ly, new Color(0xE2E8F0), "Free: "+free);
        }
        private void drawLeg(Graphics2D g2, int x, int y, Color c, String t) {
            g2.setColor(c); g2.fillOval(x, y-7, 8, 8);
            g2.setColor(new Color(0x64748B)); g2.drawString(t, x+12, y);
        }
    }

    private void styleDashboardTable(JTable t) {
        t.setBackground(Theme.BG_CARD);
        t.setForeground(Theme.TEXT_DARK);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        t.setRowHeight(36);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setFillsViewportHeight(true);

        javax.swing.table.JTableHeader h = t.getTableHeader();
        h.setPreferredSize(new Dimension(0, 38));
        h.setDefaultRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val,
                    boolean sel, boolean foc, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(tbl, val, sel, foc, row, col);
                l.setText("  " + val);
                l.setForeground(Color.WHITE);
                l.setFont(new Font("Segoe UI", Font.BOLD, 12));
                l.setHorizontalAlignment(SwingConstants.LEFT);
                l.setOpaque(false);
                return l;
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, Theme.SIDEBAR_START, getWidth(), 0, new Color(0x0D2040)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        });
    }

    public void refreshData() {
        try {
            double rev      = bookingDAO.getTodayRevenue();
            int    todayCnt = bookingDAO.getTodayCount();
            int[]  occ      = bookingDAO.getOccupiedCounts();
            int    totalS   = util.Settings.getTotalSlots();
            int    active   = (int) new dao.UserDAO().getAllUsers().stream().filter(u -> u.isActive()).count();

            revenueLabel.setText("Rs. " + String.format("%.0f", rev));
            occupiedLabel.setText((occ[0] + occ[1]) + " / " + totalS);
            bookingsLabel.setText(String.valueOf(todayCnt));
            usersLabel.setText(String.valueOf(active));

            if (revenueChart != null) revenueChart.setData(bookingDAO.getDailyRevenueLast7Days());
            if (donutChart != null)   donutChart.update(occ[0], occ[1], totalS - (occ[0]+occ[1]));

            // Update Activity Feed
            if (activityFeedPanel != null) {
                activityFeedPanel.removeAll();
                List<Booking> recent = bookingDAO.getAllBookings();
                int count = 0;
                for (Booking b : recent) {
                    if (count++ >= 8) break;
                    activityFeedPanel.add(createFeedItem(b));
                    activityFeedPanel.add(Box.createVerticalStrut(10));
                }
                activityFeedPanel.revalidate();
                activityFeedPanel.repaint();
            }
        } catch (Exception ex) {
            System.err.println("refreshData error: " + ex.getMessage());
        }
    }

    private JPanel createFeedItem(Booking b) {
        boolean isOut = b.getStatus() == Booking.BookingStatus.COMPLETED;
        JPanel p = new JPanel(new BorderLayout(12, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0xF8FAFC));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                // Status vertical bar
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
                                  new Font("Segoe UI", Font.PLAIN, 11), new Color(0x64748B));
        
        info.add(title);
        info.add(desc);
        p.add(info, BorderLayout.CENTER);

        String timeStr = b.getCheckIn().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        if (isOut && b.getCheckOut() != null) {
            timeStr = b.getCheckOut().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        }
        
        JLabel timeLbl = Theme.label(timeStr, new Font("Segoe UI", Font.BOLD, 11), new Color(0x94A3B8));
        p.add(timeLbl, BorderLayout.EAST);

        return p;
    }

    private void showShiftAlert() {
        String shift = employee.getShift().toLowerCase();
        String endTime = switch (shift) {
            case "morning" -> "04:00 PM";
            case "evening" -> "12:00 AM";
            case "night"   -> "08:00 AM";
            default        -> "Unknown";
        };

        Timer t = new Timer(800, e -> {
            JOptionPane.showMessageDialog(this,
                "<html><body style='width: 320px; padding: 10px;'>"
                + "<h2 style='color: #2D3748;'>Welcome, " + employee.getFullName() + "!</h2>"
                + "<p style='font-size: 13px; color: #4A5568; line-height: 1.5;'>"
                + "You are logged in for the <b style='color: #3182CE;'>" + shift.toUpperCase() + "</b> shift.<br><br>"
                + "🕒 Your shift ends at <b style='color: #E53E3E;'>" + endTime + "</b>.<br>"
                + "Please ensure all activities are recorded correctly.</p>"
                + "</body></html>",
                "Shift Info", JOptionPane.INFORMATION_MESSAGE);
        });
        t.setRepeats(false);
        t.start();
    }

    // ── Revenue Bar Chart ──────────────────────────────────────────────────────
    private class RevenueBarChart extends JPanel {
        private java.util.LinkedHashMap<String, Double> data = new java.util.LinkedHashMap<>();
        RevenueBarChart() { setOpaque(false); }
        void setData(java.util.LinkedHashMap<String, Double> d) { this.data = d; repaint(); }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (data == null || data.isEmpty()) return;
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int padL = 40, padR = 10, padT = 20, padB = 45;
            int chartW = getWidth() - padL - padR, chartH = getHeight() - padT - padB;
            double maxVal = data.values().stream().max(Double::compareTo).orElse(100.0);
            if (maxVal == 0) maxVal = 100.0;
            
            // Grid
            g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[]{5f}, 0f));
            for (int i = 0; i <= 4; i++) {
                int y = padT + chartH - (i * chartH / 4);
                g2.setColor(new Color(0xF1F5F9)); g2.drawLine(padL, y, getWidth()-padR, y);
                g2.setColor(new Color(0x94A3B8)); g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                String lbl = (int)(maxVal * i / 4) + "";
                g2.drawString(lbl, padL - g2.getFontMetrics().stringWidth(lbl) - 8, y + 4);
            }

            // Bars
            String[] keys = data.keySet().toArray(new String[0]);
            int n = keys.length, barSlot = chartW / n, barW = (int)(barSlot * 0.6), barOff = (barSlot - barW) / 2;
            for (int i = 0; i < n; i++) {
                double v = data.get(keys[i]);
                int bH = (int)((v / maxVal) * chartH), x = padL + i * barSlot + barOff, y = padT + chartH - bH;
                g2.setPaint(new GradientPaint(x, y, new Color(0x0EA5E9), x, padT + chartH, new Color(0x38BDF8)));
                g2.fillRoundRect(x, y, barW, bH, 6, 6);
                
                // Two-line Labels (Day/Date)
                g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
                g2.setColor(new Color(0x64748B));
                String[] parts = keys[i].split(" "); // e.g. "Mon 25"
                if (parts.length == 2) {
                    g2.drawString(parts[0], x + (barW - g2.getFontMetrics().stringWidth(parts[0]))/2, padT + chartH + 15);
                    g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
                    g2.drawString(parts[1], x + (barW - g2.getFontMetrics().stringWidth(parts[1]))/2, padT + chartH + 28);
                } else {
                    g2.drawString(keys[i], x + (barW - g2.getFontMetrics().stringWidth(keys[i]))/2, padT + chartH + 15);
                }
            }
        }
    }
}
