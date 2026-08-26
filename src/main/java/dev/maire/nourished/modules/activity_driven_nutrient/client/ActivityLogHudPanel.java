package dev.maire.nourished.modules.activity_driven_nutrient.client;

import dev.marie.framework.color.ColorKeyPair;
import dev.marie.framework.color.MarieColors;
import dev.marie.framework.tracking.tracker.MarieTracking;
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
    private static final int TITLE_ACCENT_COLOR = 0xFF7ED9A6;

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
    private static final double MIN_SHRINK_SCALE = 0.5d;

    private static ActivityLogHudPanel instance;
    private static EditModeController editModeController;
    private static ContentScaleController scaleController;

    private final DraggableResizable drag;

    /**
     * Slider-based alternative to double-click+scroll for this panel's persisted
     * contentScale/paddingScale, auto-shown alongside edit mode — same pattern as {@code
     * DietScreen#scaleConfigPanel}, just with a single entry since this panel has no sub-boxes.
     */
    private final ScaleConfigPanel scaleConfigPanel = MarieScaleConfig.create(
            List.of(new ScaleConfigEntry(PANEL_ID, Component.translatable("nourished.hud.activityLog.label"))),
            UiStatePersistence.get(), Anchor.TOP_RIGHT);
    private boolean scaleConfigVisible;

    private ActivityLogHudPanel() {
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
    public static ActivityLogHudPanel instance() {
        if (instance == null) {
            instance = new ActivityLogHudPanel();
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
        RenderContext context = new GuiGraphicsRenderContext(
                event.getGuiGraphics(), mc, Theme.DARK, event.getPartialTick().getGameTimeDeltaPartialTick(false));
        drawPanel(context, bounds, rows);
    }

    /**
     * One row per {@link #TRACKERS} entry, in fixed display order, reading each tracker's
     * current-period (today's) accumulated value via {@link MarieTracking#getCurrentTrackerValue}
     * against the local client player — the same client-side-safe accessor {@code
     * CalorieHudScreen} would use, works whether given a {@code ServerPlayer} or a client-side
     * player instance. Counts (mining/combat/starvation) render as whole numbers; distances
     * (sprint/swim) render with one decimal place and a "blocks" suffix.
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
                    ? String.format("%.1f blocks", value)
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

    /** Set by {@code Nourished#registerColorDefinitions()} at mod init. */
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
                MarieColors.shade(panelRgb, cc.activityLogHudBackgroundShade()), cc.activityLogHudBackgroundOpacity());
        int borderColor = MarieColors.withOpacity(
                MarieColors.shade(context.theme().color(ThemeKey.BORDER), cc.activityLogHudBorderShade()), cc.activityLogHudBorderOpacity());
        context.drawRoundedRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), 1, panelColor, borderColor);
        context.pushClip(bounds.x(), bounds.y(), bounds.width(), bounds.height());
        try {
            context.drawText(Component.translatable("nourished.hud.activityLog.label").getString(),
                    bounds.x() + padding, bounds.y() + padding, TITLE_ACCENT_COLOR, TITLE_SCALE);

            int defaultTextColor = MarieColors.resolveColor(COLORS.text());
            int barBg = HudDrawHelpers.barBackgroundColor();

            // Aligns every row's bar to the same x regardless of that row's own label width
            // ("Combat" vs. "Starvation"), same reasoning as HudLayout#maxLabelSw for the dynamic
            // nutrient HUD.
            int maxLabelW = 0;
            for (Row row : rows) {
                maxLabelW = Math.max(maxLabelW, context.textWidth(row.label(), (float) contentScale));
            }
            int barX = bounds.x() + padding + iconSize + HudDrawHelpers.ICON_LABEL_GAP + maxLabelW + HudDrawHelpers.LABEL_BAR_GAP;
            int valueReserve = Math.round(VALUE_RESERVE * (float) contentScale);
            int barW = Math.max(0, bounds.x() + bounds.width() - padding - valueReserve - HudDrawHelpers.BAR_PCT_GAP - barX);
            int barH = Math.max(1, Math.round(HudDrawHelpers.BAR_H * (float) contentScale));

            int rowsTop = bounds.y() + padding + HEADER_HEIGHT + HEADER_GAP;
            int maxRows = Math.max(0, (bounds.height() - padding * 2 - HEADER_HEIGHT - HEADER_GAP) / lineHeight);
            int visible = Math.min(rows.size(), maxRows);
            int y = rowsTop;
            for (int i = 0; i < visible; i++) {
                Row row = rows.get(i);
                TrackerRow tracker = TRACKERS.get(i);
                int color = ActivityDrivenNutrientRegistry.getColor(row.moduleId()).orElse(defaultTextColor);
                int rowCenterY = y + lineHeight / 2;
                int textY = rowCenterY - (int) Math.ceil(9 * contentScale) / 2;

                context.drawItem(tracker.icon(), bounds.x() + padding, rowCenterY - iconSize / 2, iconSize / 16f);
                context.drawText(row.label(), bounds.x() + padding + iconSize + HudDrawHelpers.ICON_LABEL_GAP, textY, color, (float) contentScale);

                // Self-relative per tracker, never compared against the other four: mining/combat
                // counts and sprint/swim distances live on wildly different natural scales, so a bar
                // shared across all five would always read the small-number trackers as near-empty.
                // denom = max(historicalMax, today's value) means a tracker with no history yet (day
                // one) still gets a full/near-full bar for today's own value instead of a 0/0 divide
                // or a permanently-empty bar.
                float denom = Math.max(row.historicalMax(), row.value());
                float fillPct = denom > 0f ? row.value() / denom : 0f;
                int barY = rowCenterY - barH / 2;
                context.drawBar(barX, barY, barW, barH, fillPct, barBg, color);

                int valueX = barX + barW + HudDrawHelpers.BAR_PCT_GAP;
                context.drawText(row.formattedValue(), valueX, textY, color, (float) contentScale);

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
}
