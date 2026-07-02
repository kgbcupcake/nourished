package dev.maire.nourished.kubejs.bindings;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.MarieAPI;
import dev.marie.framework.api.ValueDefinition;
import dev.marie.framework.core.IMarieConfig;
import dev.marie.framework.curve.math.CurveGrid;
import dev.marie.framework.curve.serialization.CurveGridJson;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import dev.maire.nourished.core.nutrition.curve.NutrientCurveDef;
import dev.maire.nourished.core.nutrition.curve.NutrientCurvePreset;
import dev.maire.nourished.core.nutrition.curve.NutrientCurveRegistry;
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

    /**
     * Registers a per-nutrient response curve from a KubeJS script. Highest
     * priority in the override stack (above config JSON and datapack JSON) —
     * see NutrientCurveRegistry's documented priority order.
     *
     * Script usage, preset form:
     * NourishedAPI.registerNutrientCurve({
     *   nutrient: "protein",
     *   preset: "DIMINISHING"
     * })
     *
     * Script usage, custom grid form:
     * NourishedAPI.registerNutrientCurve({
     *   nutrient: "fiber",
     *   preset: "custom",
     *   grid: {
     *     xCells: 5,
     *     yCells: 5,
     *     multipliers: [1.0, 1.0, ... ] // (xCells+1) * (yCells+1) values, row-major
     *   }
     * })
     *
     * @param spec the curve spec — requires "nutrient" and "preset"; "preset": "custom"
     *             additionally requires a "grid" object matching CurveGrid's JSON schema
     * @throws IllegalArgumentException if "nutrient" is missing, if "preset" is
     *         "custom" but "grid" is missing/malformed, or if a curve is already
     *         registered for this nutrient (registerExternal does not allow
     *         silent overwrite — matches the existing registerNutrient pattern
     *         of failing loud on duplicate registration)
     */
    public static void registerNutrientCurve(Map<String, Object> spec) {
        String nutrientKey = requireString(spec, "nutrient");
        String presetStr = requireString(spec, "preset");

        NutrientCurveDef def;
        if (NutrientCurveDef.CUSTOM_PRESET_ID.equalsIgnoreCase(presetStr)) {
            Object gridObj = spec.get("grid");
            if (!(gridObj instanceof Map<?, ?> gridMap)) {
                throw new IllegalArgumentException(
                        "NourishedAPI.registerNutrientCurve: 'grid' is required and must be an object when preset is 'custom'");
            }
            JsonObject gridJson = mapToJsonObject(gridMap);
            CurveGrid grid = CurveGridJson.fromJson(gridJson);
            if (grid == null) {
                throw new IllegalArgumentException(
                        "NourishedAPI.registerNutrientCurve: malformed 'grid' for nutrient '" + nutrientKey + "'");
            }
            def = NutrientCurveDef.custom(nutrientKey, grid);
        } else {
            NutrientCurvePreset preset;
            try {
                preset = NutrientCurvePreset.valueOf(presetStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "NourishedAPI.registerNutrientCurve: unknown preset '" + presetStr + "' for nutrient '" + nutrientKey + "'. "
                                + "Valid presets: FLAT, DIMINISHING, CONFIDENCE_GATED, SYNERGY, or 'custom' with a grid.");
            }
            def = NutrientCurveDef.fromPreset(nutrientKey, preset);
        }

        NutrientCurveRegistry.registerExternal(nutrientKey, def);
    }

    public static float getNutrientLevel(Player player, String nutrientKey) {
        return MarieAPI.getValueLevel(player, nutrientKey);
    }

    public static boolean isNutrientCritical(Player player, String nutrientKey) {
        float level = MarieAPI.getValueLevel(player, nutrientKey);
        float threshold = IMarieConfig.get().criticalThresholdFor(nutrientKey);
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

    private static JsonObject mapToJsonObject(Map<?, ?> map) {
        JsonObject obj = new JsonObject();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = String.valueOf(entry.getKey());
            Object value = entry.getValue();
            if (value instanceof Number n) {
                obj.addProperty(key, n);
            } else if (value instanceof List<?> list) {
                JsonArray arr = new JsonArray();
                for (Object item : list) {
                    if (item instanceof Number n) {
                        arr.add(n);
                    } else {
                        arr.add(String.valueOf(item));
                    }
                }
                obj.add(key, arr);
            } else {
                obj.addProperty(key, String.valueOf(value));
            }
        }
        return obj;
    }
}
