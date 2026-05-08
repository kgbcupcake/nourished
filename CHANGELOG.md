# v0.1.2-alpha
Date: 2026-05-08

## Added
- API contract baseline established for addon integrations.
- Public API methods (NourishedAPI):
  - `getCalories(Player)`
  - `getNutrientLevel(Player, String)`
  - `getFoodMemory(Player)`
  - `getNutrition(Player, String)`
  - `getCalorieCount(Player)`
  - `getDietData(Player)`
  - `modifyNutrition(Player, String, float)`
  - `getVersion()`
  - `registerNutrient(NutrientDefinition)`
  - `addNutrient(NutrientDefinition)`
  - `registerFoodClassification(ResourceLocation, String, float)`
  - `registerFood(ResourceLocation, String, float)`
  - `registerCustomEffect(EffectDefinition)`
  - `addEffect(EffectDefinition)`
  - `registerCompatEntry(CompatDefinition)`
  - `addCompat(CompatDefinition)`
  - `registerNutrientSynergy(NutrientSynergyDefinition)`
  - `addNutrientSynergy(NutrientSynergyDefinition)`
  - `registerFoodSynergy(FoodSynergyDefinition)`
  - `addFoodSynergy(FoodSynergyDefinition)`
  - `registerDietProfile(DietProfileDefinition)`
  - `addProfile(DietProfileDefinition)`
  - `registerMilestone(NutrientMilestoneDefinition)`
  - `addMilestone(NutrientMilestoneDefinition)`
  - `registerSeasonHook(NourishedSeasonHook)`
  - `addSeasonHook(NourishedSeasonHook)`
  - `registerAbsorptionModifier(NutrientAbsorptionModifier)`
  - `addAbsorptionModifier(NutrientAbsorptionModifier)`
  - `registerReportProvider(DietReportProvider)`
  - `addReportSection(DietReportProvider)`
- Public API events:
  - `NourishedEvents.NutrientChangedEvent`
  - `NourishedEvents.NutrientCriticalEvent`
  - `NourishedEvents.NutrientExcessEvent`
  - `NourishedEvents.FoodEatenEvent`
  - `NutrientModifierEvent`
- Datapack API types:
  - `nourished/nutrients`
  - `nourished/food_classifications`
  - `nourished/effects`
  - `nourished/synergies`
  - `nourished/food_synergies`
  - `nourished/milestones`
  - `nourished/diet_profiles`
  - `nourished/compat`
  - `nourished/food_families`
  - `nourished/module_locks`

## Changed
- API stability metadata enforced:
  - Internal runtime infrastructure marked `@ApiStatus.Internal` across non-API integration/internal packages.
  - `dev.maire.nourished.api.impl` and `dev.maire.nourished.api.registry` package-level docs now explicitly mark them as internal.
- Added `@NourishedDeprecated` annotation for structured future deprecations (`since`, `replacement`, `reason`).
- Added package-level API docs (`package-info.java`) for:
  - `dev.maire.nourished.api`
  - `dev.maire.nourished.api.impl`
  - `dev.maire.nourished.api.registry`
- Runtime API readiness log now emitted after API registration closes, including registered nutrient/effect/compat counts.

## Removed
- None.

## API Notes
- This entry is the baseline contract document for `v0.1.2-alpha`.
- Any future breaking API/datapack/event changes must be logged here before release.
- Current intentionally evolving surfaces (marked `@ApiStatus.Experimental`):
  - `NourishedSeasonHook`
  - `NutrientAbsorptionModifier`
  - `NutrientSynergyDefinition`
  - `FoodSynergyDefinition`
  - `NutrientMilestoneDefinition`
  - `DietProfileDefinition`
# Changelog

All notable changes to Nourished will be documented here.

## [unreleased]

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

### 🚜 Refactor

- Remove globalized settings from NourishedConfigScreen
- Streamline configuration builder in NourishedConfigScreen
- Optimize configuration handling in NourishedConfigScreen
- Update constructors and validation in API definitions

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


