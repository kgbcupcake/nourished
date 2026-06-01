package dev.maire.nourished.api;

import dev.maire.nourished.core.Nourished;

@ApiStatus.Internal
public final class NourishedAPIState {

    public enum Phase {
        MOD_INIT,
        DATAPACK_RELOAD,
        CLOSED
    }

    private static volatile Phase currentPhase = Phase.MOD_INIT;

    private NourishedAPIState() {}

    public static boolean isRegistrationAllowed() {
        return currentPhase == Phase.MOD_INIT || currentPhase == Phase.DATAPACK_RELOAD;
    }

    public static Phase getPhase() {
        return currentPhase;
    }

    /**
     * Ends the mod-init registration window. Safe to call when already {@link Phase#CLOSED}
     * (for example after a nested {@link DatapackReloadScope}).
     */
    @ApiStatus.Internal
    public static void close() {
        if (currentPhase == Phase.CLOSED) {
            return;
        }
        currentPhase = Phase.CLOSED;
        Nourished.LOGGER.info("[Nourished] Registration phase: CLOSED");
    }

    @ApiStatus.Internal
    public static DatapackReloadScope openForDatapackReload() {
        currentPhase = Phase.DATAPACK_RELOAD;
        Nourished.LOGGER.info("[Nourished] Registration phase: DATAPACK_RELOAD");
        return new DatapackReloadScope();
    }

    public static final class DatapackReloadScope implements AutoCloseable {
        @Override
        public void close() {
            currentPhase = Phase.CLOSED;
            Nourished.LOGGER.info("[Nourished] Registration phase: CLOSED");
        }
    }
}
