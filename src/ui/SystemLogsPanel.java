package ui;

import dao.LogDAO;
import util.Theme;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class SystemLogsPanel extends JPanel {
    private final LogDAO logDAO = new LogDAO();
    private JTable table;
    private DefaultTableModel model;

    public SystemLogsPanel() {
        setLayout(new BorderLayout(0, 20));
        setBackground(Theme.BG_WHITE);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titlePanel.setOpaque(false);
        titlePanel.add(Theme.label("Staff Activity Logs", Theme.FONT_HEADER, Theme.TEXT_DARK));
        header.add(titlePanel, BorderLayout.WEST);

        JButton refreshBtn = new JButton("Refresh Logs");
        Theme.styleButton(refreshBtn, Theme.ACCENT);
        refreshBtn.setPreferredSize(new Dimension(140, 38));
        refreshBtn.addActionListener(e -> refreshData());
        header.add(refreshBtn, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // Table Setup
        String[] cols = {"TIME & DATE", "OPERATOR", "ACTION TYPE", "ACTIVITY DETAILS"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        Theme.styleTable(table);
        
        // Column Widths
        table.getColumnModel().getColumn(0).setPreferredWidth(180);
        table.getColumnModel().getColumn(1).setPreferredWidth(100);
        table.getColumnModel().getColumn(2).setPreferredWidth(130);
        table.getColumnModel().getColumn(3).setPreferredWidth(350);
        
        // Professional Row Renderer
        table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                JLabel lbl = (JLabel) c;
                lbl.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));
                
                if (!sel) {
                    lbl.setBackground(row % 2 == 0 ? Color.WHITE : new Color(0xF9FAFB));
                    if (col == 2) { // Action Type Column
                        String val = String.valueOf(v).toLowerCase();
                        if (val.contains("login")) lbl.setForeground(new Color(0x0284C7));
                        else if (val.contains("delete")) lbl.setForeground(new Color(0xEF4444));
                        else if (val.contains("check")) lbl.setForeground(new Color(0x10B981));
                        else lbl.setForeground(Theme.TEXT_DARK);
                        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
                    } else {
                        lbl.setForeground(Theme.TEXT_DARK);
                        lbl.setFont(Theme.FONT_BODY);
                    }
                } else {
                    lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
                }
                return c;
            }
        });

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));
        sp.getViewport().setBackground(Color.WHITE);
        add(sp, BorderLayout.CENTER);

        refreshData();
    }

    public void refreshData() {
        model.setRowCount(0);
        List<String[]> logs = logDAO.getAllLogs();
        for (String[] row : logs) {
            model.addRow(row);
        }
    }
}
