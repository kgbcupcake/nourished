package dev.maire.nourished.client.screen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.maire.nourished.client.NourishedKeys;
import dev.marie.framework.client.MarieClientCache;
import dev.marie.framework.config.FeatureFlagCache;
import dev.marie.framework.tracking.TrackingData;
import dev.maire.nourished.config.NourishedClientConfig;
import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.ui.geometry.Bounds;
import dev.marie.framework.ui.edit.EditModeController;
import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.Theme;
import dev.marie.framework.ui.ThemeKey;
import dev.marie.framework.ui.render.GuiGraphicsRenderContext;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

@ApiStatus.Internal
public class DietScreen extends Screen {

    // ── Panel dimensions (see DietLayout) ─────────────────────────────────────
    private static final int WIDTH       = DietLayout.WIDTH;
    private static final int HEIGHT      = DietLayout.HEIGHT;
    private static final int SPLIT       = DietLayout.SPLIT;
    private static final int PAD         = DietLayout.PAD;

    // ── Right panel row geometry ─────────────────────────────────────────────
    private static final int ROW_STEP    = 26;

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
     * MarieUI edit-mode wrapper + controller — lazily constructed on first entry, not in
     * {@link #init()} (no {@link DietScreenEditTarget} is needed until edit mode is actually
     * entered) and not per-frame (a drag/resize gesture spans many frames; see
     * {@link DietScreenEditTarget}'s class javadoc for why it must be a single long-lived
     * instance). Held on this screen instance specifically because {@link DietScreen} is itself
     * constructed once per screen-open and lives exactly as long as a gesture needs to survive.
     */
    private DietScreenEditTarget marieEditTarget;
    private EditModeController marieEditModeController;

    /** 0..1 fade-in over {@link #FADE_DURATION_SEC}; updated each render from frame delta. */
    private float fadeAlpha;
    private long fadeLastFrameNanos;
    private boolean fadeClockStarted;

    public DietScreen() {
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
        DietLayout.Layout layout = DietScreenEditTarget.resolvedPanelLayout(minecraft);
        leftPos = layout.panelX();
        topPos = layout.panelY();

        visibleBars.clear();
        dragBarFromIndex = null;
        for (String key : NourishedClientConfig.get().effectiveDietBarOrder()) {
            display.putIfAbsent(key, 0f);
            visibleBars.add(key);
        }

    }

    private DietLayout.Layout currentLayout() {
        return DietScreenEditTarget.resolvedPanelLayout(minecraft);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isMouseOverEditModeToggle(DietScreenEditTarget.resolvedPanelLayout(minecraft), mouseX, mouseY)) {
            if (marieEditModeController != null && marieEditModeController.isActive()) {
                marieEditModeController.exit();
            } else {
                marieEditModeController().enter();
            }
            return true;
        }
        if (button == 0 && NourishedClientConfig.get().dietBarDragEnabled()) {
            DietLayout.Layout layout = currentLayout();
            double s = layout.scale();
            int rx = layout.panelX() + (int) Math.round((SPLIT + PAD) * s);
            int y0 = layout.panelY() + (int) Math.round(44 * s);
            int rowStep = Math.max(1, (int) Math.round(ROW_STEP * s));
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
            int rowStep = Math.max(1, (int) Math.round(ROW_STEP * s));
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

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (NourishedKeys.EDIT_DIET_SCREEN.matches(keyCode, scanCode)) {
            marieEditModeController().enter();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * Lazily builds the MarieUI edit-mode wrapper + controller on first entry (see field javadoc
     * for why this can't happen in {@link #init()} or per-frame), then reuses the same instances
     * for the rest of this screen's lifetime.
     */
    private EditModeController marieEditModeController() {
        if (marieEditModeController == null) {
            // marieEditModeController is assigned below, before the exit callback can ever
            // actually run (it's only reachable via a click inside an already-active edit
            // overlay) — safe despite referencing the not-yet-assigned field here.
            marieEditTarget = new DietScreenEditTarget(minecraft, () -> marieEditModeController.exit());
            marieEditModeController = new EditModeController(
                    marieEditTarget,
                    "Drag boxes to reposition, drag corner handles to resize. J or Esc to exit.",
                    NourishedKeys.EDIT_DIET_SCREEN.getKey().getValue(),
                    () -> {}
            );
        }
        return marieEditModeController;
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
        drawPanelViaMarieUI(g, minecraft, pt, data, visibleBars, mx, my);

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        if (showIconTooltips && data != null) {
            drawDietIconTooltips(g, layout, mx, my);
        }
        super.render(g, mx, my, pt);
    }

    /**
     * Renders the Diet Screen panel via the MarieUI Component/Container tree. Position/size come
     * from {@link DietScreenEditTarget#resolvedPanelLayout} — the persisted main-panel drag/resize
     * commit if one exists, otherwise the same centered default as before — so a commit made in
     * edit mode is still visible here after closing and reopening the Diet Screen. MarieUI has no
     * root/screen anchoring resolver of its own yet, so the {@link Bounds} passed to the panel is
     * resolved here rather than by {@link DietPanelContainer#constraint()}, same pattern as the
     * HUD's MarieUI wiring.
     */
    private void drawPanelViaMarieUI(GuiGraphics g, Minecraft mc, float partialTick, TrackingData data, List<String> bars, int mx, int my) {
        DietLayout.Layout resolvedLayout = DietScreenEditTarget.resolvedPanelLayout(mc);
        DietPanelContainer panel = new DietPanelContainer(data, bars, java.util.Collections.unmodifiableMap(display), resolvedLayout);
        RenderContext context = new GuiGraphicsRenderContext(g, mc, Theme.DARK, partialTick);
        Bounds bounds = new Bounds(resolvedLayout.panelX(), resolvedLayout.panelY(), resolvedLayout.panelW(), resolvedLayout.panelH());
        panel.render(context, bounds);

        boolean editModeActive = marieEditModeController != null && marieEditModeController.isActive();
        boolean toggleHovered = isMouseOverEditModeToggle(resolvedLayout, mx, my);
        drawEditModeToggle(context, resolvedLayout, editModeActive, toggleHovered);
    }

    // ── Edit-mode toggle (top-right corner) ─────────────────────────────────
    // Local (pre-scale) geometry, inset from the panel's top-right corner: clear of the
    // centered title (title occupies the middle of the row; this sits far right) and above
    // the divider at local y=26 (see DietPanelContainer). Right margin (6) and top position
    // (8) leave 1-2px of breathing room from the panel's 1px border on both edges.
    private static final int TOGGLE_RIGHT_MARGIN = 6;
    private static final int TOGGLE_HOUSING_W = 10;
    private static final int TOGGLE_HOUSING_H = 16;
    private static final int TOGGLE_HOUSING_TOP = 8;
    private static final int TOGGLE_LIGHT_SIZE = 6;
    private static final int TOGGLE_LIGHT_GAP = 1;
    private static final int TOGGLE_LEVER_INSET = 2;
    private static final int TOGGLE_LEVER_H = 6;

    private static final int COL_TOGGLE_LIGHT_ON = 0xFF2ECC71;
    private static final int COL_TOGGLE_LIGHT_OFF = 0xFFE74C3C;
    private static final int COL_TOGGLE_LIGHT_BORDER = 0xFF101010;
    private static final int COL_TOGGLE_HOUSING_BG = 0xFF1E1E1E;
    private static final int COL_TOGGLE_LEVER = 0xFFB0B0B0;

    static Bounds editModeToggleHousingBounds(DietLayout.Layout layout) {
        int x2 = DietLayout.toScreenX(layout, WIDTH - TOGGLE_RIGHT_MARGIN);
        int w = DietLayout.toScreenDim(layout, TOGGLE_HOUSING_W);
        int h = DietLayout.toScreenDim(layout, TOGGLE_HOUSING_H);
        return new Bounds(x2 - w, DietLayout.toScreenY(layout, TOGGLE_HOUSING_TOP), w, h);
    }

    static Bounds editModeToggleLightBounds(DietLayout.Layout layout, Bounds housing) {
        int size = DietLayout.toScreenDim(layout, TOGGLE_LIGHT_SIZE);
        int gap = DietLayout.toScreenDim(layout, TOGGLE_LIGHT_GAP);
        int x = housing.x() + (housing.width() - size) / 2;
        int y = housing.y() - gap - size;
        return new Bounds(x, y, size, size);
    }

    static boolean isMouseOverEditModeToggle(DietLayout.Layout layout, double mx, double my) {
        Bounds housing = editModeToggleHousingBounds(layout);
        return mx >= housing.x() && my >= housing.y()
                && mx < housing.x() + housing.width() && my < housing.y() + housing.height();
    }

    /**
     * Vertical light-switch: lever sits in the housing's upper half when {@code active}, lower
     * half when not, and the light above it is solid green/red — the primary at-a-glance signal.
     * Both are read directly from {@code active} every call (never a cached toggle boolean), so
     * this can never drift from {@link EditModeController#isActive()}'s live truth.
     */
    static void drawEditModeToggle(RenderContext context, DietLayout.Layout layout, boolean active, boolean hovered) {
        Bounds housing = editModeToggleHousingBounds(layout);
        Bounds light = editModeToggleLightBounds(layout, housing);

        int lightColor = active ? COL_TOGGLE_LIGHT_ON : COL_TOGGLE_LIGHT_OFF;
        context.fillRect(light.x(), light.y(), light.width(), light.height(), COL_TOGGLE_LIGHT_BORDER);
        context.fillRect(light.x() + 1, light.y() + 1, Math.max(0, light.width() - 2), Math.max(0, light.height() - 2), lightColor);

        context.fillRect(housing.x(), housing.y(), housing.width(), housing.height(), COL_TOGGLE_HOUSING_BG);
        int borderColor = hovered
                ? context.theme().color(ThemeKey.BORDER_HOVER)
                : context.theme().color(ThemeKey.BORDER);
        context.drawBorder(housing.x(), housing.y(), housing.width(), housing.height(), 1, borderColor);

        int leverInset = DietLayout.toScreenDim(layout, TOGGLE_LEVER_INSET);
        int leverW = Math.max(1, housing.width() - 2 * leverInset);
        int leverH = DietLayout.toScreenDim(layout, TOGGLE_LEVER_H);
        int leverX = housing.x() + leverInset;
        int upperY = housing.y() + leverInset;
        int lowerY = housing.y() + housing.height() - leverInset - leverH;
        int leverY = active ? upperY : lowerY;
        context.fillRect(leverX, leverY, leverW, leverH, COL_TOGGLE_LEVER);
    }

    private void drawDietIconTooltips(GuiGraphics g, DietLayout.Layout layout, int mx, int my) {
        double s = layout.scale();
        int rx = layout.panelX() + (int) Math.round((SPLIT + PAD) * s);
        int y = layout.panelY() + (int) Math.round((30 + 14) * s);
        int rowStep = Math.max(1, (int) Math.round(ROW_STEP * s));
        int iconSize = Math.max(1, (int) Math.round(20 * s));
        for (String key : visibleBars) {
            if (mx >= rx && mx <= rx + iconSize && my >= y && my <= y + iconSize) {
                g.renderTooltip(font,
                        Component.translatable("nourished.screen.diet.tooltip." + key),
                        mx, my);
                return;
            }
            y += rowStep;
        }
    }

    // ── Logic helpers ────────────────────────────────────────────────────────

    private static TrackingData getClientData() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return null;
        return MarieClientCache.get();
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
