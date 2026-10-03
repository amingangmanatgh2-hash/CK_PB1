package ckpb1.launcher.core;

import ckpb1.common.SemVer;
import ckpb1.common.release.GitHubReleases;
import ckpb1.common.release.ReleaseInfo;

/**
 * Compares the installed CK_PB1 version with the latest GitHub release.
 */
public final class UpdateChecker {

    public static final class Result {
        public final boolean updateAvailable;
        public final String currentVersion;
        public final String latestVersion;
        public final ReleaseInfo latest;
        public final String error;

        private Result(boolean updateAvailable, String currentVersion, String latestVersion,
                       ReleaseInfo latest, String error) {
            this.updateAvailable = updateAvailable;
            this.currentVersion = currentVersion;
            this.latestVersion = latestVersion;
            this.latest = latest;
            this.error = error;
        }
    }

    private UpdateChecker() {
    }

    public static Result check(String repoOverride) {
        String current = ckpb1.common.CKPB1.VERSION;
        try {
            GitHubReleases api = repoOverride == null || repoOverride.isBlank()
                    ? new GitHubReleases()
                    : new GitHubReleases("https://api.github.com/repos/" + repoOverride);
            ReleaseInfo latest = api.latest();
            if (latest == null) {
                return new Result(false, current, "", null, null);
            }
            SemVer installed = SemVer.parse(current);
            SemVer published = SemVer.parse(latest.version());
            return new Result(published.isGreaterThan(installed), current, latest.version(), latest, null);
        } catch (Exception e) {
            return new Result(false, current, "", null, e.getMessage());
        }
    }
}
