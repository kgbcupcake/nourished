package dev.maire.nourished.client.screen.diet.dynamic.persistence;

import dev.marie.framework.ui.component.ComponentState;
import dev.marie.framework.ui.geometry.Bounds;
import dev.marie.framework.ui.PersistenceProvider;
import dev.maire.nourished.client.UiStatePersistence;
import dev.maire.nourished.client.screen.diet.dynamic.layout.DietLayout;
import dev.maire.nourished.client.screen.diet.dynamic.modules.EatMoreComponent;
import dev.maire.nourished.client.screen.diet.dynamic.modules.RecentMealsComponent;
import dev.maire.nourished.config.NourishedClientConfig;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Map;

/** Facade over the shared {@link UiStatePersistence} store for the Diet Screen's panel-relative sub-boxes. */
public final class DietScreenPersistence {

    private DietScreenPersistence() {}

    /**
     * A sub-box's live drag/resize preview {@link Bounds}, keyed by component ID, for whichever box
     * edit mode is actively dragging this frame — set by {@link
     * dev.maire.nourished.client.screen.diet.dynamic.edit.DietScreenEditTarget} just before it
     * rebuilds the module chain each frame, and cleared again immediately after. Without this,
     * {@link #resolveRelativeToPanel} (and therefore {@link DietLayout#nextSiblingStartLocalY
     * DietLeftColumnComponent#nextSiblingStartLocalY}'s sibling-stacking math) only ever sees a
     * dragged box's last *committed* size, so every module stacked after it keeps the pre-drag
     * start position for the whole gesture — a later sub-box being grown live overlaps whatever
     * follows it, and a later sub-box's own fit check (e.g. {@code ActiveEffectsComponent}'s
     * header-fits-in-panel gate) evaluates against a start-Y that hasn't caught up with the box
     * actually being resized above it, so it can appear to vanish or misplace mid-drag despite the
     * panel visually having room.
     */
    private static final Map<String, Bounds> liveOverrides = new HashMap<>();

    public static PersistenceProvider get() {
        return UiStatePersistence.get();
    }

    /** Registers {@code bounds} as the live preview for {@code componentId} for the remainder of this frame's module build. */
    public static void setLiveOverride(String componentId, Bounds bounds) {
        liveOverrides.put(componentId, bounds);
    }

    /** Clears every live preview override — call once per frame after the module chain that needed them has been built. */
    public static void clearLiveOverrides() {
        liveOverrides.clear();
    }

    /**
     * Storage range for the five Diet Screen sub-boxes' persisted content zoom multipliers —
     * intentionally much wider than any box's effective on-screen range, since the real ceiling is
     * the live per-render clamp in {@code DietScreenModules#zoomedTextIconScale} against that
     * frame's own single-axis fit scale, not this bound.
     */
    private static final double CONTENT_SCALE_MIN = 0.1d;
    private static final double CONTENT_SCALE_MAX = 5.0d;

    /** A box's persisted content zoom multiplier — see {@link ComponentState#contentScale()}. Defaults to {@code 1.0} (no zoom) if never set. */
    public static double contentScale(String componentId) {
        return get().load(componentId).map(ComponentState::contentScale).orElse(ComponentState.DEFAULT_CONTENT_SCALE);
    }

    /**
     * Adjusts {@code componentId}'s persisted content zoom multiplier by {@code delta}, clamped to
     * {@code [CONTENT_SCALE_MIN, CONTENT_SCALE_MAX]} — the storage ceiling, not the effective
     * on-screen range (see {@link #CONTENT_SCALE_MIN}). Leaves the box's own position/size fields
     * untouched: if nothing is persisted yet for this box, one is derived from {@code currentBounds}
     * (today's natural stacked position) rather than defaulted to zero, since {@link
     * #resolveRelativeToPanel} reads a saved state's x/y unconditionally regardless of the manual
     * size flags — a zeroed x/y would silently relocate the box to the panel's top-left corner.
     */
    public static void adjustContentScale(String componentId, DietLayout.Layout panelLayout, Bounds currentBounds, double delta) {
        ComponentState base = existingOrDerived(componentId, panelLayout, currentBounds);
        double newScale = Mth.clamp(base.contentScale() + delta, CONTENT_SCALE_MIN, CONTENT_SCALE_MAX);
        get().save(componentId, withContentScale(base, newScale));
    }

    private static ComponentState existingOrDerived(String componentId, DietLayout.Layout panelLayout, Bounds currentBounds) {
        return get().load(componentId).orElseGet(() -> relativeState(panelLayout, currentBounds));
    }

    /** Converts an absolute-pixel {@code bounds} into a not-manually-sized {@link ComponentState}, normalized the same way {@code DietScreenEditTarget#toRelativeState} normalizes a committed drag/resize. */
    private static ComponentState relativeState(DietLayout.Layout panelLayout, Bounds bounds) {
        double scale = panelLayout.scale();
        int contentX = panelLayout.panelX() + panelLayout.leftMargin();
        return new ComponentState(
                (int) Math.round((bounds.x() - contentX) / scale),
                (int) Math.round((bounds.y() - panelLayout.panelY()) / scale),
                (int) Math.round(bounds.width() / scale),
                (int) Math.round(bounds.height() / scale),
                false, false, false, 0
        );
    }

    private static ComponentState withContentScale(ComponentState base, double contentScale) {
        return new ComponentState(base.x(), base.y(), base.width(), base.height(), base.collapsed(),
                base.widthManual(), base.heightManual(), base.leftMargin(), contentScale);
    }

    /** Resolves a sub-box's screen bounds relative to the panel: a live drag/resize preview if one is active this frame, else persisted local-unit offset/size if manually moved/resized, otherwise the natural stacked position. */
    public static Bounds resolveRelativeToPanel(String componentId, DietLayout.Layout panelLayout, int startLocalY, int localWidth, int localHeight) {
        ensureOffsetMigration();
        Bounds liveOverride = liveOverrides.get(componentId);
        if (liveOverride != null) {
            return clampToPanel(liveOverride, panelLayout);
        }
        int naturalWidth = DietLayout.toScreenDim(panelLayout, localWidth);
        int naturalHeight = DietLayout.toScreenDim(panelLayout, localHeight);
        int contentX = panelLayout.panelX() + panelLayout.leftMargin();
        Bounds resolved = get().load(componentId)
                .map(state -> new Bounds(
                        contentX + (int) Math.round(state.x() * panelLayout.scale()),
                        panelLayout.panelY() + (int) Math.round(state.y() * panelLayout.scale()),
                        state.widthManual() ? (int) Math.round(state.width() * panelLayout.scale()) : naturalWidth,
                        state.heightManual() ? (int) Math.round(state.height() * panelLayout.scale()) : naturalHeight))
                .orElseGet(() -> new Bounds(
                        contentX,
                        panelLayout.panelY() + (int) Math.round(startLocalY * panelLayout.scale()),
                        naturalWidth,
                        naturalHeight
                ));
        return clampToPanel(resolved, panelLayout);
    }

    /** Confines a sub-box to the panel rectangle and the column divider; left bound is the panel's true edge, not past the margin, since that space is draggable. */
    private static Bounds clampToPanel(Bounds bounds, DietLayout.Layout panelLayout) {
        int w = Math.min(bounds.width(), panelLayout.panelW());
        int h = Math.min(bounds.height(), panelLayout.panelH());
        int x = Math.max(panelLayout.panelX(), Math.min(bounds.x(), panelLayout.panelX() + panelLayout.panelW() - w));
        int y = Math.max(panelLayout.panelY(), Math.min(bounds.y(), panelLayout.panelY() + panelLayout.panelH() - h));

        Bounds panelBounds = new Bounds(panelLayout.panelX(), panelLayout.panelY(), panelLayout.panelW(), panelLayout.panelH());
        int dividerX = DietLayout.columnGeometry(panelLayout, panelBounds).dividerX();
        w = Math.min(w, Math.max(1, dividerX - panelLayout.panelX()));
        x = Math.max(panelLayout.panelX(), Math.min(x, dividerX - w));

        return new Bounds(x, y, w, h);
    }

    /** One-time resets for past persisted-offset schema changes (absolute -> local-unit -> scale-normalized). Each flag guards a distinct meaning change so players on an intermediate scheme aren't misinterpreted. */
    private static void ensureOffsetMigration() {
        NourishedClientConfig cc = NourishedClientConfig.get();
        boolean didWork = false;
        if (!cc.recentMealsEatMoreOffsetMigrationDone()) {
            get().remove(RecentMealsComponent.ID);
            get().remove(EatMoreComponent.ID);
            cc.setRecentMealsEatMoreOffsetMigrationDone(true);
            didWork = true;
        }
        if (!cc.recentMealsEatMoreLocalOffsetMigrationDone()) {
            get().remove(RecentMealsComponent.ID);
            get().remove(EatMoreComponent.ID);
            cc.setRecentMealsEatMoreLocalOffsetMigrationDone(true);
            didWork = true;
        }
        if (!cc.recentMealsEatMoreLocalSizeMigrationDone()) {
            get().remove(RecentMealsComponent.ID);
            get().remove(EatMoreComponent.ID);
            cc.setRecentMealsEatMoreLocalSizeMigrationDone(true);
            didWork = true;
        }
        if (!cc.recentMealsRowHeightMigrationDone()) {
            get().remove(RecentMealsComponent.ID);
            cc.setRecentMealsRowHeightMigrationDone(true);
            didWork = true;
        }
        if (didWork) {
            NourishedClientConfig.saveNow();
        }
    }
}
