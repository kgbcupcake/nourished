package dev.maire.nourished.client.colors;

import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import dev.maire.nourished.modules.activity_driven_nutrient.core.ActivityDrivenNutrientRegistry;
import dev.marie.framework.color.ColorDefinition;
import dev.marie.framework.color.ColorDefinitionRegistry;
import dev.marie.framework.color.ColorKey;
import dev.marie.framework.color.ColorKeyPair;
import dev.marie.framework.color.ColorPreviewOverrides;
import dev.marie.framework.color.ColorRegistry;
import dev.marie.framework.color.MarieColors;
import dev.marie.framework.ui.api.MarieToolbox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Binds this mod's registered colors to MariesLib's toolbox color slots, so the in-game picker edits the same
 * values the renderers already read through {@link MarieColors#resolveColor}. The live preview is a
 * {@link ColorPreviewOverrides} entry (what the Cloth hex row pushes while typing); on release the color is
 * written to {@link ColorRegistry} (or removed when it equals its default) and saved, exactly as
 * {@code ColorHexRowWidget#save} does, and the preview override is cleared. If the picker is closed or moved to
 * another slot first, the slot's cancel callback clears the uncommitted preview. Nothing here stores a color itself.
 */
public final class NourishedColorSlots {

    private NourishedColorSlots() {}

    /** One slot per registered nutrient, bound to {@code nourished:nutrient.<key>} — the color its bar and Diet-screen rows draw. */
    public static void addNutrients(MarieToolbox.PanelBuilder panel) {
        for (String key : NutrientRegistry.getKeys()) {
            add(panel, key(Nourished.MODID, "nutrient." + key), NutrientRegistry.getLabel(key));
        }
    }

    /** Background and text slots for a panel's {@link ColorKeyPair}; skipped if the pair was never registered. */
    public static void addPair(MarieToolbox.PanelBuilder panel, ColorKeyPair pair) {
        if (pair == null) {
            return;
        }
        add(panel, pair.background(), text("nourished.options.color.background"));
        add(panel, pair.text(), text("nourished.options.color.text"));
    }

    /** One slot per activity module the activity registry knows a log color for, in its display order, bound to the registry's own key. */
    public static void addActivities(MarieToolbox.PanelBuilder panel) {
        for (String id : ActivityDrivenNutrientRegistry.colorModuleIds()) {
            String langKey = "nourished.options.color.activity." + id;
            String label = text(langKey);
            add(panel, ActivityDrivenNutrientRegistry.colorKey(id), label.equals(langKey) ? id : label);
        }
    }

    /** A slot for one of {@link NourishedColors}' fixed colors; {@code langKey} names it. */
    public static void addFixed(MarieToolbox.PanelBuilder panel, ColorKey key, String langKey) {
        add(panel, key, text(langKey), NourishedColors.defaultRgb(key));
    }

    /**
     * Text, bar track and border slots for one Intake Breakdown nutrient's row — bound to {@code
     * nutrient}'s own dedicated colors (see {@link NourishedColors#registerIntakeBarColors}), not a
     * role shared across every row, so each of Fruits/Vegetables/Proteins/Grains/Dairy is
     * independently colorable instead of all five sharing one setting.
     */
    public static void addIntakeBarColors(MarieToolbox.PanelBuilder panel, String nutrient) {
        add(panel, NourishedColors.intakeBarTextKey(nutrient), text("nourished.options.color.text"));
        add(panel, NourishedColors.intakeBarTrackKey(nutrient), text("nourished.options.color.bar_track"));
        add(panel, NourishedColors.intakeBarBorderKey(nutrient), text("nourished.options.color.border"));
    }

    private static void add(MarieToolbox.PanelBuilder panel, ColorKey key, String label) {
        ColorDefinition definition = ColorDefinitionRegistry.get(key);
        add(panel, key, label, definition != null ? definition.getDefaultArgb() & 0xFFFFFF : 0xFF00FF);
    }

    private static void add(MarieToolbox.PanelBuilder panel, ColorKey key, String label, int defaultRgb) {
        panel.color(label,
                () -> MarieColors.resolveColor(key),
                rgb -> ColorPreviewOverrides.setOverride(key, 0xFF000000 | rgb),
                defaultRgb,
                () -> commit(key, defaultRgb),
                () -> ColorPreviewOverrides.setOverride(key, null));
    }

    /** Persists the previewed color (or drops the override when it equals the default), saves, and clears the preview. */
    private static void commit(ColorKey key, int defaultRgb) {
        Integer preview = ColorPreviewOverrides.getOverride(key);
        if (preview != null) {
            String registryKey = key.id().toString();
            if ((preview & 0xFFFFFF) == defaultRgb) {
                ColorRegistry.remove(registryKey);
            } else {
                ColorRegistry.setArgb(registryKey, 0xFF000000 | (preview & 0xFFFFFF));
            }
            ColorRegistry.save();
        }
        ColorPreviewOverrides.setOverride(key, null);
    }

    private static ColorKey key(String modId, String path) {
        return ColorKey.of(ResourceLocation.fromNamespaceAndPath(modId, path));
    }

    private static String text(String key) {
        return Component.translatable(key).getString();
    }
}
