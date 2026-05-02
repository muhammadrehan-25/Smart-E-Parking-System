package ui;

import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;

/**
 * High-Tech Theme for ParkNova.
 * Synchronized with util.Theme to ensure consistency across the application.
 */
public class Theme {
    // ── Dark Tech Colors (Matching Splash/Login) ──────────
    public static final Color BG_DARK = new Color(0x020817);
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

    public static void drawShieldLogo(Graphics2D g2, int cx, int cy, int size) {
        util.Theme.drawShieldLogo(g2, cx, cy, size);
    }

    public static JLabel label(String text, Font font, Color color) {
        return util.Theme.label(text, font, color);
    }

    public static JButton navyButton(String text, Color bg) {
        return util.Theme.navyButton(text, bg);
    }

    public static JPanel statCard(String iconText, String title, JLabel valueLabel, Color accent) {
        return util.Theme.statCard(iconText, title, valueLabel, accent);
    }

    public static JPanel contentPanel() {
        return util.Theme.contentPanel();
    }

    public static JButton sidebarButton(String icon, String label) {
        return util.Theme.sidebarButton(icon, label);
    }

    public static void setNavActive(JButton btn, boolean act) {
        util.Theme.setNavActive(btn, act);
    }

    public static void styleButton(JButton btn, Color bg) {
        util.Theme.styleButton(btn, bg);
    }

    public static void styleTable(JTable table) {
        util.Theme.styleTable(table);
    }
}