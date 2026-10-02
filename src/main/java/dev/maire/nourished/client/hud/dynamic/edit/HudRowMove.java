package dev.maire.nourished.client.hud.dynamic.edit;

import dev.maire.nourished.client.UiStatePersistence;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import dev.marie.framework.ui.component.ComponentState;
import dev.marie.framework.ui.geometry.Bounds;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * "Move Nutrient" for the Nutrient HUD: every nutrient row sits inside the one HUD box, so a single
 * row can't be grabbed on its own. Instead the Behavior tab picks a nutrient and switches on "Move
 * Row", and a drag anywhere inside the box then moves that whole row (icon, name, bar and percentage)
 * by its own offset — like the Diet Screen's Intake Breakdown rows, each row keeps its own position.
 *
 * <p>Offsets are screen pixels, applied on top of the panel-wide move offsets, and persisted per row
 * under {@code nourished.hud.bar.<key>#rowOffset} in {@link UiStatePersistence}; a drag updates them in
 * memory and {@link #commit} saves once on release. The picker selection and toggle are edit-session
 * state only.
 */
public final class HudRowMove {

    private static final String SUFFIX = "#rowOffset";

    /** In-memory offsets, ahead of what's saved (a drag updates these every frame). */
    private static final Map<String, int[]> LIVE = new HashMap<>();

    private static int selectedIndex;
    private static boolean enabled;

    private HudRowMove() {}

    // ── Picker / toggle (Behavior tab) ───────────────────────────────────────

    public static String[] nutrientLabels() {
        List<String> keys = NutrientRegistry.getKeys();
        String[] labels = new String[keys.size()];
        for (int i = 0; i < keys.size(); i++) {
            labels[i] = NutrientRegistry.getLabel(keys.get(i));
        }
        return labels;
    }

    public static int selectedIndex() {
        return selectedIndex;
    }

    public static void setSelectedIndex(int index) {
        selectedIndex = index;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    /** The nutrient "Move Row" currently drags, or {@code null} if it's off or nothing is registered. */
    public static String activeKey() {
        if (!enabled) {
            return null;
        }
        List<String> keys = NutrientRegistry.getKeys();
        return selectedIndex >= 0 && selectedIndex < keys.size() ? keys.get(selectedIndex) : null;
    }

    /** "Reset Row Positions": every row back to its normal place. */
    public static void resetAll() {
        for (String key : NutrientRegistry.getKeys()) {
            LIVE.remove(key);
            UiStatePersistence.get().remove(id(key));
        }
    }

    // ── Offsets ──────────────────────────────────────────────────────────────

    public static int offsetX(String key) {
        return offset(key)[0];
    }

    public static int offsetY(String key) {
        return offset(key)[1];
    }

    private static int[] offset(String key) {
        return LIVE.computeIfAbsent(key, k -> UiStatePersistence.get().load(id(k))
                .map(s -> new int[]{s.x(), s.y()})
                .orElseGet(() -> new int[]{0, 0}));
    }

    /**
     * Sets {@code key}'s offset in memory, clamped so what its row drew last frame stays inside
     * {@code box}; {@link #commit} saves it.
     */
    public static void setOffset(String key, int x, int y, Bounds box) {
        Bounds drawn = HudDrawnExtents.row(key);
        if (drawn != null && box != null) {
            // The row's extent with its current offset taken back out, i.e. where it sits unmoved.
            int baseX = drawn.x() - offsetX(key);
            int baseY = drawn.y() - offsetY(key);
            x = clamp(x, box.x() - baseX, box.x() + box.width() - baseX - drawn.width());
            y = clamp(y, box.y() - baseY, box.y() + box.height() - baseY - drawn.height());
        }
        LIVE.put(key, new int[]{x, y});
    }

    public static void commit(String key) {
        int[] o = offset(key);
        UiStatePersistence.get().save(id(key), new ComponentState(o[0], o[1], 0, 0, false, false, false, 0));
    }

    private static int clamp(int value, int min, int max) {
        // A row wider/taller than the box pins to the near edge rather than producing an inverted range.
        return Math.max(min, Math.min(Math.max(min, max), value));
    }

    private static String id(String key) {
        return "nourished.hud.bar." + key + SUFFIX;
    }
}
