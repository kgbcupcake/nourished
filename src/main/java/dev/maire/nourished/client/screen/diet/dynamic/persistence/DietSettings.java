package dev.maire.nourished.client.screen.diet.dynamic.persistence;

import dev.marie.framework.ui.api.MarieStoredStyle;
import dev.maire.nourished.config.NourishedClientConfig;
import dev.maire.nourished.config.NutritionalPanelConfig;

import java.util.List;

/**
 * The Diet Screen tab's screen-wide settings for whichever screen is open. The dynamic screen keeps
 * them in the client config (its Cloth config screen edits them); the Nutritional Panel keeps
 * opacity in its own store and bar order/drag in its own config file.
 */
public final class DietSettings {

    /** Owner id of the screen-wide stored style; the same id the Diet Screen tab's options panel uses. */
    public static final String PANEL_ID = "nourished.diet.panel";
    public static final double DEFAULT_OPACITY = 204.0d / 255.0d;

    private DietSettings() {}

    private static boolean panel() {
        return DietScreenPersistence.isNutritionalPanel();
    }

    private static NourishedClientConfig cc() {
        return NourishedClientConfig.get();
    }

    private static NutritionalPanelConfig np() {
        return NutritionalPanelConfig.get();
    }

    public static double backgroundOpacity() {
        return panel()
                ? MarieStoredStyle.backgroundOpacity(DietScreenPersistence.get(), PANEL_ID, DEFAULT_OPACITY)
                : cc().dietBackgroundOpacity();
    }

    /** The dynamic panel's text brightness; the Nutritional Panel has none. */
    public static double textBrightness() {
        return cc().dietTextBrightness();
    }

    public static double iconBrightness() {
        return cc().dietIconBrightness();
    }

    public static boolean barDragEnabled() {
        return panel() ? np().barDragEnabled() : cc().dietBarDragEnabled();
    }

    public static void setBarDragEnabled(boolean value) {
        if (panel()) np().setBarDragEnabled(value); else cc().setDietBarDragEnabled(value);
    }

    public static List<String> barOrder() {
        return panel() ? np().effectiveBarOrder() : cc().effectiveDietBarOrder();
    }

    public static void setBarOrder(List<String> keys) {
        if (panel()) np().setBarOrder(keys); else cc().setDietBarOrder(keys);
    }

    public static void resetBarOrder() {
        if (panel()) np().resetBarOrder(); else cc().resetDietBarOrder();
    }

    public static void save() {
        if (panel()) NutritionalPanelConfig.saveNow(); else NourishedClientConfig.saveNow();
    }
}
