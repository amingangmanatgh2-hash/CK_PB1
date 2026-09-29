package ckpb1.launcher.ui;

import ckpb1.launcher.core.DownloadManager;
import ckpb1.launcher.core.LauncherSettings;
import ckpb1.launcher.core.UpdateChecker;
import ckpb1.launcher.mc.FabricInstall;
import ckpb1.launcher.mc.GameLauncher;
import ckpb1.launcher.mc.MinecraftInstall;

/** Shared launcher services passed to the UI panels. */
public final class App {

    public final LauncherSettings settings;
    public final DownloadManager downloads;
    public final MinecraftInstall mc;
    public final FabricInstall fabric;
    public final GameLauncher game = new GameLauncher();
    public volatile UpdateChecker.Result update;

    public App() {
        this.settings = new LauncherSettings();
        this.downloads = new DownloadManager(settings.downloadThreads);
        this.mc = new MinecraftInstall(downloads);
        this.fabric = new FabricInstall();
    }

    public void save() {
        settings.save();
    }
}
