package ckpb1.launcher.ui;

import ckpb1.common.release.ReleaseInfo;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.List;

/** Changelog tab: release history fetched from GitHub Releases. */
public final class ChangelogPanel extends JPanel {

    private final App app;
    private final DefaultListModel<String> model = new DefaultListModel<>();
    private final JList<String> list = new JList<>(model);
    private final JTextArea body = new JTextArea();

    private List<ReleaseInfo> releases = List.of();

    public ChangelogPanel(App app) {
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
                showSelected();
            }
        });

        body.setEditable(false);
        body.setBackground(Ui.BG_PANEL);
        body.setForeground(Ui.TEXT);
        body.setWrapStyleWord(true);
        body.setLineWrap(true);
        body.setComponentOrientation(java.awt.ComponentOrientation.LEFT_TO_RIGHT);
        body.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(list, BorderLayout.LINE_START);
        add(new JScrollPane(body), BorderLayout.CENTER);

        refresh();
    }

    private void refresh() {
        model.clear();
        model.addElement("در حال دریافت ...");
        new SwingWorker<List<ReleaseInfo>, Void>() {
            @Override
            protected List<ReleaseInfo> doInBackground() {
                try {
                    return new ckpb1.common.release.GitHubReleases(
                            "https://api.github.com/repos/" + app.settings.githubRepo).list();
                } catch (Exception e) {
                    return List.of();
                }
            }

            @Override
            protected void done() {
                try {
                    releases = get();
                } catch (Exception ignored) {
                }
                model.clear();
                if (releases.isEmpty()) {
                    model.addElement("Release‌ای یافت نشد");
                    body.setText("هنوز نسخه‌ای منتشر نشده است.\n\nپس از انتشار v1.0.0 در GitHub Releases، "
                            + "تغییرات هر نسخه اینجا نمایش داده می‌شود.");
                    return;
                }
                for (ReleaseInfo r : releases) {
                    model.addElement(r.tagName + (r.prerelease ? " (pre)" : ""));
                }
                list.setSelectedIndex(0);
            }
        }.execute();
    }

    private void showSelected() {
        int idx = list.getSelectedIndex();
        if (idx < 0 || idx >= releases.size()) {
            return;
        }
        ReleaseInfo r = releases.get(idx);
        StringBuilder sb = new StringBuilder();
        sb.append("CK_PB1 ").append(r.tagName).append("\n");
        sb.append("تاریخ انتشار: ").append(r.publishedAt).append("\n\n");
        sb.append(r.body == null || r.body.isBlank() ? "(بدون توضیحات)" : r.body).append("\n\n");
        sb.append("فایل‌ها:\n");
        for (ReleaseInfo.Asset a : r.assets) {
            sb.append("  - ").append(a.name).append("  (").append(DownloadManagerSize(a.size)).append(")\n");
        }
        body.setText(sb.toString());
        body.setCaretPosition(0);
    }

    private static String DownloadManagerSize(long bytes) {
        return ckpb1.launcher.core.DownloadManager.human(bytes);
    }
}
