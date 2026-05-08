package dev.maire.nourished.compat.kubejs;

import com.google.gson.JsonObject;
import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.api.DietProfileDefinition;
import dev.maire.nourished.api.FoodSynergyDefinition;
import dev.maire.nourished.api.NourishedAPI;
import dev.maire.nourished.api.NutrientDefinition;
import dev.maire.nourished.api.NutrientMilestoneDefinition;
import dev.maire.nourished.diet.DietAttachment;
import dev.maire.nourished.diet.DietData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * KubeJS-facing Nourished API bindings.
 */
@ApiStatus.Internal
public final class NourishedKubeJSBindings {

    private NourishedKubeJSBindings() {}

    public static Object createBindingObject() {
        return new ScriptApi();
    }

    public static final class ScriptApi {

        public void registerNutrient(JsonObject data) {
            try {
                String id = data.get("id").getAsString();
                Map<String, Object> fields = new HashMap<>();
                fields.put("id", id);
                fields.put("displayName", data.get("displayName").getAsString());
                if (data.has("color")) fields.put("color", data.get("color").getAsInt());
                if (data.has("decayRate")) fields.put("defaultDecayRate", data.get("decayRate").getAsFloat());
                if (data.has("critical")) fields.put("criticalThreshold", data.get("critical").getAsFloat());
                if (data.has("low")) fields.put("lowThreshold", data.get("low").getAsFloat());
                if (data.has("excess")) fields.put("excessThreshold", data.get("excess").getAsFloat());
                NutrientDefinition definition = instantiate(NutrientDefinition.class, "dev.maire.nourished.api.NutrientDefinition$Builder", new Class<?>[]{String.class}, new Object[]{id}, fields);
                NourishedAPI.registerNutrient(definition);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to register nutrient", e);
            }
        }

        public void registerFoodClassification(String itemId, String nutrientKey, float amount) {
            NourishedAPI.registerFoodClassification(ResourceLocation.parse(itemId), nutrientKey, amount);
        }

        public void registerFoodSynergy(String foodA, String foodB, int windowSeconds, String nutrientKey, float amount) {
            try {
                String id = ResourceLocation.parse(foodA).getPath() + "_" + ResourceLocation.parse(foodB).getPath();
                Map<String, Object> fields = new HashMap<>();
                fields.put("id", id);
                fields.put("foodA", ResourceLocation.parse(foodA));
                fields.put("foodB", ResourceLocation.parse(foodB));
                fields.put("timeWindowTicks", Math.max(1, windowSeconds) * 20);
                fields.put("bonusNutrientKey", nutrientKey);
                fields.put("bonusAmount", amount);
                FoodSynergyDefinition definition = instantiate(FoodSynergyDefinition.class, "dev.maire.nourished.api.FoodSynergyDefinition$Builder", new Class<?>[]{String.class}, new Object[]{id}, fields);
                NourishedAPI.registerFoodSynergy(definition);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to register food synergy", e);
            }
        }

        public void registerMilestone(JsonObject data) {
            try {
                String id = data.get("id").getAsString();
                Map<String, Object> fields = new HashMap<>();
                fields.put("id", id);
                fields.put("nutrientKey", data.get("nutrientKey").getAsString());
                fields.put("cumulativeGoal", data.get("target").getAsFloat());
                if (data.has("effectId")) {
                    fields.put("rewardEffectId", ResourceLocation.parse(data.get("effectId").getAsString()));
                }
                NutrientMilestoneDefinition definition = instantiate(NutrientMilestoneDefinition.class, "dev.maire.nourished.api.NutrientMilestoneDefinition$Builder", new Class<?>[]{String.class}, new Object[]{id}, fields);
                NourishedAPI.registerMilestone(definition);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to register milestone", e);
            }
        }

        public void registerDietProfile(JsonObject data) {
            try {
                String id = data.get("id").getAsString();
                Map<String, Object> fields = new HashMap<>();
                fields.put("id", id);
                fields.put("displayName", data.get("displayName").getAsString());
                if (data.has("thresholds")) {
                    Map<String, Float> thresholds = new HashMap<>();
                    for (Map.Entry<String, com.google.gson.JsonElement> e : data.getAsJsonObject("thresholds").entrySet()) {
                        thresholds.put(e.getKey(), e.getValue().getAsFloat());
                    }
                    fields.put("customThresholds", thresholds);
                }
                if (data.has("decayRates")) {
                    Map<String, Float> decayRates = new HashMap<>();
                    for (Map.Entry<String, com.google.gson.JsonElement> e : data.getAsJsonObject("decayRates").entrySet()) {
                        decayRates.put(e.getKey(), e.getValue().getAsFloat());
                    }
                    fields.put("customDecayRates", decayRates);
                }
                DietProfileDefinition definition = instantiate(DietProfileDefinition.class, "dev.maire.nourished.api.DietProfileDefinition$Builder", new Class<?>[]{String.class}, new Object[]{id}, fields);
                NourishedAPI.registerDietProfile(definition);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to register diet profile", e);
            }
        }

        public float getNutrientLevel(ServerPlayer player, String nutrientKey) {
            return NourishedAPI.getNutrientLevel(player, nutrientKey);
        }

        public void setNutrientLevel(ServerPlayer player, String nutrientKey, float value) {
            DietData data = player.getData(DietAttachment.DIET.get());
            data.nutrients.put(nutrientKey, Math.max(0f, Math.min(1f, value)));
            player.setData(DietAttachment.DIET.get(), data);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T instantiate(
            Class<T> targetClass,
            String builderClassName,
            Class<?>[] builderCtorArgTypes,
            Object[] builderCtorArgs,
            Map<String, Object> builderFieldValues
    ) throws Exception {
        Class<?> builderClass = Class.forName(builderClassName);
        Constructor<?> builderCtor = builderClass.getDeclaredConstructor(builderCtorArgTypes);
        builderCtor.setAccessible(true);
        Object builder = builderCtor.newInstance(builderCtorArgs);

        for (Map.Entry<String, Object> e : builderFieldValues.entrySet()) {
            Field field = builderClass.getDeclaredField(e.getKey());
            field.setAccessible(true);
            field.set(builder, e.getValue());
        }

        Constructor<T> targetCtor = targetClass.getDeclaredConstructor(builderClass);
        targetCtor.setAccessible(true);
        return targetCtor.newInstance(builder);
    }
}
