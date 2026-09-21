package dev.maire.nourished.client.colors;

import dev.maire.nourished.core.Nourished;
import dev.marie.framework.color.ColorDefinition;
import dev.marie.framework.color.ColorKey;
import dev.marie.framework.color.MarieColors;
import dev.marie.framework.ui.Theme;
import dev.marie.framework.ui.ThemeKey;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Every fixed color the client renderers draw, as registered {@link ColorKey}s, so each is editable through
 * MariesLib's color system ({@code colors.json} / the in-game picker) instead of being a constant in a renderer.
 * Renderers read a color with {@link MarieColors#resolveColor(ColorKey)} and never hold a literal.
 *
 * <p>This class holds <b>key names and shade-step constants only — no color values</b>. A <b>nutrient's own
 * registered color</b> ({@code nourished:nutrient.<key>}) is exactly what a nutrient row draws (fill, percent
 * text, arrows, flash) in every state; there is no threshold tinting. Every other color is a
 * <b>function-named</b> key (text, border, bar.track, balance.*, toggle.*, ...) whose default is the RGB of a
 * {@link ThemeKey} of {@link Theme#DARK} (every Nourished render context uses that theme), or a named
 * {@link MarieColors#shade shade} of one, so it is not a literal and stays editable per module. Alpha is never
 * taken from the theme: each renderer keeps its own opacity handling. Naming describes function, never hue.
 */
public final class NourishedColors {

    /** Alpha mask for handing an RGB to {@link MarieColors#shade}. */
    private static final int OPAQUE = 0xFF000000;

    /** Default RGB of every key defined here, in definition order. */
    private static final Map<ColorKey, Integer> DEFAULTS = new LinkedHashMap<>();

    /** {@code panel.diet}: the theme's panel background shaded toward white by this amount. */
    public static final double DIET_PANEL_SHADE = 0.02;
    /** {@code diet.bar_track}: the theme's border shaded toward black by this amount. */
    public static final double DIET_TRACK_SHADE = -0.27;
    /** A Diet sub-surface (rows, inner panels): the Diet panel color shaded toward white by this amount. */
    public static final double SURFACE_SHADE = 0.05;

    // Panels and text
    public static final ColorKey HUD_PANEL = themed("panel.nutrient_hud", ThemeKey.PANEL_BACKGROUND);
    public static final ColorKey NUTRIENT_HUD_TEXT = themed("text.nutrient_hud", ThemeKey.TEXT_SECONDARY);
    public static final ColorKey DIET_PANEL = shaded("panel.diet", ThemeKey.PANEL_BACKGROUND, DIET_PANEL_SHADE);
    public static final ColorKey DIET_TITLE = themed("diet.title", ThemeKey.TEXT_PRIMARY);
    public static final ColorKey DIET_TODAY = themed("diet.today_text", ThemeKey.TEXT_PRIMARY);

    // Calories and the accents of the Calorie History / Activity Log panels
    public static final ColorKey CALORIE_VALUE = themed("calorie.value", ThemeKey.TEXT_PRIMARY);
    public static final ColorKey CALORIE_OVER_GOAL = themed("calorie_hud.over_goal", ThemeKey.TEXT_SECONDARY);
    public static final ColorKey CALORIE_ACCENT = themed("calorie_hud.accent", ThemeKey.TEXT_PRIMARY);
    public static final ColorKey ACTIVITY_ACCENT = themed("activity_log_hud.accent", ThemeKey.TEXT_PRIMARY);

    // Shared roles
    public static final ColorKey TEXT = themed("text", ThemeKey.TEXT_PRIMARY);
    public static final ColorKey TEXT_MUTED = themed("text.muted", ThemeKey.TEXT_SECONDARY);
    public static final ColorKey TEXT_HEADER = themed("text.header", ThemeKey.TEXT_SECONDARY);
    // Each Diet box's header has its own color, separate from the shared text.header role (which the right column's header still uses)
    public static final ColorKey CALORIES_HEADER = themed("diet.calories.header", ThemeKey.TEXT_PRIMARY);
    public static final ColorKey BALANCE_HEADER = themed("diet.balance.header", ThemeKey.TEXT_SECONDARY);
    public static final ColorKey RECENT_MEALS_HEADER = themed("diet.recent_meals.header", ThemeKey.TEXT_SECONDARY);
    public static final ColorKey EAT_MORE_HEADER = themed("diet.eat_more.header", ThemeKey.TEXT_SECONDARY);
    public static final ColorKey ACTIVE_EFFECTS_HEADER = themed("diet.active_effects.header", ThemeKey.TEXT_SECONDARY);
    public static final ColorKey RECENT_MEALS_TEXT = themed("diet.recent_meals.text", ThemeKey.TEXT_PRIMARY);
    public static final ColorKey CALORIES_BORDER = themed("diet.calories.border", ThemeKey.BORDER);
    public static final ColorKey RECENT_MEALS_BORDER = themed("diet.recent_meals.border", ThemeKey.BORDER);
    public static final ColorKey EAT_MORE_BORDER = themed("diet.eat_more.border", ThemeKey.BORDER);
    public static final ColorKey BORDER = themed("border", ThemeKey.BORDER);
    public static final ColorKey DIVIDER = themed("divider", ThemeKey.BORDER);
    public static final ColorKey BAR_TRACK = themed("bar.track", ThemeKey.BAR_BACKGROUND);
    public static final ColorKey DIET_BAR_TRACK = shaded("diet.bar_track", ThemeKey.BORDER, DIET_TRACK_SHADE);
    public static final ColorKey BALANCE_BALANCED = themed("balance.balanced", ThemeKey.TEXT_PRIMARY);
    public static final ColorKey BALANCE_LOW = themed("balance.low", ThemeKey.TEXT_SECONDARY);
    public static final ColorKey BALANCE_EXCESS = themed("balance.excess", ThemeKey.TEXT_SECONDARY);
    public static final ColorKey EFFECT_BENEFICIAL = themed("effect.beneficial", ThemeKey.TEXT_PRIMARY);
    public static final ColorKey EFFECT_HARMFUL = themed("effect.harmful", ThemeKey.TEXT_SECONDARY);
    public static final ColorKey TOGGLE_ON = themed("toggle.on", ThemeKey.TEXT_PRIMARY);
    public static final ColorKey TOGGLE_OFF = themed("toggle.off", ThemeKey.BORDER);
    public static final ColorKey TOGGLE_BORDER = themed("toggle.border", ThemeKey.PANEL_BACKGROUND);
    public static final ColorKey TOGGLE_HOUSING = themed("toggle.housing", ThemeKey.PANEL_BACKGROUND);
    public static final ColorKey TOGGLE_LEVER = themed("toggle.lever", ThemeKey.TEXT_SECONDARY);
    public static final ColorKey EDIT_OUTLINE = themed("edit.outline", ThemeKey.DASHED_PREVIEW);
    public static final ColorKey EDIT_LABEL_TEXT = themed("edit.label.text", ThemeKey.EDIT_BANNER_TEXT);
    public static final ColorKey EDIT_LABEL_SHADOW = themed("edit.label.shadow", ThemeKey.EDIT_BANNER_BACKGROUND);

    // Command Center card accents (read at draw time; one per card)
    public static final ColorKey CC_TOOLS_EXPORT_ALL = themed("command_center.tools.export_all", ThemeKey.TEXT_PRIMARY);
    public static final ColorKey CC_TOOLS_AUDIT_TAGS = themed("command_center.tools.audit_tags", ThemeKey.TEXT_PRIMARY);
    public static final ColorKey CC_TOOLS_DEBUG_ACTIVITYLOG = themed("command_center.tools.debug_activitylog", ThemeKey.TEXT_PRIMARY);
    public static final ColorKey CC_TOOLS_RELOAD = themed("command_center.tools.reload", ThemeKey.TEXT_PRIMARY);
    public static final ColorKey CC_TOOLS_INVALIDATE_CACHE = themed("command_center.tools.invalidate_cache", ThemeKey.TEXT_PRIMARY);
    public static final ColorKey CC_TOOLS_UNASSIGNED_SOURCES = themed("command_center.tools.unassigned_sources", ThemeKey.TEXT_PRIMARY);
    public static final ColorKey CC_TOOLS_NBT_PATHS = themed("command_center.tools.nbt_paths", ThemeKey.TEXT_PRIMARY);
    public static final ColorKey CC_TOOLS_PROFILE_LIST = themed("command_center.tools.profile_list", ThemeKey.TEXT_PRIMARY);
    public static final ColorKey CC_TOOLS_MY_PROFILE = themed("command_center.tools.my_profile", ThemeKey.TEXT_PRIMARY);
    public static final ColorKey CC_FRAMEWORK_STATUS = themed("command_center.framework.status", ThemeKey.TEXT_SECONDARY);
    public static final ColorKey CC_FRAMEWORK_MODS = themed("command_center.framework.mods", ThemeKey.TEXT_SECONDARY);
    public static final ColorKey CC_FRAMEWORK_API = themed("command_center.framework.api", ThemeKey.TEXT_SECONDARY);
    public static final ColorKey CC_FRAMEWORK_REGISTRIES = themed("command_center.framework.registries", ThemeKey.TEXT_SECONDARY);

    private NourishedColors() {}

    private static ColorKey def(String path, int rgb) {
        ColorKey key = key(path);
        DEFAULTS.put(key, rgb);
        return key;
    }

    /** A color whose default is the RGB of {@code themeKey} in the dark theme (alpha is not taken). */
    private static ColorKey themed(String path, ThemeKey themeKey) {
        return def(path, themeRgb(themeKey));
    }

    /** A color whose default is the RGB of {@code themeKey} shaded by {@code amount} (toward black if negative, white if positive). */
    private static ColorKey shaded(String path, ThemeKey themeKey, double amount) {
        return def(path, MarieColors.shade(OPAQUE | themeRgb(themeKey), amount) & 0x00FFFFFF);
    }

    private static int themeRgb(ThemeKey themeKey) {
        return Theme.DARK.color(themeKey) & 0x00FFFFFF;
    }

    private static ColorKey key(String path) {
        return ColorKey.of(ResourceLocation.fromNamespaceAndPath(Nourished.MODID, path));
    }

    /** Registers every color above with its default. Call from mod init and again on reload, like the other definitions. */
    public static void register() {
        DEFAULTS.forEach((key, rgb) -> MarieColors.registerColor(ColorDefinition.of(key, 0xFF000000 | rgb)));
    }

    /** The built-in RGB default of one of this class's keys. */
    public static int defaultRgb(ColorKey key) {
        return DEFAULTS.get(key);
    }

    /** Resolved RGB only (alpha stripped), for renderers that compose their own alpha. */
    public static int rgb(ColorKey key) {
        return MarieColors.resolveColor(key) & 0x00FFFFFF;
    }

    // Colors derived from a nutrient's own registered color

    /** The registry key of a nutrient's color ({@code nourished:nutrient.<key>}). */
    public static ColorKey nutrientKey(String nutrient) {
        return key("nutrient." + nutrient);
    }

    /** The nutrient's own registered color, as is: what every part of its row draws (fill, text, arrows, flash). */
    public static int nutrient(String nutrient) {
        return MarieColors.resolveColor(nutrientKey(nutrient));
    }

    /** {@link #nutrient} with alpha stripped, for overlays that compose their own alpha (the gain flash). */
    public static int nutrientRgb(String nutrient) {
        return nutrient(nutrient) & 0x00FFFFFF;
    }

    /** Diet sub-surface RGB (alpha stripped): the Diet panel color shaded toward white. */
    public static int surfaceRgb() {
        return MarieColors.shade(MarieColors.resolveColor(DIET_PANEL), SURFACE_SHADE) & 0x00FFFFFF;
    }
}
