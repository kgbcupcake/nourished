package dev.maire.nourished.api;

public final class NourishedAPIState {

    static boolean registrationOpen = true;

    private NourishedAPIState() {}

    public static void close() {
        registrationOpen = false;
    }
}
