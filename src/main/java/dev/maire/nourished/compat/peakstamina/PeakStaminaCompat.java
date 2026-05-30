package dev.maire.nourished.compat.peakstamina;

import dev.maire.nourished.api.NourishedAPI;
import dev.maire.nourished.api.NourishedEvents;
import dev.maire.nourished.config.ModuleCache;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;
import java.util.Objects;

public final class PeakStaminaCompat {

    private static final String PEAK_STAMINA_MOD_ID = "peakstamina";
    private static final ResourceLocation MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(Nourished.MODID, "peak_stamina_nutrition_modifier");
    private static final ResourceLocation STAMINA_REGEN_ATTRIBUTE_ID = ResourceLocation.fromNamespaceAndPath("peak_stamina", "stamina_regen");
    private static final ResourceLocation MAX_STAMINA_ATTRIBUTE_ID = ResourceLocation.fromNamespaceAndPath("peak_stamina", "max_stamina");
    private static final ResourceLocation STAMINA_USAGE_ATTRIBUTE_ID = ResourceLocation.fromNamespaceAndPath("peak_stamina", "stamina_usage");
    private static final ResourceLocation PENALTY_DECAY_MULTIPLIER_ATTRIBUTE_ID = ResourceLocation.fromNamespaceAndPath("peak_stamina", "penalty_decay_multiplier");
    private static final ResourceLocation EXHAUSTION_DURATION_MULTIPLIER_ATTRIBUTE_ID = ResourceLocation.fromNamespaceAndPath("peak_stamina", "exhaustion_duration_multiplier");

    private PeakStaminaCompat() {
    }

    public static void register() {
        if (!ModList.get().isLoaded(PEAK_STAMINA_MOD_ID)) {
            return;
        }
        NeoForge.EVENT_BUS.addListener(PeakStaminaCompat::onNutrientChanged);
    }

    private static void onNutrientChanged(NourishedEvents.NutrientChangedEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!ModList.get().isLoaded(PEAK_STAMINA_MOD_ID)) {
            return;
        }

        float averageNutrition = getAverageNutrition(serverPlayer);
        if (averageNutrition < 0.0f) {
            return;
        }

        double regenMultiplier = calculateRegenMultiplier(averageNutrition);
        double maxStaminaMultiplier = calculateMaxStaminaMultiplier(averageNutrition);
        double staminaUsageMultiplier = ModuleCache.enablePSStaminaUsage
                ? calculateStaminaUsageMultiplier(averageNutrition)
                : 0.0d;
        double penaltyDecayMultiplier = ModuleCache.enablePSPenaltyDecay
                ? calculatePenaltyDecayMultiplier(averageNutrition)
                : 0.0d;
        double exhaustionDurationMultiplier = ModuleCache.enablePSExhaustionDuration
                ? calculateExhaustionDurationMultiplier(averageNutrition)
                : 0.0d;

        applyAttributeModifier(serverPlayer, STAMINA_REGEN_ATTRIBUTE_ID, regenMultiplier);
        applyAttributeModifier(serverPlayer, MAX_STAMINA_ATTRIBUTE_ID, maxStaminaMultiplier);
        applyAttributeModifier(serverPlayer, STAMINA_USAGE_ATTRIBUTE_ID, staminaUsageMultiplier);
        applyAttributeModifier(serverPlayer, PENALTY_DECAY_MULTIPLIER_ATTRIBUTE_ID, penaltyDecayMultiplier);
        applyAttributeModifier(serverPlayer, EXHAUSTION_DURATION_MULTIPLIER_ATTRIBUTE_ID, exhaustionDurationMultiplier);
    }

    private static float getAverageNutrition(ServerPlayer player) {
        List<String> nutrientKeys = NutrientRegistry.getKeys();
        if (nutrientKeys.isEmpty()) {
            return -1.0f;
        }

        float total = 0.0f;
        int counted = 0;
        for (String key : nutrientKeys) {
            float value = NourishedAPI.getNutrientLevel(player, key);
            if (value < 0.0f) {
                continue;
            }
            total += value;
            counted++;
        }
        if (counted == 0) {
            return -1.0f;
        }
        return total / counted;
    }

    private static double calculateRegenMultiplier(float averageNutrition) {
        if (averageNutrition > 0.75f) {
            return 0.25d;
        }
        if (averageNutrition < 0.25f) {
            return -0.25d;
        }
        return 0.0d;
    }

    private static double calculateMaxStaminaMultiplier(float averageNutrition) {
        if (averageNutrition > 0.75f) {
            return 0.1d;
        }
        if (averageNutrition < 0.15f) {
            return -0.15d;
        }
        return 0.0d;
    }

    private static double calculateStaminaUsageMultiplier(float averageNutrition) {
        if (averageNutrition < 0.25f) {
            return 0.25d;
        }
        if (averageNutrition > 0.75f) {
            return -0.15d;
        }
        return 0.0d;
    }

    private static double calculatePenaltyDecayMultiplier(float averageNutrition) {
        if (averageNutrition > 0.75f) {
            return 0.3d;
        }
        return 0.0d;
    }

    private static double calculateExhaustionDurationMultiplier(float averageNutrition) {
        if (averageNutrition < 0.25f) {
            return 0.3d;
        }
        return 0.0d;
    }

    private static void applyAttributeModifier(ServerPlayer player, ResourceLocation attributeId, double multiplier) {
        try {
            ResourceLocation safeAttributeId = Objects.requireNonNull(attributeId);
            Holder.Reference<Attribute> attributeHolder = BuiltInRegistries.ATTRIBUTE.getHolder(safeAttributeId).orElse(null);
            if (attributeHolder == null) {
                return;
            }

            AttributeInstance instance = player.getAttribute(attributeHolder);
            if (instance == null) {
                return;
            }

            ResourceLocation modifierId = Objects.requireNonNull(MODIFIER_ID);
            instance.removeModifier(modifierId);
            if (multiplier == 0.0d) {
                return;
            }

            AttributeModifier modifier = new AttributeModifier(
                    modifierId,
                    multiplier,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            );
            instance.addOrUpdateTransientModifier(modifier);
        } catch (Throwable t) {
            Nourished.LOGGER.debug("[Nourished] Peak Stamina attributes unavailable, skipping nutrition modifiers.", t);
        }
    }
}
