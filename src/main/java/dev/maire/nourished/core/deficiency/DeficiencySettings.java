package dev.maire.nourished.core.deficiency;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.marie.framework.util.MarieJsonUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Parsed {@code deficiency.json}. Thresholds left null fall back to the group's own critical/low
 * thresholds from nutrients.json.
 */
public record DeficiencySettings(
        boolean enabled,
        int checkIntervalTicks,
        Float detectThreshold,
        Float recoverThreshold,
        Map<String, GroupThresholds> groupThresholds,
        float recoverySpeed,
        Set<String> exemptGroups,
        Set<String> requiredGroups,
        boolean resetOnDeath,
        boolean affectPeaceful,
        int bufferTicks,
        List<Tier> tiers,
        boolean notifyOnWorsen,
        boolean notifyOnRecover,
        CalorieRule calories
) {

    /** Optional calorie input: builds while yesterday and today are both under {@code minFraction} of the daily max. */
    public record CalorieRule(boolean enabled, float minFraction, float recoverFraction) {}

    public record GroupThresholds(Float detect, Float recover) {}

    public record TierEffect(String effect, int amplifier, int durationTicks) {}

    /** {@code operation}: add_value, add_multiplied_base or add_multiplied_total. */
    public record AttributePenalty(double amount, String operation) {}

    public record Tier(int afterTicks, float activityMultiplier, float regenMultiplier, float saturationMultiplier,
                       Map<String, AttributePenalty> attributes, List<TierEffect> effects) {
        public Tier {
            attributes = Map.copyOf(attributes);
            effects = List.copyOf(effects);
        }
    }

    public DeficiencySettings {
        groupThresholds = Map.copyOf(groupThresholds);
        exemptGroups = Set.copyOf(exemptGroups);
        requiredGroups = Set.copyOf(requiredGroups);
        tiers = List.copyOf(tiers);
    }

    public int maxPressure() {
        return tiers.isEmpty() ? 0 : tiers.get(tiers.size() - 1).afterTicks() + bufferTicks;
    }

    /** 0 when below the first tier, otherwise 1-based tier index. */
    public int tierFor(int pressure) {
        int tier = 0;
        for (int i = 0; i < tiers.size(); i++) {
            if (pressure >= tiers.get(i).afterTicks()) tier = i + 1;
        }
        return tier;
    }

    /** Highest tier across all groups. */
    public int overallTier(java.util.Map<String, Integer> pressure) {
        int tier = 0;
        for (int p : pressure.values()) tier = Math.max(tier, tierFor(p));
        return tier;
    }

    public float activityMultiplier(int tier) {
        Tier t = tier(tier);
        return t == null ? 1f : t.activityMultiplier();
    }

    /** The 1-based tier's settings, or null for tier 0 / out of range. */
    public Tier tier(int tier) {
        return tier <= 0 || tier > tiers.size() ? null : tiers.get(tier - 1);
    }

    public static DeficiencySettings defaults() {
        return fromJson(defaultJson());
    }

    public static JsonObject defaultJson() {
        JsonObject root = new JsonObject();
        root.addProperty("enabled", true);
        root.addProperty("check_interval_ticks", 80);
        root.addProperty("recovery_speed", 2.0);
        root.add("exempt_groups", new JsonArray());
        root.add("required_groups", new JsonArray());
        root.add("group_thresholds", new JsonObject());
        root.addProperty("reset_on_death", false);
        root.addProperty("affect_peaceful", true);
        root.addProperty("buffer_minutes", 20);
        root.addProperty("notify_on_worsen", true);
        root.addProperty("notify_on_recover", true);
        JsonObject calories = new JsonObject();
        calories.addProperty("enabled", false);
        calories.addProperty("min_fraction", 0.5);
        calories.addProperty("recover_fraction", 0.8);
        root.add("calories", calories);
        JsonArray tiers = new JsonArray();
        tiers.add(tier(20, 1.15f, 0.9f, 1.0f, attrs(), effect(MALNOURISHED, 0)));
        tiers.add(tier(60, 1.35f, 0.75f, 0.85f,
                attrs(MOVEMENT_SPEED, -0.05, BLOCK_BREAK_SPEED, -0.15), effect(MALNOURISHED, 1)));
        tiers.add(tier(120, 1.6f, 0.5f, 0.7f,
                attrs(MOVEMENT_SPEED, -0.12, BLOCK_BREAK_SPEED, -0.3, ATTACK_DAMAGE, -0.15), effect(MALNOURISHED, 2)));
        root.add("tiers", tiers);
        return root;
    }

    private static final String MALNOURISHED = "nourished:malnourished";
    private static final String MOVEMENT_SPEED = "minecraft:generic.movement_speed";
    private static final String BLOCK_BREAK_SPEED = "minecraft:player.block_break_speed";
    private static final String ATTACK_DAMAGE = "minecraft:generic.attack_damage";

    private static JsonObject attrs(Object... pairs) {
        JsonObject o = new JsonObject();
        for (int i = 0; i + 1 < pairs.length; i += 2) o.addProperty((String) pairs[i], (Double) pairs[i + 1]);
        return o;
    }

    private static JsonObject tier(int minutes, float multiplier, float regen, float saturation,
                                   JsonObject attributes, JsonObject... effects) {
        JsonObject t = new JsonObject();
        t.addProperty("after_minutes", minutes);
        t.addProperty("activity_multiplier", multiplier);
        t.addProperty("regen_multiplier", regen);
        t.addProperty("saturation_multiplier", saturation);
        t.add("attributes", attributes);
        JsonArray arr = new JsonArray();
        for (JsonObject e : effects) arr.add(e);
        t.add("effects", arr);
        return t;
    }

    private static JsonObject effect(String id, int amplifier) {
        JsonObject e = new JsonObject();
        e.addProperty("effect", id);
        e.addProperty("amplifier", amplifier);
        e.addProperty("duration_ticks", 200);
        return e;
    }

    public static DeficiencySettings fromJson(JsonObject root) {
        Map<String, GroupThresholds> groups = new HashMap<>();
        if (root.has("group_thresholds") && root.get("group_thresholds").isJsonObject()) {
            for (var e : root.getAsJsonObject("group_thresholds").entrySet()) {
                if (!e.getValue().isJsonObject()) continue;
                JsonObject g = e.getValue().getAsJsonObject();
                groups.put(e.getKey(), new GroupThresholds(optFloat(g, "detect_threshold"), optFloat(g, "recover_threshold")));
            }
        }
        List<Tier> tiers = new ArrayList<>();
        if (root.has("tiers") && root.get("tiers").isJsonArray()) {
            for (JsonElement el : root.getAsJsonArray("tiers")) {
                if (el.isJsonObject()) tiers.add(parseTier(el.getAsJsonObject()));
            }
            tiers.sort(Comparator.comparingInt(Tier::afterTicks));
        }
        return new DeficiencySettings(
                MarieJsonUtils.getOptionalBoolean(root, "enabled", true),
                Math.max(20, (int) MarieJsonUtils.getOptionalDouble(root, "check_interval_ticks", 80)),
                optFloat(root, "detect_threshold"),
                optFloat(root, "recover_threshold"),
                groups,
                (float) Math.max(0.0, MarieJsonUtils.getOptionalDouble(root, "recovery_speed", 2.0)),
                stringSet(root, "exempt_groups"),
                stringSet(root, "required_groups"),
                MarieJsonUtils.getOptionalBoolean(root, "reset_on_death", false),
                MarieJsonUtils.getOptionalBoolean(root, "affect_peaceful", true),
                minutesToTicks(MarieJsonUtils.getOptionalDouble(root, "buffer_minutes", 20)),
                tiers,
                MarieJsonUtils.getOptionalBoolean(root, "notify_on_worsen", true),
                MarieJsonUtils.getOptionalBoolean(root, "notify_on_recover", true),
                parseCalories(root)
        );
    }

    private static CalorieRule parseCalories(JsonObject root) {
        if (!root.has("calories") || !root.get("calories").isJsonObject()) return new CalorieRule(false, 0.5f, 0.8f);
        JsonObject c = root.getAsJsonObject("calories");
        float min = (float) Math.max(0.0, MarieJsonUtils.getOptionalDouble(c, "min_fraction", 0.5));
        float recover = (float) Math.max(min, MarieJsonUtils.getOptionalDouble(c, "recover_fraction", 0.8));
        return new CalorieRule(MarieJsonUtils.getOptionalBoolean(c, "enabled", false), min, recover);
    }

    private static Tier parseTier(JsonObject t) {
        List<TierEffect> effects = new ArrayList<>();
        if (t.has("effects") && t.get("effects").isJsonArray()) {
            for (JsonElement el : t.getAsJsonArray("effects")) {
                if (!el.isJsonObject()) continue;
                JsonObject e = el.getAsJsonObject();
                if (!e.has("effect")) continue;
                effects.add(new TierEffect(
                        e.get("effect").getAsString(),
                        e.has("amplifier") ? e.get("amplifier").getAsInt() : 0,
                        e.has("duration_ticks") ? Math.max(40, e.get("duration_ticks").getAsInt()) : 200));
            }
        }
        Map<String, AttributePenalty> attributes = new HashMap<>();
        if (t.has("attributes") && t.get("attributes").isJsonObject()) {
            for (var e : t.getAsJsonObject("attributes").entrySet()) {
                JsonElement v = e.getValue();
                if (v.isJsonPrimitive()) {
                    attributes.put(e.getKey(), new AttributePenalty(v.getAsDouble(), "add_multiplied_total"));
                } else if (v.isJsonObject() && v.getAsJsonObject().has("amount")) {
                    JsonObject o = v.getAsJsonObject();
                    attributes.put(e.getKey(), new AttributePenalty(o.get("amount").getAsDouble(),
                            o.has("operation") ? o.get("operation").getAsString() : "add_multiplied_total"));
                }
            }
        }
        return new Tier(
                minutesToTicks(MarieJsonUtils.getOptionalDouble(t, "after_minutes", 0)),
                (float) Math.max(0.0, MarieJsonUtils.getOptionalDouble(t, "activity_multiplier", 1.0)),
                (float) Math.max(0.0, MarieJsonUtils.getOptionalDouble(t, "regen_multiplier", 1.0)),
                (float) Math.max(0.0, MarieJsonUtils.getOptionalDouble(t, "saturation_multiplier", 1.0)),
                attributes,
                effects);
    }

    private static int minutesToTicks(double minutes) {
        return (int) Math.max(0, Math.round(minutes * 1200.0));
    }

    private static Float optFloat(JsonObject obj, String key) {
        return obj.has(key) && obj.get(key).isJsonPrimitive() ? obj.get(key).getAsFloat() : null;
    }

    private static Set<String> stringSet(JsonObject obj, String key) {
        Set<String> out = new TreeSet<>();
        if (obj.has(key) && obj.get(key).isJsonArray()) {
            for (JsonElement el : obj.getAsJsonArray(key)) {
                if (el.isJsonPrimitive()) out.add(el.getAsString());
            }
        }
        return out;
    }
}
