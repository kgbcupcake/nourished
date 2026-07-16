# Changelog

<!-- markdownlint-disable MD013 -->

## [ Unreleased ]

### CI / Tooling

- Added `check-marielib-update.yml` GitHub Actions workflow to check MarieLib package updates weekly (Mondays 12:00 UTC) or via manual dispatch.
- Workflow queries GitHub Packages Maven metadata and opens a PR against `dev` when a newer MarieLib version is detected.
- Update process requires `MARIELIB_PACKAGES_TOKEN` (PAT with `read:packages`) and does not auto-merge or target `main`.

---

[ nourished 0.2.7-beta] - 2026-07-13

### Added

- Restored classic (pre-MarieUI) HUD and Diet Screen renderers behind new `hudClassicMode` / `dietScreenClassicMode` config toggles, reusing the shared drag/resize edit-mode infrastructure instead of reviving the old hand-rolled edit screens.
- Added `DietPanelLayoutResolver` and `DietSubBoxConstraints` for resolving diet panel layout and left-column sub-box resize constraints from persisted state.
- Added `BalanceComponent` and `CaloriesComponent` as independent, self-positioning Diet Screen modules.
- Added per-item food override support so a datapack override can replace both nutrient bar weights and full source deltas (calories + nutrients) for an item.
- Added free spatial HUD panel resizing on every edge and corner, independent of content scale — resizing the box now reserves margin/free space around fixed-size content instead of rescaling it, mirroring how the Diet Screen panel already behaved.
- Added `GuiGraphicsRenderContext.graphics()` escape hatch so classic renderers can issue raw `GuiGraphics` calls from within a MarieUI-managed render context.
- Added community-tag and keyword-suffix fallback classification inside recipe inheritance ingredient scoring, so ingredients missing a confirmed nutrient tag can still contribute via those stages when confidence is high enough, backed by a much larger built-in keyword-suffix dictionary.
- Added `excluded_items.json` to fully exclude specific items from nutrient tracking (checked before tag matching, external classification, and runtime inference) — for decoy items or non-food edibles that shouldn't move any bar, independent of `food_overrides.json`'s value corrections. Vanilla hunger/saturation restoration is unaffected.
- Added graceful overflow handling for Diet Screen left-column sub-boxes (Calories, Balance, Eat More, Recent Meals, Active Effects): shrinking the panel now collapses each box to header-only, drops rows/lines one at a time, or smoothly shrinks its content (Eat More's icon row) as space runs out, instead of the whole box popping in/out the instant it no longer fits at full size. The right-column intake legend now anchors directly below the last drawn row instead of a fixed offset from the panel's bottom edge.
- HUD nutrient bars/columns are now centered within the panel box when it's resized larger than its content needs, in both row-stacked and column layouts.

### Changed

- Reorganized `client/hud/` and `client/screen/diet/` into `dynamic/{layout,modules,edit,visibility,persistence}` and `classic` packages to separate MarieUI and legacy UI implementations.
- HUD nutrient panel background now renders with rounded corners to match the classic renderer.
- Updated imports across API, config, context, effect, handler, nutrition, kubejs, and template classes to match MarieLib's restructured package layout (e.g. `dev.marie.framework.api.value`, `.effects`, `.marieapi`, `.progression`, `.reporting`, `.source`).
- Renamed `DeathNutritionBehavior` to `RespawnValueBehavior` (MarieLib rename) and updated all references.
- Renamed `MarieApiRegistries.freezeModOnlyRegistriesAfterCommonSetup` to `freezeValueTrackingOnlyRegistriesAfterCommonSetup`.
- Removed the per-item resolution cache from `RuntimeFoodResolver` in favor of always resolving uncached, now that ingredient scoring can consult the community-tag/keyword-suffix stages.
- Moved config overrides (`food_overrides.json`, `source_classifications.json`, `excluded_items.json` and their READMEs) from `config/nourished/` directly into a new `config/nourished/overrides/` subfolder. **Breaking:** update any datapacks/scripts that read or write these files at the old path.

### Fixed

- Fixed the Diet Screen open keybind so pressing it while a Diet Screen (classic or MarieUI) is already open now closes it instead of leaving a duplicate/reopened screen.
- Fixed classic HUD/Diet Screen left-edge resize so shrinking the panel back down actually reduces the reserved left margin instead of getting stuck at the width that created it.
- Fixed classic Eat More panel resize clamping so it cannot grow large enough to push Active Effects below its minimum rendering budget within the fixed left-column layout.
- Fixed The `food_overrides.json` not being wired into the `RuntimeFoodResolver` so overrides were not being applied at runtime.

## Notes

> A lot has changed in this update and some of the changes are breaking. Please read the changelog carefully and check your configs and datapacks for any necessary updates. this also include MariesLib updates aswell whicj there was a massive refactor of the package structure and some class renames. Please check the MariesLib changelog for more details.

### Removed

- Removed `TempRuntimeFoodTraceCommand` from `/nourished` command registration.

## [ Nourished 0.2.6-beta.5 ] - 2026-06-29

### Added

- Added `enableDiminishingReturns` master toggle in Advanced config to disable diminishing returns globally.
- Added config screen live sync: Save / Save All now re-broadcasts config to clients in integrated singleplayer without requiring reload or rejoin.

### Changed

- Nutrient-tag-only items are now recognized as valid food sources via fallback to `FoodNutritionRegistry.getNutrientTagScores`.
- Simplified `RecipeInheritanceStage` by removing multi-threshold filtering; unmatched keys now report `REJECT_NO_MATCHING_KEYWORDS`.
- Updated `gradle.properties` mod description to remove version-specific MarieLib changelog references.

### In Progress

- Added example source synergy datapack entries (`hearty_meal`, `balanced_plate`, `breakfast`) under `data/nourished/nourished/source_synergies/`.
- Source synergies are not yet active in runtime logic.

---

## [ Nourished 0.2.6-beta.4 ] - 2026-06-27

### Added

- Added per-item nutrient weight system via `NutrientWeightRegistry`.
- Added datapack support for weights under `data/<namespace>/nourished/config/weights/`.
- Added bundled weight presets for Farmer’s Delight, Croptopia, and Pam’s HarvestCraft 2.
- Added `SOURCE_CLASSIFICATIONS_README.md` documenting classification override schema.
- Added `NourishedExportCommands` as dedicated export subsystem.

### Removed

- Removed compat integration classes from Nourished (moved to MarieLib):
- LSOCompat
- PeakStaminaCompat
- SpiceOfLifeOnionCompat
- Removed compat toggles from `nourished-common.toml` and config UI.
- Removed `mod_compat.json` (replaced by `source_classifications.json`).
- Removed `SourceValuesValidator`.
- Removed `/nourished validate` command (replaced by MarieLib validation system).

### Changed

- Replaced `mod_compat.json` with `source_classifications.json` as canonical source definition file.
- Renamed `SourceOverridesValidator` → `SourceClassificationsValidator`.
- Delegated recipe inheritance fully to MarieLib `RecipeInheritanceResolver`.
- Hardened `NourishedPresetRegistry.applyPresetValues` to support partial presets safely.
- Updated override README to clarify export workflows.
- Reorganized nutrient tag bundles for consistency with weight system and audit results.

### MarieLib & Build

- Updated MarieLib dependency to `0.1.1-beta.2`.
- Requires MarieLib for:
- RecipeInheritanceResolver indexing
- Compat handling
- Validation pipeline
- Export system APIs

---

## [ Nourished 0.2.6-beta.3 ] - 2026-06-21

### Added

- Added full nutrient export system via `NutrientExportResolver`.
- Added `/nourished export_all` command for categorized exports.
- Added GUI Export All Foods button in Scanner tab.
- Added `OVERRIDES_README.md` auto-generation.
- Added per-nutrient response curve system (`FLAT`, `DIMINISHING`, `CONFIDENCE_GATED`, `SYNERGY`).
- Added config validation framework using 10 MarieLib validators.
- Added `/nourished validate` command (server-side validation reporting).
- Added tag audit system:
- `/nourished audit_tags`
- `/nourished audit`
- `/nourished tag`
- Added `/nourished set_all` debug utility for nutrient simulation testing.

### Fixed

- Fixed legacy nutrient color fallback (white ARGB) auto-repair.
- Fixed KubeJS nutrient registration desync with ValueRegistry.
- Fixed scanner UI stale state after world exit.
- Fixed misclassified Fruits Delight items (durian, hawberry_roll, pear_with_rock_sugar).
- Fixed `/nourished tag` crash due to missing report writer class.

### Changed

- Made tag audit output file-only (no chat spam).
- Consolidated export output structure under `nourished_nutrients_export/`.
- Clarified `/marieslib dump` vs `/nourished export_all` responsibilities.

### MarieLib & Build

- Requires MarieLib `0.1.1-beta.1+`.
- Migrated validation, export, and audit systems to MarieLib APIs.

---

## [ Nourished 0.2.6-beta.2 ] - 2026-06-16

### Added

- Added full milestone system (18 nutrient milestones + balanced global milestone).
- Added datapack milestone loading via MarieLib reload listeners.
- Added Diet Screen edit mode (drag/resize UI system).
- Added HUD nutrient color editor with live preview.
- Added template export commands:
- `/nourished export_effects_template`
- `/nourished export_values_template`
- `/nourished export_colors_template`
- Added nutrient progress tooltips for milestone tracking.

### Changed

- Default Diet Screen keybind set to `N`.
- Refactored HUD rendering to use registry-driven color system.
- Migrated milestone thresholds to corrected cumulative values.
- Improved nutrient registry reload safety and fallback behavior.
- Switched to `marie_schema_version` across datapacks.

### Fixed

- Fixed HUD color override reset issues.
- Fixed datapack effect duplication.
- Fixed decay override config not applying correctly.
- Fixed KubeJS nutrient registration loss after reload.
- Fixed translation keys in tracking screen.

### MarieLib & Build

- Requires MarieLib `0.1.0-beta.5+`.

---

## [ Nourished 0.2.6-beta.1 ] - 2026-06-14

### Added

- Added `deathNutritionBehavior` configuration (preserve, reset, vanilla_half).
- Added datapack milestone loading system.
- Added sample milestone definitions.

### Removed

- Removed legacy stamina module (~2200 lines).

### Changed

- Migrated compat integrations to MarieLib ownership model.
- Hardened registry lifecycle and preset initialization.
- Updated scanner spec schema to MarieLib format.
- Introduced `NourishedPresetRegistry`.

### Fixed

- Fixed preset initialization crash during registry lifecycle.
- Fixed effect plugin loading order issues.

---

## [ Nourished 0.2.5-beta.5 ] - 2026-06-09

### Added

- Migrated core architecture to MarieLib 1.0.0+ dependency model.
- Added KubeJS event system integration.
- Added nutrient event hooks (`nutrientChanged`, `foodEaten`, etc.).
- Added raw food penalty scripting hooks.
- Added plugin-based API bridge for external mods.

### Changed

- Renamed module toggles to match MarieLib cache system.
- Migrated scanner spec schema to MarieLib format.
- Centralized compat system ownership.
- Refactored preset system into MarieLib delegation model.

### Fixed

- Fixed config toggle desync issues.
- Fixed KubeJS plugin discovery failure in 2101 API.
- Fixed reload-time effect application inconsistencies.

---

## [ Nourished 0.2.5-beta.4 ] - 2026-06-06

### Added

- Added multiplayer config snapshot system.
- Added nutrition sync lifecycle states (UNINITIALIZED → PENDING → ACTIVE).
- Added gut health toggle system.

### Changed

- Moved diet simulation parameters into snapshot model.
- Separated client and server config authority.
- Introduced protocol versioning for network sync.

### Fixed

- Fixed config override desync in multiplayer sessions.
- Fixed stale client config leakage between worlds.
- Fixed missing snapshot injection causing incorrect simulation state.

---

## [ Nourished 0.2.5-beta.3 ] - 2026-06-03

### Added

- Added classification tracing system (`ClassificationTrace`).
- Added recipe inheritance diagnostics.
- Added confidence scoring and signal tracing.
- Added held-item classification debugging tools.

### Changed

- Improved classification pipeline observability.
- Standardized recipe failure reasons.
- Consolidated config reload lifecycle handling.

---

## [ Nourished 0.2.5-beta.2 ] - 2026-06-02

### Fixed

- Fixed Diet Screen blur interaction issue.

---

## [ Nourished 0.2.5-beta.1 ] - 2026-06-01

### Added

- Added nutrition sync reliability improvements.
- Added API safety hardening for external mods.
- Added effect re-evaluation on diet updates.

### Changed

- Cleaned debug logging in recipe pipeline.
- Clarified experimental system status (synergies, milestones).

---

## [ 0.2.5-beta ] - 2026-06-01

### Added

- Added multi-ingredient recipe inheritance system.
- Added initial scanner analysis tools.

### Fixed

- Fixed Pam’s HarvestCraft compatibility issues.
- Fixed inheritance pipeline exclusions for non-food items.
- Fixed tag resolution conflicts in composite foods.

---

## [ 0.2.4-beta ] - 2026-05-31

### Added

- Added HUD nutrient reveal-on-gain system.

### Fixed

- Fixed HUD threshold logic inconsistencies.
- Fixed config slider inversion bugs.

---

## [ 0.2.3-beta ] - 2026-05-31

### Added

- Added vertical HUD layout.
- Added HUD visibility thresholds.
- Added full scanner analysis tooling.
- Added multi-nutrient classification system.

### Changed

- Updated HUD runtime config application behavior.

### Fixed

- Fixed stale HUD config synchronization.

---

## [ 0.2.2-beta ] - 2026-05-30

### Added

- Added configurable join messages.
- Added KubeJS plugin discovery system.
- Added event bridge system for nutrition events.

### Changed

- Migrated KubeJS API to 2101 event system.
- Replaced hardcoded mod IDs with constants.
- Improved registry reload locking behavior.

### Fixed

- Fixed KubeJS plugin loading failure.
- Fixed nutrition event firing under new API.

---

## [ 0.2.1-beta-HotFix ] - 2026-05-29

### Fixed

- Fixed config screen navigation regression.

---

## [ 0.2.1-beta ] - 2026-05-29

### Added

- Added raw food penalty system.
- Added gut flora mechanic.
- Added non-beneficial nutrient system.
- Added compat config grouping.
- Added Patchouli food safety chapter.
- Added schema validation system.
- Added datapack repair command.

### Changed

- Standardized five nutrient groups (removed sugars).
- Migrated legacy nutrient data automatically.

### Fixed

- Fixed raw meat penalty detection issues.

---

## [ 0.2.0-beta ] - 2026-05-15

### Added

- Added excluded item system for scanner.
- Added async classification pipeline.
- Added archetype-based nutrient inference.
- Added large mod compatibility coverage.

### Changed

- Improved tag authority resolution logic.
- Lowered composite detection threshold.
- Expanded scanner pipeline context model.

### Fixed

- Fixed stale classification caching.
- Fixed recipe inheritance exclusions.
- Fixed composite archetype scoring.

---

## [ 0.1.9-beta ] - 2026-05-13

### Added

- Added recipe inheritance system.
- Added classification debug tooling.
- Added Patchouli guide integration.

### Fixed

- Fixed tooltip diminishing returns display.
- Fixed tag override priority ordering.

---

## [ 0.1.8-beta ] - 2026-05-13

### Fixed

- Fixed unclassified item tooltip behavior.

---

## [ 0.1.7-beta ] - 2026-05-13

### Added

- Expanded nutrient tag coverage.

---

## [ 0.1.6-beta ] - 2026-05-13

### Added

- Added Patchouli guide expansion.
- Added compatibility documentation.
- Added gameplay tips section.

---

## [ 0.1.5-beta ] - 2026-05-12

### Fixed

- Tuned diminishing returns timing behavior.

---

## [ 0.1.4-beta ] - 2026-05-12

### Fixed

- Removed example datapacks from jar.

---

## [ 0.1.3-beta ] - 2026-05-12

### Changed

- Simplified Diet Screen layout.

---

## [ 0.1.2-beta ] - 2026-05-11

### Fixed

- Fixed config description inconsistencies.

---

## [ 0.1.1-beta ] - 2026-05-11

### Added

- Added heavy meal threshold config.
- Added validation framework.

### Fixed

- Fixed config loading issues.
- Fixed GUI rendering issues.
- Fixed workflow indentation.

### Changed

- Centralized registry lifecycle.
- Improved Diet Screen layout.

---

## [ 0.1.0-beta ] - 2026-05-11

### Initial Release

- Initial beta release of Nourished.
