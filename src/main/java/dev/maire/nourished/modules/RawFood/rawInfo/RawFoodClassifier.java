package dev.maire.nourished.modules.RawFood.rawInfo;

import dev.marie.MariesLib.api.ApiStatus;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.nutrition.NutrientClassificationLookup;
import dev.maire.nourished.core.tags.NourishedItemTags;
import dev.marie.MariesLib.util.MarieRegistryUtils;
import dev.maire.nourished.modules.RawFood.core.RawFoodConfig;
import dev.maire.nourished.modules.RawFood.core.RawSeverity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves {@link RawSeverity} for an {@link ItemStack}.
 * This is the single entry point the handler uses — it orchestrates tags, tokens, and cookedness into one answer.
 *
 * <p>All classification results are cached after first resolution.
 * The hot path (player eating) performs only cache lookups.</p>
 */
@ApiStatus.Internal
public final class RawFoodClassifier {

    private static final ConcurrentHashMap<ResourceLocation, RawSeverity> SEVERITY_CACHE = new ConcurrentHashMap<>();

    private RawFoodClassifier() {}

    /**
     * Classifies the given item stack's raw food severity.
     *
     * @param stack the item stack to classify
     * @param level the world level (reserved for future use)
     * @return the raw food severity
     */
    public static RawSeverity classify(ItemStack stack, Level level) {
        ResourceLocation itemId = MarieRegistryUtils.itemKey(stack);

        RawSeverity cached = SEVERITY_CACHE.get(itemId);
        if (cached != null) {
            return cached;
        }

        if (stack.is(NourishedItemTags.rawSourceFine())) {
            SEVERITY_CACHE.put(itemId, RawSeverity.FINE);
            return RawSeverity.FINE;
        }
        if (stack.is(NourishedItemTags.rawSourceMild())) {
            SEVERITY_CACHE.put(itemId, RawSeverity.MILD);
            return RawSeverity.MILD;
        }
        if (stack.is(NourishedItemTags.rawSourceMedium())) {
            SEVERITY_CACHE.put(itemId, RawSeverity.MEDIUM);
            return RawSeverity.MEDIUM;
        }
        if (stack.is(NourishedItemTags.rawSourceSevere())) {
            SEVERITY_CACHE.put(itemId, RawSeverity.SEVERE);
            return RawSeverity.SEVERE;
        }

        float cookedness = CookednessResolver.resolve(itemId);
        RawSeverity severity = mapCookednessToSeverity(cookedness);

        if (severity == RawSeverity.FINE) {
            String itemPath = itemId.getPath();
            String displayName = new ItemStack(stack.getItem()).getHoverName().getString().toLowerCase(Locale.ROOT);

            RawSeverity fallbackSeverity = RawFoodConfig.classifyByTokens(itemPath, displayName);
            if (fallbackSeverity != RawSeverity.FINE) {
                Map<String, Float> bars = NutrientClassificationLookup.resolveBars(stack, level);
                if (!bars.isEmpty()) {
                    Nourished.LOGGER.debug(
                            "[RawFoodClassifier] {} → {} (token+bar fallback, path='{}', displayName='{}')",
                            itemId,
                            fallbackSeverity,
                            itemPath,
                            displayName
                    );
                    SEVERITY_CACHE.put(itemId, fallbackSeverity);
                    return fallbackSeverity;
                }
            }
        }

        Nourished.LOGGER.debug("[RawFoodClassifier] {} → {} (cookedness={})", itemId, severity, cookedness);

        SEVERITY_CACHE.put(itemId, severity);
        return severity;
    }

    /**
     * Clears the severity cache and delegates to CookednessResolver.invalidate().
     */
    public static void invalidate() {
        SEVERITY_CACHE.clear();
        CookednessResolver.invalidate();
    }

    /**
     * Returns the cached severity without computing.
     * Used for debug output.
     *
     * @param itemId the item's registry ID
     * @return the cached severity, or empty if not cached
     */
    public static Optional<RawSeverity> peek(ResourceLocation itemId) {
        return Optional.ofNullable(SEVERITY_CACHE.get(itemId));
    }

    private static RawSeverity mapCookednessToSeverity(float cookedness) {
        if (cookedness >= 1.0f) {
            return RawSeverity.FINE;
        }
        if (cookedness >= 0.6f) {
            return RawSeverity.MILD;
        }
        if (cookedness >= 0.25f) {
            return RawSeverity.MEDIUM;
        }
        return RawSeverity.SEVERE;
    }
}
