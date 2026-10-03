package ckpb1.common;

import java.util.Objects;

/**
 * Minimal semantic-version holder used for comparing the installed CK_PB1
 * version against the latest GitHub release tag (v1.0.0, v1.2.3-hotfix, ...).
 */
public final class SemVer implements Comparable<SemVer> {

    public final int major;
    public final int minor;
    public final int patch;
    public final String suffix;

    private SemVer(int major, int minor, int patch, String suffix) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.suffix = suffix == null ? "" : suffix;
    }

    /** Parses strings like "1.0.0", "v1.2.3", "1.2.3+build.4", "v2.0.0-rc1". */
    public static SemVer parse(String raw) {
        if (raw == null) {
            return new SemVer(0, 0, 0, "");
        }
        String s = raw.trim();
        if (s.startsWith("v") || s.startsWith("V")) {
            s = s.substring(1);
        }
        int plus = s.indexOf('+');
        if (plus >= 0) {
            s = s.substring(0, plus);
        }
        String suffix = "";
        int dash = s.indexOf('-');
        if (dash >= 0) {
            suffix = s.substring(dash + 1);
            s = s.substring(0, dash);
        }
        int major = 0, minor = 0, patch = 0;
        String[] parts = s.split("[. _]");
        try {
            if (parts.length > 0 && !parts[0].isBlank()) major = Integer.parseInt(parts[0].trim());
            if (parts.length > 1 && !parts[1].isBlank()) minor = Integer.parseInt(parts[1].trim());
            if (parts.length > 2 && !parts[2].isBlank()) patch = Integer.parseInt(parts[2].trim());
        } catch (NumberFormatException ignored) {
            // keep whatever parsed so far
        }
        return new SemVer(major, minor, patch, suffix);
    }

    @Override
    public int compareTo(SemVer o) {
        if (major != o.major) return Integer.compare(major, o.major);
        if (minor != o.minor) return Integer.compare(minor, o.minor);
        return Integer.compare(patch, o.patch);
    }

    public boolean isGreaterThan(SemVer other) {
        return compareTo(other) > 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SemVer semVer)) return false;
        return major == semVer.major && minor == semVer.minor && patch == semVer.patch;
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, patch);
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch + (suffix.isEmpty() ? "" : "-" + suffix);
    }
}
