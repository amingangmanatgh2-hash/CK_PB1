package ckpb1.launcher.mc;

import ckpb1.launcher.core.LauncherSettings;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Builds the classpath/arguments and starts the game process
 * (fabric loader KnotClient on top of vanilla Minecraft).
 */
public final class GameLauncher {

    public interface Console {
        void line(String text);
    }

    private Process process;

    public boolean isRunning() {
        return process != null && process.isAlive();
    }

    public void stop() {
        if (process != null && process.isAlive()) {
            process.destroy();
        }
    }

    /** Installs the CK_PB1 client mod into the mods folder (from a local file). */
    public static void installClientMod(Path gameDir, Path clientJar) throws IOException {
        Path mods = gameDir.resolve("mods");
        Files.createDirectories(mods);
        // remove older CK_PB1 client jars
        try (Stream<Path> existing = Files.list(mods)) {
            for (Path p : existing.filter(f -> f.getFileName().toString().startsWith("CK_PB1-Client")).toList()) {
                Files.deleteIfExists(p);
            }
        }
        Path target = mods.resolve(clientJar.getFileName().toString());
        if (!Files.exists(target)) {
            Files.copy(clientJar, target);
        }
    }

    /** Collects every jar under a libraries tree. */
    private static void collectJars(Path librariesDir, List<Path> out) throws IOException {
        if (!Files.isDirectory(librariesDir)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(librariesDir)) {
            stream.filter(p -> p.toString().endsWith(".jar")).forEach(out::add);
        }
    }

    /** Launches the game. Returns the started process. */
    public Process launch(LauncherSettings.Profile profile, Path gameDir, String javaBinary,
                          FabricInstall.Profile fabricProfile) throws Exception {
        if (isRunning()) {
            throw new IllegalStateException("A game instance is already running from this launcher");
        }
        Path versionsDir = gameDir.resolve("versions").resolve(profile.mcVersion);
        Path clientJar = versionsDir.resolve(profile.mcVersion + ".jar");

        List<Path> classpath = new ArrayList<>();
        collectJars(gameDir.resolve("libraries"), classpath);
        classpath.add(clientJar);

        // offline-style session values (private/test use)
        String username = profile.username == null || profile.username.isBlank()
                ? "Player" : profile.username.trim();
        String uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8))
                .toString().replace("-", "");

        // asset index id from the version json
        String assetIndex = "5"; // 1.20.1
        Path versionJson = versionsDir.resolve(profile.mcVersion + ".json");
        if (Files.exists(versionJson)) {
            var meta = ckpb1.common.json.MiniJson.obj(ckpb1.common.json.MiniJson.parse(
                    Files.readString(versionJson, StandardCharsets.UTF_8)));
            assetIndex = ckpb1.common.json.MiniJson.str(
                    ckpb1.common.json.MiniJson.obj(meta.get("assetIndex")).get("id"), "5");
        }

        List<String> args = new ArrayList<>();
        args.add(javaBinary);
        args.add("-Xmx" + profile.ramMB + "M");
        if (profile.jvmArgs != null && !profile.jvmArgs.isBlank()) {
            for (String extra : profile.jvmArgs.trim().split("\\s+")) {
                if (!extra.isBlank()) {
                    args.add(extra);
                }
            }
        }
        // write the classpath into an @argfile to avoid command length limits
        StringBuilder cp = new StringBuilder();
        boolean first = true;
        for (Path p : classpath) {
            if (!first) {
                cp.append(File.pathSeparatorChar);
            }
            cp.append(p.toAbsolutePath().toString().replace("\\", "\\\\"));
            first = false;
        }
        Path argFile = gameDir.resolve("ckpb1-classpath.txt");
        Files.writeString(argFile, "-classpath \"" + cp + "\"\n", StandardCharsets.UTF_8);
        args.add("@" + argFile.toAbsolutePath());
        args.add(fabricProfile.mainClass);
        args.add("--username");
        args.add(username);
        args.add("--version");
        args.add(profile.mcVersion);
        args.add("--gameDir");
        args.add(gameDir.toAbsolutePath().toString());
        args.add("--assetsDir");
        args.add(gameDir.resolve("assets").toAbsolutePath().toString());
        args.add("--assetIndex");
        args.add(assetIndex);
        args.add("--uuid");
        args.add(uuid);
        args.add("--accessToken");
        args.add("0");
        args.add("--userType");
        args.add("legacy");
        args.add("--versionType");
        args.add("CK_PB1");

        ProcessBuilder pb = new ProcessBuilder(args);
        pb.directory(gameDir.toFile());
        pb.redirectErrorStream(true);
        process = pb.start();
        return process;
    }

    /** Pumps the game console output to the callback on a daemon thread. */
    public void pumpConsole(Process process, Console console) {
        Thread out = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    console.line(line);
                }
            } catch (IOException ignored) {
            }
        }, "CK_PB1-game-console");
        out.setDaemon(true);
        out.start();
        Thread watcher = new Thread(() -> {
            try {
                int code = process.waitFor();
                console.line("---- game exited (code " + code + ") ----");
            } catch (InterruptedException ignored) {
            }
        }, "CK_PB1-game-watcher");
        watcher.setDaemon(true);
        watcher.start();
    }
}
