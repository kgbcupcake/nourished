# Nourished — Architecture Reference

## 1. Terminology

This section defines the core concepts in Nourished and how they relate to each other. When in doubt, refer back here before introducing new names in code or datapacks.

---

### Nutrient

A single tracked value in a player's diet, represented as a float between `0.0` and `1.0`. Each nutrient corresponds to one of the six food groups: **Fruits, Vegetables, Proteins, Grains, Sugars, and Dairy**. These are the bars displayed on the Diet Screen. Nutrients are defined in `NutrientRegistry` and drive all buff and debuff calculations.

> A nutrient is a bar. It goes up when you eat the right food. It decays over time.

---

### Category

A nutrient key used as a logical label when processing food. When a food is eaten, it is assigned a **dominant category** — the nutrient it contributes to most strongly (e.g. `"proteins"`). Category is used in two places: adding to the nutrient bar, and tracking category-level fatigue in `DietData.categoryMemory`.

> Category and nutrient refer to the same six groups, but from different angles. Nutrient is the value. Category is the label attached to a food.

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

```
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

### What "group" means (and why it doesn't exist as a term)

The word **group** is intentionally avoided in Nourished's API and documentation. It is ambiguous — it could mean nutrient group, food family, or category depending on context. If you find "group" appearing in code, treat it as a bug in naming and prefer `category` or `family` depending on what it actually represents.

---

## 3. Override Priority Stack

Nourished resolves food classifications through three layers, applied in order from lowest to highest priority:

```
bundled defaults  →  config override  →  datapack override
   (lowest)                                   (highest)
```

Each layer exists for a different reason and targets a different audience.

---

### Layer 1 — Bundled Defaults

The mod ships with a set of item tag lists for each of the six food groups: `fruits.json`, `vegetables.json`, `proteins.json`, `grains.json`, `sugars.json`, and `dairy.json`. These were built by hand while playing a custom modpack — they represent real food items from real mods that Nourished has encountered and classified.

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

```
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
```
