package dev.maire.nourished.compat.peakstamina;

import dev.marie.MariesLib.api.MarieAPI;
import dev.marie.MariesLib.api.MarieEvents;
import dev.maire.nourished.config.NourishedModuleCache;
import dev.marie.MariesLib.api.ValueDefinition;
import dev.marie.MariesLib.api.registry.ValueRegistry;
import dev.marie.MariesLib.core.IMarieLibConfig;
import dev.marie.MariesLib.core.MariesLib;
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
    private static final ResourceLocation MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(IMarieLibConfig.get().modId(), "peak_stamina_value_modifier");
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
        NeoForge.EVENT_BUS.addListener(PeakStaminaCompat::onValueChanged);
    }

    private static void onValueChanged(MarieEvents.ValueChangedEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!ModList.get().isLoaded(PEAK_STAMINA_MOD_ID)) {
            return;
        }

        float averageValueLevel = getAverageValueLevel(serverPlayer);
        if (averageValueLevel < 0.0f) {
            return;
        }

        double regenMultiplier = calculateRegenMultiplier(averageValueLevel);
        double maxStaminaMultiplier = calculateMaxStaminaMultiplier(averageValueLevel);
        double staminaUsageMultiplier = NourishedModuleCache.enablePSStaminaUsage
                ? calculateStaminaUsageMultiplier(averageValueLevel)
                : 0.0d;
        double penaltyDecayMultiplier = NourishedModuleCache.enablePSPenaltyDecay
                ? calculatePenaltyDecayMultiplier(averageValueLevel)
                : 0.0d;
        double exhaustionDurationMultiplier = NourishedModuleCache.enablePSExhaustionDuration
                ? calculateExhaustionDurationMultiplier(averageValueLevel)
                : 0.0d;

        applyAttributeModifier(serverPlayer, STAMINA_REGEN_ATTRIBUTE_ID, regenMultiplier);
        applyAttributeModifier(serverPlayer, MAX_STAMINA_ATTRIBUTE_ID, maxStaminaMultiplier);
        applyAttributeModifier(serverPlayer, STAMINA_USAGE_ATTRIBUTE_ID, staminaUsageMultiplier);
        applyAttributeModifier(serverPlayer, PENALTY_DECAY_MULTIPLIER_ATTRIBUTE_ID, penaltyDecayMultiplier);
        applyAttributeModifier(serverPlayer, EXHAUSTION_DURATION_MULTIPLIER_ATTRIBUTE_ID, exhaustionDurationMultiplier);
    }

    private static float getAverageValueLevel(ServerPlayer player) {
        List<String> valueKeys = ValueRegistry.getAll().stream().map(ValueDefinition::getId).toList();
        if (valueKeys.isEmpty()) {
            return -1.0f;
        }

        float total = 0.0f;
        int counted = 0;
        for (String key : valueKeys) {
            float value = MarieAPI.getValueLevel(player, key);
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

    private static double calculateRegenMultiplier(float averageValueLevel) {
        if (averageValueLevel > 0.75f) {
            return 0.25d;
        }
        if (averageValueLevel < 0.25f) {
            return -0.25d;
        }
        return 0.0d;
    }

    private static double calculateMaxStaminaMultiplier(float averageValueLevel) {
        if (averageValueLevel > 0.75f) {
            return 0.1d;
        }
        if (averageValueLevel < 0.15f) {
            return -0.15d;
        }
        return 0.0d;
    }

    private static double calculateStaminaUsageMultiplier(float averageValueLevel) {
        if (averageValueLevel < 0.25f) {
            return 0.25d;
        }
        if (averageValueLevel > 0.75f) {
            return -0.15d;
        }
        return 0.0d;
    }

    private static double calculatePenaltyDecayMultiplier(float averageValueLevel) {
        if (averageValueLevel > 0.75f) {
            return 0.3d;
        }
        return 0.0d;
    }

    private static double calculateExhaustionDurationMultiplier(float averageValueLevel) {
        if (averageValueLevel < 0.25f) {
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
            MariesLib.LOGGER.debug("[MarieLib] Peak Stamina attributes unavailable, skipping value modifiers.", t);
        }
    }
}
