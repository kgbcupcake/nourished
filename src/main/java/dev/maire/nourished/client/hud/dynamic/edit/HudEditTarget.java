package dev.maire.nourished.client.hud.dynamic.edit;

import dev.marie.framework.color.MarieColors;
import dev.maire.nourished.client.colors.NourishedColors;
import dev.marie.framework.ui.api.MarieModuleSettings;
import dev.marie.framework.ui.api.MoveDrag;
import dev.marie.framework.client.config.state.MarieClientCache;
import dev.marie.framework.tracking.TrackingData;
import dev.marie.framework.ui.geometry.Anchor;
import dev.marie.framework.ui.geometry.Bounds;
import dev.marie.framework.ui.component.AutoGrowPanelContainer;
import dev.marie.framework.ui.component.ComponentState;
import dev.marie.framework.ui.component.Constraint;
import dev.marie.framework.ui.edit.ContentScaleController;
import dev.marie.framework.ui.edit.DraggableResizable;
import dev.marie.framework.ui.api.MarieScaleConfig;
import dev.marie.framework.ui.api.SnapRegistry;
import dev.marie.framework.ui.geometry.Insets;
import dev.marie.framework.ui.component.MarieComponent;
import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.geometry.Size;
import dev.marie.framework.ui.render.GuiGraphicsRenderContext;
import dev.marie.framework.ui.scaleconfig.ScaleConfigEntry;
import dev.marie.framework.ui.scaleconfig.ScaleConfigPanel;
import dev.maire.nourished.client.UiStatePersistence;
import dev.maire.nourished.client.hud.NourishedHUD;
import dev.maire.nourished.client.hud.classic.ClassicHudPanelRenderer;
import dev.maire.nourished.client.hud.dynamic.HudDrawHelpers;
import dev.maire.nourished.client.hud.dynamic.layout.HudLayout;
import dev.maire.nourished.client.hud.dynamic.options.HudOptionsPanel;
import dev.maire.nourished.client.hud.dynamic.modules.NutrientPanelContainer;
import dev.maire.nourished.client.hud.dynamic.visibility.HudVisibility;
import dev.maire.nourished.config.NourishedClientConfig;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Map;

/**
 * Single stable {@link MarieComponent} target for the HUD's MarieUI edit mode. Unlike
 * {@link dev.maire.nourished.client.screen.diet.dynamic.edit.DietScreenEditTarget} this wraps exactly one
 * draggable/resizable region (the whole nutrient panel), so it holds a single
 * {@link DraggableResizable} rather than one per sub-region.
 *
 * <p>Dragging is purely spatial now, on every edge and both corners: content size (icon/bar/text
 * scale) always comes from {@link HudLayout#compute(Minecraft, List)}'s own {@code cc.hudScale()} —
 * the same single config value classic mode already renders from — never from the on-screen box's
 * dimensions. Resizing the box only changes how much reserved blank room surrounds that
 * fixed-size content: {@code leftMargin} on the left (grown only by a LEFT-edge/bottom-left-corner
 * drag), and free trailing space on the right/bottom for any other resize, since content no longer
 * stretches to fill the box (see {@code NutrientBarComponent#constraint}'s {@code
 * expandHorizontal = false} and {@link HudLayout.Layout#naturalPanelW}). Width and height each
 * follow {@link AutoGrowPanelContainer}'s existing manual/auto split (the same one {@code
 * DietPanelLayoutResolver} uses): auto-sized to content's natural size until the player explicitly
 * drags a handle affecting that axis, at which point {@link #commit} marks it manual and the
 * dragged size is respected — not silently overwritten — until they resize that axis again.
 *
 * <p>Position AND size are persisted independently via {@link UiStatePersistence} (same
 * {@link ComponentState} model {@code DietScreenPersistence} uses for the Diet Screen's panel/
 * sub-boxes).
 */
public final class HudEditTarget implements MarieComponent {

    private static final String ID = "nourished.hud.editwrapper";
    private static final String PANEL_ID = "nourished.hud.panel";
    private static final String CONTENT_OFFSET_ID = "nourished.hud.contentOffset";

    /** Accent used for the "Move Text and Icons" live drag affordance — this panel draws no title text of its own to already have an established accent color, unlike {@code CalorieHudScreen}/{@code ActivityLogHudPanel}. */
    private static int contentOutlineColor() {
        return MarieColors.resolveColor(NourishedColors.EDIT_OUTLINE);
    }

    /** How much bigger than content's natural size the box may be dragged, on either axis. */
    private static final double MAX_MARGIN_MULTIPLIER = 5.0d;

    private final Minecraft mc;
    private final DraggableResizable panelDrag;

    /** Whether a content-move drag (see {@link #moveContentEnabled}) is currently in progress. */
    /** Grab state for a "Move Text and Icons" or "Move Bars" drag (see {@link MoveDrag}). */
    private final MoveDrag moveDrag = new MoveDrag();

    /**
     * The content's offset from where it would otherwise sit — a plain persisted translation, not
     * a separately hit-testable box, same {@code contentOffsetX}/{@code contentOffsetY} pattern
     * {@code CalorieHudScreen}/{@code ActivityLogHudPanel} use and for the same reason: this panel
     * has nothing else in it besides its own content, so a separately draggable sub-region would
     * compete with the panel's own drag for every click inside it. {@link #moveContentEnabled}
     * disambiguates instead.
     */
    private int contentOffsetX;
    private int contentOffsetY;

    /**
     * Editor for this panel's persisted contentScale/paddingScale — same pattern as {@code
     * CalorieHudScreen}/{@code ActivityLogHudPanel}'s own single-entry {@code ScaleConfigPanel}, just
     * owned here instead since this class (not {@code NourishedHUD}) is the actual edit-mode {@link
     * MarieComponent}. Editing happens only through this card.
     */
    private final ScaleConfigPanel scaleConfigPanel = MarieScaleConfig.create(
            List.of(new ScaleConfigEntry(PANEL_ID, Component.translatable("nourished.hud.nutrientPanel.label"))
                    .withContent(HudOptionsPanel.build(PANEL_ID, this::resetContentOffset))),
            UiStatePersistence.get(), Anchor.TOP_RIGHT);
    private boolean scaleConfigVisible;

    public HudEditTarget(Minecraft mc) {
        this.mc = mc;

        List<String> keys = currentVisibleKeys();
        if (keys.isEmpty()) {
            keys = NutrientRegistry.getKeys();
        }
        HudLayout.Layout natural = HudLayout.compute(mc, keys);

        panelDrag = new DraggableResizable(this, constraintFor(natural), (target, bounds) -> commit(bounds));
        panelDrag.setSnapRegistryId(PANEL_ID);
        SnapRegistry.register(PANEL_ID, () -> resolvedBounds(this.mc, currentVisibleKeysOrFallback()));

        UiStatePersistence.get().load(CONTENT_OFFSET_ID).ifPresent(state -> {
            contentOffsetX = state.x();
            contentOffsetY = state.y();
        });
    }

    /** "Reset Positions" callback: puts this panel's text offset back to zero and saves it (the icon and bar offsets are reset by MariesLib). */
    private void resetContentOffset() {
        contentOffsetX = 0;
        contentOffsetY = 0;
        persistContentOffset();
    }

    /** Persists {@link #contentOffsetX}/{@link #contentOffsetY} — {@code width}/{@code height}/the manual-size and scale fields are unused for this key. */
    private void persistContentOffset() {
        UiStatePersistence.get().save(CONTENT_OFFSET_ID, new ComponentState(contentOffsetX, contentOffsetY, 0, 0, false, false, false, 0));
        cachedContentOffsetX = contentOffsetX;
        cachedContentOffsetY = contentOffsetY;
    }

    /** The "Move Text and Icons" toggle's live state, owned by {@link #scaleConfigPanel} (its own editor window, under Padding) rather than a button on this panel itself. */
    private boolean moveContentEnabled() {
        return scaleConfigPanel.isMoveContentEnabled(PANEL_ID);
    }

    /** The "Move Bars" toggle's live state — see {@link MarieModuleSettings#isMoveBarsEnabled}. */
    private boolean moveBarsEnabled() {
        return MarieModuleSettings.isMoveBarsEnabled(UiStatePersistence.get(), PANEL_ID);
    }

    /**
     * {@code panelDrag}'s min/preferred/max clamp, rebuilt from {@code natural} — never cached past
     * a single call. {@code natural} depends on {@code cc.hudScale()}/{@code cc.hudBarWidth()}/
     * {@code cc.hudVerticalLayout()}/visible-nutrient count, any of which the player can change via
     * the config screen while this edit target (a session-lifetime singleton, unlike {@code
     * DietScreenEditTarget} which is rebuilt each time the Diet Screen reopens) is still alive.
     * {@link #render} calls this every frame — same as {@code DietScreenEditTarget}'s own drag
     * trackers — so {@code panelDrag}'s clamp never goes stale the way {@link DraggableResizable
     * #setConstraint}'s own javadoc warns against.
     */
    private static Constraint constraintFor(HudLayout.Layout natural) {
        return new Constraint(
                new Size(natural.panelW(), natural.panelH()),
                new Size(AutoGrowPanelContainer.MIN_COLLAPSED_SIZE, AutoGrowPanelContainer.MIN_COLLAPSED_SIZE),
                new Size((int) (natural.panelW() * MAX_MARGIN_MULTIPLIER), (int) (natural.panelH() * MAX_MARGIN_MULTIPLIER)),
                false, false, true, true,
                Anchor.TOP_LEFT, Insets.NONE, Insets.NONE
        );
    }

    /** Shows this panel's {@link ScaleConfigPanel} alongside edit mode — called from both the H-keybind path ({@code NourishedHUD#enterEditModeWithScaleConfig}) and the "edit all HUDs" group entry ({@code NourishedHUD#showScaleConfigOnGroupEntry}). Never reset back to false on exit, same as {@code CalorieHudScreen}/{@code ActivityLogHudPanel}. */
    public void setScaleConfigVisible(boolean visible) {
        this.scaleConfigVisible = visible;
    }

    /**
     * This panel's persisted text-scale multiplier, read directly off the same {@link
     * UiStatePersistence} record {@link #commit} writes position/size into — no {@link
     * ContentScaleController} instance is needed here, only {@link
     * ContentScaleController#resolveContentScale}'s static pass-through at render time.
     */
    public static double persistedContentScale() {
        return UiStatePersistence.get().load(PANEL_ID).map(ComponentState::contentScale).orElse(ComponentState.DEFAULT_CONTENT_SCALE);
    }

    /** This panel's persisted padding multiplier — see {@link #persistedContentScale()}. */
    public static double persistedPaddingScale() {
        return UiStatePersistence.get().load(PANEL_ID).map(ComponentState::paddingScale).orElse(ComponentState.DEFAULT_PADDING_SCALE);
    }

    /** Same "empty visible keys -> fall back to every registered nutrient" reasoning the constructor uses above, so the {@link SnapRegistry} bounds supplier never resolves an empty-key layout while other components still expect this panel's real on-screen box. */
    private static List<String> currentVisibleKeysOrFallback() {
        List<String> keys = currentVisibleKeys();
        return keys.isEmpty() ? NutrientRegistry.getKeys() : keys;
    }

    /**
     * Resolves the HUD panel's current full {@link HudLayout.Layout}: persisted independent
     * position/size if the user has committed a main-panel drag/resize (via {@link #commit}),
     * otherwise today's content-driven default. Content-scale fields (barW, rowH, iconSize,
     * labelScale, scale, etc.) always come from {@code natural} — {@code cc.hudScale()}-derived,
     * never from the box — so they're identical whether or not a resize was ever committed.
     *
     * <p>Width and height each auto-follow {@code natural}'s content-driven size unless the player
     * explicitly resized that axis ({@link AutoGrowPanelContainer}'s manual/auto split, same one
     * {@code DietPanelLayoutResolver} uses) — this is what lets a third-party mod register more
     * nutrients via {@link dev.maire.nourished.api.NourishedAPI} and have the box grow to fit them
     * instead of clipping into a stale persisted size the player never touched.
     *
     * <p>Called by both the normal (non-edit) HUD render path and this class's own edit-mode
     * rendering, so a committed resize is reflected consistently everywhere — not just while
     * {@link HudEditTarget} itself is active.
     */
    public static HudLayout.Layout resolvedLayout(Minecraft mc, List<String> keys) {
        HudLayout.Layout natural = HudLayout.compute(mc, keys);
        return UiStatePersistence.get().load(PANEL_ID)
                .map(state -> {
                    AutoGrowPanelContainer.ManualOverride override =
                            new AutoGrowPanelContainer.ManualOverride(state.widthManual(), state.heightManual());
                    int width = AutoGrowPanelContainer.resolveWidth(override, state.width(), natural.panelW());
                    // A manually shrunk height always applies (the rows past it scroll). A manually
                    // enlarged one only applies while every bar is showing; with just a subset visible
                    // (reveal-on-gain after a meal, zero bars hidden) the box is capped at the natural
                    // height so a full-size box never pops up around one or two bars.
                    boolean allBarsVisible = keys.size() >= NourishedClientConfig.get().effectiveDietBarOrder().size();
                    int manualHeight = AutoGrowPanelContainer.resolveHeight(override, state.height(), natural.panelH());
                    int height = allBarsVisible ? manualHeight : Math.min(manualHeight, natural.panelH());
                    return new HudLayout.Layout(
                            state.x(), state.y(), width, height,
                            natural.baseX(), natural.baseY(),
                            natural.barW(), natural.rowH(), natural.iconSize(), natural.maxLabelSw(),
                            natural.scaledPad(), natural.labelScale(), natural.scale(), natural.verticalLayout(),
                            natural.verticalBarW(), natural.verticalBarH(), natural.verticalColumnW(),
                            natural.panelW(), natural.panelH(), state.leftMargin(),
                            persistedContentOffsetX(), persistedContentOffsetY()
                    );
                })
                .orElse(natural);
    }

    /** Only grown/shrunk by a left-edge (or bottom-left-corner) gesture, so content stays put on screen — same rule as the Calorie History and Activity Log boxes, via {@link AutoGrowPanelContainer#leftMarginAfterResize}. */
    private static int persistedLeftMargin() {
        return UiStatePersistence.get().load(PANEL_ID)
                .map(ComponentState::leftMargin)
                .orElse(0);
    }

    /** Committed "Move Text and Icons" offset — see {@link #contentOffsetX}. */
    private static int persistedContentOffsetX() {
        ensureContentOffsetCacheLoaded();
        return cachedContentOffsetX;
    }

    /** Committed "Move Text and Icons" offset — see {@link #contentOffsetY}. */
    private static int persistedContentOffsetY() {
        ensureContentOffsetCacheLoaded();
        return cachedContentOffsetY;
    }

    private static void ensureContentOffsetCacheLoaded() {
        if (contentOffsetCacheLoaded) {
            return;
        }
        contentOffsetCacheLoaded = true;
        UiStatePersistence.get().load(CONTENT_OFFSET_ID).ifPresent(state -> {
            cachedContentOffsetX = state.x();
            cachedContentOffsetY = state.y();
        });
    }

    private static int cachedContentOffsetX;
    private static int cachedContentOffsetY;
    private static boolean contentOffsetCacheLoaded;

    /**
     * How much of the content's own origin (its icon corner) must stay inside the panel on the far
     * edge — not the content's whole footprint. {@link #clampContentOffsetX}/{@link
     * #clampContentOffsetY} clamp only the origin into the panel's interior, using the box's own
     * live size (so a bigger box gives more room to drag, all the way to its far edge); anything
     * past the panel's own edge is invisible via {@code NutrientPanelContainer}/{@code
     * ClassicHudPanelRenderer}'s clip/scissor, not blocked from being dragged there in the first
     * place.
     */
    private static final int MIN_VISIBLE_CONTENT = 14;

    /** Clamps a candidate {@link #contentOffsetX} so the content's own origin can't be dragged past the panel's edges — see {@link #MIN_VISIBLE_CONTENT}. */
    private static int clampContentOffsetX(int offsetX, Bounds panelBounds, int leftMargin) {
        int pad = Math.round(ContentScaleController.resolvePadding(HudDrawHelpers.PANEL_PAD * persistedPaddingScale()));
        int minOffset = -(pad + leftMargin);
        int maxOffset = Math.max(minOffset, panelBounds.width() - pad - leftMargin - MIN_VISIBLE_CONTENT);
        return Math.min(maxOffset, Math.max(minOffset, offsetX));
    }

    /** Clamps a candidate {@link #contentOffsetY} the same way {@link #clampContentOffsetX} does for X. */
    private static int clampContentOffsetY(int offsetY, Bounds panelBounds) {
        int pad = Math.round(ContentScaleController.resolvePadding(HudDrawHelpers.PANEL_PAD * persistedPaddingScale()));
        int minOffset = -pad;
        int maxOffset = Math.max(minOffset, panelBounds.height() - pad - MIN_VISIBLE_CONTENT);
        return Math.min(maxOffset, Math.max(minOffset, offsetY));
    }

    /** Resolves the HUD panel's current on-screen bounds — see {@link #resolvedLayout}. */
    static Bounds resolvedBounds(Minecraft mc, List<String> keys) {
        HudLayout.Layout layout = resolvedLayout(mc, keys);
        return new Bounds(layout.panelX(), layout.panelY(), layout.panelW(), layout.panelH());
    }

    private static AutoGrowPanelContainer.ManualOverride existingManualOverride() {
        return UiStatePersistence.get().load(PANEL_ID)
                .map(s -> new AutoGrowPanelContainer.ManualOverride(s.widthManual(), s.heightManual()))
                .orElse(AutoGrowPanelContainer.ManualOverride.NONE);
    }

    private void commit(Bounds bounds) {
        List<String> keys = currentVisibleKeysOrFallback();
        if (keys.isEmpty()) {
            return;
        }
        // Read before this commit's own save below overwrites it.
        int leftMargin = AutoGrowPanelContainer.leftMarginAfterResize(persistedLeftMargin(),
                resolvedBounds(mc, keys).width(), bounds.width(), panelDrag.lastCommitWasLeftEdge());
        AutoGrowPanelContainer.ManualOverride existing = existingManualOverride();
        AutoGrowPanelContainer.ManualOverride override = AutoGrowPanelContainer.withCommit(existing, panelDrag);
        // Carries forward the existing persisted contentScale/paddingScale so a drag/resize commit
        // never resets the user's text-scale or padding adjustment — same fix CalorieHudScreen/
        // ActivityLogHudPanel's own commit() already applies.
        double contentScale = persistedContentScale();
        double paddingScale = persistedPaddingScale();
        UiStatePersistence.get().save(PANEL_ID, new ComponentState(
                bounds.x(), bounds.y(), bounds.width(), bounds.height(), false,
                override.widthManual(), override.heightManual(), leftMargin, contentScale, paddingScale));
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Constraint constraint() {
        // Unused: DraggableResizable clamps against the Constraint passed to its own constructor,
        // never against target.constraint() — same as DietScreenEditTarget's constraint() override.
        return Constraint.preferred(0, 0);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (scaleConfigVisible && scaleConfigPanel.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        List<String> keys = currentVisibleKeysOrFallback();
        if (keys.isEmpty()) {
            return false;
        }
        Bounds bounds = resolvedBounds(mc, keys);
        MoveDrag.Mode mode = MarieModuleSettings.activeMoveMode(UiStatePersistence.get(), PANEL_ID);
        if (mode != null && bounds.contains((int) mouseX, (int) mouseY)) {
            switch (mode) {
                case TEXT -> moveDrag.start(mode, mouseX, mouseY, contentOffsetX, contentOffsetY);
                case ICONS -> moveDrag.start(mode, mouseX, mouseY,
                        MarieModuleSettings.iconOffsetX(UiStatePersistence.get(), PANEL_ID), MarieModuleSettings.iconOffsetY(UiStatePersistence.get(), PANEL_ID));
                case BARS -> moveDrag.start(mode, mouseX, mouseY,
                        MarieModuleSettings.barOffsetX(UiStatePersistence.get(), PANEL_ID), MarieModuleSettings.barOffsetY(UiStatePersistence.get(), PANEL_ID));
                case ALL -> moveDrag.startAll(mouseX, mouseY, contentOffsetX, contentOffsetY,
                        MarieModuleSettings.iconOffsetX(UiStatePersistence.get(), PANEL_ID), MarieModuleSettings.iconOffsetY(UiStatePersistence.get(), PANEL_ID),
                        MarieModuleSettings.barOffsetX(UiStatePersistence.get(), PANEL_ID), MarieModuleSettings.barOffsetY(UiStatePersistence.get(), PANEL_ID));
            }
            return true;
        }
        return panelDrag.mouseClicked((int) mouseX, (int) mouseY, bounds);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scaleConfigVisible && scaleConfigPanel.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        List<String> keys = currentVisibleKeysOrFallback();
        if (scrollY == 0 || keys.isEmpty()) {
            return false;
        }
        HudLayout.Layout layout = resolvedLayout(mc, keys);
        if (!new Bounds(layout.panelX(), layout.panelY(), layout.panelW(), layout.panelH()).contains((int) mouseX, (int) mouseY)) {
            return false;
        }
        int maxScroll = maxScroll(keys.size(), layout);
        if (maxScroll <= 0) {
            return false;
        }
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) Math.signum(scrollY)));
        return true;
    }

    /** First nutrient row shown while the box is too short for all of them — set by the mouse wheel in edit mode, like {@code CalorieHudScreen}'s row scroll, and honored by the in-game HUD too. */
    private static int scrollOffset;
    /** Row count behind the last {@link #scrolledKeys} call, for {@link #drawScrollIndicator}. */
    private static int lastTotalRows;
    private static int lastCapacity;

    /** How many rows fit in the box (at least one); a row counts once half of it is visible, since the panel clip cuts the rest. */
    private static int rowCapacity(HudLayout.Layout layout) {
        int pad = Math.round(ContentScaleController.resolvePadding(HudDrawHelpers.PANEL_PAD * persistedPaddingScale()));
        int availableH = layout.panelH() - 2 * pad;
        return Math.max(1, (availableH + HudDrawHelpers.ROW_GAP + layout.rowH() / 2) / (layout.rowH() + HudDrawHelpers.ROW_GAP));
    }

    private static int maxScroll(int rowCount, HudLayout.Layout layout) {
        return layout.verticalLayout() ? 0 : Math.max(0, rowCount - rowCapacity(layout));
    }

    /** The rows that fit in {@code layout}'s box from {@link #scrollOffset} on — every row when they all fit (and always in the vertical layout, whose columns are not scrolled). Used by every path that draws this panel so they agree. */
    public static List<String> scrolledKeys(List<String> keys, HudLayout.Layout layout) {
        int maxScroll = maxScroll(keys.size(), layout);
        scrollOffset = Math.min(scrollOffset, maxScroll);
        lastTotalRows = keys.size();
        lastCapacity = maxScroll == 0 ? keys.size() : rowCapacity(layout);
        if (maxScroll == 0) {
            return keys;
        }
        return keys.subList(scrollOffset, Math.min(keys.size(), scrollOffset + lastCapacity));
    }

    /** Thin track and thumb on the box's right inner edge, only while some rows are scrolled out of view — same look as the Calorie History box. */
    public static void drawScrollIndicator(RenderContext context, Bounds bounds) {
        if (lastTotalRows <= lastCapacity) {
            return;
        }
        int trackH = Math.max(1, bounds.height() - 4);
        int thumbH = Math.max(4, trackH * lastCapacity / lastTotalRows);
        int maxScroll = lastTotalRows - lastCapacity;
        int thumbY = bounds.y() + 2 + (trackH - thumbH) * scrollOffset / maxScroll;
        int trackX = bounds.x() + bounds.width() - 3;
        context.fillRect(trackX, bounds.y() + 2, 2, trackH, context.theme().color(dev.marie.framework.ui.ThemeKey.BAR_BACKGROUND));
        context.fillRect(trackX, thumbY, 2, thumbH, contentOutlineColor());
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (scaleConfigVisible && scaleConfigPanel.mouseDragged(mouseX, mouseY, button)) {
            return true;
        }
        if (moveDrag.isActive()) {
            List<String> keys = currentVisibleKeysOrFallback();
            if (!keys.isEmpty()) {
                Bounds bounds = resolvedBounds(mc, keys);
                int x = clampContentOffsetX(moveDrag.offsetX(mouseX), bounds, persistedLeftMargin());
                int y = clampContentOffsetY(moveDrag.offsetY(mouseY), bounds);
                switch (moveDrag.mode()) {
                    case TEXT -> {
                        contentOffsetX = x;
                        contentOffsetY = y;
                    }
                    case ICONS -> MarieModuleSettings.setIconOffset(UiStatePersistence.get(), PANEL_ID, x, y);
                    case BARS -> MarieModuleSettings.setBarOffset(UiStatePersistence.get(), PANEL_ID, x, y);
                    case ALL -> {
                        // One drag shifts all three offsets by the same amount from where each started.
                        int dx = moveDrag.offsetX(mouseX);
                        int dy = moveDrag.offsetY(mouseY);
                        contentOffsetX = clampContentOffsetX(moveDrag.baseX(MoveDrag.Mode.TEXT) + dx, bounds, persistedLeftMargin());
                        contentOffsetY = clampContentOffsetY(moveDrag.baseY(MoveDrag.Mode.TEXT) + dy, bounds);
                        MarieModuleSettings.setIconOffset(UiStatePersistence.get(), PANEL_ID, clampContentOffsetX(moveDrag.baseX(MoveDrag.Mode.ICONS) + dx, bounds, persistedLeftMargin()), clampContentOffsetY(moveDrag.baseY(MoveDrag.Mode.ICONS) + dy, bounds));
                        MarieModuleSettings.setBarOffset(UiStatePersistence.get(), PANEL_ID, clampContentOffsetX(moveDrag.baseX(MoveDrag.Mode.BARS) + dx, bounds, persistedLeftMargin()), clampContentOffsetY(moveDrag.baseY(MoveDrag.Mode.BARS) + dy, bounds));
                    }
                }
            }
            return true;
        }
        if (panelDrag.isDragging() || panelDrag.isResizing()) {
            panelDrag.mouseDragged((int) mouseX, (int) mouseY);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (scaleConfigVisible && scaleConfigPanel.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        if (moveDrag.isActive()) {
            MoveDrag.Mode mode = moveDrag.mode();
            moveDrag.stop();
            switch (mode) {
                case TEXT -> persistContentOffset();
                case ICONS -> MarieModuleSettings.commitIconOffset(UiStatePersistence.get(), PANEL_ID);
                case BARS -> MarieModuleSettings.commitBarOffset(UiStatePersistence.get(), PANEL_ID);
                case ALL -> {
                    persistContentOffset();
                    MarieModuleSettings.commitIconOffset(UiStatePersistence.get(), PANEL_ID);
                    MarieModuleSettings.commitBarOffset(UiStatePersistence.get(), PANEL_ID);
                }
            }
            return true;
        }
        boolean any = panelDrag.isDragging() || panelDrag.isResizing();
        panelDrag.mouseReleased((int) mouseX, (int) mouseY);
        return any;
    }

    @Override
    public void render(RenderContext context, Bounds ignoredBounds) {
        List<String> keys = currentVisibleKeysOrFallback();
        if (keys.isEmpty()) {
            return;
        }

        panelDrag.setConstraint(constraintFor(HudLayout.compute(mc, keys)));

        int[] mouse = scaledMouse(mc);
        Bounds defaultBounds = resolvedBounds(mc, keys);
        Bounds bounds = liveOrDefault(mouse[0], mouse[1], defaultBounds);

        // Offsets are clamped only while dragging (see mouseDragged), never here: clamping at draw time
        // rewrote the shared offsets against whatever the box measured this frame, so the edit preview
        // and the in-game HUD (which never re-clamped) drew the same content in different places, and a
        // box collapsed to a sliver would permanently squash them. The panel clip hides any overflow.

        Map<String, Float> displayValues = NourishedHUD.currentDisplayValues();
        HudLayout.Layout matchedLayout = matchedLayoutFor(keys, bounds);
        if (NourishedClientConfig.get().hudClassicMode() && context instanceof GuiGraphicsRenderContext guiContext) {
            TrackingData data = MarieClientCache.get();
            ClassicHudPanelRenderer.drawPanel(
                    guiContext.graphics(), mc, data, scrolledKeys(keys, matchedLayout), matchedLayout, bounds.x(), bounds.y(), displayValues
            );
        } else {
            NutrientPanelContainer panel = new NutrientPanelContainer(scrolledKeys(keys, matchedLayout), matchedLayout, displayValues);
            panel.render(context, bounds);
        }

        boolean moveTextMode = moveContentEnabled();
        boolean moveBarsMode = moveBarsEnabled();
        boolean moveIconsMode = MarieModuleSettings.isMoveIconsEnabled(UiStatePersistence.get(), PANEL_ID);
        boolean moveAllMode = MarieModuleSettings.isMoveAllEnabled(UiStatePersistence.get(), PANEL_ID);
        if (moveTextMode || moveBarsMode || moveIconsMode || moveAllMode) {
            // Dashed rather than a solid glow outline — a live drag affordance shown only while a
            // move toggle is active, not a persistent separately-hit-tested box. Text mode wraps the
            // name column, icons mode the icon column, bars mode the bar+percentage part, each a few
            // pixels further out so it doesn't overlap them (the vertical layout has no such split, so
            // it wraps the whole content).
            int pad = Math.round(ContentScaleController.resolvePadding(HudDrawHelpers.PANEL_PAD * persistedPaddingScale()));
            int contentW = Math.max(0, matchedLayout.naturalPanelW() - 2 * pad);
            int contentH = Math.max(0, matchedLayout.naturalPanelH() - 2 * pad);
            boolean vertical = matchedLayout.verticalLayout();
            int iconW = matchedLayout.iconSize();
            int labelsW = matchedLayout.maxLabelSw();
            int contentX = bounds.x() + pad + matchedLayout.leftMargin();
            int contentY = bounds.y() + pad;
            if (moveAllMode) {
                context.drawDashedBorder(contentX + matchedLayout.contentOffsetX() - 3, contentY + matchedLayout.contentOffsetY() - 3,
                        contentW + 6, contentH + 6, contentOutlineColor());
            } else if (moveTextMode) {
                int start = vertical ? 0 : iconW + HudDrawHelpers.ICON_LABEL_GAP;
                context.drawDashedBorder(contentX + start + matchedLayout.contentOffsetX() - 3, contentY + matchedLayout.contentOffsetY() - 3,
                        (vertical ? contentW : labelsW) + 6, contentH + 6, contentOutlineColor());
            } else if (moveIconsMode) {
                context.drawDashedBorder(contentX + MarieModuleSettings.iconOffsetX(UiStatePersistence.get(), PANEL_ID) - 3, contentY + MarieModuleSettings.iconOffsetY(UiStatePersistence.get(), PANEL_ID) - 3,
                        (vertical ? contentW : iconW) + 6, contentH + 6, contentOutlineColor());
            } else {
                int barsStart = vertical ? 0 : iconW + HudDrawHelpers.ICON_LABEL_GAP + labelsW + HudDrawHelpers.LABEL_BAR_GAP;
                context.drawDashedBorder(contentX + barsStart + MarieModuleSettings.barOffsetX(UiStatePersistence.get(), PANEL_ID) - 3, contentY + MarieModuleSettings.barOffsetY(UiStatePersistence.get(), PANEL_ID) - 3,
                        Math.max(0, contentW - barsStart) + 6, contentH + 6, contentOutlineColor());
            }
        } else {
            Bounds handle = DraggableResizable.handleBounds(bounds);
            context.drawResizeHandle(handle.x(), handle.y(), panelDrag.isHandleHovered(mouse[0], mouse[1], bounds), panelDrag.isHandleActive());
            Bounds handleBL = DraggableResizable.handleBoundsBottomLeft(bounds);
            context.drawResizeHandle(handleBL.x(), handleBL.y(), panelDrag.isHandleBottomLeftHovered(mouse[0], mouse[1], bounds), panelDrag.isBottomLeftCornerActive());
            for (DraggableResizable.Edge edge : DraggableResizable.Edge.values()) {
                Bounds strip = DraggableResizable.edgeHandleBounds(bounds, edge);
                context.drawEdgeHandle(strip.x(), strip.y(), strip.width(), strip.height(), mouse[0], mouse[1],
                        panelDrag.isEdgeHovered(mouse[0], mouse[1], bounds, edge), panelDrag.isEdgeActive(edge));
            }
        }

        if (scaleConfigVisible) {
            scaleConfigPanel.render(context, new Bounds(0, 0, context.screenWidth(), context.screenHeight()));
        }
    }

    private Bounds liveOrDefault(int mx, int my, Bounds fallback) {
        if (panelDrag.isDragging() || panelDrag.isResizing()) {
            Bounds preview = panelDrag.mouseDragged(mx, my);
            if (preview != null) {
                return preview;
            }
        }
        return fallback;
    }

    /**
     * Rebuilds a full {@link HudLayout.Layout} for the live preview {@code bounds}, with position
     * AND size pinned to {@code bounds} itself rather than anchor/formula-recomputed — same
     * approach as {@code DietScreenEditTarget#matchedLayoutFor}, so the live preview rectangle and
     * the content drawn inside it always agree. Content-scale fields come from {@code natural}
     * ({@code cc.hudScale()}-only) regardless of {@code bounds} — dragging never rescales content,
     * only {@link #resolvedLayout}'s doc explains why width/height are still independently tracked.
     */
    private HudLayout.Layout matchedLayoutFor(List<String> keys, Bounds bounds) {
        int leftMargin = AutoGrowPanelContainer.leftMarginAfterResize(persistedLeftMargin(),
                resolvedBounds(mc, keys).width(), bounds.width(), panelDrag.isLeftEdgeGestureActive());
        HudLayout.Layout natural = HudLayout.compute(mc, keys);
        return new HudLayout.Layout(
                bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                natural.baseX(), natural.baseY(),
                natural.barW(), natural.rowH(), natural.iconSize(), natural.maxLabelSw(),
                natural.scaledPad(), natural.labelScale(), natural.scale(), natural.verticalLayout(),
                natural.verticalBarW(), natural.verticalBarH(), natural.verticalColumnW(),
                natural.panelW(), natural.panelH(), leftMargin,
                contentOffsetX, contentOffsetY
        );
    }

    private static List<String> currentVisibleKeys() {
        List<String> keys = NutrientRegistry.getKeys();
        if (keys.isEmpty()) {
            return keys;
        }
        TrackingData data = MarieClientCache.get();
        if (data == null) {
            return List.of();
        }
        NourishedClientConfig cc = NourishedClientConfig.get();
        return HudVisibility.visibleKeys(data, cc.effectiveDietBarOrder(), cc);
    }

    private static int[] scaledMouse(Minecraft mc) {
        double s = mc.getWindow().getGuiScale();
        return new int[]{(int) (mc.mouseHandler.xpos() / s), (int) (mc.mouseHandler.ypos() / s)};
    }
}
