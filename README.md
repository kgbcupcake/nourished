# Nourished

> **Food variety now matters.**
> **The nutrition engine for the NeoForge food ecosystem.**

Minecraft tracks hunger. Nourished tracks _what_ you ate. Six food groups, real consequences, and a HUD you can actually read - without turning survival into a spreadsheet.

![Main Menu](Assets/MainGui.png)

---

## What it does

Eat steak all day and your protein bar fills up - but your fruits, grains, and dairy start to slip. Let them drop too low and you'll feel it. Keep a balanced diet and the game rewards you.

That's it. No menus to dig through, no math to do. Just eat varied food and the system takes care of itself.

For mod and modpack authors, Nourished is a platform. Register custom nutrients, hook into diet events, define food synergies, and build addon mods on top of a stable public API - without touching Nourished internals.

---

## The HUD

The HUD is the heart of the mod. Six color-coded bars sit on screen while you play - you always know where you stand without opening a menu.

![HUD Edit Mode](Assets/nourished-MiniHud.gif)

**Drag it anywhere.** Press the keybind to enter edit mode and reposition the HUD exactly where you want it. Scale it, anchor it to any corner, or hide bars that are at zero.

---

## The Diet Screen

Open it from your inventory for a full breakdown - trend arrows, balance score, active effects, calorie tracking, and a reset timer.

![Diet Screen](Assets/nourished-MainMenu.gif)

---

## Effects

| Condition                   | Effect                                            |
| --------------------------- | ------------------------------------------------- |
| Any group < 25%             | Debuff (fatigue, weakness, slowness, or bad luck) |
| All groups > 75% + balanced | Regeneration                                      |
| All groups > 75%            | Health Boost                                      |

All effects are fully configurable and can be disabled per-module.

Diminishing returns apply - eating the same food repeatedly gives less credit each time, encouraging real variety.

---

## Modularity

Every feature in Nourished is a module toggle. Turn off decay, effects, the HUD, toasts, calorie tracking, or the diet screen independently. Modpack authors can lock modules server-side.

---

## Compatibility

| Mod | Status |
| --- | --- |
| Ars Flavor's Delight | ✅ Datapack compat included |
| AutoChef's Delight | ✅ Datapack compat included |
| Botany Pots | ✅ Datapack compat included |
| Cataclysm Delight | ✅ Datapack compat included |
| Cold Sweat | ✅ Datapack compat included |
| Create: Food | ✅ Datapack compat included |
| Crop Critters | ✅ Datapack compat included |
| Croptopia / Farmer's Croptopia | ✅ Datapack compat included |
| Croptopia Delight | ✅ Datapack compat included |
| Ecliptic Seasons | ✅ Datapack compat included |
| Ender's Delight / End's Delight | ✅ Datapack compat included |
| Expanded Delight | ✅ Datapack compat included |
| Farmer's Delight | ✅ Datapack compat included |
| Farming for Blockheads | ✅ Datapack compat included |
| Let's Do: Bakery | ✅ Datapack compat included |
| Let's Do: Brewery | ✅ Datapack compat included |
| Let's Do: Farm & Charm | ✅ Datapack compat included |
| Let's Do: HerbalBrews | ✅ Datapack compat included |
| Legendary Survival Overhaul | ⚠️ Effects disabled - LSO takes priority |
| Mama's Herbs | ✅ Datapack compat included |
| More Delight | ✅ Datapack compat included |
| Naturalist | ✅ Datapack compat included |
| Ocean's Delight | ✅ Datapack compat included |
| Pam's HarvestCraft 2: Crops | ✅ Datapack compat included |
| Pam's HarvestCraft 2: Food Core | ✅ Datapack compat included |
| Pam's HarvestCraft 2: Food Extended | ✅ Datapack compat included |
| Pam's HarvestCraft 2: Trees | ✅ Datapack compat included |
| Serene Seasons | ✅ Datapack compat included |
| Spice of Life: Onion | ✅ Datapack compat included |
| Tough As Nails | ✅ Datapack compat included |
| Any unknown food mod | ✅ Auto-detected at startup |

---

## For Mod Authors

Read [`API.md`](API.md) for the full public API and [`PHILOSOPHY.md`](PHILOSOPHY.md) for compatibility and stability guarantees. Addons can register custom nutrients, food classifications, and diet events through Java or KubeJS, and can also ship datapack-only integrations without writing Java code. See the example addon project for a minimal end-to-end integration pattern.

```java
if (!NourishedAPIVersion.isCompatible(1)) return;
NourishedAPI.registerNutrient(definition);
NourishedAPI.registerFoodClassification(foodId, nutrientKey, amount);
```

---

## Datapack Support

Nutrients, effects, food classifications, synergies, milestones, and diet profiles can all be defined in datapacks with zero Java code. For schema details and examples, see [`API.md`](API.md).

---

## Requirements

- Minecraft **1.21.1**
- NeoForge **21.1.x**
- Java **21**

---

## License

MIT

## Links
- [Modrinth](https://modrinth.com/mod/nourished)
- [API.md](API.md)
- [PHILOSOPHY.md](PHILOSOPHY.md)
