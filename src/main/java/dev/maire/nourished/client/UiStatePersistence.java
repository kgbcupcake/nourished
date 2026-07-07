package dev.maire.nourished.client;

import dev.marie.framework.ui.PersistenceProvider;
import dev.marie.framework.ui.persistence.MarieConfigPersistenceProvider;

/**
 * Single shared {@link PersistenceProvider} for every independently-positionable/resizable
 * MarieUI region in this mod (Diet Screen panel + sub-boxes, HUD panel). {@link
 * MarieConfigPersistenceProvider} caches its file contents in memory after first load and
 * overwrites the whole file on every save — two separate instances reading/writing the same
 * underlying {@code config/nourished-ui-state.json} would risk one instance's stale in-memory
 * cache clobbering entries the other just wrote. One shared instance across the {@code screen} and
 * {@code hud} packages avoids that split-brain risk.
 */
public final class UiStatePersistence {

    private static final PersistenceProvider INSTANCE = new MarieConfigPersistenceProvider();

    private UiStatePersistence() {}

    public static PersistenceProvider get() {
        return INSTANCE;
    }
}
