package dev.maire.nourished.compat;

import net.neoforged.fml.ModList;

import java.util.Map;

public final class ModCompat {

    public static final boolean LSO_LOADED       = ModList.get().isLoaded("legendarysurvivaloverhaul");
    public static final boolean CROPTOPIA_LOADED = ModList.get().isLoaded("croptopia");
    public static final boolean FARMERS_LOADED   = ModList.get().isLoaded("farmersdelight");
    public static final boolean PAMS_LOADED      = ModList.get().isLoaded("pamhc2foodcore");
    public static final boolean MAMAS_LOADED     = ModList.get().isLoaded("mamasherbs");
    public static final boolean SERENE_LOADED    = ModList.get().isLoaded("sereneseasons");

    public static final Map<String, Boolean> DETECTED = Map.of(
            "legendarysurvivaloverhaul", LSO_LOADED,
            "croptopia",                 CROPTOPIA_LOADED,
            "farmersdelight",            FARMERS_LOADED,
            "pamhc2foodcore",            PAMS_LOADED,
            "mamasherbs",                MAMAS_LOADED,
            "sereneseasons",             SERENE_LOADED
    );

    private static final Map<String, String> NAMESPACE_TO_MODID = Map.of(
            "legendarysurvivaloverhaul", "legendarysurvivaloverhaul",
            "croptopia",                 "croptopia",
            "farmersdelight",            "farmersdelight",
            "pamhc2foods",               "pamhc2foodcore",
            "pamhc2foodcore",            "pamhc2foodcore",
            "mamasherbs",                "mamasherbs",
            "sereneseasons",             "sereneseasons"
    );

    private ModCompat() {}

    /**
     * Maps an item namespace to the modid key used in compat config.
     * Returns the namespace as-is if it's a tracked mod, null if not tracked.
     */
    public static String namespaceToModid(String namespace) {
        return NAMESPACE_TO_MODID.get(namespace);
    }
}
