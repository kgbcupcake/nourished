# Changelog

All notable changes to Nourished will be documented here.

## [0.2.0-beta] - 2026-5-14

feat: Enhance nutrient resolution and debugging capabilities in RuntimeFoodResolver

- Added new metrics to CacheStats for average resolution time, slowest resolution time, and recipe timeouts.
  
- Updated ResolutionResult to include raw scores, tokens, token weights, and rejected signals.
  
- Improved normalization process in StageMath to provide rejection reasons for missing nutrients.
 
- Integrated debug commands in NourishedCommand for better diagnostics.
  
- Enhanced various resolution stages to utilize new data structures for improved nutrient scoring and rejection handling.

- Fixed Patchouli Crafting recipe for Nourished Guide






## [0.1.9-beta.1] - 2026-05-13

### 🚀 Features


- Recipe inheritance for multi-ingredient foods
  
- Debug logging system with classifier accuracy tracking

- Nourished Guide given to players on first join
  
- Crafting recipe for Nourished Guide
  
Fixed

-Tooltip now correctly shows diminishing returns value

-Tag match returns immediately without running classifier

Security

- Path traversal fix in FoodScannerWidget
  
- NaN/Infinity hardening across all external inputs
  
- Packet size limits to prevent malicious server OOM
  
- SHA-256 replaces MD5 in mod list hashing


## [0.1.8-beta] - 2026-05-13

### 🚀 Features

- Improved tooltip display in ClientEvents and NourishedFoodTooltipHelper to handle unclassified items.

- Refined logic for determining dominant nutrient categories in food items.

- Added new localization entry for unclassified tooltip in en_us.json."


## [0.1.7-beta] - 2026-05-13

### 📖 Documentation

- Update Patchouli guide


### 🚀 Features

- Add new food items to grains and proteins categories


### 🚜 Refactor

- Update food group entries and effects in the Nourished guide


## [0.1.6-beta] - 2026-05-13

## 🚀 Features

- Added
  
- Expanded all six food group entries with vanilla food source lists, farming tips, and rotation strategy pages

- New Tips & Tricks chapter with Daily Routine, Efficient Farming, Emergency Recovery, Multiplayer & Adventure, and Reading Tooltips entries

- New Compat Mods chapter covering Croptopia, Farmer's Delight, Pam's HarvestCraft 2, Herbs & Harvest, Farm & Charm, Legendary Survival Overhaul, Spice of Life: Onion, and Peak Stamina

- JEI tag search tip explaining nourished:nutrients/ filter

- Cross-link from Your First Day entry to Tips & Tricks chapter

- Changed

- Effects entry in Getting Started expanded with full per-group bonus and penalty breakdown

- Diminishing Returns entry expanded with three-tier DR system explanation and novelty bonus details




## [0.1.5-beta] - 2026-05-12

## 🐛 Bug Fixes

- Improve diminishing returns memory window and decay grace period

- Improve diminishing returns memory window and decay grace period

- Improve diminishing returns memory window and decay grace period



## [0.1.4-beta] 

## 🐛 Bug Fixes

-  remove example datapack files shipping with main mod"


## [0.1.3-beta] - 2026-05-12

### 🚜 Refactor

- Simplify DietScreen layout and remove excess legend entries


## [Nourished v0.1.2-beta] - 2026-05-11

🐛 Bug Fixes

Clarify blockHeavyMeals and blockLightFood config descriptions for servers without Spice of Life: Onion






## [0.1.1-beta] - 2026-05-12

### 🐛 Bug Fixes

- Update memory window configuration values

- Improve mod logo loading logic in NourishedConfigScreen

- Release workflow indentation


### 🚀 Features

- Add heavy meal nutrition threshold configuration

- Implement datapack diagnostics and validation framework


### 🚜 Refactor

- Centralize registry lifecycle management

- Enhance DietScreen layout and changelog generation

## [0.1.0-beta] - 2026-05-11

### 📖 Documentation

- Enhance API documentation and registration guidelines

- Added ARCHITECTURE.md

- Expand ARCHITECTURE.md with detailed override priority stack and clarifications on mod functionality

- Update food group entries and nutrition HUD in Patchouli guide

- Enhance README and implement Peak Stamina compatibility features

- Expand ARCHITECTURE.md with new sections on memory, fatigue, debt, and category management


### 🚀 Features

- Enhance configuration and UI for module management

- Add GitHub Actions workflow for automated releases

- Enhance mod compatibility and configuration UI

- Improve compatibility management and UI enhancements

- Enhance NourishedConfigScreen with new HUD features and UI improvements

- Implement nutrition system functionality in NourishedAPI

- Register NourishedDataManager reload listener in ConfigReloadHandler

- Enhance NourishedCommand with new nutrient and profile commands

- Add compatibility plugins for JEI, REI, and EMI in ClientEventRegistrar

- Add KubeJS integration support in Nourished

- Include example-addon in settings.gradle

- Improve modularity with example-addon integration

- Establish API contract and enhance documentation for v0.1.2-alpha

- Enhance UI responsiveness and button visibility in configuration widgets

- Introduce core diet and nutrition management system

- Add block heavy meals and block light food configuration options

- Integrate effect saving and streamline nutrition eating mechanics


### 🚜 Refactor

- Remove globalized settings from NourishedConfigScreen

- Streamline configuration builder in NourishedConfigScreen

- Optimize configuration handling in NourishedConfigScreen

- Update constructors and validation in API definitions

- Streamline nutrient and effect definitions with builder pattern

- Enhance registration phase management in NourishedAPI

- Streamline icon handling and improve nutrient icon fallback logic

## [0.1.1-alpha] - 2026-05-08

### 🚀 Features

- Update diet system with recent food tracking and neglected categories

## [0.1.2-alpha] - 2026-05-08

### 🚀 Features

- Expand nutrient item lists across various categories

- Enhance food scanning functionality and configuration options

- Implement lightweight delta sync for diet updates


### 🚜 Refactor

- Remove deprecated Scanner.md and Nourished.java files

## [0.1.0-alpha] - 2026-05-07

### 🐛 Bug Fixes

- Make optional nutrient tag entries so TagLoader keeps proteins/dairy/grains/sugars

- Optional nutrient tag entries, add croptopia/ends_delight/createfood proteins coverage

- Improve row height calculation in EffectBuilderWidget

- Adjust layout and rendering in EffectBuilderWidget and FoodScannerWidget

- Update diet screen messages and localization

- Tune burst multipliers and localize diet tip text

- Improve dynamic tip box rendering in DietScreen

- Update MainGui.png for improved visual design


### 🚀 Features

- Initial commit — Nourished mod v0.1.0

- HUD edit mode, drag-and-drop icons, and config expansion

- Enhance HUD edit mode and diet screen functionality

- Enhance build configuration and nutrition handling

- Validate effect ID input in EffectBuilderWidget

- Update diet button rendering and tooltip messages

- Enhance client event handling and registration

- Implement food memory tracking and tooltip enhancements

- Add nutrition effects handling to player events

- Add nutrient gain configuration options

- Add active effects display to DietScreen


### 🚜 Refactor

- Enhance diet screen UI and update language strings


