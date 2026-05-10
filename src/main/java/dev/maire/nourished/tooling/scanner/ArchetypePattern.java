package dev.maire.nourished.tooling.scanner;

import dev.maire.nourished.api.ApiStatus;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Compound food archetype detection pattern. Loaded from JSON via
 * {@link ScannerSpecRegistry}; nothing about archetypes is hardcoded in Java.
 *
 * @param pattern       Substring searched for inside the item path (case-insensitive callers expected).
 * @param contributions Per-nutrient score deltas this archetype awards on a match.
 */
@ApiStatus.Internal
public record ArchetypePattern(String pattern, Map<String, Float> contributions) {

    private static final Map<String, Pattern> COMPILED_PATTERNS = new ConcurrentHashMap<>();

    public ArchetypePattern {
        contributions = contributions == null ? Map.of() : Map.copyOf(contributions);
    }

    public boolean matches(String path) {
        return path.contains(pattern);
    }

    public Pattern compiledPattern() {
        return COMPILED_PATTERNS.computeIfAbsent(pattern, p -> Pattern.compile(".*" + Pattern.quote(p) + ".*"));
    }
}
