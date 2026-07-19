package dev.maire.nourished.core.lifecycle;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.tooltips.TooltipColorRegistry;
import dev.marie.framework.tooltips.TooltipMessageRegistry;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.nutrition.NutrientRegistry;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Seeds {@code config/nourished/tooltips/tooltip_colors.json} and {@code tooltip_messages.json}
 * with Nourished's real current values, via MarieLib's {@code seedDefaultsIfAbsent}. Those files
 * are never overwritten once present, so this only affects first-run generation.
 */
@ApiStatus.Internal
final class NourishedTooltipDefaults {

    private static final String EXCLUDED_LANG_KEY = "nourished.tooltip.excluded";
    private static final String EXCLUDED_RESOURCE_PATH = "/assets/" + Nourished.MODID + "/lang/en_us.json";
    private static final String FALLBACK_EXCLUDED = "Excluded from nutrition tracking";

    private NourishedTooltipDefaults() {}

    static void seed() {
        TooltipColorRegistry.seedDefaultsIfAbsent(Nourished.MODID, nutrientColorDefaults());
        TooltipMessageRegistry.seedDefaultsIfAbsent(Nourished.MODID, Map.of("excluded", excludedMessageDefault()));
    }

    private static Map<String, String> nutrientColorDefaults() {
        Map<String, String> defaults = new HashMap<>();
        for (NutrientRegistry.NutrientDef def : NutrientRegistry.getAll()) {
            defaults.put(def.key(), String.format("0x%08X", def.color()));
        }
        return defaults;
    }

    private static String excludedMessageDefault() {
        try (InputStream in = NourishedTooltipDefaults.class.getResourceAsStream(EXCLUDED_RESOURCE_PATH)) {
            if (in == null) {
                Nourished.LOGGER.warn("[NourishedTooltipDefaults] Missing bundled en_us.json, using fallback excluded message");
                return FALLBACK_EXCLUDED;
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                JsonObject lang = new Gson().fromJson(reader, JsonObject.class);
                if (lang != null && lang.has(EXCLUDED_LANG_KEY)) {
                    return lang.get(EXCLUDED_LANG_KEY).getAsString();
                }
            }
        } catch (IOException e) {
            Nourished.LOGGER.warn("[NourishedTooltipDefaults] Failed to read en_us.json for excluded message", e);
        }
        return FALLBACK_EXCLUDED;
    }
}
