[![License](https://img.shields.io/github/license/kgbcupcake/nourished)](LICENSE) [![Release](https://img.shields.io/github/v/release/kgbcupcake/nourished?include_prereleases)](https://github.com/kgbcupcake/nourished/releases) [![Stars](https://img.shields.io/github/stars/kgbcupcake/nourished?style=social)](https://github.com/kgbcupcake/nourished/stargazers) [![Issues](https://img.shields.io/github/issues/kgbcupcake/nourished)](https://github.com/kgbcupcake/nourished/issues) [![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen)](https://www.minecraft.net) [![NeoForge](https://img.shields.io/badge/NeoForge-21.1.229-orange)](https://neoforged.net) [![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/kgbcupcake/nourished)


![Banner](https://cdn.modrinth.com/data/cached_images/23ee1c51fc7b5b7952fd2800754279429f3a69d9_0.webp)

Minecraft's food system has always been simple: eat until you're full.

Nourished turns food into a configurable nutrition system for NeoForge 1.21.1. Track six food groups, build a varied diet, manage your calorie intake, and deal with the consequences of neglecting your nutrition.

**Everything is modular and configurable, so Nourished can be adapted to your modpack or server rather than forcing a single way of playing.** This applies across config, datapacks, KubeJS, custom nutrients, custom milestones, food overrides, compatibility, module toggles, and the in-game appearance editor.

---

<details open>
<summary>✨ Dynamic UI</summary>

### Video showcase
[![Dynamic UI showcase](https://img.youtube.com/vi/WgrOxjer6Tw/maxresdefault.jpg)](https://youtu.be/WgrOxjer6Tw)

> ▶️ Click the image above to watch the Dynamic UI showcase on YouTube.

---
### diet screen
![diet screen](https://cdn.modrinth.com/data/cached_images/d4f8bad921b1e2b37981a592fcec18207a803ce6_0.webp)
---
### Mini HUD
![Mini Hud](https://cdn.modrinth.com/data/cached_images/343277fa827ca78300419bf4cb3aadc756fd6831_0.webp)

> The image above shows the new UI introduced in **0.2.7-beta.1**.

**Dynamic UI** is Nourished's modular interface system, designed to make the HUD and diet screen highly configurable without relying on hardcoded layouts.

UI elements are organized into independent modules that can be customized individually, including their position, size, colors, and other visual properties. This allows the interface to be adjusted to fit different screen resolutions, UI layouts, and player preferences.

The system is designed to be extensible, making it possible to add new UI modules and customization options without rebuilding the entire interface.

**Features:**

* Modular UI components
* Movable UI elements
* Adjustable module sizes
* Configurable colors
* Independent module configuration
* Reusable UI architecture
* Designed for future customization options
* Used by both the Nourished HUD and Diet Screen
* Per-row dragging: reposition individual nutrient rows independently within the HUD box
* Fine-grained visibility rules: auto-hide zero-value bars, or hide/show bars above custom thresholds
* Shared edit-mode tabs for the Nutrient HUD, Calorie Log, and Activity Log panels, so only one is ever open at a time

> Dynamic UI replaces Nourished's older interaction-based UI controls with a proper modular editing system, providing a cleaner foundation for expanding UI customization over time.

> 📸 *Screenshots were taken with the PureBDCraft resource pack & custom edits. The UI works with vanilla textures, but will appear in the default Minecraft style.*

### Diet Screen

| What you see         | What it means                            |
| -------------------- | ---------------------------------------- |
| Trend arrows         | Whether each group is rising or falling  |
| Balance score        | How evenly distributed your nutrition is |
| Active effects       | What your current diet is doing to you   |
| Calorie tracking     | Daily calorie intake at a glance         |
| Recently eaten       | What you've eaten recently               |
| Neglected categories | What you should probably eat next        |

</details>

---

<details>
<summary>🎨 In-Game Appearance Editor</summary>

Every panel has its own Appearance window, opened alongside edit mode, for styling it without ever touching a config file.

### Per-panel styling

* **Background** — opacity and shade
* **Border** — opacity and shade
* **Text & icon brightness**
* **Vertical layout** toggle (stack bars vertically instead of horizontally)

### Per-nutrient bar colors

Every nutrient bar gets its own color slot, independent of the others — recolor Grains, Vegetables, Proteins, Fruits, Sugars, and Dairy individually to match your pack or taste.

### Glow

Each nutrient bar has its own glow, independently toggleable and tunable:

* Glow **color**
* Glow **strength**

### Pulse / flash

Bars that are critical or low can pulse to draw your eye, tied into the same visibility system that auto-hides or reveals bars at custom thresholds.

Every setting here writes straight to config in the background, so anything you set through the in-game editor is saved automatically and shows up the same way in `config/nourished/` if you'd rather hand-edit it later.

</details>

---

<details>
<summary>🛠️ Item Editor</summary>

[![Item Editor showcase](https://img.youtube.com/vi/gOLJr8XRMx0/maxresdefault.jpg)](https://youtu.be/gOLJr8XRMx0)

> ▶️ Click the image above to watch the Item Editor showcase on YouTube.

The **Item Editor** lets you inspect and tune how any individual item is handled by Nourished, right in-game — no config files or reloads required.

### Opening the editor

* Click the **edit** button next to any item in the Food Scanner config screen
* Or bind the **Open Item Editor** hotkey (unbound by default) to open it from anywhere, including on top of your inventory
* Drag any item straight out of **JEI** or **EMI** onto the editor to target it

### Values

* See the live classification trace for the targeted item
* Edit and save per-item nutrient value overrides that apply immediately

### Options

* Toggle **Excluded from classification** to stop an item from counting toward nutrition entirely — both in its tooltip and when it's eaten

### Exclude Tooltip

Style the "Excluded from nutrition tracking" tooltip line per item:

* Custom **message**
* Custom **color** with a color-wheel picker
* **Pulse** animation with Slow / Normal / Fast speeds

Every page uses the same header-level **Save** / **Revert** controls.

</details>

---

<details>
<summary>🏆 Milestones & Achievements</summary>

Eating consistently pays off over time. Nourished tracks cumulative intake for each food group and grants milestone rewards as you build healthy habits.

![Milestone Tree](https://cdn.modrinth.com/data/cached_images/fc757a070208988918b6adb3c60254abd75f28e8.png)

Each food group has its own three-tier progression:

* **Beginner**
* **Journeyman**
* **Master**

There is also a hidden **Perfectly Balanced** milestone for maintaining all five balance-tracked food groups (Grains, Vegetables, Proteins, Fruits, and Dairy — Sugars sits outside the balance system) at once.

Milestones grant temporary buffs and unlock entries in your Advancements tab.

Milestone goals, reward effects, and durations are fully configurable. Modpack authors can also register entirely custom milestones through datapacks.

</details>

---

<details>
<summary>❤️ What You Gain</summary>

When all five balance-tracked food groups — Grains, Vegetables, Proteins, Fruits, and Dairy — are above **75%**, you receive:

* **Health Boost I**: passively while balanced
* **Regeneration I**: passively while balanced

Let any single group drop below **25%**, and a neglect penalty kicks in:

| Group         | Neglect Penalty  |
| ------------- | ---------------- |
| 🌾 Grains     | Weakness I       |
| 🥦 Vegetables | Slowness I       |
| 🥩 Proteins   | Mining Fatigue I |
| 🍎 Fruits     | Unluck I         |
| 🍬 Sugars     | —                |
| 🥛 Dairy      | —                |

Sugars and Dairy have no penalty effect by default. The Sugars group is still tracked and factored into your overall balance score, even though it sits outside the Health Boost/Regeneration buff above.

This behavior is configurable.

</details>

---

<details>
<summary>🏃 Activity-Driven Nutrients</summary>

Your body doesn't just burn energy from being hungry; it burns energy from doing things now.

Nourished tracks your actual activity and factors it into your nutrient needs.

### Activity Costs

* **Sprinting & swimming** drain nutrients faster the longer you keep them up.
* **Mining** costs a small amount of nutrients per block broken.
* **Combat** costs nutrients per kill.
* **Starvation** applies a one-time penalty when a nutrient drops into a critical range.

Every activity module is independently toggleable. Turn off combat costs, disable starvation penalties, or run the entire system; it's up to you.

Each module's cost and HUD color are configurable.

Everything is backed by real per-player tracking:

* Distance sprinted
* Distance swum
* Blocks mined
* Kills
* Starvation events

Activity data is recorded day-by-day rather than simply estimated.

### Activity Log HUD

![Activity-Hud](https://cdn.modrinth.com/data/cached_images/fe5705ea8c5a59612c19bbad105f11cc64cf7ab3.png)

The **Activity Log HUD** is optional and can be repositioned, resized, or hidden entirely. Each statistic includes a live bar relative to your own personal best.

Milestones can also be set on activity trackers through datapacks or KubeJS, allowing modpack authors to reward goals such as the following:

* Sprint 10,000 blocks
* Swim 5,000 blocks
* Mine 10,000 blocks
* Land 500 kills

This option can be turned off in the configs.

</details>

---

<details>
<summary>🔥 Calories</summary>

Calories are the backbone of Nourished's nutrition system.

Every food you eat adds to or subtracts from your daily calorie total, and staying within your calorie goal matters alongside hitting your nutrient targets.

### Live Tracking

Your calorie total updates in real time and persists across sessions, with complete history retained for previous days.

### Calorie HUD Panel

![Calories - Hud](https://cdn.modrinth.com/data/cached_images/430556072d42be22da62c59b7bdee22c5bdf6331.png)

The **Calorie Log HUD** is optional and can be repositioned, resized, or hidden entirely.

A compact, draggable overlay displays the following:

* Today
* Yesterday
* Previous days

Each day is shown as a percentage of your calorie goal, with the display changing once you go over your target.

### Eat-Food Notifications

Eating something that changes your calorie total now produces a quick on-screen notification showing the food's name and calorie change.

You can see exactly what just happened without opening a menu.

### Diet Screen Integration

The diet screen ties everything together with:

* Full calorie breakdown
* Recent meals
* Daily calorie balance
* Foods you may want to eat to round out your diet

Every panel can be independently repositioned, resized, and scaled.

> Like everything else in Nourished, calorie goals and thresholds are configurable, and the system is datapack- and KubeJS-friendly for modpack authors who want to tune it further. This option can also be turned off in the configs.

</details>

---

<details>
<summary>🍽️ Variety Is Always Rewarded</summary>

Eating the same food repeatedly becomes less effective over time, so rotating what you eat always pays off.

Nourished uses a memory system that naturally fades over time. Foods you've avoided recover their full nutritional value as your food memory fades.

The result is simple:

> **The more varied your diet, the more effective it becomes.**

</details>

---

<details>
<summary>🍽️ Eating at Full Saturation</summary>

Vanilla Minecraft prevents you from eating when your hunger and saturation are already full.

Nourished allows you to continue eating for nutritional purposes even when your saturation bar is full.

Light foods such as berries and snacks can be eaten for their nutrients without restoring saturation, while heavier meals follow vanilla eating rules by default.

Both behaviors are configurable through:

* `enableBlockHeavySources`
* `enableBlockLightSource`

Server administrators have full control over how these eating restrictions behave.

</details>

---

<details>
<summary>🥩 Raw Food & Gut Health</summary>

Eating raw or undercooked food has consequences.

Nourished tracks a **gut health** value for every player. Gut health degrades when you eat raw food and recovers over time through cooked food and dietary variety.

### Raw Food Tiers

Raw foods are classified into four tiers:

| Tier       | Effect                           |
| ---------- | -------------------------------- |
| **Fine**   | No penalty                       |
| **Mild**   | Minor debuff, short duration     |
| **Medium** | Moderate debuff, longer duration |
| **Severe** | Strong debuff, extended duration |

Eating the same raw food repeatedly within its memory window increases your sensitivity to it. The more you repeatedly eat the same raw food, the worse the penalty becomes.

Gut health recovers passively and recovers faster when you:

* Eat cooked food
* Maintain dietary diversity

Resistance can also be built over time, reducing the scale of raw-food penalties.

Everything is configurable, including:

* Raw-food tiers
* Effect durations
* Nutrient penalties
* Recovery rates
* Resistance
* Module behavior

Configuration is handled through:

`config/nourished/raw_food.json`

</details>

---

<details>
<summary>🤝 Mod Compatibility</summary>

Nourished works with modded food items that use vanilla `FoodProperties`.

You do **not** need to write integration code for basic nutrition tracking.

Bundled compatibility data helps foods from popular farming and cooking mods appear on your nutrient bars out of the box.

For edge cases, server owners can adjust behavior through configuration files and tags.

| Mod                               | Status                           |
| --------------------------------- | -------------------------------- |
| Farmer's Delight                  | ✅ Supported                      |
| Pam's HarvestCraft 2              | ✅ Supported                      |
| Croptopia                         | ✅ Supported                      |
| Farm & Charm                      | ✅ Supported                      |
| Butchery                          | ✅ Supported                      |
| Herbs & Harvest                   | ✅ Supported                      |
| Spice of Life: Onion              | ✅ Supported                      |
| Legendary Survival Overhaul       | ⚠️ Supported — Nourished's own effects step aside so LSO's take priority |
| Peak Stamina                      | ✅ Supported — nutrition feeds into stamina |
| KubeJS                            | ✅ Scripting hooks                |
| JEI / REI / EMI                   | ✅ Tooltips in recipe viewers     |
| Other mods with edible food items | ✅ Works with standard food items |

</details>

---

<details>
<summary>🔧 Configurable for Your Server</summary>

Everything ships with sensible defaults.

Server owners and modpack authors can tune Nourished without touching Java.

### Module Controls

* Toggle individual modules on or off
* Enable or disable the HUD
* Configure nutrient decay
* Configure activity-driven costs
* Enable or disable effects
* Enable or disable gut health

### Nutrient Configuration

* Adjust decay rates
* Adjust thresholds
* Configure critical values
* Configure low and excess thresholds

### Effects

Add, remove, or replace effects through:

`config/nourished/effects.json`

### Eating Rules

Configure:

* `enableBlockHeavySources`
* `enableBlockLightSource`

### Data & Overrides

Customize:

* Food values
* Food overrides
* Nutrient colors
* Scanner specifications
* Compatibility settings
* Presets
* Module locks

All configuration lives under:

`config/nourished/`

### Config Sharing

Save and share complete configuration snapshots using a single share code.

</details>

---

<details>
<summary>🔍 The Food Scanner</summary>

Use:

```text
/nourished scan_analysis
```

to analyze all loaded food items and generate classification reports under the following:

```text
config/nourished/scanner_analysis/
```

### Scanner Output

The scanner produces:

* `multi_value_recommendations.json`:  foods confidently assigned to one or more nutrient groups
* `unassigned_sources.txt`:  foods that could not be classified automatically and need manual review
* `scanner_metrics.txt`:  summary statistics across the full item registry

Recommended tag entries can be copied into your datapack to make classifications permanent.

Unassigned items can be manually tagged using the example below.

</details>

---

<details>
<summary>📁 Data & Overrides</summary>

Most customization happens inside:

```text
config/nourished/
```

after the first launch.

This includes:

* Effects
* Food values
* Food overrides
* Colors
* Presets
* Module locks
* Scanner configuration
* Compatibility settings

Nourished also ships bundled defaults under:

```text
data/nourished/
```

These include nutrient tags, compatibility entries, and presets.

Advanced pack authors can override supported data through datapacks where applicable. Configuration files remain the primary method for server-side tuning.

The in-game **food scanner** can help inspect and assign foods in singleplayer and write generated tag files into the world's datapack folder for manual review.

### Example: Adding a Food to a Nutrient Tag

Create the following file inside your datapack:

```text
your_datapack/
└── data/
    └── nourished/
        └── tags/
            └── item/
                └── nutrients/
                    └── vegetables.json
```

```json
{
  "replace": false,
  "values": [
    {
      "id": "pamhc2foodextended:honeyglazedcarrotsitem",
      "required": false
    }
  ]
}
```

`"replace": false` means your entries are merged with Nourished's defaults rather than replacing them.

</details>

---

<details>
<summary>📜 KubeJS Support</summary>

Nourished provides optional **KubeJS** integration for modpack authors.

KubeJS must be installed separately.

Scripts go in:

```text
kubejs/server_scripts/
kubejs/startup_scripts/
```

KubeJS support is split across two layers:

* **MariesLib** provides generic value-tracking events through `MarieEvents.*`.
* **Nourished** provides nutrition-specific aliases through `NourishedEvents.*`.

### Server Scripts:  Nutrition Events

```javascript
NourishedEvents.nutrientChanged(event => {
    if (event.nutrientKey === 'proteins' && event.newValue < 0.25) {
        event.player.tell('Eat some protein!')
    }
})

NourishedEvents.nutrientModifier(event => {
    if (event.nutrientKey === 'vegetables') {
        event.amount *= 2
    }
})

NourishedEvents.foodEaten(event => {
    event.nutrientDeltas.forEach((value, key) => {
        console.log(key + ': ' + value)
    })
})
```

Also available:

* `nutrientCritical`
* `nutrientExcess`
* `rawFoodPenalty`
* `gutHealthChanged`

> **Important:** `sourceConsumed` is a generic MariesLib event and does not expose `nutrientKey` or `newValue`. Use `nutrientChanged` when you need nutrition-specific information.

### Generic MariesLib Events

MariesLib's own value-tracking events (`valueChanged`, `valueDeltaModifier`, `valueCritical`, `valueExcess`, `sourceConsumed`, `decayTick`, `playerSynced`) power the framework internally. They are not currently exposed as standalone KubeJS bindings — for KubeJS scripting, use the `NourishedEvents.*` aliases above, which cover the same data for nutrition purposes.

### Startup Scripts:  Register Custom Nutrients

```javascript
NourishedAPI.registerNutrient({
    id: 'omega3',
    displayName: 'Omega-3',
    color: 0x4AA3FF,
    decayRate: 0.0012,
    critical: 0.12,
    low: 0.30,
    excess: 0.90
})
```

See **API.md** on GitHub for the full event and binding reference.

</details>

---

<details>
<summary>💾 Old UI (Legacy)</summary>

The **Old UI** is Nourished's legacy interface — the original HUD and diet screen layout from before Dynamic UI existed. It's retained for compatibility with older setups and is no longer receiving new UI features.

> Maintained for backward compatibility only. Planned for removal in 0.2.8-beta.

</details>

---

## ⚙️ Requirements

| Requirement      | Version                  |
| ---------------- | ------------------------ |
| **Minecraft**    | 1.21.1                   |
| **NeoForge**     | 21.1.x                   |
| **Marie's Lib**  | v0.1.2-beta+             |
| **Cloth Config** | Required at runtime      |
| **Modonomicon**  | Required — in-game guide |
| **Java**         | 21                       |

> **Important:** Marie's Lib is a required dependency. Patchouli has been fully replaced by Modonomicon, which is now a required dependency.

---

## 🛠️ Support & Feedback

Found a bug, have a suggestion, or need help?

* **GitHub Issues**: report bugs and feature requests
* **Discord**:  support and development updates

Please include logs, screenshots, or reproduction steps when reporting an issue.

---

## 📜 License

**MIT License**

Nourished is free to use in modpacks, forks, and addons.
