package dev.maire.nourished.client.deficiency;

import dev.marie.framework.color.MarieColors;
import dev.marie.framework.notification.NotificationRequest;
import dev.marie.framework.notification.TextSegment;
import dev.marie.framework.ui.api.MarieNotifications;
import dev.maire.nourished.client.colors.NourishedColors;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.core.deficiency.DeficiencyCalories;
import dev.maire.nourished.core.deficiency.DeficiencyConfig;
import dev.maire.nourished.core.deficiency.DeficiencySettings;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Client copy of the local player's deficiency pressure, plus the tier-change toasts. */
public final class DeficiencyClient {

    private static final Object MERGE_KEY = "nourished.deficiency";
    private static final int DURATION_TICKS = 120;
    private static final int MUTED = 0xFF9AA4AE;

    private static volatile Map<String, Integer> pressure = Map.of();

    private DeficiencyClient() {}

    public static Map<String, Integer> pressure() {
        return pressure;
    }

    public static int pressure(String group) {
        return pressure.getOrDefault(group, 0);
    }

    public static int tier(String group) {
        return DeficiencyConfig.get().tierFor(pressure(group));
    }

    public static int overallTier() {
        return DeficiencyConfig.get().overallTier(pressure);
    }

    public static int tierColor(int tier) {
        return MarieColors.resolveColor(switch (tier) {
            case 0 -> NourishedColors.DEFICIENCY_OK;
            case 1 -> NourishedColors.DEFICIENCY_L1;
            case 2 -> NourishedColors.DEFICIENCY_L2;
            default -> NourishedColors.DEFICIENCY_L3;
        });
    }

    public static void accept(Map<String, Integer> next, boolean announce) {
        Map<String, Integer> before = pressure;
        pressure = Map.copyOf(next);
        if (announce) announce(before, pressure);
    }

    public static void clear() {
        pressure = Map.of();
    }

    private static void announce(Map<String, Integer> before, Map<String, Integer> after) {
        DeficiencySettings s = DeficiencyConfig.get();
        if (!NourishedConfig.get().enableToasts()) return;
        boolean worse = false;
        for (var e : after.entrySet()) {
            if (s.tierFor(e.getValue()) > s.tierFor(before.getOrDefault(e.getKey(), 0))) worse = true;
        }
        int was = s.overallTier(before);
        int now = s.overallTier(after);
        List<List<TextSegment>> lines = new ArrayList<>();
        if (worse && s.notifyOnWorsen()) {
            lines.add(List.of(new TextSegment(
                    Component.translatable("nourished.deficiency.toast.level" + Math.min(3, now)).getString(),
                    tierColor(now))));
            String groups = deficientGroups(after, s);
            if (!groups.isEmpty()) {
                lines.add(List.of(new TextSegment(
                        Component.translatable("nourished.deficiency.toast.groups", groups).getString(), MUTED)));
            }
        } else if (now < was && s.notifyOnRecover()) {
            String key = now == 0 ? "nourished.deficiency.toast.cleared" : "nourished.deficiency.toast.recovering";
            lines.add(List.of(new TextSegment(Component.translatable(key, now).getString(), tierColor(now))));
        }
        if (lines.isEmpty()) return;
        MarieNotifications.show(NotificationRequest.builder(lines, DURATION_TICKS)
                .mergeKey(MERGE_KEY)
                .mergeWindowTicks(DURATION_TICKS)
                .mergeFunction((oldContent, newContent) -> newContent)
                .build());
    }

    private static String deficientGroups(Map<String, Integer> pressure, DeficiencySettings s) {
        StringBuilder out = new StringBuilder();
        for (NutrientRegistry.NutrientDef def : NutrientRegistry.getAll()) {
            if (s.tierFor(pressure.getOrDefault(def.key(), 0)) == 0) continue;
            if (!out.isEmpty()) out.append(", ");
            out.append(def.displayName());
        }
        if (s.tierFor(pressure.getOrDefault(DeficiencyCalories.KEY, 0)) > 0) {
            if (!out.isEmpty()) out.append(", ");
            out.append(Component.translatable("nourished.deficiency.calories").getString());
        }
        return out.toString();
    }
}
