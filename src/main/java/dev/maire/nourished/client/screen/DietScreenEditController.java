package dev.maire.nourished.client.screen;

import dev.maire.nourished.config.NourishedClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

public final class DietScreenEditController {

    private static boolean dietDragging;
    private static boolean dietResizing;
    private static boolean recentMealsResizing;
    private static boolean eatMoreResizing;
    private static int dragGrabOffsetX;
    private static int dragGrabOffsetY;
    private static int dragAnchorBaseX;
    private static int dragAnchorBaseY;
    private static int resizeOriginX;
    private static int resizeOriginY;
    private static double resizeBaseDiagonal;
    private static double resizePreviewScale;
    private static double recentMealsResizePreviewScale;
    private static double eatMoreResizePreviewScale;
    private static double recentMealsResizeBaseDiagonal;
    private static double eatMoreResizeBaseDiagonal;

    private DietScreenEditController() {}

    public static void onEditMousePress(int mx, int my, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        DietLayout.Layout layout = previewLayout(mc);
        DietScreen.SectionBounds sections = DietScreen.computeSectionBounds(layout);

        if (sections.recentMeals() != null && isOverResizeHandle(mx, my, sections.recentMeals())) {
            recentMealsResizing = true;
            dietDragging = false;
            dietResizing = false;
            eatMoreResizing = false;
            resizeOriginX = sections.recentMeals().screenX();
            resizeOriginY = sections.recentMeals().screenY();
            recentMealsResizeBaseDiagonal = Math.hypot(sections.recentMeals().screenW(), sections.recentMeals().screenH());
            recentMealsResizePreviewScale = layout.recentMealsScale();
            return;
        }

        if (sections.eatMore() != null && isOverResizeHandle(mx, my, sections.eatMore())) {
            eatMoreResizing = true;
            dietDragging = false;
            dietResizing = false;
            recentMealsResizing = false;
            resizeOriginX = sections.eatMore().screenX();
            resizeOriginY = sections.eatMore().screenY();
            eatMoreResizeBaseDiagonal = Math.hypot(sections.eatMore().screenW(), sections.eatMore().screenH());
            eatMoreResizePreviewScale = layout.eatMoreScale();
            return;
        }

        if (isOverResizeHandle(mx, my, layout.panelX(), layout.panelY(), layout.panelW(), layout.panelH())) {
            dietResizing = true;
            dietDragging = false;
            recentMealsResizing = false;
            eatMoreResizing = false;
            resizeOriginX = layout.panelX();
            resizeOriginY = layout.panelY();
            resizeBaseDiagonal = Math.hypot(DietLayout.WIDTH, DietLayout.HEIGHT);
            resizePreviewScale = layout.scale();
            return;
        }

        boolean overPanel = mx >= layout.panelX() && my >= layout.panelY()
                && mx < layout.panelX() + layout.panelW() && my < layout.panelY() + layout.panelH();
        if (overPanel) {
            dietDragging = true;
            dietResizing = false;
            recentMealsResizing = false;
            eatMoreResizing = false;
            dragGrabOffsetX = mx - layout.panelX();
            dragGrabOffsetY = my - layout.panelY();
            dragAnchorBaseX = layout.baseX();
            dragAnchorBaseY = layout.baseY();
        }
    }

    public static void onEditMouseRelease(int mx, int my, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        NourishedClientConfig cc = NourishedClientConfig.get();

        if (dietDragging) {
            DietLayout.Layout layout = previewLayout(mc);
            cc.setDietOffsetX(layout.panelX() - dragAnchorBaseX);
            cc.setDietOffsetY(layout.panelY() - dragAnchorBaseY);
            NourishedClientConfig.saveNow();
            dietDragging = false;
            return;
        }

        if (dietResizing) {
            double finalScale = Mth.clamp(resizePreviewScale, 0.5d, 1.5d);
            cc.setDietScale(finalScale);
            DietLayout.Layout scaled = DietLayout.compute(
                    mc,
                    finalScale,
                    cc.dietOffsetX(),
                    cc.dietOffsetY(),
                    cc.recentMealsBoxScale(),
                    cc.eatMoreBoxScale()
            );
            cc.setDietOffsetX(resizeOriginX - scaled.baseX());
            cc.setDietOffsetY(resizeOriginY - scaled.baseY());
            NourishedClientConfig.saveNow();
            dietResizing = false;
            return;
        }

        if (recentMealsResizing) {
            cc.setRecentMealsBoxScale(Mth.clamp(recentMealsResizePreviewScale, 0.5d, 1.5d));
            NourishedClientConfig.saveNow();
            recentMealsResizing = false;
            return;
        }

        if (eatMoreResizing) {
            cc.setEatMoreBoxScale(Mth.clamp(eatMoreResizePreviewScale, 0.5d, 1.5d));
            NourishedClientConfig.saveNow();
            eatMoreResizing = false;
        }
    }

    public static DietLayout.Layout previewLayout(Minecraft mc) {
        NourishedClientConfig cc = NourishedClientConfig.get();
        int[] m = scaledMouse(mc);
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        double scale = cc.dietScale();
        double recentScale = cc.recentMealsBoxScale();
        double eatMoreScale = cc.eatMoreBoxScale();
        int offsetX = cc.dietOffsetX();
        int offsetY = cc.dietOffsetY();

        if (dietResizing) {
            double dist = Math.hypot(m[0] - resizeOriginX, m[1] - resizeOriginY);
            resizePreviewScale = Mth.clamp(dist / Math.max(1.0d, resizeBaseDiagonal), 0.5d, 1.5d);
            scale = resizePreviewScale;
        }
        if (recentMealsResizing) {
            double dist = Math.hypot(m[0] - resizeOriginX, m[1] - resizeOriginY);
            recentMealsResizePreviewScale = Mth.clamp(dist / Math.max(1.0d, recentMealsResizeBaseDiagonal), 0.5d, 1.5d);
            recentScale = recentMealsResizePreviewScale;
        }
        if (eatMoreResizing) {
            double dist = Math.hypot(m[0] - resizeOriginX, m[1] - resizeOriginY);
            eatMoreResizePreviewScale = Mth.clamp(dist / Math.max(1.0d, eatMoreResizeBaseDiagonal), 0.5d, 1.5d);
            eatMoreScale = eatMoreResizePreviewScale;
        }

        DietLayout.Layout layout = DietLayout.compute(mc, scale, offsetX, offsetY, recentScale, eatMoreScale);
        int panelX = layout.panelX();
        int panelY = layout.panelY();
        if (dietResizing) {
            panelX = resizeOriginX;
            panelY = resizeOriginY;
        } else if (dietDragging) {
            panelX = Mth.clamp(m[0] - dragGrabOffsetX, 0, Math.max(0, sw - layout.panelW()));
            panelY = Mth.clamp(m[1] - dragGrabOffsetY, 0, Math.max(0, sh - layout.panelH()));
        }

        return new DietLayout.Layout(
                panelX,
                panelY,
                layout.panelW(),
                layout.panelH(),
                layout.baseX(),
                layout.baseY(),
                scale,
                recentScale,
                eatMoreScale
        );
    }

    public static boolean isDietDragging() {
        return dietDragging;
    }

    public static boolean isDietResizing() {
        return dietResizing;
    }

    public static boolean isRecentMealsResizing() {
        return recentMealsResizing;
    }

    public static boolean isEatMoreResizing() {
        return eatMoreResizing;
    }

    private static boolean isOverResizeHandle(int mx, int my, int panelX, int panelY, int panelW, int panelH) {
        return dev.maire.nourished.client.hud.HudDrawHelpers.isOverResizeHandle(mx, my, panelX, panelY, panelW, panelH);
    }

    private static boolean isOverResizeHandle(int mx, int my, DietScreen.ScreenBox box) {
        return isOverResizeHandle(mx, my, box.screenX(), box.screenY(), box.screenW(), box.screenH());
    }

    private static int[] scaledMouse(Minecraft mc) {
        double s = mc.getWindow().getGuiScale();
        return new int[]{(int) (mc.mouseHandler.xpos() / s), (int) (mc.mouseHandler.ypos() / s)};
    }
}
