package dev.maire.nourished.client.hud.dynamic.options;

import dev.marie.framework.ui.api.MarieToolbox;
import dev.marie.framework.ui.component.MarieComponent;
import dev.maire.nourished.config.NourishedClientConfig;
import net.minecraft.network.chat.Component;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

/**
 * Options panel for the smaller HUD modules (Calorie History, Activity Log): the same options as
 * the Nutrient HUD box that apply to them — a Layout tab (Text Scale, Padding) and an Appearance
 * tab (Background opacity, Icon and text brightness), the latter two over the module's own config
 * fields. The Nutrient HUD's vertical layout, reveal-on-gain and threshold options are about
 * nutrient bars, which these modules don't have. Built only through {@link MarieToolbox}.
 */
public final class ModuleOptionsPanel {

    private ModuleOptionsPanel() {}

    /**
     * @param title      the module's display name (also the panel id)
     * @param panelId    the module's persisted UI-state key
     * @param opacity    getter/setter over the module's background-opacity config field (0.0-1.0)
     * @param brightness getter/setter over the module's brightness config field (0.2-2.0)
     */
    public static MarieComponent build(String title, String panelId, DoubleSupplier opacity, DoubleConsumer setOpacity,
                                       DoubleSupplier brightness, DoubleConsumer setBrightness) {
        return LayoutOptionRows.addTo(
                        MarieToolbox.panel(title).tab(Component.translatable("nourished.options.tab.layout").getString()),
                        panelId)
                .tab(Component.translatable("nourished.options.tab.appearance").getString())
                    .slider(Component.translatable("nourished.options.hud.background_opacity").getString(),
                            opacity, setOpacity, 0.0d, 1.0d, 0.01d, NourishedClientConfig::saveNow)
                    .slider(Component.translatable("nourished.options.hud.content_brightness").getString(),
                            brightness, setBrightness, 0.2d, 2.0d, 0.01d, NourishedClientConfig::saveNow)
                .build();
    }
}
