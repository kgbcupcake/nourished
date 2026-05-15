# Changelog

[0.2.0-beta] - 2026-05-14
Added

- Excluded items system — non-food items with FoodProperties (potions, soap, chicken feed, magic         essences, etc.) are now explicitly excluded from classification and will not show a Nourished
     tooltip

- excluded_items array in scanner_spec.json — modpack creators and datapack authors can extend this     list without code changes
  
-hamburger stem mapping and archetype entry for correct multi-nutrient resolution across all           hamburger variants

- sandwich archetype entry to inject grains signal for sandwich-type foods

- Community tag signals now propagate into the keyword/archetype scoring pass via StageContext          instead of short-circuiting the pipeline
   
- Average resolution time, slowest item, and recipe timeout tracking in CacheStats
  
- Raw scores, tokens, token weights, and rejected signal reasons added to ResolutionResult
  
- /nourished debug held command for in-game classifier inspection

Fixed

- Tag matches are now authoritative in blend resolution — when an item has an explicit nutrient tag,    the resolver can only contribute nutrients not already covered by the tag, preventing archetype       heuristics from overriding curated data

- Resolver cache is now invalidated on level load, fixing stale classifications persisting across jar swaps and config changes
  
-stew, soup, burger, and roast removed from PREPARATION_TOKENS — they are composite food forms, not    preparation methods, and were incorrectly penalizing their nutrient signals

- burger and hamburger archetypes now only contribute grains — proteins and vegetables are scored       from ingredient keywords and recipe inheritance, not hardcoded in the archetype

- Patchouli crafting recipe for the Nourished Guide

Changed

- Nutrient tag files updated — proteins, fruits, sugars, grains, and dairy now cover a significantly    broader range of modded foods including beverages, composite dishes, and items previously falling     through to hard fallback

-ScannerSpec extended with excludedItems set, parsed from scanner_spec.json and checked early in       both the tooltip and resolver paths

- StageContext converted from record to mutable class to support inter-stage signal propagation

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
