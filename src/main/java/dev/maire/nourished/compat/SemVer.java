package dev.maire.nourished.compat;

import dev.maire.nourished.api.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple semantic version parser and comparator.
 * Handles versions like "1.0.0", "2.1.3-beta", "1.0" etc.
 */
@ApiStatus.Internal
public record SemVer(int major, int minor, int patch, @Nullable String preRelease) implements Comparable<SemVer> {

    private static final Pattern VERSION_PATTERN = Pattern.compile(
            "^(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?(?:[-+](.+))?$"
    );

    public static @Nullable SemVer parse(String version) {
        if (version == null || version.isBlank()) return null;

        String cleaned = version.trim();
        if (cleaned.startsWith("v") || cleaned.startsWith("V")) {
            cleaned = cleaned.substring(1);
        }

        Matcher matcher = VERSION_PATTERN.matcher(cleaned);
        if (!matcher.matches()) return null;

        try {
            int major = Integer.parseInt(matcher.group(1));
            int minor = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 0;
            int patch = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
            String preRelease = matcher.group(4);
            return new SemVer(major, minor, patch, preRelease);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public int compareTo(SemVer other) {
        if (major != other.major) return Integer.compare(major, other.major);
        if (minor != other.minor) return Integer.compare(minor, other.minor);
        if (patch != other.patch) return Integer.compare(patch, other.patch);

        if (preRelease == null && other.preRelease == null) return 0;
        if (preRelease == null) return 1;
        if (other.preRelease == null) return -1;
        return preRelease.compareTo(other.preRelease);
    }

    public boolean satisfies(String rangeSpec) {
        if (rangeSpec == null || rangeSpec.isBlank()) return false;
        rangeSpec = rangeSpec.trim();

        if ("*".equals(rangeSpec)) return true;

        if (rangeSpec.startsWith(">=")) {
            SemVer target = parse(rangeSpec.substring(2));
            return target != null && this.compareTo(target) >= 0;
        }
        if (rangeSpec.startsWith("<=")) {
            SemVer target = parse(rangeSpec.substring(2));
            return target != null && this.compareTo(target) <= 0;
        }
        if (rangeSpec.startsWith(">")) {
            SemVer target = parse(rangeSpec.substring(1));
            return target != null && this.compareTo(target) > 0;
        }
        if (rangeSpec.startsWith("<")) {
            SemVer target = parse(rangeSpec.substring(1));
            return target != null && this.compareTo(target) < 0;
        }
        if (rangeSpec.startsWith("==")) {
            SemVer target = parse(rangeSpec.substring(2));
            return target != null && this.compareTo(target) == 0;
        }

        SemVer target = parse(rangeSpec);
        return target != null && this.compareTo(target) == 0;
    }

    @Override
    public String toString() {
        String base = major + "." + minor + "." + patch;
        return preRelease != null ? base + "-" + preRelease : base;
    }
}
