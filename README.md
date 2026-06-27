[Ask DeepWiki](https://deepwiki.com/kgbcupcake/nourished)

Main Menu

I got sick of Minecraft's food system. There are other nutrition mods out there,
but none of them did what I wanted or were updated for modern Minecraft,
so I decided to build my own.

---

## Community

Discord: [[https://discord.gg/EZnFJsfQup]](https://discord.gg/EZnFJsfQup])

Questions, suggestions, and development discussion are welcome.

❤️ What you gain
When you have all five food groups are above 75%, you get:

Health Boost I — passively while balanced
Regeneration I — passively while balanced

When all five food groups are above 75%, you get:

| Group         | Neglect Penalty  | Balance Buff |
| ------------- | ---------------- | ------------ |
| 🌾 Grains     | Weakness I       | ✓            |
| 🥦 Vegetables | Slowness I       | ✓            |
| 🥩 Proteins   | Mining Fatigue I | ✓            |
| 🍎 Fruits     | Unluck I         | ✓            |
| 🍬 Sugars     | —                | —            |
| 🥛 Dairy      | —                | ✓            |

---

Sugars and Dairy have no penalty effect by default these groups are tracked and affect your balance score but do not apply a debuff when depleted. Dairy still counts toward the balance buff. This is configurable.

## 🍽️ Eating at full hunger

Vanilla hunger normally blocks eating at full hunger; Nourished still counts nutrition when your bar is full. Light foods berries, fruits, and snacks can be eaten for nutrients without restoring hunger. Heavy meals follow vanilla rules by default.

Both behaviors are configurable: `blockHeavyMeals` and `blockLightFood` toggles give server admins full control.

All effects are fully configurable and can be disabled per-module.

Diminishing returns apply - eating the same food repeatedly gives less credit each time, encouraging real variety.

---

---

## 🥩 Raw Food & Gut Health

Eating raw or undercooked food has consequences. Nourished tracks a **gut health** value for every player that degrades when you eat raw food and recovers over time from cooked food and dietary variety.

Raw foods are classified into four tiers:

| Tier   | Effect                           |
| ------ | -------------------------------- |
| Fine   | No penalty                       |
| Mild   | Minor debuff, short duration     |
| Medium | Moderate debuff, longer duration |
| Severe | Strong debuff, extended duration |

Eating the same raw food repeatedly within a memory window increases sensitivity, the more you do it, the worse the penalty gets. Gut health recovers passively and faster when you eat cooked food and maintain dietary diversity.

Resistance can be built up over time, reducing penalty scale. Everything, tiers, durations, nutrient penalties, recovery rates, is configurable via `config/nourished/raw_food.json` and server module toggles.

---

## The HUD

The HUD is the heart of the mod. Five color-coded bars sit on screen while you play;you always know where you stand without opening a menu.

HUD Edit Mode

**Drag it anywhere.** Press the keybind to enter edit mode and reposition the HUD exactly where you want it. Scale it, anchor it to any corner, or hide bars that are at zero.

---

## The Diet Screen

Open it from your inventory for a full breakdown - trend arrows, balance score, active effects, calorie tracking, and a reset timer.

Diet Screen

Note: The HUD and Diet Screen screenshots were taken using the PureBDCraft resource pack. The UI is fully functional on vanilla textures but will appear in the default Minecraft style without a resource pack installed.

---

## Modularity

Every feature in Nourished is a module toggle. Turn off decay, effects, the HUD, toasts, calorie tracking, or the diet screen independently. Modpack authors can lock modules server-side.

## 🔧 Configurable to your server

Everything ships with sensible defaults. Everything can be changed:

- Toggle individual modules on or off
- Adjust decay rates and thresholds per nutrient
- Add, remove, or replace effects via `effects.json`
- Override anything through datapacks — no file editing needed
- Control eating behavior with `blockHeavyMeals` and `blockLightFood` toggles
- Save and share full config snapshots with a single share code

---

## 🤝 Broad Mod Compatibility

If a mod adds food with `FoodProperties`, Nourished handles it automatically. No data files to write, no configs to edit.

| Mod                         | Status                                   |
| --------------------------- | ---------------------------------------- |
| Farmer's Delight            | ✅ Full                                  |
| Pam's HarvestCraft 2        | ✅ Full                                  |
| Create: Food                | ✅ Full                                  |
| Croptopia                   | ✅ Full                                  |
| Farmer's Croptopia          | ✅ Full                                  |
| Croptopia Delight           | ✅ Full                                  |
| Farm & Charm                | ✅ Full                                  |
| Ender's Delight             | ✅ Full                                  |
| L_Ender's Delight           | ✅ Full                                  |
| Ars Delight                 | ✅ Full                                  |
| Autochef's Delight          | ✅ Full                                  |
| Spice of Life: Onion        | ✅ Full                                  |
| KubeJS                      | ✅ Full scripting support                |
| Peak Stamina                | ✅ Nutrition affects stamina             |
| JEI / REI / EMI             | ✅ Tooltips in recipe viewers            |
| Legendary Survival Overhaul | ⚠️ Effects disabled (LSO takes priority) |
| Any mod with FoodProperties | ✅ Auto-classified                       |

---

## 🌐 For mod developers

Nourished runs on [MariesLib](https://github.com/kgbcupcake/MarieLib) — install both mods. The nutrition API is a thin facade over the library:

```java
float level = NourishedAPI.getValueLevel(player, "proteins");
NourishedAPI.registerValue(definition);
NourishedAPI.registerSourceClassification(foodId, "proteins", 0.15f);
```

Read `[API.md](API.md)` for Nourished integration, `[PHILOSOPHY.md](PHILOSOPHY.md)` for stability rules, and [MariesLib API](https://github.com/kgbcupcake/MarieLib/blob/main/API.md) for shared types like `ValueDefinition`. Datapack-only integrations need no Java.

---

## 📦 Datapack Support

Everything in Nourished can be driven by datapacks with zero Java code:

Nutrients — define custom food groups
Food classification — assign items to nutrient bars via item tags under data/nourished/tags/item/nutrients/
Effects add or replace buff/debuff rules via effects.json
Food overrides — override specific item nutrition values via food_overrides.json
Food values — adjust category multipliers via food_values.json
Colors — customize HUD bar colors via colors.json
The built-in food scanner (MariesLib tooling, `/nourished scan`) auto-classifies unknown foods and can write datapack output into your save. See `[API.md](API.md)`.

---

## 🟨 KubeJS Support

Full KubeJS scripting support for custom nutrient events, food classifications, and diet hooks — no Java required.

```js
NourishedEvents.nutrientChanged(event => {
    if (event.valueKey === 'proteins' && event.newValue < 0.25) {
        event.player.tell('Eat some protein!')
    }
})
```

---

## 🚧 Current Focus

- Balancing nutrient decay
- Expanding datapack support
- Improving multiplayer syncing
- Additional compat integrations
- More HUD customization

---

---

## ⚙️ Requirements

|                  |                                                                |
| ---------------- | -------------------------------------------------------------- |
| **Minecraft**    | 1.21.1                                                         |
| **NeoForge**     | 21.1.x                                                         |
| **MarieLib**     | **v0.1.0-beta.1** ( ⚠️ Going forward from **v0.2.5-beta.5**. ) |
| **Cloth Config** | required at runtime                                            |
| **Patchouli**    | optional (in-game guide)                                       |
| **Java**         | 21                                                             |

---

## License

MIT

## Links

- [Modrinth](https://modrinth.com/mod/nourished)
- [MariesLib](https://github.com/kgbcupcake/MarieLib) (required dependency)
- [Contributing](docs/CONTRIBUTING.md)
- [API.md](API.md)
- [PHILOSOPHY.md](PHILOSOPHY.md)
- [ARCHITECTURE.md](ARCHITECTURE.md)
- [RoadMap.md](RoadMap.md)
