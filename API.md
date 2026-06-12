# Nourished API

Nourished is a nutrition mod for NeoForge 1.21.1. It tracks food-group balance, variety, and diet progression on top of [MariesLib](https://github.com/kgbcupcake/MarieLib). This document covers the **Nourished facade** (`NourishedAPI`). Shared types like `ValueDefinition` and `ThresholdEffect` live in MariesLib — see [MariesLib API](https://github.com/kgbcupcake/MarieLib/blob/main/API.md) for those.

## Dependencies

Nourished requires MariesLib at runtime. Both must be in the modpack.

```gradle
repositories {
    maven { url = "https://maven.neoforged.net/releases" }
}

dependencies {
    compileOnly "dev.maire.nourished:nourished:<version>"
    compileOnly "dev.marie.MariesLib:marieslib:<version>"
}
```

Guard API calls:

```java
if (!ModList.get().isLoaded("nourished")) return;
```

## Quick start

```java
@Mod("my_addon")
public final class MyAddon {
    public MyAddon() {
        if (!ModList.get().isLoaded("nourished")) return;

        NourishedAPI.registerValue(ValueDefinition.builder("fiber")
                .displayName("Fiber")
                .color(0xFF6B8E23)
                .defaultDecayRate(0.0014f)
                .criticalThreshold(0.15f)
                .lowThreshold(0.35f)
                .excessThreshold(0.9f)
                .build());

        NourishedAPI.registerSourceClassification(
                ResourceLocation.parse("minecraft:apple"), "fiber", 0.35f);
    }
}
```

Register during mod init only. See [Registration window](#registration-window) below.

## Registration window

All `NourishedAPI.register*` calls must happen in your `@Mod` constructor or `FMLCommonSetupEvent`. After init completes, registration throws `IllegalStateException`.

## NourishedAPI reference

All methods in `dev.maire.nourished.api.NourishedAPI`.

### Player queries

```java
float getTotal(Player player)                    // calorie / aggregate total
float getValueLevel(Player player, String valueKey)
ApplicationHistoryView getSourceMemory(Player player)
float getTotalCount(Player player)               // alias for getTotal
MariePlayerData getTrackingData(Player player)
void modifyValue(Player player, String valueKey, float delta)
String getVersion()
```

In nutrition context, `valueKey` is a nutrient key (`"proteins"`, `"grains"`, etc.). Query `NutrientRegistry` or call `getTrackingData` at runtime instead of hardcoding keys.

### Registration

| Method | Alias | Purpose |
|--------|-------|---------|
| `registerValue(ValueDefinition)` | `addNutrient` | Register a custom nutrient bar |
| `registerSourceClassification(id, key, amount)` | `registerSource` | Map a food item to a nutrient |
| `registerCustomEffect(ThresholdEffect)` | `addEffect` | Threshold-triggered effect |
| `registerCompatEntry(CompatDefinition)` | `addCompat` | Mod compat metadata |
| `registerValueSynergy(SynergyDefinition)` | `addNutrientSynergy` | Cross-nutrient synergy |
| `registerSourcePairSynergy(SourcePairSynergy)` | `addFoodSynergy` | Food-pair combo bonus |
| `registerTrackingProfile(ProfileDefinition)` | `addProfile` | Named diet profile |
| `registerMilestone(MilestoneDefinition)` | `addMilestone` | One-time cumulative goal |
| `registerSeasonHook(MarieSeasonHook)` | `addSeasonHook` | Seasonal modifier hook |
| `registerAbsorptionModifier(AbsorptionModifier)` | `addAbsorptionModifier` | Dynamic gain scaling |
| `registerReportProvider(ReportProvider)` | `addReportSection` | Custom `/nourished report` section |

`modifyValue` posts `ValueModifierEvent`, runs Nourished's KubeJS bridge, syncs to client, and applies effects if the effects module is enabled.

## Types from MariesLib

Nourished does not redefine these. Import from `dev.marie.MariesLib.api` or `dev.marie.MariesLib.compat`:

| Type | Package | Documented in |
|------|---------|---------------|
| `ValueDefinition` | `api` | [MarieLib API](https://github.com/kgbcupcake/MarieLib/blob/main/API.md) |
| `ThresholdEffect` | `api` | MarieLib API |
| `SynergyDefinition` | `api` | MarieLib API |
| `SourcePairSynergy` | `api` | MarieLib API |
| `ProfileDefinition` | `api` | MarieLib API |
| `MilestoneDefinition` | `api` | MarieLib API |
| `CompatDefinition` | `compat` | MarieLib API |
| `ApplicationHistoryView` | `api` | MarieLib API |
| `MariePlayerData` | `api` | MarieLib API |
| `AbsorptionModifier` | `api` | MarieLib API |
| `MarieSeasonHook` | `api` | MarieLib API |
| `ReportProvider` | `api` | MarieLib API |

## KubeJS

Nourished KubeJS events are `@Experimental`.

**Nourished-only events** (`NourishedEvents`):

| Event | ID | Notes |
|-------|-----|-------|
| `nutrientChanged` | `NourishedEvents.nutrientChanged` | Bar value changed |
| `nutrientCritical` | `NourishedEvents.nutrientCritical` | Below critical threshold |
| `nutrientExcess` | `NourishedEvents.nutrientExcess` | Above excess threshold |
| `sourceConsumed` | `NourishedEvents.sourceConsumed` | Food applied value |
| `foodEaten` | `NourishedEvents.foodEaten` | After eat + delta applied |
| `nutrientModifier` | `NourishedEvents.nutrientModifier` | Cancellable pre-apply |
| `gutHealthChanged` | `NourishedEvents.gutHealthChanged` | Raw food module |
| `rawFoodPenalty` | `NourishedEvents.rawFoodPenalty` | Raw food module |

```js
NourishedEvents.nutrientChanged(event => {
    if (event.valueKey === 'proteins' && event.newValue < 0.25) {
        event.player.tell('Eat some protein.')
    }
})
```

Generic value events (`valueChanged`, `decayTick`, etc.) also fire through MariesLib's `MarieKubeEvents`. See [MarieLib API](https://github.com/kgbcupcake/MarieLib/blob/main/API.md#kubejs).

Startup registration uses the `MarieAPI` KubeJS binding (same as MarieLib) for `registerValue`, `registerSourceClassification`, etc.

## Datapacks

Paths under `data/<namespace>/nourished/`:

| File / folder | Purpose |
|---------------|---------|
| `tags/item/nutrients/` | Item tag → nutrient group (highest priority for classification) |
| `food_values.json` | Per-category multipliers |
| `food_overrides.json` | Per-item nutrition overrides |
| `effects.json` | Threshold effects |
| `colors.json` | HUD bar colors |
| `scanner/scanner_spec.json` | Runtime classifier weights |
| `module_locks/` | Server-side feature locks |

Bundled defaults ship in the jar. Config copies land in `config/nourished/`. Datapack overrides win. See [ARCHITECTURE.md](ARCHITECTURE.md) for the full stack.

Scanner tooling runs through MariesLib — `/nourished scan` writes classification output you can paste into datapacks.

## JEI / REI / EMI

Tooltip integration is automatic when a recipe viewer is installed. No setup required. Custom report sections go through `registerReportProvider`.

## Commands

Root: `/nourished`

| Command | Permission | Description |
|---------|------------|-------------|
| `report` | — | Diet report for self |
| `report <player>` | 2 | Target report |
| `value <key>` | — | Nutrient detail for self |
| `value <key> <player>` | 2 | Target nutrient detail |
| `set <key> <value> <player>` | 2 | Set nutrient (0–1) |
| `reset <player>` | 2 | Reset all nutrients |
| `profile list` | — | List profiles |
| `profile set <profile> [player]` | 2 for target | Set profile |
| `profile get [player]` | 2 for target | Get profile |
| `reload` | 2 | Reload config + datapacks |
| `scan` / `scan_analysis` | 2 | Run food scanner |
| `debug <player>` | 2 | Raw tracking JSON |
| `diagnostics` | — | Datapack diagnostics |
| `get_unassigned` | — | Unclassified food list |
| `schema <type>` | — | Datapack schema template |

Library diagnostics also available under `/marieslib`.

## Versioning and stability

`NourishedAPI` and its method signatures are `@Stable`.

Underlying types and stability rules follow MariesLib — see [PHILOSOPHY.md](PHILOSOPHY.md) and [MariesLib PHILOSOPHY](https://github.com/kgbcupcake/MarieLib/blob/main/PHILOSOPHY.md).

KubeJS bindings are `@Experimental`.

Do not import `dev.maire.nourished.core`, `dev.maire.nourished.client`, or `dev.maire.nourished.api.impl`.
