# Nourished API

`NourishedAPI` is a thin, stable facade over [MariesLib](https://github.com/kgbcupcake/MariesLib), 
the engine that actually runs classification, tracking, decay, and effects. If you're building a new Marie mod with its own value bars (not nutrition), 
depend on MariesLib directly — see [MariesLib's API.md](https://github.com/kgbcupcake/MariesLib/blob/main/API.md).

Every method below is verified directly against source (`dev.maire.nourished.api.NourishedAPI` — the package really is spelled `maire`, not a typo).

## Getting started

Nourished requires MariesLib as a separate mod at runtime — no JarJar bundling. Add both to your dev environment:

```gradle
dependencies {
    compileOnly "dev.marie.MariesLib:marieslib:<version>"
    compileOnly "dev.maire.nourished:nourished:<version>"
}
```

Bootstrap follows MariesLib's own pattern: see [MariesLib's bootstrap docs](https://github.com/kgbcupcake/MariesLib/blob/main/API.md#bootstrap) 
for the underlying mechanism. Nourished itself calls `MarieBootstrap.attach("nourished", modEventBus)` in its own `@Mod` constructor; addon mods building against `NourishedAPI` 
don't need to call this themselves, just declare Nourished as a dependency and register during your own mod init.

## Registration window

All `register*` calls must happen during mod initialization: your `@Mod` constructor or `FMLCommonSetupEvent`. 
The window closes after init; calling register outside it throws `IllegalStateException`.

## Stability

`NourishedAPI` is `@ApiStatus.Stable` at the class level, and **every single method** carries the same `@Stable`
tier individually: no method on this facade is `@Experimental` or `@Internal`. Shared definition types (`ValueDefinition`, `ThresholdEffect`, `CompatDefinition`, etc.) 
come from MariesLib and follow its stability rules — see MariesLib's API.md for those.

---

## `NourishedAPI` reference

All static, in `dev.maire.nourished.api.NourishedAPI`. 29 public methods total: 18 primary, 11 pure aliases (each alias just delegates to its primary, no added logic).

### Player state queries

No registration-window restriction: safe to call any time.

```java
float getTotal(Player player)
float getValueLevel(Player player, String valueKey)
ApplicationHistoryView getSourceMemory(Player player)
float getTotalCount(Player player)              // alias of getTotal
MariePlayerData getTrackingData(Player player)   // aggregate snapshot of the above three
void modifyValue(Player player, String valueKey, float delta)
String getVersion()
```

### Registration:  nutrients & foods

```java
void registerValue(ValueDefinition definition)
void addNutrient(ValueDefinition definition)                 // alias
void registerSourceClassification(ResourceLocation sourceId, String valueKey, float amount)
void registerSource(ResourceLocation sourceId, String valueKey, float amount)  // alias
```

### Registration:  effects

```java
void registerCustomEffect(ThresholdEffect definition)
void addEffect(ThresholdEffect definition)  // alias
```

### Registration:  compatibility

```java
void registerCompatEntry(CompatDefinition definition)
void addCompat(CompatDefinition definition)  // alias
```

### Registration:  synergies & combos

```java
void registerValueSynergy(SynergyDefinition definition)
void addNutrientSynergy(SynergyDefinition definition)         // alias
void registerSourcePairSynergy(SourcePairSynergy definition)
void addFoodSynergy(SourcePairSynergy definition)              // alias
```

### Registration — profiles & milestones

```java
void registerTrackingProfile(ProfileDefinition definition)
void addProfile(ProfileDefinition definition)      // alias
void registerMilestone(MilestoneDefinition definition)
void addMilestone(MilestoneDefinition definition)  // alias
```

### Registration: hooks & modifiers

```java
void registerSeasonHook(MarieSeasonHook hook)
void addSeasonHook(MarieSeasonHook hook)                        // alias
void registerAbsorptionModifier(AbsorptionModifier modifier)
void addAbsorptionModifier(AbsorptionModifier modifier)         // alias
void registerReportProvider(ReportProvider provider)
void addReportSection(ReportProvider provider)                  // alias
```

All 12 `register*` primaries throw `IllegalStateException` if called after the registration window closes. The 7 query methods have no such restriction.

Definition types (`ValueDefinition`, `ThresholdEffect`, `SynergyDefinition`, `SourcePairSynergy`, `ProfileDefinition`, `MilestoneDefinition`, `CompatDefinition`)
are MariesLib types — see [MariesLib's builder reference](https://github.com/kgbcupcake/MariesLib/blob/main/API.md#definition-builders)
for their full builder syntax; Nourished doesn't wrap or replace them.

---

## Gameplay modules

Nourished currently ships one true gameplay module beyond core nutrition tracking:

**Raw Food / Gut Health**: tracks per-player gut health, degrading from raw food and recovering from cooked food and variety.
Config toggles: `enableRawFoodPenalty`, `enableGutHealth`. Config file: `config/nourished/raw_food.json`.

Stamina is **not** a native module: it's a compat integration with the separate [Peak Stamina](https://modrinth.com)
mod, registered like any other compat entry, not a Nourished-owned system.

---

## Dynamic UI config

New this release: these live in `NourishedClientConfig`:

| Key | Type | Purpose |
|---|---|---|
| `hudClassicMode` | boolean | Force the HUD back to the pre-dynamic classic renderer |
| `dietScreenClassicMode` | boolean | Force the Diet Screen back to the classic renderer |
| `showDietScreenButton` | boolean (default `true`) | Show/hide the Diet Screen's inventory button. The Diet Screen keybind still opens the screen when this is off only the button disappears. |

---

## NeoForge events

Subscribe to MariesLib's `MarieEvents` — Nourished doesn't define its own parallel event set for Java consumers:

- `ValueChangedEvent`:  nutrient bar changed
- `SourceAppliedEvent`:  food eaten, value applied
- `ValueCriticalEvent`: `ValueExcessEvent` — threshold crossings
- `ValueModifierEvent`:  cancellable, fires before a delta lands
- `SourceTriggerEvent`:  cancellable, before the pipeline runs

Full signatures in [MariesLib's API.md](https://github.com/kgbcupcake/MariesLib/blob/main/API.md#neoforge-events).

---

## KubeJS

Backed by `dev.maire.nourished.kubejs.NourishedKubeEvents`. The global JS binding is `NourishedEvents` (an event-group ID, not a Java class, don't confuse the two when reading source). Confirmed real events:

```js
NourishedEvents.nutrientChanged(event => { /* ... */ })
NourishedEvents.foodEaten(event => { /* ... */ })
NourishedEvents.gutHealthChanged(event => { /* ... */ })
NourishedEvents.rawFoodPenalty(event => { /* ... */ })
```

The real event group has 8 total handlers; the remaining 4 weren't individually enumerated in the last source pass.
If you're relying on KubeJS integration beyond the four above, check `NourishedKubeEvents` 
directly or ask for a follow-up pass to fill in the rest before depending on undocumented ones.

---

## Datapack & config paths

| Concept | Path |
|---|---|
| Food classification tags | `data/nourished/tags/item/nutrients/{fruits,vegetables,proteins,grains,dairy}.json` |
| Food overrides | `config/nourished/overrides/Overrides/food_overrides.json` |
| Source classification overrides | `config/nourished/overrides/Overrides/source_classifications.json` |
| Excluded items | `config/nourished/overrides/Overrides/excluded_items.json` (owned by MariesLib's `ExcludedItemsRegistry`; Nourished only reads via `isExcluded(...)`) |
| Effects | `effects.json` |
| Colors | `colors.json` |
| Raw Food config | `config/nourished/raw_food.json` |
| Scanner spec (food-classification weights) | `config/nourished/scanner_spec.json` — see [CONTRIBUTING.md](CONTRIBUTING.md) for how to extend this |

Legacy flat `overrides/` paths (without the nested `Overrides/` subfolder) auto-migrate on load — no manual file moves needed.

Note: `excluded_items.json`'s array here is unrelated to `scanner_spec.json`'s own separate `excluded_items` JSON key — same concept, two independent mechanisms owned by different MariesLib registries. Don't conflate them.

---

## Mod compatibility

30+ dedicated `CompatDefinition` entries beyond automatic `FoodProperties` detection: Delight-family mods, Pam's HarvestCraft 2, Croptopia, Farmer's Delight, Cold Sweat, 
and more. Full list in [README.md](README.md#-broad-mod-compatibility).

Any mod exposing standard `FoodProperties` is auto-classified with zero configuration.

---

## Versioning

Nourished tracks MariesLib's API version (`MarieAPIVersion` — see MariesLib's API.md). Both mods must be present at runtime and compatible. 
This release hard-depends on MariesLib **0.1.1-beta.5+**.
