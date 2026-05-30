package dev.maire.nourished.compat.kubejs;

import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.event.EventGroupWrapper;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.api.DietProfileDefinition;
import dev.maire.nourished.api.FoodSynergyDefinition;
import dev.maire.nourished.api.NourishedAPI;
import dev.maire.nourished.api.NutrientDefinition;
import dev.maire.nourished.api.NutrientMilestoneDefinition;
import dev.maire.nourished.core.diet.DietAttachment;
import dev.maire.nourished.core.diet.DietData;
import dev.maire.nourished.core.util.NourishedValidation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

/**
 * KubeJS-facing Nourished API and event bindings.
 */
@ApiStatus.Internal
public final class NourishedKubeJSBindings {

    public static final String API_BINDING = "NourishedAPI";
    public static final String EVENTS_BINDING = "NourishedEvents";

    private NourishedKubeJSBindings() {}

    public static Object createBindingObject() {
        return new ScriptApi();
    }

    public static Object createEventsBindingObject(ScriptType type) {
        return new EventGroupWrapper(type, NourishedKubeJSEvents.GROUP);
    }

    public static final class ScriptApi {

        public void registerNutrient(JsonObject data) {
            try {
                String id = data.get("id").getAsString();
                NutrientDefinition.Builder builder = NutrientDefinition.builder(id);
                builder.displayName(data.get("displayName").getAsString());
                if (data.has("color")) builder.color(data.get("color").getAsInt());
                if (data.has("decayRate")) builder.defaultDecayRate(data.get("decayRate").getAsFloat());
                if (data.has("critical")) builder.criticalThreshold(data.get("critical").getAsFloat());
                if (data.has("low")) builder.lowThreshold(data.get("low").getAsFloat());
                if (data.has("excess")) builder.excessThreshold(data.get("excess").getAsFloat());
                NourishedAPI.registerNutrient(builder.build());
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to register nutrient", e);
            }
        }

        public void registerFoodClassification(String itemId, String nutrientKey, float amount) {
            ResourceLocation loc = ResourceLocation.parse(itemId);
            NourishedValidation.requireNonNullId(loc, "registerFoodClassification");
            NourishedValidation.requireFinite(amount, -10f, 10f, "registerFoodClassification.amount");
            NourishedAPI.registerFoodClassification(loc, nutrientKey, amount);
        }

        public void registerFoodSynergy(String foodA, String foodB, int windowSeconds, String nutrientKey, float amount) {
            try {
                if (windowSeconds <= 0 || windowSeconds >= 3600) {
                    throw new IllegalArgumentException("windowSeconds must be in (0, 3600), got: " + windowSeconds);
                }
                NourishedValidation.requireFinite(amount, -10f, 10f, "registerFoodSynergy.amount");
                String id = ResourceLocation.parse(foodA).getPath() + "_" + ResourceLocation.parse(foodB).getPath();
                FoodSynergyDefinition definition = FoodSynergyDefinition.builder(id)
                        .foodA(ResourceLocation.parse(foodA))
                        .foodB(ResourceLocation.parse(foodB))
                        .timeWindowTicks(windowSeconds * 20)
                        .bonusNutrientKey(nutrientKey)
                        .bonusAmount(amount)
                        .build();
                NourishedAPI.registerFoodSynergy(definition);
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to register food synergy", e);
            }
        }

        public void registerMilestone(JsonObject data) {
            try {
                String id = data.get("id").getAsString();
                NutrientMilestoneDefinition.Builder builder = NutrientMilestoneDefinition.builder(id)
                        .nutrientKey(data.get("nutrientKey").getAsString())
                        .cumulativeGoal(data.get("target").getAsFloat());
                if (data.has("effectId")) {
                    builder.rewardEffect(ResourceLocation.parse(data.get("effectId").getAsString()));
                }
                NourishedAPI.registerMilestone(builder.build());
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to register milestone", e);
            }
        }

        public void registerDietProfile(JsonObject data) {
            try {
                String id = data.get("id").getAsString();
                DietProfileDefinition.Builder builder = DietProfileDefinition.builder(id)
                        .displayName(data.get("displayName").getAsString());
                if (data.has("thresholds")) {
                    for (Map.Entry<String, com.google.gson.JsonElement> e : data.getAsJsonObject("thresholds").entrySet()) {
                        builder.customThreshold(e.getKey(), e.getValue().getAsFloat());
                    }
                }
                if (data.has("decayRates")) {
                    for (Map.Entry<String, com.google.gson.JsonElement> e : data.getAsJsonObject("decayRates").entrySet()) {
                        builder.customDecayRate(e.getKey(), e.getValue().getAsFloat());
                    }
                }
                NourishedAPI.registerDietProfile(builder.build());
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
}
