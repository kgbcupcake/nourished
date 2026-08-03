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
import dev.marie.framework.ui.edit.DraggableResizable;
import dev.marie.framework.ui.edit.EditModeController;
import dev.marie.framework.ui.geometry.Anchor;
import dev.marie.framework.ui.geometry.Bounds;
import dev.marie.framework.ui.geometry.Insets;
import dev.marie.framework.ui.geometry.Size;
import dev.marie.framework.ui.render.GuiGraphicsRenderContext;
import dev.maire.nourished.api.NourishedAPI;
import dev.maire.nourished.client.NourishedKeys;
import dev.maire.nourished.client.UiStatePersistence;
import dev.maire.nourished.client.hud.dynamic.HudDrawHelpers;
import dev.maire.nourished.config.NourishedClientConfig;
import dev.maire.nourished.core.Nourished;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
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

    private static final int PANEL_WIDTH = 160;
    private static final int LINE_HEIGHT = 10;
    private static final int PADDING = 4;
    private static final int DEFAULT_X = 8;
    private static final int DEFAULT_Y = 60;

    /** How much bigger than content's natural size the box may be dragged, on either axis. */
    private static final double MAX_MARGIN_MULTIPLIER = 5.0d;

    private static CalorieHudScreen instance;
    private static EditModeController editModeController;

    private final DraggableResizable drag;

    private CalorieHudScreen() {
        Size natural = naturalSize(currentRows().size());
        Constraint constraint = new Constraint(
                natural, natural,
                new Size((int) (natural.width() * MAX_MARGIN_MULTIPLIER), (int) (natural.height() * MAX_MARGIN_MULTIPLIER)),
                false, false, true, true,
                Anchor.TOP_LEFT, Insets.NONE, Insets.NONE
        );
        drag = new DraggableResizable(this, constraint, (target, bounds) -> commit(bounds));
    }

    private static CalorieHudScreen instance() {
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

    public static void onRenderGuiPost(RenderGuiEvent.Post event) {
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
     * <p>The "Today" row reads {@link TrackingData#trackingAccumulators} off {@link
     * MarieClientCache#get()}, not {@code NourishedAPI#getTotal} ({@code CaloriesComponent}'s
     * "current" value, a decaying calorie gauge — a different quantity than the tracker's per-day
     * accumulation shown in every other row of this panel) and not {@code
     * MarieAPI#getCurrentTrackerValue} against {@code mc.player} directly (the client-side player
     * entity's own attachment is only ever written on a full tracking sync — login/respawn/
     * dimension change — never on the lightweight delta sync that fires per food-eaten, so that
     * read showed a permanently stale, usually-zero value). {@code MarieClientCache} is the
     * snapshot Nourished's delta-sync pipeline actually keeps current, and matches the units of
     * the history rows below it.
     */
    private static List<Row> currentRows() {
        List<Row> rows = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        TrackingData data = MarieClientCache.get();
        if (mc.player != null) {
            float today = data.trackingAccumulators.getOrDefault(NourishedAPI.CALORIES_TRACKER_ID, 0f);
            rows.add(new Row(Component.translatable("nourished.hud.calorieHistory.today", (int) today).getString(), true));
        }
        List<TrackerHistoryEntry> history = data.trackingHistory.get(NourishedAPI.CALORIES_TRACKER_ID);
        if (history != null) {
            for (int i = 0; i < history.size(); i++) {
                TrackerHistoryEntry entry = history.get(i);
                Component label = i == 0
                        ? Component.translatable("nourished.hud.calorieHistory.yesterday", (int) entry.value())
                        : Component.translatable("nourished.hud.calorieHistory.daysAgo", i + 1, (int) entry.value());
                rows.add(new Row(label.getString(), false));
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
            editModeController().enter();
        }
    }

    private static Size naturalSize(int rowCount) {
        int rows = Math.max(1, rowCount);
        return new Size(PANEL_WIDTH, PADDING * 2 + rows * LINE_HEIGHT);
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
        int panelRgb = MarieColors.resolveColor(COLORS.background());
        int panelColor = HudDrawHelpers.panelColorWithOpacity(
                panelRgb, NourishedClientConfig.get().calorieHudBackgroundOpacity());
        context.fillRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), panelColor);
        context.drawBorder(bounds.x(), bounds.y(), bounds.width(), bounds.height(), 1,
                context.theme().color(ThemeKey.BORDER));
        context.pushClip(bounds.x(), bounds.y(), bounds.width(), bounds.height());
        try {
            int textColor = MarieColors.resolveColor(COLORS.text());
            int maxRows = Math.max(0, (bounds.height() - PADDING * 2) / LINE_HEIGHT);
            int visible = Math.min(rows.size(), maxRows);
            int y = bounds.y() + PADDING;
            for (int i = 0; i < visible; i++) {
                Row row = rows.get(i);
                context.drawText(row.text(), bounds.x() + PADDING, y, textColor, 1f);
                y += LINE_HEIGHT;
            }
        } finally {
            context.popClip();
        }
    }

    private void commit(Bounds bounds) {
        boolean widthManual = existingWidthManual() || drag.lastCommitAffectedWidth();
        boolean heightManual = existingHeightManual() || drag.lastCommitAffectedHeight();
        UiStatePersistence.get().save(PANEL_ID, new ComponentState(
                bounds.x(), bounds.y(), bounds.width(), bounds.height(), false,
                widthManual, heightManual, 0));
    }

    private static boolean existingWidthManual() {
        return UiStatePersistence.get().load(PANEL_ID).map(ComponentState::widthManual).orElse(false);
    }

    private static boolean existingHeightManual() {
        return UiStatePersistence.get().load(PANEL_ID).map(ComponentState::heightManual).orElse(false);
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
        Bounds bounds = resolvedBounds(currentRows().size());
        return drag.mouseClicked((int) mouseX, (int) mouseY, bounds);
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

    /** One rendered row: {@code live} is true only for the in-progress "Today" row. */
    private record Row(String text, boolean live) {}
}
