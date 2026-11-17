package dev.maire.nourished.client.hud.dynamic.edit;

import dev.marie.framework.color.MarieColors;
import dev.maire.nourished.client.colors.NourishedColors;
import dev.marie.framework.ui.api.MarieModuleSettings;
import dev.marie.framework.client.config.state.MarieClientCache;
import dev.marie.framework.tracking.TrackingData;
import dev.marie.framework.ui.geometry.Anchor;
import dev.marie.framework.ui.geometry.Bounds;
import dev.marie.framework.ui.component.AutoGrowPanelContainer;
import dev.marie.framework.ui.component.ComponentState;
import dev.marie.framework.ui.component.Constraint;
import dev.marie.framework.ui.edit.ContentScaleController;
import dev.marie.framework.ui.api.MarieScaleConfig;
import dev.marie.framework.ui.geometry.Insets;
import dev.marie.framework.ui.component.MarieComponent;
import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.geometry.Size;
import dev.marie.framework.ui.render.GuiGraphicsRenderContext;
import dev.marie.framework.ui.scaleconfig.ScaleConfigEntry;
import dev.marie.framework.ui.scaleconfig.ScaleConfigPanel;
import dev.maire.nourished.client.UiStatePersistence;
import dev.maire.nourished.client.edit.HostedEditBox;
import dev.marie.framework.ui.api.EditCapabilities;
import dev.marie.framework.ui.api.EditGesture;
import dev.marie.framework.ui.api.ModuleEditHost;
import dev.maire.nourished.client.hud.NourishedHUD;
import dev.maire.nourished.client.hud.classic.ClassicHudPanelRenderer;
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
import java.util.Optional;

/**
 * Single stable {@link MarieComponent} target for the HUD's MarieUI edit mode. Unlike
 * {@link dev.maire.nourished.client.screen.diet.dynamic.edit.DietScreenEditTarget} this wraps exactly one
 * draggable/resizable region (the whole nutrient panel), moved and resized through MariesLib's
 * {@link ModuleEditHost} rather than one tracker per sub-region.
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
 * drags a handle affecting that axis, at which point {@link HudPanelPlacement} marks it manual and the
 * dragged size is respected — not silently overwritten — until they resize that axis again.
 *
 * <p>Position AND size are persisted independently via {@link UiStatePersistence} (same
 * {@link ComponentState} model {@code DietScreenPersistence} uses for the Diet Screen's panel/
 * sub-boxes).
 */
public final class HudEditTarget implements MarieComponent {

    private static final String ID = "nourished.hud.editwrapper";
    private static final String PANEL_ID = "nourished.hud.panel";
    /** How many rows {@link HudPanelPlacement}'s persisted height last fit — see {@link #persistedManualHeightRowCount()}. */
    private static final String ROW_COUNT_ID = "nourished.hud.panel.rowCount";

    /** Accent for the move outlines and the collapsed tab; this panel has no title of its own to take one from. */
    private static int contentOutlineColor() {
        return MarieColors.resolveColor(NourishedColors.EDIT_OUTLINE);
    }

    /** How much bigger than content's natural size the box may be dragged, on either axis. */
    private static final double MAX_MARGIN_MULTIPLIER = 5.0d;

    private static final EditCapabilities PANEL_CAPS = EditCapabilities.full().withMoveModes().keepContentOnEdgeResize(false);

    private final Minecraft mc;
    /** Moves and resizes the panel itself; its content moves (and Move Nutrient) stay below, since they use this HUD's own offsets and outlines. */
    private final ModuleEditHost host = ModuleEditHost.builder(UiStatePersistence.get(), new HudPanelPlacement())
            .useSnapRegistry(true)
            .sizeLabels(false)
            .afterCommit((id, bounds) -> saveManualHeightRowCount(currentVisibleKeysOrFallback().size()))
            .build();

    private final HudContentMove contentMove = new HudContentMove(PANEL_ID);

    /** This panel's settings window, owned here since this class (not {@code NourishedHUD}) is the edit-mode {@link MarieComponent}. */
    private final ScaleConfigPanel scaleConfigPanel = MarieScaleConfig.create(
            List.of(new ScaleConfigEntry(PANEL_ID, Component.translatable("nourished.hud.nutrientPanel.label"))
                    .withContent(HudOptionsPanel.build(PANEL_ID, contentMove::reset))),
            UiStatePersistence.get(), Anchor.TOP_RIGHT);
    private boolean scaleConfigVisible;

    public HudEditTarget(Minecraft mc) {
        this.mc = mc;
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
     * The panel's resize clamp, rebuilt from {@code natural} every frame: the HUD scale, bar width,
     * layout and visible-nutrient count can all change in the config screen while this target is alive.
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

    /** Shows this panel's {@link ScaleConfigPanel} alongside edit mode — called from both the H-keybind path ({@code NourishedHUD#enterEditModeWithScaleConfig}) and the "edit all HUDs" group entry ({@code NourishedHUD#showScaleConfigOnGroupEntry}). Never reset back to false on exit. */
    public void setScaleConfigVisible(boolean visible) {
        this.scaleConfigVisible = visible;
    }

    /**
     * This panel's persisted text-scale multiplier, read directly off the same {@link
     * UiStatePersistence} record {@link HudPanelPlacement} writes position/size into — no {@link
     * ContentScaleController} instance is needed here, only {@link
     * ContentScaleController#resolveContentScale}'s static pass-through at render time.
     */
    public static double persistedContentScale() {
        return UiStatePersistence.get().load(PANEL_ID).map(ComponentState::contentScale).orElse(ComponentState.DEFAULT_CONTENT_SCALE);
    }

    /**
     * Always {@link ComponentState#DEFAULT_PADDING_SCALE} — the Padding slider is removed from this
     * panel's editor (see {@code HudOptionsPanel#build}'s {@code withoutPadding()}), so any value a
     * player set before it was removed is no longer read back, rather than silently continuing to
     * affect this panel's padding with no slider left to see or change it by.
     */
    public static double persistedPaddingScale() {
        return ComponentState.DEFAULT_PADDING_SCALE;
    }

    /** Same "empty visible keys -> fall back to every registered nutrient" reasoning the constructor uses above, so nothing ever resolves an empty-key layout while other components still expect this panel's real on-screen box. */
    private static List<String> currentVisibleKeysOrFallback() {
        List<String> keys = currentVisibleKeys();
        return keys.isEmpty() ? NutrientRegistry.getKeys() : keys;
    }

    /**
     * Resolves the HUD panel's current full {@link HudLayout.Layout}: persisted independent
     * position/size if the user has committed a main-panel drag/resize (via {@link HudPanelPlacement}),
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
                    // A manual height only applies once every bar is showing — the "settled" state the
                    // player was actually looking at when they dragged the handle. With just a subset
                    // visible (reveal-on-gain after a meal, some bars still hidden) the box always uses
                    // the natural height for however many rows are showing right now instead: capping a
                    // manually *enlarged* box down to natural keeps a full-size box from popping up
                    // around one or two bars, and — the bug this used to have — NOT also flooring a
                    // manually *shrunk* box at its old (smaller) natural height meant a box resized back
                    // when fewer nutrients were revealed stayed stuck at that size as more crossed the
                    // reveal threshold, clipping/half-showing whichever row didn't fit instead of the
                    // box growing to fit them. `Math.min` against the stale manual height was exactly
                    // that floor; natural alone can't undershoot the current row count since it's
                    // computed from it.
                    boolean allBarsVisible = keys.size() >= NourishedClientConfig.get().effectiveDietBarOrder().size();
                    // A manual height is also only trusted for however many rows it was actually sized
                    // for: a third-party mod (or the player) adding more nutrients than were visible at
                    // the last resize grows effectiveDietBarOrder()/keys past that, and without this
                    // check allBarsVisible alone would still honor the now-too-small persisted height,
                    // clipping whichever new row doesn't fit instead of the box growing for it.
                    boolean manualHeightFitsCurrentRows = keys.size() <= persistedManualHeightRowCount(state, allBarsVisible, keys.size());
                    int manualHeight = AutoGrowPanelContainer.resolveHeight(override, state.height(), natural.panelH());
                    int height = (allBarsVisible && manualHeightFitsCurrentRows) ? manualHeight : natural.panelH();
                    return new HudLayout.Layout(
                            state.x(), state.y(), width, height,
                            natural.baseX(), natural.baseY(),
                            natural.barW(), natural.rowH(), natural.iconSize(), natural.maxLabelSw(),
                            natural.scaledPad(), natural.labelScale(), natural.scale(), natural.verticalLayout(),
                            natural.verticalBarW(), natural.verticalBarH(), natural.verticalColumnW(),
                            natural.panelW(), natural.panelH(), state.leftMargin(),
                            HudContentMove.persistedOffsetX(), HudContentMove.persistedOffsetY()
                    );
                })
                .orElse(natural);
    }

    /** Only grown/shrunk by a left-edge (or bottom-left-corner) gesture, so content stays put on screen, via {@link AutoGrowPanelContainer#leftMarginAfterResize}. */
    private static int persistedLeftMargin() {
        return UiStatePersistence.get().load(PANEL_ID)
                .map(ComponentState::leftMargin)
                .orElse(0);
    }

    /**
     * How many rows were currently visible the last time {@link HudPanelPlacement} persisted a manual height.
     * A manual height saved before this record existed has none; it's adopted (and persisted) as
     * fitting the full row set the first time every bar is showing, so the height keeps applying
     * after upgrading yet a nutrient added later still makes the box grow.
     */
    private static int persistedManualHeightRowCount(ComponentState panel, boolean allBarsVisible, int rowCount) {
        Optional<ComponentState> saved = UiStatePersistence.get().load(ROW_COUNT_ID);
        if (saved.isPresent()) {
            return saved.get().x();
        }
        if (panel.heightManual() && allBarsVisible) {
            saveManualHeightRowCount(rowCount);
            return rowCount;
        }
        return 0;
    }

    private static void saveManualHeightRowCount(int rowCount) {
        UiStatePersistence.get().save(ROW_COUNT_ID, new ComponentState(rowCount, 0, 0, 0, false, false, false, 0));
    }

    /** Resolves the HUD panel's current on-screen bounds — see {@link #resolvedLayout}. */
    static Bounds resolvedBounds(Minecraft mc, List<String> keys) {
        HudLayout.Layout layout = resolvedLayout(mc, keys);
        return new Bounds(layout.panelX(), layout.panelY(), layout.panelW(), layout.panelH());
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Constraint constraint() {
        // Unused: the edit host clamps resizes with the panel box's own constraint, never this one.
        return Constraint.preferred(0, 0);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (scaleConfigVisible && scaleConfigPanel.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (HudEditTabs.isCollapsed(UiStatePersistence.get(), PANEL_ID)) {
            Bounds tabBounds = HudEditTabs.tabBounds(PANEL_ID);
            if (button == 0 && tabBounds.contains((int) mouseX, (int) mouseY)) {
                HudEditTabs.open(UiStatePersistence.get(), PANEL_ID);
                return true;
            }
            return false;
        }
        List<String> keys = currentVisibleKeysOrFallback();
        if (keys.isEmpty()) {
            return false;
        }
        Bounds bounds = resolvedBounds(mc, keys);
        if (button == 0 && HudEditTabs.collapseButtonBounds(bounds).contains((int) mouseX, (int) mouseY)) {
            HudEditTabs.setCollapsed(UiStatePersistence.get(), PANEL_ID, true);
            return true;
        }
        if (contentMove.click(mouseX, mouseY, bounds)) {
            return true;
        }
        return host.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scaleConfigVisible && scaleConfigPanel.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        if (HudEditTabs.isCollapsed(UiStatePersistence.get(), PANEL_ID)) {
            return false;
        }
        List<String> keys = currentVisibleKeysOrFallback();
        if (scrollY == 0 || keys.isEmpty()) {
            return false;
        }
        HudLayout.Layout layout = resolvedLayout(mc, keys);
        if (!new Bounds(layout.panelX(), layout.panelY(), layout.panelW(), layout.panelH()).contains((int) mouseX, (int) mouseY)) {
            return false;
        }
        return HudRowScroll.scroll(keys.size(), layout, scrollY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (scaleConfigVisible && scaleConfigPanel.mouseDragged(mouseX, mouseY, button)) {
            return true;
        }
        if (HudEditTabs.isCollapsed(UiStatePersistence.get(), PANEL_ID)) {
            return false;
        }
        List<String> keys = currentVisibleKeysOrFallback();
        Bounds box = keys.isEmpty() ? null : resolvedBounds(mc, keys);
        if (contentMove.drag(mouseX, mouseY, box, persistedLeftMargin())) {
            return true;
        }
        return host.mouseDragged(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (scaleConfigVisible && scaleConfigPanel.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        if (contentMove.release()) {
            return true;
        }
        return host.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void render(RenderContext context, Bounds ignoredBounds) {
        if (HudEditTabs.isCollapsed(UiStatePersistence.get(), PANEL_ID)) {
            HudEditTabs.drawTab(context, Component.translatable("nourished.hud.nutrientPanel.label").getString(),
                    contentOutlineColor(), HudEditTabs.tabBounds(PANEL_ID));
            if (scaleConfigVisible) {
                scaleConfigPanel.render(context, new Bounds(0, 0, context.screenWidth(), context.screenHeight()));
            }
            return;
        }

        List<String> keys = currentVisibleKeysOrFallback();
        if (keys.isEmpty()) {
            return;
        }

        int[] mouse = scaledMouse(mc);
        Bounds defaultBounds = resolvedBounds(mc, keys);
        String rowKey = HudRowMove.activeKey();
        boolean moveTextMode = moveContentEnabled();
        boolean moveBarsMode = moveBarsEnabled();
        boolean moveIconsMode = MarieModuleSettings.isMoveIconsEnabled(UiStatePersistence.get(), PANEL_ID);
        boolean moveAllMode = MarieModuleSettings.isMoveAllEnabled(UiStatePersistence.get(), PANEL_ID);
        boolean contentMode = rowKey != null || moveTextMode || moveBarsMode || moveIconsMode || moveAllMode;
        host.beginFrame();
        Bounds bounds = host.track(new HostedEditBox(PANEL_ID, defaultBounds, constraintFor(HudLayout.compute(mc, keys)),
                PANEL_CAPS, "", "", null, !contentMode), defaultBounds);

        // Offsets are clamped only while dragging (see mouseDragged), never here: clamping at draw time
        // rewrote the shared offsets against whatever the box measured this frame, so the edit preview
        // and the in-game HUD (which never re-clamped) drew the same content in different places, and a
        // box collapsed to a sliver would permanently squash them. The panel clip hides any overflow.

        Map<String, Float> displayValues = NourishedHUD.currentDisplayValues();
        HudLayout.Layout matchedLayout = matchedLayoutFor(keys, bounds);
        if (NourishedClientConfig.get().hudClassicMode() && context instanceof GuiGraphicsRenderContext guiContext) {
            // The classic renderer records no extents, so no move outline is drawn for it.
            HudDrawnExtents.beginFrame();
            TrackingData data = MarieClientCache.get();
            ClassicHudPanelRenderer.drawPanel(
                    guiContext.graphics(), mc, data, HudRowScroll.scrolledKeys(keys, matchedLayout), matchedLayout, bounds.x(), bounds.y(), displayValues
            );
        } else {
            NutrientPanelContainer panel = new NutrientPanelContainer(HudRowScroll.scrolledKeys(keys, matchedLayout), matchedLayout, displayValues);
            panel.render(context, bounds);
        }
        HudEditTabs.drawCollapseButton(context, HudEditTabs.collapseButtonBounds(bounds), contentOutlineColor());

        // Every outline wraps what the rows actually drew this frame (HudDrawnExtents), so it stays on
        // the content however rows, icons, text or bars have been moved or resized.
        if (contentMode) {
            // Dashed rather than a solid glow outline — a live drag affordance shown only while a move
            // toggle is active, not a persistent separately-hit-tested box. "Move Nutrient" (the picked
            // row) wins over the panel-wide modes, matching which one a drag actually moves.
            Bounds outline;
            if (rowKey != null) {
                outline = HudDrawnExtents.row(rowKey);
            } else if (moveAllMode) {
                outline = HudDrawnExtents.all();
            } else if (moveTextMode) {
                outline = HudDrawnExtents.part(HudDrawnExtents.Part.TEXT);
            } else if (moveIconsMode) {
                outline = HudDrawnExtents.part(HudDrawnExtents.Part.ICON);
            } else {
                outline = HudDrawnExtents.part(HudDrawnExtents.Part.BAR);
            }
            if (outline != null) {
                // Clipped to the box, the same way the content itself is cut off at its edge.
                context.pushClip(bounds.x(), bounds.y(), bounds.width(), bounds.height());
                try {
                    context.drawDashedBorder(outline.x() - 3, outline.y() - 2, outline.width() + 6, outline.height() + 4, contentOutlineColor());
                } finally {
                    context.popClip();
                }
            }
        }
        host.renderOverlay(context, mouse[0], mouse[1]);

        if (scaleConfigVisible) {
            scaleConfigPanel.render(context, new Bounds(0, 0, context.screenWidth(), context.screenHeight()));
        }
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
        EditGesture gesture = host.activeGesture();
        boolean leftEdgeResize = gesture != null && gesture.kind() == EditGesture.Kind.RESIZE && gesture.leftEdge();
        int leftMargin = AutoGrowPanelContainer.leftMarginAfterResize(persistedLeftMargin(),
                resolvedBounds(mc, keys).width(), bounds.width(), leftEdgeResize);
        HudLayout.Layout natural = HudLayout.compute(mc, keys);
        return new HudLayout.Layout(
                bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                natural.baseX(), natural.baseY(),
                natural.barW(), natural.rowH(), natural.iconSize(), natural.maxLabelSw(),
                natural.scaledPad(), natural.labelScale(), natural.scale(), natural.verticalLayout(),
                natural.verticalBarW(), natural.verticalBarH(), natural.verticalColumnW(),
                natural.panelW(), natural.panelH(), leftMargin,
                contentMove.offsetX(), contentMove.offsetY()
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
        // Editing shows every bar so hidden ones can still be placed and styled.
        if (dev.maire.nourished.client.hud.NourishedHUD.isEditing()) {
            return cc.effectiveDietBarOrder();
        }
        return HudVisibility.visibleKeys(data, cc.effectiveDietBarOrder(), cc);
    }

    private static int[] scaledMouse(Minecraft mc) {
        double s = mc.getWindow().getGuiScale();
        return new int[]{(int) (mc.mouseHandler.xpos() / s), (int) (mc.mouseHandler.ypos() / s)};
    }
}
