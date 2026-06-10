package dev.maire.nourished.compat.spiceoflifeonion;

import dev.marie.MariesLib.config.ModuleCache;
import dev.marie.MariesLib.core.MarieLibContext;
import dev.marie.MariesLib.core.MariesLib;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import team.creative.solonion.api.FoodPlayerData;
import team.creative.solonion.api.SOLOnionAPI;
import team.creative.solonion.common.SOLOnion;

import java.util.Objects;

public final class SpiceOfLifeOnionCompat {

    private static final String SOL_ONION_MOD_ID = "solonion";
    private static final ResourceLocation MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(MarieLibContext.get().modId(), "sol_onion_value_modifier");
    private static final ResourceLocation MAX_HEALTH_ATTRIBUTE_ID =
            ResourceLocation.fromNamespaceAndPath("minecraft", "max_health");

    private SpiceOfLifeOnionCompat() {}

    public static void register() {
        if (!ModList.get().isLoaded(SOL_ONION_MOD_ID)) {
            return;
        }
        NeoForge.EVENT_BUS.addListener(SpiceOfLifeOnionCompat::onItemUseFinish);
    }

    private static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!ModList.get().isLoaded(SOL_ONION_MOD_ID)) {
            return;
        }
        ItemStack stack = event.getItem();
        FoodProperties sourceProperties = stack.getItem().getFoodProperties(stack, serverPlayer);
        if (sourceProperties == null) {
            return;
        }

        double normalized = normalizedDiversity(serverPlayer);
        if (normalized < 0.0d) {
            return;
        }

        double combined = 0.0d;
        if (ModuleCache.enableSOLDiversityHealth) {
            combined += diversityHealthMaxHealthModifier(normalized);
        }
        if (ModuleCache.enableSOLDiversityPenalty) {
            combined += diversityPenaltyMaxHealthModifier(normalized);
        }

        applyAttributeModifier(serverPlayer, MAX_HEALTH_ATTRIBUTE_ID, combined);
    }

    private static double diversityHealthMaxHealthModifier(double diversity) {
        if (diversity > 0.75d) {
            return 0.15d;
        }
        return 0.0d;
    }

    private static double diversityPenaltyMaxHealthModifier(double diversity) {
        if (diversity < 0.25d) {
            return -0.15d;
        }
        return 0.0d;
    }

    private static double normalizedDiversity(ServerPlayer player) {
        try {
            FoodPlayerData playerData = SOLOnionAPI.getFoodCapability(player);
            double rawDiversity = playerData.foodDiversity(player);
            int trackCount = Math.max(1, SOLOnion.CONFIG.trackCount);
            return Math.min(1.0d, rawDiversity / (double) trackCount);
        } catch (Throwable t) {
            MariesLib.LOGGER.debug("[MarieLib] Spice of Life: Onion diversity unavailable, skipping SoL modifiers.", t);
            return -1.0d;
        }
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
            MariesLib.LOGGER.debug("[MarieLib] SoL Onion attribute modifier skipped for {}.", attributeId, t);
        }
    }
}
