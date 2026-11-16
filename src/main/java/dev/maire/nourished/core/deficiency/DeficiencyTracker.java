package dev.maire.nourished.core.deficiency;

import dev.marie.framework.config.FeatureFlagCache;
import dev.marie.framework.tracking.TrackingAttachment;
import dev.marie.framework.tracking.TrackingData;
import dev.marie.framework.util.MarieEffectUtils;
import dev.maire.nourished.api.NourishedAPI;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.core.NourishedKubeIntegration;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import dev.maire.nourished.modules.activity_driven_nutrient.handler.ActivityEffectLog;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Builds deficiency pressure while a group sits below its detect threshold and drains it once the
 * group is back above its recover threshold. Between the two, pressure holds.
 */
public final class DeficiencyTracker {

    public static final String ANY = "any";
    private static final int REFRESH_BELOW_TICKS = 60;

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        DeficiencySettings s = DeficiencyConfig.get();
        if (player.level().getGameTime() % s.checkIntervalTicks() != 0) return;
        boolean active = DeficiencyPenalties.active(player, s);
        if (active) {
            update(player, s);
            applyTierEffects(player, s);
        }
        DeficiencyPenalties.applyAttributes(player, active ? overallTier(player) : 0, s);
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!event.isEndConquered() && DeficiencyConfig.get().resetOnDeath()) clear(player, null);
        DeficiencyNetworking.sync(player, false);
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) DeficiencyNetworking.sync(player, false);
    }

    @SubscribeEvent
    public void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) DeficiencyNetworking.sync(player, false);
    }

    @SubscribeEvent
    public void onDatapackSync(OnDatapackSyncEvent event) {
        event.getRelevantPlayers().forEach(p -> DeficiencyNetworking.sync(p, false));
    }

    private static void update(ServerPlayer player, DeficiencySettings s) {
        if (!TrackingAttachment.isRegistered()) return;
        TrackingData diet = TrackingAttachment.getData(player);
        Map<String, Integer> current = DeficiencyAttachment.get(player);
        Map<String, Integer> next = new HashMap<>();
        boolean kube = NourishedKubeIntegration.isPresent();
        for (NutrientRegistry.NutrientDef def : NutrientRegistry.getAll()) {
            String key = def.key();
            int pressure = current.getOrDefault(key, 0);
            Float value = diet.values.get(key);
            if (DeficiencyCalories.KEY.equals(key)) continue;
            if (tracked(s, def) && value != null) {
                pressure = advance(player, s, key, value, pressure, groupState(s, def, value), kube);
            } else {
                pressure = 0;
            }
            if (pressure > 0) next.put(key, pressure);
        }
        if (s.calories().enabled() && NourishedConfig.get().enableCalorieTracking()) {
            float today = NourishedAPI.getTodayCalories(player);
            int state = DeficiencyCalories.state(s.calories(), NourishedAPI.getYesterdayCalories(player), today, diet.maxTotal);
            float fraction = diet.maxTotal > 0 ? today / diet.maxTotal : 0f;
            int pressure = advance(player, s, DeficiencyCalories.KEY, fraction,
                    current.getOrDefault(DeficiencyCalories.KEY, 0), state, kube);
            if (pressure > 0) next.put(DeficiencyCalories.KEY, pressure);
        }
        if (next.equals(current)) return;
        store(player, current, next, s);
    }

    /** +1 below the detect threshold, -1 at or above the recover threshold, 0 in between. */
    public static int groupState(DeficiencySettings s, NutrientRegistry.NutrientDef def, float value) {
        if (value < detect(s, def)) return 1;
        return value >= recover(s, def) ? -1 : 0;
    }

    private static int advance(ServerPlayer player, DeficiencySettings s, String key, float value, int pressure,
                               int state, boolean kube) {
        int interval = s.checkIntervalTicks();
        int delta = state > 0 ? interval : state < 0 ? -Math.round(interval * s.recoverySpeed()) : 0;
        if (delta != 0 && kube) {
            delta = NourishedKubeIntegration.fireDeficiencyPressure(player, key, value, pressure, delta);
        }
        int after = Math.max(0, Math.min(s.maxPressure(), pressure + delta));
        return kube ? steerTier(player, s, key, pressure, after) : after;
    }

    /** Lets scripts redirect or cancel a natural tier change by moving pressure to the tier they chose. */
    private static int steerTier(ServerPlayer player, DeficiencySettings s, String key, int before, int after) {
        int oldTier = s.tierFor(before);
        int newTier = s.tierFor(after);
        if (oldTier == newTier) return after;
        int wanted = Math.max(0, Math.min(s.tiers().size(),
                NourishedKubeIntegration.fireDeficiencyTierChanged(player, key, oldTier, newTier)));
        if (wanted == newTier) return after;
        if (wanted > newTier) return s.tiers().get(wanted - 1).afterTicks();
        return wanted < s.tiers().size() ? Math.max(0, s.tiers().get(wanted).afterTicks() - 1) : after;
    }

    private static void store(ServerPlayer player, Map<String, Integer> current, Map<String, Integer> next,
                              DeficiencySettings s) {
        DeficiencyAttachment.set(player, next);
        int before = overallTier(current, s);
        int after = overallTier(next, s);
        if (before != after) onTierChanged(player, before, after, next, s);
        DeficiencyNetworking.sync(player, !tiers(current, s).equals(tiers(next, s)));
    }

    private static Map<String, Integer> tiers(Map<String, Integer> pressure, DeficiencySettings s) {
        Map<String, Integer> out = new HashMap<>();
        pressure.forEach((k, p) -> {
            int t = s.tierFor(p);
            if (t > 0) out.put(k, t);
        });
        return out;
    }

    public static boolean tracked(DeficiencySettings s, NutrientRegistry.NutrientDef def) {
        if (!def.beneficial() || s.exemptGroups().contains(def.key())) return false;
        return s.requiredGroups().isEmpty() || s.requiredGroups().contains(def.key());
    }

    public static float detect(DeficiencySettings s, NutrientRegistry.NutrientDef def) {
        DeficiencySettings.GroupThresholds g = s.groupThresholds().get(def.key());
        if (g != null && g.detect() != null) return g.detect();
        return s.detectThreshold() != null ? s.detectThreshold() : def.criticalThreshold();
    }

    public static float recover(DeficiencySettings s, NutrientRegistry.NutrientDef def) {
        DeficiencySettings.GroupThresholds g = s.groupThresholds().get(def.key());
        float r;
        if (g != null && g.recover() != null) r = g.recover();
        else r = s.recoverThreshold() != null ? s.recoverThreshold() : def.lowThreshold();
        return Math.max(r, detect(s, def));
    }

    private static void onTierChanged(ServerPlayer player, int before, int after,
                                      Map<String, Integer> pressure, DeficiencySettings s) {
        StringBuilder groups = new StringBuilder();
        pressure.forEach((k, p) -> {
            int t = s.tierFor(p);
            if (t > 0) groups.append(groups.isEmpty() ? "" : ", ").append(k).append(" L").append(t);
        });
        String desc = after > before
                ? "Deficiency L" + before + " -> L" + after + (groups.isEmpty() ? "" : " (" + groups + ")")
                : "Recovering L" + before + " -> L" + after;
        ActivityEffectLog.record("deficiency", player.getGameProfile().getName(), desc);
    }

    private static void applyTierEffects(ServerPlayer player, DeficiencySettings s) {
        if (!FeatureFlagCache.enableEffects()) return;
        int tier = overallTier(player);
        if (tier <= 0) return;
        for (DeficiencySettings.TierEffect e : s.tiers().get(tier - 1).effects()) {
            Holder<MobEffect> holder = MarieEffectUtils.resolveEffect(e.effect()).orElse(null);
            if (holder == null) continue;
            MobEffectInstance existing = player.getEffect(holder);
            if (existing != null && (existing.getAmplifier() > e.amplifier()
                    || (existing.getAmplifier() == e.amplifier() && existing.getDuration() >= REFRESH_BELOW_TICKS))) {
                continue;
            }
            player.addEffect(new MobEffectInstance(holder, e.durationTicks(), e.amplifier(), true, false, true));
        }
    }

    public static int tier(ServerPlayer player, String group) {
        DeficiencySettings s = DeficiencyConfig.get();
        if (!s.enabled()) return 0;
        if (group == null || ANY.equals(group) || "all".equals(group)) return overallTier(player);
        return s.tierFor(DeficiencyAttachment.get(player).getOrDefault(group, 0));
    }

    public static int overallTier(ServerPlayer player) {
        DeficiencySettings s = DeficiencyConfig.get();
        return s.enabled() ? overallTier(DeficiencyAttachment.get(player), s) : 0;
    }

    private static int overallTier(Map<String, Integer> pressure, DeficiencySettings s) {
        return s.overallTier(pressure);
    }

    public static float activityMultiplier(ServerPlayer player) {
        DeficiencySettings s = DeficiencyConfig.get();
        if (!DeficiencyPenalties.active(player, s)) return 1f;
        return s.activityMultiplier(overallTier(player));
    }

    public static Map<String, Integer> pressures(ServerPlayer player) {
        return DeficiencyAttachment.get(player);
    }

    public static int pressure(ServerPlayer player, String group) {
        return DeficiencyAttachment.get(player).getOrDefault(group, 0);
    }

    /** Sets a group's pressure directly; clamped to the configured maximum. */
    public static void setPressure(ServerPlayer player, String group, int ticks) {
        DeficiencySettings s = DeficiencyConfig.get();
        Map<String, Integer> current = DeficiencyAttachment.get(player);
        Map<String, Integer> next = new HashMap<>(current);
        int clamped = Math.max(0, Math.min(s.maxPressure(), ticks));
        if (clamped == 0) next.remove(group);
        else next.put(group, clamped);
        store(player, current, next, s);
    }

    /** Pressure that puts a group exactly at the start of {@code tier}. */
    public static int pressureForTier(int tier) {
        DeficiencySettings s = DeficiencyConfig.get();
        if (tier <= 0 || s.tiers().isEmpty()) return 0;
        return s.tiers().get(Math.min(tier, s.tiers().size()) - 1).afterTicks();
    }

    /** Clears one group, or every group when {@code group} is null. */
    public static void clear(ServerPlayer player, String group) {
        if (group == null) {
            DeficiencySettings s = DeficiencyConfig.get();
            store(player, DeficiencyAttachment.get(player), Map.of(), s);
        } else {
            setPressure(player, group, 0);
        }
    }
}
