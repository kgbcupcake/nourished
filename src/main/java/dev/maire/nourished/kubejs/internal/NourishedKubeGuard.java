package dev.maire.nourished.kubejs.internal;

import dev.latvian.mods.kubejs.event.EventHandler;
import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.kubejs.internal.KubeGuard;
import dev.maire.nourished.kubejs.NourishedKubeEvents;

import java.util.concurrent.ConcurrentHashMap;

@ApiStatus.Internal
public final class NourishedKubeGuard {

    private static final ConcurrentHashMap<String, Boolean> LISTENER_CACHE = new ConcurrentHashMap<>();

    private NourishedKubeGuard() {}

    public static boolean isPresent() {
        return KubeGuard.isPresent();
    }

    public static boolean hasListeners(String eventId) {
        if (!isPresent()) {
            return false;
        }
        return LISTENER_CACHE.computeIfAbsent(eventId, NourishedKubeGuard::resolveHasListeners);
    }

    private static boolean resolveHasListeners(String eventId) {
        EventHandler handler = NourishedKubeEvents.handlerFor(eventId);
        return handler != null && handler.hasListeners();
    }

    public static void invalidateCache() {
        LISTENER_CACHE.clear();
    }
}
