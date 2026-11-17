package dev.maire.nourished.core.micro;

import dev.marie.framework.config.FeatureFlagCache;
import dev.marie.framework.util.MarieEffectUtils;
import dev.maire.nourished.config.MicroConfig;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Applies nutrient-driven effects. Effects are short and only refreshed while their condition holds,
 * so they run out on their own instead of being removed (which could strip the same effect from
 * another source).
 */
public final class MicroEffects {

    public static final int CHECK_INTERVAL_TICKS = 80;
    private static final int REFRESH_BELOW_TICKS = 60;

    private static volatile List<MicroEffect> packEffects = List.of();
    private static volatile List<MicroEffect> configEffects = List.of();
    private static volatile List<MicroEffect> effects = List.of();

    private MicroEffects() {}

    static void set(List<MicroEffect> packs, List<MicroEffect> config) {
        packEffects = List.copyOf(packs);
        configEffects = List.copyOf(config);
        publish();
    }

    static void setConfig(List<MicroEffect> config) {
        configEffects = List.copyOf(config);
        publish();
    }

    private static void publish() {
        List<MicroEffect> merged = new java.util.ArrayList<>(packEffects);
        merged.addAll(configEffects);
        effects = List.copyOf(merged);
    }

    public static List<MicroEffect> all() {
        return effects;
    }

    static void apply(ServerPlayer player) {
        if (!FeatureFlagCache.enableEffects()) return;
        List<MicroEffect> list = effects;
        if (list.isEmpty()) return;
        boolean builtins = MicroConfig.get().builtinEffects();
        Map<String, Float> levels = MicroAttachment.get(player);
        Map<String, MicroDef> defs = MicroRegistry.server().defs();
        Map<String, Integer> held = player.getData(MicroAttachment.HELD_TICKS.get());
        Map<String, Integer> nextHeld = new HashMap<>();
        for (MicroEffect e : list) {
            if (e.builtin() && !builtins) continue;
            if (!active(e, levels, defs)) continue;
            int amplifier = e.amplifier();
            if (!e.tiers().isEmpty()) {
                int ticks = held.getOrDefault(e.id(), 0) + CHECK_INTERVAL_TICKS;
                nextHeld.put(e.id(), ticks);
                amplifier = e.amplifierAfter(ticks);
            }
            grant(player, e, amplifier);
        }
        if (!nextHeld.equals(held)) {
            player.setData(MicroAttachment.HELD_TICKS.get(), Map.copyOf(nextHeld));
        }
    }

    /** How long each tiered rule has held for this player, by rule id. */
    public static Map<String, Integer> heldTicks(ServerPlayer player) {
        return player.getData(MicroAttachment.HELD_TICKS.get());
    }

    static boolean active(MicroEffect e, Map<String, Float> levels, Map<String, MicroDef> defs) {
        if (e.nutrient().equals("all")) {
            if (defs.isEmpty()) return false;
            for (String key : defs.keySet()) {
                if (levels.getOrDefault(key, 0f) <= e.threshold()) return false;
            }
            return true;
        }
        Float level = levels.get(e.nutrient());
        return switch (e.trigger()) {
            // no penalty until the player has eaten the nutrient at least once
            case "below" -> level != null && level < e.threshold();
            case "above", "all_above" -> level != null && level > e.threshold();
            default -> false;
        };
    }

    private static void grant(ServerPlayer player, MicroEffect e, int amplifier) {
        Holder<MobEffect> holder = MarieEffectUtils.resolveEffect(e.effect()).orElse(null);
        if (holder == null) return;
        MobEffectInstance existing = player.getEffect(holder);
        if (existing != null && (existing.getAmplifier() > amplifier
                || (existing.getAmplifier() == amplifier && existing.getDuration() >= REFRESH_BELOW_TICKS))) {
            return;
        }
        player.addEffect(new MobEffectInstance(holder, e.durationTicks(), amplifier, true, false, true));
    }
}
