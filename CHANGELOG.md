# Changelog

<!-- markdownlint-disable MD013 -->

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
