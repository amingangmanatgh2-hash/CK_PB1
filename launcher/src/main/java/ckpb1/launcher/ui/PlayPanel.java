package ckpb1.launcher.ui;

import ckpb1.common.CKPB1;
import ckpb1.common.release.ReleaseInfo;
import ckpb1.launcher.core.JavaLocator;
import ckpb1.launcher.core.LauncherSettings;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Play tab: Minecraft version, Java, RAM, game directory, Launch, console
 * output and the client-mod updater.
 */
public final class PlayPanel extends JPanel {

    private final App app;
    private final MainFrame frame;
    private final JComboBox<String> versionCombo = new JComboBox<>();
    private final JComboBox<String> javaCombo = new JComboBox<>();
    private final JTextField userField = new JTextField(14);
    private final JTextField dirField = new JTextField(22);
    private final JSlider ramSlider = new JSlider(1024, Math.max(2048, maxRamMb()), 2048);
    private final JLabel ramLabel = new JLabel();
    private final JButton launchButton = Ui.button("▶  اجرای بازی (Launch)");
    private final JTextArea console = new JTextArea();

    public PlayPanel(App app, MainFrame frame) {
        this.app = app;
        this.frame = frame;
        setLayout(new BorderLayout(12, 12));
        setBackground(Ui.BG);
        setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Ui.BG_PANEL);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x232B3D)),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 8, 6, 8);
        g.anchor = GridBagConstraints.LINE_START;
        g.fill = GridBagConstraints.HORIZONTAL;

        LauncherSettings.Profile profile = app.settings.active();

        // version
        g.gridx = 0;
        g.gridy = 0;
        form.add(label("نسخه Minecraft (Version):"), g);
        versionCombo.setPreferredSize(new Dimension(220, 28));
        versionCombo.addItem(CKPB1.MINECRAFT_VERSION + "  (CK_PB1)");
        versionCombo.setSelectedIndex(0);
        g.gridx = 1;
        form.add(versionCombo, g);

        // username
        g.gridx = 0;
        g.gridy = 1;
        form.add(label("نام کاربری (Username):"), g);
        userField.setText(profile.username);
        userField.addFocusListener(saveOnChange());
        g.gridx = 1;
        form.add(userField, g);

        // java
        g.gridx = 0;
        g.gridy = 2;
        form.add(label("نسخه Java (>=17):"), g);
        JPanel javaRow = new JPanel(new BorderLayout(6, 0));
        javaRow.setOpaque(false);
        javaCombo.setPreferredSize(new Dimension(300, 28));
        javaCombo.addItem("در حال جستجوی Java ...");
        javaRow.add(javaCombo, BorderLayout.CENTER);
        JButton refreshJava = Ui.button("جستجوی مجدد");
        refreshJava.addActionListener(e -> locateJavas());
        javaRow.add(refreshJava, BorderLayout.LINE_END);
        g.gridx = 1;
        form.add(javaRow, g);

        // ram
        g.gridx = 0;
        g.gridy = 3;
        form.add(label("مقدار RAM:"), g);
        ramSlider.setOpaque(false);
        ramSlider.setMajorTickSpacing(1024);
        ramSlider.setPaintTicks(true);
        ramSlider.setPaintLabels(false);
        ramSlider.setValue(clampRam(profile.ramMB));
        updateRamLabel();
        ramSlider.addChangeListener(e -> {
            int value = ramSlider.getValue();
            ramSlider.setValue(value - value % 512);
            updateRamLabel();
        });
        JPanel ramRow = new JPanel(new BorderLayout(8, 0));
        ramRow.setOpaque(false);
        ramRow.add(ramSlider, BorderLayout.CENTER);
        ramLabel.setForeground(Ui.TEXT);
        ramRow.add(ramLabel, BorderLayout.LINE_END);
        g.gridx = 1;
        form.add(ramRow, g);

        // game dir
        g.gridx = 0;
        g.gridy = 4;
        form.add(label("پوشه بازی (Game Directory):"), g);
        JPanel dirRow = new JPanel(new BorderLayout(6, 0));
        dirRow.setOpaque(false);
        if (profile.gameDir == null || profile.gameDir.isBlank()) {
            profile.gameDir = defaultGameDir().toString();
        }
        dirField.setText(profile.gameDir);
        dirField.addFocusListener(saveOnChange());
        dirRow.add(dirField, BorderLayout.CENTER);
        JButton browse = Ui.button("انتخاب...");
        browse.addActionListener(e -> {
            var chooser = new javax.swing.JFileChooser(dirField.getText());
            chooser.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);
            chooser.setDialogTitle("انتخاب پوشه بازی");
            if (chooser.showOpenDialog(this) == javax.swing.JFileChooser.APPROVE_OPTION) {
                dirField.setText(chooser.getSelectedFile().toString());
                saveProfile();
            }
        });
        dirRow.add(browse, BorderLayout.LINE_END);
        g.gridx = 1;
        form.add(dirRow, g);

        // launch + client update buttons
        g.gridx = 0;
        g.gridy = 5;
        g.gridwidth = 2;
        g.fill = GridBagConstraints.NONE;
        JPanel buttons = new JPanel();
        buttons.setOpaque(false);
        launchButton.setFont(launchButton.getFont().deriveFont(Font.BOLD, 15f));
        launchButton.addActionListener(e -> launch());
        JButton updateClient = Ui.button("بروزرسانی کلاینت (CK_PB1)");
        updateClient.addActionListener(e -> installClientFromRelease(true));
        buttons.add(launchButton);
        buttons.add(updateClient);
        form.add(buttons, g);

        add(form, BorderLayout.NORTH);

        // console
        JPanel consolePanel = new JPanel(new BorderLayout(6, 6));
        consolePanel.setBackground(Ui.BG_PANEL);
        consolePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x232B3D)),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        JLabel consoleTitle = new JLabel("کنسول بازی (Console)");
        consoleTitle.setForeground(Ui.TEXT_DIM);
        consolePanel.add(consoleTitle, BorderLayout.NORTH);
        console.setEditable(false);
        console.setBackground(Ui.BG_FIELD);
        console.setForeground(new Color(0xB8D8A8));
        console.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        console.setComponentOrientation(java.awt.ComponentOrientation.LEFT_TO_RIGHT);
        consolePanel.add(new javax.swing.JScrollPane(console), BorderLayout.CENTER);
        add(consolePanel, BorderLayout.CENTER);

        loadVersions();
        locateJavas();
    }

    private static int maxRamMb() {
        long max = Runtime.getRuntime().maxMemory();
        long sys = ((com.sun.management.OperatingSystemMXBean)
                java.lang.management.ManagementFactory.getOperatingSystemMXBean()).getTotalMemorySize();
        long half = sys / 2 / (1024 * 1024);
        int cap = (int) Math.min(16384, Math.max(2048, half));
        return cap;
    }

    private static int clampRam(int mb) {
        return Math.min(maxRamMb(), Math.max(1024, mb - mb % 512));
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(Ui.TEXT_DIM);
        return l;
    }

    private FocusAdapter saveOnChange() {
        return new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                saveProfile();
            }
        };
    }

    private void saveProfile() {
        LauncherSettings.Profile p = app.settings.active();
        p.username = userField.getText();
        p.gameDir = dirField.getText();
        p.ramMB = ramSlider.getValue();
        app.save();
    }

    private void updateRamLabel() {
        ramLabel.setText(ramSlider.getValue() / 1024 + " GB  (" + ramSlider.getValue() + " MB)");
    }

    private static Path defaultGameDir() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (appData != null) {
                return Path.of(appData, ".minecraft");
            }
        }
        return Path.of(System.getProperty("user.home"), ".minecraft");
    }

    // ------------------------------------------------------------- versions

    private void loadVersions() {
        new SwingWorker<List<String[]>, Void>() {
            @Override
            protected List<String[]> doInBackground() {
                try {
                    return app.mc.fetchVersions();
                } catch (Exception e) {
                    return List.of();
                }
            }

            @Override
            protected void done() {
                try {
                    List<String[]> versions = get();
                    DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
                    model.addElement(CKPB1.MINECRAFT_VERSION + "  (CK_PB1)");
                    for (String[] v : versions) {
                        if (v[0].equals(CKPB1.MINECRAFT_VERSION)) {
                            continue; // already first with badge
                        }
                        if (v[1].equals("release")) {
                            model.addElement(v[0]);
                        }
                    }
                    versionCombo.setModel(model);
                } catch (Exception ignored) {
                }
            }
        }.execute();
    }

    // ----------------------------------------------------------------- java

    private void locateJavas() {
        javaCombo.removeAllItems();
        javaCombo.addItem("در حال جستجوی Java ...");
        new SwingWorker<List<JavaLocator.JavaInstall>, Void>() {
            @Override
            protected List<JavaLocator.JavaInstall> doInBackground() {
                return JavaLocator.findJavas();
            }

            @Override
            protected void done() {
                try {
                    List<JavaLocator.JavaInstall> found = get();
                    DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
                    for (JavaLocator.JavaInstall install : found) {
                        model.addElement(install.toString());
                    }
                    if (model.getSize() == 0) {
                        model.addElement("Java پیدا نشد - Java 17 نصب کنید");
                    }
                    javaCombo.setModel(model);
                    LauncherSettings.Profile p = app.settings.active();
                    if (p.javaPath != null && !p.javaPath.isBlank()) {
                        for (int i = 0; i < model.getSize(); i++) {
                            if (model.getElementAt(i).contains(p.javaPath)) {
                                javaCombo.setSelectedIndex(i);
                                break;
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }.execute();
    }

    private String selectedJavaBinary() {
        Object selected = javaCombo.getSelectedItem();
        if (selected == null) {
            return null;
        }
        String s = selected.toString();
        // entries look like "Java 17.0.9  -  C:\...javaw.exe"
        int idx = s.lastIndexOf("  -  ");
        String path = idx > 0 ? s.substring(idx + 5) : s;
        return "Java پیدا نشد".equals(s) ? null : path;
    }

    // --------------------------------------------------------------- launch

    private void log(String line) {
        SwingUtilities.invokeLater(() -> {
            console.append(line + "\n");
            console.setCaretPosition(console.getDocument().getLength());
        });
    }

    private void launch() {
        saveProfile();
        String javaBinary = selectedJavaBinary();
        if (javaBinary == null) {
            frame.setStatus("یک نسخه Java معتبر (17+) انتخاب کنید");
            return;
        }
        LauncherSettings.Profile profile = app.settings.active();
        String versionId = versionCombo.getSelectedItem() == null
                ? CKPB1.MINECRAFT_VERSION
                : versionCombo.getSelectedItem().toString().replaceAll("\\s*\\(.*\\)", "");
        boolean supported = versionId.equals(CKPB1.MINECRAFT_VERSION);

        launchButton.setEnabled(false);
        console.setText("");
        log("== CK_PB1 Launcher " + CKPB1.VERSION + " ==");
        log("Java: " + javaBinary);
        log("Minecraft: " + versionId + (supported ? " (پشتیبانی CK_PB1)" : " (کلاینت CK_PB1 فقط برای "
                + CKPB1.MINECRAFT_VERSION + " است)"));

        new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() {
                try {
                    Path gameDir = Path.of(profile.gameDir);
                    publish("آماده‌سازی پوشه بازی: " + gameDir);
                    Files.createDirectories(gameDir);

                    publish("بررسی نصب Minecraft " + versionId + " ...");
                    app.mc.install(versionId, gameDir, step -> publish(step));

                    ckpb1.launcher.mc.FabricInstall.Profile fabricProfile;
                    publish("نصب Fabric Loader ...");
                    fabricProfile = app.fabric.install(versionId, profile.gameDir, app.downloads);

                    if (supported) {
                        publish("نصب کلاینت CK_PB1 ...");
                        installClient(gameDir);
                    } else {
                        publish("کلاینت CK_PB1 فقط برای " + CKPB1.MINECRAFT_VERSION + " نصب می‌شود.");
                    }

                    publish("اجرا کردن بازی ...");
                    Process process = app.game.launch(profile, gameDir, javaBinary, fabricProfile);
                    app.game.pumpConsole(process, this::publish);
                    publish("بازی اجرا شد! (Minecraft " + versionId + " + CK_PB1)");
                } catch (Exception e) {
                    publish("خطا: " + e.getMessage());
                }
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String line : chunks) {
                    log(line);
                    frame.setStatus(line);
                }
            }

            @Override
            protected void done() {
                launchButton.setEnabled(true);
                if (!app.settings.keepConsoleOpen) {
                    frame.setStatus("");
                }
            }
        }.execute();
    }

    /** Finds the CK_PB1 client jar: bundled next to the launcher, or GitHub release. */
    private void installClient(Path gameDir) throws Exception {
        Path bundled = findBundledClientJar();
        if (bundled != null) {
            ckpb1.launcher.mc.GameLauncher.installClientMod(gameDir, bundled);
            log("کلاینت از فایل محلی نصب شد: " + bundled.getFileName());
            return;
        }
        // fall back to the GitHub release asset
        installClientFromRelease(false);
    }

    private Path findBundledClientJar() {
        try {
            Path launcherDir = Path.of(getClass().getProtectionDomain().getCodeSource().getLocation().toURI())
                    .getParent();
            try (Stream<Path> stream = Files.list(launcherDir)) {
                return stream.filter(p -> p.getFileName().toString().startsWith("CK_PB1-Client")
                                && p.toString().endsWith(".jar"))
                        .findFirst().orElse(null);
            }
        } catch (Exception e) {
            return null;
        }
    }

    /** Downloads the latest client mod from GitHub Releases into mods/. */
    private void installClientFromRelease(boolean manual) {
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                try {
                    var api = new ckpb1.common.release.GitHubReleases(
                            "https://api.github.com/repos/" + app.settings.githubRepo);
                    ReleaseInfo latest = api.latest();
                    if (latest == null) {
                        return "هیچ Release‌ای یافت نشد (کلاینت باید به‌صورت محلی نصب شود)";
                    }
                    var asset = latest.asset("Client");
                    if (asset == null) {
                        return "فایل کلاینت در Release پیدا نشد";
                    }
                    Path gameDir = Path.of(app.settings.active().gameDir);
                    Path mods = gameDir.resolve("mods");
                    Files.createDirectories(mods);
                    // remove old versions
                    try (Stream<Path> existing = Files.list(mods)) {
                        for (Path p : existing.filter(f -> f.getFileName().toString().startsWith("CK_PB1-Client")).toList()) {
                            Files.deleteIfExists(p);
                        }
                    }
                    Path target = mods.resolve(asset.name);
                    app.downloads.enqueue(asset.name, asset.downloadUrl, target, null, asset.size);
                    return "کلاینت " + latest.version() + " در حال دانلود است (تب دانلودها)";
                } catch (Exception e) {
                    return "دانلود کلاینت ناموفق: " + e.getMessage();
                }
            }

            @Override
            protected void done() {
                try {
                    String result = get();
                    frame.setStatus(result);
                    log(result);
                    if (manual) {
                        frame.showTab("downloads");
                    }
                } catch (Exception ignored) {
                }
            }
        }.execute();
    }
}
