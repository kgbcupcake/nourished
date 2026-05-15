# Changelog

## [0.2.0-beta] - 2026-05-14

### Added
- Archetype-based composite classification — foods like stews, burgers, and curries now correctly resolve multiple nutrients instead of falling back to a single category
- Community tag signals now feed into the keyword/archetype scoring pass instead of short-circuiting it, fixing single-nutrient results for explicitly tagged items
- `hamburger` stem and archetype entry so Farmer's Delight hamburgers resolve correctly
- Average resolution time, slowest resolution time, and recipe timeout tracking in CacheStats
- Raw scores, tokens, token weights, and rejected signals added to ResolutionResult for better debug output
- Debug item command for in-game classifier inspection

### Fixed
- `stew`, `soup`, `burger`, and `roast` removed from preparation token penalty list — they're composite foods, not preparation methods
- Patchouli crafting recipe for the Nourished Guide

### Changed
- StageMath normalization now includes rejection reasons for missing nutrients


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
