# Nourished — Architecture Reference

## 1. Terminology

This section defines the core concepts in Nourished and how they relate to each other. When in doubt, refer back here before introducing new names in code or datapacks.

---

### Nutrient

A single tracked value in a player's diet, represented as a float between `0.0` and `1.0`. Each nutrient corresponds to one of the five food groups: **Fruits, Vegetables, Proteins, Grains, and Dairy**. These are the bars displayed on the Diet Screen. Nutrients are defined in `NutrientRegistry` and drive all buff and debuff calculations.

> A nutrient is a bar. It goes up when you eat the right food. It decays over time.

---

### Category

A nutrient key used as a logical label when processing food. When a food is eaten, it is assigned a **dominant category** — the nutrient it contributes to most strongly (e.g. `"proteins"`). Category is used in two places: adding to the nutrient bar, and tracking category-level fatigue in `DietData.categoryMemory`.

> Category and nutrient refer to the same five groups, but from different angles. Nutrient is the value. Category is the label attached to a food.

---

### Family

A grouping of foods that are similar in kind but may span multiple categories. Examples: `"fish"`, `"bread"`, `"leafy_greens"`. Family is used exclusively for **fatigue tracking** — if a player eats salmon, tuna, and cod in quick succession, their `familyMemory` for `"fish"` accumulates, reducing the nutritional return of further fish even if the specific items differ.

Family is nullable. Not every food needs one.

> Family answers the question: "have I been eating the same _kind_ of thing too much?" Category answers: "have I been eating the same _nutrient group_ too much?"

---

### Classification

The **process** by which the scanner pipeline examines a food item and determines its category and family. Classification is not a data structure you store — it is a pipeline stage. The scanner reads item tags, namespaces, keywords, and other signals (`ClassificationSignal`) and produces a score map that resolves into a dominant category and an optional family.

> Classification is the verb. Category and family are the nouns it produces.

---

### ClassificationSignal

A single piece of evidence collected during classification. Each signal has a type (e.g. `COMMUNITY_TAG`, `KEYWORD`, `NAMESPACE`), a source (what triggered it), and a map of nutrient score contributions. Multiple signals are aggregated by the scanner pipeline to determine the final classification.

> A signal is one clue. Classification is the conclusion drawn from all the clues.

---

### The Pipeline (how these connect)

```text
Food Item
    │
    ▼
Scanner reads item tags, name, namespace
    │
    ▼
ClassificationSignals are collected
    │
    ▼
Scores aggregated → dominant Category + optional Family assigned
    │
    ▼
Player eats food → DietData.recordEat(itemId, category, family, time)
    │
    ├─→ nutrients map updated (the bar goes up)
    ├─→ categoryMemory updated (category fatigue tracked)
    └─→ familyMemory updated (family fatigue tracked)
```

---

### Memory

**Memory** is the collective name for the three server-side maps on `DietData` that remember recent eating: **`foodMemory`** (per item id), **`categoryMemory`** (per dominant nutrient category), and **`familyMemory`** (per food family). Each entry is a `FoodMemoryEntry` — essentially a weighted eat count with a last-eaten timestamp — that **decays over time** using the configured memory window. Memory never leaves the server; the client only receives derived hints (e.g. neglected categories and fatigued families) for display.

> Memory is “what have you been eating lately, at item / category / family granularity?” It drives diminishing returns, not the nutrient bars directly.

---

### Fatigue

**Fatigue** is the **loss of nutritional efficiency** from repeating the same signal before memory has faded. Mechanically, decayed eat counts from memory maps are mapped through the same diminishing curve as **item** repetition; **category** and **family** contributions are blended into the final multiplier (`computeBlendedMultiplier`). High fatigue means a **lower** multiplier — you still gain nutrients, but less per bite. **Category fatigue** can also be read as a normalized “freshness” score via `getCategoryFatigue` (1 = fresh, 0 = saturated).

> Fatigue is the penalty side of memory: the more you lean on one item, category, or family in a short window, the less rewarding the next bite becomes.

---

### Debt

**Debt** (more precisely **nutritional debt**) is a **soft variety nudge** in `applyNutritionalDebt`: when the **dominant category** you are eating has a **decayed category-memory count** above the configured **debt threshold**, Nourished finds another category with the **lowest nutrient bar** (excluding the one you are eating) and applies a **small downward tick** to that bar, bounded by a floor. It is not a loan or currency — it models “if you keep hammering one group, something else you have neglected slips a bit further.”

> Debt is the system gently pulling neglected bars down when you over-repeat a category, to encourage rotation without hard-blocking food.

---

### Neglected Category

A **neglected category** is a **nutrient category whose bar value is among the lowest** compared to the others. `getMostNeglectedCategories(n)` sorts nutrient bars ascending and takes the bottom _n_ keys — used for HUD / Diet Screen copy and internally when debt chooses which bar to nudge. “Neglected” refers to **low intake relative to other groups**, not to memory maps.

> A neglected category is a bar you have starved relative to the rest of your diet.

---

### Fatigued Family

A **fatigued family** is a **food family key** (e.g. `"fish"`) whose **family memory** still has meaningful weight: among non-expired `familyMemory` entries, families are ranked by **decayed eat count** descending, and `getMostFatiguedFamilies(n, time)` returns the top _n_ — the families you have **eaten most repetitively** recently, i.e. the ones contributing most to **family-side fatigue** in the multiplier blend.

> A fatigued family is a “kind” of food you have leaned on too hard lately; the HUD can surface it so the player sees the pattern.

---

### What "group" means (and why it doesn't exist as a term)

The word **group** is intentionally avoided in Nourished's API and documentation. It is ambiguous — it could mean nutrient group, food family, or category depending on context. If you find "group" appearing in code, treat it as a bug in naming and prefer `category` or `family` depending on what it actually represents.

---

## 3. Override Priority Stack

Nourished resolves food classifications through three layers, applied in order from lowest to highest priority:

```text
bundled defaults  →  config override  →  datapack override
   (lowest)                                   (highest)
```

Each layer exists for a different reason and targets a different audience.

---

### Layer 1 — Bundled Defaults

The mod ships with a set of item tag lists for each of the five food groups: `fruits.json`, `vegetables.json`, `proteins.json`, `grains.json`, and `dairy.json`. These were built by hand while playing a custom modpack — they represent real food items from real mods that Nourished has encountered and classified.

This layer exists so the mod works out of the box. A server owner should not need to configure anything for common food mods to work correctly.

> Bundled defaults are curated, not generated. They grow over time as more mods are tested.

---

### Layer 2 — Config Override

The server owner can change anything exposed in the Nourished config file. This includes nutrition mechanics (decay rates, memory windows, diminishing return curves, starting values) as well as any classification values that the config exposes.

This layer exists because every server has different balance needs. A hardcore survival server might want faster decay. A casual server might want higher starting values. Neither should need to touch a datapack to tune the experience.

> Config is for the server owner. It changes how the mod behaves, not what food belongs where.

---

### Layer 3 — Datapack Override

A datapack can override the bundled nutritional tag lists entirely. This means a modpack maker or server admin can reassign foods to different groups, add foods that Nourished doesn't know about, or remove foods from a group — all without editing any Java or config files.

This layer sits highest in the stack. A datapack assignment always wins over both the bundled defaults and anything the config sets for classifications.

This layer exists because modpack makers need control. A food mod that Nourished has never seen should still be classifiable without waiting for a mod update. A server running a themed pack (e.g. a medieval pack where sugar is rare) should be able to restrict what counts as a sugar food without forking the mod.

> Datapack is for modpack makers and server admins. It changes what food belongs where.

---

### Why this order

The stack is designed so that **specificity wins**. A generic bundled default is the weakest signal. A deliberate server-level config is stronger. A deliberate datapack override is the strongest because it requires the most intentional action to set up.

This means:

- Nourished always has a sane baseline with no setup required.
- Server owners can tune mechanics without touching files they shouldn't need to touch.
- Modpack makers have full control without needing to fork the mod or write any Java.

---

## 6. What Nourished Is Not

This section is as important as everything else in this document. Knowing what Nourished deliberately does not do prevents bad feature requests, bad API design, and scope creep.

---

### Nourished is not a hardcore survival mod

Nourished does not punish players. It does not add starvation mechanics beyond what vanilla already does. It does not lock content behind nutrition thresholds. It does not make food dangerous or force players to micromanage every meal.

The design goal is a lightweight nutrition layer that rewards dietary variety — not a system that makes the game harder to play.

> If a feature would make a casual player feel punished for eating bread twice, it does not belong in Nourished core.

---

### Nourished is not a food mod

Nourished does not add food items, crops, cooking mechanics, or recipes. It only tracks and reacts to food that already exists in the game. Other mods add the food. Nourished gives that food nutritional meaning.

---

### Nourished is not a monolith

Stamina and hydration are planned features, but they will be added as **modules within Nourished** — not tangled into the nutrition core. The nutrition system, the stamina system, and the hydration system should each be independently understandable. Adding hydration should not require touching nutrition code.

A separate companion mod will also be built on top of Nourished's public API. That mod exists specifically to validate that the API works correctly for external developers. If something is hard to do from that companion mod, it is a bug in the API, not a reason to add it to Nourished core.

---

### Nourished is not opinionated about your modpack

Nourished ships with sensible defaults but does not enforce them. A modpack maker can reclassify any food, rebalance any value, and override any default via datapacks. Nourished does not assume it knows better than the person running the server.

---

## 2. Runtime Model

---

### What lives where

**Server only:**

- All nutrition calculations (`FoodEatenHandler`)
- Diet data storage (`DietData` via `DietAttachment`)
- Food memory — item, category, and family (`foodMemory`, `categoryMemory`, `familyMemory`)
- Diminishing return multipliers
- Buff and debuff application (`NutritionEffectApplier`)
- Threshold crossing detection
- The food scanner and classification pipeline

**Client only:**

- `ClientDietCache` — a read-only snapshot of the last diet state sent by the server
- HUD rendering (`NourishedHUD`)
- Diet Screen (`DietScreen`)
- Bar flash animations

**Both:**

- `DietData` as a class exists on both sides, but the client copy is always a display-only shell. The client's `DietData` never has populated memory maps — those are server-side only and never travel to the client.

---

### What ClientDietCache is and why it exists separately from DietData

`DietData` is the real data. It lives on the server, attached to the player, and contains everything: nutrient values, calorie totals, food memory, category memory, family memory, timestamps.

`ClientDietCache` is a display cache. It holds a snapshot of whatever the server last sent — nutrient bar values, recent foods, neglected categories, fatigued families — just enough to draw the HUD and Diet Screen. It never does math. It never stores memory maps. It is replaced wholesale whenever a new sync packet arrives from the server.

The reason they are separate is thread safety. The server writes to `DietData` on the server thread. The client reads from `ClientDietCache` on the render thread. By keeping them separate and making the cache snapshot `volatile`, the two threads never touch the same object.

---

### Lifecycle of a player's diet data from login to logout

```text
Player logs in
    │
    ▼
DietData loaded from player attachment (or created fresh for new players)
Starting nutrient values set from config (default 50%)
    │
    ▼
Server sends full DietData snapshot to client → ClientDietCache.set()
Client HUD and Diet Screen now have something to display
    │
    ▼
Player eats food
    │
    ├─ FoodEatenHandler fires on server thread
    ├─ Classification resolved (override → scanner → defaults)
    ├─ Dominant category and family determined
    ├─ Memory updated, multiplier computed
    ├─ Nutrient deltas applied (with seasonal and absorption modifiers)
    ├─ Threshold crossings checked → events fired if crossed
    ├─ Effects applied if effects module is enabled
    ├─ DietData saved back to player attachment
    └─ Delta packet sent to client → ClientDietCache.applyDelta()
    │
    ▼
Client receives delta packet
    │
    ├─ Nutrient bar values updated
    ├─ Recent foods, neglected categories, fatigued families updated
    └─ Bar flash animation triggered for any nutrient that increased
    │
    ▼
Player logs out
    │
    └─ DietData saved automatically via NeoForge attachment system

---

## 4. Threading Model

Nourished touches three threads during normal gameplay. Understanding which thread owns which data prevents the concurrency bugs that kill framework mods.

---

### The three threads

**Server thread** — owns all nutrition state. `FoodEatenHandler` fires here. `DietData` is read and written here. All nutrient math, memory updates, multiplier calculations, threshold checks, and effect applications happen on this thread. Nothing else should write to `DietData`.

**Network thread** — carries data from server to client. When the server calls `ModNetworking.syncDietDelta()`, a packet is assembled and dispatched. On the client side, `ClientDietCache.applyDelta()` or `ClientDietCache.set()` is called on this thread to update the cache snapshot.

**Render thread** — reads display state only. The HUD and Diet Screen read from `ClientDietCache` here. They never write anything. They never call into `DietData` directly.

---

### How the handoff works safely

`ClientDietCache` holds a `volatile Snapshot` record. The network thread replaces the entire snapshot atomically. The render thread reads it. Because the snapshot is replaced whole rather than mutated field by field, and because `volatile` guarantees visibility across threads, the render thread always sees either the old complete snapshot or the new complete snapshot — never a half-updated mix of both.

The food memory maps (`foodMemory`, `categoryMemory`, `familyMemory`) never travel to the client at all. The client `DietData` copy always has empty memory maps. This is intentional — memory is server-side state and the client has no need for it.

---

### The rule

> If it calculates, it runs on the server thread.
> If it displays, it runs on the render thread and reads only from `ClientDietCache`.
> The network thread is a one-way bridge between them.

---

## 5. Extension Points

There are three ways to extend Nourished. They are not interchangeable — each one exists for a different use case.

---

### 1. The Java API (`NourishedAPI`)

Use this when you are writing a Java mod that depends on Nourished.

`NourishedAPI` is the single entry point for all external Java integration. It is marked `@ApiStatus.Stable` and will not break between minor versions. Through this class you can:

- Read a player's nutrient levels and food memory
- Register custom nutrients that participate in all standard mechanics
- Register food classifications (map a food item to a nutrient)
- Register custom effects triggered by threshold crossings
- Register compat entries for other mods' food items
- Register synergies, absorption modifiers, season hooks, milestones, and diet profiles

All registration must happen during mod initialization, before the server starts. Calling registration methods after that point throws an `IllegalStateException`.

> Use the Java API when you need to do something in code that datapacks cannot express.

---

### 2. Datapacks

Use this when you want to reclassify foods, add foods Nourished doesn't know about, or change nutritional values — without writing any Java.

Datapacks sit at the top of the override stack and win over both bundled defaults and config. A modpack maker can ship a datapack that completely redefines which foods belong to which nutrient group. No mod dependency required beyond Nourished itself.

> Use datapacks when you are building a modpack or running a server and need control over food classifications.

---

### 3. NeoForge Events (`NourishedEvents`)

Use this when you want to react to nutrition changes without owning the food or the player.

Nourished fires events on the NeoForge event bus at key moments:

- `NutrientChangedEvent` — a nutrient bar value changed
- `FoodEatenEvent` — a food was eaten and a nutrient delta was applied
- `NutrientCriticalEvent` — a nutrient crossed below its critical threshold
- `NutrientExcessEvent` — a nutrient crossed above the excess threshold
- `NutrientModifierEvent` — fired before a delta is applied, cancellable and modifiable

You can also listen for `NutrientModifierEvent` to intercept and adjust any nutrient change before it lands, or cancel it entirely.

> Use events when you want to react to nutrition state without modifying food classifications or registering new nutrients.

---

## 7. Registry Lifecycle

Nourished uses two different “registry stories”: **keyed config registries** (nutrients, food values, JSON on disk / datapack slices) and **API list registries** (definitions exposed through `dev.maire.nourished.api.registry` that are filled from mods and datapacks).

### reset() → register() → freeze()

Internal keyed and list registries (`AbstractRegistry`, `ListRegistry`) share a common lifecycle:

1. **`reset()`** — clears mutable storage, clears the frozen snapshot, and returns the registry to a **mutable registration phase**. Used when reloading definitions from disk or when rebuilding state before a new parse.

2. **`register()` / `register(key, value)`** — appends entries **only while unfrozen**. After `freeze()`, registration throws `IllegalStateException` (registry name appears in the message). This guarantees gameplay code never mutates definitions mid-tick by accident.

3. **`freeze()`** — copies mutable entries into **immutable snapshots** used for fast, allocation-free reads at runtime. Some API-facing registries expose `freezeInternal()` through `NourishedApiDefinitionRegistries` so datapack apply can close a pass deterministically.

Addon-facing registries documented under “Extension Points” follow the same rule: **register during mod init before the server starts** — late registration is rejected so load order stays predictable.

### When `load()` vs `reload()` is used

- **`load()`** — **cold startup** path: called from the mod constructor (`Nourished`) once per game launch to **create default files if missing** and **populate registries from config / bundled JSON** before other systems wire up. It is not tied to `/reload` or the config screen.

- **`reload()`** — **runtime refresh** of the same config-backed files while the game is running (after edits, import, server startup, or `/nourished reload`). Implementations typically **`reset()`**, re-parse, **`freeze()`**, and sometimes call follow-up hooks (e.g. `NutrientRegistry.reload()` re-runs `FoodNutritionRegistry.init()`).

**Datapack JSON** for Nourished definitions uses a separate path: `NourishedDataLoader` / `loadFromDatapack(...)` on individual registries where applicable — that is **data reload**, not the same as the static `*.reload()` pipeline in §8.

### `RegistryLifecycleManager` vs `NourishedApiDefinitionRegistries`

| | **`RegistryLifecycleManager`** | **`NourishedApiDefinitionRegistries`** |
|---|-------------------------------------------|----------------------------------------|
| **Purpose** | One ordered pass to **re-read server/client config files** (and datapack slices) for the nine static registries (nutrients, colors, effects, food values, overrides, scanner spec, locks, mod compat, presets). | **Coordinates reset/freeze** of **public API list registries** around **`NourishedDataLoader`** datapack apply and common setup. |
| **Trigger** | Bootstrap `loadAll()`, server startup (`ServerStartingEvent`), `/nourished reload`, config UI “reload”, import apply — see §8. Datapack reload uses `loadAll(ResourceManager)`. | Start of each datapack `apply` (`onDatapackApplyBegin` — **reset** selected registries after the first pass), end of apply (`onDatapackApplyEnd` — **freeze**), and after common setup (`freezeModOnlyRegistriesAfterCommonSetup` for registries that only accept mod-time registration). |
| **Data source** | Config directory JSON / TOML-adjacent files written by the mod and edited by users; optional `loadFromDatapack` hook for datapack JSON. | Datapack JSON under `data/<namespace>/nourished/...` plus mod constructor registrations. |

They **do not call each other**. Think: **RegistryLifecycleManager** = “refresh local config files”; **ApiDefinitionRegistries** = “wrap datapack-driven API registry passes in reset/freeze discipline.”

> `NourishedReloadPipeline.reloadAll()` is retained as a thin shim that delegates to `RegistryLifecycleManager.reloadAll()`. Older call sites (`ImportExportManager`, `NourishedCommand`, `NourishedConfigScreen`) still target the shim; new code should call `RegistryLifecycleManager.reloadAll()` directly.

---

## 8. Reload Pipeline

**Entry point:** `dev.maire.nourished.core.registry.RegistryLifecycleManager.reloadAll()` — a single static method that reloads every **config-backed** registry in a **fixed order** so downstream readers always see a consistent snapshot. `NourishedReloadPipeline.reloadAll()` remains as a one-line shim that delegates here (kept to avoid churn at older call sites).

The manager is also the **bootstrap entry point** (`loadAll()`, called once during mod construction) and the **datapack reload entry point** (`loadAll(ResourceManager)`, called by `ConfigReloadHandler.onAddReloadListeners`). All three flows iterate the same registration list in the same order.

### Registration order (and why order matters)

1. **`NutrientRegistry`** — Re-establishes nutrient keys, thresholds, icons, and tags; every later registry and `DietData` logic assumes this set is current. *No datapack hook.*
2. **`ColorRegistry`** — Re-binds display colors to the current nutrient key list. *Has datapack hook.*
3. **`EffectRegistry`** — Re-loads threshold-triggered effects; definitions reference nutrient ids from the live key set. *Has datapack hook.*
4. **`FoodValueRegistry`** — Re-loads per-food nutrition contributions; classification and HUD use nutrient keys from (1). *Has datapack hook.*
5. **`FoodOverrideRegistry`** — Re-applies item-level overrides on top of base food values. *Has datapack hook.*
6. **`ScannerSpecRegistry`** — Re-loads scanner/heuristic spec used by the food tooling pipeline. *Has datapack hook.*
7. **`LockRegistry`** — Re-loads merge / lock rules used when persisting or combining config-derived maps. *Has datapack hook.*
8. **`ModCompatRegistry`** — Re-reads the bundled `mod_compat.json`. *Reload is a no-op (preserves prior behavior: this registry was never in the legacy reload pipeline). No datapack hook.*
9. **`PresetRegistry`** — Re-ensures built-in preset files on disk. Load and reload both call `ensureBuiltInFilesOnDisk()`. *No datapack hook.*

> **Out of scope for the manager:** `FoodFamilyResolver`, `DietProfileRegistry`, and `MilestoneRegistry` do not have `load()`/`reload()` methods — they are populated via API registrations (mod constructor or KubeJS) and frozen by `NourishedApiDefinitionRegistries` around the datapack apply cycle. See §7.

### Call sites

| Handler | Event | Calls |
|---------|-------|-------|
| `ConfigReloadHandler.onServerStarting()` | **`ServerStartingEvent`** | `NourishedReloadPipeline.reloadAll()` |
| `ConfigReloadHandler.onAddReloadListeners()` | **`AddReloadListenerEvent`** | `RegistryLifecycleManager.loadAll(ResourceManager)` |
| `NourishedCommand.reloadAll` | **`/nourished reload`** | `NourishedReloadPipeline.reloadAll()` (after `MinecraftServer.reloadResources(...)`, on the server executor so datapack and config views stay aligned) |
| `NourishedConfigScreen.ReloadConfigsListEntry` (button lambda) | Config screen “reload configs” | `NourishedReloadPipeline.reloadAll()` — immediate client-side refresh for local files |
| `ImportExportManager.applyImport` | Config import completes | `NourishedReloadPipeline.reloadAll()`, then `NutrientUiColors.clearOverrides()` |

### Server startup reload

Config-backed registries reload **once** when the server starts via **`ServerStartingEvent`**. This consolidates the reload into a single pass, avoiding per-dimension duplicate cycles.

| | |
|---|---|
| **Event** | `ServerStartingEvent` (not `LevelEvent.Load`) |
| **When** | Server startup (once per server lifecycle) |
| **What** | Full config reload via `NourishedReloadPipeline.reloadAll()` → `RegistryLifecycleManager.reloadAll()` |
| **Where** | `ConfigReloadHandler.onServerStarting()` in `ConfigReloadHandler.java` |
| **Why** | The previous `LevelEvent.Load` hook fired once per dimension (overworld, nether, end), triggering multiple full reload cycles on every startup |

See `ConfigReloadHandler.onServerStarting()` for implementation.

Do not reorder the nine entries registered in `Nourished.registerLifecycleEntries()` without auditing dependents; the Javadoc on that method and the order list above should stay in sync.

### Adding a new registry

To wire a new config-backed registry into the lifecycle:

1. **Implement the registry hooks on your class.** At minimum provide a `public static void load()` (called at bootstrap) and a `public static void reload()` (called on `/nourished reload` and friends). If the registry should also re-read from datapacks, add a `public static void loadFromDatapack(ResourceManager rm)` that uses `NourishedResourceLoader.loadFromModConfig(...)`.

2. **Register it in `Nourished.registerLifecycleEntries()`.** Insert the call in dependency order — entries earlier in the list run first, so anything that depends on nutrient keys must come after `NutrientRegistry`. Pick the right overload:

    ```java
    // No datapack hook (rare):
    RegistryLifecycleManager.registerRegistry(
            "MyRegistry", MyRegistry::load, MyRegistry::reload);

    // With datapack hook (typical for config-backed registries):
    RegistryLifecycleManager.registerRegistry(
            "MyRegistry", MyRegistry::load, MyRegistry::reload, MyRegistry::loadFromDatapack);
    ```
3. **Do not call `MyRegistry.load()` elsewhere from `Nourished`'s constructor.** `RegistryLifecycleManager.loadAll()` handles it. Likewise, do not add `MyRegistry.reload()` to any other reload chain — the shim `NourishedReloadPipeline.reloadAll()` and `ConfigReloadHandler` both route through the manager.
4. **Update the order list above** in this section to keep the docs aligned with `Nourished.registerLifecycleEntries()`.

Registries without a meaningful reload (e.g. write-on-startup helpers) should pass `() -> {}` as the reload runnable rather than skip registration, so logging still shows them in the lifecycle pass.
