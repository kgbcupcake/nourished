# Nourished

Minecraft already nags you about hunger, but it never asks whether you ate anything *useful*. Nourished adds a lightweight diet layer on top: you still fill the bar, but now fruits, veggies, protein, and the rest each matter in their own lane. Watch the bars move, chase a decent balance score, and actually feel like your pantry choices mean something.

**Nourished** is a NeoForge **1.21.1** mod that tracks six food groups and shows them in a simple in-game diet screen.

## Features

- **Six food group bars** — each meal nudges separate tracks for:
  - fruits
  - vegetables
  - proteins
  - grains
  - sugars
  - dairy
- **Diet screen** — open from your inventory; segmented bars, trend arrows, and color-coded status so you can see what you are neglecting without digging through numbers.
- **Calorie tracking** — daily total with a soft cap so “eat everything” is not automatically optimal.
- **Balance score** — rewards variety, not just volume.
- **Passive effects** — run low in a group and you pick up debuffs; stay fed and balanced for regeneration and health boost.
- **Nutrient decay** — values ease down over time so the system stays relevant instead of one feast and forget.
- **Saves to disk** — player data persists via codec serialization.
- **Mod-friendly detection** — tag-based matching; works well with common kitchen mods out of the box (see compatibility below).

## How it works

When you eat, the mod walks a priority list of item tags and maps the item to one or more food groups. Vanilla items are registered explicitly. Modded foods usually land via shared tags like `c:foods/fruits`, `farmersdelight:vegetables`, and `pamhc2food:proteinitem`. If nothing lines up, a generic fallback still counts the meal so nothing silently does nothing.

Nutrients live as normalized values between 0 and 1. The diet UI reads them from the client attachment, which the server syncs on login and after meals.

## Effects

Rough guide to what the numbers do to your character:

| Condition                   | Effect         |
| --------------------------- | -------------- |
| Protein < 25%               | Mining Fatigue |
| Carbs < 25%                 | Weakness       |
| Vitamins < 25%              | Bad Luck       |
| Hydration < 25%             | Slowness       |
| All macros > 75% + balanced | Regeneration   |
| All macros > 75%            | Health Boost   |

## Compatibility

| Mod                            | Status |
| ------------------------------ | ------ |
| Farmer's Delight               | Full tag support |
| Pam's HarvestCraft 2           | Full tag support |
| Legendary Survival Overhaul    | ⚠️ Effects disabled — LSO takes priority |
| Any mod using `c:foods/*` tags | Automatic where tags exist |
| Croptopia                      | Partial / in progress — broader item coverage planned |
| Mama's Herbs and Harvest       | Partial / in progress — broader item coverage planned |
| Other food mods                | Generic fallback when tags do not match |

## Screenshots

Placeholder for now. When you have captures, drop them here (for example under `docs/screenshots/`) and wire them up like:

| Main Gui                                                 | Hud-Gui                                                |
|:---------------------------------------------------------:|:-------------------------------------------------------:|
| ![Main Gui](Assets/MainGui.png) | ![Hud-Gui](Assets/HubGui.png) |

```markdown
![Diet screen overview](docs/screenshots/diet-overview.png)
```

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.x
- Java 21 (Gradle toolchain handles it for builds)

## Building

```bash
./gradlew build
```

The built jar lands in `build/libs/` as `nourished-<version>.jar` (see `version` in `build.gradle`).


## License

MIT
