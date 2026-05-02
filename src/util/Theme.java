package util;

import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

/**
 * Enhanced Theme for Smart ePark.
 * Now includes "Tech" theme colors and UI components matching the futuristic Splash/Login screens.
 */
public class Theme {
    // ── Dark Tech Colors (Matching Splash/Login) ──────────
    public static final Color BG_DARK = new Color(0x020817); // Deepest blue/black
    public static final Color SIDEBAR_START = new Color(0x020817);
    public static final Color SIDEBAR_END = new Color(0x0F172A);
    public static final Color SIDEBAR_ACTIVE = new Color(0x0EA5E9);
    public static final Color CYAN_BRIGHT = new Color(0x22D3EE);
    public static final Color ACCENT_BLUE = new Color(0x0EA5E9);
    
    // ── Main content (Light theme for readability) ────────
    public static final Color BG_WHITE = new Color(0xF1F5F9);
    public static final Color BG_CARD = Color.WHITE;
    public static final Color BORDER = new Color(0xE2E8F0);
    public static final Color DIVIDER = new Color(0xF1F5F9);

    // ── Status Colours ─────────────────────────────────────
    public static final Color ACCENT = new Color(0x0EA5E9);
    public static final Color ACCENT_TEAL = new Color(0x22D3EE);
    public static final Color AMBER = new Color(0xF59E0B);
    public static final Color RED = new Color(0xEF4444);
    public static final Color FREE_GREEN = new Color(0x10B981);
    public static final Color OCC_RED = new Color(0xEF4444);
    public static final Color ACC_PURPLE = new Color(0x8B5CF6);

    // ── Text ─────────────────────────────────────────────
    public static final Color TEXT_WHITE = Color.WHITE;
    public static final Color TEXT_DARK = new Color(0x0F172A);
    public static final Color TEXT_GRAY = new Color(0x64748B);
    public static final Color TEXT_MUTED = new Color(0x94A3B8);

    // ── Fonts ────────────────────────────────────────────
    public static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 26);
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);

    private Theme() {}

    public static Color hex(String code) {
        return Color.decode(code.startsWith("#") ? code : "#" + code);
    }

    public static JLabel label(String text, Font font, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(color);
        return l;
    }

    /** 
     * Draws the High-Tech Shield Logo. 
     * Use this in sidebars/headers for resolution-independent branding.
     */
    public static void drawShieldLogo(Graphics2D g2, int cx, int cy, int size) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        float scale = size / 100f;
        int half = size / 2;

        // Shield Shape
        Path2D shield = new Path2D.Float();
        shield.moveTo(cx, cy - (35 * scale));
        shield.lineTo(cx + (28 * scale), cy - (22 * scale));
        shield.lineTo(cx + (28 * scale), cy + (8 * scale));
        shield.quadTo(cx + (28 * scale), cy + (35 * scale), cx, cy + (45 * scale));
        shield.quadTo(cx - (28 * scale), cy + (35 * scale), cx - (28 * scale), cy + (8 * scale));
        shield.lineTo(cx - (28 * scale), cy - (22 * scale));
        shield.closePath();

        // Fill with gradient
        g2.setPaint(new GradientPaint(cx, cy - (35 * scale), ACCENT_BLUE, cx, cy + (45 * scale), new Color(0x0369A1)));
        g2.fill(shield);

        // Border
        g2.setColor(CYAN_BRIGHT);
        g2.setStroke(new BasicStroke(1.5f * scale));
        g2.draw(shield);

        // Letter P
        g2.setFont(new Font("Segoe UI", Font.BOLD, (int)(42 * scale)));
        g2.setColor(Color.WHITE);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString("P", cx - fm.stringWidth("P")/2 - (int)(1*scale), cy + (int)(15*scale));
    }

    public static JButton navyButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setBackground(bg != null ? bg : ACCENT);
        b.setForeground(TEXT_WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        return b;
    }

    public static JPanel statCard(String iconText, String title, JLabel valueLabel, Color accent) {
        // White card with colored border all around + thick left accent bar
        Border normalBorder = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 5, 0, 0, accent),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(accent, 1),
                        BorderFactory.createEmptyBorder(18, 16, 18, 18)));

        Border hoverBorder = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 7, 0, 0, accent.darker()),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(accent.darker(), 1),
                        BorderFactory.createEmptyBorder(18, 14, 18, 18)));

        JPanel card = new JPanel(new BorderLayout(0, 6)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setBackground(Color.WHITE);
        card.setBorder(normalBorder);
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.setOpaque(true);

        JLabel titleLbl = label(title, FONT_SMALL, TEXT_GRAY);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        valueLabel.setForeground(accent);

        JPanel txt = new JPanel();
        txt.setOpaque(false);
        txt.setLayout(new BoxLayout(txt, BoxLayout.Y_AXIS));
        txt.add(titleLbl);
        txt.add(Box.createVerticalStrut(4));
        txt.add(valueLabel);

        card.add(txt, BorderLayout.CENTER);

        // ── Hover effect ───────────────────────────────────────────────────────
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                card.setBackground(new Color(
                        Math.min(255, 240 + accent.getRed()   / 10),
                        Math.min(255, 240 + accent.getGreen() / 10),
                        Math.min(255, 240 + accent.getBlue()  / 10)));
                card.setBorder(hoverBorder);
                card.repaint();
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                card.setBackground(Color.WHITE);
                card.setBorder(normalBorder);
                card.repaint();
            }
        });

        return card;
    }

    public static JPanel contentPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 24)) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(14, 165, 233, 40)); // Faint cyan brackets
                g2.setStroke(new BasicStroke(1.5f));
                int s = 20, m = 15, w = getWidth(), h = getHeight();
                g2.drawLine(m, m, m+s, m); g2.drawLine(m, m, m, m+s);
                g2.drawLine(w-m, m, w-m-s, m); g2.drawLine(w-m, m, w-m, m+s);
                g2.drawLine(m, h-m, m+s, h-m); g2.drawLine(m, h-m, m, h-m-s);
                g2.drawLine(w-m, h-m, w-m-s, h-m); g2.drawLine(w-m, h-m, w-m, h-m-s);
                g2.dispose();
            }
        };
        p.setBackground(BG_WHITE);
        p.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));
        return p;
    }

    public static JButton sidebarButton(String icon, String label) {
        JButton btn = new JButton(label) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getClientProperty("active") == Boolean.TRUE) {
                    GradientPaint gp = new GradientPaint(0, 0, ACCENT_BLUE, getWidth(), 0, CYAN_BRIGHT);
                    g2.setPaint(gp);
                    g2.fillRoundRect(12, 4, getWidth()-24, getHeight()-8, 10, 10);
                } else if (getModel().isRollover()) {
                    g2.setColor(new Color(0x1E293B));
                    g2.fillRoundRect(12, 4, getWidth()-24, getHeight()-8, 10, 10);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(TEXT_MUTED);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(240, 44));
        btn.setMaximumSize(new Dimension(240, 44));
        btn.setHorizontalAlignment(SwingConstants.CENTER);
        btn.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { if(btn.getClientProperty("active") != Boolean.TRUE) btn.setForeground(Color.WHITE); }
            public void mouseExited(java.awt.event.MouseEvent e) { if(btn.getClientProperty("active") != Boolean.TRUE) btn.setForeground(TEXT_MUTED); }
        });
        return btn;
    }

    public static void setNavActive(JButton btn, boolean act) {
        btn.putClientProperty("active", act);
        btn.setForeground(act ? Color.WHITE : TEXT_MUTED);
        btn.repaint();
    }

    public static void styleButton(JButton btn, Color bg) {
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
    }

    public static void styleTable(JTable table) {
        table.setRowHeight(40);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setGridColor(new Color(0xE2E8F0));
        table.setShowVerticalLines(true);
        table.setShowHorizontalLines(true);
        table.setSelectionBackground(new Color(0xE0F2FE)); // Light blue selection
        table.setSelectionForeground(new Color(0x0369A1));
        
        // Header Styling - FIXING INVISIBLE TEXT
        JTableHeader header = table.getTableHeader();
        header.setPreferredSize(new Dimension(0, 45));
        header.setBackground(new Color(0x0F172A)); // Dark Navy
        header.setForeground(Color.WHITE);
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setReorderingAllowed(false);
        
        // Custom Header Renderer to ensure background/foreground apply correctly
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                l.setBackground(new Color(0x1E293B));
                l.setForeground(Color.WHITE);
                l.setFont(new Font("Segoe UI", Font.BOLD, 13));
                l.setHorizontalAlignment(SwingConstants.CENTER);
                l.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(0x334155)));
                return l;
            }
        });
    }
}
