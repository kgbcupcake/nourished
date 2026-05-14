package dev.maire.nourished.core.nutrition.stages;

import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.nutrition.ResolutionResult;
import dev.maire.nourished.core.nutrition.StageContext;
import dev.maire.nourished.tooling.scanner.ScannerSpecRegistry;
import dev.maire.nourished.tooling.scanner.ScannerSpecRegistry.ScannerSpec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Stage 1: matches the item against community {@code c:foods/*} tags and accumulates
 * nutrient weight contributions from scanner spec community tag weights.
 */
public final class CommunityTagStage implements ResolutionStageHandler {

    @Override
    @Nullable
    public ResolutionResult resolve(ResourceLocation itemId, StageContext ctx) {
        ScannerSpec spec = ScannerSpecRegistry.get();
        Map<String, Map<String, Float>> communityTagWeights = spec.communityTagWeights();
        Map<String, Float> contributions = new HashMap<>();

        for (Map.Entry<String, Map<String, Float>> entry : communityTagWeights.entrySet()) {
            String suffix = entry.getKey();
            ResourceLocation tagLoc = ResourceLocation.tryParse("c:foods/" + suffix);
            if (tagLoc == null) {
                Nourished.LOGGER.debug("[RuntimeFoodResolver] Skipping invalid community tag suffix: {}", suffix);
                continue;
            }
            TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagLoc);
            if (ctx.holder().is(tagKey)) {
                for (Map.Entry<String, Float> contrib : entry.getValue().entrySet()) {
                    contributions.merge(contrib.getKey(), contrib.getValue(), Float::sum);
                }
            }
        }

        if (contributions.isEmpty()) return null;

        // Deposit into context for KeywordSuffixStage to merge — do not return early
        ctx.communityTagSignal().putAll(contributions);
        return null;
    }
}
