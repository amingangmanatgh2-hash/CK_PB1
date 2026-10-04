package ckpb1.launcher.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds installed Java runtimes (17+) for launching CK_PB1.
 */
public final class JavaLocator {

    public static final class JavaInstall {
        public final String path;
        public final String version;
        public final int major;

        JavaInstall(String path, String version, int major) {
            this.path = path;
            this.version = version;
            this.major = major;
        }

        public boolean usable() {
            return major >= 17;
        }

        @Override
        public String toString() {
            return "Java " + version + "  -  " + path;
        }
    }

    private JavaLocator() {
    }

    /** Executes {@code <java> -version} and parses the major version. */
    public static JavaInstall probe(String javaBinary) {
        try {
            ProcessBuilder pb = new ProcessBuilder(javaBinary, "-version");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String out;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
                out = sb.toString();
            }
            boolean done = p.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);
            if (!done) {
                p.destroyForcibly();
                return null;
            }
            Matcher m = Pattern.compile("version \"([^\"]+)\"").matcher(out);
            if (!m.find()) {
                return null;
            }
            String version = m.group(1);
            int major;
            if (version.startsWith("1.")) {
                major = Integer.parseInt(version.substring(2, 3));
            } else {
                int dot = version.indexOf('.');
                major = Integer.parseInt(dot > 0 ? version.substring(0, dot) : version.replaceAll("[^0-9].*", ""));
            }
            return new JavaInstall(javaBinary, version, major);
        } catch (Exception e) {
            return null;
        }
    }

    private static String binary(Path home) {
        String os = System.getProperty("os.name", "").toLowerCase();
        String exe = os.contains("win") ? "javaw.exe" : "java";
        Path p = home.resolve("bin").resolve(exe);
        if (!java.nio.file.Files.exists(p) && os.contains("win")) {
            p = home.resolve("bin").resolve("java.exe");
        }
        return java.nio.file.Files.exists(p) ? p.toString() : null;
    }

    /** Scans the system for Java installations. */
    public static List<JavaInstall> findJavas() {
        Set<String> candidates = new LinkedHashSet<>();
        // bundled runtime shipped with the installer (next to the launcher jar)
        try {
            Path launcherDir = Path.of(JavaLocator.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()).getParent();
            String bundled = binary(launcherDir.resolve("jre"));
            if (bundled != null) {
                candidates.add(bundled);
            }
            bundled = binary(launcherDir.resolve("runtime"));
            if (bundled != null) {
                candidates.add(bundled);
            }
        } catch (Exception ignored) {
        }
        // current JVM
        candidates.add(binary(Path.of(System.getProperty("java.home"))));
        // JAVA_HOME
        String jh = System.getenv("JAVA_HOME");
        if (jh != null && !jh.isBlank()) {
            String b = binary(Path.of(jh));
            if (b != null) {
                candidates.add(b);
            }
        }
        candidates.add("java"); // PATH
        // common Windows locations
        String[] roots = {
                "C:\\Program Files\\Java",
                "C:\\Program Files (x86)\\Java",
                "C:\\Program Files\\Eclipse Adoptium",
                "C:\\Program Files\\Microsoft",
                "C:\\Program Files\\Zulu",
                "C:\\Program Files\\Amazon Corretto",
                "C:\\Program Files\\BellSoft",
        };
        for (String root : roots) {
            File dir = new File(root);
            File[] children = dir.listFiles();
            if (children != null) {
                for (File child : children) {
                    String b = binary(child.toPath());
                    if (b != null) {
                        candidates.add(b);
                    }
                }
            }
        }
        // linux locations
        File[] jvms = new File("/usr/lib/jvm").listFiles();
        if (jvms != null) {
            for (File jvm : jvms) {
                String b = binary(jvm.toPath());
                if (b != null) {
                    candidates.add(b);
                }
            }
        }

        List<JavaInstall> found = new ArrayList<>();
        for (String candidate : candidates) {
            JavaInstall install = probe(candidate);
            if (install != null && !found.stream().anyMatch(j -> j.path.equals(install.path))) {
                found.add(install);
            }
        }
        // best (usable, highest version) first
        found.sort((a, b) -> {
            int usable = Boolean.compare(b.usable(), a.usable());
            return usable != 0 ? usable : Integer.compare(b.major, a.major);
        });
        return found;
    }
}
