# Changelog

All notable changes to Nourished will be documented here.

## [unreleased]

### 📖 Documentation

- Enhance API documentation and registration guidelines
- Added ARCHITECTURE.md
- Expand ARCHITECTURE.md with detailed override priority stack and clarifications on mod functionality
- Update food group entries and nutrition HUD in Patchouli guide
- Enhance README and implement Peak Stamina compatibility features

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

### 🚜 Refactor

- Remove globalized settings from NourishedConfigScreen
- Streamline configuration builder in NourishedConfigScreen
- Optimize configuration handling in NourishedConfigScreen
- Update constructors and validation in API definitions
- Streamline nutrient and effect definitions with builder pattern
- Enhance registration phase management in NourishedAPI

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


