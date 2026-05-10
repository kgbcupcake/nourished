package dev.maire.nourished.api.registry;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.api.NourishedSeasonHook;
import dev.maire.nourished.core.registry.ListRegistry;

import java.util.List;

/**
 * Internal storage for season hooks registered via the public API.
 */
@ApiStatus.Internal
public final class SeasonHookRegistry {

    private static final ListRegistry<NourishedSeasonHook> REGISTRY = new ListRegistry<>("SeasonHookRegistry", null);

    private SeasonHookRegistry() {}

    @ApiStatus.Internal
    public static void freezeInternal() {
        REGISTRY.freeze();
    }

    /**
     * Registers a season hook.
     *
     * @param hook the season hook to register
     * @throws IllegalArgumentException if {@code hook} is null
     */
    public static void register(NourishedSeasonHook hook) {
        REGISTRY.register(hook);
    }

    /**
     * Returns all registered season hooks.
     *
     * @return an unmodifiable list of season hooks
     */
    public static List<NourishedSeasonHook> getAll() {
        return REGISTRY.values();
    }
}
