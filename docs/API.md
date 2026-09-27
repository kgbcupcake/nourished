# Nourished API

`dev.maire.nourished.api.NourishedAPI` is Nourished's public Java API for querying player nutrition state, registering nutrition definitions, and extending Nourished behavior. Nourished uses [MariesLib](https://github.com/kgbcupcake/MariesLib) for shared value tracking and definition types.

Use Nourished's API for nutrition-specific integrations. If your mod only needs a general-purpose value or tracking system, depend on MariesLib directly instead.

## Requirements and setup

Nourished targets Minecraft 1.21.1 on NeoForge 21.1.228 or later and requires MariesLib 0.1.1-beta.5 or later at runtime. Cloth Config is also required on the client.

For a development environment, make the Nourished and MariesLib API available at compile time using your project's normal NeoForge dependency setup. Example Gradle coordinates:

```gradle
dependencies {
    compileOnly "dev.maire.nourished:nourished:<nourished-version>"
    compileOnly "dev.marie.MariesLib:marieslib:<marieslib-version>"
}
```

Do not bundle Nourished or MariesLib inside your mod. If Nourished is an optional integration, declare it as an optional mod dependency and isolate code that references Nourished classes so that it is only loaded when Nourished is present. If your mod directly uses MariesLib types exposed by the API, those types must also be available at compile time.

## API stability and compatibility

`NourishedAPI` and each of its public methods are marked `@ApiStatus.Stable`. Nourished's API policy is that these method signatures will not break without a major version bump and deprecation cycle. Types supplied by MariesLib, such as `ValueDefinition` and `ThresholdEffect`, follow MariesLib's stability policy.

Nourished does not currently expose a separate Nourished API version. `NourishedAPI.getVersion()` returns MariesLib's API version, not Nourished's mod or API version. Check the installed mod versions and their dependency requirements when determining compatibility.

Only depend on `dev.maire.nourished.api` and documented public MariesLib API types. Packages such as `dev.maire.nourished.core`, `client`, `config`, `compat`, and `api.impl` are implementation details and are not supported integration surfaces.

## Registration lifecycle

All `NourishedAPI` registration methods must be called while MariesLib's registration window is open. Nourished opens its registrations during mod initialization and closes them during `FMLCommonSetupEvent` after its setup work. A registration attempted after the window closes throws `IllegalStateException`.

There is currently no dedicated Nourished registration event. Register early during mod initialization; do not defer registrations to server start, datapack reload, or runtime callbacks. Where one registration references another definition, register the referenced definition first—for example, register a custom nutrient before registering a food mapping that uses its key. Addons with complex cross-mod ordering requirements should account for NeoForge mod initialization ordering rather than assuming a separate, ordered Nourished phase.

## Registering a nutrient and food

`ValueDefinition` is supplied by MariesLib. See the [MariesLib API reference](https://github.com/kgbcupcake/MariesLib/blob/main/API.md) for its builder and available fields.

```java
import dev.maire.nourished.api.NourishedAPI;
import dev.marie.framework.api.value.ValueDefinition;
import net.minecraft.resources.ResourceLocation;

ValueDefinition omega3 = ValueDefinition.builder("omega3")
        .displayName("Omega-3")
        .beneficial(true)
        .build();

NourishedAPI.registerValue(omega3);
NourishedAPI.registerSourceClassification(
        ResourceLocation.fromNamespaceAndPath("example_food", "starfruit"),
        "omega3",
        1.0f
);
```

Nutrient keys must be unique. A duplicate key passed to `registerValue` throws `IllegalArgumentException`. `registerSourceClassification` accepts an item ID, a registered nutrient key, and a finite contribution amount. An unknown nutrient key is rejected; an item ID that is not registered yet is logged as a warning.

An explicit source classification is a direct mapping for the specified item and is considered ahead of scanner inference. It does not bypass Nourished's excluded-item handling or other runtime food overrides. For tag-based mappings or richer static data, use datapacks instead; see the [Datapack Support wiki page](https://github.com/kgbcupcake/nourished/wiki/Datapack-Support).

## API reference

All methods are static on `dev.maire.nourished.api.NourishedAPI`. Registration methods (the `register*` methods below) require the registration window described above. Each `add*` method is an alias for its corresponding registration method.

The public constant `NourishedAPI.CALORIES_TRACKER_ID` is the MariesLib tracker ID used for Nourished's calorie history.

### Player state queries

These methods do not require the registration window to be open:

```java
float getTotal(Player player)
float getValueLevel(Player player, String valueKey)
ApplicationHistoryView getSourceMemory(Player player)
float getTotalCount(Player player) // alias of getTotal
float getYesterdayCalories(Player player)
List<TrackerHistoryEntry> getCalorieHistory(Player player)
MariePlayerData getTrackingData(Player player)
void modifyValue(Player player, String valueKey, float delta)
String getVersion()
```

- `getTotal` and `getTotalCount` return the player's current calorie total.
- `getValueLevel` returns a nutrient level, or `-1.0f` for an unknown key or a `null` player.
- `getSourceMemory` returns a read-only view of recent food history; a `null` player yields an empty view.
- `getYesterdayCalories` returns the latest completed daily calorie total, or `-1.0f` if calorie history is disabled or no history is available.
- `getCalorieHistory` returns completed-period entries, newest first. It returns an empty list when history is disabled, unavailable, or the player is `null`.
- `getTrackingData` returns a snapshot of calories, registered nutrient levels, and food memory.
- `modifyValue` applies a direct nutrient delta through the NeoForge `ValueModifierEvent`; a cancelled event prevents the change. The server synchronizes the updated value for server players.
- `getVersion` returns the MariesLib API version; it is not a Nourished version query.

Nutrient keys are data-defined and may vary with datapacks and other integrations. Avoid hardcoding assumptions about the set of keys; inspect `getTrackingData(player)` for the registered keys and values.

### Registration methods

| Method | Purpose |
|---|---|
| `registerValue(ValueDefinition)` | Register a nutrient. Alias: `addNutrient`. |
| `registerSourceClassification(ResourceLocation, String, float)` | Map one item to a nutrient and contribution amount. Alias: `registerSource`. |
| `registerCustomEffect(ThresholdEffect)` | Register an effect triggered by nutrient thresholds. Alias: `addEffect`. |
| `registerCompatEntry(CompatDefinition)` | Register a compatibility definition. Alias: `addCompat`. |
| `registerValueSynergy(SynergyDefinition)` | Register an interaction between nutrient values. Alias: `addNutrientSynergy`. |
| `registerSourcePairSynergy(SourcePairSynergy)` | Register a food-pair synergy. Alias: `addFoodSynergy`. |
| `registerTrackingProfile(ProfileDefinition)` | Register a named nutrition profile. Alias: `addProfile`. |
| `registerMilestone(MilestoneDefinition)` | Register a nutrition milestone. Alias: `addMilestone`. |
| `registerSeasonHook(MarieSeasonHook)` | Register a seasonal integration hook. Alias: `addSeasonHook`. |
| `registerAbsorptionModifier(AbsorptionModifier)` | Register an absorption modifier. Alias: `addAbsorptionModifier`. |
| `registerReportProvider(ReportProvider)` | Add a section to the `/nourished` report. Alias: `addReportSection`. |

These definition and hook interfaces are provided by MariesLib. Consult the [MariesLib API reference](https://github.com/kgbcupcake/MariesLib/blob/main/API.md) for builder syntax and type-specific contracts.

## NeoForge events

For Java event integration, subscribe to MariesLib's `MarieEvents` on the NeoForge event bus. Relevant events include:

- `ValueChangedEvent`
- `ValueCriticalEvent`
- `ValueExcessEvent`
- `SourceAppliedEvent`
- `ValueModifierEvent`
- `SourceTriggerEvent`

These events are provided by MariesLib, not duplicated as a Nourished-specific Java event API. See [MariesLib's event documentation](https://github.com/kgbcupcake/MariesLib/blob/main/API.md#neoforge-events) for signatures and event behavior.

## KubeJS

Nourished's optional KubeJS integration is experimental. It exposes the `NourishedAPI` script binding and the `NourishedEvents` event group. The binding includes nutrient, nutrient-curve, and tracker-milestone registration, along with player nutrient and gut-health queries. Events cover nutrient changes and thresholds, food consumption, gut health, raw-food penalties, and nutrient modifiers.

See the [KubeJS Integration wiki page](https://github.com/kgbcupcake/nourished/wiki/KubeJS-Integration) for script examples, event fields, and cancellation behavior.

## Datapacks and configuration

Prefer datapacks for static food classifications and content. Nourished datapack resources use paths such as:

```text
data/<namespace>/nourished/nutrients/<id>.json
data/<namespace>/nourished/source_classifications/<id>.json
data/<namespace>/nourished/effects/<id>.json
data/<namespace>/nourished/compat/<id>.json
data/nourished/tags/item/nutrients/<nutrient>.json
```

The `data/nourished/tags/...` path is for tags in the `nourished` namespace. Other Nourished resource types can use the namespace of the datapack that provides them.

See the [Datapack Support](https://github.com/kgbcupcake/nourished/wiki/Datapack-Support) and [Community Tags](https://github.com/kgbcupcake/nourished/wiki/Community-Tags) wiki pages for schemas and examples.
