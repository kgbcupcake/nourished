# Nourished 0.2.7-beta.3

A new "Nutritional HUD" Diet Screen style — a resizable sidebar window with a Home tab (your character and a live summary) — plus a big item editor expansion: Bulk Edit, Browse Items, Edited Items and per-item Options/Calories pages.

> **Requires:** Modonomicon, MariesLib / Marie's Framework **0.1.2-beta.1** or newer.

## ✨ Highlights

- **New "Nutritional HUD" Diet Screen style.** A resizable sidebar window — Home, Calories, Balance, Recent Meals, Eat More, Active Effects, Intake Breakdown — each tab fully stylable, draggable and resizable like the dynamic screen's boxes, with its own edit-mode toggle.
- **Home tab.** Your character in 3D next to a live summary of calories, balance, intake percents and active effects — styled, resizable and themeable like every other box.
- **Item editor expansion.** New Browse Items (filter + search), Edited Items, Bulk Edit (apply a change to many items at once), Calories, and Exclude Tooltip pages, alongside per-item toggles for Silent Eat, Exclude from Recent Meals, Raw Food Tier and Diminishing Returns.
- **Diet Screen/HUD edit mode rewrite.** Dragging or resizing a box no longer snaps back to a smaller size on release; the panel now grows to fit boxes pulled past its edge.

## ➕ Added

- **Diet deficiency tiers.** Letting a food group stay low now builds up over play time into three deficiency levels (20 / 60 / 120 min by default) with growing penalties: more nutrient drain from sprinting, mining, swimming and combat, then slower natural healing, less saturation from food, and lower movement, mining and attack speed, shown as a new Malnourished effect. Fixing your diet recovers you gradually. Fully configurable in `gameplay/deficiency.json` or by datapack, and effects.json rules can trigger off a deficiency tier.
- **Deficiency tab** in the Nutritional Panel: your current level and penalties, a bar per food group showing how close it is to the next level, and how long until it worsens or recovers. You also get a notification when your diet slips into a new level or recovers.
- **Deficiency commands and scripting:** `/nourished deficiency` shows your status, and ops can `get`/`set`/`clear` a player's deficiency or `reload` the config. KubeJS can read and set deficiency (`NourishedDeficiency`) and can steer or block tier changes and build-up (`NourishedEvents.deficiencyTierChanged`, `deficiencyPressure`). There are also Command Center cards to open the config or check your status.
- **Malnourished:** each deficiency level shows as Malnourished I-III. The penalties (regen, saturation, movement/mining/attack) can't be cleared with milk, and every one of them, including any attribute, is set per level in `deficiency.json`.
- **Optional calorie deficiency:** packs can turn on a Calories row in `deficiency.json`, so not eating enough for a day builds deficiency too. Off by default.
- **Nutrients alongside the food groups:** Omega-3, Fiber, Vitamins, Minerals and Fat, with their own levels, tooltip line and Diet Screen box. Fish now give Omega-3, and crafted or cooked foods add up the nutrients of their ingredients. Fully datapack/config driven.
- Nutrient effects: low Omega-3, Fiber, Vitamins or Minerals bring a mild debuff, high Omega-3 gives Dolphin's Grace, and keeping every nutrient up gives Haste. Off by default (`builtinEffects`); datapacks can add their own.
- Click the Nutrients box on the Diet Screen for a detail view: level, trend, what you've eaten for it, and the best foods for it
- `/nourished nutrients` commands and an item editor Nutrients page for setting a food's nutrient values in game
- Protein and Carbs join the nutrients, and each fish has its own Omega-3 (salmon highest)
- Nutrient deficiencies get worse the longer they last (stronger debuff after 20 and 60 minutes)
- Optional nutrient HUD overlay with its own toggle key
- New Nutrients tab in the Nutritional Panel: an overview of every nutrient plus a page each with trend, recent sources and best foods
- Modpack authors can pin or hide foods in each nutrient's "Best sources" list (datapack, config, command or KubeJS)
- Saved daily intake history: 7-day charts for each nutrient and a new Food groups tab in the Nutrients screen/tab (follows the Calorie History setting)
- Item editor Info tab shows each ingredient and how much it added to the food's groups
- `/nourished diagnostics` now also lists broken datapack files, nutrient file errors and item ids that don't exist (typos)
- Foods with no recipe now get food groups from their name, using other mods' tagged items (a "kiwi smoothie" counts as fruit if any mod has a tagged kiwi)
- KubeJS: `NourishedNutrients` for adding nutrients, setting food values and reading/setting levels
- New in-game Settings window with the main feature switches and balance options (opened with its own keybind or from the Command Center)
- Command Center now has a Config tab that opens Settings, the full config and the Diet Screen editor
- Nutritional HUD's Home tab: Behavior/Style/Glow/Pulse/Colors, in its own styled box
- Nutritional HUD's Editor window chrome gets Style/Glow/Pulse like every module
- Nutritional HUD's Intake Breakdown tab scrolls with the mouse wheel when it overflows
- New "Tab color" slot for the Nutritional HUD's sidebar tabs
- Item editor: Browse Items (filter/search), Edited Items, Bulk Edit, Calories (custom calorie value), and Exclude Tooltip pages
- Item editor: per-item Silent Eat, Exclude from Recent Meals, Raw Food Tier override, and Diminishing Returns Off toggles
- Item editor: mod-wide "Diminishing Returns (All Food)" toggle
- New keybind to open the item editor directly, for dragging any item from JEI/EMI onto it
- **Calorie History and Activity Log tabs** in the Nutritional Panel, styled, moved and resized like every other tab

## 🔧 Changed

- The `config/nourished/` folder is tidier: nutrient and food value files are in `nutrition/`, effects and locks in `gameplay/`, raw food settings in `modules/raw_food/`. Existing files are moved over automatically on first launch.
- Diet Screen and HUD edit mode now use MariesLib's shared edit-box system: resizing past the panel's edge grows the panel instead of snapping the box back
- Item editor's Bulk Edit: Nutrient Group is now a per-group On/Off row instead of one Add/Remove row per click
- Item editor's scrollbars (Browse Items, etc.) can be grabbed and dragged, not just wheel-scrolled
- Item editor's recipe/3D preview for Create mod items shows the plain crafting-table recipe again (a regression had dropped it)
- Item editor's Options page split: calorie settings moved to their own Calories page

## 🐛 Fixed

- Turning off calorie history no longer hides today's calories on the Diet Screen.
- Cake and other foods eaten off a placed block (like modded pies) now give nutrition
- Nutritional HUD's Home tab showed "Join a world to see your character" instead of the 3D preview even with a player loaded
- An item excluded only via the item editor's toggle didn't show the "Excluded from nutrition tracking" tooltip line
- "Diminishing Returns Off" didn't apply correctly in modpacks running more than one MariesLib-based mod
- Item editor pages that change gameplay now correctly show read-only on a remote/dedicated server instead of silently no-opping

## 🗑️ Removed

- Item editor's standalone "Nutrient Groups" page (now covered by Bulk Edit's per-group rows and the Values page)
- Classic Diet Screen — Dynamic is now the only non-Hub option
- The separate on-screen Calorie History and Activity Log HUD boxes, their C/K edit keys and their HUD config options. They now live as tabs in the Nutritional Panel

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
