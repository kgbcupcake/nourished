package dev.maire.nourished.client.screen;

import dev.marie.framework.ui.Bounds;
import dev.marie.framework.ui.PersistenceProvider;
import dev.marie.framework.ui.persistence.MarieConfigPersistenceProvider;

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
     * Resolves a sub-box's screen {@link Bounds}: the persisted position/size if the user has
     * already committed a drag/resize for {@code componentId}, otherwise today's default stacked
     * position — the same math this box used before it stopped being Layout-managed — so
     * first-launch rendering (no saved state yet) is unchanged.
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
}
