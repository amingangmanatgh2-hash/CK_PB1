package ckpb1.launcher.ui;

import ckpb1.launcher.core.DownloadManager;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** Downloads tab: live download manager table with cancel/retry. */
public final class DownloadsPanel extends JPanel {

    private final App app;
    private final JTable table;

    public DownloadsPanel(App app) {
        this.app = app;
        this.table = new JTable(app.downloads.model());
        setLayout(new BorderLayout(12, 12));
        setBackground(Ui.BG);
        setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        table.setRowHeight(26);
        table.setForeground(Ui.TEXT);
        table.setBackground(Ui.BG_FIELD);
        table.setGridColor(new Color(0x232B3D));
        table.getColumnModel().getColumn(1).setCellRenderer(new ProgressRenderer());
        table.getColumnModel().getColumn(1).setPreferredWidth(160);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    act("retry");
                }
            }
        });

        JPanel buttons = new JPanel();
        buttons.setOpaque(false);
        JButton retry = Ui.button("تلاش مجدد (Retry)");
        retry.addActionListener(e -> act("retry"));
        JButton cancel = Ui.button("لغو (Cancel)");
        cancel.addActionListener(e -> act("cancel"));
        JButton clear = Ui.button("پاک‌کردن لیست");
        clear.addActionListener(e -> {
            for (int i = app.downloads.model().tasks.size() - 1; i >= 0; i--) {
                DownloadManager.Task t = app.downloads.model().tasks.get(i);
                if (t.status == DownloadManager.Status.DONE
                        || t.status == DownloadManager.Status.FAILED
                        || t.status == DownloadManager.Status.CANCELLED) {
                    app.downloads.model().tasks.remove(i);
                }
            }
            app.downloads.model().fireTableDataChanged();
        });
        buttons.add(retry);
        buttons.add(cancel);
        buttons.add(clear);

        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
    }

    private void act(String action) {
        int row = table.getSelectedRow();
        if (row < 0 || row >= app.downloads.model().tasks.size()) {
            return;
        }
        DownloadManager.Task task = app.downloads.model().tasks.get(row);
        if (action.equals("retry")) {
            app.downloads.retry(task);
        } else {
            app.downloads.cancel(task);
        }
    }

    private static final class ProgressRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean sel, boolean foc,
                                                       int row, int col) {
            int percent = value instanceof Integer i ? i : 0;
            String text = percent + "%";
            JLabelBar bar = new JLabelBar(percent, text);
            return bar;
        }
    }

    private static final class JLabelBar extends JPanel {
        private final int percent;

        JLabelBar(int percent, String text) {
            this.percent = percent;
            setLayout(new BorderLayout());
            setOpaque(true);
            setBackground(Ui.BG_FIELD);
            javax.swing.JLabel label = new javax.swing.JLabel(text);
            label.setForeground(Ui.TEXT);
            label.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
            add(label, BorderLayout.CENTER);
        }

        @Override
        protected void paintComponent(java.awt.Graphics g) {
            super.paintComponent(g);
            g.setColor(new Color(0x1F6FA8));
            int w = (int) (getWidth() * (percent / 100.0));
            g.fillRect(0, getHeight() - 4, w, 4);
        }
    }
}
