package ckpb1.launcher.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/** Settings tab: update behavior, console, download threads, repository. */
public final class SettingsPanel extends JPanel {

    private final App app;
    private final JCheckBox updatesCheck = new JCheckBox("هنگام اجرا بررسی آپدیت از GitHub");
    private final JCheckBox consoleCheck = new JCheckBox("کنسول بازی باز بماند");
    private final JTextField threadsField = new JTextField(6);
    private final JTextField repoField = new JTextField(20);

    public SettingsPanel(App app) {
        this.app = app;
        setLayout(new BorderLayout(12, 12));
        setBackground(Ui.BG);
        setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Ui.BG_PANEL);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x232B3D)),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(8, 8, 8, 8);
        g.anchor = GridBagConstraints.LINE_START;
        g.fill = GridBagConstraints.HORIZONTAL;

        updatesCheck.setSelected(app.settings.checkUpdatesOnStart);
        updatesCheck.setForeground(Ui.TEXT);
        updatesCheck.setOpaque(false);
        consoleCheck.setSelected(app.settings.keepConsoleOpen);
        consoleCheck.setForeground(Ui.TEXT);
        consoleCheck.setOpaque(false);

        g.gridx = 0;
        g.gridy = 0;
        g.gridwidth = 2;
        form.add(updatesCheck, g);
        g.gridy = 1;
        form.add(consoleCheck, g);

        g.gridwidth = 1;
        g.gridy = 2;
        g.gridx = 0;
        JLabel t = new JLabel("تعداد دانلود همزمان:");
        t.setForeground(Ui.TEXT_DIM);
        form.add(t, g);
        g.gridx = 1;
        threadsField.setText(String.valueOf(app.settings.downloadThreads));
        form.add(threadsField, g);

        g.gridy = 3;
        g.gridx = 0;
        JLabel r = new JLabel("مخزن GitHub:");
        r.setForeground(Ui.TEXT_DIM);
        form.add(r, g);
        g.gridx = 1;
        repoField.setText(app.settings.githubRepo);
        form.add(repoField, g);

        JButton save = Ui.button("ذخیره تنظیمات");
        save.addActionListener(e -> save());
        JButton checkNow = Ui.button("بررسی آپدیت");
        checkNow.addActionListener(e -> {
            app.settings.githubRepo = repoField.getText().trim();
            save();
            findFrame().checkForUpdates(true);
        });
        JPanel buttons = new JPanel();
        buttons.setOpaque(false);
        buttons.add(save);
        buttons.add(checkNow);

        add(form, BorderLayout.NORTH);
        add(buttons, BorderLayout.CENTER);
    }

    private MainFrame findFrame() {
        return (MainFrame) javax.swing.SwingUtilities.getAncestorOfClass(MainFrame.class, this);
    }

    private void save() {
        app.settings.checkUpdatesOnStart = updatesCheck.isSelected();
        app.settings.keepConsoleOpen = consoleCheck.isSelected();
        app.settings.githubRepo = repoField.getText().trim();
        try {
            app.settings.downloadThreads = Math.max(1, Math.min(16,
                    Integer.parseInt(threadsField.getText().trim())));
        } catch (NumberFormatException ignored) {
        }
        app.save();
        if (findFrame() != null) {
            findFrame().setStatus("تنظیمات ذخیره شد");
        }
    }
}
