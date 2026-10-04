package ckpb1.launcher.ui;

import ckpb1.common.CKPB1;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.GridLayout;

/** About tab: CK_PB1 branding, version info and legal notice. */
public final class AboutPanel extends JPanel {

    public AboutPanel() {
        setLayout(new BorderLayout(12, 12));
        setBackground(Ui.BG);
        setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        JPanel box = new JPanel(new GridLayout(0, 1, 4, 4));
        box.setBackground(Ui.BG_PANEL);
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x232B3D)),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)));

        JLabel name = new JLabel("CK_PB1");
        name.setFont(name.getFont().deriveFont(Font.BOLD, 30f));
        name.setForeground(Ui.ACCENT);
        box.add(name);
        box.add(dim("کلاینت اختصاصی Minecraft Java - نسخه " + CKPB1.VERSION));
        box.add(dim("ساخته‌شده برای Singleplayer و سرورهای خصوصی/تستی"));
        box.add(dim(" "));
        box.add(dim("Minecraft نسخه هدف: " + CKPB1.MINECRAFT_VERSION + " (Fabric)"));
        box.add(dim("Launcher: CK_PB1 Launcher " + CKPB1.VERSION));
        box.add(dim("Installer: CK_PB1_Setup.exe"));
        box.add(dim(" "));
        box.add(dim("مخزن و Releaseها:"));
        JButton repo = Ui.button("github.com/" + CKPB1.GITHUB_REPO);
        repo.addActionListener(e -> {
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(java.net.URI.create(
                            "https://github.com/" + CKPB1.GITHUB_REPO));
                }
            } catch (Exception ignored) {
            }
        });
        JPanel repoRow = new JPanel(new BorderLayout());
        repoRow.setOpaque(false);
        repoRow.add(repo, BorderLayout.LINE_START);
        box.add(repoRow);
        box.add(dim(" "));
        box.add(dim("قابلیت‌های PvP و Automation فقط برای محیط‌های خصوصی و تستی پیاده‌سازی شده‌اند؛"));
        box.add(dim("استفاده در سرورهای عمومی ممکن است قوانین آن‌ها را نقض کند."));
        box.add(dim(" "));
        box.add(dim("CK_PB1 وابسته به Mojang/Microsoft نیست. Minecraft باید به‌صورت قانونی نصب شده باشد؛"));
        box.add(dim("هیچ فایل Minecraft در این Launcher بازتوزیع نمی‌شود (همه‌چیز در زمان اجرا از سرورهای رسمی دانلود می‌شود)."));

        add(box, BorderLayout.NORTH);
    }

    private JLabel dim(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(Ui.TEXT_DIM);
        return l;
    }
}
