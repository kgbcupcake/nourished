# API Reference

MarieLib exposes a public Java API for mod authors who want to read player tracking state, register custom values or effects, or hook into the value system programmatically.

> This page is for **mod developers**. If you want datapack-only integration, see [Datapack Support](Datapack-Support).

---

## 🔗 Depending on MarieLib

Add MarieLib as a dependency in your `build.gradle`:

```groovy
dependencies {
    compileOnly "dev.marie.MariesLib:marieslib:1.0.0"
}
```

Declare the dependency in your `mods.toml`:

```toml
[[dependencies.yourmod]]
    modId = "marieslib"
    type = "required"
    versionRange = "[1.0.0,)"
    ordering = "AFTER"
    side = "BOTH"
```

All public API is under `dev.marie.MariesLib.api`. **Never reference anything outside that package** — internal classes are not part of the API contract.

Wire your mod through `MarieLibContext` at bootstrap. See [Getting Started](Getting-Started).

---

## 📊 Reading Player State

All player queries go through `MarieAPI`.

### Get a single value level

```java
float proteins = MarieAPI.getValueLevel(player, "proteins");
// Returns 0.0–1.0, or -1.0 if the value key doesn't exist
```

### Get all values at once

```java
MariePlayerData data = MarieAPI.getTrackingData(player);
float proteins = data.values().get("proteins");
float total = data.total();
MemoryView memory = data.sourceMemory();
```

`MariePlayerData` is an immutable snapshot — safe to store and pass around.

### Get total count

```java
float total = MarieAPI.getTotal(player);
// Alias: MarieAPI.getTotalCount(player)
```

### Check recent source history

```java
MemoryView memory = MarieAPI.getSourceMemory(player);
boolean ateCarrots = memory.hasSourceRecently(ResourceLocation.parse("minecraft:carrot"));
List<ResourceLocation> recent = memory.getRecentSources();
```

---

## ✏️ Modifying Values

```java
// Add 10% to a player's protein bar
MarieAPI.modifyValue(player, "proteins", 0.10f);

// Remove 5% from a player's grain bar
MarieAPI.modifyValue(player, "grains", -0.05f);
```

`modifyValue` fires a `ValueModifierEvent` before applying. Other mods can intercept and modify or cancel the change.

Must be called **server-side** for changes to sync to clients correctly.

---

## 📋 Registration

All registration must happen during mod initialization before the server starts. Calling registration methods after init throws `IllegalStateException`.

### Register a custom value

```java
ValueDefinition spices = ValueDefinition.builder("spices")
    .displayName("Spices")
    .color(0xFFE8A020)
    .defaultDecayRate(0.0005f)
    .lowThreshold(0.25f)
    .excessThreshold(0.9f)
    .build();

MarieAPI.registerValue(spices);
```

### Register a source classification

```java
MarieAPI.registerSourceClassification(
    ResourceLocation.parse("mymod:spicy_pepper"),
    "spices",
    0.08f
);
```

### Register a custom effect

```java
ThresholdEffect effect = ThresholdEffect.builder()
    .valueKey("spices")
    .threshold(0.2f)
    .thresholdType(ThresholdEffect.ThresholdType.LOW)
    .effectId(ResourceLocation.parse("minecraft:weakness"))
    .amplifier(0)
    .duration(300)
    .build();

MarieAPI.registerCustomEffect(effect);
```

`ThresholdType` options:

- `CRITICAL` / `LOW` — penalty fires when value is below threshold
- `EXCESS` / `BONUS` — buff fires when value is above threshold

### Register a compat entry

```java
CompatDefinition compat = CompatDefinition.builder("mymod")
    .category(CompatDefinition.CompatCategory.SOURCE_MOD)
    .addSourceMapping(ResourceLocation.parse("mymod:spicy_stew"), "spices")
    .addSourceMapping(ResourceLocation.parse("mymod:herb_salad"), "vegetables")
    .build();

MarieAPI.registerCompatEntry(compat);
```

`CompatDefinition` lives in `dev.marie.MariesLib.compat.CompatDefinition`.

### Register a tracking profile

```java
ProfileDefinition profile = ProfileDefinition.builder("hardcore")
    .displayName("Hardcore")
    .description("Values decay faster.")
    .customDecayRate("proteins", 0.002f)
    .customDecayRate("grains", 0.002f)
    .build();

MarieAPI.registerTrackingProfile(profile);
```

---

## 📡 Events

Subscribe to MarieLib events on the NeoForge event bus.

### ValueChangedEvent

Fires after a value changes. Not cancellable.

```java
@SubscribeEvent
public void onValueChanged(MarieEvents.ValueChangedEvent event) {
    Player player = event.getPlayer();
    String key = event.getValueKey();
    float oldVal = event.getOldValue();
    float newVal = event.getNewValue();
}
```

### ValueCriticalEvent

Fires once when a value crosses below the critical threshold. Not cancellable.

### ValueExcessEvent

Fires once when a value crosses above the excess threshold. Not cancellable.

### SourceAppliedEvent

Fires after a player applies a source item and gains value.

```java
@SubscribeEvent
public void onSourceApplied(MarieEvents.SourceAppliedEvent event) {
    ResourceLocation source = event.getSourceId();
    String valueKey = event.getValueKey();
    float gained = event.getAmount();
}
```

### ValueModifierEvent ⚡ Cancellable

Fires before a value gain is applied. Modify the amount or cancel entirely.

```java
@SubscribeEvent
public void onValueGain(ValueModifierEvent event) {
    if (event.getValueKey().equals("proteins") && playerHasDebuff(event.getPlayer())) {
        event.setAmount(event.getAmount() * 0.5f);
    }

    if (event.getSourceId().equals(ResourceLocation.parse("mymod:poison_apple"))) {
        event.setCanceled(true);
    }
}
```

---

## 🔧 Hooks & Modifiers

### AbsorptionModifier @Experimental

Dynamically adjusts how much of a value a player absorbs based on their state.

```java
public class IllnessAbsorptionModifier implements AbsorptionModifier {
    @Override
    public String getModifierId() { return "mymod:illness_penalty"; }

    @Override
    public float getAbsorptionMultiplier(Player player, String valueKey, float baseAmount) {
        if (player.hasEffect(MobEffects.NAUSEA)) return 0.5f;
        return 1.0f;
    }
}

MarieAPI.registerAbsorptionModifier(new IllnessAbsorptionModifier());
```

Return values: `1.0` = no change, `0.5` = half absorption, `0.0` = blocked entirely.

### MarieSeasonHook @Experimental

Integrates with season mods to vary decay and absorption by season.

### ReportProvider @Experimental

Injects custom sections into the consuming mod's `/report` command output.

---

## 🏷️ API Stability

| Annotation | Meaning |
|---|---|
| `@ApiStatus.Stable` | Safe to use — breaking changes require a major version bump |
| `@ApiStatus.Experimental` | May change without warning |
| `@ApiStatus.Internal` | Not for external use |

Everything in `MarieAPI` is `@Stable` unless otherwise annotated.

---

## ⚠️ What's Not Wired Yet

These registration methods exist but underlying systems may not be fully active in all consuming mods:

- `registerValueSynergy` — two-value interaction effects
- `registerSourcePairSynergy` — source combo bonuses
- `registerMilestone` — cumulative value goals

---

## 📚 Related Pages

- [KubeJS Scripting](KubeJS-Scripting)
- [Datapack Support](Datapack-Support)
- [Compatibility Guide](Compatibility-Guide)
- [Getting Started](Getting-Started)
