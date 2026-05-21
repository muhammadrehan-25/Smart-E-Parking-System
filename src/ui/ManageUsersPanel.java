package ui;

import dao.UserDAO;
import model.User;
import util.Theme;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.List;

public class ManageUsersPanel extends JPanel {
    private final UserDAO userDAO = new UserDAO();
    private JTable table;
    private DefaultTableModel tableModel;
    private JLabel totalLbl, activeLbl, inactiveLbl, adminLbl, titleCountLbl;

    private static final Color GRAD_START = new Color(0x1E3A5F);
    private static final Color GRAD_END   = new Color(0x0F172A);
    private static final Color CLR_STRIPE = new Color(0xF8FAFC);

    public ManageUsersPanel() {
        setLayout(new BorderLayout(0, 20));
        setOpaque(true);
        setBackground(Theme.BG_WHITE);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        buildUI();
        refreshData();
    }

    private void buildUI() {
        // ── Header ────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        
        // Title with count badge
        JPanel titleWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titleWrap.setOpaque(false);
        titleWrap.add(Theme.label("User Administration", Theme.FONT_HEADER, Theme.TEXT_DARK));
        titleCountLbl = new JLabel("0 Users") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0x0EA5E9));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        titleCountLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        titleCountLbl.setForeground(Color.WHITE);
        titleCountLbl.setOpaque(false);
        titleCountLbl.setBorder(BorderFactory.createEmptyBorder(3, 12, 3, 12));
        titleWrap.add(titleCountLbl);
        topRow.add(titleWrap, BorderLayout.WEST);

        JPanel searchWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        searchWrap.setOpaque(false);
        JTextField searchField = new JTextField(15);
        searchField.setPreferredSize(new Dimension(180, 32));
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.BORDER, 1),
            BorderFactory.createEmptyBorder(0, 10, 0, 10)
        ));
        searchField.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override public void keyReleased(java.awt.event.KeyEvent e) { refreshData(searchField.getText().trim()); }
        });
        searchWrap.add(Theme.label("Search:", Theme.FONT_SMALL, Theme.TEXT_GRAY));
        searchWrap.add(searchField);
        topRow.add(searchWrap, BorderLayout.EAST);
        header.add(topRow, BorderLayout.NORTH);

        // ── Stats Cards ────────────────────────────────────────
        JPanel statsRow = new JPanel(new GridLayout(1, 4, 14, 0));
        statsRow.setOpaque(false);
        statsRow.setBorder(BorderFactory.createEmptyBorder(16, 0, 8, 0));
        statsRow.setPreferredSize(new Dimension(0, 90));

        totalLbl    = new JLabel("0");
        activeLbl   = new JLabel("0");
        inactiveLbl = new JLabel("0");
        adminLbl    = new JLabel("0");

        statsRow.add(miniStatCard("Total Users",    totalLbl,    new Color(0x0EA5E9)));
        statsRow.add(miniStatCard("Active",         activeLbl,   new Color(0x10B981)));
        statsRow.add(miniStatCard("Inactive",       inactiveLbl, new Color(0xEF4444)));
        statsRow.add(miniStatCard("Admins",         adminLbl,    new Color(0x8B5CF6)));
        header.add(statsRow, BorderLayout.CENTER);

        // ── Buttons ────────────────────────────────────────────
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        btnPanel.setOpaque(false);

        JButton addBtn    = gradButton("Add User",        new Color(0x06B6D4), new Color(0x0891B2));
        JButton editBtn   = gradButton("Edit User",       new Color(0x6366F1), new Color(0x4F46E5));
        JButton resetBtn  = gradButton("Reset Password",  new Color(0xF97316), new Color(0xEA580C));
        JButton roleBtn   = gradButton("Change Role",     new Color(0x8B5CF6), new Color(0x7C3AED));
        JButton activBtn  = gradButton("Activate",        new Color(0x10B981), new Color(0x059669));
        JButton deactBtn  = gradButton("Deactivate",      new Color(0xF59E0B), new Color(0xD97706));
        JButton deleteBtn = gradButton("Delete User",     new Color(0xEF4444), new Color(0xDC2626));

        addBtn.addActionListener(e    -> showAddUserDialog());
        editBtn.addActionListener(e   -> editSelectedUser());
        resetBtn.addActionListener(e  -> resetSelectedUserPassword());
        roleBtn.addActionListener(e   -> changeSelectedUserRole());
        activBtn.addActionListener(e  -> activateSelectedUser());
        deactBtn.addActionListener(e  -> deactivateSelectedUser());
        deleteBtn.addActionListener(e -> deleteSelectedUser());

        btnPanel.add(addBtn);
        btnPanel.add(editBtn);
        btnPanel.add(resetBtn);
        btnPanel.add(roleBtn);
        btnPanel.add(activBtn);
        btnPanel.add(deactBtn);
        btnPanel.add(deleteBtn);

        JPanel controlWrapper = new JPanel();
        controlWrapper.setLayout(new BoxLayout(controlWrapper, BoxLayout.Y_AXIS));
        controlWrapper.setOpaque(false);
        controlWrapper.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        btnPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        controlWrapper.add(btnPanel);

        header.add(controlWrapper, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        // ── Table ─────────────────────────────────────────────
        String[] cols = {"User ID", "Username", "Full Name", "Email", "Role", "Active"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(tableModel);
        table.setRowHeight(32);
        table.setFont(Theme.FONT_BODY);
        table.setShowGrid(true);
        table.setGridColor(Theme.DIVIDER);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
        table.setSelectionBackground(Theme.ACCENT);
        table.setSelectionForeground(Theme.TEXT_WHITE);

        styleTableHeader();
        styleRoleColumn();
        styleActiveColumn();
        styleDefaultColumns();

        JScrollPane sp = new JScrollPane(table);
        sp.getViewport().setBackground(Theme.BG_WHITE);
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        add(sp, BorderLayout.CENTER);
    }

    private JButton gradButton(String text, Color start, Color end) {
        JButton btn = new JButton(text) {
            private boolean hovered = false;
            {
                addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override public void mouseEntered(java.awt.event.MouseEvent e) { hovered = true; repaint(); }
                    @Override public void mouseExited(java.awt.event.MouseEvent e) { hovered = false; repaint(); }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                Color s = hovered ? start.brighter() : start;
                Color e = hovered ? end.brighter() : end;
                
                g2.setPaint(new GradientPaint(0, 0, s, 0, getHeight(), e));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                
                // Thin glass border
                g2.setColor(new Color(255, 255, 255, 40));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 10, 10);
                
                if (hovered) {
                    g2.setColor(new Color(255, 255, 255, 30));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(10, 24, 10, 24));
        return btn;
    }

    private void styleTableHeader() {
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
                g2.setPaint(new GradientPaint(0, 0, GRAD_START, getWidth(), 0, GRAD_END));
                g2.fillRect(0, 0, getWidth(), getHeight());
                // Glowing Bottom Line
                g2.setColor(new Color(34, 211, 238, 100));
                g2.fillRect(0, getHeight()-1, getWidth(), 1);
                g2.dispose();
                super.paintComponent(g);
            }
        });
    }

    private void styleRoleColumn() {
        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                JPanel cell = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 4));
                cell.setBackground(sel ? t.getSelectionBackground() : (row % 2 == 0 ? Color.WHITE : CLR_STRIPE));

                String role = v != null ? v.toString().toUpperCase() : "";
                JLabel badge = new JLabel(role) {
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
                badge.setBorder(BorderFactory.createEmptyBorder(2, 12, 2, 12));

                if (role.equals("ADMIN")) {
                    badge.setBackground(new Color(0xFEE2E2));
                    badge.setForeground(new Color(0x991B1B));
                } else {
                    badge.setBackground(new Color(0xDBEAFE));
                    badge.setForeground(new Color(0x1E40AF));
                }

                cell.add(badge);
                return cell;
            }
        });
    }

    private void styleActiveColumn() {
        table.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                JPanel cell = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 4));
                cell.setBackground(sel ? t.getSelectionBackground() : (row % 2 == 0 ? Color.WHITE : CLR_STRIPE));

                boolean active = "Yes".equals(v != null ? v.toString() : "");
                JLabel badge = new JLabel(active ? "● ACTIVE" : "● INACTIVE") {
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
                badge.setBorder(BorderFactory.createEmptyBorder(2, 12, 2, 12));

                if (active) {
                    badge.setBackground(new Color(0xD1FAE5));
                    badge.setForeground(new Color(0x059669));
                } else {
                    badge.setBackground(new Color(0xFEE2E2));
                    badge.setForeground(new Color(0xDC2626));
                }

                cell.add(badge);
                return cell;
            }
        });
    }

    private void styleDefaultColumns() {
        DefaultTableCellRenderer striped = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                setOpaque(true);
                setBackground(sel ? Theme.ACCENT : (row % 2 == 0 ? Theme.BG_CARD : CLR_STRIPE));
                setForeground(sel ? Color.WHITE : Theme.TEXT_DARK);
                setFont(Theme.FONT_BODY);
                return this;
            }
        };
        for (int i = 0; i < 4; i++)
            table.getColumnModel().getColumn(i).setCellRenderer(striped);

        int[] widths = {70, 110, 140, 190, 110, 90};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
    }

    private void refreshData() { refreshData(""); }

    private void refreshData(String search) {
        tableModel.setRowCount(0);
        List<User> users = userDAO.getAllUsers();
        int total = 0, active = 0, inactive = 0, admins = 0;
        for (User u : users) {
            String role = u.getRole() != null
                ? u.getRole().toUpperCase()
                : u.getClass().getSimpleName().toUpperCase();
            if ("CUSTOMER".equals(role)) continue;

            boolean isActive = userDAO.isUserActive(u.getUserId());
            total++;
            if (isActive) active++; else inactive++;
            if ("ADMIN".equals(role)) admins++;

            // Search filter
            if (!search.isEmpty()) {
                String s = search.toUpperCase();
                if (!u.getUsername().toUpperCase().contains(s) && 
                    !u.getFullName().toUpperCase().contains(s) &&
                    !u.getEmail().toUpperCase().contains(s)) continue;
            }

            tableModel.addRow(new Object[]{
                u.getUserId(),
                u.getUsername(),
                u.getFullName(),
                u.getEmail(),
                role,
                isActive ? "Yes" : "No"
            });
        }
        // Update stats
        if (titleCountLbl != null) titleCountLbl.setText(total + " Users");
        if (totalLbl != null)    totalLbl.setText(String.valueOf(total));
        if (activeLbl != null)   activeLbl.setText(String.valueOf(active));
        if (inactiveLbl != null) inactiveLbl.setText(String.valueOf(inactive));
        if (adminLbl != null)    adminLbl.setText(String.valueOf(admins));
    }

    private void showAddUserDialog() {
        JTextField uname = new JTextField();
        JPasswordField pass = new JPasswordField();
        JTextField fname = new JTextField();
        JTextField email = new JTextField();
        // CUSTOMER hata diya — sirf ADMIN & EMPLOYEE
        JComboBox<String> role  = new JComboBox<>(new String[]{"ADMIN", "EMPLOYEE"});
        JComboBox<String> shift = new JComboBox<>(new String[]{"morning", "evening", "night"});

        JPanel panel = new JPanel(new GridLayout(6, 2, 10, 10));
        panel.add(new JLabel("Username:"));    panel.add(uname);
        panel.add(new JLabel("Password:"));    panel.add(pass);
        panel.add(new JLabel("Full Name:"));   panel.add(fname);
        panel.add(new JLabel("Email:"));       panel.add(email);
        panel.add(new JLabel("Role:"));        panel.add(role);
        panel.add(new JLabel("Shift (Emp):")); panel.add(shift);

        int result = JOptionPane.showConfirmDialog(this, panel,
                "Add New User", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) return;

        if (uname.getText().trim().isEmpty()
                || new String(pass.getPassword()).trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Username and password required.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        boolean success = userDAO.save(
            uname.getText().trim(),
            new String(pass.getPassword()),
            fname.getText().trim(),
            email.getText().trim(),
            "",
            (String) role.getSelectedItem(),
            "EMPLOYEE".equals(role.getSelectedItem()) ? (String) shift.getSelectedItem() : null
        );
        if (success) {
            JOptionPane.showMessageDialog(this, "User added successfully.");
            refreshData();
        } else {
            JOptionPane.showMessageDialog(this,
                "Failed to add user (username may already be taken).",
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── NEW: Activate User ─────────────────────────────────────
    private void activateSelectedUser() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this,
                "Please select a user to activate.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int    userId = (int)    tableModel.getValueAt(row, 0);
        String uname  = (String) tableModel.getValueAt(row, 1);
        String active = (String) tableModel.getValueAt(row, 5);

        if ("Yes".equals(active)) {
            JOptionPane.showMessageDialog(this,
                "User '" + uname + "' is already active.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            "<html>Are you sure you want to activate user <b>" + uname + "</b>?</html>",
            "Activate User", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        if (userDAO.activateUser(userId)) {
            JOptionPane.showMessageDialog(this, "User '" + uname + "' has been activated.");
            refreshData();
        } else {
            JOptionPane.showMessageDialog(this,
                "Failed to activate user.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Deactivate User ─────────────────────
    private void deactivateSelectedUser() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this,
                "Please select a user to deactivate.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int    userId = (int)    tableModel.getValueAt(row, 0);
        String uname  = (String) tableModel.getValueAt(row, 1);
        String active = (String) tableModel.getValueAt(row, 5);

        if ("admin".equalsIgnoreCase(uname)) {
            JOptionPane.showMessageDialog(this,
                "Cannot deactivate the default admin account.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if ("No".equals(active)) {
            JOptionPane.showMessageDialog(this,
                "User '" + uname + "' is already inactive.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            "<html>Are you sure you want to deactivate user <b>" + uname + "</b>?<br>"
            + "They will no longer be able to log in.</html>",
            "Deactivate User", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        if (userDAO.deactivateUser(userId)) {
            JOptionPane.showMessageDialog(this, "User '" + uname + "' has been deactivated.");
            refreshData();
        } else {
            JOptionPane.showMessageDialog(this,
                "Failed to deactivate user.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── NEW: Delete User (permanent) ───────────────────────────
    private void deleteSelectedUser() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this,
                "Please select a user to delete.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int    userId = (int)    tableModel.getValueAt(row, 0);
        String uname  = (String) tableModel.getValueAt(row, 1);

        if ("admin".equalsIgnoreCase(uname)) {
            JOptionPane.showMessageDialog(this,
                "Cannot delete the default admin account.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            "<html>Are you sure you want to <b>permanently delete</b> user <b>" + uname + "</b>?<br>"
            + "<font color='red'>This action cannot be undone.</font></html>",
            "Delete User", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        if (userDAO.deleteUser(userId)) {
            JOptionPane.showMessageDialog(this, "User '" + uname + "' has been deleted.");
            refreshData();
        } else {
            JOptionPane.showMessageDialog(this,
                "Failed to delete user.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void editSelectedUser() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this,
                "Please select a user to edit.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int    userId   = (int)    tableModel.getValueAt(row, 0);
        String fname    = (String) tableModel.getValueAt(row, 2);
        String emailStr = (String) tableModel.getValueAt(row, 3);
        String roleStr  = (String) tableModel.getValueAt(row, 4);

        JTextField fnameField = new JTextField(fname);
        JTextField emailField = new JTextField(emailStr);
        // CUSTOMER hata diya
        JComboBox<String> role  = new JComboBox<>(new String[]{"ADMIN", "EMPLOYEE"});
        role.setSelectedItem(roleStr);
        JComboBox<String> shift = new JComboBox<>(new String[]{"morning", "evening", "night"});

        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10));
        panel.add(new JLabel("Full Name:"));   panel.add(fnameField);
        panel.add(new JLabel("Email:"));       panel.add(emailField);
        panel.add(new JLabel("Role:"));        panel.add(role);
        panel.add(new JLabel("Shift (Emp):")); panel.add(shift);

        int result = JOptionPane.showConfirmDialog(this, panel,
                "Edit User", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) return;

        boolean success = userDAO.updateUser(
            userId,
            fnameField.getText().trim(),
            emailField.getText().trim(),
            (String) role.getSelectedItem(),
            "EMPLOYEE".equals(role.getSelectedItem()) ? (String) shift.getSelectedItem() : null
        );
        if (success) {
            JOptionPane.showMessageDialog(this, "User updated successfully.");
            refreshData();
        } else {
            JOptionPane.showMessageDialog(this,
                "Failed to update user.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Reset Password ─────────────────────────────────────────
    private void resetSelectedUserPassword() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this,
                "Please select a user to reset password.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int userId = (int) tableModel.getValueAt(row, 0);
        String uname = (String) tableModel.getValueAt(row, 1);

        JPasswordField newPass = new JPasswordField();
        JPasswordField confirmPass = new JPasswordField();
        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
        panel.add(new JLabel("New Password:"));     panel.add(newPass);
        panel.add(new JLabel("Confirm Password:")); panel.add(confirmPass);

        int result = JOptionPane.showConfirmDialog(this, panel,
                "Reset Password for '" + uname + "'", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) return;

        String p1 = new String(newPass.getPassword()).trim();
        String p2 = new String(confirmPass.getPassword()).trim();
        if (p1.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Password cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!p1.equals(p2)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (userDAO.resetPassword(userId, p1)) {
            JOptionPane.showMessageDialog(this, "Password for '" + uname + "' has been reset successfully.");
        } else {
            JOptionPane.showMessageDialog(this, "Failed to reset password.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Change Role ─────────────────────────────────────────────
    private void changeSelectedUserRole() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this,
                "Please select a user to change role.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int userId = (int) tableModel.getValueAt(row, 0);
        String uname = (String) tableModel.getValueAt(row, 1);
        String currentRole = (String) tableModel.getValueAt(row, 4);

        if ("admin".equalsIgnoreCase(uname)) {
            JOptionPane.showMessageDialog(this,
                "Cannot change role of the default admin account.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String newRole = "ADMIN".equals(currentRole) ? "EMPLOYEE" : "ADMIN";
        int confirm = JOptionPane.showConfirmDialog(this,
            "<html>Change role of <b>" + uname + "</b> from <b>" + currentRole + "</b> to <b>" + newRole + "</b>?</html>",
            "Change Role", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        if (userDAO.changeRole(userId, newRole)) {
            JOptionPane.showMessageDialog(this, "Role changed to '" + newRole + "' successfully.");
            refreshData();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to change role.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Mini Stat Card ──────────────────────────────────────────
    private JPanel miniStatCard(String title, JLabel valueLbl, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 4)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(new Color(0xE2E8F0));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 12, 12);
                // Accent top bar
                g2.setColor(accent);
                g2.fillRoundRect(0, 0, getWidth(), 3, 4, 4);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(12, 14, 8, 14));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        titleLbl.setForeground(new Color(0x64748B));

        valueLbl.setFont(new Font("Segoe UI", Font.BOLD, 22));
        valueLbl.setForeground(accent);

        card.add(titleLbl, BorderLayout.NORTH);
        card.add(valueLbl, BorderLayout.CENTER);
        return card;
    }
}
