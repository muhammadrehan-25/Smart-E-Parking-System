package ui;

import dao.UserDAO;
import model.*;
import util.FileManager;
import util.Theme;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

/**
 * Redesigned High-Tech Login Screen matching the splash screen.
 * Features a split layout with the tech branding on the left and a clean glowing login form on the right.
 */
public class LoginScreen extends JFrame {
    private JTextField userField;
    private JPasswordField passField;
    private JLabel statusLabel;
    private final UserDAO userDAO = new UserDAO();

    // Design Colors (Matches Splash)
    private static final Color BG_DARK = new Color(0x020817);
    private static final Color ACCENT_BLUE = new Color(0x0EA5E9);
    private static final Color CYAN_BRIGHT = new Color(0x22D3EE);
    private static final Color TEXT_GRAY = new Color(0x64748B);

    public LoginScreen() {
        setTitle("ParkNova - Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));

        JPanel root = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                int w = getWidth(), h = getHeight();
                g2.setColor(BG_DARK);
                g2.fillRect(0, 0, w, h);

                // Tech background pattern
                drawTechPattern(g2, w, h);
                // Corner brackets
                drawBrackets(g2, w, h);

                g2.dispose();
            }
        };
        root.setPreferredSize(new Dimension(950, 600));

        // --- Left Branding Side ---
        JPanel left = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                // Subtle gradient overlay for the left side
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, new Color(14, 165, 233, 20), getWidth(), 0, new Color(0,0,0,0)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        left.setOpaque(false);
        left.setPreferredSize(new Dimension(420, 600));
        left.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(0x1E293B)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;

        // Drawn Shield Logo (Reduced size version of Splash logo)
        JPanel logo = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int cx = getWidth()/2, cy = getHeight()/2;
                
                Path2D shield = new Path2D.Float();
                shield.moveTo(cx, cy - 50);
                shield.lineTo(cx + 40, cy - 32);
                shield.lineTo(cx + 40, cy + 10);
                shield.quadTo(cx + 40, cy + 40, cx, cy + 55);
                shield.quadTo(cx - 40, cy + 40, cx - 40, cy + 10);
                shield.lineTo(cx - 40, cy - 32);
                shield.closePath();

                g2.setPaint(new GradientPaint(cx, cy-50, new Color(0x0EA5E9), cx, cy+55, new Color(0x0369A1)));
                g2.fill(shield);
                g2.setColor(CYAN_BRIGHT);
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(shield);

                g2.setFont(new Font("Segoe UI", Font.BOLD, 55));
                g2.setColor(Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString("P", cx - fm.stringWidth("P")/2 - 2, cy + 20);
                g2.dispose();
            }
        };
        logo.setOpaque(false);
        logo.setPreferredSize(new Dimension(150, 150));
        gbc.insets = new Insets(0, 0, 20, 0);
        left.add(logo, gbc);

        gbc.gridy++;
        JLabel title1 = new JLabel("PARK");
        title1.setFont(new Font("Segoe UI Black", Font.BOLD, 48));
        title1.setForeground(CYAN_BRIGHT);
        left.add(title1, gbc);

        gbc.gridy++;
        JLabel title2 = new JLabel("NOVA");
        title2.setFont(new Font("Segoe UI Black", Font.BOLD, 36));
        title2.setForeground(Color.WHITE);
        gbc.insets = new Insets(-8, 0, 10, 0);
        left.add(title2, gbc);

        gbc.gridy++;
        JLabel sub = new JLabel("Intelligent Management Console");
        sub.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 13));
        sub.setForeground(TEXT_GRAY);
        left.add(sub, gbc);

        root.add(left, BorderLayout.WEST);

        // --- Right Login Side ---
        JPanel right = new JPanel(new GridBagLayout());
        right.setOpaque(false);
        right.setBorder(BorderFactory.createEmptyBorder(40, 60, 40, 80));

        GridBagConstraints rbc = new GridBagConstraints();
        rbc.fill = GridBagConstraints.HORIZONTAL;
        rbc.gridx = 0; rbc.gridy = 0;
        rbc.weightx = 1.0;

        // Welcome Header
        JLabel welcome = new JLabel("Identity Access");
        welcome.setFont(new Font("Segoe UI Semibold", Font.BOLD, 28));
        welcome.setForeground(Color.WHITE);
        right.add(welcome, rbc);

        rbc.gridy++;
        rbc.insets = new Insets(4, 0, 40, 0);
        JLabel instruction = new JLabel("Authorized Personnel Only");
        instruction.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        instruction.setForeground(TEXT_GRAY);
        right.add(instruction, rbc);

        // Inputs
        rbc.gridy++; rbc.insets = new Insets(0, 0, 8, 0);
        right.add(label("OPERATOR ID"), rbc);
        rbc.gridy++; rbc.insets = new Insets(0, 0, 24, 0);
        userField = styledInput();
        right.add(userField, rbc);

        rbc.gridy++; rbc.insets = new Insets(0, 0, 8, 0);
        right.add(label("ACCESS KEY"), rbc);
        rbc.gridy++; rbc.insets = new Insets(0, 0, 12, 0);
        passField = styledPass();
        right.add(passField, rbc);

        rbc.gridy++; rbc.insets = new Insets(0, 0, 20, 0);
        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        statusLabel.setForeground(new Color(0xFB7185));
        right.add(statusLabel, rbc);

        // Login Button
        rbc.gridy++; rbc.insets = new Insets(0, 0, 0, 0);
        JButton loginBtn = techButton("AUTHENTICATE SYSTEM");
        loginBtn.addActionListener(e -> doLogin());
        right.add(loginBtn, rbc);

        // Close button at top right
        JButton close = new JButton("×");
        close.setFont(new Font("Arial", Font.PLAIN, 28));
        close.setForeground(TEXT_GRAY);
        close.setOpaque(false); close.setContentAreaFilled(false);
        close.setBorderPainted(false); close.setFocusPainted(false);
        close.setCursor(new Cursor(Cursor.HAND_CURSOR));
        close.addActionListener(e -> System.exit(0));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topBar.setOpaque(false);
        topBar.add(close);
        
        // Wrap right panel to allow top bar
        JPanel rightWrapper = new JPanel(new BorderLayout());
        rightWrapper.setOpaque(false);
        rightWrapper.add(topBar, BorderLayout.NORTH);
        rightWrapper.add(right, BorderLayout.CENTER);
        
        root.add(rightWrapper, BorderLayout.CENTER);

        // Enter key listeners
        KeyAdapter enter = new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) doLogin();
            }
        };
        userField.addKeyListener(enter);
        passField.addKeyListener(enter);

        setContentPane(root);
        pack();
        setLocationRelativeTo(null);
    }

    private JLabel label(String txt) {
        JLabel l = new JLabel(txt);
        l.setFont(new Font("Segoe UI", Font.BOLD, 11));
        l.setForeground(ACCENT_BLUE);
        return l;
    }

    private JTextField styledInput() {
        JTextField f = new JTextField();
        styleComponent(f);
        return f;
    }

    private JPasswordField styledPass() {
        JPasswordField f = new JPasswordField();
        styleComponent(f);
        return f;
    }

    private void styleComponent(JComponent c) {
        c.setBackground(new Color(0x0F172A));
        c.setForeground(Color.WHITE);
        c.setFont(new Font("Segoe UI", Font.BOLD, 16));
        c.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x1E293B), 1),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        c.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { c.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CYAN_BRIGHT, 1), BorderFactory.createEmptyBorder(10, 15, 10, 15))); }
            @Override public void focusLost(FocusEvent e) { c.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x1E293B), 1), BorderFactory.createEmptyBorder(10, 15, 10, 15))); }
        });
    }

    private JButton techButton(String text) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, ACCENT_BLUE, getWidth(), 0, CYAN_BRIGHT);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(Color.WHITE);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), (getWidth()-fm.stringWidth(getText()))/2, (getHeight()+fm.getAscent()-fm.getDescent())/2);
                g2.dispose();
            }
        };
        b.setPreferredSize(new Dimension(0, 50));
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setForeground(Color.WHITE);
        b.setOpaque(false); b.setContentAreaFilled(false);
        b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void drawTechPattern(Graphics2D g2, int w, int h) {
        g2.setColor(new Color(14, 165, 233, 15));
        g2.setStroke(new BasicStroke(0.8f));
        int[][] lines = {{30,50,80,50,80,100}, {w-30,h-50,w-80,h-50,w-80,h-100}, {40,h-80,90,h-80,90,h-130}};
        for (int[] l : lines) {
            Path2D path = new Path2D.Float();
            path.moveTo(l[0], l[1]); path.lineTo(l[2], l[3]); path.lineTo(l[4], l[5]);
            g2.draw(path);
        }
    }

    private void drawBrackets(Graphics2D g2, int w, int h) {
        g2.setColor(new Color(14, 165, 233, 60));
        g2.setStroke(new BasicStroke(1.5f));
        int s = 25, m = 15;
        g2.drawLine(m, m, m+s, m); g2.drawLine(m, m, m, m+s);
        g2.drawLine(w-m, m, w-m-s, m); g2.drawLine(w-m, m, w-m, m+s);
        g2.drawLine(m, h-m, m+s, h-m); g2.drawLine(m, h-m, m, h-m-s);
        g2.drawLine(w-m, h-m, w-m-s, h-m); g2.drawLine(w-m, h-m, w-m, h-m-s);
    }

    private void doLogin() {
        String username = userField.getText().trim();
        String password = new String(passField.getPassword());
        try {
            if (username.isEmpty() || password.isEmpty()) throw new IllegalArgumentException("Enter credentials.");
            User user = userDAO.authenticate(username, password);
            if (user == null) throw new SecurityException("Access Denied: Invalid Key.");

            // Check Maintenance Mode
            if (util.Settings.isMaintenanceMode() && !user.getRole().equalsIgnoreCase("ADMIN")) {
                JOptionPane.showMessageDialog(this,
                    "<html><body style='width: 250px;'>" +
                    "<h3 style='color: #E11D48;'>System Maintenance</h3>" +
                    "<p>The system is currently under maintenance by the Administrator.<br><br>" +
                    "Please try again later.</p></body></html>",
                    "Access Denied", JOptionPane.WARNING_MESSAGE);
                return;
            }

            new dao.LogDAO().log(user.getUserId(), user.getUsername(), "Login", "Successful login");

            JFrame dashboard = switch (user.getRole()) {
                case "ADMIN" -> new AdminDashboard((Admin) user);
                default      -> new EmployeeDashboard((Employee) user);
            };
            dashboard.setVisible(true);
            dispose();
        } catch (Exception ex) {
            statusLabel.setText("! " + ex.getMessage());
        }
    }
}