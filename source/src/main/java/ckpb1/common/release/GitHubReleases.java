package ckpb1.common.release;

import ckpb1.common.CKPB1;
import ckpb1.common.json.MiniJson;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Read-only GitHub Releases access for CK_PB1 update checks and changelogs.
 * Works against the public api.github.com endpoint; 404 (no releases yet) is
 * reported as "no releases" instead of an error.
 */
public final class GitHubReleases {

    public static final class ReleaseException extends RuntimeException {
        public ReleaseException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private final HttpClient http;
    private final String apiBase;

    public GitHubReleases() {
        this(CKPB1.githubApiRepo());
    }

    public GitHubReleases(String apiRepoUrl) {
        this.apiBase = apiRepoUrl;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    private String get(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", CKPB1.NAME + "/" + CKPB1.VERSION)
                    .GET()
                    .build();
            HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 404) {
                return null;
            }
            if (resp.statusCode() >= 400) {
                throw new ReleaseException("HTTP " + resp.statusCode() + " for " + url, null);
            }
            return resp.body();
        } catch (ReleaseException e) {
            throw e;
        } catch (Exception e) {
            throw new ReleaseException("Request failed for " + url + ": " + e.getMessage(), e);
        }
    }

    /** Latest published release, or null when the repository has no releases yet. */
    public ReleaseInfo latest() {
        String body = get(apiBase + "/releases/latest");
        if (body == null) {
            return null;
        }
        return ReleaseInfo.fromJson(MiniJson.parse(body));
    }

    /** All published releases, newest first (per page of 30). */
    public List<ReleaseInfo> list() {
        String body = get(apiBase + "/releases?per_page=30");
        List<ReleaseInfo> out = new ArrayList<>();
        if (body == null) {
            return out;
        }
        for (Object o : MiniJson.arr(MiniJson.parse(body))) {
            out.add(ReleaseInfo.fromJson(o));
        }
        return out;
    }
}
