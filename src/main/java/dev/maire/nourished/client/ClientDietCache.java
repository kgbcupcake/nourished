package dev.maire.nourished.client;

import dev.maire.nourished.diet.DietData;

public class ClientDietCache {

    private static DietData current = new DietData();

    public static void set(DietData data) {
        current = data;
    }

    public static DietData get() {
        return current;
    }
}
