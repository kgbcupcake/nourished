# Changelog

<!-- markdownlint-disable MD013 -->

## [ Unreleased ]

### CI / Tooling

- **`check-marielib-update.yml`** GitHub Actions workflow: runs manually (`workflow_dispatch`) or
  weekly (Mondays at 12:00 UTC), queries GitHub Packages Maven metadata for the latest published
  MarieLib release, and opens a PR against `dev` bumping `marie_lib_version` and
  `marie_lib_version_range` in `gradle.properties` when a newer version is found. Does not
  auto-merge or touch `main`; requires a manually-created `MARIELIB_PACKAGES_TOKEN` repo secret
  (PAT with `read:packages` scope) since the default `GITHUB_TOKEN` cannot read packages from the
  separate MarieLib repository.

## [ Nourished 0.2.6-beta.5 ] 2026-6-29

### Added

- **`enableDiminishingReturns` master toggle** for the diminishing-returns system, exposed in the
  Advanced config tab alongside the existing `diminishingFloor`, `diminishingSteepness`, and
  `diminishingMidpoint` sliders. When disabled, `diminishingFloor` and `noveltyBonus` are both
  forced to `1.0` (repeated foods keep full value) on server, client, and client-fallback memory
  configs alike (`NourishedMemoryConfig`).
- Config screen **Save & Save All Files** now reloads and re-broadcasts config to connected
  players when an integrated singleplayer server is running, instead of requiring a manual
  `/nourished reload` or world rejoin to see changes take effect.

### Changed

- **Items with only nutrient-tag data (no vanilla `FoodProperties`) are now recognized as
  nutritious food**: `NourishedItems.isNutritiousFood` and `FoodNutritionRegistry.getFoodProperties`
  both fall back to `FoodNutritionRegistry.getNutrientTagScores`, so tag-only items no longer get
  silently skipped by the eating/tooltip pipeline.
- **`RecipeInheritanceStage` simplified**: the multi-nutrient qualifying-threshold filter
  (`MultiNutrientInheritance.filterQualifyingNutrients`, `NUTRIENTS_BELOW_THRESHOLD` failure
  reason) was removed — inherited nutrients that pass the earlier filtering stage are used
  directly, and unmatched keys are now reported as `REJECT_NO_MATCHING_KEYWORDS` rather than being
  split between low-confidence and no-match reasons.
- Mod description in `gradle.properties` no longer references a version-specific MarieLib
  changelog note (`(from v0.2.5-beta.5)`); still requires MarieLib 0.1.0-beta.2+.

### In Progress

- Example **source synergy** datapack entries added under
  `data/nourished/nourished/source_synergies/` (`hearty_meal`, `balanced_plate`, `breakfast`) —
  paired-food bonus definitions (`source_a`/`source_b`, time window, bonus value/modifier). Not
  yet loaded or consumed by any code; not active in this build.

## [ Nourished 0.2.6-beta.4 ] 2026-6-27

### Added

- **Per-item nutrient weight system**: `NutrientWeightRegistry` loads datapack files from
  `data/<namespace>/nourished/config/weights/*.json`, mapping item IDs to per-nutrient float
  weights. Weights are consumed by the classification pipeline to bias multi-nutrient scoring
  without requiring explicit tag overrides.
- **Bundled nutrient weight files** for three major food mods:
  - `FD_nutrient_weights.json`: Farmer's Delight per-item weights
  - `croptopia_nutrient_weights.json`: Croptopia per-item weights
  - `pamhc2_nutrient_weights.json`: Pam's HarvestCraft 2 per-item weights
- **`data/nourished/config/SOURCE_CLASSIFICATIONS_README.md`**: written into the jar bundle;
  documents the `source_classifications.json` schema and how to add per-source nutrient
  classification overrides.
- **`NourishedExportCommands`**: export command logic extracted from `NourishedCommand` into a
  dedicated class (`exportAll`); `NourishedCommand` now delegates to it cleanly.

### Removed

- **Compat integration hooks** moved out of Nourished into MarieLib — the following classes are
  gone from this jar:
  - `dev.maire.nourished.compat.lso.LSOCompat`
  - `dev.maire.nourished.compat.peakstamina.PeakStaminaCompat`
  - `dev.maire.nourished.compat.spiceoflifeonion.SpiceOfLifeOnionCompat`
- **Compat module config toggles** removed: `enablePSStaminaUsage`, `enablePSPenaltyDecay`,
  `enablePSExhaustionDuration`, `enableSOLDiversityHealth`, `enableSOLDiversityPenalty`,
  `enableLSOThermalResistance`, `enableLSOBrokenHeartResilience`, and `enableLSOThirstSaturation`
  are no longer defined in `nourished-common.toml`, `NourishedConfig`, `NourishedModuleCache`, or
  the Modules config category. Compat behaviour for Peak Stamina, Spice of Life: Onion, and LSO
  is now governed entirely by MarieLib.
- **Modules config screen subcategories** for `peakstamina`, `spiceoflife`, and `lso` removed;
  the Modules tab now groups only `core`, `rawfood`, `ui`, and `other`.
- **`mod_compat.json`** (`data/nourished/config/mod_compat.json`) removed; replaced by
  `source_classifications.json` (see Changed).
- **`SourceValuesValidator`** (`nourished_source_values`) removed — the `source_values.json`
  registry it validated has been superseded by MarieLib-side infrastructure.
- **`/nourished validate`** command removed from `NourishedCommand` — validation output is now
  provided by MarieLib's own command surface; run `/marieslib validate nourished` for per-validator
  status and findings.

### Changed

- **`source_classifications.json`** replaces `mod_compat.json` and `source_overrides.json` as the
  canonical per-source nutrient classification file under `data/nourished/config/`; starts empty
  (`[]`) and is documented by the new `SOURCE_CLASSIFICATIONS_README.md`.
- **`SourceOverridesValidator` → `SourceClassificationsValidator`**: renamed to match the new
  file (`source_classifications.json`); validator ID changed from `nourished_source_overrides` to
  `nourished_source_classifications`.
- **Recipe inheritance delegated to MarieLib**: `RecipeInheritanceStage` no longer owns a
  `BoundedLRU` cache or the inline `discoverRecipeIngredients` loop. It now delegates entirely to
  MarieLib's `RecipeInheritanceResolver`, whose index is pre-built at recipe-manager availability
  via `RuntimeFoodResolver.buildRecipeIndex(RecipeManager)`. Per-item recipe timeouts and `RECIPE_TIMEOUT` failure reasons are removed; `INGREDIENT_CAP_EXCEEDED` failure reason removed (cap logic lives in MarieLib's resolver).
- **`NourishedPresetRegistry.applyPresetValues`**: now reads preset values as a `JsonObject` with
  null-safe field presence checks (`if (v.has(...))`) rather than directly consuming all fields
  from a `PresetValues` record: partial presets that omit fields no longer reset those config
  values to unintended defaults.
- **`FoodOverrideRegistry` override README** updated to clarify the two export paths: the
  categorized `nourished_nutrients_export/` folder (via Export All Foods button or
  `/nourished export_all`) is recommended for building `food_overrides.json`; the single-file
  `/marieslib dump nourished_nutrients` export is documented as a secondary quick-inspection
  option.
- **Nutrient tag maintenance** across all five groups: broad reorganization of bundled tags
  (`fruits.json`, `vegetables.json`, `proteins.json`, `grains.json`, `dairy.json`) to align with
  expanded weight-file coverage and tag audit findings; net reduction in duplicate / misclassified
  entries.

### MarieLib & Build

- Bumped MarieLib dependency to **0.1.1-beta.2** (`marie_lib_version_range=[0.1.1-beta.2,)`).
  Requires MarieLib **0.1.1-beta.2+** for:
  - `RecipeInheritanceResolver` pre-built index (recipe inheritance delegation)
  - Compat hooks for Peak Stamina, Spice of Life: Onion, and LSO (moved from Nourished)
  - `/marieslib validate nourished` command (replaces `/nourished validate`)
- Removed `spice_of_life_onion_version` from `gradle.properties` (no longer a compile dependency).

### Important Upgrade Notes

If updating from 0.2.6-beta.3:

1. **Requires MarieLib 0.1.1-beta.2+**. Update MarieLib on Modrinth before launching.
2. **Remove compat module toggles from your server config**: `enablePSStaminaUsage`,
   `enablePSPenaltyDecay`, `enablePSExhaustionDuration`, `enableSOLDiversityHealth`,
   `enableSOLDiversityPenalty`, `enableLSOThermalResistance`, `enableLSOBrokenHeartResilience`, and
   `enableLSOThirstSaturation` are no longer read from `nourished-common.toml`. Delete or ignore
   these keys; compat is now controlled by MarieLib.
3. **`/nourished validate` is gone**: use `/marieslib validate nourished` for config validation
   output going forward.
4. `mod_compat.json` is no longer used and can be deleted from your config folder. Per-source
   classification overrides now live in `source_classifications.json`.
5. The `nourished_source_values` config validator no longer runs; `nourished_source_classifications`
   replaces it for `source_classifications.json` validation.

## [ Nourished 0.2.6-beta.3 ] 2026-6-21

### Added

- **Full nutrient export** for modpack authors: `NutrientExportResolver` registers MarieLib export
  resolver `nourished_nutrients` (per-item nutrients + calories from the live classification
  cascade). `NutrientFullExporter` writes categorized reference files to
  `config/nourished/nourished_nutrients_export/` (`fruits.json`, `proteins.json`, etc. — one file
  per nutrient key, dominant-category grouping).
- **Export All Foods** button on the Scanner config tab (singleplayer with an active world);
  same output as the exporter above, with in-GUI toast feedback.
- **`/nourished export_all`** (op level 2) — server/console alias for the full categorized export.
- **`config/nourished/OVERRIDES_README.md`** — written on first load when absent; documents
  `food_overrides.json` schema, export-to-override workflow, and links to the export button /
  MarieLib dump command.
- **Per-nutrient response curves** (opt-in, disabled by default): `NutrientCurveRegistry` with
  presets `FLAT`, `DIMINISHING`, `CONFIDENCE_GATED`, and `SYNERGY`, plus custom grid support.
  Override stack: bundled defaults → `config/nourished/nutrient_curves.json` → datapack override →
  KubeJS (`NourishedAPI.registerNutrientCurve`). General tab toggles `enableNutrientCurves` and
  global `defaultCurvePreset`; Nutrients tab exposes a per-nutrient preset picker. When curves are
  off, legacy flat scale/clamp math is unchanged.
- **Config validation framework** — ten MarieLib `ConfigValidator` registrations covering
  `nutrients.json`, `colors.json`, `food_overrides.json`, `scanner_spec.json`, source overrides/
  values, effects, food values, locks, and raw food config. Runs automatically after initial registry
  load; FAIL/WARN counts log to the server console. **`/nourished validate`** (op level 2) prints
  per-validator status and findings. Client shows a toast on FAIL directing you to run the command.
- **Tag audit tooling** for modpack authors: `NourishedTagAuditContext` plus two audit rules —
  `TagInferenceMismatchRule` (bundled tag vs live runtime inference) and `NamespaceBiasRule`
  (bundled tag vs scanner namespace weights). **`/nourished audit_tags`**, **`/nourished audit`**, and
  **`/nourished tag`** (aliases, op level 2) scan all bundled nutrient tags and write
  `config/nourished/tag_audit_report.json`. Chat shows only the report path; full issue/suggestion
  detail stays in the JSON file. Also available via `/marieslib audit_tags nourished` (same quiet
  chat behavior).
- **`/nourished set_all <value> <player>`** (op level 2) — sets every registered nutrient bar to the
  same fill level (0.0–1.0) in one command, without clearing diminishing-returns memory. Complements
  the existing per-key **`/nourished set`** and config-default **`/nourished reset`**.

### Fixed

- Existing `config/nourished/nutrients.json` files that still had legacy white (`0xFFFFFFFF` / `-1`)
  colors for built-in nutrients (fruits, vegetables, proteins, grains, dairy) are auto-repaired on
  load to the corrected bundled defaults (dairy cream, grains amber, etc.); repaired values are
  written back to disk so the fix runs once.
- KubeJS startup scripts failing `requireValueKey` for built-in or persisted nutrients — nutrients
  now sync into `ValueRegistry` immediately after `NutrientRegistry.loadDefinitions()` in the mod
  constructor via `syncToValueRegistryUnfrozen()` (without freezing); `syncAndFreeze()` at common
  setup still resets and re-populates from the same source before freezing, so the early sync is
  idempotent.
- Custom nutrients registered via API/KubeJS (`registerExternal`) not immediately classifiable —
  each external registration now publishes into `ValueRegistry` right away through
  `syncValueRegistryEntry()`, including mid-script (before common setup) and mid-game (after
  `ValueRegistry` is already frozen).
- Food Scanner config tab showing a stale dark list panel after leaving a world — scan results are
  cleared when no client level is loaded (`canScan()` false), so the list viewport background cannot
  draw without an active world.
- Mis-tagged **Fruits Delight** items in bundled nutrient tags — `durian_flesh`, `hawberry_roll`, and
  `pear_with_rock_sugar` moved from `proteins` to `fruits` (the inference-mismatch cases that
  motivated the tag audit rules).
- **`/nourished tag`** crashing with `NoClassDefFoundError: TagAuditReportWriter` when run against a
  published MarieLib jar — report JSON writing now lives in Nourished as
  `NourishedTagAuditReportWriter`, so the command no longer depends on MarieLib classes that may not
  yet be in the Modrinth artifact.

### Changed

- Cloth Config screen refactored into per-tab category classes (`GeneralCategory`,
  `NutrientsCategory`, `ScannerCategory`, etc.) with shared widgets extracted to
  `NourishedConfigSharedWidgets`; behavior unchanged, maintenance surface reduced.
- Tag audit commands no longer spam individual findings into chat — only the written report path is
  shown in-game; use the JSON report for full issue/suggestion lists.
- Full food reference export is **`/nourished export_all`** (categorized files under
  `nourished_nutrients_export/`). There is no `/nourished dump`; use
  **`/marieslib dump nourished_nutrients`** for MarieLib's single-file export variant.

### MarieLib & Build

- Bumped MarieLib dependency to **0.1.1-beta.1** (`marie_lib_version_range=[0.1.1-beta.1,)`).
  Requires MarieLib **0.1.1-beta.1+** for:
  - `ValueRegistry.isFrozen()` (external nutrient registration after freeze)
  - `MarieAPI.registerExportResolver` / `RegistryExporter` (nutrient export pipeline)
  - `MarieAPI.registerConfigValidator` / `ValidationRunner` (config validation)
  - `MarieAPI.registerTagRule` / `registerTagAuditContext` / `TagScanner` (tag audit pipeline)
  - Consumer command **`/nourished set_all`** (implemented in MarieLib's player command tree)
  - Local/dev MarieLib builds also ship `TagAuditReportWriter` for `/marieslib audit_tags`; Nourished's
      own audit commands write reports via `NourishedTagAuditReportWriter` so they work with the
      published jar alone.

### Important Upgrade Notes

If updating from 0.2.6-beta.2:

1. **Requires MarieLib 0.1.1-beta.1+**. Update MarieLib on Modrinth before launching.
2. Existing worlds with legacy white built-in nutrient colors in `nutrients.json` are repaired
   automatically on first load after upgrade; no manual edit needed.
3. First launch creates `OVERRIDES_README.md` beside `food_overrides.json` if you do not already
   have one; existing pack edits to that file are never overwritten.
4. **Nutrient response curves are off by default** — enable `enableNutrientCurves` in General config
   (or set it in your server config) only when you want curve-based scaling; until then behavior
   matches prior releases.
5. Run **`/nourished tag`** (or `/nourished audit` / `/nourished audit_tags`) after upgrading in a
   modpack world to generate `config/nourished/tag_audit_report.json` and review bundled-tag
   disagreements with live inference or namespace bias.
6. Use **`/nourished set_all 0.8 @s`** (example) to fill every nutrient bar to the same level for
   testing; **`/nourished reset @s`** still restores the configured starting fill instead.

## [ Nourished 0.2.6-beta.2 ] 2026-6-16

### Milestones

- Full per-nutrient tier chain: 18 milestones (beginner / journeyman / master for fruits, vegetables,
  proteins, grains, dairy) plus `perfectly_balanced` cross-group milestone, each with chained
  advancement entries and lang descriptions under `data/nourished/advancement/milestones/`.
- Milestones now actually load from datapacks: `NeoForge.EVENT_BUS.addListener(MarieDataManager::registerReloadListener)`
  registers MarieLib's reload listener at mod init.
- All bundled milestone JSON files include `marie_schema_version`.
- Cross-nutrient `all` milestone support via `MilestoneRegistry.getForAll()`.
- Cumulative goals corrected to **0.5** beginner / **2.0** journeyman / **5.0** master (was 5.0
  across all tiers).
- Removed `test_fruits` example milestone and all **Sugars** milestone/advancement files (no
  sugars nutrient registered).

### Diet Screen Edit Mode

- In-game layout editor: press the **Edit Diet Screen** keybind (`J` by default) while the Diet
  screen is open to drag the panel, resize the main panel, and resize the Recent Meals and Eat
  more of… boxes independently.
- `DietLayout` centralizes panel dimensions, scale, and offset math; `DietScreenEditController`
  handles drag/resize preview and writes back to client config on release.
- `DietScreenEditScreen` overlay keeps the world visible (`isPauseScreen=false`) while editing.
- New client config options: `dietScale`, `dietOffsetX`, `dietOffsetY`, `dietBackgroundOpacity`,
  `recentMealsBoxScale`, `eatMoreBoxScale`.
- New **Diet Screen** Cloth Config category with sliders, reset-position button, and hotkey
  binding entry; defaults in `client_defaults.json` and lang entries updated.
- `DietScreen` refactored for scaled layout, configurable background opacity, and edit-mode
  resize handles / preview overlays.

### HUD & Nutrient Colors

- Per-nutrient **#RRGGBB** color editor in the HUD & Display config tab: live preview swatch,
  validation, per-row reset, and Reset All Colors.
- `ValueDefinition.colorOverride` wired through `NutrientRegistry.toValueDefinition()` so nutrient
  colors from `nutrients.json` render in the HUD.
- Bundled nutrient colors updated: dairy cream (`0xFFE8D5B7`), grains amber (`0xFFD9A521`), and
  proper ARGB values for all five groups (previously `-1` / white).
- "Counts toward: X beginner" tooltip line on food items for milestone progress.

### Template Export Commands

- `/nourished export_effects_template` — writes a starter effects datapack to the world folder.
- `/nourished export_values_template` — writes a starter values/nutrients datapack.
- `/nourished export_colors_template` — writes resolved HUD colors as `config/colors.json`.
- `_comment_*` documentation fields in all template export outputs for modpack authors.

### Added

- `EffectRegistry.upsertFromDatapack` — datapack effects replace bundled effects matching
  (nutrient, trigger, effect) instead of duplicating; threshold is the overridable field.
- `NourishedDatapackCallbacks.registerCustomEffect` wired to `EffectRegistry.upsertFromDatapack`.
- `NutrientRegistry.save()` persists in-memory nutrient definitions back to
  `config/nourished/nutrients.json`; config screen save now calls it alongside other registries.
- Compat config screen groups food-source mods under `CompatCategory.SOURCE_MOD`; bundled compat
  JSON files updated to match.

### Changed

- Default **N** keybind now opens the Diet screen (`OPEN_DIET_SCREEN`) instead of the mod config
  screen (`OPEN_CONFIG` removed).
- `HudDrawHelpers` border/handle drawing utilities promoted to `public static` for reuse by the
  diet edit overlay.
- `NutrientRegistry` load path hardened: validates config in place, falls back to bundled
  defaults with automatic repair instead of deleting the file on schema mismatch.
- External/API effect registration replaces an existing entry when (nutrient, trigger, effect)
  match instead of always appending.
- Removed unused JEI / REI / EMI `compileOnly` dependencies from `build.gradle` (recipe viewers
  handled by MarieLib).
- GitHub Actions `actions/checkout` upgraded from v4 to v5 in `auto-tag.yml` and `codeql.yml`.

### MarieLib & Build

- Bumped MarieLib dependency to **0.1.0-beta.5** (`marie_lib_version_range=[0.1.0-beta.5,)`).

### Fixed

- HUD bars showing white — `colorOverride` now set from `nutrients.json` `color` field.
- Datapack effect overrides no longer duplicate — upsert dedup key is (nutrient, trigger, effect)
  only.
- HUD color config row reset and save now compare against `MarieValueColors.resolvedDefaultArgb`
  (nutrients.json defaults) instead of palette-only colors — fixes wrong hex after reset and
  spurious `colors.json` writes when the chosen color matches bundled defaults.
- Custom HUD colors silently reverting to white on load — ColorRegistry.parseArgbString used Integer.decode on full-alpha 0x-prefixed ARGB strings, which always overflows Integer.parseInt's positive-value range and threw, falling back to white every time; switched to Long.parseLong to match the method's other hex branches.
- Per-nutrient decay rate overrides in config had no effect — `NourishedContextBuilder` now wires
  `decayRateFor` from `resolvedDecayRateFor()` so nutrition decay honors per-nutrient sliders.
- Nutrients registered via API/KubeJS lost on `nutrients.json` reload — registry tracks externally
  registered defs and reapplies them after reload; `loadDefinitions()` / `syncAndFreeze()` defers
  ValueRegistry publish until common setup so external registration runs first.
- External mod food classifications ignored — `NutrientClassificationLookup` now checks
  `SourceRegistry.getExternalClassification()` before tag/recipe resolution.
- Tracking screen nutrient bar labels showing raw translation keys — added missing
  `nourished.screen.tracking.bar.*` lang entries.

### Important Upgrade Notes

If updating from 0.2.6-beta.1:

1. **Requires MarieLib 0.1.0-beta.5+**. Update MarieLib on Modrinth before launching.
2. The **N** key now opens the Diet screen, not the Nourished config. Use Mods → Nourished →
   Config or `/nourished` commands for settings.
3. Diet screen position, scale, and section sizes persist in `config/nourished-client.toml`. Use
   the Edit Diet Screen keybind or the **Diet Screen** config tab to adjust layout.
4. Milestone cumulative goals changed — existing worlds may need `/reload` to pick up corrected
   tier thresholds.

## [ Nourished 0.2.6-beta.1 ] 2026-6-14

### Death Nutrition Behavior

- Added `deathNutritionBehavior` config option under `[general]` in `nourished-common.toml`
  and `common_defaults.json`. Controls what happens to nutrient bars and food memory when a
  player respawns after death:
- `preserve` (default): keep current nutrient levels and eating memory
- `reset_to_starting`: reset all bars to `startingNutrientValue` and clear food memory
- `vanilla_half`: legacy 50% reset with cleared food memory
- Wired through `NourishedContextBuilder` into MarieLib's tracking pipeline so death handling
  respects server config in singleplayer and multiplayer.
- Config screen, import/export JSON, and lang entries added for the new setting.
- `startingNutrientValue` description updated: it now also applies to `/nourished reset` and
  death when `deathNutritionBehavior` is `reset_to_starting`.

### Milestones

- Datapack milestone definitions under `data/<namespace>/nourished/milestones/` now load
  through `NourishedDatapackCallbacks.registerMilestone()` and register into MarieLib's
  milestone registry at datapack apply time.
- `enableMilestones` module toggle (already present) gates milestone checks at runtime.
- Bundled example milestone `test_fruits`: awards Regeneration I when cumulative fruit
  nutrition reaches 0.5, with matching advancement tab entries under
  `data/nourished/advancement/milestones/`.

### MarieLib & Build

- Bumped MarieLib dependency to **0.1.0-beta.3** (`marie_lib_version_range=[0.1.0-beta.2,)`):
  Requires MarieLib **0.1.0-beta.2+** on the classpath.
- Gradle now always resolves MarieLib from Maven for compilation; local `../MarieLib` composite
  builds are still supported for development via `includeBuild` with a simpler compile ordering
  fix (no more intermittent clean-build races with NeoForge moddev artifacts).
- Removed redundant JEI / REI / EMI compatibility bootstrap from `ClientEventRegistrar`:
  recipe viewer plugins are initialized by MarieLib directly.

### Raw Food

- Added per-item **cookedness overrides** in `config/nourished/raw_food.json` (`overrides`
  section). Modpack authors can pin cookedness values (0.0–1.0) for items that the resolver
  misclassifies; overrides are registered in `CookednessResolver` and cleared on config reload.

### Removed

- **Stamina module** removed entirely (~2,200 lines). Stamina tracking, HUD, combat/movement
  drain, and `stamina.json` config are gone. Peak Stamina compat integration remains under
  module toggles for servers using that mod.

### Changed

- All bundled datapack JSON files now use `marie_schema_version` instead of the legacy
  `nourished_schema_version` key (MarieLib still accepts the old key when reading overrides).
- Nutrient registry loading hardened: redundant init guards, tag-key cache in
  `FoodNutritionRegistry`, and cache invalidation on nutrient reload.
- `NourishedAPI` registration methods annotated `@ApiStatus.Stable` for clearer external-mod
  contract signaling.
- Import/export and preset file writes now validate paths stay within the config directory.
- `EffectRegistry` and `NourishedLockRegistry` truncate oversized entry lists with logging
  instead of growing unbounded.

### Fixed

- Local MarieLib composite builds no longer hit race conditions where `compileJava` started
  before NeoForge artifacts existed on disk.

### Important Upgrade Notes

If updating from 0.2.5-beta.5 or earlier:

1. **Requires MarieLib 0.1.0-beta.2+** (bundled build uses 0.1.0-beta.3). Update MarieLib on
   Modrinth before launching.
2. If you used the removed Stamina module, delete `config/nourished/stamina.json` — it is no
   longer read. Stamina HUD and drain mechanics are not available in this version.
3. Review `deathNutritionBehavior` in `config/nourished-common.toml` — default is `preserve`
   (bars kept on death). Set to `reset_to_starting` or `vanilla_half` for stricter death
   penalties.
4. To customize raw-food cookedness for specific items, add an `overrides` block to
   `config/nourished/raw_food.json`.
5. After upgrading, run `/nourished reload` on servers or rejoin worlds to refresh cached
   classification and config-driven module state.

## [ Nourished 0.2.5-beta.5 ] 2026-6-9

### MarieLib Integration

- Nourished now **requires MarieLib 1.0.0+** on the classpath. From here on out, Nourished will
  be built against the latest version of MarieLib from Modrinth.
- Nourished now builds against **MarieLib** (`marie_lib_version=1.0.0`) as an included Gradle
  composite project. Shared infrastructure - scanner pipeline, tracking/memory, datapack
  loaders, compat framework, registries, client widgets, and most public API types - lives
  in MarieLib; Nourished owns nutrition-specific gameplay, config, and datapacks.
- Removed duplicate Nourished copies of API/registry/scanner/tooling classes that now come
  from MarieLib. `NourishedAPI` and related entry points delegate to MarieLib types.
- KubeJS bridge now registers through MarieLib's plugin surface.

### Config & Module Toggles

- Renamed module toggles in `nourished-common.toml` / defaults to match MarieLib
  `ModuleCache` field names:
- `enableNutritionEating` -> `enableSourceApplication`
- `blockHeavyMeals` -> `enableBlockHeavySources`
- `blockLightFood` -> `enableBlockLightSource`
- `heavyMealNutritionThreshold` -> `heavySourcePropertyThreshold`
- Added `NourishedConfig.syncModuleCache()` so module toggles are copied into MarieLib's
  hot-path cache on config load, reload, save, and config-screen apply (fixes toggles
  that previously had no runtime effect).
- Bundled `common_defaults.json` and lang entries updated for the new key names. Defaults
  loader still accepts legacy JSON keys for datapack overrides.

### Scanner & Datapacks

- Bundled `scanner_spec.json` now uses MarieLib schema keys (`source_property_heuristics`,
  `source_properties`, `saturating_source`, `light_application`, `min_property_points`,
  `max_property_points`). MarieLib still accepts legacy `food_*` / `*_meal` / `*_snack`
  keys when reading older copies from `config/nourished/scanner_spec.json`.
- `mod_compat.json` uses `heavySourcePropertyThreshold` (legacy key still accepted).

### Compat Ownership

- `compat_registry.json` now ships under `data/nourished/compat/` (Nourished-owned mod
  compat catalog).
- Mod-specific integration hooks moved from MarieLib into Nourished:
- `dev.maire.nourished.compat.lso.LSOCompat`
- `dev.maire.nourished.compat.peakstamina.PeakStaminaCompat`
- `dev.maire.nourished.compat.spiceoflifeonion.SpiceOfLifeOnionCompat`

### Bootstrap & Presets

- Nourished registers `NourishedConfig` / `NourishedClientConfig` and their mod-bus
  listeners before `MarieLibContext.register()`, so config is available when MarieLib
  hooks are wired
- Added `NourishedPresetRegistry` for Nourished-specific preset behavior: seeding built-in
  Casual / Survival / Hardcore JSON from the jar, applying preset values to config, and
  force-enabling all effects for the Hardcore preset
- MarieLib `PresetRegistry` stub methods (`ensureBuiltInFilesOnDisk`, `applyPresetValues`,
  `enableAllEffects`) now delegate through `MarieLibContext` instead of throwing at runtime

### KubeJS

- Added dedicated `NourishedKubePlugin` with `kubejs.plugins.txt` and service-loader registration
  for KubeJS 2101 discovery.
- Added `NourishedEvents` server event group: `nutrientChanged`, `nutrientCritical`,
  `nutrientExcess`, `sourceConsumed`, `gutHealthChanged`, `rawFoodPenalty`, `nutrientModifier`,
  and `foodEaten`.
- Added `NourishedKubeBindings` (`NourishedAPI` in scripts) with `registerNutrient`,
  `getNutrientLevel`, `isNutrientCritical`, `getGutHealth`, and `getNutrientKeys`.
- Added `NourishedKubeIntegration` reflection bridge so Nourished loads without KubeJS on the
  classpath; gameplay hooks fire KubeJS events only when the mod is present.
- Added `NourishedKubeEventBridge` to forward MarieLib value events and Nourished gameplay
  (food eaten, gut health ticks/recovery, raw food penalties) into KubeJS.
- `rawFoodPenalty` scripts can cancel penalties via `event.cancel()`.
- `nutrientModifier` scripts can adjust `event.amount` or cancel gains before they apply.
- Updated bundled `nourished_example_events.js` with current event field names and usage.

### Fixed

- Fixed mod-load crash (`UnsupportedOperationException: Implement via consuming mod`) when
  `PresetRegistry.ensureBuiltInFilesOnDisk()` ran during registry lifecycle init
- Updated client code for MarieLib API renames: `VALUE_COLORS` / `SOURCE_VALUES`
  import-export sections, `getRecentSourceIds()`, and `onFullTrackingSynced()`
- KubeJS event payloads now expose `event.player` on `nutrientChanged`, `nutrientCritical`,
  `nutrientExcess`, `gutHealthChanged`, and `rawFoodPenalty` (previously only `playerId` was
  available on those events; `foodEaten` already had `player`).

### Important Upgrade Notes

If updating from 0.2.5-beta.4 or earlier:

1. **Requires MarieLib 1.0.0+** on the classpath. From here on out, Nourished will be built
   against the latest version of MarieLib from Modrinth.
2. Delete `config/nourished/scanner_spec.json` before first launch on this version so it
   regenerates with the new schema keys (same advice as prior scanner upgrades).
3. Review `config/nourished-common.toml` - rename the four module keys above if you had
   custom values; unset keys fall back to defaults.
4. After upgrading, run `/nourished reload` on servers or rejoin worlds to refresh cached
   classification and config-driven module state.

## [ Nourished 0.2.5-beta.4 ] 2026-6-6

### Multiplayer / Server Sync

- Fixed a fundamental multiplayer bug where client config was overriding server-authoritative
  gameplay parameters (decay rate, thresholds, memory window) on dedicated servers
- Config snapshot is now sent to clients on join before diet data, ensuring correct values
  are in place before any simulation runs
- Added `SyncState.PENDING` lifecycle: client transitions UNINITIALIZED → PENDING on
  snapshot receipt, PENDING → ACTIVE on full diet sync
- Client state now resets correctly on disconnect, preventing stale server config from
  leaking into the next connection
- `/nourished reload` now re-syncs full diet data to all connected players in addition to
  the config snapshot
- Bumped network protocol to version 3: servers and clients on mismatched versions will
  log a warning and discard the packet rather than silently corrupting state

### Raw Food / Gut Health

- Added `enableGutHealth` module toggle: gut health tracking, recovery, and sensitivity
  can now be disabled independently of raw food penalties
- `GutHealthTickHandler` and `GutHealthRecoveryHandler` now gate on `enableGutHealth`
  instead of `enableRawFoodPenalty`
- Raw food penalty effects remain controlled by `enableRawFoodPenalty` only

### Architecture

- Introduced `DietMemoryConfig`: diet simulation parameters are now injected at system
  boundaries rather than pulled directly from raw config at runtime
- `DietData` no longer reads `NourishedConfig` directly; all memory/multiplier values come
  from the server snapshot in multiplayer or raw config in singleplayer
- Client network handling extracted out of common code into client-only classes, fixing
  a dedicated server classloading issue

### Config Snapshot

- Snapshot now carries `memoryWindowMinutes`, `noveltyBonus`, `noveltyDecayCap`,
  `diminishingFloor`, and `enableGutHealth` in addition to existing fields
- Commands (`/nourished report`, `/nourished nutrient`) now prefer snapshot values over
  raw config, with a fallback notice when out of sync

### Diagnostics

- Protocol version logged at server startup
- Warn-once logging added at all injection points when config snapshot is null
- Null snapshot no longer silently falls back — missed injection sites now produce a
  visible error rather than wrong gameplay values

## [ Nourished 0.2.5-beta.3 ] 2026-6-3

### Diagnostics & Classification Tracing

- Added `ClassificationTrace` infrastructure for recording food classification decisions.
- Added `ClassificationPipeline` support to identify trace origin.
- Added `ClassificationTraceStep` for step-by-step classification tracking.
- Added `ClassificationTraceFormatter` for human-readable diagnostic reports.

### Runtime Trace Improvements

- Added runtime `SIGNAL_AGGREGATION` tracing.
- Added runtime `WINNER_SELECTION` tracing.
- Added runtime `CONFIDENCE` tracing.
- Added confidence and uncertainty propagation to runtime traces.

### Classification Explainability

- Added archetype match evidence capture.
- Added token demotion evidence capture.
- Added negative keyword contribution tracing.
- Improved held-item diagnostics and classification reasoning output.

### Recipe Diagnostics

Added detailed recipe failure reporting:

- `NO_RECIPE_FOUND`
- `NULL_RECIPE_MANAGER`
- `INGREDIENT_CAP_EXCEEDED`
- `CONFIRMED_THRESHOLD_FAILED`
- `NUTRIENTS_BELOW_THRESHOLD`
- `RECIPE_EXCEPTION`

### Fixes & Lifecycle Improvements

- Consolidated config reload handling to ServerStartingEvent, replacing LevelEvent.Load to prevent duplicate reload cycles during world initialization.
- Removed legacy HUD threshold migration from NourishedClientConfig.
- Improved datapack error logging for duplicate nutrients and malformed entries with clearer messages.

### Notes

This update significantly expands Nourished's diagnostic capabilities and lays the foundation for future self-diagnosing classification and validation tooling.

## [ Nourished 0.2.5-beta.2 ] Ui Fixes- 2026-6-2

### 0.2.5-beta.2

- Fixed the Diet Screen being affected by Minecraft's Menu Background Blur setting. The UI Should now render sharply regardless of your blur setting, with the world still visible behind the panel.
  ![Thanks-To](https://github.com/kgbcupcake/nourished/pull/2)
  ![Main-Gui](https://cdn.modrinth.com/data/cached_images/583dd8a2f4d4e8bf5c7501c0565ef12aa1986fee_0.webp)
  ![Blur Setting](https://cdn.modrinth.com/data/cached_images/39c00c193c7890a769af63b294672640586e6892.png)

## [ Nourished 0.2.5-beta.1 ] Stability & API Patch - 2026-6-1

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

## Internal Cleanup

- Debug logging cleanup
- Temporary debugging logic in RecipeInheritanceStage has been removed, including targeted diagnostic logs used during development.
- Logging behavior has been restored to standard debug-level output only.

## System Status Notes

- The following systems are currently registered but not fully wired into gameplay logic:
- Nutrient Synergies
- Food Synergy Bonuses
- Milestone Rewards
- These systems are exposed through the API and available for integration, but are not yet active in the core eating/progression pipeline. They are planned for future updates.

## Compatibility

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

## [0.2.5-beta] - 2026-06-1

### Important Upgrade Notes

If updating from an earlier 0.2.x beta:

1. Delete `config/nourished/scanner_spec.json` before launching the game.
2. Load your world and run:

```bash
 /nourished invalidate_cache
```

3. Rejoin the world to rebuild cached nutrition data.

Existing worlds may continue using outdated scanner data until these steps are completed.

These steps ensure recipe inheritance, nutrient tags, and scanner data are regenerated using the latest compatibility improvements.

### Added

- Recipe inheritance now works client-side, allowing composite foods to display accurate multi-nutrient tooltips without requiring server-side lookups.
- Single-ingredient transformation recipes now inherit nutritional categories correctly (e.g. bread → bread slice, raw meat → cooked meat, apple → apple mash).

### Fixed

- Pam's HarvestCraft mixing bowl recipes and other custom recipe types are now discovered by the inheritance pipeline. Previously, only crafting and smelting recipes were supported.
- Pam's cooking containers (bakeware, cutting board, pot, saucepan, mixing bowl, skillet, juicer, grinder) and Croptopia cooking tools (frying pan, cooking pot, food press, knife) are no longer treated as nutritional ingredients during recipe inheritance.
- Non-food items such as salt, oils, spices, flavorings, yeast, water bottles, and alchemical ingredients are now ignored during inheritance instead of lowering confidence counts.
- Fruits such as berries and raspberries no longer incorrectly classify as raw foods.

### Compatibility

- Greatly expanded nutrient tag coverage across Pam's HarvestCraft 2 (Crops, Trees, Food Core, Food Extended), Croptopia, Create: Food, Farmer's Delight, Wilder Nature, Undergarden Delight, and additional food mods.
- Hundreds of composite foods now inherit and display more accurate multi-nutrient profiles.

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

- Multi-nutrient recipe inheritance: complex dishes now contribute to multiple food groups based on their ingredients. Eating a steak sandwich gives Proteins, Grains, and Vegetables credit automatically.
- HUD hide-above threshold: bars above a configurable percentage are hidden from the HUD. Set to 0.4 to only see bars that need attention. as requested. in #3
- HUD show-below threshold: bars below a configurable percentage are always shown regardless of other visibility settings.
- Show zero nutrients on HUD: toggle to show bars at 0% so you always know what you're missing.
- HUD background opacity: configurable from fully transparent to fully opaque.
- Vertical HUD layout: bars render as side-by-side columns filling upward instead of horizontal rows.
- HUD flash on nutrient increase: bars briefly highlight for 2 seconds when a nutrient increases from eating.
- Multi-nutrient full registry analysis: /nourished scan_analysis command analyzes all 4700+ classified foods and writes multi-nutrient recommendations, overlap matrix, ambiguity report, and scanner metrics to config/nourished/scanner_analysis/.
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
- `showJoinMessage`: toggle welcome and notice chat lines on login (default: `true`)
- `joinMessageLine1`: welcome text after the NOURISHED header (default: `Welcome to the nutrition engine.`)
- `joinMessageLine2`: secondary notice line (default: `Beta NOTICE - features and balance may change.`)
- Matching defaults in `data/nourished/config/common_defaults.json` for modpack and datapack overrides.
- KubeJS 2101 plugin discovery via `kubejs.plugins.txt` plus a `META-INF/services` service entry as a backup path.
- Manual KubeJS plugin bootstrap fallback when automatic discovery fails (for example under Architectury), invoked from mod initialization.
- `NourishedEvents` KubeJS script binding so scripts can subscribe to the `NourishedEvents` event group.
- `NourishedKubeJSEventBridge`: dedicated NeoForge → KubeJS bridge for runtime nutrition events.
- `ConfigReloadHandler.isReloadInProgress()` so handlers can defer work during config and datapack reloads.

### Changed

- **KubeJS (2101 migration):**
- Migrated runtime events to the KubeJS 2101 `EventGroup` / `EventHandler` interface pattern.
- Moved NeoForge subscription and event wrapping out of `NourishedKubeJSEvents` into `NourishedKubeJSEventBridge`.
- Event payload `player` fields are now typed as `ServerPlayer` instead of `Object`.
- Join welcome and beta notice messages now read from config while keeping the existing styled chat formatting (◆ NOURISHED ◆ header, ⚠ notice line, and bold/light split when `joinMessageLine2` contains `-`).
- Replaced hardcoded `"nourished"` mod id strings with `Nourished.MODID` across configs, registries, compat hooks, and tooling.
- Registry reload pipeline hardened with read/write locks on `AbstractRegistry` and `ListRegistry` so reset → repopulate → freeze is atomic during `/reload`.
- Datapack and config loaders now log warnings on swallowed `IOException`s instead of failing silently.

### Fixed

- **KubeJS integration not loading** on KubeJS 2101: Nourished scripts and bindings were unavailable when plugin discovery missed the jar; fixed with `kubejs.plugins.txt`, service-loader entry, and manual bootstrap registration.
- **KubeJS nutrition events not firing** after the 2101 API change: event registration and bridging now follow the current KubeJS event-group pattern.
- **Nutrition effects applying during reload**: effect application and player-tick effect handlers now skip work while a config or datapack reload is in progress, avoiding inconsistent effect state mid-reload.

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

- Excluded items system: non-food items with FoodProperties (potions, soap, chicken feed, magic essences, etc.) are now explicitly excluded from classification and will not show a Nourished tooltip

- excluded_items array in scanner_spec.json: modpack creators and datapack authors can extend this list without code changes

- hamburger stem mapping and archetype entry for correct multi-nutrient resolution across all hamburger variants; sandwich archetype entry to inject grains signal for sandwich-type foods

- sandwich, burger, and vegan keyword entries in scanner_spec.json for improved composite classification

- Community tag signals now propagate into the keyword/archetype scoring pass via StageContext instead of short-circuiting the pipeline

- Async scanner classification on server start — untagged foods are resolved off-thread and applied to both tooltip and eating paths; average resolution time, slowest item, and recipe timeout tracking in CacheStats

- Raw scores, tokens, token weights, and rejected signal reasons added to ResolutionResult

- /nourished debug held command for in-game classifier inspection; full pipeline trace written to config/nourished/debug/

- /nourished invalidatecache command for operators to force a fresh scan without restarting

- Croptopia compatibility: full nutrient tag coverage for 200+ items including meals, produce, seafood, desserts, and drinks

- Pam's HarvestCraft 2 compatibility: 700+ items tagged across all six nutrient categories

- camelCase token splitting for mods that use concatenated item names (e.g. pamhc2foodextended): improves scanner classification for previously unresolvable items

### Fixed

- Tag matches are now authoritative in blend resolution: when an item has an explicit nutrient tag, the resolver can only contribute nutrients not already covered by the tag, preventing archetype heuristics from overriding curated data

- Resolver cache is now invalidated on level load, fixing stale classifications persisting across jar swaps and config changes

- stew, soup, burger, and roast removed from PREPARATION_TOKENS: they are composite food forms, not preparation methods

- burger and hamburger archetypes now only contribute grains: proteins and vegetables are scored from ingredient keywords and recipe inheritance

- Patchouli crafting recipe for the Nourished Guide

### Changed

- Nutrient tag files updated: proteins, fruits, grains, and dairy now cover a significantly broader range of modded foods including beverages, composite dishes, and items previously falling through to hard fallback

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
