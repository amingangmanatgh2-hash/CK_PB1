package ckpb1.launcher.ui;

import ckpb1.launcher.core.LauncherSettings;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Profiles tab: manage multiple launch profiles (version, java, ram,
 * directory, username per profile).
 */
public final class ProfilesPanel extends JPanel {

    private final App app;
    private final DefaultListModel<String> model = new DefaultListModel<>();
    private final JList<String> list = new JList<>(model);
    private final JTextField nameField = new JTextField(14);
    private final JTextField userField = new JTextField(14);
    private final JTextField dirField = new JTextField(18);
    private final JTextField javaField = new JTextField(18);
    private final JTextField ramField = new JTextField(8);
    private final JLabel activeLabel = new JLabel();

    public ProfilesPanel(App app) {
        this.app = app;
        setLayout(new BorderLayout(12, 12));
        setBackground(Ui.BG);
        setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        list.setPreferredSize(new Dimension(200, 0));
        list.setBackground(Ui.BG_FIELD);
        list.setForeground(Ui.TEXT);
        list.setBorder(BorderFactory.createLineBorder(new Color(0x232B3D)));
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelected();
            }
        });

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Ui.BG_PANEL);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x232B3D)),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 8, 6, 8);
        g.anchor = GridBagConstraints.LINE_START;
        g.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        addRow(form, g, row++, "نام پروفایل:", nameField);
        addRow(form, g, row++, "نام کاربری:", userField);
        addRow(form, g, row++, "پوشه بازی:", dirField);
        addRow(form, g, row++, "مسیر Java:", javaField);
        addRow(form, g, row++, "RAM (MB):", ramField);
        g.gridx = 0;
        g.gridy = row;
        g.gridwidth = 2;
        activeLabel.setForeground(Ui.ACCENT);
        form.add(activeLabel, g);

        JPanel buttons = new JPanel();
        buttons.setOpaque(false);
        JButton save = Ui.button("ذخیره");
        save.addActionListener(e -> saveForm());
        JButton activate = Ui.button("فعال‌سازی");
        activate.addActionListener(e -> activateSelected());
        JButton create = Ui.button("پروفایل جدید");
        create.addActionListener(e -> createProfile());
        JButton delete = Ui.button("حذف");
        delete.addActionListener(e -> deleteSelected());
        buttons.add(save);
        buttons.add(activate);
        buttons.add(create);
        buttons.add(delete);

        add(list, BorderLayout.LINE_START);
        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        refreshList();
    }

    private void addRow(JPanel form, GridBagConstraints g, int row, String label, JTextField field) {
        g.gridx = 0;
        g.gridy = row;
        JLabel l = new JLabel(label);
        l.setForeground(Ui.TEXT_DIM);
        form.add(l, g);
        g.gridx = 1;
        form.add(field, g);
    }

    private void refreshList() {
        SwingUtilities.invokeLater(() -> {
            model.clear();
            for (LauncherSettings.Profile p : app.settings.profiles) {
                model.addElement(p.name + (p.name.equalsIgnoreCase(app.settings.activeProfile) ? "  ✔" : ""));
            }
            for (int i = 0; i < model.size(); i++) {
                if (model.get(i).startsWith(app.settings.activeProfile)) {
                    list.setSelectedIndex(i);
                    break;
                }
            }
            activeLabel.setText("پروفایل فعال: " + app.settings.activeProfile);
            loadSelected();
        });
    }

    private void loadSelected() {
        LauncherSettings.Profile p = profileFromSelection();
        if (p == null) {
            return;
        }
        nameField.setText(p.name);
        userField.setText(p.username);
        dirField.setText(p.gameDir);
        javaField.setText(p.javaPath);
        ramField.setText(String.valueOf(p.ramMB));
    }

    private LauncherSettings.Profile profileFromSelection() {
        String selected = list.getSelectedValue();
        if (selected == null) {
            return null;
        }
        String name = selected.replace("  ✔", "").trim();
        for (LauncherSettings.Profile p : app.settings.profiles) {
            if (p.name.equalsIgnoreCase(name)) {
                return p;
            }
        }
        return null;
    }

    private void saveForm() {
        LauncherSettings.Profile p = profileFromSelection();
        if (p == null) {
            return;
        }
        p.username = userField.getText();
        p.gameDir = dirField.getText();
        p.javaPath = javaField.getText();
        try {
            p.ramMB = Integer.parseInt(ramField.getText().trim());
        } catch (NumberFormatException e) {
            // keep old value
        }
        app.save();
        refreshList();
    }

    private void activateSelected() {
        LauncherSettings.Profile p = profileFromSelection();
        if (p != null) {
            app.settings.activeProfile = p.name;
            app.save();
            refreshList();
        }
    }

    private void createProfile() {
        String base = "Profile " + (app.settings.profiles.size() + 1);
        LauncherSettings.Profile p = new LauncherSettings.Profile();
        p.name = base;
        LauncherSettings.Profile current = app.settings.active();
        p.username = current.username;
        p.gameDir = current.gameDir;
        p.javaPath = current.javaPath;
        p.ramMB = current.ramMB;
        app.settings.profiles.add(p);
        app.save();
        refreshList();
    }

    private void deleteSelected() {
        LauncherSettings.Profile p = profileFromSelection();
        if (p == null) {
            return;
        }
        if (p.name.equalsIgnoreCase(app.settings.activeProfile) || app.settings.profiles.size() <= 1) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "پروفایل فعال یا تنها پروفایل قابل حذف نیست",
                    "CK_PB1", javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }
        app.settings.profiles.remove(p);
        app.save();
        refreshList();
    }
}
