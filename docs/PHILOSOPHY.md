# Nourished — API Philosophy

## What Nourished Is

Nourished is a nutrition gameplay mod for NeoForge 1.21.1. It tracks food-group balance, rewards variety, and applies configurable buffs and debuffs. Under the hood it runs on MariesLib, the engine, scanner, and tracking layer live there. Nourished is the food-domain implementation players actually install.

If you're building a new Marie mod with your own value bars (not nutrition), depend on [MariesLib](https://github.com/kgbcupcake/MarieLib/blob/main/PHILOSOPHY.md) directly.

## What We Guarantee

`dev.maire.nourished.api.NourishedAPI` is `@ApiStatus.Stable`. Method signatures on that class won't break without a major version bump and deprecation cycle.

Shared types (`ValueDefinition`, `ThresholdEffect`, `CompatDefinition`, etc.) follow MariesLib's stability rules.

## What We Don't Guarantee

KubeJS bindings (`NourishedKubeEvents`) are `@Experimental`.

Anything in `dev.maire.nourished.core`, `client`, `config`, `compat`, or `api.impl` is internal. Same for MariesLib internals — see MarieLib PHILOSOPHY.

## What Nutrition Addons Should Build Against

- `NourishedAPI` for registering nutrients, food mappings, and hooks
- MariesLib API types for definitions (`ValueDefinition`, `ThresholdEffect`, …)
- `MarieEvents` for NeoForge event subscriptions
- Datapacks for static food classification

Do not import Nourished or MarieLib implementation packages.

## What Addons Should Avoid

Don't hardcode nutrient keys like `"proteins"` — query at runtime via `getTrackingData()` or `NutrientRegistry`.

Don't depend on HUD layout internals or gut health implementation details unless you're intentionally coupling to those modules.

Don't reflect into internal classes.

## Configuration Layering

Java defaults → TOML → config JSON → datapack JSON. Datapacks win for food classification. Server owners tune mechanics in config. Modpack makers override classifications in datapacks.

## Data vs Code

Ship food classifications and balance in datapacks when you can. Java registration is for dynamic or addon-mod-time definitions.

## Versioning

Nourished tracks MariesLib's API version (`MarieAPIVersion`). Both mods must be present at runtime. Check compatibility at startup if your addon depends on a minimum version.

## Ecosystem Intent

Nourished is the nutrition layer other food mods integrate with. Register your items. Listen to events. The goal is one coherent diet system across a modpack, not competing nutrition mods.

## Engine Separation

Done. MariesLib is the engine. Nourished is gameplay. Addons that only need generic value tracking don't need Nourished on the classpath.

## Contact

Repository: [https://github.com/kgbcupcake/nourished](https://github.com/kgbcupcake/nourished)

Compat PRs welcome — nutrient tag mappings for popular food mods are especially useful.
