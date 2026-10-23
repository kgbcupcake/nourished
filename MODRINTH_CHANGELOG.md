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
