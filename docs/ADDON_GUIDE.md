# Making a Nourished Addon

This guide shows how to add your own content to Nourished from another mod: new nutrients, food
mappings, effects, synergies, profiles and milestones. It walks through a complete example addon, then
covers each registration type on its own.

For the full method list and stability rules, see [API.md](API.md). This guide is the practical
companion to it.

## Contents

1. [What you can add](#what-you-can-add)
2. [Project setup](#project-setup)
3. [A complete example addon](#a-complete-example-addon)
4. [How registration works](#how-registration-works)
5. [Registration reference](#registration-reference)
6. [Reading and changing player nutrition](#reading-and-changing-player-nutrition)
7. [Reacting to nutrition events](#reacting-to-nutrition-events)
8. [What players can customize for free](#what-players-can-customize-for-free)
9. [Testing and debugging](#testing-and-debugging)
10. [Common mistakes](#common-mistakes)

---

## What you can add

| You want to…                                         | Register a…           | Event method              |
|------------------------------------------------------|-----------------------|---------------------------|
| Track a brand-new nutrient (e.g. Omega-3)            | `ValueDefinition`     | `registerNutrient`        |
| Say "this item gives this nutrient"                  | food classification   | `registerFood`            |
| Map many items from your mod at once                 | `CompatDefinition`    | `registerCompat`          |
| Give a potion effect when a nutrient is low/high     | `ThresholdEffect`     | `registerEffect`          |
| Reward two nutrients being in a certain state        | `SynergyDefinition`   | `registerNutrientSynergy` |
| Reward eating two foods close together (meal combo)  | `SourcePairSynergy`   | `registerFoodSynergy`     |
| Add a selectable diet profile (e.g. "Athlete")       | `ProfileDefinition`   | `registerProfile`         |
| Reward reaching a total intake goal                  | `MilestoneDefinition` | `registerMilestone`       |

All of these definition types come from MariesLib (`dev.marie.framework.*`). The event itself is
Nourished's `dev.maire.nourished.api.event.NourishedRegisterEvent`.

---

## Project setup

### Gradle

Compile against Nourished and MariesLib, but don't bundle either one into your jar:

```gradle
dependencies {
    compileOnly "dev.maire.nourished:nourished:<nourished-version>"
    compileOnly "dev.marie.MariesLib:marieslib:<marieslib-version>"
}
```

For testing in your dev client, add both jars to your run classpath as well (for example through
`localRuntime`, or by dropping them into `run/mods`).

### neoforge.mods.toml

If your mod **requires** Nourished:

```toml
[[dependencies.yourmodid]]
    modId = "nourished"
    type = "required"
    versionRange = "[0.2.7,)"
    ordering = "NONE"
    side = "BOTH"
```

If Nourished is **optional**, meaning your mod still works without it, use `type = "optional"`. Then
follow the isolation rule in the next section.

### Keep Nourished code in its own class

If Nourished is optional, **never reference Nourished or MariesLib classes from your main mod class.**
Put every Nourished touchpoint in a separate class and only load that class when Nourished is present.
Otherwise your mod crashes with `ClassNotFoundException` for anyone who doesn't have Nourished installed.

```java
// Your main mod class: no Nourished imports here.
@Mod("omegamod")
public class OmegaMod {
    public OmegaMod(IEventBus modEventBus) {
        if (ModList.get().isLoaded("nourished")) {
            OmegaNourishedCompat.init(modEventBus);
        }
    }
}
```

---

## A complete example addon

This addon adds an **Omega-3** nutrient, feeds it from fish, gives Weakness when it runs low, and adds
a salmon-and-kelp meal combo.

```java
package com.example.omegamod.compat;

import dev.maire.nourished.api.event.NourishedRegisterEvent;
import dev.marie.framework.api.effects.ThresholdEffect;
import dev.marie.framework.api.source.SourcePairSynergy;
import dev.marie.framework.api.value.ValueDefinition;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;

public final class OmegaNourishedCompat {

    private OmegaNourishedCompat() {}

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(NourishedRegisterEvent.class, OmegaNourishedCompat::register);
    }

    private static void register(NourishedRegisterEvent event) {
        // 1. The nutrient itself
        event.registerNutrient(ValueDefinition.builder("omega3")
                .displayName("Omega-3")
                .color(0xFF3FA7D6)            // ARGB: bar and tooltip color
                .icon("minecraft:cooked_salmon")
                .beneficial(true)
                .criticalThreshold(0.10f)     // below 10% = critical
                .lowThreshold(0.30f)          // below 30% = low
                .excessThreshold(0.90f)       // above 90% = excess
                .defaultDecayRate(0.001f)
                .tags(java.util.List.of("c:foods/raw_fish", "c:foods/cooked_fish"))
                .build());

        // 2. Specific foods that give it
        event.registerFood(mc("cooked_salmon"), "omega3", 1.0f);
        event.registerFood(mc("cooked_cod"), "omega3", 0.6f);

        // 3. An effect when it runs low
        event.registerEffect(ThresholdEffect.builder()
                .valueKey("omega3")
                .thresholdType(ThresholdEffect.ThresholdType.LOW)
                .threshold(0.30f)
                .effectId(mc("weakness"))
                .amplifier(0)
                .duration(200)                // ticks
                .build());

        // 4. A meal combo: salmon + dried kelp within 60 seconds gives bonus omega3
        event.registerFoodSynergy(SourcePairSynergy.builder("omegamod:surf_and_turf")
                .sourceA(mc("cooked_salmon"))
                .sourceB(mc("dried_kelp"))
                .timeWindowTicks(1200)
                .bonusValueKey("omega3")
                .bonusAmount(0.1f)
                .build());
    }

    private static ResourceLocation mc(String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }
}
```

After launching, Omega-3 will:

- show on the Nutrient HUD and get its own Intake Breakdown row on the Diet Screen
- appear in item tooltips for the mapped foods
- go up when you eat those foods and trigger Weakness when it drops below 30%
- be fully customizable by players (see [below](#what-players-can-customize-for-free))

---

## How registration works

`NourishedRegisterEvent` fires **once**, on each mod's event bus, during Nourished's common setup. It
runs in two phases.

**1. Collect.** Everything you register goes into a temporary batch. Nothing is applied yet.

**2. Validate and apply.** After every mod's listener has run, Nourished checks the whole batch, then
applies what passed in this order:

```
nutrients → foods → compat → effects → nutrient synergies → food synergies → profiles → milestones
```

This means **the order listeners run in doesn't matter.** Your mod can map food to a nutrient that a
different addon registers, even if that addon's listener runs after yours. Nourished checks the food
mapping only after it has collected every nutrient.

### Validation rules

| Situation                                                    | Result                                          |
|--------------------------------------------------------------|-------------------------------------------------|
| Two mods register the same nutrient key                      | First mod in load order wins; the other is rejected |
| A nutrient key that already exists (e.g. `proteins`)         | Rejected                                        |
| Food/effect/synergy/milestone/profile uses an unknown nutrient | That entry is rejected                        |
| Two synergies/profiles/milestones with the same id in the batch | The second is rejected                       |
| Food amount is `NaN` or infinite                             | Rejected                                        |

A rejected entry never takes the rest down with it. Every other entry still applies. Each rejection
is logged at `ERROR` level with the id of the mod that submitted it:

```
[Nourished] NourishedRegisterEvent rejected 1 registration(s):
[Nourished]   [omegamod] food minecraft:bread -> omega33: unknown nutrient [omega33]
```

### Timing rules

- Register **inside the listener**. If you keep a reference to the event and call it later, you get
  an `IllegalStateException`.
- Calling `NourishedAPI.register*` from inside the listener works exactly like the event's own
  methods. The call goes into the same batch.
- After setup finishes, the registration window closes and any registration attempt throws.

### Direct registration (older style)

You can still call `NourishedAPI.registerValue(...)` and the other `register*` methods directly in your
mod constructor. Those apply immediately and are checked on the spot, so you have to register a
nutrient **before** anything that uses it. Cross-mod ordering then depends on NeoForge's load order.
Prefer the event.

---

## Registration reference

Each example below is written as the body of your event listener.

### Nutrient: `registerNutrient(ValueDefinition)`

```java
event.registerNutrient(ValueDefinition.builder("omega3")   // unique key, lowercase
        .displayName("Omega-3")
        .color(0xFF3FA7D6)                // ARGB
        .tooltipColor(0xFF3FA7D6)         // optional, ARGB
        .icon("minecraft:cooked_salmon")  // item id shown next to the bar
        .beneficial(true)                 // false = something you want to keep low
        .criticalThreshold(0.10f)
        .lowThreshold(0.30f)
        .excessThreshold(0.90f)
        .defaultDecayRate(0.001f)         // per decay interval
        .tags(List.of("c:foods/cooked_fish")) // item tags that count toward it (no '#')
        .build());
```

- The key is permanent. Save data and config files use it, so don't rename it later.
- `tags` is how Nourished *infers* this nutrient for every item in those tags. Use `registerFood` for
  exact per-item values.
- Thresholds are fractions from `0.0` to `1.0`.

### Food: `registerFood(ResourceLocation item, String nutrientKey, float amount)`

```java
event.registerFood(ResourceLocation.fromNamespaceAndPath("omegamod", "fish_oil"), "omega3", 1.0f);
```

- The amount uses the same scale as tag weights. `1.0` is a standard full contribution.
- **An explicit mapping is authoritative.** For that item it *replaces* tag/scanner inference; it
  doesn't add to it. Register every nutrient you want the item to give, not just the new one.
- An item id that doesn't exist yet only logs a warning, so mapping items from mods that load later is
  fine.

### Many foods at once: `registerCompat(CompatDefinition)`

```java
event.registerCompat(CompatDefinition.builder("omegamod")
        .category(CompatDefinition.CompatCategory.SOURCE_MOD)   // or FARMING_MOD, SURVIVAL_OVERHAUL
        .addSourceMapping(ResourceLocation.fromNamespaceAndPath("omegamod", "fish_oil"), "omega3")
        .addSourceMapping(ResourceLocation.fromNamespaceAndPath("omegamod", "sardine"), "proteins")
        .build());
```

Each mapping must point at a known nutrient, otherwise the whole compat entry is rejected.

### Threshold effect: `registerEffect(ThresholdEffect)`

```java
event.registerEffect(ThresholdEffect.builder()
        .valueKey("omega3")
        .thresholdType(ThresholdEffect.ThresholdType.EXCESS)   // CRITICAL, LOW, EXCESS, BONUS
        .threshold(0.90f)
        .effectId(ResourceLocation.withDefaultNamespace("nausea"))
        .amplifier(0)
        .duration(100)   // ticks
        .build());
```

Every type compares against the `threshold` **you pass**, not the nutrient's own critical/low/excess
levels. The type sets the direction and how the effect is labeled:

| Type       | Effect is active while the nutrient is… |
|------------|-----------------------------------------|
| `CRITICAL` | below `threshold`                       |
| `LOW`      | below `threshold`                       |
| `EXCESS`   | above `threshold`                       |
| `BONUS`    | above `threshold` (a reward for keeping it stocked) |

The effect is kept on while the condition holds and removed once it stops holding.

### Nutrient synergy: `registerNutrientSynergy(SynergyDefinition)`

Fires when two nutrients are in given states at the same time.

```java
event.registerNutrientSynergy(SynergyDefinition.builder("omegamod:brain_food")
        .valueA("omega3", SynergyDefinition.LevelCondition.OPTIMAL)   // HIGH, LOW, OPTIMAL
        .valueB("proteins", SynergyDefinition.LevelCondition.OPTIMAL)
        .bonusEffect(ResourceLocation.withDefaultNamespace("night_vision"))
        .effectAmplifier(0)
        .effectDuration(400)
        .penalty(false)            // true = treat it as a negative combo
        .build());
```

### Food synergy (meal combo): `registerFoodSynergy(SourcePairSynergy)`

```java
event.registerFoodSynergy(SourcePairSynergy.builder("omegamod:fish_and_chips")
        .sourceA(ResourceLocation.withDefaultNamespace("cooked_cod"))
        .sourceB(ResourceLocation.withDefaultNamespace("baked_potato"))
        .timeWindowTicks(1200)          // both eaten within 60 s
        .bonusValueKey("omega3")
        .bonusAmount(0.1f)
        .build());
```

### Diet profile: `registerProfile(ProfileDefinition)`

```java
event.registerProfile(ProfileDefinition.builder("omegamod:pescatarian")
        .displayName("Pescatarian")
        .description("Relies on fish instead of meat.")
        .customThreshold("omega3", 0.40f)
        .customDecayRate("proteins", 0.0005f)
        .addBonusEffect(ResourceLocation.withDefaultNamespace("water_breathing"))
        .build());
```

### Milestone: `registerMilestone(MilestoneDefinition)`

```java
event.registerMilestone(MilestoneDefinition.builder("omegamod:fish_lover")
        .valueKey("omega3")
        .cumulativeGoal(50f)                                      // total intake over time
        .rewardEffect(ResourceLocation.withDefaultNamespace("luck"))
        .rewardAmplifier(0)
        .rewardDuration(6000)
        .advancement(ResourceLocation.fromNamespaceAndPath("omegamod", "fish_lover")) // optional
        .build());
```

### Hooks that aren't part of the event

These don't reference nutrients, so they're applied straight away. Call them from `NourishedAPI`
during mod setup:

```java
NourishedAPI.registerSeasonHook(mySeasonHook);            // MarieSeasonHook
NourishedAPI.registerAbsorptionModifier(myModifier);      // AbsorptionModifier
NourishedAPI.registerReportProvider(myReportSection);     // ReportProvider
```

---

## Reading and changing player nutrition

These work any time. They don't depend on the registration window.

```java
float omega = NourishedAPI.getValueLevel(player, "omega3");   // 0.0–1.0, or -1 if unknown key
float calories = NourishedAPI.getTotal(player);
var snapshot = NourishedAPI.getTrackingData(player);          // calories + every nutrient + food memory

// Give or take a nutrient directly (fires ValueModifierEvent first, so other mods can adjust or cancel it)
NourishedAPI.modifyValue(player, "omega3", 0.05f);
```

Call `modifyValue` on the server. Nourished syncs the change to the client and re-checks effects for you.

---

## Reacting to nutrition events

Listen on the NeoForge game bus (`NeoForge.EVENT_BUS`) for MariesLib's events in
`dev.marie.framework.api.marie.MarieEvents`:

```java
NeoForge.EVENT_BUS.addListener(MarieEvents.ValueCriticalEvent.class, e -> {
    if ("omega3".equals(e.getValueKey())) {
        e.getPlayer().displayClientMessage(Component.literal("You need more fish!"), true);
    }
});
```

| Event                     | Useful getters                                            |
|---------------------------|-----------------------------------------------------------|
| `ValueChangedEvent`       | `getPlayer`, `getValueKey`, `getOldValue`, `getNewValue`  |
| `ValueCriticalEvent`      | `getPlayer`, `getValueKey`                                |
| `ValueExcessEvent`        | `getPlayer`, `getValueKey`                                |
| `SourceAppliedEvent`      | `getPlayer`, `getSourceId`, `getValueKey`, `getAmount`    |
| `SourceTriggerEvent`      | `getPlayer`, `getTrigger` (cancellable)                   |
| `MilestoneTriggeredEvent` | `getPlayer`, `getMilestone`, `getValueKey`                |

---

## What players can customize for free

You don't write any UI code. Once your nutrient is registered, Nourished's editors pick it up
automatically:

| Where                                | What players can change for your nutrient                         |
|--------------------------------------|-------------------------------------------------------------------|
| Nutrient HUD (edit mode, Colors/Glow) | bar color, per-nutrient bar glow                                  |
| Diet Screen → Intake Breakdown       | its own row: move/resize, bar fill, text, track, border, icon border |
| Config → Nutrients                   | decay rate override, critical threshold override, curve preset   |
| Config → Effects                     | create/edit threshold effects that use your nutrient              |

Your `ValueDefinition` supplies the **defaults**, and player changes are saved on top of them. Decay
and threshold overrides are stored in `config/nourished/nutrient_overrides.json`, so they work for
addon nutrients the same way they work for built-in ones.

If a player has rearranged their Intake Breakdown rows, your new row is never placed on top of one of
them. If its normal spot is taken, it goes below the lowest row.

---

## Testing and debugging

Open the **Command Center** in game and go to the **Nourished Test** tab (needs cheats/operator):

- **Registration Report** (`/nourished debug registrations`) lists everything applied and rejected
  through the event on this launch, each entry tagged with the mod that submitted it.
- **Check Held Food** (`/nourished debug food`), with an item in your main hand, shows:
  - `Explicit (API) mapping`: what `registerFood`/compat set for this item
  - `Tag inference`: what its tags alone would give
  - `Eating gives`: what's actually applied when it's eaten

You can also search `latest.log` for `NourishedRegisterEvent`. You'll see one summary line plus one
line per rejected entry.

A quick checklist after adding a nutrient:

1. The Registration Report shows it under **Applied**.
2. HUD edit mode (H) shows its bar.
3. The Diet Screen has its Intake Breakdown row.
4. Check Held Food on one of your mapped foods shows it under "Eating gives".

---

## Common mistakes

**"unknown nutrient" rejections.** Usually a typo in the key, or your nutrient was itself rejected
(check for a duplicate key). Keys are case-sensitive.

**My food lost its old nutrients.** An explicit mapping replaces inference for that item. Map every
nutrient you want it to give.

**`ClassNotFoundException` without Nourished installed.** A Nourished class is referenced from code
that loads even when Nourished is absent. Move it into the separate compat class and guard it with
`ModList.get().isLoaded("nourished")`.

**`IllegalStateException: NourishedRegisterEvent has finished`.** You called the event (or kept a
reference to it) after your listener returned. Do all registration inside the listener.

**My nutrient has no section in `nourished-common.toml`.** Expected. Per-nutrient settings for every
nutrient live in `nutrient_overrides.json` and the in-game config screen, not the TOML file.

**My nutrient's bar is missing from the HUD.** Check the player's HUD visibility settings ("Hide above
threshold", hide empty bars). HUD edit mode always shows every bar, so it's a quick way to tell whether
the nutrient exists.
