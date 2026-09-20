package dev.maire.nourished.client.colors;

import dev.maire.nourished.core.Nourished;
import dev.marie.framework.color.ColorRegistry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One-time cleanup of {@code colors.json} entries under keys {@link NourishedColors} no longer registers. A
 * key that was simply renamed to the same role keeps the player's override (copied to the new key before the old
 * one is dropped); a key whose color is now derived from a nutrient or another color has nothing to carry it to,
 * so its override is dropped. Runs on every load but only does anything once: it acts only on entries that are
 * still there, and reports once when it changes something. Pure key strings — no UI classes.
 */
public final class RetiredColorKeys {

    private static final String PREFIX = Nourished.MODID + ":";

    /** Old key path -> the key path for the same role. */
    private static final Map<String, String> RENAMED = new LinkedHashMap<>();
    /** Old key paths whose colors are now derived, so their overrides are simply dropped. */
    private static final List<String> DROPPED = List.of(
            "hud.bar_low", "hud.bar_critical", "hud.pct_good", "hud.pct_low", "hud.pct_critical", "hud.flash",
            "hud.notification_text", "diet.right_panel_background", "diet.row_background", "diet.border_light",
            "diet.segment_empty", "diet.flash", "diet.legend_text", "diet.legend_low",
            "diet.good", "diet.warn", "diet.bad");

    static {
        RENAMED.put("hud.calorie", "calorie.value");
        RENAMED.put("hud.label", "text.nutrient_hud");
        RENAMED.put("diet.panel_background", "panel.diet");
        RENAMED.put("hud.bar_background", "bar.track");
        RENAMED.put("diet.text", "text");
        RENAMED.put("diet.muted_text", "text.muted");
        RENAMED.put("diet.header_text", "text.header");
        RENAMED.put("diet.border", "border");
        RENAMED.put("diet.divider", "divider");
        RENAMED.put("diet.toggle_on", "toggle.on");
        RENAMED.put("diet.toggle_off", "toggle.off");
        RENAMED.put("diet.toggle_border", "toggle.border");
        RENAMED.put("diet.toggle_housing", "toggle.housing");
        RENAMED.put("diet.toggle_lever", "toggle.lever");
        RENAMED.put("edit.accent", "edit.outline");
        RENAMED.put("edit.label_text", "edit.label.text");
        RENAMED.put("edit.label_shadow", "edit.label.shadow");
    }

    private RetiredColorKeys() {}

    /** Migrates renamed overrides, drops the rest of the retired ones, and saves {@link ColorRegistry} if anything changed. */
    public static void migrate() {
        int copied = 0;
        int dropped = 0;
        for (Map.Entry<String, String> rename : RENAMED.entrySet()) {
            String oldKey = PREFIX + rename.getKey();
            var old = ColorRegistry.getArgb(oldKey);
            if (old.isEmpty()) {
                continue;
            }
            String newKey = PREFIX + rename.getValue();
            if (ColorRegistry.getArgb(newKey).isEmpty()) {
                ColorRegistry.setArgb(newKey, old.get());
                copied++;
            }
            ColorRegistry.remove(oldKey);
            dropped++;
        }
        for (String path : DROPPED) {
            if (ColorRegistry.getArgb(PREFIX + path).isPresent()) {
                ColorRegistry.remove(PREFIX + path);
                dropped++;
            }
        }
        if (copied > 0 || dropped > 0) {
            ColorRegistry.save();
            Nourished.LOGGER.info("[Nourished] colors.json cleanup: moved {} override(s) to renamed colors, removed {} retired entr{}",
                    copied, dropped, dropped == 1 ? "y" : "ies");
        }
    }
}
