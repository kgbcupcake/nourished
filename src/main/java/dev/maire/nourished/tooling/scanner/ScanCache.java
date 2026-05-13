package dev.maire.nourished.tooling.scanner;

import dev.maire.nourished.api.ApiStatus;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.moddiscovery.ModInfo;

import javax.annotation.Nullable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache for scan results with automatic invalidation when mod list changes.
 * Uses a mod list hash to detect when the cache should be cleared.
 */
@ApiStatus.Internal
public final class ScanCache {

    private final ConcurrentHashMap<ResourceLocation, ClassificationResult> cache;
    private volatile String modListHash;
    @Nullable
    private volatile ScanSummary lastSummary;

    public ScanCache() {
        this.cache = new ConcurrentHashMap<>();
        this.modListHash = computeModListHash();
    }

    /**
     * Check if the cache is still valid for the current mod list.
     */
    public boolean isValid() {
        return Objects.equals(modListHash, computeModListHash());
    }

    /**
     * Get a cached result, or null if not cached.
     */
    @Nullable
    public ClassificationResult get(ResourceLocation itemId) {
        if (!isValid()) {
            invalidate();
            return null;
        }
        return cache.get(itemId);
    }

    /**
     * Store a classification result in the cache.
     */
    public void put(ResourceLocation itemId, ClassificationResult result) {
        cache.put(itemId, result);
    }

    /**
     * Get all cached results.
     */
    public Collection<ClassificationResult> getAll() {
        return cache.values();
    }

    /**
     * Get all cached results as a map.
     */
    public ConcurrentHashMap<ResourceLocation, ClassificationResult> getMap() {
        return cache;
    }

    /**
     * Invalidate the cache and update the mod list hash.
     */
    public void invalidate() {
        cache.clear();
        modListHash = computeModListHash();
        lastSummary = null;
    }

    /**
     * Get the current cache size.
     */
    public int size() {
        return cache.size();
    }

    /**
     * Check if the cache is empty.
     */
    public boolean isEmpty() {
        return cache.isEmpty();
    }

    /**
     * Get the current mod list hash.
     */
    public String getModListHash() {
        return modListHash;
    }

    /**
     * Store a scan summary for diff computation.
     */
    public void setLastSummary(ScanSummary summary) {
        this.lastSummary = summary;
    }

    /**
     * Get the last scan summary.
     */
    @Nullable
    public ScanSummary getLastSummary() {
        return lastSummary;
    }

    /**
     * Compute a hash of the current mod list (IDs + versions).
     */
    private static String computeModListHash() {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            StringBuilder sb = new StringBuilder();

            ModList.get().getMods().stream()
                    .sorted((a, b) -> a.getModId().compareTo(b.getModId()))
                    .forEach(mod -> {
                        sb.append(mod.getModId());
                        sb.append(":");
                        sb.append(mod.getVersion().toString());
                        sb.append(";");
                    });

            byte[] digest = md.digest(sb.toString().getBytes());
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(ModList.get().getMods().size());
        }
    }

    /**
     * Summary data for a scan, used for diff computation.
     */
    public record ScanSummary(
            String modListHash,
            long timestamp,
            int totalScanned,
            int autoClassified,
            int uncertain,
            int alreadyTagged,
            ConcurrentHashMap<ResourceLocation, String> dominantCategories
    ) {
        /**
         * Compute diff from another summary.
         */
        public ScanDiff diffFrom(@Nullable ScanSummary previous) {
            if (previous == null) {
                return new ScanDiff(
                        dominantCategories.keySet().stream().toList(),
                        java.util.List.of(),
                        java.util.List.of()
                );
            }

            java.util.List<ResourceLocation> added = new java.util.ArrayList<>();
            java.util.List<ResourceLocation> changed = new java.util.ArrayList<>();
            java.util.List<ResourceLocation> removed = new java.util.ArrayList<>();

            for (ResourceLocation id : dominantCategories.keySet()) {
                if (!previous.dominantCategories.containsKey(id)) {
                    added.add(id);
                } else if (!Objects.equals(dominantCategories.get(id), previous.dominantCategories.get(id))) {
                    changed.add(id);
                }
            }

            for (ResourceLocation id : previous.dominantCategories.keySet()) {
                if (!dominantCategories.containsKey(id)) {
                    removed.add(id);
                }
            }

            return new ScanDiff(added, changed, removed);
        }
    }

    /**
     * Diff between two scan summaries.
     */
    public record ScanDiff(
            java.util.List<ResourceLocation> added,
            java.util.List<ResourceLocation> changed,
            java.util.List<ResourceLocation> removed
    ) {
        public boolean hasChanges() {
            return !added.isEmpty() || !changed.isEmpty() || !removed.isEmpty();
        }

        public int totalChanges() {
            return added.size() + changed.size() + removed.size();
        }
    }
}
