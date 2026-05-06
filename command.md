Add compatibility tab, server commands, and per-mod compat toggles.
Context
The mod already has ModCompat.java with an LSO check, a full config system with LockRegistry, NourishedConfig, and a Cloth Config screen organized into 8 categories. This prompt adds a Compatibility tab to the config screen, two server commands, and a proper per-mod compat system.

Step 1 — Expand ModCompat.java
Replace the single LSO_LOADED boolean with a full detection map for all supported mods:
javapublic static final boolean LSO_LOADED         = ModList.get().isLoaded("legendarysurvivaloverhaul");
public static final boolean CROPTOPIA_LOADED   = ModList.get().isLoaded("croptopia");
public static final boolean FARMERS_LOADED     = ModList.get().isLoaded("farmersdelight");
public static final boolean PAMS_LOADED        = ModList.get().isLoaded("pamhc2foodcore");
public static final boolean MAMAS_LOADED       = ModList.get().isLoaded("mamasherbs");
public static final boolean SERENE_LOADED      = ModList.get().isLoaded("sereneseasons");
Also add a public static final Map<String, Boolean> DETECTED that maps a clean string key to each boolean, so the config screen can iterate it dynamically:
javapublic static final Map<String, Boolean> DETECTED = Map.of(
    "legendarysurvivaloverhaul", LSO_LOADED,
    "croptopia",                 CROPTOPIA_LOADED,
    "farmersdelight",            FARMERS_LOADED,
    "pamhc2foodcore",            PAMS_LOADED,
    "mamasherbs",                MAMAS_LOADED,
    "sereneseasons",             SERENE_LOADED
);

Step 2 — Add per-mod compat toggles to NourishedConfig.java
For each mod key in ModCompat.DETECTED, add two config values:

compat.<modid>.enableCodeCompat — boolean, default true. When false, any code-level special behavior for that mod is skipped (e.g. LSO disabling effects).
compat.<modid>.enableTagCompat — boolean, default true. When false, that mod's foods are excluded from nutrient tag classification.

Build these dynamically in the NourishedConfig constructor by iterating ModCompat.DETECTED.keySet() — do not hardcode each mod individually. Store them in:
javaprivate final Map<String, ModConfigSpec.BooleanValue> compatCodeToggles = new LinkedHashMap<>();
private final Map<String, ModConfigSpec.BooleanValue> compatTagToggles  = new LinkedHashMap<>();
Expose getters:
javapublic boolean isCodeCompatEnabled(String modid)
public boolean isTagCompatEnabled(String modid)

Step 3 — Wire compat toggles into the mod
Code compat (LSO example): In Nourished.java, the NutritionDecayHandler is currently skipped when ModCompat.LSO_LOADED. Change this to:
javaif (!ModCompat.LSO_LOADED || !NourishedConfig.get().isCodeCompatEnabled("legendarysurvivaloverhaul")) {
    NeoForge.EVENT_BUS.register(new NutritionDecayHandler());
}
Apply the same pattern to any other location that checks ModCompat.*_LOADED for code-level behavior.
Tag compat: In FoodNutritionRegistry.resolveNutrientBars, after resolving tags for an item, check which mod owns the item using ResourceLocation.parse(item.getDescriptionId()).getNamespace() mapped to the mod's namespace. If NourishedConfig.get().isTagCompatEnabled(modid) returns false for that namespace, remove any matches that came from that mod's namespace and treat the item as unclassified.
Add a helper in ModCompat:
javapublic static String namespaceToModid(String namespace) {
    // maps item namespace to modid key used in config
    // e.g. "farmersdelight" -> "farmersdelight", "pamhc2foods" -> "pamhc2foodcore"
    // return namespace as-is for known matches, null if not a tracked mod
}

Step 4 — Add Compatibility tab to NourishedConfigScreen
Add a new Compatibility category after the Advanced category. For each entry in ModCompat.DETECTED, add a subsection showing:

Mod name as a label (use the modid formatted as title case)
Status indicator in the tooltip: "Status: Installed ✅" or "Status: Not detected ❌" — the toggle is still shown even when not detected so pack creators can pre-configure it
A boolean toggle for enableCodeCompat with tooltip: "Enable special code-level behavior for this mod (e.g. disabling conflicting systems)."
A boolean toggle for enableTagCompat with tooltip: "Enable nutrient tag classification for foods from this mod."

Both toggles should respect LockRegistry as all other entries do. Build the entries by iterating ModCompat.DETECTED.keySet() — do not hardcode per-mod entries.

Step 5 — Create NourishedCommand.java
Create src/main/java/dev/maire/nourished/command/NourishedCommand.java.
Register it on RegisterCommandsEvent. Root literal: nourished.
/nourished debug <player>

Accessible to any player (permission level 0)
If the executor is a non-op player, they can only query themselves — if they pass a different player's name, respond with "You can only view your own nutrition data."
Op players (permission level 2+) can query any online player
Output to the executor's chat (not broadcast):

=== Nourished Debug: <PlayerName> ===
Calories: 1240 / 2000
fruits:      0.72  [▓▓▓▓▓▓▓░░░]
vegetables:  0.45  [▓▓▓▓░░░░░░]
proteins:    0.91  [▓▓▓▓▓▓▓▓▓░]
grains:      0.23  [▓▓░░░░░░░░]  ⚠ CRITICAL
sugars:      0.60  [▓▓▓▓▓▓░░░░]
dairy:       0.38  [▓▓▓░░░░░░░]

The bar is 10 characters wide, filled proportionally with ▓ and ░
Append ⚠ CRITICAL when value is below criticalThreshold
Nutrient keys come from NutrientRegistry.getKeys() — do not hardcode them
Uses DietAttachment to read the player's data server-side

/nourished get_unassigned_foods

Op only (permission level 2)
Scans the entire item registry (BuiltInRegistries.ITEM) for every item that has FoodProperties (i.e. is edible)
For each edible item, calls FoodNutritionRegistry.resolveNutrientBars(stack, false) — if the result only contains the fallback key (first key from NutrientRegistry.getKeys()) AND the item has no matching tag in any nourished:nutrients/* tag, it is considered unassigned
Writes results to config/nourished/unassigned_foods.txt (not chat, not a mod folder — use FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID).resolve("unassigned_foods.txt"))
File format:

# Nourished — Unassigned Foods
# Generated: <timestamp>
# These items have no nourished:nutrients/* tag and defaulted to the fallback nutrient.
# Add them to data/nourished/tags/items/nutrients/<category>.json to classify them.

minecraft:bread         [fallback: grains]
croptopia:tomato        [fallback: grains]

After writing, send the executor a chat message: "Wrote <count> unassigned foods to config/nourished/unassigned_foods.txt"
Uses NutrientRegistry.getKeys() for the fallback key — do not hardcode "grains"

Register NourishedCommand in Nourished.java via:
javaNeoForge.EVENT_BUS.register(new NourishedCommand());

Constraints

NeoForge 1.21+ mod
Do not modify NutrientRegistry.java, DietData.java, DietAttachment.java, or ModNetworking.java
No hardcoded nutrient key names or mod IDs in logic — iterate registries and maps
All new config keys follow the existing NourishedConfig pattern with the -1 sentinel or boolean default
All new Cloth Config entries respect LockRegistry.isLocked() and LockRegistry.isServerOnly()
Build must pass with no new errors beyond existing NeoForge deprecation warnings
Every new config key added must have a corresponding translation key placeholder comment in the format // config.nourished.<keyName> and // config.nourished.<keyName>.desc
