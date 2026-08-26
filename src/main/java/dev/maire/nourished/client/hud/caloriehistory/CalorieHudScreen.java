package dev.maire.nourished.client.hud.caloriehistory;

import dev.marie.framework.client.config.state.MarieClientCache;
import dev.marie.framework.color.ColorKeyPair;
import dev.marie.framework.color.MarieColors;
import dev.marie.framework.config.FeatureFlagCache;
import dev.marie.framework.tracking.TrackingData;
import dev.marie.framework.tracking.tracker.definition.TrackerHistoryEntry;
import dev.marie.framework.ui.RenderContext;
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
    private static ContentScaleController scaleController;

    private final DraggableResizable drag;

    /**
     * Slider-based alternative to double-click+scroll for this panel's persisted
     * contentScale/paddingScale, auto-shown alongside edit mode — same pattern as {@code
     * DietScreen#scaleConfigPanel}, just with a single entry since this panel has no sub-boxes.
     */
    private final ScaleConfigPanel scaleConfigPanel = MarieScaleConfig.create(
            List.of(new ScaleConfigEntry(PANEL_ID, Component.translatable("nourished.hud.calorieHistory.label"))),
            UiStatePersistence.get(), Anchor.TOP_RIGHT);
    private boolean scaleConfigVisible;

    private CalorieHudScreen() {
        Size natural = naturalSize(currentRows().size());
        Size minSize = new Size(
                (int) (natural.width() * MIN_SHRINK_SCALE), (int) (natural.height() * MIN_SHRINK_SCALE));
        Constraint constraint = new Constraint(
                natural, minSize,
                new Size((int) (natural.width() * MAX_MARGIN_MULTIPLIER), (int) (natural.height() * MAX_MARGIN_MULTIPLIER)),
                false, false, true, true,
                Anchor.TOP_LEFT, Insets.NONE, Insets.NONE
        );
        drag = new DraggableResizable(this, constraint, (target, bounds) -> commit(bounds));
        drag.setSnapRegistryId(PANEL_ID);
        SnapRegistry.register(PANEL_ID, () -> resolvedBounds(currentRows().size()));
    }

    /** Public so {@code ClientEventRegistrar} can pass this same singleton to {@code EditModeCoordinator.registerGroupCapable} as its {@code MarieComponent} target — the exact instance this panel's own {@link #editModeController()} already wraps. */
    public static CalorieHudScreen instance() {
        if (instance == null) {
            instance = new CalorieHudScreen();
        }
        return instance;
    }

    /** This panel's own {@link ContentScaleController} — independent of every other component's, so zooming/padding-adjusting this panel never affects another's active mode. */
    private static ContentScaleController scaleController() {
        if (scaleController == null) {
            scaleController = new ContentScaleController(UiStatePersistence.get());
        }
        return scaleController;
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
        RenderContext context = new GuiGraphicsRenderContext(
                event.getGuiGraphics(), mc, Theme.DARK, event.getPartialTick().getGameTimeDeltaPartialTick(false));
        drawPanel(context, bounds, rows);
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

    /** Set by {@link Nourished#registerColorDefinitions()} at mod init. */
    public static ColorKeyPair COLORS;

    private static void drawPanel(RenderContext context, Bounds bounds, List<Row> rows) {
        // Text/padding render scale is the user's persisted adjustment alone — box size (bounds) plays
        // no part in it. MIN_SHRINK_SCALE still gates how small the box itself can be dragged (see the
        // Constraint built in the constructor); that's unrelated and untouched by this.
        double contentScale = ContentScaleController.resolveContentScale(persistedContentScale());
        double userPadding = PADDING * persistedPaddingScale();
        int padding = Math.round(ContentScaleController.resolvePadding(userPadding));
        int lineHeight = Math.max(1, (int) Math.round(LINE_HEIGHT * contentScale));
        int iconSize = Math.max(8, Math.round(ICON_SIZE * (float) contentScale));

        NourishedClientConfig cc = NourishedClientConfig.get();
        int panelRgb = MarieColors.resolveColor(COLORS.background());
        int panelColor = MarieColors.withOpacity(
                MarieColors.shade(panelRgb, cc.calorieHudBackgroundShade()), cc.calorieHudBackgroundOpacity());
        int borderColor = MarieColors.withOpacity(
                MarieColors.shade(context.theme().color(ThemeKey.BORDER), cc.calorieHudBorderShade()), cc.calorieHudBorderOpacity());
        context.drawRoundedRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), 1, panelColor, borderColor);
        context.pushClip(bounds.x(), bounds.y(), bounds.width(), bounds.height());
        try {
            context.drawText(Component.translatable("nourished.hud.calorieHistory.label").getString(),
                    bounds.x() + padding, bounds.y() + padding, TITLE_ACCENT_COLOR, TITLE_SCALE);

            int labelColor = MarieColors.resolveColor(COLORS.text());
            int barBg = HudDrawHelpers.barBackgroundColor();
            float maxTotal = MarieClientCache.get().maxTotal;

            // Aligns every row's bar to the same x regardless of that row's own label width ("Today"
            // vs. "5 days ago"), same reasoning as HudLayout#maxLabelSw for the dynamic nutrient HUD.
            int maxLabelW = 0;
            for (Row row : rows) {
                maxLabelW = Math.max(maxLabelW, context.textWidth(row.label(), (float) contentScale));
            }
            int barX = bounds.x() + padding + iconSize + HudDrawHelpers.ICON_LABEL_GAP + maxLabelW + HudDrawHelpers.LABEL_BAR_GAP;
            int pctReserve = Math.round(PCT_RESERVE * (float) contentScale);
            int barW = Math.max(0, bounds.x() + bounds.width() - padding - pctReserve - HudDrawHelpers.BAR_PCT_GAP - barX);
            int barH = Math.max(1, Math.round(HudDrawHelpers.BAR_H * (float) contentScale));

            int rowsTop = bounds.y() + padding + HEADER_HEIGHT + HEADER_GAP;
            int maxRows = Math.max(0, (bounds.height() - padding * 2 - HEADER_HEIGHT - HEADER_GAP) / lineHeight);
            int visible = Math.min(rows.size(), maxRows);
            int y = rowsTop;
            for (int i = 0; i < visible; i++) {
                Row row = rows.get(i);
                int rowCenterY = y + lineHeight / 2;
                int textY = rowCenterY - (int) Math.ceil(9 * contentScale) / 2;

                context.drawItem(CALORIE_ICON, bounds.x() + padding, rowCenterY - iconSize / 2, iconSize / 16f);
                context.drawText(row.label(), bounds.x() + padding + iconSize + HudDrawHelpers.ICON_LABEL_GAP, textY, labelColor, (float) contentScale);

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
        } finally {
            context.popClip();
        }
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

    /** This panel's persisted text-scale multiplier — see {@link ContentScaleController#contentScaleAdjustment}. */
    private static double persistedContentScale() {
        return scaleController().contentScaleAdjustment(PANEL_ID);
    }

    /** This panel's persisted padding multiplier — see {@link ContentScaleController#paddingScaleAdjustment}. */
    private static double persistedPaddingScale() {
        return scaleController().paddingScaleAdjustment(PANEL_ID);
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
        if (scaleController().onClick(PANEL_ID, button, mouseX, mouseY, bounds)) {
            return true;
        }
        return drag.mouseClicked((int) mouseX, (int) mouseY, bounds);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scaleConfigVisible && scaleConfigPanel.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        return scaleController().handleScroll(PANEL_ID, scrollY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (drag.isDragging() || drag.isResizing()) {
            drag.mouseDragged((int) mouseX, (int) mouseY);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean any = drag.isDragging() || drag.isResizing();
        drag.mouseReleased((int) mouseX, (int) mouseY);
        return any;
    }

    @Override
    public void render(RenderContext context, Bounds ignoredBounds) {
        List<Row> rows = currentRows();
        int[] mouse = scaledMouse(Minecraft.getInstance());
        Bounds defaultBounds = resolvedBounds(rows.size());
        Bounds bounds = liveOrDefault(mouse[0], mouse[1], defaultBounds);

        drawPanel(context, bounds, rows);

        Bounds handle = DraggableResizable.handleBounds(bounds);
        context.drawResizeHandle(handle.x(), handle.y(), drag.isHandleHovered(mouse[0], mouse[1], bounds), drag.isHandleActive());
        Bounds handleBL = DraggableResizable.handleBoundsBottomLeft(bounds);
        context.drawResizeHandle(handleBL.x(), handleBL.y(), drag.isHandleBottomLeftHovered(mouse[0], mouse[1], bounds), drag.isBottomLeftCornerActive());
        for (DraggableResizable.Edge edge : DraggableResizable.Edge.values()) {
            Bounds strip = DraggableResizable.edgeHandleBounds(bounds, edge);
            context.drawEdgeHandle(strip.x(), strip.y(), strip.width(), strip.height(), mouse[0], mouse[1],
                    drag.isEdgeHovered(mouse[0], mouse[1], bounds, edge), drag.isEdgeActive(edge));
        }
        drawScaleOverlay(context, bounds);

        if (scaleConfigVisible) {
            scaleConfigPanel.render(context, new Bounds(0, 0, context.screenWidth(), context.screenHeight()));
        }
    }

    /** While the panel's {@link ContentScaleController} has an active mode, shows the current text-scale or padding percentage below the box so scroll-wheel adjustments are visible — same look as {@code DietScreenEditTarget#drawZoomLabel}. */
    private void drawScaleOverlay(RenderContext context, Bounds bounds) {
        ContentScaleController.Mode mode = scaleController().activeMode(PANEL_ID);
        String label = switch (mode) {
            case TEXT_SCALE -> "TEXT SCALE " + Math.round(persistedContentScale() * 100) + "%";
            case PADDING -> "PADDING " + Math.round(persistedPaddingScale() * 100) + "%";
            case NONE -> null;
        };
        if (label == null) {
            return;
        }
        int lx = bounds.x() + 4;
        int ly = bounds.y() + bounds.height() + 6;
        context.drawText(label, lx + 1, ly + 1, 0xFF000000, 0.75f);
        context.drawText(label, lx, ly, 0xFFFFFFFF, 0.75f);
    }

    private Bounds liveOrDefault(int mx, int my, Bounds fallback) {
        if (drag.isDragging() || drag.isResizing()) {
            Bounds preview = drag.mouseDragged(mx, my);
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
