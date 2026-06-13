package dev.maire.nourished.kubejs.bindings;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.api.MarieAPI;
import dev.marie.MariesLib.api.ValueDefinition;
import dev.marie.MariesLib.core.IMarieLibConfig;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import dev.maire.nourished.modules.RawFood.Gut.GutHealthAttachment;
import dev.maire.nourished.modules.RawFood.Gut.GutHealthData;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Map;

@ApiStatus.Experimental
public final class NourishedKubeBindings {

    private NourishedKubeBindings() {}

    public static void registerNutrient(Map<String, Object> spec) {
        String id = requireString(spec, "id");
        ValueDefinition.Builder builder = ValueDefinition.builder(id);
        builder.displayName(requireString(spec, "displayName"));
        if (spec.containsKey("color")) {
            builder.color(asInt(spec.get("color")));
        }
        if (spec.containsKey("decayRate")) {
            builder.defaultDecayRate(asFloat(spec.get("decayRate")));
        }
        if (spec.containsKey("critical")) {
            builder.criticalThreshold(asFloat(spec.get("critical")));
        }
        if (spec.containsKey("low")) {
            builder.lowThreshold(asFloat(spec.get("low")));
        }
        if (spec.containsKey("excess")) {
            builder.excessThreshold(asFloat(spec.get("excess")));
        }
        if (spec.containsKey("beneficial")) {
            builder.beneficial(asBoolean(spec.get("beneficial")));
        } else {
            builder.beneficial(true);
        }
        MarieAPI.registerValue(builder.build());
    }

    public static float getNutrientLevel(Player player, String nutrientKey) {
        return MarieAPI.getValueLevel(player, nutrientKey);
    }

    public static boolean isNutrientCritical(Player player, String nutrientKey) {
        float level = MarieAPI.getValueLevel(player, nutrientKey);
        float threshold = IMarieLibConfig.get().criticalThresholdFor(nutrientKey);
        return level >= 0 && level < threshold;
    }

    public static float getGutHealth(Player player) {
        if (player == null) {
            return 0f;
        }
        GutHealthData gut = player.getData(GutHealthAttachment.GUT.get());
        return gut != null ? gut.getGutHealth() : 0f;
    }

    public static List<String> getNutrientKeys() {
        return NutrientRegistry.getAll().stream()
                .map(NutrientRegistry.NutrientDef::key)
                .toList();
    }

    private static String requireString(Map<String, Object> spec, String key) {
        Object value = spec.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing required field: " + key);
        }
        return String.valueOf(value);
    }

    private static float asFloat(Object value) {
        if (value instanceof Number number) {
            return number.floatValue();
        }
        return Float.parseFloat(String.valueOf(value));
    }

    private static int asInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private static boolean asBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }
}
