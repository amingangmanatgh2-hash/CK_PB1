package ckpb1.launcher;

import ckpb1.common.CKPB1;
import ckpb1.launcher.selftest.SelfTest;
import ckpb1.launcher.ui.App;
import ckpb1.launcher.ui.MainFrame;
import ckpb1.launcher.ui.Ui;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * CK_PB1 Launcher entrypoint.
 *
 * <p>Flags: {@code --selftest} runs a headless service check (used by CI),
 * {@code --version} prints the version.</p>
 */
public final class LauncherMain {

    public static void main(String[] args) {
        for (String arg : args) {
            if (arg.equals("--version") || arg.equals("-v")) {
                System.out.println(CKPB1.NAME + " " + CKPB1.VERSION);
                return;
            }
            if (arg.equals("--selftest")) {
                System.exit(SelfTest.run());
            }
        }
        System.setProperty("sun.java2d.uiScale", "1");
        SwingUtilities.invokeLater(() -> {
            Ui.install();
            App app = new App();
            MainFrame frame = new MainFrame(app);
            frame.setVisible(true);
            if (app.settings.checkUpdatesOnStart) {
                frame.checkForUpdates(true);
            }
        });
    }

    private LauncherMain() {
    }
}
