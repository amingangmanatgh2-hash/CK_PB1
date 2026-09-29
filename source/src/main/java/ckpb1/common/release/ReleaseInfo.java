package ckpb1.common.release;

import ckpb1.common.json.MiniJson;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Model of a GitHub release of CK_PB1. */
public final class ReleaseInfo {

    public final String tagName;
    public final String name;
    public final String body;
    public final String publishedAt;
    public final String htmlUrl;
    public final boolean prerelease;
    public final List<Asset> assets = new ArrayList<>();

    public static final class Asset {
        public final String name;
        public final String downloadUrl;
        public final long size;

        Asset(String name, String downloadUrl, long size) {
            this.name = name;
            this.downloadUrl = downloadUrl;
            this.size = size;
        }
    }

    public static ReleaseInfo fromJson(Object parsed) {
        Map<String, Object> m = MiniJson.obj(parsed);
        ReleaseInfo r = new ReleaseInfo();
        r.tagName = MiniJson.str(m.get("tag_name"), "");
        r.name = MiniJson.str(m.get("name"), r.tagName);
        r.body = MiniJson.str(m.get("body"), "");
        r.publishedAt = MiniJson.str(m.get("published_at"), "");
        r.htmlUrl = MiniJson.str(m.get("html_url"), "");
        r.prerelease = MiniJson.bool(m.get("prerelease"), false);
        for (Object a : MiniJson.arr(m.get("assets"))) {
            Map<String, Object> am = MiniJson.obj(a);
            String an = MiniJson.str(am.get("name"), "");
            String url = MiniJson.str(am.get("browser_download_url"), "");
            long size = MiniJson.lng(am.get("size"), 0);
            if (!an.isEmpty() && !url.isEmpty()) {
                r.assets.add(new Asset(an, url, size));
            }
        }
        return r;
    }

    /** Finds the release asset whose name contains the given fragment (case-insensitive). */
    public Asset asset(String nameFragment) {
        for (Asset a : assets) {
            if (a.name.toLowerCase().contains(nameFragment.toLowerCase())) {
                return a;
            }
        }
        return null;
    }

    public String version() {
        return tagName.startsWith("v") ? tagName.substring(1) : tagName;
    }

    @Override
    public String toString() {
        return tagName + " (" + assets.size() + " assets)";
    }
}
