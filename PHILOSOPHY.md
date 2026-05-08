# Nourished - API Philosophy

## What Nourished Is
Nourished is a nutrition engine and ecosystem layer for NeoForge 1.21.1. It is not just a nutrition mod. It is infrastructure other mods build on top of. The default gameplay experience (HUD, effects, decay, balancing) is one implementation of that engine, not the engine itself.

## What We Guarantee
Everything annotated `@ApiStatus.Stable` in `dev.maire.nourished.api` is a public contract. We will not remove or change stable method signatures without a major version bump and a deprecation cycle. Stable APIs will work across minor and patch releases.

## What We Don't Guarantee
Anything annotated `@ApiStatus.Experimental` may change between minor versions. Anything annotated `@ApiStatus.Internal` is not part of the public contract and may change or disappear at any time without notice. Do not depend on internal classes.

## What Addons Should Build Against
Only `dev.maire.nourished.api.*`. Never import from `dev.maire.nourished.diet`, `dev.maire.nourished.nutrition`, `dev.maire.nourished.effect`, `dev.maire.nourished.handler`, or any `api.impl` or `api.registry` subpackage. Those are internal.

## What Addons Should Avoid
Do not depend on specific nutrient keys like `"proteins"` or `"carbs"` being present. Query `NourishedAPI` at runtime. Do not depend on specific internal balancing values. Those evolve with gameplay. Do not depend on HUD layout or rendering internals. Do not reflect into internal classes.

## Configuration Layering
Nourished uses a four-layer config system: Java defaults -> TOML config -> config JSON files -> datapack JSON. Datapacks have highest priority. Addons should prefer datapack JSON for food classifications and compat entries rather than calling `registerFoodClassification` in code. This gives modpack authors override authority.

## Data vs Code
Prefer registering nutrients, effects, compat entries, and food classifications via datapack JSON over Java API calls where possible. Java API calls are for runtime-dynamic behavior only. Static definitions belong in data.

## Versioning
Nourished uses semantic versioning. Major version bumps may break `@Stable` APIs with a migration guide. Minor version bumps may evolve `@Experimental` APIs. Patch releases are stable. Check `NourishedAPIVersion.isCompatible(requiredMajor)` at startup if your addon requires a minimum API version.

## Ecosystem Intent
Nourished is meant to be the nutrition layer other mods integrate with, not a competing system. If your mod adds food, register your items with Nourished. If your mod affects player state, consider firing or listening to `NourishedEvents`. The goal is a coherent food ecosystem, not a walled garden.

## What Will Physically Separate Eventually
The engine (nutrition calculations, APIs, registries) and the gameplay layer (HUD, effects, overlays) will eventually ship as separate modules. Addons depending only on the engine will not need to pull in client-side rendering code. This separation is planned but not yet complete. Design your addon accordingly.

## Contact and Contributions
Repository: [https://github.com/kgbcupcake/nourished](https://github.com/kgbcupcake/nourished)  
Issue tracker: [https://github.com/kgbcupcake/nourished/issues](https://github.com/kgbcupcake/nourished/issues)

Compat PRs are welcome. New nutrient tag mappings for popular food mods are especially appreciated.
