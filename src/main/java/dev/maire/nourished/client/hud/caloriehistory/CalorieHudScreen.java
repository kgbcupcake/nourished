package dev.maire.nourished.client.hud.caloriehistory;

import dev.marie.framework.client.config.state.MarieClientCache;
import dev.marie.framework.color.ColorKeyPair;
import dev.marie.framework.color.MarieColors;
import dev.marie.framework.config.FeatureFlagCache;
import dev.marie.framework.tracking.TrackingData;
import dev.marie.framework.tracking.tracker.definition.TrackerHistoryEntry;
import dev.marie.framework.ui.RenderContext;
import dev.maire.nourished.client.hud.dynamic.options.ModuleOptionsPanel;
import dev.maire.nourished.client.render.BrightnessRenderContext;
import dev.marie.framework.ui.Theme;
import dev.marie.framework.ui.ThemeKey;
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

    /** Row icon size, matching {@code HudLayout}'s own icon-size floor/scale reasoning. */
    private static final int ICON_SIZE = 10;
    /** Fixed-width reserve for the trailing percentage-of-goal text, so bars stay aligned even as the number of digits varies. */
    private static final int PCT_RESERVE = 30;
    /** Same "over-threshold" red used throughout the codebase (see {@code HudDrawHelpers#barFillColor}/{@code ClassicDietColorLogic}) — calories have no other established graduated color rule, so over-goal is a flat color shift from the established calorie color to this. */
    private static final int COL_OVER_GOAL = 0xFFFF5555;

    /** Every row shows a calorie value, so every row shares the same icon — the same item {@code CaloriesComponent} already uses for its own calorie icon. */
    private static final ItemStack CALORIE_ICON = new ItemStack(Items.FIRE_CHARGE);

    /** Title row height/gap, unscaled by contentScale — chrome, not user content, matching {@code ScaleConfigPanel}'s own fixed HEADER_HEIGHT. */
    private static final int HEADER_HEIGHT = 12;
    private static final int HEADER_GAP = 2;
    /** Slightly larger than 1.0 stands in for "bold" — same trick {@code ScaleConfigPanel#drawCard} uses for its own header. */
    private static final float TITLE_SCALE = 1.05f;
    /** Orange-red accent — one of {@code ScaleConfigPanel.ACCENT_PALETTE}'s colors, reused here since this card now follows that same visual language. */
    private static final int TITLE_ACCENT_COLOR = 0xFFE98F5D;

    /** How much bigger than content's natural size the box may be dragged, on either axis. */
    private static final double MAX_MARGIN_MULTIPLIER = 5.0d;

    /** How much smaller than content's natural size the box may be dragged, on either axis. */
    private static final double MIN_SHRINK_SCALE = 0.5d;

    private static CalorieHudScreen instance;
    private static EditModeController editModeController;

    /** Row scroll position (in rows, not pixels) into {@link #currentRows()} — only movable while edit mode has this panel's mouse input wired up; see {@link #mouseScrolled}. Clamped every {@link #drawPanel} pass against however many rows currently fit, so it self-corrects if the row count or box size changes out from under it. */
    private static int scrollOffset;

    private final DraggableResizable drag;

    /** Whether a content-move drag (see {@link #moveContentEnabled}) is currently in progress. */
    private boolean draggingContent;
    private int contentGrabOffsetX;
    private int contentGrabOffsetY;

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
                    .withContent(ModuleOptionsPanel.build(Component.translatable("nourished.hud.calorieHistory.label").getString(), PANEL_ID,
                            () -> NourishedClientConfig.get().calorieHudBackgroundOpacity(),
                            v -> NourishedClientConfig.get().setCalorieHudBackgroundOpacity(v),
                            () -> NourishedClientConfig.get().calorieHudContentBrightness(),
                            v -> NourishedClientConfig.get().setCalorieHudContentBrightness(v)))),
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
        Size minSize = new Size(
                (int) (natural.width() * MIN_SHRINK_SCALE), (int) (natural.height() * MIN_SHRINK_SCALE));
        return new Constraint(
                natural, minSize,
                new Size((int) (natural.width() * MAX_MARGIN_MULTIPLIER), (int) (natural.height() * MAX_MARGIN_MULTIPLIER)),
                false, false, true, true,
                Anchor.TOP_LEFT, Insets.NONE, Insets.NONE
        );
    }

    /** Persists {@link #contentOffsetX}/{@link #contentOffsetY} — {@code width}/{@code height}/the manual-size and scale fields are unused for this key. */
    private void persistContentOffset() {
        UiStatePersistence.get().save(CONTENT_OFFSET_ID, new ComponentState(contentOffsetX, contentOffsetY, 0, 0, false, false, false, 0));
    }

    /** The "Move Text and Icons" toggle's live state, owned by {@link #scaleConfigPanel} (its own editor window, under Padding) rather than a button on this panel itself. */
    private boolean moveContentEnabled() {
        return scaleConfigPanel.isMoveContentEnabled(PANEL_ID);
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
            drawPanel(BrightnessRenderContext.wrap(context, NourishedClientConfig.get().calorieHudContentBrightness()),
                    bounds, offsetX, offsetY, rows, false, false);
        } finally {
            context.resetClip();
        }
    }

    /**
     * Live "Today" row (if any) followed by completed-period history, newest first, already
     * retention-capped server-side.
     *
     * <p>The "Today" row reads {@code NourishedAPI#getTotal}'s value off {@link
     * MarieClientCache#get()} — the same current-calories figure shown on the diet screen's
     * Calories box — not {@link TrackingData#trackingAccumulators} (the tracker's own per-day sum,
     * which lags behind on its throttled sync and isn't the number players expect "Today" to
     * match). {@code MarieClientCache} is the snapshot Nourished's delta-sync pipeline actually
     * keeps current, and matches the units of the history rows below it.
     */
    private static List<Row> currentRows() {
        List<Row> rows = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        TrackingData data = MarieClientCache.get();
        if (mc.player != null) {
            rows.add(new Row(Component.translatable("nourished.hud.calorieHistory.today").getString(), data.total, true));
        }
        List<TrackerHistoryEntry> history = data.trackingHistory.get(NourishedAPI.CALORIES_TRACKER_ID);
        if (history != null) {
            for (int i = 0; i < history.size(); i++) {
                TrackerHistoryEntry entry = history.get(i);
                Component label = i == 0
                        ? Component.translatable("nourished.hud.calorieHistory.yesterday")
                        : Component.translatable("nourished.hud.calorieHistory.daysAgo", i + 1);
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
                .orElseGet(() -> new Bounds(DEFAULT_X, DEFAULT_Y, natural.width(), natural.height()));
    }

    /** How many rows fit vertically in {@code bounds} at the current content scale — shared by {@link #drawPanel} (what to draw) and {@link #mouseScrolled} (how far scrolling can go). */
    private static int visibleRowCapacity(Bounds bounds) {
        double contentScale = ContentScaleController.resolveContentScale(persistedContentScale());
        int lineHeight = Math.max(1, (int) Math.round(LINE_HEIGHT * contentScale));
        double userPadding = PADDING * persistedPaddingScale();
        int padding = Math.round(ContentScaleController.resolvePadding(userPadding));
        return Math.max(1, (bounds.height() - padding * 2 - HEADER_HEIGHT - HEADER_GAP) / lineHeight);
    }

    /** Set by {@link Nourished#registerColorDefinitions()} at mod init. */
    public static ColorKeyPair COLORS;

    private static void drawPanel(RenderContext context, Bounds bounds, int contentOffsetX, int contentOffsetY, List<Row> rows, boolean editMode, boolean moveContentMode) {
        // Text/padding render scale is the user's persisted adjustment alone — box size (bounds)
        // plays no part in it, matching HudEditTarget's Nutrient HUD panel exactly: content never
        // shrinks to fit a smaller box, a resize only changes the box itself, and whatever doesn't
        // fit is handled by scrolling (see visibleRowCapacity/mouseScrolled), not shrinking.
        // MIN_SHRINK_SCALE still gates how small the box itself can be dragged (see the Constraint
        // built in the constructor); that's unrelated and untouched by this.
        double contentScale = ContentScaleController.resolveContentScale(persistedContentScale());
        double userPadding = PADDING * persistedPaddingScale();
        int padding = Math.round(ContentScaleController.resolvePadding(userPadding));
        int lineHeight = Math.max(1, (int) Math.round(LINE_HEIGHT * contentScale));
        // Capped at lineHeight, not just floored at a fixed minimum: at a low Text Scale, lineHeight
        // shrinks with no floor of its own, but the old fixed 8px icon floor didn't shrink with it —
        // once lineHeight dropped below 8, the icon overflowed into the rows above/below and every
        // row visually collided into unreadable mush. Icon still shrinks proportionally with
        // contentScale above that point, same as everything else in the row.
        int iconSize = Math.min(lineHeight, Math.max(1, Math.round(ICON_SIZE * (float) contentScale)));

        NourishedClientConfig cc = NourishedClientConfig.get();
        int panelRgb = MarieColors.resolveColor(COLORS.background());
        int panelColor = MarieColors.withOpacity(
                MarieColors.shade(panelRgb, cc.calorieHudBackgroundShade()), cc.calorieHudBackgroundOpacity());
        int borderColor = MarieColors.withOpacity(
                MarieColors.shade(context.theme().color(ThemeKey.BORDER), cc.calorieHudBorderShade()), cc.calorieHudBorderOpacity());
        context.drawRoundedRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), 1, HudDrawHelpers.PANEL_CORNER_RADIUS, panelColor, borderColor);
        context.pushClip(bounds.x(), bounds.y(), bounds.width(), bounds.height());
        try {
            context.drawText(Component.translatable("nourished.hud.calorieHistory.label").getString(),
                    bounds.x() + padding, bounds.y() + padding, TITLE_ACCENT_COLOR, TITLE_SCALE);

            int rowsX = bounds.x() + padding + contentOffsetX;
            int rowsTop = bounds.y() + padding + HEADER_HEIGHT + HEADER_GAP + contentOffsetY;

            int labelColor = MarieColors.resolveColor(COLORS.text());
            int barBg = HudDrawHelpers.barBackgroundColor();
            float maxTotal = MarieClientCache.get().maxTotal;

            // Aligns every row's bar to the same x regardless of that row's own label width ("Today"
            // vs. "5 days ago"), same reasoning as HudLayout#maxLabelSw for the dynamic nutrient HUD.
            int maxLabelW = 0;
            int maxLabelWNatural = 0;
            for (Row row : rows) {
                maxLabelW = Math.max(maxLabelW, context.textWidth(row.label(), (float) contentScale));
                maxLabelWNatural = Math.max(maxLabelWNatural, context.textWidth(row.label(), 1f));
            }
            int barX = rowsX + iconSize + HudDrawHelpers.ICON_LABEL_GAP + maxLabelW + HudDrawHelpers.LABEL_BAR_GAP;
            int pctReserve = Math.round(PCT_RESERVE * (float) contentScale);
            // barW is itself part of the content that scales with contentScale (Text Scale), same as
            // icon/label/value — it is NOT pinned to a fixed unscaled right edge, which previously
            // meant a high Text Scale grew every other element (icon, label, pct reserve) while the
            // bar's target right edge stayed fixed, shrinking it toward (and eventually to) zero
            // width. Instead, the reference "how much room is left for the bar" is computed once at
            // scale 1.0 (naturalBarW, unaffected by the live box size — a box dragged wider/narrower
            // still only changes surrounding margin, per the class-level contract above), then that
            // reference width itself scales by contentScale like everything else in the row.
            int naturalReserved = PADDING * 2 + ICON_SIZE + HudDrawHelpers.ICON_LABEL_GAP + maxLabelWNatural
                    + HudDrawHelpers.LABEL_BAR_GAP + PCT_RESERVE + HudDrawHelpers.BAR_PCT_GAP;
            int naturalBarW = Math.max(1, naturalSize(rows.size()).width() - naturalReserved);
            int barW = Math.max(1, Math.round(naturalBarW * (float) contentScale));
            int barH = Math.max(1, Math.round(HudDrawHelpers.BAR_H * (float) contentScale));

            // Whatever doesn't fit is handled by scrolling, not shrinking or a silent hard clip: only the rows in
            // [scrollOffset, scrollOffset + capacity) are drawn, and drawScrollIndicator marks that
            // there's more above/below when capacity < rows.size().
            int capacity = visibleRowCapacity(bounds);
            int maxScroll = Math.max(0, rows.size() - capacity);
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
            int lastVisible = Math.min(rows.size(), scrollOffset + capacity);
            int y = rowsTop;
            for (int i = scrollOffset; i < lastVisible; i++) {
                Row row = rows.get(i);
                int rowCenterY = y + lineHeight / 2;
                int textY = rowCenterY - (int) Math.ceil(9 * contentScale) / 2;

                context.drawItem(CALORIE_ICON, rowsX, rowCenterY - iconSize / 2, iconSize / 16f);
                context.drawText(row.label(), rowsX + iconSize + HudDrawHelpers.ICON_LABEL_GAP, textY, labelColor, (float) contentScale);

                float pct = maxTotal > 0 ? row.value() / maxTotal : 0f;
                boolean overGoal = pct > 1f;
                float cappedPct = Mth.clamp(pct, 0f, 1f);
                // Calories have no established graduated color rule (unlike nutrient bars) — the
                // codebase always draws calorie values in HudDrawHelpers.CALORIE_COLOR, so that's
                // reused as-is; over-goal is the only shift, to the same red used everywhere else in
                // this codebase for "over threshold". The bar itself stays capped at 100% width
                // regardless, per the "don't draw past the bar's bounds" requirement.
                int fillColor = overGoal ? COL_OVER_GOAL : HudDrawHelpers.CALORIE_COLOR;
                int barY = rowCenterY - barH / 2;
                context.drawBar(barX, barY, barW, barH, cappedPct, barBg, fillColor);

                String pctText = Math.round(pct * 100) + "%";
                int pctX = barX + barW + HudDrawHelpers.BAR_PCT_GAP;
                context.drawText(pctText, pctX, textY, fillColor, (float) contentScale);

                y += lineHeight;
            }

            if (editMode && moveContentMode) {
                // Dashed rather than a solid glow outline — a live drag affordance shown only while
                // the toggle is active, not a persistent separately-hit-tested box; wraps the actual
                // drawn content's own bounding box (icon through the pct-reserve column, top through
                // the last visible row), a few pixels further out so it doesn't overlap it.
                int contentRight = barX + barW + HudDrawHelpers.BAR_PCT_GAP + pctReserve;
                context.drawDashedBorder(rowsX - 3, rowsTop - 3, contentRight - rowsX + 6, y - rowsTop + 6, TITLE_ACCENT_COLOR);
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
        context.fillRect(trackX, thumbY, 2, thumbH, TITLE_ACCENT_COLOR);
    }

    /** Carries forward the existing persisted contentScale/paddingScale so a drag/resize commit never resets the user's text-scale or padding adjustment. */
    private void commit(Bounds bounds) {
        var base = UiStatePersistence.get().load(PANEL_ID);
        boolean widthManual = base.map(ComponentState::widthManual).orElse(false) || drag.lastCommitAffectedWidth();
        boolean heightManual = base.map(ComponentState::heightManual).orElse(false) || drag.lastCommitAffectedHeight();
        double contentScale = base.map(ComponentState::contentScale).orElse(ComponentState.DEFAULT_CONTENT_SCALE);
        double paddingScale = base.map(ComponentState::paddingScale).orElse(ComponentState.DEFAULT_PADDING_SCALE);
        UiStatePersistence.get().save(PANEL_ID, new ComponentState(
                bounds.x(), bounds.y(), bounds.width(), bounds.height(), false,
                widthManual, heightManual, 0, contentScale, paddingScale));
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
        if (moveContentEnabled() && bounds.contains((int) mouseX, (int) mouseY)) {
            draggingContent = true;
            contentGrabOffsetX = (int) mouseX - contentOffsetX;
            contentGrabOffsetY = (int) mouseY - contentOffsetY;
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
        if (draggingContent) {
            List<Row> rows = currentRows();
            Bounds bounds = resolvedBounds(rows.size());
            contentOffsetX = clampContentOffsetX((int) mouseX - contentGrabOffsetX, bounds);
            contentOffsetY = clampContentOffsetY((int) mouseY - contentGrabOffsetY, bounds);
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
        if (draggingContent) {
            draggingContent = false;
            persistContentOffset();
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

        boolean moveContentMode = moveContentEnabled();
        // Re-clamped defensively here too — see the same comment on the onRenderGuiPost call site.
        int offsetX = clampContentOffsetX(contentOffsetX, bounds);
        int offsetY = clampContentOffsetY(contentOffsetY, bounds);
        drawPanel(BrightnessRenderContext.wrap(context, NourishedClientConfig.get().calorieHudContentBrightness()),
                bounds, offsetX, offsetY, rows, true, moveContentMode);

        // While move-content mode is active, dragging is exclusively routed to the content offset
        // (see mouseClicked/mouseDragged) — the panel's own resize handles would be inert, so they
        // aren't drawn, to avoid implying they still work.
        if (!moveContentMode) {
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
