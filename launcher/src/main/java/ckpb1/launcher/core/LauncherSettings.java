package ckpb1.launcher.core;

import ckpb1.common.json.MiniJson;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Launcher settings + profiles, persisted as JSON. The settings file lives in
 * a portable CK_PB1 folder (APPDATA/CK_PB1 on Windows, ~/.ckpb1 elsewhere)
 * so it survives installer upgrades.
 */
public final class LauncherSettings {

    /** One launch profile (version + java + ram + directory + username). */
    public static final class Profile {
        public String name = "Default";
        public String mcVersion = ckpb1.common.CKPB1.MINECRAFT_VERSION;
        public String username = "Player";
        public String javaPath = "";
        public int ramMB = 2048;
        public String gameDir = "";
        public String jvmArgs = "";

        public Map<String, Object> toJson() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", name);
            m.put("mcVersion", mcVersion);
            m.put("username", username);
            m.put("javaPath", javaPath);
            m.put("ramMB", ramMB);
            m.put("gameDir", gameDir);
            m.put("jvmArgs", jvmArgs);
            return m;
        }

        public static Profile fromJson(Map<String, Object> m) {
            Profile p = new Profile();
            p.name = MiniJson.str(m.get("name"), p.name);
            p.mcVersion = MiniJson.str(m.get("mcVersion"), p.mcVersion);
            p.username = MiniJson.str(m.get("username"), p.username);
            p.javaPath = MiniJson.str(m.get("javaPath"), p.javaPath);
            p.ramMB = (int) MiniJson.lng(m.get("ramMB"), p.ramMB);
            p.gameDir = MiniJson.str(m.get("gameDir"), p.gameDir);
            p.jvmArgs = MiniJson.str(m.get("jvmArgs"), p.jvmArgs);
            return p;
        }
    }

    public final List<Profile> profiles = new ArrayList<>();
    public String activeProfile = "Default";
    public boolean checkUpdatesOnStart = true;
    public boolean keepConsoleOpen = true;
    public int downloadThreads = 4;
    public String githubRepo = ckpb1.common.CKPB1.GITHUB_REPO;

    private final Path file;

    public LauncherSettings() {
        this(defaultFile());
    }

    public LauncherSettings(Path file) {
        this.file = file;
        profiles.add(new Profile());
        load();
    }

    public static Path defaultFile() {
        String override = System.getProperty("ckpb1.home");
        Path base;
        if (override != null && !override.isBlank()) {
            base = Path.of(override);
        } else {
            String os = System.getProperty("os.name", "").toLowerCase();
            String appData = System.getenv("APPDATA");
            if (os.contains("win") && appData != null) {
                base = Path.of(appData).resolve("CK_PB1");
            } else {
                base = Path.of(System.getProperty("user.home"), ".ckpb1");
            }
        }
        return base.resolve("launcher-settings.json");
    }

    public Profile active() {
        for (Profile p : profiles) {
            if (p.name.equalsIgnoreCase(activeProfile)) {
                return p;
            }
        }
        return profiles.get(0);
    }

    public void load() {
        if (!Files.exists(file)) {
            return;
        }
        try {
            Map<String, Object> root = MiniJson.obj(MiniJson.parse(Files.readString(file, StandardCharsets.UTF_8)));
            activeProfile = MiniJson.str(root.get("activeProfile"), activeProfile);
            checkUpdatesOnStart = MiniJson.bool(root.get("checkUpdatesOnStart"), checkUpdatesOnStart);
            keepConsoleOpen = MiniJson.bool(root.get("keepConsoleOpen"), keepConsoleOpen);
            downloadThreads = (int) MiniJson.lng(root.get("downloadThreads"), downloadThreads);
            githubRepo = MiniJson.str(root.get("githubRepo"), githubRepo);
            profiles.clear();
            for (Object o : MiniJson.arr(root.get("profiles"))) {
                Profile p = Profile.fromJson(MiniJson.obj(o));
                if (!profiles.stream().anyMatch(x -> x.name.equalsIgnoreCase(p.name))) {
                    profiles.add(p);
                }
            }
            if (profiles.isEmpty()) {
                profiles.add(new Profile());
            }
        } catch (Exception e) {
            System.err.println("CK_PB1: could not load settings: " + e.getMessage());
        }
    }

    public void save() {
        try {
            Files.createDirectories(file.getParent());
            Map<String, Object> root = new LinkedHashMap<>();
            root.put("activeProfile", activeProfile);
            root.put("checkUpdatesOnStart", checkUpdatesOnStart);
            root.put("keepConsoleOpen", keepConsoleOpen);
            root.put("downloadThreads", downloadThreads);
            root.put("githubRepo", githubRepo);
            List<Object> list = new ArrayList<>();
            for (Profile p : profiles) {
                list.add(p.toJson());
            }
            root.put("profiles", list);
            Files.writeString(file, MiniJson.write(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("CK_PB1: could not save settings: " + e.getMessage());
        }
    }
}
