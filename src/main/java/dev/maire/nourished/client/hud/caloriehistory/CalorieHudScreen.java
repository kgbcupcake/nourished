package dev.maire.nourished.client.hud.caloriehistory;

import dev.maire.nourished.client.colors.NourishedColors;
import dev.maire.nourished.client.colors.NourishedColorSlots;
import dev.marie.framework.ui.api.MarieModuleSettings;
import dev.marie.framework.ui.api.MoveDrag;
import dev.marie.framework.client.config.state.MarieClientCache;
import dev.marie.framework.color.ColorKeyPair;
import dev.marie.framework.color.MarieColors;
import dev.marie.framework.config.FeatureFlagCache;
import dev.marie.framework.tracking.TrackingData;
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
import dev.maire.nourished.api.NourishedAPI;
import dev.maire.nourished.client.NourishedKeys;
import dev.maire.nourished.client.UiStatePersistence;
import dev.maire.nourished.client.hud.dynamic.HudDrawHelpers;
import dev.maire.nourished.config.NourishedClientConfig;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.modules.activity_driven_nutrient.client.ActivityLogHudPanel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Persistent, player-facing HUD panel showing the calories tracker's history — a live "Today" row
 * plus completed daily periods (see {@link NourishedAPI#CALORIES_TRACKER_ID}). Same tier as {@code
 * NourishedHUD}/{@code ActivityLogHudPanel}: config-toggleable, draggable/resizable via its own
 * edit-mode keybind ({@link NourishedKeys#EDIT_CALORIE_HUD}), independent of the nutrition HUD's
 * and activity log HUD's edit modes.
 *
 * <p>Unlike {@code ActivityLogHudPanel}, which reads a client-local ring buffer of live-observed
 * events, calorie history is already server-authoritative: it's synced down whole as part of
 * {@link TrackingData#trackingHistory} on full tracking sync (see {@code
 * MarieClientCache#onFullTrackingSync}), the same field {@code CaloriesComponent}'s "yesterday"
 * line already reads via {@link MarieClientCache#get()}. There is no client-local buffer to
 * maintain here — this panel just reads that synced snapshot directly.
 *
 * <p>Unlike {@code NourishedHUD}/{@code HudEditTarget}, this panel has no per-frame lerped display
 * state to share between a normal render path and an edit-mode wrapper, so both live in this one
 * {@link MarieComponent} implementation: {@link #onRenderGuiPost} draws it directly outside edit
 * mode, and {@link #render} (via {@link EditModeController}'s overlay) draws the live drag/resize
 * preview.
 */
public final class CalorieHudScreen implements MarieComponent {

    private static final String ID = "nourished.calorieHud.panel";
    private static final String PANEL_ID = "nourished.calorieHud.panel";
    private static final String CONTENT_OFFSET_ID = "nourished.calorieHud.contentOffset";

    private static final int PANEL_WIDTH = 190;
    private static final int LINE_HEIGHT = 12;
    private static final int PADDING = 4;
    private static final int DEFAULT_X = 8;
    private static final int DEFAULT_Y = 60;

    /** Gap kept below the Activity Log HUD's actual bottom edge when stacking this box's default underneath it — see {@link #defaultY}. */
    private static final int STACKED_DEFAULT_GAP = 6;

    /** Row icon size, matching {@code HudLayout}'s own icon-size floor/scale reasoning. */
    private static final int ICON_SIZE = 10;
    /** Fixed-width reserve for the trailing percentage-of-goal text, so bars stay aligned even as the number of digits varies. */
    private static final int PCT_RESERVE = 30;
    /** Color of a bar that has gone over its goal: calories have no other established graduated color rule, so over-goal is a flat shift from the calorie value color to this (see {@code NourishedColors.CALORIE_OVER_GOAL}). */
    private static int colOverGoal() {
        return MarieColors.resolveColor(NourishedColors.CALORIE_OVER_GOAL);
    }

    /** Every row shows a calorie value, so every row shares the same icon — the same item {@code CaloriesComponent} already uses for its own calorie icon. */
    private static final ItemStack CALORIE_ICON = new ItemStack(Items.FIRE_CHARGE);

    /** Title row height/gap, unscaled by contentScale — chrome, not user content, matching {@code ScaleConfigPanel}'s own fixed HEADER_HEIGHT. */
    private static final int HEADER_HEIGHT = 12;
    private static final int HEADER_GAP = 2;
    /** Slightly larger than 1.0 stands in for "bold" — same trick {@code ScaleConfigPanel#drawCard} uses for its own header. */
    private static final float TITLE_SCALE = 1.05f;
    /** Orange-red accent — one of {@code ScaleConfigPanel.ACCENT_PALETTE}'s colors, reused here since this card now follows that same visual language. */
    private static int titleAccentColor() {
        return MarieColors.resolveColor(NourishedColors.CALORIE_ACCENT);
    }

    /** How much bigger than content's natural size the box may be dragged, on either axis. */
    private static final double MAX_MARGIN_MULTIPLIER = 5.0d;

    /** How much smaller than content's natural size the box may be dragged, on either axis. */

    private static CalorieHudScreen instance;
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
     * while it's on, they move the content.
     */
    private int contentOffsetX;
    private int contentOffsetY;

    /**
     * Editor for this panel's persisted contentScale/paddingScale, auto-shown alongside edit mode —
     * same pattern as {@code DietScreen#scaleConfigPanel}, just with a single entry since this panel
     * has no sub-boxes.
     */
    private final ScaleConfigPanel scaleConfigPanel = MarieScaleConfig.create(
            List.of(new ScaleConfigEntry(PANEL_ID, Component.translatable("nourished.hud.calorieHistory.label"))
                    .withContent(MarieModuleSettings.standardPanel(Component.translatable("nourished.hud.calorieHistory.label").getString(), UiStatePersistence.get(), PANEL_ID)
                            .opacity(() -> NourishedClientConfig.get().calorieHudBackgroundOpacity(), v -> NourishedClientConfig.get().setCalorieHudBackgroundOpacity(v), 204.0d / 255.0d)
                            .textBrightness(() -> NourishedClientConfig.get().calorieHudTextBrightness(), v -> NourishedClientConfig.get().setCalorieHudTextBrightness(v))
                            .iconBrightness(() -> NourishedClientConfig.get().calorieHudIconBrightness(), v -> NourishedClientConfig.get().setCalorieHudIconBrightness(v))
                            .backgroundShade(() -> NourishedClientConfig.get().calorieHudBackgroundShade(), v -> NourishedClientConfig.get().setCalorieHudBackgroundShade(v))
                            .borderOpacity(() -> NourishedClientConfig.get().calorieHudBorderOpacity(), v -> NourishedClientConfig.get().setCalorieHudBorderOpacity(v))
                            .borderShade(() -> NourishedClientConfig.get().calorieHudBorderShade(), v -> NourishedClientConfig.get().setCalorieHudBorderShade(v))
                            .onCommit(NourishedClientConfig::saveNow)
                            .onReset(this::resetContentOffset)
                            .extraTabs(panel -> {
                                panel.colorTab(Component.translatable("config.marieslib.moduleoptions.tab.colors").getString());
                                NourishedColorSlots.addPair(panel, COLORS);
                                NourishedColorSlots.addFixed(panel, NourishedColors.CALORIE_VALUE, "nourished.options.color.calorie");
                                NourishedColorSlots.addFixed(panel, NourishedColors.CALORIE_OVER_GOAL, "nourished.options.color.over_goal");
                                NourishedColorSlots.addFixed(panel, NourishedColors.CALORIE_ACCENT, "nourished.options.color.accent");
                                NourishedColorSlots.addFixed(panel, NourishedColors.CALORIE_HUD_BORDER, "nourished.options.color.border");
                            })
                            .build())),
            UiStatePersistence.get(), Anchor.TOP_RIGHT);
    private boolean scaleConfigVisible;

    private CalorieHudScreen() {
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
    public static CalorieHudScreen instance() {
        if (instance == null) {
            instance = new CalorieHudScreen();
        }
        return instance;
    }

    private static EditModeController editModeController() {
        if (editModeController == null) {
            editModeController = new EditModeController(
                    instance(),
                    "Drag the Calorie History HUD to reposition, drag the corner handle to resize. C or Esc to exit.",
                    NourishedKeys.EDIT_CALORIE_HUD.getKey().getValue(),
                    () -> {}
            );
        }
        return editModeController;
    }

    /**
     * C ({@link NourishedKeys#EDIT_CALORIE_HUD}) enters edit mode through here so the scale panel
     * always appears alongside it — same pattern as {@code DietScreen#enterEditModeWithScaleConfig}.
     * Only sets {@link #scaleConfigVisible} {@code true} on entry; exiting edit mode is untouched.
     */
    private void enterEditModeWithScaleConfig() {
        scaleConfigVisible = true;
        editModeController().enter();
    }

    /**
     * Group-entry counterpart to {@link #enterEditModeWithScaleConfig()} — sets {@link
     * #scaleConfigVisible} {@code true} without itself entering edit mode, since the group path
     * (unlike the {@code C} keybind's individual path above) already opens the shared overlay via
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
        if (!FeatureFlagCache.enableCalorieHistory() || !NourishedClientConfig.get().enableCalorieHistoryHud()) {
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
        CalorieHudScreen self = instance();
        // Re-clamped defensively at draw time too (not just on drag) — the panel/row count this
        // offset is valid against can change independently of dragging (a row added/removed, or the
        // panel resized), and a stale offset should self-correct visually rather than let content
        // drift outside the panel until the user happens to drag it again.
        int offsetX = clampContentOffsetX(self.contentOffsetX, bounds);
        int offsetY = clampContentOffsetY(self.contentOffsetY, bounds);
        GuiGraphicsRenderContext context = new GuiGraphicsRenderContext(
                event.getGuiGraphics(), mc, Theme.DARK, event.getPartialTick().getGameTimeDeltaPartialTick(false));
        // Defense-in-depth: resetClip() forces the scissor stack/GL state back to empty even if
        // drawPanel throws partway through its pushClip/popClip pair — see
        // GuiGraphicsRenderContext#resetClip.
        try {
            drawPanel(MarieModuleSettings.withBrightness(context, NourishedClientConfig.get().calorieHudTextBrightness(), NourishedClientConfig.get().calorieHudIconBrightness()),
                    bounds, persistedLeftMargin(), offsetX, offsetY, rows, false, false, false, false, false);
        } finally {
            context.resetClip();
        }
    }

    /**
     * Live "Today" row (if any) followed by completed-period history, newest first, already
     * retention-capped server-side.
     *
     * <p>The "Today" row reads the calorie tracker's live per-day accumulator via {@link
     * MarieTracking#getCurrentTrackerValue}, the same figure as the diet screen's Calories box.
     * It resets at each game-day rollover, and matches the units of the history rows below it.
     * {@code TrackingData#total} is the lifetime figure and is never shown as "Today".
     */
    private static List<Row> currentRows() {
        List<Row> rows = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        TrackingData data = MarieClientCache.get();
        if (mc.player != null) {
            rows.add(new Row(Component.translatable("nourished.hud.calorieHistory.today").getString(), MarieTracking.getCurrentTrackerValue(mc.player, NourishedAPI.CALORIES_TRACKER_ID), true));
        }
        List<TrackerHistoryEntry> history = data.trackingHistory.get(NourishedAPI.CALORIES_TRACKER_ID);
        if (history != null) {
            for (int i = 0; i < history.size(); i++) {
                TrackerHistoryEntry entry = history.get(i);
                Component label = Component.translatable(i == 0
                        ? "nourished.hud.calorieHistory.oneDay"
                        : "nourished.hud.calorieHistory.days", i + 1);
                rows.add(new Row(label.getString(), entry.value(), false));
            }
        }
        return rows;
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            return;
        }
        if (!FeatureFlagCache.enableCalorieHistory() || !NourishedClientConfig.get().enableCalorieHistoryHud()) {
            return;
        }
        while (NourishedKeys.EDIT_CALORIE_HUD.consumeClick()) {
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
                .orElseGet(() -> new Bounds(DEFAULT_X, defaultY(), natural.width(), natural.height()));
    }

    /**
     * {@link #DEFAULT_Y}, or lower still if the Activity Log HUD's current bounds sit in the same
     * horizontal column and reach past it — a fixed install used to give both boxes fixed defaults only
     * 52px apart, which Activity Log's natural height (row-count-dependent) regularly exceeds (any more
     * than ~2 tracked activities), so the two overlapped before either box had ever been dragged. Reads
     * Activity Log's *current* bounds (default or user-moved) rather than assuming it's still at its own
     * unmoved default, so this box's own still-unmoved default keeps avoiding it wherever it actually is;
     * outside that column (e.g. the player moved Activity Log elsewhere), there's nothing to avoid and
     * this falls back to the plain {@link #DEFAULT_Y}.
     */
    private static int defaultY() {
        Bounds activityLog = ActivityLogHudPanel.currentBoundsForStacking();
        if (activityLog == null) {
            return DEFAULT_Y;
        }
        boolean sameColumn = activityLog.x() < DEFAULT_X + PANEL_WIDTH && activityLog.x() + activityLog.width() > DEFAULT_X;
        if (!sameColumn) {
            return DEFAULT_Y;
        }
        return Math.max(DEFAULT_Y, activityLog.y() + activityLog.height() + STACKED_DEFAULT_GAP);
    }

    /** How many rows fit vertically in {@code bounds} at the current content scale — shared by {@link #drawPanel} (what to draw) and {@link #mouseScrolled} (how far scrolling can go). */
    private static int visibleRowCapacity(Bounds bounds) {
        double contentScale = ContentScaleController.resolveContentScale(persistedContentScale());
        int lineHeight = LINE_HEIGHT;
        double userPadding = PADDING * persistedPaddingScale();
        int padding = Math.round(ContentScaleController.resolvePadding(userPadding));
        return Math.max(1, (bounds.height() - padding * 2 - HEADER_HEIGHT - HEADER_GAP) / lineHeight);
    }

    /** Set by {@link Nourished#registerColorDefinitions()} at mod init. */
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
                MarieColors.shade(panelRgb, cc.calorieHudBackgroundShade()), cc.calorieHudBackgroundOpacity());
        int borderColor = MarieColors.withOpacity(
                MarieColors.shade(MarieColors.resolveColor(NourishedColors.CALORIE_HUD_BORDER), cc.calorieHudBorderShade()), cc.calorieHudBorderOpacity());
        context.drawRoundedRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), 1, HudDrawHelpers.PANEL_CORNER_RADIUS, panelColor, borderColor);
        context.pushClip(bounds.x(), bounds.y(), bounds.width(), bounds.height());
        try {
            context.drawText(Component.translatable("nourished.hud.calorieHistory.label").getString(),
                    bounds.x() + leftMargin + padding, bounds.y() + padding, titleAccentColor(), TITLE_SCALE);

            int rowsX = bounds.x() + leftMargin + padding + contentOffsetX;
            // "Move Bars" offset: bars and their value text sit at the unshifted content origin plus this,
            // independent of the icon/name offset above.
            int barDx = clampContentOffsetX(MarieModuleSettings.barOffsetX(UiStatePersistence.get(), PANEL_ID), bounds);
            int barDy = clampContentOffsetY(MarieModuleSettings.barOffsetY(UiStatePersistence.get(), PANEL_ID), bounds);
            // "Move Icons" offset, and the Bar size multiplier (bar length/thickness and the value text at its end).
            int iconDx = clampContentOffsetX(MarieModuleSettings.iconOffsetX(UiStatePersistence.get(), PANEL_ID), bounds);
            int iconDy = clampContentOffsetY(MarieModuleSettings.iconOffsetY(UiStatePersistence.get(), PANEL_ID), bounds);
            float barScale = ContentScaleController.resolveContentScale(MarieModuleSettings.barScale(UiStatePersistence.get(), PANEL_ID));
            int rowsTop = bounds.y() + padding + HEADER_HEIGHT + HEADER_GAP + contentOffsetY;

            int labelColor = MarieColors.resolveColor(COLORS.text());
            int barBg = HudDrawHelpers.barBackgroundColor();
            float maxTotal = MarieClientCache.get().maxTotal;

            // Aligns every row's bar to the same x regardless of that row's own label width ("Today"
            // vs. "5 days ago"), same reasoning as HudLayout#maxLabelSw for the dynamic nutrient HUD.
            int maxLabelW = 0;
            int maxLabelWNatural = 0;
            for (Row row : rows) {
                maxLabelW = Math.max(maxLabelW, context.textWidth(row.label(), 1f));
                maxLabelWNatural = Math.max(maxLabelWNatural, context.textWidth(row.label(), 1f));
            }
            int barX = bounds.x() + leftMargin + padding + iconSize + HudDrawHelpers.ICON_LABEL_GAP + maxLabelW + HudDrawHelpers.LABEL_BAR_GAP + barDx;
            int pctReserve = Math.round(PCT_RESERVE * barScale);
            // Bar size is fixed at the natural room left at scale 1.0 (unaffected by the live box size or by
            // Text/Icon size), like the Nutrient HUD's bars.
            int naturalReserved = PADDING * 2 + ICON_SIZE + HudDrawHelpers.ICON_LABEL_GAP + maxLabelWNatural
                    + HudDrawHelpers.LABEL_BAR_GAP + PCT_RESERVE + HudDrawHelpers.BAR_PCT_GAP;
            int naturalBarW = Math.max(1, naturalSize(rows.size()).width() - naturalReserved);
            int barW = Math.max(1, Math.round(naturalBarW * barScale));
            int barH = Math.max(1, Math.round(HudDrawHelpers.BAR_H * barScale));

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
                    int rowCenterY = y + lineHeight / 2;
                    int textY = rowCenterY - (int) Math.ceil(9 * contentScale) / 2;

                    if (!MarieModuleSettings.isIconsHidden(UiStatePersistence.get(), PANEL_ID)) context.drawItem(CALORIE_ICON, bounds.x() + leftMargin + padding + iconDx, rowCenterY + (iconDy - contentOffsetY) - iconSize / 2, iconSize / 16f * (float) iconScale);
                    context.drawText(row.label(), rowsX + iconSize + HudDrawHelpers.ICON_LABEL_GAP, textY, labelColor, (float) contentScale);

                    float pct = maxTotal > 0 ? row.value() / maxTotal : 0f;
                    boolean overGoal = pct > 1f;
                    float cappedPct = Mth.clamp(pct, 0f, 1f);
                    // Calories have no established graduated color rule (unlike nutrient bars) — the
                    // codebase always draws calorie values in HudDrawHelpers.calorieColor(), so that's
                    // reused as-is; over-goal is the only shift, to the same red used everywhere else in
                    // this codebase for "over threshold". The bar itself stays capped at 100% width
                    // regardless, per the "don't draw past the bar's bounds" requirement.
                    int fillColor = overGoal ? colOverGoal() : HudDrawHelpers.calorieColor();
                    int barShiftY = barDy - contentOffsetY;
                    int barY = rowCenterY + barShiftY - barH / 2;
                    context.drawBar(barX, barY, barW, barH, cappedPct, barBg, fillColor);

                    String pctText = Math.round(pct * 100) + "%";
                    int pctX = barX + barW + HudDrawHelpers.BAR_PCT_GAP;
                    context.drawText(pctText, pctX, rowCenterY + barShiftY - (int) Math.ceil(9 * barScale) / 2, fillColor, barScale);

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
                int contentRight = barX + barW + HudDrawHelpers.BAR_PCT_GAP + PCT_RESERVE;
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
        drawPanel(MarieModuleSettings.withBrightness(context, NourishedClientConfig.get().calorieHudTextBrightness(), NourishedClientConfig.get().calorieHudIconBrightness()),
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

    /** One rendered row: {@code live} is true only for the in-progress "Today" row. {@code value} is the raw calorie total for that day, scored against {@link TrackingData#maxTotal} for the bar/percentage. */
    private record Row(String label, float value, boolean live) {}
}
