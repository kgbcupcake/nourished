package dev.maire.nourished.client.screen;

import dev.marie.framework.ui.Bounds;
import dev.marie.framework.ui.PersistenceProvider;
import dev.maire.nourished.client.UiStatePersistence;
import dev.maire.nourished.config.NourishedClientConfig;

/**
 * Diet-Screen-specific facade over the mod-wide {@link UiStatePersistence} shared instance (also
 * used by the HUD panel) — kept as its own class for the Diet-specific {@link #resolve}/{@link
 * #resolveRelativeToPanel}/{@link #ensureOffsetMigration} helpers, not because it owns a separate
 * persistence store.
 */
final class DietScreenPersistence {

    private DietScreenPersistence() {}

    static PersistenceProvider get() {
        return UiStatePersistence.get();
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
     * Resolves a panel-relative sub-box's screen {@link Bounds}. Position and size are both always
     * derived from {@code panelLayout}'s <em>current</em> scale/position plus a persisted local-unit
     * offset/size — so dragging/resizing the main panel always carries these boxes with it and
     * rescales them with it, instead of a committed drag/resize pinning the box to whatever absolute
     * screen pixels it was saved at (the bug this replaced: see the stale-absolute-persistence
     * investigation).
     *
     * <p>The persisted x/y/width/height are all stored in the same local (pre-scale) unit space as
     * {@code startLocalY}/{@code localWidth}/{@code localHeight} — i.e. each is divided by
     * {@code panelLayout.scale()} at save time ({@link DietScreenEditTarget#toRelativeState}) and
     * multiplied back by the <em>current</em> {@code panelLayout.scale()} here — exactly like the
     * default-position branch below already does. This makes a box behave as a genuine child of the
     * panel (per the user's explicit ask: it should scale with the panel like a normal child widget,
     * not just track the panel's position) while still allowing the user to independently nudge a
     * box's own relative size/position on top of that — the local units captured at commit simply
     * differ from the natural default (e.g. {@code bw}) by whatever the user dragged/resized it to,
     * and that difference itself then also scales with the panel afterward.
     *
     * <p>{@link #ensureOffsetMigration()} runs first so any pre-existing entry saved under any older
     * scheme (absolute-screen-coordinate; the first offset scheme that didn't normalize by scale; or
     * the scheme that normalized position but not size) is discarded rather than silently
     * reinterpreted under the new meaning.
     */
    static Bounds resolveRelativeToPanel(String componentId, DietLayout.Layout panelLayout, int startLocalY, int localWidth, int localHeight) {
        ensureOffsetMigration();
        Bounds resolved = get().load(componentId)
                .map(state -> new Bounds(
                        panelLayout.panelX() + (int) Math.round(state.x() * panelLayout.scale()),
                        panelLayout.panelY() + (int) Math.round(state.y() * panelLayout.scale()),
                        (int) Math.round(state.width() * panelLayout.scale()),
                        (int) Math.round(state.height() * panelLayout.scale())))
                .orElseGet(() -> new Bounds(
                        panelLayout.panelX(),
                        panelLayout.panelY() + (int) Math.round(startLocalY * panelLayout.scale()),
                        DietLayout.toScreenDim(panelLayout, localWidth),
                        DietLayout.toScreenDim(panelLayout, localHeight)
                ));
        return clampToPanel(resolved, panelLayout);
    }

    /**
     * Confines a sub-box's resolved {@link Bounds} to the main panel's current rectangle: shrinks
     * width/height down to the panel's own dimensions first (a saved size larger than the panel
     * can't be "contained" by moving alone), then slides x/y so the whole box sits within
     * {@code [panelX, panelX+panelW] x [panelY, panelY+panelH]}. Nothing upstream of this (drag,
     * resize, or a raw saved offset) previously enforced any relationship to the panel's bounds, so
     * a sub-box could end up partially or fully outside it.
     */
    private static Bounds clampToPanel(Bounds bounds, DietLayout.Layout panelLayout) {
        int w = Math.min(bounds.width(), panelLayout.panelW());
        int h = Math.min(bounds.height(), panelLayout.panelH());
        int x = Math.max(panelLayout.panelX(), Math.min(bounds.x(), panelLayout.panelX() + panelLayout.panelW() - w));
        int y = Math.max(panelLayout.panelY(), Math.min(bounds.y(), panelLayout.panelY() + panelLayout.panelH() - h));
        return new Bounds(x, y, w, h);
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
     *
     * <p>Runs a second, independently-gated reset for the later scale-normalization change to that
     * same offset (raw absolute-pixel delta → local-unit delta divided by
     * {@code panelLayout.scale()}): players who already tripped the flag above under the
     * intermediate absolute-delta scheme would otherwise have their old-meaning offset silently
     * reinterpreted as a local-unit one.
     *
     * <p>Runs a third, independently-gated reset for extending that same scale-normalization from
     * position-only to size as well (width/height were still raw absolute pixels until now):
     * players who already tripped the second flag would otherwise have their old absolute-pixel
     * size silently reinterpreted as a local-unit one and blown up/shrunk by the current scale.
     */
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
        if (didWork) {
            NourishedClientConfig.saveNow();
        }
    }
}
