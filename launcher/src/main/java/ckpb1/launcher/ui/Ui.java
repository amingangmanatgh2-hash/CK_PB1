package ckpb1.launcher.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Enumeration;

/** CK_PB1 launcher dark theme + small Swing helpers (Persian-first UI). */
public final class Ui {

    // palette
    public static final Color BG = new Color(0x10141F);
    public static final Color BG_PANEL = new Color(0x161C2A);
    public static final Color BG_FIELD = new Color(0x0D1119);
    public static final Color ACCENT = new Color(0x40C8FF);
    public static final Color ACCENT_DARK = new Color(0x1F6FA8);
    public static final Color TEXT = new Color(0xE8EEF8);
    public static final Color TEXT_DIM = new Color(0x9AA8BC);
    public static final Color GOOD = new Color(0x55DD88);
    public static final Color WARN = new Color(0xFFC860);
    public static final Color BAD = new Color(0xFF6E6E);

    private Ui() {
    }

    public static void install() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // keep default L&F
        }
        UIManager.put("control", BG_PANEL);
        UIManager.put("nimbusBase", ACCENT_DARK);
        UIManager.put("nimbusBlueGrey", BG_PANEL);
        UIManager.put("nimbusFocus", ACCENT);
        UIManager.put("nimbusSelectionBackground", ACCENT_DARK);
        UIManager.put("Table.background", BG_FIELD);
        UIManager.put("Table.altRowColor", BG_PANEL);
        UIManager.put("Table.foreground", TEXT);
        UIManager.put("Table.gridColor", new Color(0x232B3D));
        UIManager.put("ScrollPane.background", BG);
        Font base = new Font("Segoe UI", Font.PLAIN, 13);
        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource) {
                UIManager.put(key, new FontUIResource(base));
            }
        }
    }

    public static JButton button(String text) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? ACCENT_DARK : BG_PANEL);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(ACCENT_DARK);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setContentAreaFilled(false);
        b.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        b.setForeground(TEXT);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        hover(b);
        return b;
    }

    public static void hover(Component c) {
        c.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                c.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                c.repaint();
            }
        });
    }

    public static javax.swing.JPanel panel(String title) {
        javax.swing.JPanel p = new javax.swing.JPanel();
        p.setBackground(BG_PANEL);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x232B3D)),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)));
        if (title != null) {
            p.setBorder(BorderFactory.createCompoundBorder(
                    p.getBorder(),
                    BorderFactory.createTitledBorder(
                            BorderFactory.createEmptyBorder(), title)));
        }
        return p;
    }
}
