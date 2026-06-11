package dev.maire.nourished.client.hud;

import dev.marie.MariesLib.client.MarieClientCache;
import dev.maire.nourished.client.NourishedKeys;
import dev.marie.MariesLib.config.ModuleCache;
import dev.maire.nourished.config.NourishedClientConfig;
import dev.marie.MariesLib.tracking.TrackingData;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class NourishedHUD {

    private static final Map<String, Float> displayValues = new HashMap<>();
    private static long lastNano = 0;

    private static boolean hudDragging;
    private static boolean hudResizing;
    private static int dragGrabOffsetX;
    private static int dragGrabOffsetY;
    private static int dragAnchorBaseX;
    private static int dragAnchorBaseY;
    private static int resizeOriginX;
    private static int resizeOriginY;
    private static double resizeBaseDiagonal;
    private static double resizePreviewScale;

    private NourishedHUD() {}

    public static void onRenderGuiPost(RenderGuiEvent.Post event) {
        if (!ModuleCache.enableHUD) {
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
        HudLayout.Layout layout = HudLayout.compute(mc, visibleKeys);
        drawHudPanel(
                event.getGuiGraphics(),
                mc,
                data,
                visibleKeys,
                layout,
                layout.panelX(),
                layout.panelY()
        );
    }

    public static void renderForEditScreen(GuiGraphics g, Minecraft mc) {
        if (!ModuleCache.enableHUD) {
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
            HudDrawHelpers.drawEditBanner(g, mc);
            return;
        }
        HudLayout.Layout layout = HudLayout.compute(mc, visibleKeys);
        int[] m = scaledMouse(mc);
        if (hudResizing) {
            double dist = Math.hypot(m[0] - resizeOriginX, m[1] - resizeOriginY);
            resizePreviewScale = Mth.clamp(dist / Math.max(1.0d, resizeBaseDiagonal), 0.3d, 3.0d);
            layout = HudLayout.compute(mc, visibleKeys, resizePreviewScale);
        }

        int panelX = layout.panelX();
        int panelY = layout.panelY();
        if (hudDragging) {
            panelX = Mth.clamp(m[0] - dragGrabOffsetX, 0, Math.max(0, mc.getWindow().getGuiScaledWidth() - layout.panelW()));
            panelY = Mth.clamp(m[1] - dragGrabOffsetY, 0, Math.max(0, mc.getWindow().getGuiScaledHeight() - layout.panelH()));
        } else if (hudResizing) {
            panelX = resizeOriginX;
            panelY = resizeOriginY;
        }

        boolean hovered = m[0] >= panelX && m[1] >= panelY
                && m[0] < panelX + layout.panelW() && m[1] < panelY + layout.panelH();
        g.fill(panelX - 2, panelY - 2, panelX + layout.panelW() + 2, panelY + layout.panelH() + 2, HudDrawHelpers.editOverlayColor());
        if (hovered || hudDragging || hudResizing) {
            HudDrawHelpers.drawBorder(g, panelX - 1, panelY - 1, layout.panelW() + 2, layout.panelH() + 2, 1, HudDrawHelpers.hoverBorderColor());
        }
        if (hudResizing) {
            HudDrawHelpers.drawDashedBorder(g, panelX - 2, panelY - 2, layout.panelW() + 4, layout.panelH() + 4, HudDrawHelpers.dashedPreviewColor());
        }

        drawHudPanel(g, mc, data, visibleKeys, layout, panelX, panelY);
        boolean handleHovered = HudDrawHelpers.isOverResizeHandle(m[0], m[1], panelX, panelY, layout.panelW(), layout.panelH());
        HudDrawHelpers.drawResizeHandle(g, mc, panelX, panelY, layout.panelW(), layout.panelH(), handleHovered, hudResizing, m[0], m[1]);
        if (hudResizing) {
            String scaleText = String.format("Scale: %.1fx", resizePreviewScale);
            int textX = panelX + Math.max(0, (layout.panelW() - mc.font.width(scaleText)) / 2);
            g.drawString(mc.font, scaleText, textX, panelY + layout.panelH() + 6, HudDrawHelpers.handleActiveColor(), false);
        }
        HudDrawHelpers.drawEditBanner(g, mc);
    }

    public static void onEditMousePress(int mx, int my, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        List<String> keys = NutrientRegistry.getKeys();
        if (keys.isEmpty()) {
            return;
        }
        TrackingData data = MarieClientCache.get();
        NourishedClientConfig cc = NourishedClientConfig.get();
        List<String> visibleKeys = HudVisibility.visibleKeys(data, cc.effectiveDietBarOrder(), cc);
        if (visibleKeys.isEmpty()) {
            return;
        }

        HudLayout.Layout layout = HudLayout.compute(mc, visibleKeys);
        int panelX = layout.panelX();
        int panelY = layout.panelY();
        boolean overHandle = HudDrawHelpers.isOverResizeHandle(mx, my, panelX, panelY, layout.panelW(), layout.panelH());

        if (overHandle) {
            hudResizing = true;
            hudDragging = false;
            resizeOriginX = panelX;
            resizeOriginY = panelY;
            HudLayout.Layout baseScaleLayout = HudLayout.compute(mc, visibleKeys, 1.0d);
            resizeBaseDiagonal = Math.hypot(baseScaleLayout.panelW(), baseScaleLayout.panelH());
            resizePreviewScale = layout.scale();
            return;
        }

        boolean over = mx >= panelX && my >= panelY && mx < panelX + layout.panelW() && my < panelY + layout.panelH();
        if (over) {
            hudDragging = true;
            hudResizing = false;
            dragGrabOffsetX = mx - panelX;
            dragGrabOffsetY = my - panelY;
            dragAnchorBaseX = layout.baseX();
            dragAnchorBaseY = layout.baseY();
        }
    }

    public static void onEditMouseRelease(int mx, int my, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        List<String> keys = NutrientRegistry.getKeys();
        if (keys.isEmpty()) {
            return;
        }
        TrackingData data = MarieClientCache.get();
        NourishedClientConfig cc = NourishedClientConfig.get();
        List<String> visibleKeys = HudVisibility.visibleKeys(data, cc.effectiveDietBarOrder(), cc);
        if (visibleKeys.isEmpty()) {
            return;
        }

        if (hudDragging) {
            HudLayout.Layout layout = HudLayout.compute(mc, visibleKeys);
            int sw = mc.getWindow().getGuiScaledWidth();
            int sh = mc.getWindow().getGuiScaledHeight();
            int px = Mth.clamp(mx - dragGrabOffsetX, 0, Math.max(0, sw - layout.panelW()));
            int py = Mth.clamp(my - dragGrabOffsetY, 0, Math.max(0, sh - layout.panelH()));
            cc.setHudOffsetX(px - dragAnchorBaseX);
            cc.setHudOffsetY(py - dragAnchorBaseY);
            NourishedClientConfig.saveNow();
            hudDragging = false;
            return;
        }
        if (hudResizing) {
            double finalScale = Mth.clamp(resizePreviewScale, 0.3d, 3.0d);
            cc.setHudScale(finalScale);
            HudLayout.Layout scaled = HudLayout.compute(mc, visibleKeys, finalScale);
            cc.setHudOffsetX(resizeOriginX - scaled.baseX());
            cc.setHudOffsetY(resizeOriginY - scaled.baseY());
            NourishedClientConfig.saveNow();
            hudResizing = false;
        }
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            return;
        }
        if (!ModuleCache.enableHUD) {
            return;
        }
        while (NourishedKeys.EDIT_HUD.consumeClick()) {
            HUDEditMode.setActive(true);
        }
    }

    private static void drawHudPanel(
            GuiGraphics g,
            Minecraft mc,
            TrackingData data,
            List<String> keys,
            HudLayout.Layout layout,
            int panelX,
            int panelY
    ) {
        HudPanelRenderer.drawPanel(g, mc, data, keys, layout, panelX, panelY, displayValues);
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

    private static int[] scaledMouse(Minecraft mc) {
        double s = mc.getWindow().getGuiScale();
        return new int[]{(int) (mc.mouseHandler.xpos() / s), (int) (mc.mouseHandler.ypos() / s)};
    }
}
