package dev.maire.nourished.modules.Stamina.HUD;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.config.ModuleCache;
import dev.maire.nourished.config.NourishedClientConfig;
import dev.maire.nourished.modules.Stamina.Core.StaminaSyncPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.lwjgl.glfw.GLFW;

@ApiStatus.Internal
public final class StaminaHUD {

    private static final int BAR_H = 5;
    private static final int BAR_W = 80;
    private static final int ROW_GAP = 4;
    private static final int PANEL_PAD = 8;
    private static final int PANEL_W = 120;
    private static final int PANEL_H = 42;

    private static final int COL_PANEL_BG = 0xCC101010;
    private static final int COL_BAR_BG = 0x99111111;
    private static final int COL_PHYSICAL_FILL = 0xFF5B8CFF;
    private static final int COL_MENTAL_FILL = 0xFF8B5BFF;
    private static final int COL_BONUS = 0xFFFFD65C;
    private static final int COL_FATIGUE = 0xFFFF5555;
    private static final int COL_DEBT = 0xFFFF8800;
    private static final int COL_LABEL = 0xFFAAAAAA;
    private static final int COL_EXHAUSTED = 0xFFFF3333;

    public static volatile float physicalStamina = 100f;
    public static volatile float physicalMax = 100f;
    public static volatile float physicalFatigue = 0f;
    public static volatile float physicalBonus = 0f;
    public static volatile float physicalDebt = 0f;
    public static volatile float mentalStamina = 100f;
    public static volatile float mentalMax = 100f;
    public static volatile float mentalFatigue = 0f;
    public static volatile float mentalBonus = 0f;
    public static volatile float mentalDebt = 0f;

    private static boolean hudDragging;
    private static int dragGrabOffsetX;
    private static int dragGrabOffsetY;

    private StaminaHUD() {}

    public static void updateFromPayload(StaminaSyncPayload payload) {
        physicalStamina = payload.physicalStamina();
        physicalMax = payload.physicalMax();
        physicalFatigue = payload.physicalFatiguePenalty();
        physicalBonus = payload.physicalBonusStamina();
        physicalDebt = payload.physicalDebt();
        mentalStamina = payload.mentalStamina();
        mentalMax = payload.mentalMax();
        mentalFatigue = payload.mentalFatiguePenalty();
        mentalBonus = payload.mentalBonusStamina();
        mentalDebt = payload.mentalDebt();
    }

    public static void onRenderGuiPost(RenderGuiEvent.Post event) {
        if (!ModuleCache.enableStamina) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return;
        LocalPlayer player = mc.player;
        if (player == null || !player.isAlive()) return;

        Layout layout = computeLayout(mc);
        drawPanel(event.getGuiGraphics(), mc, layout.panelX, layout.panelY);
    }

    public static void onEditMousePress(int mx, int my, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return;
        Minecraft mc = Minecraft.getInstance();
        Layout layout = computeLayout(mc);
        boolean over = mx >= layout.panelX && my >= layout.panelY
                && mx < layout.panelX + PANEL_W && my < layout.panelY + PANEL_H;
        if (over) {
            hudDragging = true;
            dragGrabOffsetX = mx - layout.panelX;
            dragGrabOffsetY = my - layout.panelY;
        }
    }

    public static void onEditMouseRelease(int mx, int my, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return;
        if (!hudDragging) return;

        Minecraft mc = Minecraft.getInstance();
        Layout layout = computeLayout(mc);
        int px = Mth.clamp(mx - dragGrabOffsetX, 0, Math.max(0, layout.screenW - PANEL_W));
        int py = Mth.clamp(my - dragGrabOffsetY, 0, Math.max(0, layout.screenH - PANEL_H));

        NourishedClientConfig cc = NourishedClientConfig.get();
        cc.setStaminaHudOffsetX(px - layout.baseX);
        cc.setStaminaHudOffsetY(py - layout.baseY);
        NourishedClientConfig.saveNow();
        hudDragging = false;
    }

    private static void drawPanel(GuiGraphics graphics, Minecraft mc, int panelX, int panelY) {
        graphics.fill(panelX, panelY, panelX + PANEL_W, panelY + PANEL_H, COL_PANEL_BG);

        int barX = panelX + PANEL_PAD;
        int physicalLabelY = panelY + PANEL_PAD - 2;
        int physicalBarY = physicalLabelY + 10;
        int mentalLabelY = physicalBarY + BAR_H + ROW_GAP + 1;
        int mentalBarY = mentalLabelY + 10;

        drawStaminaRow(
                graphics,
                mc,
                "Physical",
                panelX + PANEL_PAD,
                physicalLabelY,
                barX,
                physicalBarY,
                physicalStamina,
                physicalMax,
                physicalFatigue,
                physicalBonus,
                physicalDebt,
                COL_PHYSICAL_FILL
        );
        drawStaminaRow(
                graphics,
                mc,
                "Mental",
                panelX + PANEL_PAD,
                mentalLabelY,
                barX,
                mentalBarY,
                mentalStamina,
                mentalMax,
                mentalFatigue,
                mentalBonus,
                mentalDebt,
                COL_MENTAL_FILL
        );
    }

    private static void drawStaminaRow(
            GuiGraphics g,
            Minecraft mc,
            String label,
            int labelX,
            int labelY,
            int barX,
            int barY,
            float stamina,
            float max,
            float fatigue,
            float bonus,
            float debt,
            int fillColor
    ) {
        float safeMax = Math.max(1f, max);
        float pct = Mth.clamp(stamina / safeMax, 0f, 1f);
        int fillW = Mth.clamp((int) (pct * BAR_W), 0, BAR_W);
        int fatigueW = Mth.clamp((int) (fatigue / safeMax * BAR_W), 0, BAR_W);
        int bonusW = Mth.clamp((int) (bonus / safeMax * BAR_W), 0, 20);
        int pctValue = Mth.clamp((int) (stamina / safeMax * 100f), 0, 999);

        drawScaledLabel(g, mc, label, labelX, labelY, COL_LABEL, 6f / 9f);
        g.fill(barX, barY, barX + BAR_W, barY + BAR_H, COL_BAR_BG);
        if (fillW > 0) {
            g.fill(barX, barY, barX + fillW, barY + BAR_H, stamina <= 0f ? COL_EXHAUSTED : fillColor);
        }
        if (fatigueW > 0) {
            g.fill(barX + BAR_W - fatigueW, barY, barX + BAR_W, barY + BAR_H, COL_FATIGUE);
        }
        if (bonusW > 0) {
            g.fill(barX + BAR_W, barY, barX + BAR_W + bonusW, barY + BAR_H, COL_BONUS);
        }
        if (debt > 0f) {
            g.fill(barX + BAR_W - 3, barY - 1, barX + BAR_W, barY + BAR_H + 1, COL_DEBT);
        }
        drawScaledLabel(g, mc, pctValue + "%", barX + BAR_W + 4, barY - 2, COL_LABEL, 6f / 9f);
    }

    private static void drawScaledLabel(GuiGraphics g, Minecraft mc, String text, int x, int y, int color, float scale) {
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.scale(scale, scale, 1f);
        g.drawString(mc.font, text, 0, 0, color, false);
        pose.popPose();
    }

    private static Layout computeLayout(Minecraft mc) {
        NourishedClientConfig cc = NourishedClientConfig.get();
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        int baseX = sw - PANEL_W;
        int baseY = sh - PANEL_H;
        int panelX = Mth.clamp(baseX + cc.staminaHudOffsetX(), 0, Math.max(0, sw - PANEL_W));
        int panelY = Mth.clamp(baseY + cc.staminaHudOffsetY(), 0, Math.max(0, sh - PANEL_H));
        return new Layout(panelX, panelY, baseX, baseY, sw, sh);
    }

    private record Layout(int panelX, int panelY, int baseX, int baseY, int screenW, int screenH) {}
}
