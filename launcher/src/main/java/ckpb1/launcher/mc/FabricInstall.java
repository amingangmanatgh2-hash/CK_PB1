package ckpb1.launcher.mc;

import ckpb1.common.json.MiniJson;
import ckpb1.launcher.core.DownloadManager;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Installs the Fabric loader for a Minecraft version using the official
 * fabric meta API (meta.fabricmc.net) - the same source the official
 * installer uses.
 */
public final class FabricInstall {

    public static final String META = "https://meta.fabricmc.net/v2";
    public static final String DEFAULT_MAVEN = "https://maven.fabricmc.net/";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

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

    /** Newest stable loader version for the given Minecraft version. */
    public String latestLoaderVersion(String mcVersion) throws IOException, InterruptedException {
        String body = get(META + "/versions/loader/" + mcVersion);
        for (Object o : MiniJson.arr(MiniJson.parse(body))) {
            Map<String, Object> entry = MiniJson.obj(o);
            Map<String, Object> loader = MiniJson.obj(entry.get("loader"));
            if (MiniJson.bool(loader.get("stable"), false)) {
                return MiniJson.str(loader.get("version"), "");
            }
        }
        throw new IOException("No stable fabric loader for " + mcVersion);
    }

    public static final class Profile {
        public final String mainClass;
        public final List<String[]> libraries = new ArrayList<>(); // {group:path, url}

        Profile(String mainClass) {
            this.mainClass = mainClass;
        }
    }

    /** Loader profile (main class + libraries) for a version/loader pair. */
    public Profile fetchProfile(String mcVersion, String loaderVersion) throws IOException, InterruptedException {
        String body = get(META + "/versions/loader/" + mcVersion + "/" + loaderVersion + "/profile/json");
        Map<String, Object> root = MiniJson.obj(MiniJson.parse(body));
        Profile profile = new Profile(MiniJson.str(root.get("mainClass"),
                "net.fabricmc.loader.impl.launch.knot.KnotClient"));
        for (Object o : MiniJson.arr(root.get("libraries"))) {
            Map<String, Object> lib = MiniJson.obj(o);
            String name = MiniJson.str(lib.get("name"), "");
            String url = MiniJson.str(lib.get("url"), DEFAULT_MAVEN);
            if (!name.isBlank()) {
                profile.libraries.add(new String[]{name, url});
            }
        }
        return profile;
    }

    /** maven "group:artifact:version" -> repositories path. */
    public static Path mavenArtifactPath(String coordinates) {
        String[] parts = coordinates.split(":");
        if (parts.length < 3) {
            return Path.of(coordinates.replace(':', '_') + ".jar");
        }
        String groupPath = parts[0].replace('.', '/');
        String artifact = parts[1];
        String version = parts[2];
        String classifier = parts.length >= 4 ? "-" + parts[3] : "";
        return Path.of(groupPath, artifact, version, artifact + "-" + version + classifier + ".jar");
    }

    /** Installs the fabric loader libraries into gameDir/libraries. */
    public Profile install(String mcVersion, String gameDirString, DownloadManager downloads) throws Exception {
        Path gameDir = Path.of(gameDirString);
        Path librariesDir = gameDir.resolve("libraries");
        Files.createDirectories(librariesDir);

        String loaderVersion = latestLoaderVersion(mcVersion);
        Profile profile = fetchProfile(mcVersion, loaderVersion);
        List<DownloadManager.Task> tasks = new ArrayList<>();
        for (String[] lib : profile.libraries) {
            Path target = librariesDir.resolve(mavenArtifactPath(lib[0]));
            if (Files.exists(target)) {
                continue;
            }
            Files.createDirectories(target.getParent());
            tasks.add(downloads.enqueue(target.getFileName().toString(),
                    lib[1] + mavenArtifactPath(lib[0]).toString().replace('\\', '/'),
                    target, null, 0));
        }
        if (!DownloadManager.awaitAll(tasks)) {
            throw new IOException("Fabric library download failed - see the Downloads tab");
        }
        return profile;
    }
}
