package dev.maire.nourished.client.micro;

import dev.maire.nourished.config.MicroClientConfig;
import dev.maire.nourished.core.micro.MicroAttachment;
import dev.maire.nourished.core.micro.MicroDef;
import dev.maire.nourished.core.micro.MicroRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.List;
import java.util.Map;

/** Optional compact overlay: one labelled mini-bar per nutrient. Off by default. */
public final class MicroHud {

    private static final int LABEL_W = 48;
    private static final int BAR_W = 50;
    private static final int ROW_H = 9;
    private static final int PAD = 3;

    private MicroHud() {}

    static void onRenderGuiPost(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        MicroClientConfig cfg = MicroClientConfig.get();
        if (cfg == null || !cfg.hudEnabled() || mc.options.hideGui || mc.screen != null) return;
        if (mc.player == null || !mc.player.isAlive() || mc.getDebugOverlay().showDebugScreen()) return;
        MicroRegistry.Snapshot snap = MicroRegistry.forSide(true);
        if (!snap.settings().enabled() || snap.defs().isEmpty()) return;
        List<MicroDef> defs = List.copyOf(snap.defs().values());
        Map<String, Float> levels = MicroAttachment.get(mc.player);
        if (cfg.hudOnlyWhenLow() && defs.stream().noneMatch(d -> levels.getOrDefault(d.key(), 0f) < d.lowThreshold())) return;

        GuiGraphics g = event.getGuiGraphics();
        Font font = mc.font;
        float scale = cfg.hudScale();
        int w = PAD * 2 + LABEL_W + BAR_W;
        int h = PAD * 2 + defs.size() * ROW_H;
        int sw = Math.round(w * scale);
        int sh = Math.round(h * scale);
        int screenW = g.guiWidth();
        int screenH = g.guiHeight();
        int x = switch (cfg.hudCorner()) {
            case TOP_LEFT, BOTTOM_LEFT -> cfg.hudOffsetX();
            case TOP_RIGHT, BOTTOM_RIGHT -> screenW - sw - cfg.hudOffsetX();
        };
        int y = switch (cfg.hudCorner()) {
            case TOP_LEFT, TOP_RIGHT -> cfg.hudOffsetY();
            case BOTTOM_LEFT, BOTTOM_RIGHT -> screenH - sh - cfg.hudOffsetY();
        };

        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1f);
        g.fill(0, 0, w, h, 0x90000000);
        for (int i = 0; i < defs.size(); i++) {
            MicroDef def = defs.get(i);
            int color = 0xFF000000 | def.color();
            float level = Math.max(0f, Math.min(1f, levels.getOrDefault(def.key(), 0f)));
            int rowY = PAD + i * ROW_H;
            g.pose().pushPose();
            g.pose().translate(PAD, rowY + 1, 0);
            g.pose().scale(0.75f, 0.75f, 1f);
            g.drawString(font, def.displayName(), 0, 0, level < def.lowThreshold() ? 0xFFE05050 : 0xFFDDDDDD);
            g.pose().popPose();
            int bx = PAD + LABEL_W;
            g.fill(bx, rowY + 2, bx + BAR_W, rowY + 6, 0xFF2A3038);
            g.fill(bx, rowY + 2, bx + Math.round(BAR_W * level), rowY + 6, color);
        }
        g.pose().popPose();
    }
}
