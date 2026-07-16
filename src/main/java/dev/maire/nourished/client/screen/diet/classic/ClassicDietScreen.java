package dev.maire.nourished.client.screen.diet.classic;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import dev.maire.nourished.client.screen.diet.dynamic.layout.DietLayout;
import dev.maire.nourished.client.NourishedKeys;
import dev.marie.framework.client.config.state.MarieClientCache;
import dev.marie.framework.tracking.TrackingData;
import dev.maire.nourished.config.NourishedClientConfig;
import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.ui.edit.EditModeController;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Pre-MarieUI Diet Screen, restored verbatim from before the MarieUI-only collapse (see
 * {@code git show a6566c6:.../client/screen/DietScreen.java}), renamed and package-adjusted only.
 * Same raw {@code GuiGraphics}/{@code PoseStack} drawing logic as the historical implementation —
 * only the edit-mode wiring changed, from the deleted {@code DietScreenEditController}/
 * {@code DietScreenEditMode}/{@code DietScreenEditScreen} trio to {@link ClassicDietEditTarget}
 * via the shared marie-ui {@link EditModeController}. This class stays solely responsible for
 * rendering; all drag/resize interaction state lives in {@link ClassicDietEditTarget}.
 */
@ApiStatus.Internal
public class ClassicDietScreen extends Screen {
    // ── Panel dimensions (see DietLayout) ─────────────────────────────────────
    private static final int WIDTH       = DietLayout.WIDTH;
    private static final int HEIGHT      = DietLayout.HEIGHT;
    private static final int SPLIT       = DietLayout.SPLIT;
    private static final int PAD         = DietLayout.PAD;
    private static final float FADE_DURATION_SEC = 0.15f;
    private static final float FADE_TOOLTIP_THRESHOLD = 0.9f;
    /** Bar fill display lerps toward server values over this duration (seconds). */
    private static final float ANIM_DURATION_SEC = 0.3f;
    // ── State ────────────────────────────────────────────────────────────────
    private int leftPos, topPos;
    private final Map<String, Float>     display = new LinkedHashMap<>();
    private final List<String> visibleBars        = new ArrayList<>();
    /** Index of nutrient row whose icon is being dragged; null when not dragging. */
    private Integer dragBarFromIndex;

    /**
     * MarieUI edit-mode wrapper + controller — lazily constructed on first entry, not per-frame (a
     * drag/resize gesture spans many frames; see {@link ClassicDietEditTarget}'s class javadoc).
     */
    private ClassicDietEditTarget classicEditTarget;
    private EditModeController classicEditModeController;

    /** 0..1 fade-in over {@link #FADE_DURATION_SEC}; updated each render from frame delta. */
    private float fadeAlpha;
    private long fadeLastFrameNanos;
    private boolean fadeClockStarted;
    public ClassicDietScreen() {
        super(Component.translatable("nourished.screen.diet"));
        fadeAlpha = 0f;
        fadeClockStarted = false;
    }
    @Override
    public void onClose() {
        dragBarFromIndex = null;
        super.onClose();
    }
    // ── Init ─────────────────────────────────────────────────────────────────
    @Override
    protected void init() {
        DietLayout.Layout layout = DietLayout.compute(minecraft);
        leftPos = layout.panelX();
        topPos = layout.panelY();
        visibleBars.clear();
        dragBarFromIndex = null;
        for (String key : NourishedClientConfig.get().effectiveDietBarOrder()) {
            display.putIfAbsent(key, 0f);
            visibleBars.add(key);
        }
        int closeW = DietLayout.scaledDim(118, layout.scale());
        int closeH = DietLayout.scaledDim(18, layout.scale());
        int closeX = leftPos + DietLayout.scaledDim((WIDTH - 118) / 2, layout.scale());
        int closeY = topPos + DietLayout.scaledDim(HEIGHT - 22, layout.scale());
        addRenderableWidget(
                Button.builder(
                        Component.translatable("nourished.screen.diet.close"),
                        b -> onClose()
                ).bounds(closeX, closeY, closeW, closeH).build()
        );
    }
    private DietLayout.Layout currentLayout() {
        return DietLayout.compute(minecraft);
    }
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && NourishedClientConfig.get().dietBarDragEnabled()) {
            DietLayout.Layout layout = currentLayout();
            double s = layout.scale();
            int rx = layout.panelX() + (int) Math.round((SPLIT + PAD) * s);
            int y0 = layout.panelY() + (int) Math.round(44 * s);
            int rowStep = Math.max(1, (int) Math.round(ClassicDietRightPanel.ROW_STEP * s));
            int iconSize = Math.max(1, (int) Math.round(20 * s));
            for (int i = 0; i < visibleBars.size(); i++) {
                int by = y0 + i * rowStep;
                if (mouseX >= rx && mouseX <= rx + iconSize && mouseY >= by && mouseY <= by + iconSize) {
                    dragBarFromIndex = i;
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragBarFromIndex != null && button == 0 && NourishedClientConfig.get().dietBarDragEnabled()) {
            DietLayout.Layout layout = currentLayout();
            double s = layout.scale();
            int y0 = layout.panelY() + (int) Math.round(44 * s);
            int rowStep = Math.max(1, (int) Math.round(ClassicDietRightPanel.ROW_STEP * s));
            int to = Mth.clamp((int) ((mouseY - y0 + rowStep / 2.0) / rowStep), 0, visibleBars.size() - 1);
            int from = dragBarFromIndex;
            dragBarFromIndex = null;
            if (from >= 0 && from < visibleBars.size() && from != to) {
                String moved = visibleBars.remove(from);
                visibleBars.add(to, moved);
                NourishedClientConfig.get().setDietBarOrder(new ArrayList<>(visibleBars));
                NourishedClientConfig.saveNow();
            }
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
    /**
     * Lazily builds the marie-ui edit-mode wrapper + controller on first entry, then reuses the
     * same instances for the rest of this screen's lifetime — mirrors the dynamic
     * {@code DietScreen}'s {@code marieEditModeController()}.
     */
    private EditModeController classicEditModeController() {
        if (classicEditModeController == null) {
            classicEditTarget = new ClassicDietEditTarget(minecraft);
            classicEditModeController = new EditModeController(
                    classicEditTarget,
                    "Drag the panel to reposition, drag the corner handle to resize. J or Esc to exit.",
                    NourishedKeys.EDIT_DIET_SCREEN.getKey().getValue(),
                    () -> {}
            );
        }
        return classicEditModeController;
    }
    
@Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (NourishedKeys.EDIT_DIET_SCREEN.matches(keyCode, scanCode)) {
            classicEditModeController().enter();
            return true;
        }
        if (NourishedKeys.OPEN_DIET_SCREEN.matches(keyCode, scanCode)) {
            onClose();
            while (NourishedKeys.OPEN_DIET_SCREEN.consumeClick()) {
                // drain queued click so ClientEvents.onClientTick doesn't instantly reopen this screen
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    } 
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragBarFromIndex != null && button == 0) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
    // ── Render ───────────────────────────────────────────────────────────────
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // intentionally empty — suppress vanilla menu blur; panel draws its own backdrop
    }
    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        long now = System.nanoTime();
        if (!fadeClockStarted) {
            fadeLastFrameNanos = now;
            fadeClockStarted = true;
        }
        float dt = (now - fadeLastFrameNanos) / 1_000_000_000f;
        fadeLastFrameNanos = now;
        fadeAlpha = Mth.clamp(fadeAlpha + dt / FADE_DURATION_SEC, 0f, 1f);
        boolean showIconTooltips = fadeAlpha > FADE_TOOLTIP_THRESHOLD;
        RenderSystem.setShaderColor(1f, 1f, 1f, fadeAlpha);
        renderBackground(g, mx, my, pt);
        TrackingData data = getClientData();
        // Animate display values toward target using dt-based lerp (~300ms convergence)
        if (data != null) {
            float animStep = dt <= 0f ? 0f : Math.min(1f, dt / ANIM_DURATION_SEC);
            for (String k : visibleBars) {
                float target = data.values.getOrDefault(k, 0f);
                float cur    = display.getOrDefault(k, 0f);
                display.put(k, cur + (target - cur) * animStep);
            }
        }
        DietLayout.Layout layout = currentLayout();
        drawPanelAt(g, minecraft, layout, data, fadeAlpha, mx, my, visibleBars);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        if (showIconTooltips && data != null) {
            ClassicDietRightPanel.drawDietIconTooltips(g, minecraft, layout, visibleBars, mx, my);
        }
        super.render(g, mx, my, pt);
    }
    private void drawPanelAt(
            GuiGraphics g,
            Minecraft mc,
            DietLayout.Layout layout,
            TrackingData data,
            float panelFadeAlpha,
            int mx,
            int my,
            List<String> bars
    ) {
        leftPos = layout.panelX();
        topPos = layout.panelY();
        int localMx = layout.scale() > 0 ? (int) ((mx - layout.panelX()) / layout.scale()) : mx;
        int localMy = layout.scale() > 0 ? (int) ((my - layout.panelY()) / layout.scale()) : my;
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(layout.panelX(), layout.panelY(), 0);
        pose.scale((float) layout.scale(), (float) layout.scale(), 1f);
        double bgOpacity = NourishedClientConfig.get().dietBackgroundOpacity();
        int outerFill = ClassicDietDrawHelpers.panelColorWithOpacity(ClassicDietDrawHelpers.COL_BG_RGB, bgOpacity);
        ClassicDietDrawHelpers.drawRoundedPanel(g, 0, 0, WIDTH, HEIGHT, outerFill, ClassicDietDrawHelpers.COL_BORDER_LT, ClassicDietDrawHelpers.COL_BORDER);
        String titleText = "☘ Diet ☘";
        int titleW = mc.font.width(titleText);
        g.drawString(mc.font, titleText, (WIDTH / 2) - (titleW / 2), 9, 0xFF9BD36A, false);
        g.fill(SPLIT, 26, SPLIT + 1, HEIGHT - 34, ClassicDietDrawHelpers.COL_DIVIDER);
        if (data == null) {
            g.drawCenteredString(mc.font,
                    Component.translatable("nourished.screen.diet.no_player"),
                    WIDTH / 2, HEIGHT / 2, ClassicDietDrawHelpers.COL_GRAY);
        } else {
            ClassicDietLeftPanel.drawLeftPanel(g, mc, data, 0, 0, layout.recentMealsScale(), layout.eatMoreScale());
            ClassicDietRightPanel.drawRightPanel(g, mc, data, display, panelFadeAlpha, localMx, localMy, 0, 0, bars);
        }
        pose.popPose();
    }
    @Override
    public boolean isPauseScreen() { return false; }
    // ── Logic helpers ────────────────────────────────────────────────────────
    private static TrackingData getClientData() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return null;
        return MarieClientCache.get();
    }

    private static final ClassicDietScreen EDIT_DRAW = new ClassicDietScreen();

    /**
     * Renders the diet panel's content only (no edit-mode chrome — handles/overlay/dashed border
     * are {@link ClassicDietEditTarget}'s responsibility) at an edit-mode-resolved {@code layout}.
     * {@code EDIT_DRAW}'s own {@link #display} map is never populated outside a live {@link
     * #render}, so this always shows raw (unlerped) values, matching the historical edit-preview
     * behavior.
     */
    static void renderForEdit(GuiGraphics g, Minecraft mc, DietLayout.Layout layout, int mx, int my) {
        TrackingData data = getClientData();
        List<String> bars = NourishedClientConfig.get().effectiveDietBarOrder();
        EDIT_DRAW.drawPanelAt(g, mc, layout, data, 1f, mx, my, bars);
    }
}
