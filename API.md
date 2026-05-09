# Nourished API

Nourished is a NeoForge nutrition system mod for Minecraft 1.21.1 that tracks nutrient balance, food variety, and diet progression. It exposes a public API so other mods and modpacks can register nutrients, food mappings, effects, synergies, profiles, milestones, and extension hooks without editing Nourished internals. For a full working integration sample, see [`example-addon/`](example-addon/).

## Getting Started

Add Nourished as a compile-time dependency in your addon mod:

```gradle
repositories {
    maven { url = "https://maven.neoforged.net/releases" }
}

dependencies {
    compileOnly "dev.maire.nourished:nourished:<version>"
}
```

Minimum setup:
- Depend on Nourished (`compileOnly` for API compile; runtime via modpack/mod list).
- Guard API calls with `ModList.get().isLoaded("nourished")`.
- Register your definitions during mod initialization (`@Mod` constructor or setup flow).

Hello world (register one nutrient):

```java
@Mod("my_addon")
public final class MyAddon {
    public MyAddon() {
        if (!ModList.get().isLoaded("nourished")) return;
        NutrientDefinition def = NutrientDefinition.builder("my_nutrient")
                .displayName("My Nutrient")
                .color(0xFF66CCFF)
                .defaultDecayRate(0.001f)
                .criticalThreshold(0.1f)
                .lowThreshold(0.3f)
                .excessThreshold(0.9f)
                .build();
        NourishedAPI.registerNutrient(def);
    }
}
```

## Registration Window

All calls to NourishedAPI.register*() must happen during mod initialization —
specifically inside your @Mod constructor or a FMLCommonSetupEvent handler.

The registration window closes automatically after mod initialization completes.
Attempting to register outside this window throws IllegalStateException.

Datapack reloads open a temporary secondary registration window for datapack-driven
content only. This window is managed internally by Nourished and addon mods do not
need to handle it.

Rules:
- DO register in your @Mod constructor
- DO register in FMLCommonSetupEvent if order relative to other mods matters
- DO NOT register lazily on first use
- DO NOT register inside event handlers that fire after startup
- DO NOT register on the server thread at runtime

Example of correct registration timing:

```java
@Mod("my_addon")
public final class MyAddon {
    public MyAddon() {
        if (!ModList.get().isLoaded("nourished")) return;
        // safe — inside @Mod constructor, registration window is open
        NourishedAPI.registerNutrient(myNutrientDef);
        NourishedAPI.registerFoodClassification(
            ResourceLocation.parse("minecraft:apple"), "fiber", 0.35f);
    }
}
```

## Core Concepts

- **Nutrients**: Named bars (for example `proteins`, `grains`) with thresholds and decay behavior.
- **Food classifications**: Mapping from item/tag to nutrient key and nutrient gain amount.
- **Diet profiles**: Named archetypes with per-nutrient threshold and decay overrides.
- **Synergies**: Cross-nutrient or food-pair interactions that grant effects or bonuses.
- **Milestones**: One-time cumulative nutrient goals with optional rewards.
- **Absorption modifiers**: Runtime hooks that scale nutrient gain before application.
- **Season hooks**: Optional seasonal adapters that alter nutrient behavior by season.

## API Reference

All classes below are in `dev.maire.nourished.api`.

### `NourishedAPI`

Static API entrypoint.

```java
public static float getCalories(Player player)
public static float getNutrientLevel(Player player, String nutrientKey)
public static FoodMemoryView getFoodMemory(Player player)
public static void registerNutrient(NutrientDefinition definition)
public static void registerFoodClassification(ResourceLocation foodId, String nutrientKey, float amount)
public static void registerCustomEffect(EffectDefinition definition)
public static void registerCompatEntry(CompatDefinition definition)
public static void registerNutrientSynergy(NutrientSynergyDefinition definition)
public static void registerFoodSynergy(FoodSynergyDefinition definition)
public static void registerDietProfile(DietProfileDefinition definition)
public static void registerMilestone(NutrientMilestoneDefinition definition)
public static void registerSeasonHook(NourishedSeasonHook hook)
public static void registerAbsorptionModifier(NutrientAbsorptionModifier modifier)
public static void registerReportProvider(DietReportProvider provider)
```

Parameters/returns:
- `Player player`: target player.
- `String nutrientKey`: registered nutrient id.
- `ResourceLocation foodId`: item id (`namespace:path`).
- `float amount`: nutrient gain/override amount.
- Registration methods: `void`, throw `IllegalArgumentException` on invalid definitions.

Example:

```java
NourishedAPI.registerFoodClassification(ResourceLocation.parse("minecraft:apple"), "fiber", 0.35f);
float fiber = NourishedAPI.getNutrientLevel(player, "fiber");
```

### `NutrientDefinition`

Defines a nutrient.

```java
public static NutrientDefinition.Builder builder(String id)
public String getId()
public String getDisplayName()
public int getColor()
public float getDefaultDecayRate()
public float getCriticalThreshold()
public float getLowThreshold()
public float getExcessThreshold()
public @Nullable NutrientRenderer getCustomRenderer()
```

Builder methods:

```java
public Builder displayName(String displayName)
public Builder color(int color)
public Builder defaultDecayRate(float rate)
public Builder criticalThreshold(float threshold)
public Builder lowThreshold(float threshold)
public Builder excessThreshold(float threshold)
public Builder customRenderer(@Nullable NutrientRenderer renderer)
public NutrientDefinition build()
```

Example:

```java
NutrientDefinition fiber = NutrientDefinition.builder("fiber")
        .displayName("Fiber")
        .color(0xFF6B8E23)
        .defaultDecayRate(0.0014f)
        .criticalThreshold(0.15f)
        .lowThreshold(0.35f)
        .excessThreshold(0.9f)
        .build();
```

### `EffectDefinition`

Defines nutrient threshold effects.

```java
public static EffectDefinition.Builder builder()
public String getNutrientKey()
public float getThreshold()
public EffectDefinition.ThresholdType getThresholdType()
public ResourceLocation getEffectId()
public int getAmplifier()
public int getDuration()
```

Builder methods:

```java
public Builder nutrientKey(String nutrientKey)
public Builder threshold(float threshold)
public Builder thresholdType(EffectDefinition.ThresholdType type)
public Builder effectId(ResourceLocation effectId)
public Builder amplifier(int amplifier)
public Builder duration(int duration)
public EffectDefinition build()
```

Example:

```java
EffectDefinition bonus = EffectDefinition.builder()
        .nutrientKey("fiber")
        .threshold(0.8f)
        .thresholdType(EffectDefinition.ThresholdType.EXCESS)
        .effectId(ResourceLocation.parse("minecraft:speed"))
        .amplifier(0)
        .duration(200)
        .build();
```

### `CompatDefinition`

Defines compat metadata and optional item->nutrient mappings for another mod.

```java
public static CompatDefinition.Builder builder(String modId)
public String getModId()
public CompatDefinition.CompatCategory getCategory()
public Map<ResourceLocation, String> getFoodTagMappings()
```

Builder methods:

```java
public Builder category(CompatDefinition.CompatCategory category)
public Builder addFoodTagMapping(ResourceLocation foodId, String nutrientKey)
public Builder addAllFoodTagMappings(Map<ResourceLocation, String> mappings)
public CompatDefinition build()
```

Example:

```java
CompatDefinition compat = CompatDefinition.builder("my_food_mod")
        .category(CompatDefinition.CompatCategory.FOOD_MOD)
        .addFoodTagMapping(ResourceLocation.parse("my_food_mod:salad"), "fiber")
        .build();
```

### `FoodMemoryView`

Read-only view into a player's recent food consumption history. Use this to query
dietary variety, check for recent foods, or implement diminishing returns mechanics
in your addon.

Obtain via:

```java
FoodMemoryView view = NourishedAPI.getFoodMemory(player);
```

Methods:

```java
// Returns recently consumed food ids, most recent first
List<ResourceLocation> getRecentFoods()

// Returns true if the player has eaten this food within the memory window
boolean hasEatenRecently(ResourceLocation foodId)

// Returns ticks since the player last ate this food, or -1 if not in memory
long getTimeSinceEaten(ResourceLocation foodId)
```

The memory window duration is controlled by the memoryWindowMinutes config value.
Entries decay exponentially and are removed once their effective weight drops below
the configured threshold.

Example — reduce nutrient gain for recently eaten foods in an absorption modifier:

```java
public class MyDiminishingReturnsModifier implements NutrientAbsorptionModifier {
    @Override
    public String getModifierId() { return "myaddon:diminishing_returns"; }

    @Override
    public float getAbsorptionMultiplier(Player player, String nutrientKey, float baseAmount) {
        FoodMemoryView memory = NourishedAPI.getFoodMemory(player);
        // if player ate this type of food very recently, reduce gain
        // this is an example pattern — your logic will vary
        return 1.0f;
    }
}
```

Do not mutate game state from inside FoodMemoryView — it is a read-only snapshot.

### `NutrientSynergyDefinition`

Defines a nutrient pair synergy.

```java
public static NutrientSynergyDefinition.Builder builder(String id)
public String getId()
public String getNutrientKeyA()
public NutrientSynergyDefinition.LevelCondition getConditionA()
public String getNutrientKeyB()
public NutrientSynergyDefinition.LevelCondition getConditionB()
public @Nullable ResourceLocation getBonusEffectId()
public int getEffectAmplifier()
public int getEffectDuration()
public boolean isPenalty()
```

Builder methods:

```java
public Builder nutrientA(String nutrientKey, NutrientSynergyDefinition.LevelCondition condition)
public Builder nutrientB(String nutrientKey, NutrientSynergyDefinition.LevelCondition condition)
public Builder bonusEffect(ResourceLocation effectId)
public Builder effectAmplifier(int amplifier)
public Builder effectDuration(int duration)
public Builder penalty(boolean penalty)
public NutrientSynergyDefinition build()
```

Example:

```java
NutrientSynergyDefinition synergy = NutrientSynergyDefinition.builder("fiber_protein_regen")
        .nutrientA("fiber", NutrientSynergyDefinition.LevelCondition.OPTIMAL)
        .nutrientB("proteins", NutrientSynergyDefinition.LevelCondition.HIGH)
        .bonusEffect(ResourceLocation.parse("minecraft:regeneration"))
        .effectAmplifier(0)
        .effectDuration(100)
        .build();
```

### `FoodSynergyDefinition`

Defines a food pair combo.

```java
public static FoodSynergyDefinition.Builder builder(String id)
public String getId()
public ResourceLocation getFoodA()
public ResourceLocation getFoodB()
public int getTimeWindowTicks()
public String getBonusNutrientKey()
public float getBonusAmount()
```

Builder methods:

```java
public Builder foodA(ResourceLocation foodA)
public Builder foodB(ResourceLocation foodB)
public Builder timeWindowTicks(int ticks)
public Builder bonusNutrientKey(String nutrientKey)
public Builder bonusAmount(float amount)
public FoodSynergyDefinition build()
```

Example:

```java
FoodSynergyDefinition combo = FoodSynergyDefinition.builder("apple_carrot_fiber")
        .foodA(ResourceLocation.parse("minecraft:apple"))
        .foodB(ResourceLocation.parse("minecraft:carrot"))
        .timeWindowTicks(200)
        .bonusNutrientKey("fiber")
        .bonusAmount(0.5f)
        .build();
```

### `DietProfileDefinition`

Defines diet archetype overrides.

```java
public static DietProfileDefinition.Builder builder(String id)
public String getId()
public String getDisplayName()
public Map<String, Float> getCustomThresholds()
public Map<String, Float> getCustomDecayRates()
public List<ResourceLocation> getBonusEffects()
public @Nullable String getDescription()
```

Builder methods:

```java
public Builder displayName(String displayName)
public Builder customThreshold(String nutrientKey, float threshold)
public Builder customDecayRate(String nutrientKey, float decayRate)
public Builder addBonusEffect(ResourceLocation effectId)
public Builder description(String description)
public DietProfileDefinition build()
```

Example:

```java
DietProfileDefinition herbivore = DietProfileDefinition.builder("herbivore")
        .displayName("Herbivore")
        .customThreshold("fiber", 0.45f)
        .customDecayRate("fiber", 0.0009f)
        .build();
```

### `NutrientMilestoneDefinition`

Defines cumulative nutrient milestones.

```java
public static NutrientMilestoneDefinition.Builder builder(String id)
public String getId()
public String getNutrientKey()
public float getCumulativeGoal()
public @Nullable ResourceLocation getRewardEffectId()
public int getRewardAmplifier()
public int getRewardDuration()
public @Nullable ResourceLocation getAdvancementId()
```

Builder methods:

```java
public Builder nutrientKey(String nutrientKey)
public Builder cumulativeGoal(float goal)
public Builder rewardEffect(ResourceLocation effectId)
public Builder rewardAmplifier(int amplifier)
public Builder rewardDuration(int duration)
public Builder advancement(ResourceLocation advancementId)
public NutrientMilestoneDefinition build()
```

Example:

```java
NutrientMilestoneDefinition milestone = NutrientMilestoneDefinition.builder("fiber_100")
        .nutrientKey("fiber")
        .cumulativeGoal(100f)
        .rewardEffect(ResourceLocation.parse("minecraft:night_vision"))
        .rewardDuration(600)
        .build();
```

### `NutrientAbsorptionModifier`

Runtime hook to scale nutrient gains.

Expected usage:

```java
NourishedAPI.registerAbsorptionModifier((player, nutrientKey, baseAmount) -> baseAmount);
```

Typical parameters:
- player context
- nutrient key
- input amount

Return:
- modified absorption amount (`float`)

### `NourishedSeasonHook`

Season adapter hook for seasonal nutrient adjustments.

Expected usage:

```java
NourishedAPI.registerSeasonHook(mySeasonHookImplementation);
```

Implementations should return season-aware modifiers used by Nourished internals.

### `DietReportProvider`

Extends `/nourished report` output.

```java
public String getSectionId()
public Component getSectionTitle()
public List<Component> generateReport(Player player)
```

Example:

```java
public final class FiberReport implements DietReportProvider {
    public String getSectionId() { return "my_mod:fiber"; }
    public Component getSectionTitle() { return Component.literal("Fiber Status"); }
    public List<Component> generateReport(Player player) {
        float v = NourishedAPI.getNutrientLevel(player, "fiber");
        return List.of(Component.literal("Fiber: " + v));
    }
}
```

### `NutrientRenderer`

Optional renderer interface for custom nutrient HUD rendering.

Used via:

```java
NutrientDefinition.builder("my_nutrient")
    .customRenderer(myRenderer)
    .build();
```

Implement this when default bar rendering is insufficient for your nutrient.

## Events

All events are in `dev.maire.nourished.api.NourishedEvents`, plus `NutrientModifierEvent` in `dev.maire.nourished.api`.

### `NutrientChangedEvent`
- Fires: after nutrient value changes.
- Fields: `player`, `nutrientKey`, `oldValue`, `newValue`.
- Cancellable: **No**.

Java:

```java
@SubscribeEvent
public static void onChanged(NourishedEvents.NutrientChangedEvent e) {
    System.out.println(e.getNutrientKey() + ": " + e.getOldValue() + " -> " + e.getNewValue());
}
```

KubeJS:

```js
ServerEvents.custom("nourished.nutrient_changed", event => {
  console.info(`${event.nutrientKey}: ${event.oldValue} -> ${event.newValue}`)
})
```

### `NutrientCriticalEvent`
- Fires: when nutrient crosses into critical-low state.
- Fields: `player`, `nutrientKey`.
- Cancellable: **No**.

Java:

```java
@SubscribeEvent
public static void onCritical(NourishedEvents.NutrientCriticalEvent e) {
    e.getPlayer().sendSystemMessage(Component.literal("Critical: " + e.getNutrientKey()));
}
```

KubeJS:

```js
ServerEvents.custom("nourished.nutrient_critical", event => {
  event.player.tell(`Critical nutrient: ${event.nutrientKey}`)
})
```

### `NutrientExcessEvent`
- Fires: when nutrient crosses above excess threshold.
- Fields: `player`, `nutrientKey`.
- Cancellable: **No**.

Java:

```java
@SubscribeEvent
public static void onExcess(NourishedEvents.NutrientExcessEvent e) {
    System.out.println("Excess: " + e.getNutrientKey());
}
```

KubeJS:

```js
ServerEvents.custom("nourished.nutrient_excess", event => {
  console.info(`Excess nutrient: ${event.nutrientKey}`)
})
```

### `FoodEatenEvent`
- Fires: after Nourished computes food nutrient gain.
- Fields: `player`, `foodId`, `nutrientKey`, `amount`.
- Cancellable: **No**.

Java:

```java
@SubscribeEvent
public static void onFood(NourishedEvents.FoodEatenEvent e) {
    System.out.println(e.getFoodId() + " -> " + e.getNutrientKey() + " +" + e.getAmount());
}
```

KubeJS:

```js
ServerEvents.custom("nourished.food_eaten", event => {
  console.info(`${event.foodId} gave ${event.nutrientKey} +${event.amount}`)
})
```

### `NutrientModifierEvent`
- Fires: before nutrient gain is applied.
- Fields: `player`, `foodId`, `nutrientKey`, mutable `amount`.
- Cancellable: **Yes** (`ICancellableEvent`).

Java:

```java
@SubscribeEvent
public static void onModifier(NutrientModifierEvent e) {
    if (e.getNutrientKey().equals("fiber")) {
        e.setAmount(e.getAmount() * 1.2f);
    }
}
```

KubeJS:

```js
ServerEvents.custom("nourished.nutrient_modifier", event => {
  if (event.nutrientKey === "fiber") {
    event.amount = event.amount * 1.2
  }
  // event.cancelled = true // optional cancel
})
```

## Datapack Support

Base path: `data/<namespace>/nourished/`

Malformed entries:
- Logged as `WARN` with path
- Skipped
- Other entries continue loading

### 1) Nutrients
Path: `nutrients/<id>.json`

```json
{
  "display_name": "Example Nutrient",
  "color": -16711936,
  "default_decay_rate": 0.001,
  "critical_threshold": 0.1,
  "low_threshold": 0.3,
  "excess_threshold": 0.9
}
```

### 2) Food classifications
Path: `food_classifications/<id>.json`

```json
{
  "item": "minecraft:apple",
  "nutrient_key": "example_nutrient",
  "amount": 0.2
}
```

or tag-based:

```json
{
  "tag": "#forge:fruits",
  "nutrient_key": "fiber",
  "amount": 0.25
}
```

### 3) Effects
Path: `effects/<id>.json`

```json
{
  "nutrient_key": "fiber",
  "threshold": 0.8,
  "threshold_type": "EXCESS",
  "effect_id": "minecraft:speed",
  "amplifier": 0,
  "duration": 200
}
```

### 4) Synergies
Path: `synergies/<id>.json`

```json
{
  "nutrient_a_key": "fiber",
  "nutrient_a_condition": "OPTIMAL",
  "nutrient_b_key": "proteins",
  "nutrient_b_condition": "HIGH",
  "bonus_effect_id": "minecraft:regeneration",
  "amplifier": 0,
  "effect_duration": 100,
  "is_penalty": false
}
```

### 5) Food synergies
Path: `food_synergies/<id>.json`

```json
{
  "food_a": "minecraft:apple",
  "food_b": "minecraft:carrot",
  "time_window_ticks": 200,
  "bonus_nutrient_key": "fiber",
  "bonus_amount": 0.5
}
```

### 6) Milestones
Path: `milestones/<id>.json`

```json
{
  "nutrient_key": "fiber",
  "cumulative_goal": 100.0,
  "reward_effect_id": "minecraft:night_vision",
  "amplifier": 0,
  "reward_duration": 600
}
```

### 7) Diet profiles
Path: `diet_profiles/<id>.json`

```json
{
  "display_name": "Herbivore",
  "description": "Fiber-focused profile.",
  "custom_thresholds": { "fiber": 0.45 },
  "custom_decay_rates": { "fiber": 0.0009 },
  "bonus_effects": [ "minecraft:speed" ]
}
```

### 8) Compat
Path: `compat/<id>.json`

```json
{
  "mod_id": "my_food_mod",
  "category": "FOOD_MOD",
  "mappings": {
    "my_food_mod:salad": "fiber",
    "my_food_mod:protein_bowl": "proteins"
  }
}
```

## KubeJS Integration

Binding name: `NourishedAPI`

Available methods:

```js
NourishedAPI.registerNutrient({id, displayName, color, decayRate, critical, low, excess})
NourishedAPI.registerFoodClassification(itemId, nutrientKey, amount)
NourishedAPI.registerFoodSynergy(foodA, foodB, windowSeconds, nutrientKey, amount)
NourishedAPI.registerMilestone({id, nutrientKey, target, effectId, persistent})
NourishedAPI.registerDietProfile({id, displayName, thresholds, decayRates})
NourishedAPI.getNutrientLevel(player, nutrientKey)
NourishedAPI.setNutrientLevel(player, nutrientKey, value)
```

Startup script context (`kubejs/startup_scripts/`) is used for registration-style setup.
Server script context (`kubejs/server_scripts/`) is used for runtime reads/writes and event reactions.

Examples:

Register nutrient:

```js
NourishedAPI.registerNutrient({
  id: "omega3",
  displayName: "Omega-3",
  color: 0x4AA3FF,
  decayRate: 0.0012,
  critical: 0.12,
  low: 0.30,
  excess: 0.90
})
```

Register profile:

```js
NourishedAPI.registerDietProfile({
  id: "athlete",
  displayName: "Athlete",
  thresholds: { proteins: 0.65, grains: 0.50 },
  decayRates: { proteins: 0.0008, grains: 0.0010 }
})
```

Register milestone:

```js
NourishedAPI.registerMilestone({
  id: "fiber_100",
  nutrientKey: "fiber",
  target: 100.0,
  effectId: "minecraft:night_vision",
  persistent: true
})
```

Listen to events:

```js
ServerEvents.custom("nourished.nutrient_critical", event => {
  event.player.tell(`Critical nutrient: ${event.nutrientKey}`)
})
```

## JEI / REI / EMI

Tooltip integration is automatic when JEI/REI/EMI are installed.

- No user setup required.
- Food tooltip lines are generated through Nourished shared helper logic.
- Custom report sections for `/nourished report` should be added through `DietReportProvider`.

## Commands

Root: `/nourished`

- `report` — player report for self
- `report <player>` — target report (**perm 2**)
- `nutrient <key>` — nutrient detail for self
- `nutrient <key> <player>` — target nutrient detail (**perm 2**)
- `set <key> <value> <player>` — set nutrient value (**perm 2**)
- `reset <player>` — reset all nutrients to starting value (**perm 2**)
- `profile list` — list registered profiles
- `profile set <profile> [player]` — set profile for self/target (target requires **perm 2**)
- `profile get [player]` — get profile for self/target (target requires **perm 2**)
- `reload` — reload Nourished data (**perm 2**)
- `debug <player>` — print raw diet data JSON (**perm 2**)

Examples:

```text
/nourished report
/nourished nutrient fiber
/nourished set fiber 0.75 Notch
/nourished profile set herbivore
/nourished reload
```

## Versioning and Stability Guarantees

Nourished uses API status annotations to communicate backwards-compatibility guarantees:

### @ApiStatus.Stable
- Will not have breaking changes within the same minor version (1.x.y → 1.x.z)
- Breaking changes require a major version bump (1.x → 2.x) with a migration guide
- Safe to ship addons against
- Current stable surface: NourishedAPI, NutrientDefinition, EffectDefinition,
  CompatDefinition, NourishedEvents, NutrientModifierEvent, FoodMemoryView,
  NourishedPlayerData, NourishedAPIVersion

### @ApiStatus.Experimental
- May change in any release without a major version bump
- Usable but expect migration work between versions
- Current experimental surface: DietProfileDefinition, FoodSynergyDefinition,
  NutrientMilestoneDefinition, NutrientSynergyDefinition, NourishedSeasonHook,
  NutrientAbsorptionModifier, NutrientRenderer, DietReportProvider

### @ApiStatus.Internal
- Not part of the public contract
- Will change without notice in any release
- Do not reference in addons — not even via reflection

For addon authors:
- Depend only on @Stable APIs in released addons
- Use @Experimental with the understanding you may need to update on any release
- Never reference @Internal — if you need something that is currently Internal,
  open an issue so it can be promoted with a proper stability guarantee

## Example Addon

Reference implementation: [`example-addon/`](example-addon/)

What each class demonstrates:
- `NourishedExampleAddon`: guarded API bootstrap
- `ExampleNutrientRegistration`: custom nutrient registration
- `ExampleFoodClassification`: item->nutrient mappings
- `ExampleEffectRegistration`: threshold effects
- `ExampleSynergyRegistration`: nutrient + food synergies
- `ExampleMilestoneRegistration`: cumulative milestone
- `ExampleDietProfile`: profile registration
- `ExampleCompatEntry`: compat metadata
- `ExampleEventListener`: runtime event subscriptions
- `ExampleReportProvider`: report section injection

