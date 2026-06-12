# Nourished — Architecture Reference

Nourished is a nutrition mod built on [MariesLib](https://github.com/kgbcupcake/MarieLib/blob/main/ARCHITECTURE.md). The engine (tracking, classification pipeline, registry lifecycle, sync) lives in the library. This document covers nutrition-specific behavior and how Nourished wires the library up.

---

## 1. Terminology

When in doubt, refer back here before introducing new names in code or datapacks.

---

### Nutrient

A single tracked value in a player's diet, represented as a float between `0.0` and `1.0`. Each nutrient corresponds to one of the five food groups: **Fruits, Vegetables, Proteins, Grains, and Dairy**. These are the bars on the Diet Screen. Nutrients are defined in `NutrientRegistry` and drive buff and debuff calculations.

In MariesLib terms, a nutrient is a **value**.

> A nutrient is a bar. It goes up when you eat the right food. It decays over time.

---

### Category

A nutrient key used as a logical label when processing food. When food is eaten, it gets a **dominant category** — the nutrient it contributes to most strongly (e.g. `"proteins"`). Category drives the primary bar update and `categoryMemory` fatigue.

> Category and nutrient refer to the same five groups, but from different angles. Nutrient is the value. Category is the label attached to a food.

---

### Family

A grouping of foods similar in kind but possibly spanning categories. Examples: `"fish"`, `"bread"`, `"leafy_greens"`. Family is for **fatigue tracking only** — salmon, tuna, and cod all build `familyMemory` for `"fish"` even though the items differ.

Family is nullable. Not every food needs one.

> Family: "have I been eating the same _kind_ of thing too much?" Category: "have I been eating the same _nutrient group_ too much?"

---

### Classification

The process by which the scanner pipeline examines a food item and determines its category and family. Not a stored data structure — a pipeline stage. Tags, namespaces, keywords, and scanner signals feed into a score map that resolves to a dominant category and optional family.

> Classification is the verb. Category and family are the nouns it produces.

---

### The pipeline

```text
Food item
    │
    ▼
Classification (tags, scanner_spec, runtime resolver)
    │
    ▼
Scores aggregated → dominant category + optional family
    │
    ▼
NourishedFoodTriggerHandler → SourceApplicationPipeline
    │
    ├─→ nutrient bars updated (TrackingData)
    ├─→ sourceMemory updated
    ├─→ categoryMemory updated
    └─→ familyMemory updated
```

Nourished adds food-specific resolver stages in `NourishedResolverStages` (community tags, recipe inheritance, hard fallback, etc.). The pipeline itself is MariesLib's `SourceApplicationPipeline`.

---

### Memory

The three server-side maps on `TrackingData`:

- **`sourceMemory`** — per item id
- **`categoryMemory`** — per dominant nutrient category
- **`familyMemory`** — per food family

Each entry is a weighted apply count with a timestamp, decaying over the configured memory window. Memory never leaves the server raw; the client gets derived hints (neglected categories, fatigued families) for the HUD and Diet Screen.

> Memory is what you ate lately. It drives diminishing returns, not the bars directly.

---

### Fatigue

Loss of nutritional efficiency from repeating the same item, category, or family before memory fades. High fatigue = lower multiplier per bite, not zero gain.

---

### Debt

When the dominant category you're eating has high category memory, the system may nudge a different nutrient bar down — the lowest one, excluding what you're eating. Soft variety push, not a currency.

---

### Neglected category

A nutrient whose bar is among the lowest compared to the others. Used for HUD copy and debt targeting. Refers to low intake, not memory state.

---

### Fatigued family

A family key whose `familyMemory` still has weight — the kinds of food you've leaned on repetitively lately.

---

### What "group" means (and why we avoid it)

**Group** is ambiguous — nutrient group, food family, or category depending on context. Prefer `category` or `family` in code and docs.

---

## 2. Runtime model

### What lives where

**Server only:**

- Nutrition calculations (`SourceApplicationPipeline`, triggered by `NourishedFoodTriggerHandler`)
- Tracking storage (`TrackingData` / `NourishedTrackingData` via `TrackingAttachment`)
- Memory maps and diminishing-return multipliers
- Buff/debuff application (`NutritionEffectApplier`)
- Threshold crossing and NeoForge events (`MarieEvents`)
- Food scanner and classification

**Client only:**

- `MarieClientCache` — read-only sync snapshot
- HUD (`NourishedHUD`), Diet Screen, bar animations
- Gut health display (Raw Food module)

**Both:**

- `TrackingData` exists on both sides. Client copy is display-only — no populated memory maps.

Gut health (`GutHealthData` / `GutHealthAttachment`) is server-authoritative with its own sync. Raw Food module only.

---

### MarieClientCache vs TrackingData

`TrackingData` on the server is real state: nutrient values, calorie total, memory, timestamps.

`MarieClientCache` is whatever the server last sent — enough to draw the HUD. No math. Replaced wholesale on each sync packet.

Separate objects for thread safety. Server writes on the server thread. Client reads on the render thread.

---

### Lifecycle

```text
Player logs in
    │
    ▼
TrackingData loaded from attachment (or fresh with config starting values)
    │
    ▼
Full snapshot → MarieClientCache
    │
    ▼
Player eats food
    │
    ├─ NourishedFoodTriggerHandler on server thread
    ├─ Classification resolved (override → scanner → defaults)
    ├─ Memory updated, multiplier computed
    ├─ Nutrient deltas applied
    ├─ Threshold crossings → MarieEvents fired
    ├─ Effects applied if module enabled
    ├─ TrackingData saved to attachment
    └─ Delta packet → MarieClientCache
    │
    ▼
Player logs out → attachment auto-saves
```

---

## 3. Override priority stack

Food classifications resolve lowest to highest priority:

```text
bundled defaults  →  config override  →  datapack override
   (lowest)                                   (highest)
```

**Bundled defaults** — Item tag lists per food group (`fruits.json`, `vegetables.json`, etc.). Curated from real modpack playtesting. Works out of the box.

**Config** — Server owner tuning: decay, memory windows, diminishing curves, starting values. Changes how the mod behaves.

**Datapack** — Modpack maker control. Reassigns foods, adds unknown items. Always wins for classification.

> Specificity wins. Datapack took the most effort to set up, so it sits on top.

---

## 4. Threading

Same three-thread model as MariesLib — see [MarieLib ARCHITECTURE](https://github.com/kgbcupcake/MarieLib/blob/main/ARCHITECTURE.md#4-threading-model).

Nourished-specific rule unchanged:

> If it calculates, server thread. If it displays, render thread reading `MarieClientCache`. Network thread is the bridge.

Memory maps never go to the client.

---

## 5. Extension points

### NourishedAPI

Java mods depending on Nourished. Register nutrients, food classifications, effects, compat, synergies, profiles, milestones, hooks. Init-time only. See [API.md](API.md).

### Datapacks

Reclassify foods and rebalance values without Java. Top of the override stack.

### Events

Subscribe to `MarieEvents` on the NeoForge bus:

- `ValueChangedEvent` — nutrient bar changed
- `SourceAppliedEvent` — food eaten, value applied
- `ValueCriticalEvent` / `ValueExcessEvent` — threshold crossings
- `ValueModifierEvent` — cancellable, fires before a delta lands
- `SourceTriggerEvent` — cancellable, before pipeline runs

KubeJS wrappers live in `NourishedKubeEvents` (`nutrientChanged`, `foodEaten`, `gutHealthChanged`, etc.).

---

## 6. What Nourished is not

### Not a hardcore survival mod

No starvation beyond vanilla. No content locked behind nutrition thresholds. Rewards variety; does not punish casual play.

> If eating bread twice would feel bad to a casual player, it doesn't belong in core.

### Not a food mod

No crops, recipes, or cooking. Other mods add food. Nourished gives it meaning.

### Not a monolith

Stamina and hydration are **modules**, not tangled into nutrition core. Raw Food (gut health) is already a separate module with its own data and handlers.

The engine lives in MariesLib now. Nourished is the nutrition gameplay layer on top.

### Not opinionated about your modpack

Sensible defaults, full datapack override authority.

---

## 7. Modules

### Raw Food / Gut Health

Tracks per-player gut health. Raw food tiers (fine → severe) apply debuffs. Cooked food and variety help recovery. Config under `config/nourished/raw_food.json`. Data in `GutHealthAttachment`.

KubeJS: `gutHealthChanged`, `rawFoodPenalty`.

### Stamina

Reads nutrition and gut health to modulate stamina costs. Config under stamina module paths. Does not own nutrient bars — read-only layer.

---

## 8. Nourished wiring

`NourishedContextBuilder` registers food-specific resolvers, sync (`ModNetworking`), config screens, and the registration delegate with `MarieLibContext`.

`NourishedTrackingData` extends `TrackingData` with network delta payload support for Nourished's sync packets.

Bootstrap order in `Nourished` constructor: config → `NourishedLifecycle.register()` → `MariesLibBootstrap.attach()` → context builder → attachments → handlers.

---

## 9. Registry lifecycle (Nourished-specific)

Generic lifecycle mechanics (`reset → register → freeze`, load vs reload) are in [MarieLib ARCHITECTURE](https://github.com/kgbcupcake/MarieLib/blob/main/ARCHITECTURE.md#6-registry-lifecycle). `RegistryLifecycleManager` lives in MariesLib.

Nourished registers these registries in `NourishedLifecycle`, in order:

1. `NutrientRegistry` — keys everything else depends on
2. `ColorRegistry` (MarieLib)
3. `EffectRegistry`
4. `RawFoodConfig`
5. `StaminaConfig`
6. `FoodValueRegistry`
7. `FoodOverrideRegistry`
8. `ScannerSpecRegistry` (MarieLib)
9. `NourishedLockRegistry`
10. `ModCompatRegistry` (MarieLib, reload no-op)
11. `NourishedPresetRegistry`

Reload entry point: `ReloadPipeline.reloadAll()` (MarieLib), triggered by `/nourished reload`, server start, and config import.

Profiles, milestones, and food families are populated via API registration and frozen around datapack apply — not in the lifecycle list above.

---

## 10. Classification stages (Nourished)

Nourished registers custom resolver stages through `NourishedContextBuilder`:

- Community / explicit nutrient tags (`nourished:nutrients/*`)
- Recipe inheritance
- Runtime food resolver (scanner_spec weights)
- Hard fallback for unclassified edible items

Traces are inspectable in-game. See README scanner section and `/nourished diagnostics`.
