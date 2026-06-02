# Changelog

<!-- markdownlint-disable MD013 -->

## [ Nourished 0.2.5-beta.1 ]  Stability & API Patch - 2026-6-1

## This update focuses on runtime stability, API safety, and synchronization reliability across the nutrition system. It also includes internal cleanup and clarifications to the current state of experimental systems.

## Stability Fixes

- Diet synchronization corrected

- Fixed an issue where changes made through modifyNutrition() were not consistently synchronized to clients or followed by effect re-evaluation.

This could previously result in:

- temporary desync between server and client diet values
- missing or delayed application of nutrition-based effects

Nutrition state updates now correctly:

- sync to client when running on the server
- re-apply threshold-based effects when diet changes
- Nutrition effect consistency

- Ensured that nutrition threshold effects are properly re-evaluated after external or API-driven diet modifications, aligning behavior with internal gameplay pipelines.

## API Safety Improvements
- Null-safe API entry points
- Public API methods have been hardened against null player references to prevent crashes when called from external mods or scripts.
- This improves compatibility and prevents unexpected failures in integration scenarios.
- API lifecycle protection
- NourishedAPIState lifecycle control methods are now marked internal to prevent unintended external modification of:
- registry state
- datapack reload phases
- Public query methods remain unchanged.

##  Internal Cleanup
- Debug logging cleanup
- Temporary debugging logic in RecipeInheritanceStage has been removed, including targeted diagnostic logs used during development.
- Logging behavior has been restored to standard debug-level output only.

##  System Status Notes
- The following systems are currently registered but not fully wired into gameplay logic:
- Nutrient Synergies
- Food Synergy Bonuses
- Milestone Rewards
- These systems are exposed through the API and available for integration, but are not yet active in the core eating/progression pipeline. They are planned for future updates.

##  Compatibility
- No changes to:
- food classification behavior
- datapack structure
- existing configuration formats
- This update is fully backward compatible with existing saves.

## Summary
- This release improves:
- synchronization reliability
- API safety for external mods
- internal stability and logging cleanliness
- While also clarifying the current state of upcoming progression systems.

## Nourished 0.2.5-beta.1

A stability-focused patch improving consistency, safety, and integration reliability across the nutrition system.


## 0.2.5-beta

### Important Upgrade Notes

If updating from an earlier 0.2.x beta:

1. Delete `config/nourished/scanner_spec.json` before launching the game.
2. Load your world and run:

   ```
   /nourished invalidate_cache
   ```
3. Rejoin the world to rebuild cached nutrition data.

Existing worlds may continue using outdated scanner data until these steps are completed.

These steps ensure recipe inheritance, nutrient tags, and scanner data are regenerated using the latest compatibility improvements.

### Added

* Recipe inheritance now works client-side, allowing composite foods to display accurate multi-nutrient tooltips without requiring server-side lookups.
* Single-ingredient transformation recipes now inherit nutritional categories correctly (e.g. bread → bread slice, raw meat → cooked meat, apple → apple mash).

### Fixed

* Pam's HarvestCraft mixing bowl recipes and other custom recipe types are now discovered by the inheritance pipeline. Previously, only crafting and smelting recipes were supported.
* Pam's cooking containers (bakeware, cutting board, pot, saucepan, mixing bowl, skillet, juicer, grinder) and Croptopia cooking tools (frying pan, cooking pot, food press, knife) are no longer treated as nutritional ingredients during recipe inheritance.
* Non-food items such as salt, oils, spices, flavorings, yeast, water bottles, and alchemical ingredients are now ignored during inheritance instead of lowering confidence counts.
* Fruits such as berries and raspberries no longer incorrectly classify as raw foods.

### Compatibility

* Greatly expanded nutrient tag coverage across Pam's HarvestCraft 2 (Crops, Trees, Food Core, Food Extended), Croptopia, Create: Food, Farmer's Delight, Wilder Nature, Undergarden Delight, and additional food mods.
* Hundreds of composite foods now inherit and display more accurate multi-nutrient profiles.

## [0.2.4-beta] - 2026-05-31

### Added
- HUD bars temporarily reveal when a nutrient increases from eating — no need to open the
  nutrition screen to see what changed. Configurable via "Reveal HUD on nutrient gain" in
  HUD & Display settings.

### Fixed
- "Also show above threshold" and "Hide above threshold" now share consistent off-state
  semantics: `1.0` = disabled on both. Previously Show used `0.0` as off, opposite of Hide.
- Show threshold slider is now disabled until Hide is active, preventing the two sliders
  from conflicting silently.
- Existing configs with `hudShowAboveThreshold = 0.0` are automatically migrated to `1.0`
  on load — no manual action required.

[0.2.3-beta] - 2026-05-31

### Added
- Multi-nutrient recipe inheritance — complex dishes now contribute to multiple food groups based on their ingredients. Eating a steak sandwich gives Proteins, Grains, and Vegetables credit automatically.
- HUD hide-above threshold — bars above a configurable percentage are hidden from the HUD. Set to 0.4 to only see bars that need attention. as requested. in #3 
- HUD show-below threshold — bars below a configurable percentage are always shown regardless of other visibility settings.
- Show zero nutrients on HUD — toggle to show bars at 0% so you always know what you're missing.
- HUD background opacity — configurable from fully transparent to fully opaque.
- Vertical HUD layout — bars render as side-by-side columns filling upward instead of horizontal rows.
- HUD flash on nutrient increase — bars briefly highlight for 2 seconds when a nutrient increases from eating.
- Multi-nutrient full registry analysis — /nourished scan_analysis command analyzes all 4700+ classified foods and writes multi-nutrient recommendations, overlap matrix, ambiguity report, and scanner metrics to config/nourished/scanner_analysis/.
- Run Analysis button in the Scanner config tab triggers full registry analysis in game.
- Data Scan button renamed from Scan for clarity.

## Changed

- HUD settings now apply live without requiring Save & Quit.
- Pre-scan hint text removed from Scanner config tab.

### Fixed 
- HUD vertical layout and zero-bar visibility were reading stale config values due to Cloth Config write-on-save behavior — fixed with live config application.


## [0.2.2-beta] - 2026-05-30

### Added

- Configurable player join messages under `[general]` in `nourished-common.toml`:
  - `showJoinMessage` — toggle welcome and notice chat lines on login (default: `true`)
  - `joinMessageLine1` — welcome text after the NOURISHED header (default: `Welcome to the nutrition engine.`)
  - `joinMessageLine2` — secondary notice line (default: `Beta NOTICE - features and balance may change.`)
- Matching defaults in `data/nourished/config/common_defaults.json` for modpack and datapack overrides.
- KubeJS 2101 plugin discovery via `kubejs.plugins.txt` plus a `META-INF/services` service entry as a backup path.
- Manual KubeJS plugin bootstrap fallback when automatic discovery fails (for example under Architectury), invoked from mod initialization.
- `NourishedEvents` KubeJS script binding so scripts can subscribe to the `NourishedEvents` event group.
- `NourishedKubeJSEventBridge` — dedicated NeoForge → KubeJS bridge for runtime nutrition events.
- `ConfigReloadHandler.isReloadInProgress()` so handlers can defer work during config and datapack reloads.

### Changed

- **KubeJS (2101 migration):**
  - Migrated runtime events to the KubeJS 2101 `EventGroup` / `EventHandler` interface pattern.
  - Moved NeoForge subscription and event wrapping out of `NourishedKubeJSEvents` into `NourishedKubeJSEventBridge`.
  - Event payload `player` fields are now typed as `ServerPlayer` instead of `Object`.
- Join welcome and beta notice messages now read from config while keeping the existing styled chat formatting (◆ NOURISHED ◆ header, ⚠ notice line, and bold/light split when `joinMessageLine2` contains ` - `).
- Replaced hardcoded `"nourished"` mod id strings with `Nourished.MODID` across configs, registries, compat hooks, and tooling.
- Registry reload pipeline hardened with read/write locks on `AbstractRegistry` and `ListRegistry` so reset → repopulate → freeze is atomic during `/reload`.
- Datapack and config loaders now log warnings on swallowed `IOException`s instead of failing silently.

### Fixed

- **KubeJS integration not loading** on KubeJS 2101 — Nourished scripts and bindings were unavailable when plugin discovery missed the jar; fixed with `kubejs.plugins.txt`, service-loader entry, and manual bootstrap registration.
- **KubeJS nutrition events not firing** after the 2101 API change — event registration and bridging now follow the current KubeJS event-group pattern.
- **Nutrition effects applying during reload** — effect application and player-tick effect handlers now skip work while a config or datapack reload is in progress, avoiding inconsistent effect state mid-reload.

## [0.2.1-beta-HotFix] - 2026-05-29

### Fixed

- Restored config screen left-sidebar navigation so **Modules**, **General**, and other category tabs respond to clicks again.

## [0.2.1-beta] - 2026-05-29

### Upgrade Notes

If upgrading from 0.2.0-beta or earlier, delete:

- `config/nourished/scanner_spec.json`
- `config/nourished/nutrients.json`

Both files will regenerate on launch.

If your world contains an older `nourished-generated` datapack, run:

`/nourished repair_generated_datapack`

then:

`/reload`

---

### Added

- Raw Food Penalties
- Raw foods can now apply effects and nutrient penalties.
- Four severity levels: Fine, Mild, Medium, Severe.
- Settings are configurable in `config/nourished/raw_food.json`.

- Gut Flora
- Repeated raw food consumption increases sensitivity to future penalties.
- Recovery occurs through cooked and varied diets.
- Stored separately from the nutrition system.

- Non-Beneficial Nutrients
- Nutrients can now be marked as harmful when accumulated in excess.
- HUD indicators, toasts, and threshold checks account for this behavior.

- Raw Food configuration options added to the Cloth Config screen.

- Compat integrations now appear in dedicated config categories.
- Large Stamina Overhaul
- Peak Stamina
- Spice of Life: Onion

- New Food Safety chapter in the Patchouli guide.

- Nutrient schema validation.
- Outdated `nutrients.json` files are automatically regenerated when required fields are missing.

- `/nourished repair_generated_datapack`
- Repairs generated nutrient tags after nutrient definition changes.

- Expanded nutrient tag coverage for vanilla and supported food mods.

- Added:
- `PHILOSOPHY.md`
- `ARCHITECTURE.md`

---

### Changed

- Returned to five nutrient groups:
- Fruits
- Vegetables
- Proteins
- Grains
- Dairy

- Removed the Sugars category.
- Existing sugar-tagged foods have been reassigned.

- Legacy player data containing removed nutrient keys is now migrated automatically.

- Documentation updated for the five-group system.

---

### Fixed

- Raw vanilla meats now correctly trigger raw food penalties when appropriate.

- Non-beneficial nutrients no longer trigger low or critical nutrient warnings while decaying.

## [0.2.0-beta] - 2026-05-15

⚠️ Upgrading from 0.1.x? Delete config/nourished/scanner_spec.json before launching. It will regenerate automatically with the new defaults. Keeping the old file will cause missing archetypes and incorrect classifications.

---

### Added

- Excluded items system — non-food items with FoodProperties (potions, soap, chicken feed, magic essences, etc.) are now explicitly excluded from classification and will not show a Nourished tooltip

- excluded_items array in scanner_spec.json — modpack creators and datapack authors can extend this list without code changes

- hamburger stem mapping and archetype entry for correct multi-nutrient resolution across all hamburger variants; sandwich archetype entry to inject grains signal for sandwich-type foods

- sandwich, burger, and vegan keyword entries in scanner_spec.json for improved composite classification

- Community tag signals now propagate into the keyword/archetype scoring pass via StageContext instead of short-circuiting the pipeline

- Async scanner classification on server start — untagged foods are resolved off-thread and applied to both tooltip and eating paths; average resolution time, slowest item, and recipe timeout tracking in CacheStats

- Raw scores, tokens, token weights, and rejected signal reasons added to ResolutionResult

- /nourished debug held command for in-game classifier inspection; full pipeline trace written to config/nourished/debug/

- /nourished invalidatecache command for operators to force a fresh scan without restarting

- Croptopia compatibility — full nutrient tag coverage for 200+ items including meals, produce, seafood, desserts, and drinks

- Pam's HarvestCraft 2 compatibility — 700+ items tagged across all six nutrient categories

- camelCase token splitting for mods that use concatenated item names (e.g. pamhc2foodextended) — improves scanner classification for previously unresolvable items

### Fixed

- Tag matches are now authoritative in blend resolution — when an item has an explicit nutrient tag, the resolver can only contribute nutrients not already covered by the tag, preventing archetype heuristics from overriding curated data

- Resolver cache is now invalidated on level load, fixing stale classifications persisting across jar swaps and config changes

- stew, soup, burger, and roast removed from PREPARATION_TOKENS — they are composite food forms, not preparation methods

- burger and hamburger archetypes now only contribute grains — proteins and vegetables are scored from ingredient keywords and recipe inheritance

- Patchouli crafting recipe for the Nourished Guide

### Changed

- Nutrient tag files updated — proteins, fruits, grains, and dairy now cover a significantly broader range of modded foods including beverages, composite dishes, and items previously falling through to hard fallback

- compositeRatioThreshold default lowered from 0.5 to 0.4 for better composite detection on borderline foods

- ScannerSpec extended with excludedItems set, parsed from scanner_spec.json and checked early in both the tooltip and resolver paths

- StageContext converted from record to mutable class to support inter-stage signal propagation

- Scanner classifications now store full nutrient maps instead of single dominant keys, and are merged into tooltip rendering as well as eating

## [0.1.9-beta] - 2026-05-13

### Added

- Recipe inheritance for multi-ingredient foods
- Debug logging with classifier accuracy tracking
- Nourished Guide given to players on first join, with crafting recipe

### Fixed

- Tooltip now correctly shows diminishing returns value
- Tag-matched items return immediately without running the classifier

### Security

- Path traversal fix in FoodScannerWidget
- NaN/Infinity hardening across external inputs
- Packet size limits added
- SHA-256 replaces MD5 in mod list hashing

## [0.1.8-beta] - 2026-05-13

### Fixed

- Unclassified items now display correctly in tooltips instead of showing no information
- Dominant nutrient category logic refined

## [0.1.7-beta] - 2026-05-13

### Added

- New food items added to grains and proteins tag lists

### Changed

- Patchouli guide updated

## [0.1.6-beta] - 2026-05-13

### Added

- All six food group entries expanded with vanilla food sources, farming tips, and rotation strategy
- Tips & Tricks chapter covering daily routine, efficient farming, emergency recovery, multiplayer, and tooltip reading
- Compat Mods chapter covering Croptopia, Farmer's Delight, Pam's HarvestCraft 2, Herbs & Harvest, Farm & Charm, LSO, Spice of Life: Onion, and Peak Stamina
- JEI tag search tip for nourished:nutrients/ filter
- Cross-link from Your First Day to Tips & Tricks

### Changed

- Effects entry expanded with full per-group bonus and penalty breakdown
- Diminishing Returns entry expanded with three-tier explanation and novelty bonus details

## [0.1.5-beta] - 2026-05-12

### Fixed

- Diminishing returns memory window and decay grace period tuning

## [0.1.4-beta] - 2026-05-12

### Fixed

- Example datapack files removed from main mod jar

## [0.1.3-beta] - 2026-05-12

### Changed

- DietScreen layout simplified, excess legend entries removed

## [0.1.2-beta] - 2026-05-11

### Fixed

- Clarified blockHeavyMeals and blockLightFood config descriptions for servers without Spice of Life: Onion

## [0.1.1-beta] - 2026-05-11

### Added

- Heavy meal nutrition threshold configuration
- Datapack diagnostics and validation framework

### Fixed

- Memory window configuration values
- Mod logo loading in NourishedConfigScreen
- Release workflow indentation

### Changed

- Registry lifecycle management centralized
- DietScreen layout improvements

## [0.1.0-beta] - 2026-05-11

Initial beta release.
