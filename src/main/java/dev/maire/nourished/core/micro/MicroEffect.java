package dev.maire.nourished.core.micro;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A status effect driven by a nutrient level. {@code nutrient} is a nutrient key, or {@code "all"}
 * with the {@code all_above} trigger. {@code builtin} marks Nourished's own bundled rules, which
 * only run when {@code builtinEffects} is on. {@code tiers} (below trigger only) raise the amplifier
 * the longer the condition has held.
 */
public record MicroEffect(
        String id,
        String nutrient,
        String trigger,
        float threshold,
        String effect,
        int amplifier,
        int durationTicks,
        boolean builtin,
        List<Tier> tiers
) {

    /** From {@code afterTicks} of the condition holding, use {@code amplifier}. */
    public record Tier(int afterTicks, int amplifier) {}

    public MicroEffect {
        tiers = List.copyOf(tiers);
    }

    /** Amplifier for a condition that has held for {@code heldTicks}. */
    public int amplifierAfter(int heldTicks) {
        int amp = amplifier;
        for (Tier t : tiers) {
            if (heldTicks >= t.afterTicks()) amp = t.amplifier();
        }
        return amp;
    }

    public static MicroEffect fromJson(String id, JsonObject obj, boolean builtin) {
        if (!obj.has("effect") || !obj.has("nutrient")) {
            throw new IllegalArgumentException("needs 'effect' and 'nutrient'");
        }
        String trigger = obj.has("trigger") ? obj.get("trigger").getAsString() : "below";
        if (!trigger.equals("below") && !trigger.equals("above") && !trigger.equals("all_above")) {
            throw new IllegalArgumentException("unknown trigger '" + trigger + "'");
        }
        List<Tier> tiers = new ArrayList<>();
        if (obj.has("tiers") && obj.get("tiers").isJsonArray()) {
            for (JsonElement el : obj.getAsJsonArray("tiers")) {
                if (!el.isJsonObject()) continue;
                JsonObject t = el.getAsJsonObject();
                float minutes = t.has("after_minutes") ? t.get("after_minutes").getAsFloat() : 0f;
                tiers.add(new Tier(Math.max(0, Math.round(minutes * 1200f)),
                        t.has("amplifier") ? t.get("amplifier").getAsInt() : 0));
            }
            tiers.sort(Comparator.comparingInt(Tier::afterTicks));
        }
        return new MicroEffect(
                id,
                obj.get("nutrient").getAsString(),
                trigger,
                obj.has("threshold") ? obj.get("threshold").getAsFloat() : 0.15f,
                obj.get("effect").getAsString(),
                obj.has("amplifier") ? obj.get("amplifier").getAsInt() : 0,
                obj.has("duration_ticks") ? Math.max(40, obj.get("duration_ticks").getAsInt()) : 200,
                builtin,
                tiers
        );
    }
}
