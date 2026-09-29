package ckpb1.launcher.mc;

import ckpb1.common.json.MiniJson;
import ckpb1.launcher.core.DownloadManager;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Downloads and installs vanilla Minecraft (official Mojang manifest + client
 * jar + libraries + natives + assets) into a game directory. Nothing is
 * redistributed - everything is fetched at runtime from the official servers,
 * exactly like the vanilla launcher does.
 */
public final class MinecraftInstall {

    public static final String MANIFEST_URL =
            "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";

    public interface Progress {
        void update(String step);
    }

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final DownloadManager downloads;

    public MinecraftInstall(DownloadManager downloads) {
        this.downloads = downloads;
    }

    private String get(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", "CK_PB1-Launcher/" + ckpb1.common.CKPB1.VERSION)
                .GET()
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IOException("HTTP " + response.statusCode() + " for " + url);
        }
        return response.body();
    }

    private String osName() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) return "windows";
        if (os.contains("mac")) return "osx";
        return "linux";
    }

    private String osArch() {
        String arch = System.getProperty("os.arch", "");
        return arch.contains("aarch64") || arch.contains("arm") ? "arm64" : "x86";
    }

    // --------------------------------------------------------------- manifest

    /** All versions from the official manifest (id, type, url). */
    public List<String[]> fetchVersions() throws IOException, InterruptedException {
        String body = get(MANIFEST_URL);
        List<String[]> out = new ArrayList<>();
        for (Object o : MiniJson.arr(MiniJson.parse(body))) {
            Map<String, Object> v = MiniJson.obj(o);
            out.add(new String[]{
                    MiniJson.str(v.get("id"), ""),
                    MiniJson.str(v.get("type"), ""),
                    MiniJson.str(v.get("url"), "")
            });
        }
        return out;
    }

    // ---------------------------------------------------------------- install

    private static final class Batch {
        final List<DownloadManager.Task> tasks = new ArrayList<>();
        final List<Path> nativeJars = new ArrayList<>();

        void await() throws Exception {
            if (!DownloadManager.awaitAll(tasks)) {
                throw new IOException("Some downloads failed - see the Downloads tab");
            }
        }
    }

    /** Installs vanilla Minecraft into gameDir. Returns the version meta. */
    public Map<String, Object> install(String versionId, Path gameDir, Progress progress) throws Exception {
        Path versionsDir = gameDir.resolve("versions");
        Path librariesDir = gameDir.resolve("libraries");
        Path assetsDir = gameDir.resolve("assets");
        Files.createDirectories(versionsDir);
        Files.createDirectories(librariesDir);
        Files.createDirectories(assetsDir);

        progress.update("Fetching version manifest...");
        String versionUrl = null;
        for (String[] v : fetchVersions()) {
            if (v[0].equals(versionId)) {
                versionUrl = v[2];
                break;
            }
        }
        if (versionUrl == null) {
            throw new IOException("Minecraft version " + versionId + " not found in the official manifest");
        }
        // version json (small - synchronous)
        Path versionJson = versionsDir.resolve(versionId).resolve(versionId + ".json");
        if (!Files.exists(versionJson)) {
            Files.createDirectories(versionJson.getParent());
            Files.writeString(versionJson, get(versionUrl), StandardCharsets.UTF_8);
        }
        Map<String, Object> meta = MiniJson.obj(MiniJson.parse(Files.readString(versionJson, StandardCharsets.UTF_8)));

        // ---- client jar + libraries + natives --------------------------------
        Batch core = new Batch();
        progress.update("Downloading Minecraft " + versionId + " client jar...");
        Map<String, Object> client = MiniJson.obj(MiniJson.obj(meta.get("downloads")).get("client"));
        Path clientJar = versionsDir.resolve(versionId).resolve(versionId + ".jar");
        enqueueIfNeeded(core, MiniJson.str(client.get("url"), ""), clientJar,
                MiniJson.str(client.get("sha1"), null), MiniJson.lng(client.get("size"), 0));

        progress.update("Downloading libraries...");
        String nativesClassifier = "natives-" + osName();
        for (Object o : MiniJson.arr(meta.get("libraries"))) {
            Map<String, Object> lib = MiniJson.obj(o);
            if (!rulesAllow(lib)) {
                continue;
            }
            Map<String, Object> downloadsObj = MiniJson.obj(lib.get("downloads"));
            Map<String, Object> artifact = MiniJson.obj(downloadsObj.get("artifact"));
            String artifactPath = MiniJson.str(artifact.get("path"), "");
            if (!artifactPath.isBlank()) {
                enqueueIfNeeded(core, MiniJson.str(artifact.get("url"), ""),
                        librariesDir.resolve(mavenPath(artifactPath)),
                        MiniJson.str(artifact.get("sha1"), null), MiniJson.lng(artifact.get("size"), 0));
            }
            Map<String, Object> classifiers = MiniJson.obj(downloadsObj.get("classifiers"));
            Map<String, Object> nativeArtifact = MiniJson.obj(classifiers.get(nativesClassifier));
            String nativePath = MiniJson.str(nativeArtifact.get("path"), "");
            if (!nativePath.isBlank()) {
                Path nativeJar = librariesDir.resolve(mavenPath(nativePath));
                enqueueIfNeeded(core, MiniJson.str(nativeArtifact.get("url"), ""), nativeJar,
                        MiniJson.str(nativeArtifact.get("sha1"), null), MiniJson.lng(nativeArtifact.get("size"), 0));
                core.nativeJars.add(nativeJar);
            }
        }
        core.await();

        // ---- natives extraction ----------------------------------------------
        progress.update("Extracting natives...");
        Path nativesDir = versionsDir.resolve(versionId).resolve("natives");
        for (Path jar : core.nativeJars) {
            extractNatives(jar, nativesDir);
        }

        // ---- assets ------------------------------------------------------------
        progress.update("Downloading assets...");
        Map<String, Object> assetIndex = MiniJson.obj(meta.get("assetIndex"));
        String indexId = MiniJson.str(assetIndex.get("id"), "legacy");
        Path indexFile = assetsDir.resolve("indexes").resolve(indexId + ".json");
        if (!Files.exists(indexFile)) {
            Files.createDirectories(indexFile.getParent());
            Files.writeString(indexFile, get(MiniJson.str(assetIndex.get("url"), "")), StandardCharsets.UTF_8);
        }
        Map<String, Object> index = MiniJson.obj(MiniJson.parse(Files.readString(indexFile, StandardCharsets.UTF_8)));
        Map<String, Object> objects = MiniJson.obj(index.get("objects"));
        int total = objects.size();
        int done = 0;
        Batch assets = new Batch();
        for (Map.Entry<String, Object> e : objects.entrySet()) {
            Map<String, Object> obj = MiniJson.obj(e.getValue());
            String hash = MiniJson.str(obj.get("hash"), "");
            long size = MiniJson.lng(obj.get("size"), 0);
            if (hash.isBlank()) {
                continue;
            }
            Path target = assetsDir.resolve("objects").resolve(hash.substring(0, 2)).resolve(hash);
            if (!Files.exists(target) || Files.size(target) != size) {
                enqueueIfNeeded(assets,
                        "https://resources.download.minecraft.net/" + hash.substring(0, 2) + "/" + hash,
                        target, hash, size);
            }
            done++;
            if (done % 200 == 0) {
                progress.update("Assets: " + done + "/" + total);
                assets.await(); // await in waves to keep the queue bounded
            }
        }
        assets.await();

        progress.update("Minecraft " + versionId + " ready");
        return meta;
    }

    /** Normalizes a maven artifact path using the platform separator. */
    private static Path mavenPath(String artifactPath) {
        return Path.of(artifactPath.replace('\\', '/'));
    }

    private boolean rulesAllow(Map<String, Object> lib) {
        Object rulesObj = lib.get("rules");
        if (!(rulesObj instanceof List<?> rules) || rules.isEmpty()) {
            return true;
        }
        boolean allowed = false;
        for (Object r : rules) {
            Map<String, Object> rule = MiniJson.obj(r);
            String action = MiniJson.str(rule.get("action"), "");
            Map<String, Object> os = MiniJson.obj(rule.get("os"));
            if (os.isEmpty()) {
                allowed = action.equals("allow");
                continue;
            }
            String osNameRule = MiniJson.str(os.get("name"), "");
            if (!osNameRule.isEmpty() && !osNameRule.equals(osName())) {
                continue;
            }
            String arch = MiniJson.str(os.get("arch"), "");
            if (!arch.isEmpty() && !arch.equals(osArch())) {
                continue;
            }
            allowed = action.equals("allow");
        }
        return allowed;
    }

    private void enqueueIfNeeded(Batch batch, String url, Path target, String sha1, long size) throws IOException {
        if (url == null || url.isBlank()) {
            return;
        }
        if (Files.exists(target)) {
            if (size > 0 && Files.size(target) == size && (sha1 == null || sha1Matches(target, sha1))) {
                return;
            }
            Files.deleteIfExists(target);
        }
        Files.createDirectories(target.getParent());
        batch.tasks.add(downloads.enqueue(target.getFileName().toString(), url, target, sha1, size));
    }

    private static boolean sha1Matches(Path file, String expected) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            try (InputStream in = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) > 0) {
                    digest.update(buffer, 0, read);
                }
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : digest.digest()) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString().equalsIgnoreCase(expected);
        } catch (Exception e) {
            return false;
        }
    }

    private static void extractNatives(Path jar, Path intoDir) throws IOException {
        Files.createDirectories(intoDir);
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(jar))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory() || entry.getName().startsWith("META-INF/")) {
                    continue;
                }
                Path out = intoDir.resolve(entry.getName().replace('/', java.io.File.separatorChar));
                if (!out.normalize().startsWith(intoDir.normalize())) {
                    continue; // zip-slip guard
                }
                Files.createDirectories(out.getParent());
                Files.copy(zip, out, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
}
