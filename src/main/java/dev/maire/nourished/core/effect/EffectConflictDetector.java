package dev.maire.nourished.effect;

import dev.maire.nourished.nutrition.Nourished;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class EffectConflictDetector {

    private static final Set<String> warned = new HashSet<>();

    private EffectConflictDetector() {}

    static void checkAndWarn(
            EffectRegistry.EffectDef def,
            boolean shouldApply,
            Map<ResourceLocation, String> seenActiveEffects
    ) {
        if (!shouldApply) {
            return;
        }
        ResourceLocation effectId;
        try {
            effectId = ResourceLocation.parse(def.effect());
        } catch (Exception ignored) {
            return;
        }

        String previous = seenActiveEffects.putIfAbsent(effectId, def.id());
        if (previous == null) {
            return;
        }

        String conflictKey = effectId + "|" + previous + "|" + def.id();
        if (warned.add(conflictKey)) {
            Nourished.LOGGER.warn(
                    "[EffectRegistry] Conflicting active effect rules for {}: '{}' and '{}'",
                    effectId,
                    previous,
                    def.id()
            );
        }
    }

    static void clearWarned() {
        warned.clear();
    }
}
