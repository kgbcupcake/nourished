package dev.maire.nourished.client.screen;

import dev.marie.framework.ui.Bounds;
import dev.marie.framework.ui.PersistenceProvider;
import dev.marie.framework.ui.persistence.MarieConfigPersistenceProvider;
import dev.maire.nourished.config.NourishedClientConfig;

/**
 * Single shared {@link PersistenceProvider} for the Diet Screen's independently-positionable
 * MarieUI sub-boxes (currently {@link RecentMealsComponent}, {@link EatMoreComponent}; more will
 * follow once drag/resize lands). {@link MarieConfigPersistenceProvider} caches its file contents
 * after first load, but constructing one shared instance — rather than one per component per
 * frame — avoids redundant {@code config/nourished-ui-state.json} reads and keeps a future save
 * from one box immediately visible to every other reader.
 */
final class DietScreenPersistence {

    private static final PersistenceProvider INSTANCE = new MarieConfigPersistenceProvider();

    private DietScreenPersistence() {}

    static PersistenceProvider get() {
        return INSTANCE;
    }

    /**
     * Resolves the main panel's own screen {@link Bounds}: absolute screen pixels, since the panel
     * is the anchor everything else is positioned relative to and has no parent of its own. Only
     * ever called with {@link DietScreenEditTarget#PANEL_ID}.
     */
    static Bounds resolve(String componentId, DietLayout.Layout layout, int startLocalY, int localWidth, int localHeight) {
        return get().load(componentId)
                .map(state -> new Bounds(state.x(), state.y(), state.width(), state.height()))
                .orElseGet(() -> new Bounds(
                        layout.panelX(),
                        layout.panelY() + (int) Math.round(startLocalY * layout.scale()),
                        DietLayout.toScreenDim(layout, localWidth),
                        DietLayout.toScreenDim(layout, localHeight)
                ));
    }

    /**
     * Resolves a panel-relative sub-box's screen {@link Bounds}. Position is always derived from
     * {@code panelLayout}'s <em>current</em> resolved position plus a persisted offset — so
     * dragging/resizing the main panel always carries these boxes with it, instead of the box
     * being pinned to whatever absolute screen pixel it was saved at (the bug this replaced: see
     * the stale-absolute-persistence investigation). Only x/y are relative; width/height are
     * stored as absolute pixels, same as before — a box's own size doesn't need to track the
     * panel's position, only its position does.
     *
     * <p>{@link #ensureOffsetMigration()} runs first so any pre-existing entry saved under the old
     * absolute-coordinate scheme is discarded rather than silently reinterpreted as an offset.
     */
    static Bounds resolveRelativeToPanel(String componentId, DietLayout.Layout panelLayout, int startLocalY, int localWidth, int localHeight) {
        ensureOffsetMigration();
        return get().load(componentId)
                .map(state -> new Bounds(
                        panelLayout.panelX() + state.x(),
                        panelLayout.panelY() + state.y(),
                        state.width(),
                        state.height()))
                .orElseGet(() -> new Bounds(
                        panelLayout.panelX(),
                        panelLayout.panelY() + (int) Math.round(startLocalY * panelLayout.scale()),
                        DietLayout.toScreenDim(panelLayout, localWidth),
                        DietLayout.toScreenDim(panelLayout, localHeight)
                ));
    }

    /**
     * One-time reset for the absolute-to-relative coordinate migration. There is no version marker
     * anywhere in {@link dev.marie.framework.ui.ComponentState}/the persisted JSON file to
     * distinguish an old-format (absolute) entry from a new-format (offset) one — the same int
     * fields mean something different before/after this change — so an in-place numeric conversion
     * can't safely tell "not yet migrated" from "already migrated" on a later load, and would risk
     * re-applying the transform to already-correct data. Instead, gated by a one-time flag in
     * {@link NourishedClientConfig} (this mod's own config, not the shared Marieslib persistence
     * file), any pre-existing recentmeals/eatmore entry is discarded outright — falling through to
     * the default stacked position — rather than reinterpreted; a previously custom-dragged
     * position simply resets once and must be re-dragged after this update.
     */
    private static void ensureOffsetMigration() {
        NourishedClientConfig cc = NourishedClientConfig.get();
        if (cc.recentMealsEatMoreOffsetMigrationDone()) {
            return;
        }
        get().remove(RecentMealsComponent.ID);
        get().remove(EatMoreComponent.ID);
        cc.setRecentMealsEatMoreOffsetMigrationDone(true);
        NourishedClientConfig.saveNow();
    }
}
