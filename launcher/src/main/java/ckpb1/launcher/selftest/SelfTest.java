package ckpb1.launcher.selftest;

import ckpb1.common.CKPB1;
import ckpb1.common.SemVer;
import ckpb1.common.json.MiniJson;
import ckpb1.common.release.GitHubReleases;
import ckpb1.common.release.ReleaseInfo;
import ckpb1.launcher.core.JavaLocator;
import ckpb1.launcher.core.LauncherSettings;
import ckpb1.launcher.mc.FabricInstall;
import ckpb1.launcher.mc.MinecraftInstall;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Headless self-test of the launcher services. Runs in CI to prove the
 * launcher actually talks to Mojang/Fabric/GitHub correctly.
 * Exit code 0 = all tests passed.
 */
public final class SelfTest {

    private static int failures;

    private SelfTest() {
    }

    public static int run() {
        System.out.println("== CK_PB1 Launcher self-test ==");
        testJson();
        testSemVer();
        testSettings();
        testJavaLocator();
        testMinecraftManifest();
        testFabricMeta();
        testGitHubReleases();
        System.out.println(failures == 0 ? "== ALL TESTS PASSED ==" : "== " + failures + " TEST(S) FAILED ==");
        return failures == 0 ? 0 : 1;
    }

    private static void check(String name, boolean ok, String detail) {
        System.out.printf("  [%s] %s%s%n", ok ? "PASS" : "FAIL", name, detail == null ? "" : " - " + detail);
        if (!ok) {
            failures++;
        }
    }

    private static void testJson() {
        try {
            Map<String, Object> m = MiniJson.obj(MiniJson.parse("{\"a\":[1,2.5,\"x\",true,null],\"b\":\"سلام\"}"));
            String out = MiniJson.write(m);
            Map<String, Object> again = MiniJson.obj(MiniJson.parse(out));
            check("MiniJson round-trip", again.get("b").equals("سلام") && MiniJson.arr(m.get("a")).size() == 5, out);
        } catch (Exception e) {
            check("MiniJson round-trip", false, e.toString());
        }
    }

    private static void testSemVer() {
        boolean ok = SemVer.parse("v1.2.3").isGreaterThan(SemVer.parse("1.2.2"))
                && !SemVer.parse("1.0.0").isGreaterThan(SemVer.parse("v1.0.0"))
                && SemVer.parse("2.0.0-rc1").isGreaterThan(SemVer.parse("1.9.9"));
        check("SemVer comparisons", ok, SemVer.parse("v1.2.3").toString());
    }

    private static void testSettings() {
        try {
            Path tmp = Files.createTempDirectory("ckpb1-test");
            LauncherSettings settings = new LauncherSettings(tmp.resolve("settings.json"));
            settings.activeProfile = "Test";
            settings.active().username = "Steve";
            settings.save();
            LauncherSettings loaded = new LauncherSettings(tmp.resolve("settings.json"));
            check("Settings save/load", "Steve".equals(loaded.active().username)
                    && "Test".equals(loaded.activeProfile), tmp.toString());
        } catch (Exception e) {
            check("Settings save/load", false, e.toString());
        }
    }

    private static void testJavaLocator() {
        List<JavaLocator.JavaInstall> javas = JavaLocator.findJavas();
        check("Java locator finds a JVM", !javas.isEmpty() && javas.get(0).major >= 17,
                javas.isEmpty() ? "none" : javas.get(0).toString());
    }

    private static void testMinecraftManifest() {
        try {
            List<String[]> versions = new MinecraftInstall(null).fetchVersions();
            boolean has1201 = versions.stream().anyMatch(v -> v[0].equals(CKPB1.MINECRAFT_VERSION));
            check("Mojang version manifest", has1201, versions.size() + " versions, 1.20.1=" + has1201);
        } catch (Exception e) {
            check("Mojang version manifest", false, e.toString());
        }
    }

    private static void testFabricMeta() {
        try {
            FabricInstall fabric = new FabricInstall();
            String loader = fabric.latestLoaderVersion(CKPB1.MINECRAFT_VERSION);
            FabricInstall.Profile profile = fabric.fetchProfile(CKPB1.MINECRAFT_VERSION, loader);
            check("Fabric meta", loader != null && !loader.isBlank() && !profile.libraries.isEmpty(),
                    "loader " + loader + ", " + profile.libraries.size() + " libraries, main " + profile.mainClass);
        } catch (Exception e) {
            check("Fabric meta", false, e.toString());
        }
    }

    private static void testGitHubReleases() {
        try {
            GitHubReleases api = new GitHubReleases();
            ReleaseInfo latest = api.latest();
            check("GitHub releases", latest != null, latest == null ? "no releases yet" : latest.toString());
        } catch (Exception e) {
            check("GitHub releases", false, e.toString());
        }
    }
}
