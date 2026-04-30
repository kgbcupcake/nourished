package dev.maire.nourished.compat;

import net.neoforged.fml.ModList;

public final class ModCompat {

    public static final boolean LSO_LOADED = ModList.get().isLoaded("legendarysurvivaloverhaul");

    private ModCompat() {}
}
