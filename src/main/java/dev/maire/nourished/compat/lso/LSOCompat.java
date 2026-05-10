package dev.maire.nourished.compat.lso;

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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

public final class LSOCompat {

    private static final String LSO_MOD_ID = "legendarysurvivaloverhaul";
    private static final ResourceLocation MODIFIER_ID = ResourceLocation.fromNamespaceAndPath("nourished", "lso_nutrition_modifier");
    private static final ResourceLocation THERMAL_RESISTANCE_ATTRIBUTE_ID = ResourceLocation.fromNamespaceAndPath(
            "legendarysurvivaloverhaul",
            "thermal_resistance"
    );
    private static final ResourceLocation BROKEN_HEART_RESILIENCE_ATTRIBUTE_ID = ResourceLocation.fromNamespaceAndPath(
            "legendarysurvivaloverhaul",
            "broken_heart_resilience"
    );

    private static final String[] MOD_ATTACHMENTS_CLASS_CANDIDATES = {
            "sfiomn.legendarysurvivaloverhaul.registry.ModAttachments",
            "sfiomn.legendarysurvivaloverhaul.common.attachments.ModAttachments",
            "sfiomn.legendarysurvivaloverhaul.common.registry.ModAttachments",
    };

    private static Method entityGetDataMethod;
    private static boolean entityGetDataLookupFailed;

    private LSOCompat() {
    }

    public static void register() {
        if (!ModList.get().isLoaded(LSO_MOD_ID)) {
            return;
        }
        NeoForge.EVENT_BUS.addListener(LSOCompat::onNutrientChanged);
        NeoForge.EVENT_BUS.addListener(LSOCompat::onFoodEaten);
    }

    private static void onNutrientChanged(NourishedEvents.NutrientChangedEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!ModList.get().isLoaded(LSO_MOD_ID)) {
            return;
        }

        float averageNutrition = getAverageNutrition(serverPlayer);
        if (averageNutrition < 0.0f) {
            return;
        }

        double thermal = ModuleCache.enableLSOThermalResistance
                ? calculateThermalResistanceBonus(averageNutrition)
                : 0.0d;
        double brokenHeart = ModuleCache.enableLSOBrokenHeartResilience
                ? calculateBrokenHeartResilienceMultiplier(averageNutrition)
                : 0.0d;

        applyAttributeModifier(serverPlayer, THERMAL_RESISTANCE_ATTRIBUTE_ID, thermal, AttributeModifier.Operation.ADD_VALUE);
        applyAttributeModifier(
                serverPlayer,
                BROKEN_HEART_RESILIENCE_ATTRIBUTE_ID,
                brokenHeart,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
    }

    private static void onFoodEaten(NourishedEvents.FoodEatenEvent event) {
        if (!ModList.get().isLoaded(LSO_MOD_ID)) {
            return;
        }
        if (!ModuleCache.enableLSOThirstSaturation) {
            return;
        }
        if (!(event.getPlayer() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (event.getAmount() <= 0.05f) {
            return;
        }
        tryAddThirstSaturation(serverPlayer, 0.5f);
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

    private static double calculateThermalResistanceBonus(float averageNutrition) {
        if (averageNutrition > 0.75f) {
            return 5.0d;
        }
        if (averageNutrition < 0.25f) {
            return -3.0d;
        }
        return 0.0d;
    }

    private static double calculateBrokenHeartResilienceMultiplier(float averageNutrition) {
        if (averageNutrition > 0.75f) {
            return 0.25d;
        }
        return 0.0d;
    }

    private static void applyAttributeModifier(
            ServerPlayer player,
            ResourceLocation attributeId,
            double amount,
            AttributeModifier.Operation operation
    ) {
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
            if (amount == 0.0d) {
                return;
            }

            AttributeModifier modifier = new AttributeModifier(modifierId, amount, operation);
            instance.addOrUpdateTransientModifier(modifier);
        } catch (Throwable t) {
            Nourished.LOGGER.debug("[Nourished] LSO attributes unavailable, skipping nutrition modifiers.", t);
        }
    }

    /**
     * Resolves {@code ModAttachments.THIRST.get()} style registration across LSO package layouts and
     * calls {@code addSaturationLevel} on the player's thirst attachment when present.
     */
    private static void tryAddThirstSaturation(ServerPlayer player, float saturation) {
        for (String className : MOD_ATTACHMENTS_CLASS_CANDIDATES) {
            try {
                Class<?> modAttachmentsClz = Class.forName(className);
                Field thirstField = modAttachmentsClz.getField("THIRST");
                Object holder = thirstField.get(null);
                Object attachmentType = unwrapAttachmentType(holder);
                if (attachmentType == null) {
                    continue;
                }
                Method getData = resolveEntityGetDataMethod(player);
                if (getData == null) {
                    return;
                }
                Object thirstData = getData.invoke(player, attachmentType);
                if (thirstData == null) {
                    return;
                }
                Method addSaturation = thirstData.getClass().getMethod("addSaturationLevel", float.class);
                addSaturation.invoke(thirstData, saturation);
                return;
            } catch (Throwable ignored) {
                // Wrong class or API shape for this LSO build — try next candidate.
            }
        }
    }

    private static Object unwrapAttachmentType(Object holder) throws Exception {
        Object cur = holder;
        for (int i = 0; i < 4; i++) {
            if (cur == null) {
                return null;
            }
            String typeName = cur.getClass().getName();
            if (typeName.contains("AttachmentType")) {
                return cur;
            }
            try {
                Method get = cur.getClass().getMethod("get");
                cur = get.invoke(cur);
            } catch (NoSuchMethodException e) {
                return cur;
            }
        }
        return cur;
    }

    private static Method resolveEntityGetDataMethod(ServerPlayer player) {
        if (entityGetDataLookupFailed) {
            return null;
        }
        if (entityGetDataMethod != null) {
            return entityGetDataMethod;
        }
        Class<?> attachmentTypeClass;
        try {
            attachmentTypeClass = Class.forName("net.neoforged.neoforge.attachment.AttachmentType");
        } catch (ClassNotFoundException e) {
            entityGetDataLookupFailed = true;
            return null;
        }
        Class<?> clazz = player.getClass();
        while (clazz != null) {
            try {
                Method m = clazz.getMethod("getData", attachmentTypeClass);
                entityGetDataMethod = m;
                return m;
            } catch (NoSuchMethodException e) {
                clazz = clazz.getSuperclass();
            }
        }
        entityGetDataLookupFailed = true;
        return null;
    }
}
