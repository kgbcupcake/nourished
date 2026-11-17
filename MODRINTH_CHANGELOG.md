# Nourished 0.2.7-beta.4

Nutrients and deficiency. Alongside the five food groups you now track Omega-3, Fiber, Vitamins, Minerals, Fat, Protein and Carbs, and letting your diet slip for too long builds into a three-level deficiency with real penalties. The guide book has also been rewritten, and there's a new in-game Settings window.

> **Requires:** Modonomicon, MariesLib / Marie's Framework **0.1.2-beta.2** or newer.

## ✨ Highlights

- **Nutrients.** Omega-3, Fiber, Vitamins, Minerals, Fat, Protein and Carbs, each with its own level, tooltip line and Diet Screen box. Fish give Omega-3 (salmon the most), and crafted or cooked foods add up the nutrients of their ingredients. Everything is datapack/config driven.
- **Diet deficiency.** Keep a food group low and it builds over play time into three levels (20 / 60 / 120 min by default). Penalties grow with each level: faster drain from sprinting, mining, swimming and combat, then slower healing, less saturation, and lower movement, mining and attack speed, shown as a new **Malnourished I-III** effect that milk can't clear. Fixing your diet recovers you gradually.
- **Guide book redesign.** Seven chapters: a short Getting Started path, then Food, Nutrition, Activity, Gut Health, Progression and Advanced as reference. 27 new pages.
- **In-game Settings window** with the main feature switches and balance options, opened with its own keybind or from the Command Center's new Config tab.

## ➕ Added

### Nutrients
- New **Nutrients tab** in the Nutritional Panel: an overview of every nutrient plus a page each with level, trend, recent sources and best foods. Clicking the Nutrients box on the Diet Screen opens the same detail view.
- Saved daily intake history with 7-day charts for each nutrient (follows the Calorie History setting).
- Nutrient effects: low Omega-3, Fiber, Vitamins or Minerals bring a mild debuff that gets stronger after 20 and 60 minutes, high Omega-3 gives Dolphin's Grace, and keeping every nutrient up gives Haste. Off by default (`builtinEffects`); datapacks can add their own.
- Optional nutrient HUD overlay with its own toggle key.
- `/nourished nutrients` commands and an item editor Nutrients page for setting a food's nutrient values in game.
- Modpack authors can pin or hide foods in each nutrient's "Best sources" list (datapack, config, command or KubeJS).
- KubeJS: `NourishedNutrients` for adding nutrients, setting food values and reading/setting levels.

### Deficiency
- New **Deficiency tab**: your current level and penalties, a bar per food group showing how close it is to the next level, how long until it worsens or recovers, and when you last ate each group. You also get a notification when you slip into a new level or recover.
- Fully configurable in `gameplay/deficiency.json` or by datapack, including every penalty and attribute per level. `effects.json` rules can trigger off a deficiency tier.
- Optional calorie deficiency: packs can turn on a Calories row so not eating enough for a day builds deficiency too. Off by default.
- `/nourished deficiency` shows your status; ops can `get`/`set`/`clear` a player's deficiency or `reload` the config. Command Center cards open the config or show your status.
- KubeJS: `NourishedDeficiency` to read and set deficiency, plus `NourishedEvents.deficiencyTierChanged` and `deficiencyPressure` to steer or block tier changes and build-up.

### Everything else
- **Calorie History and Activity Log tabs** in the Nutritional Panel, styled, moved and resized like every other tab.
- Command Center **Config tab** that opens Settings, the full config and the Diet Screen editor.
- The Deficiency and Nutrients boxes can move and hide their text, icons and bars in the editor, like the Intake Breakdown rows.
- Panel tabs scroll when their content doesn't fit.
- Item editor Info tab shows each ingredient and how much it added to the food's groups.
- Foods with no recipe now get food groups from their name, using other mods' tagged items (a "kiwi smoothie" counts as fruit if any mod has a tagged kiwi).
- `/nourished diagnostics` now also lists broken datapack files, nutrient file errors and item ids that don't exist (typos).

## 🔧 Changed

- The `config/nourished/` folder is tidier: nutrient and food value files are in `nutrition/`, effects and locks in `gameplay/`, raw food settings in `modules/raw_food/`. Existing files are moved over automatically on first launch.
- Bars and charts in the panel tabs keep their size when you resize a box instead of shrinking.

## 🐛 Fixed

- **The Sleep Bonus didn't work.** Waking up with every food group at least half full now gives Regeneration I for 30 seconds.
- Cake and other foods eaten off a placed block (like modded pies) now give nutrition.
- Turning off calorie history no longer hides today's calories on the Diet Screen.
- The guide book described per-group bonuses that don't exist. Effects pages now match what actually happens, and pages that showed "Format error" are fixed.

## 🗑️ Removed

- The separate on-screen Calorie History and Activity Log HUD boxes, their C/K edit keys and their HUD config options. They now live as tabs in the Nutritional Panel.
- Guide book pages for Legendary Survival Overhaul, Peak Stamina and Spice of Life: Onion.

> ⚠️ **Beta:** Your config files get moved into the new folders on first launch. Back up `config/nourished/` first if you've customized it, and please report anything odd on the issue tracker or Discord!

---

# Nourished 0.2.7-beta.3

A new "Nutritional HUD" Diet Screen style — a resizable sidebar window with a Home tab (your character and a live summary) — plus a big item editor expansion: Bulk Edit, Browse Items, Edited Items and per-item Options/Calories pages.

> **Requires:** Modonomicon, MariesLib / Marie's Framework **0.1.2-beta.1** or newer.

## ✨ Highlights

- **New "Nutritional HUD" Diet Screen style.** A resizable sidebar window — Home, Calories, Balance, Recent Meals, Eat More, Active Effects, Intake Breakdown — each tab fully stylable, draggable and resizable like the dynamic screen's boxes, with its own edit-mode toggle.
- **Home tab.** Your character in 3D next to a live summary of calories, balance, intake percents and active effects — styled, resizable and themeable like every other box.
- **Item editor expansion.** New Browse Items (filter + search), Edited Items, Bulk Edit (apply a change to many items at once), Calories, and Exclude Tooltip pages, alongside per-item toggles for Silent Eat, Exclude from Recent Meals, Raw Food Tier and Diminishing Returns.
- **Diet Screen/HUD edit mode rewrite.** Dragging or resizing a box no longer snaps back to a smaller size on release; the panel now grows to fit boxes pulled past its edge.

## ➕ Added

- Nutritional HUD's Home tab: Behavior/Style/Glow/Pulse/Colors, in its own styled box
- Nutritional HUD's Editor window chrome gets Style/Glow/Pulse like every module
- Nutritional HUD's Intake Breakdown tab scrolls with the mouse wheel when it overflows
- New "Tab color" slot for the Nutritional HUD's sidebar tabs
- Item editor: Browse Items (filter/search), Edited Items, Bulk Edit, Calories (custom calorie value), and Exclude Tooltip pages
- Item editor: per-item Silent Eat, Exclude from Recent Meals, Raw Food Tier override, and Diminishing Returns Off toggles
- Item editor: mod-wide "Diminishing Returns (All Food)" toggle
- New keybind to open the item editor directly, for dragging any item from JEI/EMI onto it

## 🔧 Changed

- Diet Screen and HUD edit mode now use MariesLib's shared edit-box system: resizing past the panel's edge grows the panel instead of snapping the box back
- Item editor's Bulk Edit: Nutrient Group is now a per-group On/Off row instead of one Add/Remove row per click
- Item editor's scrollbars (Browse Items, etc.) can be grabbed and dragged, not just wheel-scrolled
- Item editor's recipe/3D preview for Create mod items shows the plain crafting-table recipe again (a regression had dropped it)
- Item editor's Options page split: calorie settings moved to their own Calories page

## 🐛 Fixed

- Nutritional HUD's Home tab showed "Join a world to see your character" instead of the 3D preview even with a player loaded
- An item excluded only via the item editor's toggle didn't show the "Excluded from nutrition tracking" tooltip line
- "Diminishing Returns Off" didn't apply correctly in modpacks running more than one MariesLib-based mod
- Item editor pages that change gameplay now correctly show read-only on a remote/dedicated server instead of silently no-opping

## 🗑️ Removed

- Item editor's standalone "Nutrient Groups" page (now covered by Bulk Edit's per-group rows and the Values page)
- Classic Diet Screen — Dynamic is now the only non-Hub option

> ⚠️ **Beta:** Some of your old layout and color settings may reset. Please report anything odd on the issue tracker or Discord!

---

# Nourished 0.2.7-beta.2

A big UI overhaul: nearly every box on the HUD and Diet Screen can now be moved, resized, restyled and recolored in-game. The guide book has also moved from Patchouli to Modonomicon.

> **Requires:** Modonomicon, MariesLib / Marie's Framework **0.1.2-beta** or newer. Patchouli is **no longer needed**.

## ✨ Highlights

- **New guide book (Modonomicon).** Getting Started, How It Works, Food Safety, Food Groups and Compat Mods chapters. Compat pages only show up for mods you actually have installed.
- **The Diet Screen is fully customizable.** Every box (Calories, Balance, Recent Meals, Eat More, Active Effects and each Intake Breakdown row) can be dragged, resized and restyled, and its text, icons, bars and header can be moved separately.
- **A new editor hub.** The Diet Screen's box settings now live in one resizable window with a sidebar, instead of a stack of tabs.
- **In-game color editing.** Every HUD and Diet Screen color can be changed from a Colors tab. The color picker can also save favorite colors.

## ➕ Added

- Style options on every box: background/border opacity and shade, text and border shadow, and border/bar glow
- Nutrient HUD: its own bar glow for each nutrient, plus Hide Bars, Hide Text, Hide Icons and Hide Window toggles
- Header size and Hide Header on every Diet box
- Separate Text size, Icon size, Bar size and brightness sliders on every box
- Intake Breakdown rows: bar fill color, icon border color, icon size, and a Move Icon toggle
- Active Effects: icons and +/- markers can be moved separately from the effect names
- Nutrient HUD scrolls when it's shorter than its rows
- A single "Reset This Module" button on every box
- "Free Bites Before Diminishing" setting (how many times a food can be eaten at full value)
- **For addon devs:** new `NourishedRegisterEvent` for registering nutrients, foods and effects in one place, a full `docs/ADDON_GUIDE.md` with an example addon, and a "Nourished Test" Command Center tab (Registration Report, Check Held Food)
- KubeJS `registerNutrient` accepts `icon` and `tags`

## 🔧 Changed

- HUD panels have rounded corners
- Calorie History reads "1 day", "2 days"… and the HUD shows "Veggies"
- Nutrients always draw in their own color (no more threshold tinting)
- Removed redundant options (padding sliders, Move/Hide toggles that did nothing on some boxes)

## 🐛 Fixed

- **Every item in the game showed a Nourished tooltip.** Fixed.
- **"Today" calories never reset.** It now resets each in-game day, including after sleeping or using `/time`.
- The screen flickered or flashed black while eating, and the HUD stuttered
- Game crashed when opening "Edit All" HUD mode
- The guide book couldn't be crafted
- Addon nutrients got no Diet Screen row, and their decay/threshold overrides weren't saved
- The Nutrient HUD's edit mode now shows every bar, even hidden ones
- Activity Log and Calorie History no longer overlap on a fresh install or after a reset
- Bar glow, Hide Icons, Hide Bars and Icon size didn't work on several boxes
- Resizing boxes from the left edge dragged their contents along
- The Nutrient HUD's height snapped back or didn't fit its visible bars
- Rows overlapped and bars vanished at very small or very large text sizes
- The config sidebar ran off-screen at high GUI Scale
- `/nourished validate` false warnings on servers, plus startup log warnings cleaned up

## 🗑️ Removed

- Patchouli dependency and the old Patchouli book
- Text glow and header glow (they looked bad at Minecraft's font size)
- Intake Breakdown legend box (bar colors already show this)

> ⚠️ **Beta:** Some of your old layout and color settings may reset. Please report anything odd on the issue tracker or Discord!
