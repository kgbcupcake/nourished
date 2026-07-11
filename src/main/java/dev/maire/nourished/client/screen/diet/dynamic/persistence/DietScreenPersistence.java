package dev.maire.nourished.client.screen.diet.dynamic.persistence;

import dev.marie.framework.ui.geometry.Bounds;
import dev.marie.framework.ui.PersistenceProvider;
import dev.maire.nourished.client.UiStatePersistence;
import dev.maire.nourished.client.screen.diet.dynamic.layout.DietLayout;
import dev.maire.nourished.client.screen.diet.dynamic.modules.EatMoreComponent;
import dev.maire.nourished.client.screen.diet.dynamic.modules.RecentMealsComponent;
import dev.maire.nourished.config.NourishedClientConfig;

/** Facade over the shared {@link UiStatePersistence} store for the Diet Screen's panel-relative sub-boxes. */
public final class DietScreenPersistence {

    private DietScreenPersistence() {}

    public static PersistenceProvider get() {
        return UiStatePersistence.get();
    }

    /** Resolves a sub-box's screen bounds relative to the panel: persisted local-unit offset/size if manually moved/resized, otherwise the natural stacked position. */
    public static Bounds resolveRelativeToPanel(String componentId, DietLayout.Layout panelLayout, int startLocalY, int localWidth, int localHeight) {
        ensureOffsetMigration();
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
