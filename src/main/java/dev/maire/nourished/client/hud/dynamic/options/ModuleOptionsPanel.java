package dev.maire.nourished.client.hud.dynamic.options;

import dev.marie.framework.ui.api.MarieToolbox;
import dev.marie.framework.ui.component.MarieComponent;
import dev.maire.nourished.config.NourishedClientConfig;
import net.minecraft.network.chat.Component;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

/**
 * Options panel for the smaller HUD modules (Calorie History, Activity Log): the same options as the
 * Nutrient HUD box that apply to them — Layout (Padding), Behavior (Move Text and Icons) and Appearance (Text size, Icon size, Text
 * brightness, Icon brightness, Background opacity), the last three over the module's own config
 * fields. The Nutrient HUD's vertical layout, reveal-on-gain and threshold options are about nutrient
 * bars, which these modules don't have. Built only through {@link MarieToolbox}.
 */
public final class ModuleOptionsPanel {

    private ModuleOptionsPanel() {}

    /**
     * @param title    the module's display name (also the panel id)
     * @param panelId  the module's persisted UI-state key
     * @param opacity  getter/setter over the module's background-opacity config field (0.0-1.0)
     * @param text     getter/setter over the module's text-brightness config field (0.2-2.0)
     * @param icon     getter/setter over the module's icon-brightness config field (0.2-2.0)
     */
    public static MarieComponent build(String title, String panelId,
                                       DoubleSupplier opacity, DoubleConsumer setOpacity,
                                       DoubleSupplier text, DoubleConsumer setText,
                                       DoubleSupplier icon, DoubleConsumer setIcon) {
        Runnable save = NourishedClientConfig::saveNow;
        MarieToolbox.PanelBuilder layout = PanelOptionRows.addPadding(
                MarieToolbox.panel(title).tab(label("nourished.options.tab.layout")), panelId);
        MarieToolbox.PanelBuilder appearance = PanelOptionRows.addMoveContent(
                        layout.tab(label("nourished.options.tab.behavior")), panelId)
                .tab(label("nourished.options.tab.appearance"));
        return PanelOptionRows.addSizes(appearance, panelId)
                .slider(label("nourished.options.text_brightness"), text, setText, 0.2d, 2.0d, 0.01d, save)
                .slider(label("nourished.options.icon_brightness"), icon, setIcon, 0.2d, 2.0d, 0.01d, save)
                .slider(label("nourished.options.hud.background_opacity"), opacity, setOpacity, 0.0d, 1.0d, 0.01d, save)
                .build();
    }

    private static String label(String key) {
        return Component.translatable(key).getString();
    }
}
