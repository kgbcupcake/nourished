package dev.maire.nourished.core.micro;

import com.google.gson.JsonObject;

/**
 * A status effect driven by a nutrient level. {@code nutrient} is a nutrient key, or {@code "all"}
 * with the {@code all_above} trigger. {@code builtin} marks Nourished's own bundled rules, which
 * only run when {@code builtinEffects} is on.
 */
public record MicroEffect(
        String id,
        String nutrient,
        String trigger,
        float threshold,
        String effect,
        int amplifier,
        int durationTicks,
        boolean builtin
) {

    public static MicroEffect fromJson(String id, JsonObject obj, boolean builtin) {
        if (!obj.has("effect") || !obj.has("nutrient")) {
            throw new IllegalArgumentException("needs 'effect' and 'nutrient'");
        }
        String trigger = obj.has("trigger") ? obj.get("trigger").getAsString() : "below";
        if (!trigger.equals("below") && !trigger.equals("above") && !trigger.equals("all_above")) {
            throw new IllegalArgumentException("unknown trigger '" + trigger + "'");
        }
        return new MicroEffect(
                id,
                obj.get("nutrient").getAsString(),
                trigger,
                obj.has("threshold") ? obj.get("threshold").getAsFloat() : 0.15f,
                obj.get("effect").getAsString(),
                obj.has("amplifier") ? obj.get("amplifier").getAsInt() : 0,
                obj.has("duration_ticks") ? Math.max(40, obj.get("duration_ticks").getAsInt()) : 200,
                builtin
        );
    }
}
