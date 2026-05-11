# Changelog

All notable changes to Nourished will be documented here.

## [0.1.0-beta] - 2026-05-10

### 🐛 Bug Fixes

- Fix config menu keybind not firing (OPEN_CONFIG was bound to GLFW_KEY_UNKNOWN)
- Fix eating at full vanilla hunger not triggering nutrition-only bypass
- Fix mob effects flickering by tracking which effects Nourished applied
- Fix effects not saving when using the config screen
- Fix eating animation — now uses vanilla eat animation via startUsingItem

### 🚀 Features

- Nutrition-only eating at full hunger — any food can contribute to nutrients without restoring hunger
- Configurable block toggles: blockHeavyMeals and blockLightFood for server admins
- Per-player cooldown (20 ticks) on nutrition-only eating to prevent spamming
- Add block heavy meals and block light food configuration options
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
- Enhance UI responsiveness and button visibility in configuration widgets
- Introduce core diet and nutrition management system

### 🚜 Refactor

- Remove globalized settings from NourishedConfigScreen
- Streamline configuration builder in NourishedConfigScreen
- Optimize configuration handling in NourishedConfigScreen
- Update constructors and validation in API definitions
- Streamline nutrient and effect definitions with builder pattern
- Enhance registration phase management in NourishedAPI
- Streamline icon handling and improve nutrient icon fallback logic

### 📖 Documentation

- Enhance API documentation and registration guidelines
- Added ARCHITECTURE.md
- Expand ARCHITECTURE.md with detailed override priority stack and clarifications on mod functionality
- Update food group entries and nutrition HUD in Patchouli guide
- Enhance README and implement Peak Stamina compatibility features
- Expand ARCHITECTURE.md with new sections on memory, fatigue, debt, and category management
- Establish API contract and enhance documentation for v0.1.2-alpha

## [0.1.2-alpha] - 2026-05-08

### 🚀 Features

- Expand nutrient item lists across various categories
- Enhance food scanning functionality and configuration options
- Implement lightweight delta sync for diet updates

### 🚜 Refactor

- Remove deprecated Scanner.md and Nourished.java files

## [0.1.1-alpha] - 2026-05-08

### 🚀 Features

- Update diet system with recent food tracking and neglected categories

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
