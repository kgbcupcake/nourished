package dev.maire.nourished.modules.activity_driven_nutrient.client;

import dev.maire.nourished.client.colors.NourishedColors;
import dev.maire.nourished.client.colors.NourishedColorSlots;
import dev.marie.framework.ui.api.MarieModuleSettings;
import dev.marie.framework.ui.api.MoveDrag;
import dev.marie.framework.color.ColorKeyPair;
import dev.marie.framework.color.MarieColors;
import dev.marie.framework.tracking.tracker.MarieTracking;
import dev.marie.framework.tracking.tracker.definition.TrackerHistoryEntry;
import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.Theme;
import dev.marie.framework.ui.ThemeKey;
import dev.marie.framework.ui.component.AutoGrowPanelContainer;
import dev.marie.framework.ui.component.ComponentState;
import dev.marie.framework.ui.component.Constraint;
import dev.marie.framework.ui.component.MarieComponent;
import dev.marie.framework.ui.edit.ContentScaleController;
import dev.marie.framework.ui.edit.DraggableResizable;
import dev.marie.framework.ui.edit.EditModeController;
import dev.marie.framework.ui.geometry.Anchor;
import dev.marie.framework.ui.geometry.Bounds;
import dev.marie.framework.ui.geometry.Insets;
import dev.marie.framework.ui.geometry.Size;
import dev.marie.framework.ui.render.GuiGraphicsRenderContext;
import dev.marie.framework.ui.api.MarieScaleConfig;
import dev.marie.framework.ui.api.SnapRegistry;
import dev.marie.framework.ui.scaleconfig.ScaleConfigEntry;
import dev.marie.framework.ui.scaleconfig.ScaleConfigPanel;
import dev.maire.nourished.client.NourishedKeys;
import dev.maire.nourished.client.UiStatePersistence;
import dev.maire.nourished.client.hud.dynamic.HudDrawHelpers;
import dev.maire.nourished.config.NourishedClientConfig;
import dev.maire.nourished.modules.activity_driven_nutrient.core.ActivityDrivenNutrientRegistry;
import dev.maire.nourished.modules.activity_driven_nutrient.core.ActivityTrackerIds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Persistent, player-facing HUD panel showing the local player's daily activity totals — one row
 * per {@link ActivityTrackerIds} tracker (mining blocks, combat kills, sprint/swim distance,
 * starvation crossings), read live via {@link MarieTracking#getCurrentTrackerValue}. Same tier as
 * {@code NourishedHUD}: config-toggleable, draggable/resizable via its own edit-mode keybind
 * ({@link NourishedKeys#EDIT_ACTIVITY_LOG_HUD}), independent of the nutrition HUD's edit mode.
 *
 * <p>Formerly read {@link ActivityLogClientBuffer}'s session-only "last event fired" counts; now
 * reads the persistent, server-authoritative daily trackers instead, the same
 * client-side-safe accessor {@code CalorieHudScreen} uses for its own tracker — {@code
 * MarieTracking.getCurrentTrackerValue} is documented to work identically whether called with a
 * {@code ServerPlayer} or a client-side player instance. {@code ActivityLogClientBuffer} itself is
 * untouched and no longer consulted by this panel.
 *
 * <p>Unlike {@code NourishedHUD}/{@code HudEditTarget}, this panel has no per-frame lerped display
 * state to share between a normal render path and an edit-mode wrapper, so both live in this one
 * {@link MarieComponent} implementation: {@link #onRenderGuiPost} draws it directly outside edit
 * mode, and {@link #render} (via {@link EditModeController}'s overlay) draws the live drag/resize
 * preview.
 */
public final class ActivityLogHudPanel implements MarieComponent {

    private static final String ID = "nourished.activityLogHud.panel";
    private static final String PANEL_ID = "nourished.activityLogHud.panel";
    private static final String CONTENT_OFFSET_ID = "nourished.activityLogHud.contentOffset";

    private static final int PANEL_WIDTH = 220;
    private static final int LINE_HEIGHT = 12;
    private static final int PADDING = 4;
    private static final int DEFAULT_X = 8;
    private static final int DEFAULT_Y = 8;

    /** Row icon size, matching {@code HudLayout}'s own icon-size floor/scale reasoning. */
    private static final int ICON_SIZE = 10;
    /** Fixed-width reserve for the trailing formatted-value text, so bars stay aligned across rows regardless of digit count. */
    private static final int VALUE_RESERVE = 56;

    /** Title row height/gap, unscaled by contentScale — chrome, not user content, matching {@code ScaleConfigPanel}'s own fixed HEADER_HEIGHT. */
    private static final int HEADER_HEIGHT = 12;
    private static final int HEADER_GAP = 2;
    /** Slightly larger than 1.0 stands in for "bold" — same trick {@code ScaleConfigPanel#drawCard} uses for its own header. */
    private static final float TITLE_SCALE = 1.05f;
    /** Green accent — one of {@code ScaleConfigPanel.ACCENT_PALETTE}'s colors, reused here since this card now follows that same visual language. */
    private static int titleAccentColor() {
        return MarieColors.resolveColor(NourishedColors.ACTIVITY_ACCENT);
    }

    /**
     * One row per tracker, in fixed display order. {@code moduleId} keys {@link
     * ActivityDrivenNutrientRegistry#getColor}, matching the existing per-module HUD log colors.
     * {@code icon} has no existing per-tracker asset to reuse (unlike nutrients, which have a
     * datapack-configurable icon via {@code NutrientRegistry#getIcon}) — these are fixed vanilla
     * item placeholders, one per metric, resolved the same static-{@code ItemStack} way {@link
     * dev.maire.nourished.client.hud.caloriehistory.CalorieHudScreen}'s single calorie icon is.
     */
    private record TrackerRow(String moduleId, ResourceLocation trackerId, String label, boolean isDistance, ItemStack icon) {}

    private static final List<TrackerRow> TRACKERS = List.of(
            new TrackerRow("mining", ActivityTrackerIds.MINING_BLOCKS_ID, "Mining", false, new ItemStack(Items.IRON_PICKAXE)),
            new TrackerRow("combat", ActivityTrackerIds.COMBAT_KILLS_ID, "Combat", false, new ItemStack(Items.IRON_SWORD)),
            new TrackerRow("sprint", ActivityTrackerIds.SPRINT_DISTANCE_ID, "Sprint", true, new ItemStack(Items.LEATHER_BOOTS)),
            new TrackerRow("swim", ActivityTrackerIds.SWIM_DISTANCE_ID, "Swim", true, new ItemStack(Items.WATER_BUCKET)),
            new TrackerRow("starvation", ActivityTrackerIds.STARVATION_CROSSINGS_ID, "Starvation", false, new ItemStack(Items.ROTTEN_FLESH))
    );

    /**
     * One rendered row: {@code moduleId} drives the per-module color/icon lookup, {@code value} is
     * the raw current-period tracker total, {@code formattedValue} its display text, and {@code
     * historicalMax} is the highest value in that same tracker's own retained history (0 if it has
     * none yet) — each tracker's bar is scaled against its own history, never another tracker's.
     */
    private record Row(String moduleId, String label, float value, String formattedValue, float historicalMax) {}

    /** How much bigger than content's natural size the box may be dragged, on either axis. */
    private static final double MAX_MARGIN_MULTIPLIER = 5.0d;

    /** How much smaller than content's natural size the box may be dragged, on either axis. */

    private static ActivityLogHudPanel instance;
    private static EditModeController editModeController;

    /** Row scroll position (in rows, not pixels) into {@link #currentRows()} — only movable while edit mode has this panel's mouse input wired up; see {@link #mouseScrolled}. Clamped every {@link #drawPanel} pass against however many rows currently fit, so it self-corrects if the row count or box size changes out from under it. */
    private static int scrollOffset;

    private final DraggableResizable drag;

    /** Whether a content-move drag (see {@link #moveContentEnabled}) is currently in progress. */
    /** Grab state for a "Move Text and Icons" or "Move Bars" drag (see {@link MoveDrag}). */
    private final MoveDrag moveDrag = new MoveDrag();

    /**
     * The row content's offset from where it would otherwise sit (just below the header, inset by
     * padding) — a plain persisted translation, not a separately hit-testable box: there's nothing
     * else in this panel besides the rows, so a separately draggable/glowing sub-region for them
     * competed with the panel's own drag for every click inside the panel body. {@link
     * #moveContentEnabled} disambiguates instead: while it's off, panel-body clicks move the panel;
     * while it's on, they move the content. Same pattern as {@code CalorieHudScreen}'s
     * {@code contentOffsetX}/{@code contentOffsetY}, which this replaces the {@code rowsDrag}-based
     * predecessor of.
     */
    private int contentOffsetX;
    private int contentOffsetY;

    /**
     * Editor for this panel's persisted contentScale/paddingScale, auto-shown alongside edit mode —
     * same pattern as {@code DietScreen#scaleConfigPanel}, just with a single entry since this panel
     * has no sub-boxes.
     */
    private final ScaleConfigPanel scaleConfigPanel = MarieScaleConfig.create(
            List.of(new ScaleConfigEntry(PANEL_ID, Component.translatable("nourished.hud.activityLog.label"))
                    .withContent(MarieModuleSettings.standardPanel(Component.translatable("nourished.hud.activityLog.label").getString(), UiStatePersistence.get(), PANEL_ID)
                            .opacity(() -> NourishedClientConfig.get().activityLogHudBackgroundOpacity(), v -> NourishedClientConfig.get().setActivityLogHudBackgroundOpacity(v), 204.0d / 255.0d)
                            .textBrightness(() -> NourishedClientConfig.get().activityLogHudTextBrightness(), v -> NourishedClientConfig.get().setActivityLogHudTextBrightness(v))
                            .iconBrightness(() -> NourishedClientConfig.get().activityLogHudIconBrightness(), v -> NourishedClientConfig.get().setActivityLogHudIconBrightness(v))
                            .backgroundShade(() -> NourishedClientConfig.get().activityLogHudBackgroundShade(), v -> NourishedClientConfig.get().setActivityLogHudBackgroundShade(v))
                            .borderOpacity(() -> NourishedClientConfig.get().activityLogHudBorderOpacity(), v -> NourishedClientConfig.get().setActivityLogHudBorderOpacity(v))
                            .borderShade(() -> NourishedClientConfig.get().activityLogHudBorderShade(), v -> NourishedClientConfig.get().setActivityLogHudBorderShade(v))
                            .onCommit(NourishedClientConfig::saveNow)
                            .onReset(this::resetContentOffset)
                            .extraTabs(panel -> {
                                panel.colorTab(Component.translatable("config.marieslib.moduleoptions.tab.colors").getString());
                                NourishedColorSlots.addPair(panel, COLORS);
                                NourishedColorSlots.addFixed(panel, NourishedColors.ACTIVITY_ACCENT, "nourished.options.color.accent");
                                NourishedColorSlots.addFixed(panel, NourishedColors.ACTIVITY_LOG_BORDER, "nourished.options.color.border");
                                NourishedColorSlots.addActivities(panel);
                            })
                            .build())),
            UiStatePersistence.get(), Anchor.TOP_RIGHT);
    private boolean scaleConfigVisible;

    private ActivityLogHudPanel() {
        drag = new DraggableResizable(this, panelConstraintFor(naturalSize(currentRows().size())), (target, bounds) -> commit(bounds));
        drag.setSnapRegistryId(PANEL_ID);
        SnapRegistry.register(PANEL_ID, () -> resolvedBounds(currentRows().size()));

        UiStatePersistence.get().load(CONTENT_OFFSET_ID).ifPresent(state -> {
            contentOffsetX = state.x();
            contentOffsetY = state.y();
        });
    }

    /** {@code drag}'s min/preferred/max clamp, rebuilt fresh from {@code natural} — never cached past a single call, same reasoning as {@code HudEditTarget#constraintFor}. */
    private static Constraint panelConstraintFor(Size natural) {
        Size minSize = new Size(AutoGrowPanelContainer.MIN_COLLAPSED_SIZE, AutoGrowPanelContainer.MIN_COLLAPSED_SIZE);
        return new Constraint(
                natural, minSize,
                new Size((int) (natural.width() * MAX_MARGIN_MULTIPLIER), (int) (natural.height() * MAX_MARGIN_MULTIPLIER)),
                false, false, true, true,
                Anchor.TOP_LEFT, Insets.NONE, Insets.NONE
        );
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
    }

    /** The "Move Text and Icons" toggle's live state, owned by {@link #scaleConfigPanel} (its own editor window, under Padding) rather than a button on this panel itself. */
    private boolean moveContentEnabled() {
        return scaleConfigPanel.isMoveContentEnabled(PANEL_ID);
    }

    /** The "Move Bars" toggle's live state — see {@link MarieModuleSettings#isMoveBarsEnabled}. */
    private boolean moveBarsEnabled() {
        return MarieModuleSettings.isMoveBarsEnabled(UiStatePersistence.get(), PANEL_ID);
    }

    /** Public so {@code ClientEventRegistrar} can pass this same singleton to {@code EditModeCoordinator.registerGroupCapable} as its {@code MarieComponent} target — the exact instance this panel's own {@link #editModeController()} already wraps. */
    public static ActivityLogHudPanel instance() {
        if (instance == null) {
            instance = new ActivityLogHudPanel();
        }
        return instance;
    }

    private static EditModeController editModeController() {
        if (editModeController == null) {
            editModeController = new EditModeController(
                    instance(),
                    "Drag the Activity Log HUD to reposition, drag the corner handle to resize. K or Esc to exit.",
                    NourishedKeys.EDIT_ACTIVITY_LOG_HUD.getKey().getValue(),
                    () -> {}
            );
        }
        return editModeController;
    }

    /**
     * K ({@link NourishedKeys#EDIT_ACTIVITY_LOG_HUD}) enters edit mode through here so the scale
     * panel always appears alongside it — same pattern as {@code
     * DietScreen#enterEditModeWithScaleConfig}. Only sets {@link #scaleConfigVisible} {@code true}
     * on entry; exiting edit mode is untouched.
     */
    private void enterEditModeWithScaleConfig() {
        scaleConfigVisible = true;
        editModeController().enter();
    }

    /**
     * Group-entry counterpart to {@link #enterEditModeWithScaleConfig()} — sets {@link
     * #scaleConfigVisible} {@code true} without itself entering edit mode, since the group path
     * (unlike the {@code K} keybind's individual path above) already opens the shared overlay via
     * {@code EditModeCoordinator.enterAll()}. Passed as the {@code onGroupEnter} callback to {@code
     * EditModeCoordinator.registerGroupCapable} in {@code ClientEventRegistrar}, so this panel's
     * scale-config panel shows up whether edit mode is entered individually or via the shared group
     * toggle.
     */
    public static void showScaleConfigOnGroupEntry() {
        instance().scaleConfigVisible = true;
    }

    public static void onRenderGuiPost(RenderGuiEvent.Post event) {
        if (Minecraft.getInstance().options.hideGui) {
            return;
        }
        if (!NourishedClientConfig.get().enableActivityLogHud()) {
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
        List<Row> rows = currentRows();
        if (rows.isEmpty()) {
            return;
        }
        Bounds bounds = resolvedBounds(rows.size());
        ActivityLogHudPanel self = instance();
        // Re-clamped defensively at draw time too — see the same reasoning on CalorieHudScreen's
        // equivalent call site: the panel/row count this offset is valid against can change
        // independently of dragging, and a stale offset should self-correct visually.
        int offsetX = clampContentOffsetX(self.contentOffsetX, bounds);
        int offsetY = clampContentOffsetY(self.contentOffsetY, bounds);
        GuiGraphicsRenderContext context = new GuiGraphicsRenderContext(
                event.getGuiGraphics(), mc, Theme.DARK, event.getPartialTick().getGameTimeDeltaPartialTick(false));
        // Defense-in-depth: resetClip() forces the scissor stack/GL state back to empty even if
        // drawPanel throws partway through its pushClip/popClip pair — see
        // GuiGraphicsRenderContext#resetClip.
        try {
            drawPanel(MarieModuleSettings.withBrightness(context, NourishedClientConfig.get().activityLogHudTextBrightness(), NourishedClientConfig.get().activityLogHudIconBrightness()),
                    bounds, persistedLeftMargin(), offsetX, offsetY, rows, false, false, false, false, false);
        } finally {
            context.resetClip();
        }
    }

    /**
     * One row per {@link #TRACKERS} entry, in fixed display order, reading each tracker's
     * current-period (today's) accumulated value via {@link MarieTracking#getCurrentTrackerValue}
     * against the local client player — the same client-side-safe accessor {@code
     * CalorieHudScreen} would use, works whether given a {@code ServerPlayer} or a client-side
     * player instance. Counts (mining/combat/starvation) render as whole numbers; distances
     * (sprint/swim) render with one decimal place, no unit suffix — the value reserve is sized
     * for the widest digit-only case and a "blocks" suffix ran past it in a shrunk box.
     *
     * <p>Also reads each tracker's own retained history via {@link
     * MarieTracking#getTrackerHistory} — the same call {@code CalorieHudScreen} uses for its
     * history rows — to find that tracker's highest-ever completed-period value, so {@link
     * #drawPanel} can scale each bar against its own history instead of the other four trackers.
     */
    private static List<Row> currentRows() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return List.of();
        }
        List<Row> rows = new ArrayList<>(TRACKERS.size());
        for (TrackerRow tracker : TRACKERS) {
            float value = MarieTracking.getCurrentTrackerValue(player, tracker.trackerId());
            String formatted = tracker.isDistance()
                    ? String.format("%.1f", value)
                    : String.valueOf((int) value);
            float historicalMax = 0f;
            for (TrackerHistoryEntry entry : MarieTracking.getTrackerHistory(player, tracker.trackerId())) {
                historicalMax = Math.max(historicalMax, entry.value());
            }
            rows.add(new Row(tracker.moduleId(), tracker.label(), value, formatted, historicalMax));
        }
        return rows;
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            return;
        }
        if (!NourishedClientConfig.get().enableActivityLogHud()) {
            return;
        }
        while (NourishedKeys.EDIT_ACTIVITY_LOG_HUD.consumeClick()) {
            instance().enterEditModeWithScaleConfig();
        }
    }

    private static Size naturalSize(int rowCount) {
        int rows = Math.max(1, rowCount);
        return new Size(PANEL_WIDTH, PADDING * 2 + HEADER_HEIGHT + HEADER_GAP + rows * LINE_HEIGHT);
    }

    /**
     * How much of the content's own origin (its icon corner) must stay inside the panel on the far
     * edge — not the content's whole footprint. {@link #clampContentOffsetX}/{@link
     * #clampContentOffsetY} clamp only the origin into the panel's interior, using the box's own
     * live size (so a bigger box gives more room to drag, all the way to its far edge); anything
     * past the panel's own edge is invisible via {@link #drawPanel}'s existing {@code pushClip}, not
     * blocked from being dragged there in the first place.
     */
    private static final int MIN_VISIBLE_CONTENT = 14;

    /** Clamps a candidate {@link #contentOffsetX} so the content's own origin can't be dragged past the panel's edges — see {@link #MIN_VISIBLE_CONTENT}. */
    private static int clampContentOffsetX(int offsetX, Bounds panelBounds) {
        int minOffset = -PADDING;
        int maxOffset = Math.max(minOffset, panelBounds.width() - PADDING - MIN_VISIBLE_CONTENT);
        return Math.min(maxOffset, Math.max(minOffset, offsetX));
    }

    /** Clamps a candidate {@link #contentOffsetY} the same way {@link #clampContentOffsetX} does for X. */
    private static int clampContentOffsetY(int offsetY, Bounds panelBounds) {
        int minOffset = -(PADDING + HEADER_HEIGHT + HEADER_GAP);
        int maxOffset = Math.max(minOffset, panelBounds.height() - PADDING - HEADER_HEIGHT - HEADER_GAP - LINE_HEIGHT);
        return Math.min(maxOffset, Math.max(minOffset, offsetY));
    }

    private static Bounds resolvedBounds(int rowCount) {
        Size natural = naturalSize(rowCount);
        return UiStatePersistence.get().load(PANEL_ID)
                .map(state -> {
                    int width = state.widthManual() ? state.width() : natural.width();
                    int height = state.heightManual() ? state.height() : natural.height();
                    return new Bounds(state.x(), state.y(), width, height);
                })
                .orElseGet(() -> new Bounds(DEFAULT_X, DEFAULT_Y, natural.width(), natural.height()));
    }

    /**
     * This box's current on-screen bounds (persisted, or its own default) — for {@link
     * dev.maire.nourished.client.hud.caloriehistory.CalorieHudScreen}, whose own fixed default sits in the
     * same top-left corner just below this one, to stack its default underneath this box's actual bottom
     * edge instead of a fixed gap: a first-time install used to give both boxes fixed defaults only 52px
     * apart, which this box's natural height (it grows with however many activities are being tracked)
     * regularly exceeds, so they overlapped before either box had ever been dragged. {@code null} while
     * this feature is off, so the caller falls back to its own baseline default.
     */
    public static Bounds currentBoundsForStacking() {
        if (!NourishedClientConfig.get().enableActivityLogHud()) {
            return null;
        }
        return resolvedBounds(currentRows().size());
    }

    /** How many rows fit vertically in {@code bounds} at the current content scale — shared by {@link #drawPanel} (what to draw) and {@link #mouseScrolled} (how far scrolling can go). */
    private static int visibleRowCapacity(Bounds bounds) {
        double contentScale = ContentScaleController.resolveContentScale(persistedContentScale());
        int lineHeight = LINE_HEIGHT;
        double userPadding = PADDING * persistedPaddingScale();
        int padding = Math.round(ContentScaleController.resolvePadding(userPadding));
        return Math.max(1, (bounds.height() - padding * 2 - HEADER_HEIGHT - HEADER_GAP) / lineHeight);
    }

    /** Set by {@code Nourished#registerColorDefinitions()} at mod init. */
    public static ColorKeyPair COLORS;

    private static void drawPanel(RenderContext context, Bounds bounds, int leftMargin, int contentOffsetX, int contentOffsetY, List<Row> rows, boolean editMode, boolean moveTextMode, boolean moveIconsMode, boolean moveBarsMode, boolean moveAllMode) {
        if (MarieModuleSettings.isWindowHidden(UiStatePersistence.get(), PANEL_ID)) {
            return;
        }
        // Text/padding render scale is the user's persisted adjustment alone — box size (bounds)
        // plays no part in it, matching HudEditTarget's Nutrient HUD panel exactly: content never
        // shrinks to fit a smaller box, a resize only changes the box itself, and whatever doesn't
        // fit is handled by scrolling (see visibleRowCapacity/mouseScrolled), not shrinking.
        // The box itself can be dragged down to AutoGrowPanelContainer#MIN_COLLAPSED_SIZE (see the
        // Constraint built in the constructor); content is simply clipped away as it shrinks.
        double contentScale = ContentScaleController.resolveContentScale(persistedContentScale());
        double userPadding = PADDING * persistedPaddingScale();
        int padding = Math.round(ContentScaleController.resolvePadding(userPadding));
        // Text and icons have independent sizes (see MarieModuleSettings#iconScale): the icon is sized by its own
        // multiplier and the row grows to the taller of the two.
        double iconScale = ContentScaleController.resolveContentScale(MarieModuleSettings.iconScale(UiStatePersistence.get(), PANEL_ID));
        // Row geometry (row height, icon slot, label column, bar size) stays at its natural, unscaled
        // size, exactly like the Nutrient HUD: Text size and Icon size only change the drawn glyphs,
        // which are clipped to their row; Move Text and Icons / Move Bars re-place them.
        int iconSize = ICON_SIZE;
        int lineHeight = LINE_HEIGHT;

        NourishedClientConfig cc = NourishedClientConfig.get();
        int panelRgb = MarieColors.resolveColor(COLORS.background());
        int panelColor = MarieColors.withOpacity(
                MarieColors.shade(panelRgb, cc.activityLogHudBackgroundShade()), cc.activityLogHudBackgroundOpacity());
        int borderColor = MarieColors.withOpacity(
                MarieColors.shade(MarieColors.resolveColor(NourishedColors.ACTIVITY_LOG_BORDER), cc.activityLogHudBorderShade()), cc.activityLogHudBorderOpacity());
        context.drawRoundedRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), 1, HudDrawHelpers.PANEL_CORNER_RADIUS, panelColor, borderColor);
        context.pushClip(bounds.x(), bounds.y(), bounds.width(), bounds.height());
        try {
            context.drawText(Component.translatable("nourished.hud.activityLog.label").getString(),
                    bounds.x() + leftMargin + padding, bounds.y() + padding, titleAccentColor(), TITLE_SCALE);

            int defaultTextColor = MarieColors.resolveColor(COLORS.text());
            int barBg = HudDrawHelpers.barBackgroundColor();

            // Aligns every row's bar to the same x regardless of that row's own label width
            // ("Combat" vs. "Starvation"), same reasoning as HudLayout#maxLabelSw for the dynamic
            // nutrient HUD.
            int maxLabelW = 0;
            int maxLabelWNatural = 0;
            for (Row row : rows) {
                maxLabelW = Math.max(maxLabelW, context.textWidth(row.label(), 1f));
                maxLabelWNatural = Math.max(maxLabelWNatural, context.textWidth(row.label(), 1f));
            }
            int rowsX = bounds.x() + leftMargin + padding + contentOffsetX;
            // "Move Bars" offset: bars and their value text sit at the unshifted content origin plus this,
            // independent of the icon/name offset above.
            int barDx = clampContentOffsetX(MarieModuleSettings.barOffsetX(UiStatePersistence.get(), PANEL_ID), bounds);
            int barDy = clampContentOffsetY(MarieModuleSettings.barOffsetY(UiStatePersistence.get(), PANEL_ID), bounds);
            // "Move Icons" offset, and the Bar size multiplier (bar length/thickness and the value text at its end).
            int iconDx = clampContentOffsetX(MarieModuleSettings.iconOffsetX(UiStatePersistence.get(), PANEL_ID), bounds);
            int iconDy = clampContentOffsetY(MarieModuleSettings.iconOffsetY(UiStatePersistence.get(), PANEL_ID), bounds);
            float barScale = ContentScaleController.resolveContentScale(MarieModuleSettings.barScale(UiStatePersistence.get(), PANEL_ID));
            int barX = bounds.x() + leftMargin + padding + iconSize + HudDrawHelpers.ICON_LABEL_GAP + maxLabelW + HudDrawHelpers.LABEL_BAR_GAP + barDx;
            int valueReserve = Math.round(VALUE_RESERVE * barScale);
            // Bar size is fixed at the natural room left at scale 1.0 (unaffected by the live box size or by
            // Text/Icon size), like the Nutrient HUD's bars.
            int naturalReserved = PADDING * 2 + ICON_SIZE + HudDrawHelpers.ICON_LABEL_GAP + maxLabelWNatural
                    + HudDrawHelpers.LABEL_BAR_GAP + VALUE_RESERVE + HudDrawHelpers.BAR_PCT_GAP;
            int naturalBarW = Math.max(1, naturalSize(rows.size()).width() - naturalReserved);
            int barW = Math.max(1, Math.round(naturalBarW * barScale));
            int barH = Math.max(1, Math.round(HudDrawHelpers.BAR_H * barScale));

            int rowsTop = bounds.y() + padding + HEADER_HEIGHT + HEADER_GAP + contentOffsetY;
            // Whatever doesn't fit is handled by scrolling, not shrinking or a silent hard clip: only the rows in
            // [scrollOffset, scrollOffset + capacity) are drawn, and drawScrollIndicator marks that
            // there's more above/below when capacity < rows.size().
            int capacity = visibleRowCapacity(bounds);
            int maxScroll = Math.max(0, rows.size() - capacity);
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
            int lastVisible = Math.min(rows.size(), scrollOffset + capacity);
            int y = rowsTop;
            // Enlarged text/icons are clipped at their own row, as in the Nutrient HUD — unless a move
            // offset is set, which deliberately places content outside its slot.
            boolean clipRows = contentOffsetX == 0 && contentOffsetY == 0 && iconDx == 0 && iconDy == 0 && barDx == 0 && barDy == 0;
            for (int i = scrollOffset; i < lastVisible; i++) {
                if (clipRows) {
                    context.pushClip(bounds.x(), y, bounds.width(), lineHeight);
                }
                try {
                    Row row = rows.get(i);
                    TrackerRow tracker = TRACKERS.get(i);
                    int color = ActivityDrivenNutrientRegistry.getColor(row.moduleId()).orElse(defaultTextColor);
                    int rowCenterY = y + lineHeight / 2;
                    int textY = rowCenterY - (int) Math.ceil(9 * contentScale) / 2;

                    if (!MarieModuleSettings.isIconsHidden(UiStatePersistence.get(), PANEL_ID)) context.drawItem(tracker.icon(), bounds.x() + leftMargin + padding + iconDx, rowCenterY + (iconDy - contentOffsetY) - iconSize / 2, iconSize / 16f * (float) iconScale);
                    context.drawText(row.label(), rowsX + iconSize + HudDrawHelpers.ICON_LABEL_GAP, textY, color, (float) contentScale);

                    // Self-relative per tracker, never compared against the other four: mining/combat
                    // counts and sprint/swim distances live on wildly different natural scales, so a bar
                    // shared across all five would always read the small-number trackers as near-empty.
                    // denom = max(historicalMax, today's value) means a tracker with no history yet (day
                    // one) still gets a full/near-full bar for today's own value instead of a 0/0 divide
                    // or a permanently-empty bar.
                    float denom = Math.max(row.historicalMax(), row.value());
                    float fillPct = denom > 0f ? row.value() / denom : 0f;
                    int barShiftY = barDy - contentOffsetY;
                    int barY = rowCenterY + barShiftY - barH / 2;
                    context.drawBar(barX, barY, barW, barH, fillPct, barBg, color);

                    int valueX = barX + barW + HudDrawHelpers.BAR_PCT_GAP;
                    context.drawText(row.formattedValue(), valueX, rowCenterY + barShiftY - (int) Math.ceil(9 * barScale) / 2, color, barScale);

                } finally {
                    if (clipRows) {
                        context.popClip();
                    }
                }
                y += lineHeight;
            }

            if (editMode && (moveTextMode || moveIconsMode || moveBarsMode || moveAllMode)) {
                // Dashed rather than a solid glow outline — a live drag affordance shown only while a
                // move toggle is active, not a persistent separately-hit-tested box. Text mode wraps the
                // name column, icons mode the icon column, bars mode the bar+value column, top through the
                // last visible row, a few pixels further out so it doesn't overlap them.
                int contentRight = barX + barW + HudDrawHelpers.BAR_PCT_GAP + VALUE_RESERVE;
                if (moveAllMode) {
                    context.drawDashedBorder(rowsX - 3, rowsTop - 3, contentRight - rowsX + 6, y - rowsTop + 6, titleAccentColor());
                } else if (moveTextMode) {
                    context.drawDashedBorder(rowsX + iconSize + HudDrawHelpers.ICON_LABEL_GAP - 3, rowsTop - 3, maxLabelW + 6, y - rowsTop + 6, titleAccentColor());
                } else if (moveIconsMode) {
                    context.drawDashedBorder(bounds.x() + leftMargin + padding + iconDx - 3, rowsTop - contentOffsetY + iconDy - 3, Math.round(iconSize * (float) iconScale) + 6, y - rowsTop + 6, titleAccentColor());
                } else {
                    context.drawDashedBorder(barX - 3, rowsTop - contentOffsetY + barDy - 3, contentRight - barX + 6, y - rowsTop + 6, titleAccentColor());
                }
            }

            if (maxScroll > 0) {
                drawScrollIndicator(context, bounds, rowsTop, capacity, lineHeight, rows.size(), scrollOffset);
            }
        } finally {
            context.popClip();
        }
    }

    /** Thin track+thumb on the box's right inner edge, only drawn when {@link #scrollOffset} can't show every row at once — a plain manual scrollbar since {@link RenderContext} has no dedicated primitive for one (its {@code drawVerticalBar} fills a percentage from an edge, not a repositionable thumb). */
    private static void drawScrollIndicator(RenderContext context, Bounds bounds, int rowsTop, int capacity, int lineHeight, int rowCount, int scrollOffset) {
        int trackX = bounds.x() + bounds.width() - 3;
        int trackH = capacity * lineHeight;
        int thumbH = Math.max(4, trackH * capacity / rowCount);
        int maxScroll = rowCount - capacity;
        int thumbY = rowsTop + (maxScroll > 0 ? (trackH - thumbH) * scrollOffset / maxScroll : 0);
        context.fillRect(trackX, rowsTop, 2, trackH, context.theme().color(ThemeKey.BAR_BACKGROUND));
        context.fillRect(trackX, thumbY, 2, thumbH, titleAccentColor());
    }

    /** Carries forward the existing persisted contentScale/paddingScale so a drag/resize commit never resets the user's text-scale or padding adjustment. */
    private void commit(Bounds bounds) {
        var base = UiStatePersistence.get().load(PANEL_ID);
        boolean widthManual = base.map(ComponentState::widthManual).orElse(false) || drag.lastCommitAffectedWidth();
        boolean heightManual = base.map(ComponentState::heightManual).orElse(false) || drag.lastCommitAffectedHeight();
        double contentScale = base.map(ComponentState::contentScale).orElse(ComponentState.DEFAULT_CONTENT_SCALE);
        double paddingScale = base.map(ComponentState::paddingScale).orElse(ComponentState.DEFAULT_PADDING_SCALE);
        int leftMargin = AutoGrowPanelContainer.leftMarginAfterResize(persistedLeftMargin(),
                resolvedBounds(currentRows().size()).width(), bounds.width(), drag.lastCommitWasLeftEdge());
        UiStatePersistence.get().save(PANEL_ID, new ComponentState(
                bounds.x(), bounds.y(), bounds.width(), bounds.height(), false,
                widthManual, heightManual, leftMargin, contentScale, paddingScale));
    }

    /** Dead space between the box's left edge and its content, grown only by a left-edge/bottom-left-corner resize — see {@link AutoGrowPanelContainer#leftMarginAfterResize}. */
    private static int persistedLeftMargin() {
        return UiStatePersistence.get().load(PANEL_ID).map(ComponentState::leftMargin).orElse(0);
    }

    /** {@link #persistedLeftMargin()} plus the live left-edge gesture's width delta, so the preview keeps content still while dragging. */
    private static int liveLeftMargin(DraggableResizable drag, Bounds liveBounds, Bounds committedBounds) {
        return AutoGrowPanelContainer.leftMarginAfterResize(persistedLeftMargin(),
                committedBounds.width(), liveBounds.width(), drag.isLeftEdgeGestureActive());
    }

    /** This panel's persisted text-scale multiplier — defaults to {@link ComponentState#DEFAULT_CONTENT_SCALE} if never set. */
    private static double persistedContentScale() {
        return UiStatePersistence.get().load(PANEL_ID).map(ComponentState::contentScale).orElse(ComponentState.DEFAULT_CONTENT_SCALE);
    }

    /** This panel's persisted padding multiplier — defaults to {@link ComponentState#DEFAULT_PADDING_SCALE} if never set. */
    private static double persistedPaddingScale() {
        return UiStatePersistence.get().load(PANEL_ID).map(ComponentState::paddingScale).orElse(ComponentState.DEFAULT_PADDING_SCALE);
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Constraint constraint() {
        // Unused: DraggableResizable clamps against the Constraint passed to its own constructor,
        // never against target.constraint() — same as HudEditTarget's constraint() override.
        return Constraint.preferred(0, 0);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (scaleConfigVisible && scaleConfigPanel.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        Bounds bounds = resolvedBounds(currentRows().size());
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
        return drag.mouseClicked((int) mouseX, (int) mouseY, bounds);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scaleConfigVisible && scaleConfigPanel.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        List<Row> rows = currentRows();
        Bounds bounds = resolvedBounds(rows.size());
        if (scrollY == 0 || !bounds.contains((int) mouseX, (int) mouseY)) {
            return false;
        }
        int maxScroll = Math.max(0, rows.size() - visibleRowCapacity(bounds));
        if (maxScroll <= 0) {
            return false;
        }
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) Math.signum(scrollY)));
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (scaleConfigVisible && scaleConfigPanel.mouseDragged(mouseX, mouseY, button)) {
            return true;
        }
        if (moveDrag.isActive()) {
            Bounds bounds = resolvedBounds(currentRows().size());
            int x = clampContentOffsetX(moveDrag.offsetX(mouseX), bounds);
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
                        contentOffsetX = clampContentOffsetX(moveDrag.baseX(MoveDrag.Mode.TEXT) + dx, bounds);
                        contentOffsetY = clampContentOffsetY(moveDrag.baseY(MoveDrag.Mode.TEXT) + dy, bounds);
                        MarieModuleSettings.setIconOffset(UiStatePersistence.get(), PANEL_ID, clampContentOffsetX(moveDrag.baseX(MoveDrag.Mode.ICONS) + dx, bounds), clampContentOffsetY(moveDrag.baseY(MoveDrag.Mode.ICONS) + dy, bounds));
                        MarieModuleSettings.setBarOffset(UiStatePersistence.get(), PANEL_ID, clampContentOffsetX(moveDrag.baseX(MoveDrag.Mode.BARS) + dx, bounds), clampContentOffsetY(moveDrag.baseY(MoveDrag.Mode.BARS) + dy, bounds));
                    }
                }
            return true;
        }
        if (drag.isDragging() || drag.isResizing()) {
            drag.mouseDragged((int) mouseX, (int) mouseY);
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
        boolean any = drag.isDragging() || drag.isResizing();
        drag.mouseReleased((int) mouseX, (int) mouseY);
        return any;
    }

    @Override
    public void render(RenderContext context, Bounds ignoredBounds) {
        List<Row> rows = currentRows();
        drag.setConstraint(panelConstraintFor(naturalSize(rows.size())));

        int[] mouse = scaledMouse(Minecraft.getInstance());
        Bounds defaultBounds = resolvedBounds(rows.size());
        Bounds bounds = liveOrDefault(drag, mouse[0], mouse[1], defaultBounds);

        boolean moveTextMode = moveContentEnabled();
        boolean moveBarsMode = moveBarsEnabled();
        boolean moveIconsMode = MarieModuleSettings.isMoveIconsEnabled(UiStatePersistence.get(), PANEL_ID);
        boolean moveAllMode = MarieModuleSettings.isMoveAllEnabled(UiStatePersistence.get(), PANEL_ID);
        // Re-clamped defensively here too — see the same comment on the onRenderGuiPost call site.
        int offsetX = clampContentOffsetX(contentOffsetX, bounds);
        int offsetY = clampContentOffsetY(contentOffsetY, bounds);
        drawPanel(MarieModuleSettings.withBrightness(context, NourishedClientConfig.get().activityLogHudTextBrightness(), NourishedClientConfig.get().activityLogHudIconBrightness()),
                bounds, liveLeftMargin(drag, bounds, defaultBounds), offsetX, offsetY, rows, true, moveTextMode, moveIconsMode, moveBarsMode, moveAllMode);

        // While move-content mode is active, dragging is exclusively routed to the content offset
        // (see mouseClicked/mouseDragged) — the panel's own resize handles would be inert, so they
        // aren't drawn, to avoid implying they still work.
        if (!moveTextMode && !moveIconsMode && !moveBarsMode && !moveAllMode) {
            Bounds handle = DraggableResizable.handleBounds(bounds);
            context.drawResizeHandle(handle.x(), handle.y(), drag.isHandleHovered(mouse[0], mouse[1], bounds), drag.isHandleActive());
            Bounds handleBL = DraggableResizable.handleBoundsBottomLeft(bounds);
            context.drawResizeHandle(handleBL.x(), handleBL.y(), drag.isHandleBottomLeftHovered(mouse[0], mouse[1], bounds), drag.isBottomLeftCornerActive());
            for (DraggableResizable.Edge edge : DraggableResizable.Edge.values()) {
                Bounds strip = DraggableResizable.edgeHandleBounds(bounds, edge);
                context.drawEdgeHandle(strip.x(), strip.y(), strip.width(), strip.height(), mouse[0], mouse[1],
                        drag.isEdgeHovered(mouse[0], mouse[1], bounds, edge), drag.isEdgeActive(edge));
            }
        }

        if (scaleConfigVisible) {
            scaleConfigPanel.render(context, new Bounds(0, 0, context.screenWidth(), context.screenHeight()));
        }
    }

    private static Bounds liveOrDefault(DraggableResizable d, int mx, int my, Bounds fallback) {
        if (d.isDragging() || d.isResizing()) {
            Bounds preview = d.mouseDragged(mx, my);
            if (preview != null) {
                return preview;
            }
        }
        return fallback;
    }

    private static int[] scaledMouse(Minecraft mc) {
        double s = mc.getWindow().getGuiScale();
        return new int[]{(int) (mc.mouseHandler.xpos() / s), (int) (mc.mouseHandler.ypos() / s)};
    }
}
