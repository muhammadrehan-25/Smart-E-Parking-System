package ui;

import dao.BookingDAO;
import model.*;
import util.Theme;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.List;

/** Customer self-service portal - view bookings, map, membership. */
public class CustomerDashboard extends JFrame {
    private final Customer customer;
    private final BookingDAO dao = new BookingDAO();

    public CustomerDashboard(Customer customer) {
        this.customer = customer;
        setTitle("ParkNova - Customer Portal");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setExtendedState(MAXIMIZED_BOTH);
        getContentPane().setBackground(Theme.BG_WHITE);
        setLayout(new BorderLayout());
        buildUI();
    }

    private void buildUI() {
        add(buildHeader(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(Theme.BG_WHITE);
        tabs.setForeground(Theme.TEXT_DARK);
        tabs.setFont(Theme.FONT_BODY);

        tabs.addTab("  Parking Map", buildMapTab());
        tabs.addTab("  My Bookings", buildBookingsTab());
        tabs.addTab("  My Profile",  buildProfileTab());

        tabs.setBorder(BorderFactory.createEmptyBorder(16, 20, 20, 20));
        add(tabs, BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel h = new JPanel(new BorderLayout());
        h.setBackground(Theme.BG_WHITE);
        h.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
            BorderFactory.createEmptyBorder(12, 24, 12, 24)));

        JLabel title = Theme.label("Welcome, " + customer.getFullName(), new Font("Segoe UI", Font.BOLD, 18), Theme.TEXT_DARK);
        h.add(title, BorderLayout.WEST);

        JButton logout = Theme.navyButton("Logout", Theme.RED);
        logout.setFont(Theme.FONT_SMALL);
        logout.addActionListener(e -> { dispose(); new LoginScreen().setVisible(true); });
        h.add(logout, BorderLayout.EAST);
        return h;
    }

    private JPanel buildMapTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Theme.BG_WHITE);
        ParkingMapPanel map = new ParkingMapPanel(true);
        panel.add(map, BorderLayout.CENTER);

        JPanel info = new JPanel(new FlowLayout(FlowLayout.CENTER));
        info.setBackground(Theme.BG_WHITE);
        info.add(Theme.label("Read-only map. Contact front desk to book.", Theme.FONT_BODY, Theme.TEXT_GRAY));
        panel.add(info, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildBookingsTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(Theme.BG_WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] cols = {"Booking #", "Vehicle", "Slot", "Check-In", "Check-Out", "Amount", "Status"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        // Load this customer's bookings from DB (join vehicles.customer_id via BookingDAO)
        List<Booking> all = dao.getBookingsByCustomer(customer.getUserId());
        for (Booking b : all) {
            model.addRow(new Object[]{
                b.getConfCode(), b.getVehiclePlate(), b.getSlotNumber(),
                b.getCheckIn() != null ? b.getCheckIn().toString().replace("T", " ").substring(0, 16) : "",
                b.getCheckOut() != null ? b.getCheckOut().toString().replace("T", " ").substring(0, 16) : "Active",
                b.getTotalAmount() > 0 ? "Rs. " + String.format("%.0f", b.getTotalAmount()) : "-",
                b.getStatus()
            });
        }

        JTable table = new JTable(model);
        table.setBackground(Theme.BG_WHITE); table.setForeground(Theme.TEXT_DARK);
        table.setFont(Theme.FONT_BODY); table.setRowHeight(30);
        table.setGridColor(Theme.BORDER);
        table.getTableHeader().setBackground(Theme.BORDER);
        table.getTableHeader().setForeground(Theme.TEXT_DARK);
        table.getTableHeader().setFont(Theme.FONT_SUBTITLE);
        table.setSelectionBackground(Theme.ACCENT.darker());
        table.setSelectionForeground(Theme.TEXT_WHITE);

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        sp.getViewport().setBackground(Theme.BG_WHITE);
        panel.add(sp, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildProfileTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Theme.BG_WHITE);

        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL; gc.gridx = 0;
        gc.insets = new Insets(6, 0, 6, 0);

        JPanel card = new JPanel();
        card.setBackground(Theme.BG_WHITE);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.BORDER),
            BorderFactory.createEmptyBorder(24, 32, 24, 32)));
        card.setPreferredSize(new Dimension(420, 0));

        card.add(Theme.label("Profile Information", Theme.FONT_HEADER, Theme.TEXT_DARK));
        card.add(Box.createVerticalStrut(16));
        card.add(profileRow("Full Name", customer.getFullName()));
        card.add(profileRow("Username", customer.getUsername()));
        card.add(profileRow("Email", customer.getEmail()));
        card.add(profileRow("Phone", customer.getPhone() != null ? customer.getPhone() : "-"));
        card.add(profileRow("Role", customer.getRole()));
        card.add(Box.createVerticalStrut(16));
        card.add(Theme.label("Membership", Theme.FONT_HEADER, Theme.TEXT_DARK));
        card.add(Box.createVerticalStrut(8));
        card.add(profileRow("Status", customer.hasMembership() ? "Active" : "Not subscribed"));
        card.add(profileRow("Discount", customer.hasMembership()
            ? customer.getMembershipDiscount() + "%" : "-"));

        panel.add(card, gc);
        return panel;
    }

    private JPanel profileRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        row.add(Theme.label(label, Theme.FONT_SMALL, Theme.TEXT_GRAY), BorderLayout.WEST);
        row.add(Theme.label(value, Theme.FONT_BODY, Theme.TEXT_DARK), BorderLayout.EAST);
        return row;
    }
}
