package dev.maire.nourished.client.screen;

import dev.maire.nourished.config.NourishedClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public final class DietLayout {

    public static final int WIDTH = 316;
    public static final int HEIGHT = 268;
    public static final int SPLIT = 108;
    public static final int PAD = 10;

    public record Layout(
            int panelX,
            int panelY,
            int panelW,
            int panelH,
            int baseX,
            int baseY,
            double scale,
            double recentMealsScale,
            double eatMoreScale
    ) {}

    private DietLayout() {}

    public static Layout compute(Minecraft mc) {
        NourishedClientConfig cc = NourishedClientConfig.get();
        return compute(
                mc,
                cc.dietScale(),
                cc.dietOffsetX(),
                cc.dietOffsetY(),
                cc.recentMealsBoxScale(),
                cc.eatMoreBoxScale()
        );
    }

    public static Layout compute(
            Minecraft mc,
            double scale,
            int offsetX,
            int offsetY,
            double recentMealsScale,
            double eatMoreScale
    ) {
        int panelW = scaledDim(WIDTH, scale);
        int panelH = scaledDim(HEIGHT, scale);
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        int baseX = (sw - panelW) / 2;
        int baseY = (sh - panelH) / 2;
        int panelX = Mth.clamp(baseX + offsetX, 0, Math.max(0, sw - panelW));
        int panelY = Mth.clamp(baseY + offsetY, 0, Math.max(0, sh - panelH));
        return new Layout(
                panelX,
                panelY,
                panelW,
                panelH,
                baseX,
                baseY,
                scale,
                recentMealsScale,
                eatMoreScale
        );
    }

    public static int scaledDim(int base, double scale) {
        return Math.max(1, (int) Math.round(base * scale));
    }

    public static int toScreenX(Layout layout, int localX) {
        return layout.panelX() + (int) Math.round(localX * layout.scale());
    }

    public static int toScreenY(Layout layout, int localY) {
        return layout.panelY() + (int) Math.round(localY * layout.scale());
    }

    public static int toScreenDim(Layout layout, int localDim) {
        return Math.max(1, (int) Math.round(localDim * layout.scale()));
    }
}
