package ckpb1.common;

/**
 * Central CK_PB1 branding and build constants.
 *
 * CK_PB1 is the name of the whole client. It must never be used as the name
 * of a module or a feature - only for the product itself (launcher, client,
 * installer, repository, releases).
 */
public final class CKPB1 {

    private CKPB1() {
    }

    /** Product name. */
    public static final String NAME = "CK_PB1";

    /** Machine identifier used for the fabric mod id and config folders. */
    public static final String ID = "ckpb1";

    /** Current CK_PB1 version (keep in sync with gradle.properties). */
    public static final String VERSION = "1.1.0";

    /** Minecraft version this client build targets. */
    public static final String MINECRAFT_VERSION = "1.20.1";

    /** GitHub repository that hosts CK_PB1 releases (owner/name). */
    public static final String GITHUB_REPO = "amingangmanatgh2-hash/CK_PB1";

    /** API base used for update checks. */
    public static final String GITHUB_API = "https://api.github.com";

    public static String githubApiRepo() {
        return GITHUB_API + "/repos/" + GITHUB_REPO;
    }

    /** Asset naming used across releases: CK_PB1-v1.0.0-Setup.exe */
    public static String setupAssetName(String version) {
        return NAME + "-v" + version + "-Setup.exe";
    }

    public static String clientJarAssetName(String version) {
        return NAME + "-Client-" + version + "-mc" + MINECRAFT_VERSION + ".jar";
    }

    public static String launcherJarAssetName(String version) {
        return NAME + "-Launcher-" + version + ".jar";
    }

    public static String fullName() {
        return NAME + " " + VERSION;
    }
}
