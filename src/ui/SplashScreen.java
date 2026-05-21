package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import util.Theme;

public class SplashScreen extends JWindow {
    private JProgressBar progress;
    private int pct = 0;
    private JLabel loadingLbl;
    private float fadeAlpha = 0f;

    // Design Colors from Image
    private static final Color BG_DARK = new Color(0x020817); // Deepest blue/black
    private static final Color ACCENT_BLUE = new Color(0x0EA5E9); // Tech blue
    private static final Color TEXT_WHITE = new Color(0xF8FAFC);
    private static final Color TEXT_GRAY = new Color(0x64748B);

    public SplashScreen() {
        setBackground(new Color(0, 0, 0, 0));
        JPanel root = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

                // Fade-in effect
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fadeAlpha));

                int w = getWidth(), h = getHeight();

                // 1. Dark Background
                g2.setColor(BG_DARK);
                g2.fillRect(0, 0, w, h);

                // 2. Techy Background Pattern (Circuit lines)
                drawTechPattern(g2, w, h);

                // 3. Corner Brackets
                drawBrackets(g2, w, h);

                g2.dispose();
            }
        };
        root.setOpaque(false);
        root.setLayout(new BorderLayout());
        root.setPreferredSize(new Dimension(800, 450)); // Wider like the image

        // --- Fade-in Timer ---
        Timer fadeTimer = new Timer(16, null);
        fadeTimer.addActionListener(e -> {
            fadeAlpha += 0.05f;
            if (fadeAlpha >= 1f) { fadeAlpha = 1f; ((Timer)e.getSource()).stop(); }
            root.repaint();
        });
        fadeTimer.start();

        // ── Central Content ──────────────────────────────────────────────
        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;

        // Central Shield Logo
        JPanel logoPanel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                int cx = w / 2, cy = h / 2;

                // Glowing background rings
                for (int i = 3; i >= 1; i--) {
                    float opacity = 0.05f * i;
                    g2.setColor(new Color(14, 165, 233, (int)(opacity * 255)));
                    g2.fillOval(cx - 60 - i*8, cy - 60 - i*8, 120 + i*16, 120 + i*16);
                }

                // Shield Shape
                Path2D shield = new Path2D.Float();
                shield.moveTo(cx, cy - 70);
                shield.lineTo(cx + 55, cy - 45);
                shield.lineTo(cx + 55, cy + 15);
                shield.quadTo(cx + 55, cy + 55, cx, cy + 75);
                shield.quadTo(cx - 55, cy + 55, cx - 55, cy + 15);
                shield.lineTo(cx - 55, cy - 45);
                shield.closePath();

                // Shield Fill (Dark Tech Blue)
                g2.setPaint(new GradientPaint(cx, cy - 70, new Color(0x0F172A), cx, cy + 75, BG_DARK));
                g2.fill(shield);

                // Shield Borders (Multi-layered for glow)
                g2.setColor(new Color(0x0EA5E9, true));
                g2.setStroke(new BasicStroke(0.5f));
                g2.draw(shield);
                
                // Cyan inner border
                g2.setColor(new Color(0x22D3EE));
                g2.setStroke(new BasicStroke(1.2f));
                Path2D innerShield = new Path2D.Float();
                innerShield.moveTo(cx, cy - 65);
                innerShield.lineTo(cx + 50, cy - 41);
                innerShield.lineTo(cx + 50, cy + 12);
                innerShield.quadTo(cx + 50, cy + 51, cx, cy + 70);
                innerShield.quadTo(cx - 50, cy + 51, cx - 50, cy + 12);
                innerShield.lineTo(cx - 50, cy - 41);
                innerShield.closePath();
                g2.draw(innerShield);

                // Large Letter P
                g2.setFont(new Font("Segoe UI", Font.BOLD, 90));
                g2.setColor(Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                String pStr = "P";
                g2.drawString(pStr, cx - fm.stringWidth(pStr)/2 - 4, cy + 32);

                // Lightning Bolt Overlay
                Path2D bolt = new Path2D.Float();
                bolt.moveTo(cx + 10, cy - 35);
                bolt.lineTo(cx - 5,  cy - 2);
                bolt.lineTo(cx + 10, cy - 2);
                bolt.lineTo(cx - 8,  cy + 40);
                bolt.lineTo(cx + 25, cy + 5);
                bolt.lineTo(cx + 10, cy + 5);
                bolt.closePath();

                g2.setColor(new Color(0xFACC15)); // Yellow bolt
                g2.fill(bolt);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(bolt);

                g2.dispose();
            }
        };
        logoPanel.setOpaque(false);
        logoPanel.setPreferredSize(new Dimension(200, 200));
        gbc.insets = new Insets(0, 0, 10, 0);
        center.add(logoPanel, gbc);

        // Branding Text
        gbc.gridy++;
        JLabel title1 = new JLabel("PARK");
        title1.setFont(new Font("Segoe UI Black", Font.BOLD, 52));
        title1.setForeground(ACCENT_BLUE);
        gbc.insets = new Insets(0, 0, -10, 0);
        center.add(title1, gbc);

        gbc.gridy++;
        JLabel title2 = new JLabel("NOVA");
        title2.setFont(new Font("Segoe UI Black", Font.BOLD, 42));
        title2.setForeground(Color.WHITE);
        gbc.insets = new Insets(-10, 0, 10, 0);
        center.add(title2, gbc);

        gbc.gridy++;
        JLabel subtitle = new JLabel("INTELLIGENT · EFFICIENT · ELECTRIC");
        subtitle.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 14));
        subtitle.setForeground(new Color(0x38BDF8));
        center.add(subtitle, gbc);

        root.add(center, BorderLayout.CENTER);

        // Progress and Version
        JPanel bottom = new JPanel(new GridBagLayout());
        bottom.setOpaque(false);
        bottom.setBorder(BorderFactory.createEmptyBorder(0, 50, 20, 50));
        GridBagConstraints bbc = new GridBagConstraints();
        bbc.gridx = 0; bbc.gridy = 0; bbc.fill = GridBagConstraints.HORIZONTAL; bbc.weightx = 1.0;

        loadingLbl = new JLabel("Loading modules...");
        loadingLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        loadingLbl.setForeground(TEXT_GRAY);
        loadingLbl.setHorizontalAlignment(SwingConstants.CENTER);
        bottom.add(loadingLbl, bbc);

        bbc.gridy++; bbc.insets = new Insets(10, 200, 15, 200);
        progress = new JProgressBar(0, 100) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0x1E293B));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                int barW = (int) (getWidth() * (getValue() / 100.0));
                if (barW > 0) {
                    g2.setPaint(new GradientPaint(0, 0, new Color(0x0EA5E9), barW, 0, new Color(0x22D3EE)));
                    g2.fillRoundRect(0, 0, barW, getHeight(), 4, 4);
                }
                g2.dispose();
            }
        };
        progress.setPreferredSize(new Dimension(0, 4));
        progress.setBorderPainted(false);
        progress.setOpaque(false);
        bottom.add(progress, bbc);

        bbc.gridy++; bbc.insets = new Insets(0,0,0,0);
        JLabel version = new JLabel("v 1.0");
        version.setFont(new Font("Segoe UI", Font.BOLD, 10));
        version.setForeground(TEXT_GRAY);
        version.setHorizontalAlignment(SwingConstants.CENTER);
        bottom.add(version, bbc);

        root.add(bottom, BorderLayout.SOUTH);

        add(root);
        pack();
        setLocationRelativeTo(null);
    }

    private void drawTechPattern(Graphics2D g2, int w, int h) {
        g2.setColor(new Color(14, 165, 233, 15)); // Very faint tech lines
        g2.setStroke(new BasicStroke(0.8f));
        
        // Random tech lines on left/right
        int[][] lines = {
            {50, 150, 100, 150, 100, 200},
            {w-50, 150, w-100, 150, w-100, 200},
            {80, 220, 120, 220, 120, 260},
            {w-80, 220, w-120, 220, w-120, 260}
        };

        for (int[] l : lines) {
            Path2D path = new Path2D.Float();
            path.moveTo(l[0], l[1]);
            path.lineTo(l[2], l[3]);
            path.lineTo(l[4], l[5]);
            g2.draw(path);
            g2.fillOval(l[0]-2, l[1]-2, 4, 4);
            g2.fillOval(l[4]-2, l[5]-2, 4, 4);
        }
    }

    private void drawBrackets(Graphics2D g2, int w, int h) {
        g2.setColor(new Color(14, 165, 233, 80));
        g2.setStroke(new BasicStroke(1.5f));
        int s = 30; // length
        int m = 20; // margin

        // Top Left
        g2.drawLine(m, m, m+s, m); g2.drawLine(m, m, m, m+s);
        // Top Right
        g2.drawLine(w-m, m, w-m-s, m); g2.drawLine(w-m, m, w-m, m+s);
        // Bottom Left
        g2.drawLine(m, h-m, m+s, h-m); g2.drawLine(m, h-m, m, h-m-s);
        // Bottom Right
        g2.drawLine(w-m, h-m, w-m-s, h-m); g2.drawLine(w-m, h-m, w-m, h-m-s);
    }

    public void showAndLoad() {
        setVisible(true);
        String[] msgs = { "Initializing...", "Loading Modules...", "Connecting DB...", "Finalizing UI...", "Welcome!" };
        
        Timer timer = new Timer(30, null);
        timer.addActionListener(e -> {
            pct++;
            progress.setValue(pct);
            if (pct % 20 == 0) loadingLbl.setText(msgs[Math.min(pct/20, msgs.length-1)]);
            if (pct >= 100) {
                timer.stop();
                try { Thread.sleep(500); } catch(Exception ex){}
                dispose();
                SwingUtilities.invokeLater(() -> new LoginScreen().setVisible(true));
            }
        });
        timer.start();
    }
}
