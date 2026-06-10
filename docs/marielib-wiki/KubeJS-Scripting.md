# KubeJS Scripting

MarieLib has full KubeJS support. You can read and modify player values, register custom values and profiles, and react to tracking events — all from `.js` scripts.

KubeJS must be installed. MarieLib's KubeJS integration loads automatically when both mods are present.

---

## 📁 Script Locations

| Script type | Folder |
|---|---|
| Reading/modifying player data, events | `kubejs/server_scripts/` |
| Registering values, profiles, milestones | `kubejs/startup_scripts/` |

---

## 📊 Reading Player Values

Use the `MarieAPI` binding in a server script.

```javascript
// server_scripts/value_check.js

MarieEvents.valueChanged(event => {
    const level = MarieAPI.getValueLevel(event.player, "proteins")
    console.log(`${event.player.name}'s proteins: ${level}`)
})
```

### MarieAPI.getValueLevel(player, valueKey)

Returns the player's current level for a value as a float between `0.0` and `1.0`. Returns `-1.0` if the value key doesn't exist.

```javascript
const proteins = MarieAPI.getValueLevel(player, "proteins")
const grains   = MarieAPI.getValueLevel(player, "grains")
```

---

## ✏️ Modifying Player Values

### MarieAPI.setValueLevel(player, valueKey, value)

Directly sets a value to an exact level. Clamped to `0.0`–`1.0`. Does not fire a modifier event.

```javascript
MarieAPI.setValueLevel(player, "proteins", 0.5)
```

For gameplay mechanics that should respect other mods, prefer listening to `valueModifier` instead.

---

## 📡 Server Events

All events run server-side. Subscribe in `server_scripts/`.

### MarieEvents.valueChanged

Fires whenever any value changes.

```javascript
MarieEvents.valueChanged(event => {
    // event.player     — the ServerPlayer
    // event.valueKey   — which value changed
    // event.oldValue   — previous value (0.0–1.0)
    // event.newValue   — new value (0.0–1.0)

    if (event.valueKey === "proteins" && event.newValue >= 0.9) {
        event.player.tell("Your protein intake is excellent!")
    }
})
```

### MarieEvents.valueCritical

Fires once when a value crosses below its critical threshold.

```javascript
MarieEvents.valueCritical(event => {
    event.player.tell(`Warning: your ${event.valueKey} is critically low!`)
})
```

### MarieEvents.sourceApplied

Fires after a player applies a source item and gains value.

```javascript
MarieEvents.sourceApplied(event => {
    // event.player    — the ServerPlayer
    // event.sourceId  — item ID string (e.g. "minecraft:carrot")
    // event.valueKey  — value gained
    // event.amount    — how much was gained

    console.log(`${event.player.name} applied ${event.sourceId} (+${event.amount} ${event.valueKey})`)
})
```

### MarieEvents.valueModifier ⚡ Cancellable

Fires before a value gain is applied. Modify the amount or cancel it entirely.

```javascript
MarieEvents.valueModifier(event => {
    // event.player    — the ServerPlayer
    // event.sourceId  — item ID string
    // event.valueKey  — value being applied
    // event.amount    — gain amount (read/write)
    // event.cancelled — set to true to block the gain

    if (event.valueKey === "vegetables") {
        event.amount *= 2
    }

    if (event.sourceId === "mymod:mystery_meat") {
        event.cancelled = true
    }
})
```

---

## 🚀 Startup Registration

Registration happens in `startup_scripts/`. Event names use the **consuming mod's id** as prefix.

For Nourished, events are `nourished.startup.register_values`, `nourished.startup.register_profiles`, and `nourished.startup.register_milestones`. For a custom Marie mod, substitute your mod id.

### Register a value

```javascript
// startup_scripts/values.js

StartupEvents.registry('nourished.startup.register_values', event => {
    event.registerValue({
        id: "spices",
        displayName: "Spices",
        color: 0xFFE8A020,
        decayRate: 0.0005,
        low: 0.25,
        excess: 0.9
    })
})
```

### Register a source classification

```javascript
StartupEvents.registry('nourished.startup.register_values', event => {
    event.registerSourceClassification("mymod:spicy_pepper", "spices", 0.08)
})
```

### Register a tracking profile

```javascript
StartupEvents.registry('nourished.startup.register_profiles', event => {
    event.registerTrackingProfile({
        id: "hardcore",
        displayName: "Hardcore",
        decayRates: {
            proteins: 0.002,
            grains: 0.002
        },
        thresholds: {
            proteins: 0.4
        }
    })
})
```

### Register a source pair synergy

```javascript
StartupEvents.registry('nourished.startup.register_profiles', event => {
    event.registerSourcePairSynergy("minecraft:bread", "minecraft:cheese", 30, "dairy", 0.1)
})
```

### Register a milestone

```javascript
StartupEvents.registry('nourished.startup.register_milestones', event => {
    event.registerMilestone({
        id: "protein_master",
        valueKey: "proteins",
        target: 100.0,
        effectId: "minecraft:strength"
    })
})
```

---

## 💡 Tips

- Use `valueModifier` for dynamic rules — anything you can check about a player can influence value gains.
- `setValueLevel` is direct and bypasses the modifier event. Good for admin tools or death penalties.
- Registration is startup-only. You cannot register new values from server scripts.

---

## 📚 Related Pages

- [API Reference](API-Reference)
- [Datapack Support](Datapack-Support)
- [Getting Started](Getting-Started)
