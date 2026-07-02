package dev.maire.nourished.modules.RawFood.handler;

import dev.marie.framework.api.ApiStatus;
import dev.maire.nourished.config.NourishedModuleCache;
import dev.marie.framework.tracking.TrackingAttachment;
import dev.marie.framework.tracking.TrackingData;
import dev.maire.nourished.core.effect.NutritionEffectApplier;
import dev.maire.nourished.core.network.ModNetworking;
import dev.maire.nourished.core.nutrition.FoodNutritionRegistry;
import dev.maire.nourished.core.nutrition.NutrientClassificationLookup;
import dev.marie.framework.util.MarieEffectUtils;
import dev.marie.framework.util.MarieRegistryUtils;
import dev.maire.nourished.modules.RawFood.core.RawFoodConfig;
import dev.maire.nourished.modules.RawFood.core.RawFoodTierDef;
import dev.maire.nourished.modules.RawFood.core.RawSeverity;
import dev.maire.nourished.modules.RawFood.Gut.GutHealthAttachment;
import dev.maire.nourished.modules.RawFood.Gut.GutHealthData;
import dev.maire.nourished.modules.RawFood.rawInfo.CookedVersionResolver;
import dev.maire.nourished.modules.RawFood.rawInfo.CookednessResolver;
import dev.maire.nourished.modules.RawFood.rawInfo.RawFoodClassifier;
import dev.maire.nourished.modules.RawFood.rawInfo.RawFoodResistanceResolver;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.NourishedKubeIntegration;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ApiStatus.Internal
public class RawFoodPenaltyHandler {

    private static final ConcurrentHashMap<UUID, Map<String, Long>> RAW_EAT_TIMESTAMPS = new ConcurrentHashMap<>();

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onFoodEaten(LivingEntityUseItemEvent.Finish event) {
        if (!NourishedModuleCache.enableRawSourcePenalty) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack stack = event.getItem();
        ResourceLocation itemId = MarieRegistryUtils.itemKey(stack);
        RawSeverity severity = RawFoodClassifier.classify(stack, player.level());
        float resistance = RawFoodResistanceResolver.resolve(player, severity);
        float penaltyScale = 1.0f - resistance;
        Nourished.LOGGER.debug(
                "[RawFoodPenaltyHandler] item={}, severity={}, resistance={}, penaltyScale={}",
                itemId, severity, resistance, penaltyScale);

        if (FoodNutritionRegistry.foodPropertiesForNutrition(stack, player) == null) {
            return;
        }
        if (severity == RawSeverity.FINE) {
            return;
        }

        if (penaltyScale <= 0.0f) {
            Nourished.LOGGER.debug("[RawFoodPenaltyHandler] {} ate {} — fully resisted (severity={})",
                    player.getName().getString(), itemId, severity);
            return;
        }

        RawFoodTierDef tierDef = RawFoodConfig.getTier(severity);
        boolean repeatedRawEat = updateRawEatTimestamp(player, itemId.toString());

        if (NourishedKubeIntegration.isRawFoodPenaltyCancelled(
                player.getUUID().toString(), itemId.toString(), severity.name().toLowerCase())) {
            return;
        }

        applyRawFoodEffect(player, severity, tierDef, repeatedRawEat);
        applyNutrientPenalty(player, stack, tierDef, penaltyScale);
        sendRawFoodMessage(player, severity);
    }

    private static boolean updateRawEatTimestamp(ServerPlayer player, String itemId) {
        long now = System.currentTimeMillis();
        Map<String, Long> playerTimestamps = RAW_EAT_TIMESTAMPS.computeIfAbsent(player.getUUID(), ignored -> new ConcurrentHashMap<>());
        Long previous = playerTimestamps.put(itemId, now);
        return previous != null && now - previous <= RawFoodConfig.memorySecs() * 1000L;
    }

    private static void applyRawFoodEffect(ServerPlayer player, RawSeverity severity, RawFoodTierDef tierDef, boolean repeatedRawEat) {
        if (tierDef.effectPool().isEmpty()) {
            return;
        }

        SelectedEffect selected = repeatedRawEat
                ? activeEffectFromPool(player, tierDef.effectPool())
                : null;
        if (selected == null) {
            selected = randomEffectFromPool(player, tierDef.effectPool());
        }
        if (selected == null) {
            return;
        }

        int durationTicks = tierDef.durationTicks();
        if (repeatedRawEat && selected.existing() != null) {
            durationTicks += selected.existing().getDuration();
        }
        int amplifier = severity == RawSeverity.SEVERE ? tierDef.amplifier() : 0;
        player.addEffect(new MobEffectInstance(selected.effect(), durationTicks, amplifier, false, true));
    }

    private static SelectedEffect activeEffectFromPool(ServerPlayer player, List<String> effectPool) {
        for (String effectId : effectPool) {
            Holder<MobEffect> effect = MarieEffectUtils.resolveEffect(effectId).orElse(null);
            if (effect == null) {
                continue;
            }
            MobEffectInstance existing = player.getEffect(effect);
            if (existing != null) {
                return new SelectedEffect(effect, existing);
            }
        }
        return null;
    }

    private static SelectedEffect randomEffectFromPool(ServerPlayer player, List<String> effectPool) {
        List<Holder<MobEffect>> resolved = new ArrayList<>();
        for (String effectId : effectPool) {
            Holder<MobEffect> effect = MarieEffectUtils.resolveEffect(effectId).orElse(null);
            if (effect != null) {
                resolved.add(effect);
            }
        }
        if (resolved.isEmpty()) {
            return null;
        }
        Holder<MobEffect> effect = resolved.get(player.getRandom().nextInt(resolved.size()));
        return new SelectedEffect(effect, player.getEffect(effect));
    }

    private static void applyNutrientPenalty(ServerPlayer player, ItemStack stack, RawFoodTierDef tierDef, float penaltyScale) {
        Map<String, Float> matchedBars = NutrientClassificationLookup.resolveBars(stack, player.level());
        String dominantKey = matchedBars.entrySet().stream()
                .max(Comparator.comparingDouble(entry -> entry.getValue()))
                .map(Map.Entry::getKey)
                .orElse(null);
        if (dominantKey == null) {
            return;
        }

        ResourceLocation itemId = MarieRegistryUtils.itemKey(stack);
        float cookedness = CookednessResolver.resolve(itemId);

        GutHealthData gut = player.getData(GutHealthAttachment.GUT.get());
        float oldGutHealth = gut.getGutHealth();
        gut.applyRawEat(cookedness, Math.abs(tierDef.nutrientPenalty()) * penaltyScale);
        float sensitivityMultiplier = gut.effectivePenaltyMultiplier();
        player.setData(GutHealthAttachment.GUT.get(), gut);
        ModNetworking.syncGutHealth(player, gut);
        NourishedKubeIntegration.fireGutHealthChanged(
                player.getUUID().toString(), oldGutHealth, gut.getGutHealth(), "raw_food");

        TrackingData diet = player.getData(TrackingAttachment.TRACKING.get());

        float scaledNutrientPenalty = tierDef.nutrientPenalty() * penaltyScale * sensitivityMultiplier;
        diet.addValue(dominantKey, scaledNutrientPenalty);

        if (tierDef.missedOpportunityMultiplier() > 0f) {
            Optional<Map<String, Float>> cookedNutrients = CookedVersionResolver.resolveCooked(itemId);
            if (cookedNutrients.isPresent()) {
                for (Map.Entry<String, Float> entry : cookedNutrients.get().entrySet()) {
                    float missedAmount = entry.getValue() * tierDef.missedOpportunityMultiplier() * penaltyScale * sensitivityMultiplier;
                    diet.addValue(entry.getKey(), -missedAmount);
                }
            }
        }

        player.setData(TrackingAttachment.TRACKING.get(), diet);
        ModNetworking.syncDietDelta(player, diet);
        NutritionEffectApplier.apply(player, diet);
    }

    private static void sendRawFoodMessage(ServerPlayer player, RawSeverity severity) {
        player.sendSystemMessage(
                Component.literal("⚠ ").withStyle(ChatFormatting.RED)
                        .append(Component.literal("Raw Food ").withStyle(style -> style.withColor(0xFF6B6B).withBold(true)))
                        .append(Component.literal(messageFor(severity)).withStyle(style -> style.withColor(0xFFC2C2)))
        );
    }

    private static String messageFor(RawSeverity severity) {
        return switch (severity) {
            case MILD -> "That probably could have used some cooking.";
            case MEDIUM -> "You really should have cooked that.";
            case SEVERE -> "That was a terrible idea to eat.";
            case FINE -> "";
        };
    }

    private record SelectedEffect(Holder<MobEffect> effect, MobEffectInstance existing) {}
}
