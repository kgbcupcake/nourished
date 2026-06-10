# Compatibility Guide

MarieLib is built to work with any mod that adds edible items automatically, without per-item configuration. This page covers how classification and compatibility discovery work, and how to add compat for your own mod.

---

## 🤖 Automatic Classification

When a consuming mod loads, MarieLib scans every installed source item and classifies it using a multi-stage pipeline:

1. **Value tags** — checks for registered value tags on items
2. **Community tags** — checks for standard `c:foods/*` tags
3. **Keyword matching** — analyzes item names for food-related terms
4. **Recipe inheritance** — looks at what ingredients go into a recipe
5. **Namespace grouping** — clusters items from the same mod together
6. **Fallback** — unrecognizable items are left unclassified

See [Classification Pipeline](Classification-Pipeline) for the full trace output.

The more tags a mod's items have (especially `c:foods/*` tags), the better MarieLib classifies them.

---

## 🔧 Three-Tier Compat System

MarieLib uses a layered compat registry so mod authors, addon authors, and modpack creators can declare compatibility without recompiling:

| Tier | Source | Notes |
|---|---|---|
| 1 | `data/<modId>/compat/compat_registry.json` in the consuming mod | Base registry |
| 2 | `data/<otherModId>/marie_compat.json` from loaded mods | Mod-provided declarations |
| 3 | `config/<modId>/compat_overrides.json` | Modpack overrides |

Later tiers merge into earlier entries rather than replacing them wholesale.

**Example compat entry:**

```json
{
  "modId": "farmersdelight",
  "category": "FOOD_MOD",
  "providesSourceTags": true,
  "namespaces": ["farmersdelight"]
}
```

| Field | Description |
|---|---|
| `modId` | The mod being registered |
| `category` | `FOOD_MOD`, `FARMING_MOD`, or `SURVIVAL_OVERHAUL` |
| `namespaces` | Item namespaces MarieLib should attribute to this mod |
| `providesSourceTags` | `true` if items use `c:foods/*` or value tags |

---

## 🔌 Adding Compat for Your Mod

### Option 1 — Ship a compat file with your mod

Create `data/<yourmod>/marie_compat.json` in your mod's resources:

```json
{
  "entries": [
    {
      "modId": "yourmod",
      "displayName": "Your Mod",
      "category": "FOOD_MOD",
      "namespaces": ["yourmod"],
      "providesSourceTags": true
    }
  ]
}
```

### Option 2 — Datapack / config override

Modpack authors can drop `config/<modId>/compat_overrides.json` to override any compat entry.

### Option 3 — Java API

```java
CompatDefinition compat = CompatDefinition.builder("yourmod")
    .category(CompatDefinition.CompatCategory.SOURCE_MOD)
    .addSourceMapping(ResourceLocation.parse("yourmod:special_fruit"), "fruits")
    .build();

MarieAPI.registerCompatEntry(compat);
```

See [API Reference](API-Reference) for details.

---

## 📖 Recipe Viewers — JEI / REI / EMI

MarieLib injects value bar tooltips into JEI, REI, and EMI item views. Hover over any classified source item to see which value key it belongs to.

Activates automatically when any of these mods are installed.

---

## 🟨 KubeJS

MarieLib includes KubeJS integration for scripting value events and registering content from scripts. See [KubeJS Scripting](KubeJS-Scripting).

---

## ⚠️ Survival Overhaul Mods

Mods categorized as `SURVIVAL_OVERHAUL` (Legendary Survival Overhaul, TerraFirmaCraft, etc.) often manage their own hunger and health systems. MarieLib detects these and consuming mods may disable certain effects or decay modules to avoid conflicts.

---

## 📚 Related Pages

- [Classification Pipeline](Classification-Pipeline)
- [Community Tags](Community-Tags)
- [API Reference](API-Reference)
- [Datapack Support](Datapack-Support)
