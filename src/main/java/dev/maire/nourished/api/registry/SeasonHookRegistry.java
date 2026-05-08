package dev.maire.nourished.api.registry;

import dev.maire.nourished.api.NourishedSeasonHook;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Internal storage for season hooks registered via the public API.
 */
public final class SeasonHookRegistry {

    private static final List<NourishedSeasonHook> HOOKS = new ArrayList<>();

    private SeasonHookRegistry() {}

    /**
     * Registers a season hook.
     *
     * @param hook the season hook to register
     */
    public static void register(NourishedSeasonHook hook) {
        HOOKS.add(hook);
    }

    /**
     * Returns all registered season hooks.
     *
     * @return an unmodifiable list of season hooks
     */
    public static List<NourishedSeasonHook> getAll() {
        return Collections.unmodifiableList(HOOKS);
    }
}
