package dev.maire.nourished.api;

import dev.maire.nourished.nutrition.Nourished;

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

    public static void close() {
        currentPhase = Phase.CLOSED;
        Nourished.LOGGER.info("[Nourished] Registration phase: CLOSED");
    }

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
