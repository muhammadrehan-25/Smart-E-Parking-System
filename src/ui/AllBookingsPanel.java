package ui;

import dao.BookingDAO;
import model.Booking;
import util.Theme;
import util.FileManager;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public class AllBookingsPanel extends JPanel {
    private final BookingDAO bookingDAO = new BookingDAO();
    private JTable table;
    private DefaultTableModel tableModel;
    private JComboBox<String> statusFilter;
    private Integer filterEmployeeId = null;
    private boolean filterToday = false;

    // Top nav bar gradient (header row) - Lightened for better visibility
    private static final Color HDR_START  = new Color(0x1E3A5F);
    private static final Color HDR_END    = new Color(0x0F172A);

    // Sleek Indigo-Blue for date-separator rows (Matches theme better)
    private static final Color SEP_START  = new Color(0x312E81);
    private static final Color SEP_END    = new Color(0x1E1B4B);
    private static final Color SEP_TEXT   = Color.WHITE;

    private static final String SEP = "\u0000SEP\u0000";

    public AllBookingsPanel() { this(null, false); }

    public AllBookingsPanel(Integer employeeId, boolean todayOnly) {
        this.filterEmployeeId = employeeId;
        this.filterToday      = todayOnly;
        setLayout(new BorderLayout(0, 20));
        setOpaque(true);
        setBackground(Theme.BG_WHITE);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        buildUI();
        refreshData();
    }

    private boolean isSepRow(int modelRow) {
        Object v = tableModel.getValueAt(modelRow, 0);
        return v instanceof String s && s.startsWith(SEP);
    }

    private String sepLabel(int modelRow) {
        Object v = tableModel.getValueAt(modelRow, 0);
        return v instanceof String s ? s.substring(SEP.length()) : "";
    }

    private void buildUI() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(Theme.label("Booking History", Theme.FONT_HEADER, Theme.TEXT_DARK), BorderLayout.WEST);

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        filters.setOpaque(false);

        // Modern Search Bar
        JTextField searchField = new JTextField(12);
        searchField.setPreferredSize(new Dimension(150, 32));
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.BORDER, 1),
            BorderFactory.createEmptyBorder(0, 10, 0, 10)
        ));
        searchField.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override public void keyReleased(java.awt.event.KeyEvent e) { refreshData(searchField.getText().trim()); }
        });

        statusFilter = new JComboBox<>(new String[]{"All Status", "Active", "Completed"});
        statusFilter.setFont(Theme.FONT_BODY);
        statusFilter.addActionListener(e -> refreshData(searchField.getText().trim()));

        JButton exportBtn = new JButton("EXPORT CSV");
        Theme.styleButton(exportBtn, Theme.ACCENT);
        exportBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        exportBtn.setPreferredSize(new Dimension(110, 32));
        exportBtn.setMaximumSize(new Dimension(110, 32));
        exportBtn.addActionListener(e -> {
            String path = FileManager.exportBookingsCSV(bookingDAO.getAllBookings());
            JOptionPane.showMessageDialog(this,
                path != null ? "Exported to:\n" + path : "Export failed.",
                "Export", JOptionPane.INFORMATION_MESSAGE);
        });

        filters.add(Theme.label("Search:", Theme.FONT_SMALL, Theme.TEXT_GRAY));
        filters.add(searchField);
        filters.add(statusFilter);
        filters.add(exportBtn);
        header.add(filters, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        String[] cols = {"Conf #", "Vehicle Plate", "Type", "Slot", "Check-In", "Check-Out", "Hours", "Amount", "Status"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(tableModel) {

            // ── FIX: overdraw only separator rows, never data rows ──
            @Override
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                for (int row = 0; row < getRowCount(); row++) {
                    if (!isSepRow(row)) continue;
                    Rectangle r = getCellRect(row, 0, true);
                    r.x = 0; r.width = getWidth();
                    // Modern slate-style date separator
                    g2.setPaint(new GradientPaint(0, r.y, SEP_START, getWidth(), r.y, SEP_END));
                    g2.fillRect(0, r.y, r.width, r.height);
                    
                    // Cyan accent line
                    g2.setColor(new Color(0x0EA5E9));
                    g2.fillRect(0, r.y, 4, r.height);
                    
                    // Subtle borders
                    g2.setColor(new Color(0x4338CA)); // Indigo border
                    g2.drawLine(0, r.y, getWidth(), r.y);
                    g2.drawLine(0, r.y + r.height - 1, getWidth(), r.y + r.height - 1);
                    
                    String label = sepLabel(row);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    g2.setColor(SEP_TEXT);
                    FontMetrics fm = g2.getFontMetrics();
                    int ty = r.y + (r.height + fm.getAscent() - fm.getDescent()) / 2;
                    g2.drawString(label.toUpperCase(), 16, ty);
                }
                g2.dispose();
            }

            @Override
            public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (isSepRow(row)) {
                    if (c instanceof JLabel jl) jl.setText("");
                    return c;
                }
                if (isRowSelected(row)) {
                    c.setBackground(new Color(0x0EA5E9));
                    c.setForeground(Color.WHITE);
                } else {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(0xF8FAFC));
                    c.setForeground(Theme.TEXT_DARK);
                }
                return c;
            }
        };

        styleTable();

        JScrollPane sp = new JScrollPane(table);
        sp.getViewport().setBackground(Theme.BG_WHITE);
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        add(sp, BorderLayout.CENTER);

        JPanel summary = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 6));
        summary.setOpaque(false);
        summary.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER));
        JLabel totalLbl = Theme.label("", Theme.FONT_SMALL, Theme.TEXT_GRAY);
        JLabel revLbl   = Theme.label("", Theme.FONT_SMALL, Theme.ACCENT);
        summary.add(totalLbl);
        summary.add(revLbl);
        add(summary, BorderLayout.SOUTH);

        this.putClientProperty("totalLbl", totalLbl);
        this.putClientProperty("revLbl",   revLbl);
    }

    public void refreshData() { refreshData(""); }

    public void refreshData(String search) {
        tableModel.setRowCount(0);

        List<Booking> bookings = bookingDAO.getAllBookings();
        String filter   = (String) statusFilter.getSelectedItem();
        String todayStr = LocalDate.now().toString();

        List<Booking> filtered = new ArrayList<>();
        for (Booking b : bookings) {
            if (filterEmployeeId != null && b.getEmployeeId() != filterEmployeeId) continue;
            if (filterToday && b.getCheckIn() != null
                    && !b.getCheckIn().toString().startsWith(todayStr)) continue;
            if (!"All Status".equals(filter)
                    && !b.getStatus().name().equalsIgnoreCase(filter)) continue;
            
            // Search filter
            if (!search.isEmpty()) {
                String s = search.toUpperCase();
                if (!b.getVehiclePlate().toUpperCase().contains(s) && 
                    !b.getConfCode().toUpperCase().contains(s)) continue;
            }
            filtered.add(b);
        }

        filtered.sort((a, b2) -> {
            if (a.getCheckIn() == null) return 1;
            if (b2.getCheckIn() == null) return -1;
            return b2.getCheckIn().compareTo(a.getCheckIn());
        });

        String currentDate = null;
        double totalRev = 0;
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("EEEE,  dd  MMMM  yyyy");

        for (Booking b : filtered) {
            String dateKey = b.getCheckIn() != null
                ? b.getCheckIn().toString().substring(0, 10) : "Unknown Date";

            if (!dateKey.equals(currentDate)) {
                String label;
                try { label = LocalDate.parse(dateKey).format(dayFmt); }
                catch (Exception ex) { label = dateKey; }
                tableModel.addRow(new Object[]{SEP + label, "", "", "", "", "", "", "", ""});
                currentDate = dateKey;
            }

            String checkIn  = b.getCheckIn()  != null
                ? b.getCheckIn().toString().replace("T"," ").substring(0,16) : "";
            String checkOut = b.getCheckOut() != null
                ? b.getCheckOut().toString().replace("T"," ").substring(0,16) : "\u2014";
            String hours    = b.getTotalHours()  > 0
                ? String.format("%.1f h", b.getTotalHours())  : "\u2014";
            String amount   = b.getTotalAmount() > 0
                ? "Rs. " + String.format("%.0f", b.getTotalAmount()) : "\u2014";
            String status   = b.getStatus().name().substring(0,1).toUpperCase()
                            + b.getStatus().name().substring(1).toLowerCase();
            String vType    = capitalize(b.getVehicleType());

            tableModel.addRow(new Object[]{
                b.getConfCode(), b.getVehiclePlate(), vType, b.getSlotNumber(),
                checkIn, checkOut, hours, amount, status
            });
            totalRev += b.getTotalAmount();
        }

        JLabel tl = (JLabel) this.getClientProperty("totalLbl");
        JLabel rl = (JLabel) this.getClientProperty("revLbl");
        if (tl != null) tl.setText("Total records: " + filtered.size());
        if (rl != null) rl.setText("Total Revenue: Rs. " + String.format("%.0f", totalRev));
    }

    private void styleTable() {
        table.setRowHeight(32);
        table.setFont(Theme.FONT_BODY);
        table.setGridColor(Theme.DIVIDER);
        table.setShowGrid(true);

        // ── Gradient header (dark navy — same as top nav) ──────
        JTableHeader h = table.getTableHeader();
        h.setOpaque(false);
        h.setPreferredSize(new Dimension(0, 38));
        h.setBorder(BorderFactory.createEmptyBorder());
        h.setReorderingAllowed(false);
        h.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val,
                    boolean sel, boolean foc, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(tbl, val, sel, foc, row, col);
                l.setText("  " + val.toString().toUpperCase());
                l.setForeground(Color.WHITE);
                l.setFont(new Font("Segoe UI", Font.BOLD, 11));
                l.setHorizontalAlignment(SwingConstants.LEFT);
                l.setOpaque(false);
                return l;
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Cyber-Glow Header
                g2.setPaint(new GradientPaint(0, 0, HDR_START, getWidth(), 0, HDR_END));
                g2.fillRect(0, 0, getWidth(), getHeight());
                // Glowing Bottom Line
                g2.setColor(new Color(34, 211, 238, 100));
                g2.fillRect(0, getHeight()-1, getWidth(), 1);
                g2.dispose();
                super.paintComponent(g);
            }
        });

        table.setSelectionBackground(Theme.ACCENT);
        table.setSelectionForeground(Theme.TEXT_WHITE);

        // ── Status column colour ───────────────────────────────
        table.getColumnModel().getColumn(8).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                if (isSepRow(row)) return super.getTableCellRendererComponent(t, "", sel, foc, row, col);
                
                String status = v.toString();
                JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 4));
                p.setOpaque(true);
                p.setBackground(sel ? t.getSelectionBackground() : (row % 2 == 0 ? Color.WHITE : new Color(0xF8FAFC)));
                
                JLabel badge = new JLabel(status.toUpperCase()) {
                    @Override protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(getBackground());
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                        super.paintComponent(g);
                        g2.dispose();
                    }
                };
                badge.setFont(new Font("Segoe UI", Font.BOLD, 10));
                badge.setOpaque(false);
                badge.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));
                
                if (status.equalsIgnoreCase("Completed")) {
                    badge.setBackground(new Color(0xD1FAE5));
                    badge.setForeground(new Color(0x059669));
                } else if (status.equalsIgnoreCase("Active")) {
                    badge.setBackground(new Color(0xDBEAFE));
                    badge.setForeground(new Color(0x2563EB));
                } else {
                    badge.setBackground(new Color(0xF1F5F9));
                    badge.setForeground(new Color(0x475569));
                }
                
                p.add(badge);
                return p;
            }
        });

        int[] widths = {90, 110, 80, 70, 140, 140, 70, 90, 90};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }
}