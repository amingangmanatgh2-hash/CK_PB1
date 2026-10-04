package ckpb1.launcher.ui;

import ckpb1.common.CKPB1;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * CK_PB1 Launcher main window: sidebar navigation (Play / Profiles /
 * Changelog / Downloads / Settings / About), update banner and status bar.
 */
public final class MainFrame extends JFrame {

    private final App app;
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final JLabel statusLabel = new JLabel(" ");
    private final JPanel bannerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6));
    private final JLabel currentVersionLabel = new JLabel();
    private final JLabel latestVersionLabel = new JLabel();

    public MainFrame(App app) {
        super(CKPB1.NAME + " Launcher");
        this.app = app;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1000, 680);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
        applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        // ----- top header -----------------------------------------------------
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Ui.BG);
        header.setPreferredSize(new Dimension(0, 92));
        header.setBorder(BorderFactory.createEmptyBorder(10, 16, 6, 16));

        JPanel titleBox = new JPanel();
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.setOpaque(false);
        JLabel title = new JLabel("CK_PB1 Launcher");
        title.setForeground(Ui.ACCENT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        currentVersionLabel.setForeground(Ui.TEXT_DIM);
        latestVersionLabel.setForeground(Ui.TEXT_DIM);
        titleBox.add(title);
        titleBox.add(currentVersionLabel);
        titleBox.add(latestVersionLabel);
        header.add(titleBox, BorderLayout.LINE_START);

        bannerPanel.setBackground(Ui.BG);
        bannerPanel.setVisible(false);
        header.add(bannerPanel, BorderLayout.LINE_END);

        // ----- sidebar ---------------------------------------------------------
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(0x0C1018));
        sidebar.setPreferredSize(new Dimension(190, 0));
        sidebar.setBorder(BorderFactory.createEmptyBorder(14, 10, 14, 10));
        String[][] items = {
                {"▶  بازی  (Play)", "play"},
                {"👤  پروفایل‌ها", "profiles"},
                {"📜  تغییرات (Changelog)", "changelog"},
                {"⬇  دانلودها", "downloads"},
                {"⚙  تنظیمات", "settings"},
                {"ℹ  درباره (About)", "about"},
        };
        for (String[] item : items) {
            JButton b = Ui.button(item[0]);
            b.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
            b.addActionListener(e -> {
                cards.show(content, item[1]);
                setStatus("");
            });
            sidebar.add(b);
            sidebar.add(Box.createVerticalStrut(8));
        }

        // ----- content ---------------------------------------------------------
        content.setBackground(Ui.BG);
        content.add(new PlayPanel(app, this), "play");
        content.add(new ProfilesPanel(app), "profiles");
        content.add(new ChangelogPanel(app), "changelog");
        content.add(new DownloadsPanel(app), "downloads");
        content.add(new SettingsPanel(app), "settings");
        content.add(new AboutPanel(), "about");

        // ----- status bar ------------------------------------------------------
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(new Color(0x0C1018));
        statusBar.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        statusLabel.setForeground(Ui.TEXT_DIM);
        statusBar.add(statusLabel, BorderLayout.LINE_START);

        setLayout(new BorderLayout());
        add(header, BorderLayout.NORTH);
        add(sidebar, BorderLayout.LINE_START);
        add(content, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                app.save();
                System.exit(0);
            }
        });

        refreshVersionLabels(null);
        checkForUpdates(false);
        cards.show(content, "play");
    }

    public void setStatus(String text) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(text == null || text.isBlank() ? " " : text));
    }

    void refreshVersionLabels(ckpb1.launcher.core.UpdateChecker.Result result) {
        currentVersionLabel.setText("نسخه فعلی: " + CKPB1.VERSION
                + "  |  Minecraft " + CKPB1.MINECRAFT_VERSION);
        if (result == null) {
            latestVersionLabel.setText("آخرین نسخه: ...");
        } else if (result.error != null) {
            latestVersionLabel.setText("آخرین نسخه: نامشخص (" + result.error + ")");
        } else if (result.latestVersion == null || result.latestVersion.isBlank()) {
            latestVersionLabel.setText("آخرین نسخه: هنوز Release‌ای منتشر نشده");
        } else {
            latestVersionLabel.setText("آخرین نسخه: " + result.latestVersion
                    + (result.updateAvailable ? "  ( جدیدتر )" : "  ( به‌روز هستید )"));
        }
    }

    /** Runs the GitHub update check (async) and shows the banner when needed. */
    public void checkForUpdates(boolean manual) {
        refreshVersionLabels(null);
        new SwingWorker<ckpb1.launcher.core.UpdateChecker.Result, Void>() {
            @Override
            protected ckpb1.launcher.core.UpdateChecker.Result doInBackground() {
                return ckpb1.launcher.core.UpdateChecker.check(app.settings.githubRepo);
            }

            @Override
            protected void done() {
                try {
                    ckpb1.launcher.core.UpdateChecker.Result result = get();
                    app.update = result;
                    refreshVersionLabels(result);
                    if (result.updateAvailable) {
                        showUpdateBanner(result);
                        if (manual) {
                            setStatus("آپدیت جدید در دسترس است: v" + result.latestVersion);
                        }
                    } else if (manual) {
                        setStatus(result.error != null
                                ? "بررسی آپدیت ناموفق: " + result.error
                                : "شما آخرین نسخه را دارید (v" + result.currentVersion + ")");
                    }
                } catch (Exception e) {
                    refreshVersionLabels(null);
                }
            }
        }.execute();
    }

    private void showUpdateBanner(ckpb1.launcher.core.UpdateChecker.Result result) {
        bannerPanel.removeAll();
        bannerPanel.setBackground(new Color(0x2A2410));
        JLabel label = new JLabel("آپدیت جدید در دسترس است  (v" + result.latestVersion + ")");
        label.setForeground(Ui.WARN);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 14f));
        JButton download = Ui.button("دانلود و نصب آپدیت");
        download.addActionListener(e -> downloadUpdate(result));
        JButton later = Ui.button("بعداً");
        later.addActionListener(e -> bannerPanel.setVisible(false));
        bannerPanel.add(label);
        bannerPanel.add(download);
        bannerPanel.add(later);
        bannerPanel.setVisible(true);
        bannerPanel.revalidate();
        bannerPanel.repaint();
    }

    /** Downloads the new CK_PB1 Setup.exe from the GitHub release. */
    private void downloadUpdate(ckpb1.launcher.core.UpdateChecker.Result result) {
        var asset = result.latest.asset("Setup.exe");
        if (asset == null) {
            setStatus("فایل Setup در Release پیدا نشد");
            return;
        }
        try {
            Path downloadsDir = Path.of(System.getProperty("user.home"), "Downloads");
            if (!Files.isDirectory(downloadsDir)) {
                downloadsDir = Path.of(System.getProperty("user.home"));
            }
            Path target = downloadsDir.resolve(CKPB1.setupAssetName(result.latestVersion));
            setStatus("در حال دانلود " + target.getFileName() + " ... (پیشرفت در تب دانلودها)");
            app.downloads.enqueue(target.getFileName().toString(), asset.downloadUrl, target, null, asset.size);
            // switch to downloads tab
            cards.show(content, "downloads");
            JButton open = Ui.button("اجرای نصب‌کننده");
            open.addActionListener(ev -> {
                try {
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().open(target.toFile());
                    }
                } catch (Exception ex) {
                    setStatus("نصب‌کننده دانلود شد: " + target);
                }
            });
            bannerPanel.removeAll();
            bannerPanel.add(new JLabel("Setup در حال دانلود است - فایل: " + target) {{
                setForeground(Ui.TEXT);
            }});
            bannerPanel.add(open);
            bannerPanel.revalidate();
        } catch (Exception e) {
            setStatus("دانلود آپدیت ناموفق: " + e.getMessage());
        }
    }

    public void showTab(String name) {
        cards.show(content, name);
    }
}
