package dev.maire.nourished.client.hud;

import dev.marie.framework.client.MarieClientCache;
import dev.maire.nourished.client.NourishedKeys;
import dev.marie.framework.config.FeatureFlagCache;
import dev.maire.nourished.config.NourishedClientConfig;
import dev.marie.framework.tracking.TrackingData;
import dev.marie.framework.ui.Bounds;
import dev.marie.framework.ui.EditModeController;
import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.Theme;
import dev.marie.framework.ui.render.GuiGraphicsRenderContext;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class NourishedHUD {

    private static final Map<String, Float> displayValues = new HashMap<>();
    private static long lastNano = 0;

    private static HudEditTarget marieEditTarget;
    private static EditModeController marieEditModeController;

    private NourishedHUD() {}

    public static void onRenderGuiPost(RenderGuiEvent.Post event) {
        if (!FeatureFlagCache.enableHUD()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) {
            return;
        }
        LocalPlayer player = mc.player;
        if (player == null || !player.isAlive()) {
            return;
        }

        TrackingData data = MarieClientCache.get();
        List<String> keys = NutrientRegistry.getKeys();
        if (keys.isEmpty()) {
            return;
        }

        advanceLerp(data, keys);
        NourishedClientConfig cc = NourishedClientConfig.get();
        List<String> orderedKeys = cc.effectiveDietBarOrder();
        List<String> visibleKeys = HudVisibility.visibleKeys(data, orderedKeys, cc);
        if (visibleKeys.isEmpty()) {
            return;
        }
        HudLayout.Layout layout = HudEditTarget.resolvedLayout(mc, visibleKeys);
        drawHudPanelViaMarieUI(event.getGuiGraphics(), mc, event.getPartialTick().getGameTimeDeltaPartialTick(false), visibleKeys, layout);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            return;
        }
        if (!FeatureFlagCache.enableHUD()) {
            return;
        }
        while (NourishedKeys.EDIT_HUD.consumeClick()) {
            marieEditModeController().enter();
        }
    }

    /**
     * Lazily builds the MarieUI edit-mode wrapper + controller on first entry (the HUD has no
     * {@link net.minecraft.client.gui.screens.Screen} of its own to construct these in, unlike
     * {@link dev.maire.nourished.client.screen.DietScreen}, so they live here as statics instead),
     * then reuses the same instances for the rest of the session.
     */
    private static EditModeController marieEditModeController() {
        if (marieEditModeController == null) {
            marieEditTarget = new HudEditTarget(Minecraft.getInstance());
            marieEditModeController = new EditModeController(
                    marieEditTarget,
                    "Drag the HUD panel to reposition, drag the corner handle to resize. H or Esc to exit.",
                    NourishedKeys.EDIT_HUD.getKey().getValue(),
                    () -> {}
            );
        }
        return marieEditModeController;
    }

    /** Package-private accessor for {@link HudEditTarget}'s render path, which reuses the same lerped values as the normal HUD render. */
    static Map<String, Float> currentDisplayValues() {
        return displayValues;
    }

    /**
     * Renders the nutrient HUD panel via the MarieUI Component/Container tree. MarieUI has no root/
     * screen anchoring resolver of its own yet, so the {@link Bounds} passed to the panel is
     * resolved here rather than by {@link NutrientPanelContainer#constraint()}.
     */
    private static void drawHudPanelViaMarieUI(GuiGraphics g, Minecraft mc, float partialTick, List<String> keys, HudLayout.Layout layout) {
        NutrientPanelContainer panel = new NutrientPanelContainer(keys, layout, java.util.Collections.unmodifiableMap(displayValues));
        RenderContext context = new GuiGraphicsRenderContext(g, mc, Theme.DARK, partialTick);
        Bounds bounds = new Bounds(layout.panelX(), layout.panelY(), layout.panelW(), layout.panelH());
        panel.render(context, bounds);
    }

    private static void advanceLerp(TrackingData data, List<String> keys) {
        long now = System.nanoTime();
        float dt = lastNano == 0 ? 0f : Math.min((now - lastNano) / 1_000_000_000f, 0.1f);
        lastNano = now;
        float step = dt <= 0 ? 1f : Math.min(1f, dt / 0.2f);
        for (String key : keys) {
            float target = data.values.getOrDefault(key, 0f);
            float cur = displayValues.getOrDefault(key, target);
            displayValues.put(key, cur + (target - cur) * step);
        }
    }

}
