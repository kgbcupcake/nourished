# Changelog

<!-- markdownlint-disable MD013 -->

## [ Unreleased ]

### Added

- Added `INSTANCE_TAGS_README.md`, bundled and copied by MariesLib into `config/nourished/instance_tags/` on load, documenting the single consolidated `instance_tags.json` file (categories keyed within one JSON object) that folder holds.
- Added activity-driven nutrient modules: sprint/swim decay boosts, per-block mining cost, per-kill combat cost, and a one-time starvation penalty applied when a nutrient crosses into critical. Each module (`SprintDecayModule`, `SwimDecayModule`, `MiningModule`, `CombatModule`, `StarvationModule`) is independently toggleable and dispatched through `ActivityModuleDispatcher`/`ActivityModuleRegistry`.
- Added a config-screen category (`ActivityDrivenNutrientCategory`) for adjusting activity-driven nutrient toggles, costs, and per-module HUD log colors.
- Added the Activity Log HUD panel (`ActivityLogHudPanel`): a draggable/resizable, config-toggleable (`enableActivityLogHud`) on-screen log of recent activity-driven nutrient effects for the local player, with its own edit-mode keybind (default `K`), fed by a small client-side ring buffer (`ActivityLogClientBuffer`) synced per-entry from the server.
- Migrated activity-driven nutrient settings off the old `ModConfig.Type.SERVER` TOML spec onto a JSON registry (`ActivityDrivenNutrientRegistry`) at `config/nourished/modules/activity/activity_config.json`, with its own `ACTIVITY_CONFIG_README.md`, datapack override support, and five per-module ARGB colors (mining/combat/sprint/swim/starvation) used to color each Activity Log HUD line — editable via new swatch+hex+reset rows in the config screen, falling back to the theme's default text color when unset.
- Server→client sync for the activity-driven nutrient registry now goes through MarieLib's new generic `MarieResourcesAPI` config-sync mechanism (`registerConfigSyncSupplier`/`registerConfigSyncClientHandler`/`broadcastConfigSyncReload`/`getConfigSyncState`) instead of NeoForge's built-in `ModConfig.Type.SERVER` sync, matching the JSON-registry pattern the rest of Nourished's config already uses.

### Fixed

- Editing a nutrient/activity/panel hex color in the config screen now updates the HUD live as you type, instead of only after Save+reopen — `ColorHexRowWidget` (MarieLib) now pushes the in-progress value to a transient preview override that `MarieAPI.resolveColor` reads immediately.
- The HUD colors "Reset All" button no longer closes and reopens the whole config screen to show the cleared colors; it now calls `ColorHexRowWidget.syncFromEffectiveColor()` on each visible row directly.
- Wired up `registerCompatEntry` in `NourishedDatapackCallbacks`. The 34 datapack-driven compat entries under `data/nourished/nourished/compat/` were being parsed on every datapack apply but silently discarded, since the callback had no override and defaulted to a no-op — none of them ever actually took effect. They now register into `ModCompat` and apply as intended.
- `CommunityTagStage` was a hand-written duplicate of MariesLib's `CommunityTagResolutionStage`, but diverged in behavior: it always deposited into the shared community-tag signal and returned `null` instead of returning a result, so the community-tag cascade never actually terminated, and it never ran the instance-tags OR-check at all. It now delegates directly to MariesLib's `CommunityTagResolutionStage`, so a community-tag match (including instance-tags) is correctly recognized as a confirmed classification wherever this stage is used — most notably in recipe-ingredient confirmation during recipe inheritance.
- Saving the config screen crashed the client with `IllegalStateException: cannot register while frozen`. `ensureCalorieTrackerRegistered()` reopened the MarieAPI registration phase before calling `registerCalorieTracker()`, but by the time the config screen can be saved `TrackerRegistry` itself is already frozen, and `TrackerRegistry.register()` throws on any registration attempt while frozen regardless of the API phase state. It now also unfreezes/refreezes `TrackerRegistry` around the call, matching the pattern already used in `MarieContext.reloadBroadcastHook()`.
- `NourishedSourceRules.isHeavyBlocked` blocked nutrient values from any consumed food at or above `heavySourcePropertyThreshold`, regardless of hunger state, silently preventing normal eating from applying nutrients. It now only blocks when the player can't normally eat (`player.canEat(false)` is `false`), matching the intended "hunger bar full" condition.
- `NutrientClassificationLookup.resolveNutrientBars()` was blending an authoritative `SourceRegistry.getExternalClassification()` hit with the live `RuntimeResolver` recipe-inheritance guess via `TagRuntimeBlend.blend()`, diluting clean external classifications (e.g. `minecraft:porkchop`'s `{proteins=1.0}`) with low-confidence resolver noise. External classification now short-circuits straight to the result, matching the intent already preserved in the `resolveBars(Item)` overload; the resolved/blend path only runs when there's no authoritative external classification.

### CI / Tooling

- Added `check-marielib-update.yml` GitHub Actions workflow to check MarieLib package updates weekly (Mondays 12:00 UTC) or via manual dispatch.
- Workflow queries GitHub Packages Maven metadata and opens a PR against `dev` when a newer MarieLib version is detected.
- Update process requires `MARIELIB_PACKAGES_TOKEN` (PAT with `read:packages`) and does not auto-merge or target `main`.

---

[ nourished 0.2.7-beta.1]

[ nourished 0.2.7-beta] - 2026-07-13

## Notes

> A lot has changed in this update and some of the changes are breaking. Please read the changelog carefully and check
> your configs and datapacks for any necessary updates. This also includes MariesLib updates
> several packages/classes were renamed or moved (tooltip helpers, override file layout). Please check the MariesLib
> changelog for details.

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
- Added auto-generated `Read_Me/` README files (`LOCKS_README.md`, `EFFECTS_README.md`, `FOOD_VALUES_README.md`,
  `NUTRIENTS_README.md`, `NUTRIENT_CURVES_README.md`, `RAW_FOOD_README.md`) written from bundled resources into each
  registry's config directory on first load, if not already present.
- Wired Nourished's tooltip lines into MarieLib's `TooltipColorRegistry`/`TooltipMessageRegistry`, including an
  `excluded` message key and `nourished.tooltip.excluded` lang entry for excluded items. Added
  `NourishedTooltipDefaults` to seed `tooltip_colors.json`/`tooltip_messages.json` with Nourished's real nutrient colors
  and excluded-item message on first run.
- Added `TOOLTIP_COLORS_README.md` / `TOOLTIP_MESSAGES_README.md` to Nourished's own `data/nourished/config/` resources:
  MarieLib's bundled copies were never reachable at runtime (looked up under `data/<modId>/config/...` using Nourished's
  own modId, but bundled under marie-ui's `marieslib` namespace instead), so each consumer now needs its own copy.
- Added `COLORS_README.md` / `SCANNER_SPEC_README.md` to Nourished's own `data/nourished/config/` resources for the same
  reason: MarieLib's `ColorRegistry`/`ScannerSpecRegistry` bundled their READMEs under marie-core's own `marieslib`
  namespace instead of the consuming mod's, so they were never reachable.
- Added a debug-only live size readout (`width x height`) next to the active resize handle while dragging/resizing a
  Diet Screen edit-mode box, to help correlate box size with `ActiveEffectsComponent`'s visibility threshold. No config
  toggle — it only shows during an active drag.
- Added a per-box text/icon zoom to all five Diet Screen left-column sub-boxes (Calories/Balance/Recent Meals/Eat more
  of.../Active Effects), independent of each box's own proportional fit scale: left-double-click a box in edit mode to
  enter zoom mode (scroll adjusts that box's zoom),
  right-double-click to exit. Zoom is persisted per box via MarieLib's `ComponentState#contentScale` (the same store as
  each box's own position/size, keyed by component ID) instead of the previous standalone `caloriesContentScale`/
  `balanceContentScale`/`recentMealsContentScale`/`eatMoreContentScale`/`activeEffectsContentScale`
  `nourished-client.toml` entries, which are now obsolete and stripped on load (any previously-set zoom resets to
  default, same as other one-time persisted-schema changes in this file). Zoom stays live-clamped every render to that
  box's own current single-axis fit range, so it can never exceed what a single-axis-only resize of that box would
  already produce, and never shrinks/grows the box's own outer rectangle. A small "zoom x\_.\_\_" label shows under a box
  in edit mode while it's zoomed.

### Changed

- Reorganized `client/hud/` and `client/screen/diet/` into `dynamic/{layout,modules,edit,visibility,persistence}` and `classic` packages to separate MarieUI and legacy UI implementations.
- HUD nutrient panel background now renders with rounded corners to match the classic renderer.
- Updated imports across API, config, context, effect, handler, nutrition, kubejs, and template classes to match MarieLib's restructured package layout (e.g. `dev.marie.framework.api.value`, `.effects`, `.marieapi`, `.progression`, `.reporting`, `.source`).
- Renamed `DeathNutritionBehavior` to `RespawnValueBehavior` (MarieLib rename) and updated all references.
- Renamed `MarieApiRegistries.freezeModOnlyRegistriesAfterCommonSetup` to `freezeValueTrackingOnlyRegistriesAfterCommonSetup`.
- Removed the per-item resolution cache from `RuntimeFoodResolver` in favor of always resolving uncached, now that ingredient scoring can consult the community-tag/keyword-suffix stages.
- Moved config overrides (`food_overrides.json`, `source_classifications.json`, `excluded_items.json` and their READMEs) from `config/nourished/` directly into a new `config/nourished/overrides/` subfolder. **Breaking:** update any datapacks/scripts that read or write these files at the old path.
- `food_overrides.json` moved from `config/nourished/overrides/` into `config/nourished/overrides/Overrides/`, with its
  README moved into a new `overrides/Read_Me/` folder; existing files are migrated automatically on load.
- `food_overrides.json`'s `nutrients` now merges over normal tag/scanner classification instead of fully replacing it:
  any key you list overrides that nutrient's value (including explicit `0` to zero it out), and any omitted key still
  falls back to whatever Nourished would normally classify. `calories` remains a full override. (
  `NutrientClassificationLookup`, `NourishedContextBuilder`, `OVERRIDES_README.md`)
- Updated import for MarieLib's tooltip package restructure (`dev.marie.framework.compat.MarieTooltipHelper` →
  `dev.marie.framework.tooltips.MarieTooltipHelper`).
- `CaloriesComponent`/`BalanceComponent` now share their local-to-screen coordinate mapping and draw helpers (`sx`/`sy`/
  `sd`/`drawText`/`drawItem`/`drawOuterBox`) via a new `SummaryBoxRenderSupport`, removing the duplicate implementations
  that previously lived in both classes identically. `RecentMealsComponent`/`EatMoreComponent`/`ActiveEffectsComponent`
  remain independent, per their existing separation.
- Diet Screen left-column sub-boxes (Calories/Balance/Recent Meals/Eat more of.../Active Effects) no longer collapse
  from fully-visible to fully-gone the instant their header stops fitting. Each box's header and body content now scale
  down together continuously as the panel shrinks — the same style of shrink `EatMoreComponent`'s icon row already
  used — and only actually disappear once there's less than a handful of local units of room left. Recent Meals/Active
  Effects also no longer drop whole rows/lines one at a time as room tightens; every natural row/line still draws, just
  smaller, until the box itself fades out.
- Removed the now-unused hard-cutoff helpers (`DietLayout#headerFitsInPanel`/`#bodyBlockFitsInPanel`/
  `#bodyBlockRoomInPanel`/`#stackedBodyUnitsFit`) in favor of the new continuous `DietLayout#roomInPanel`.
- The continuous fade only applies to a box's own natural (never manually dragged/resized) size — a sub-box the player
  has independently resized keeps rendering at that persisted size regardless of how the main panel is later resized,
  since its size is that box's own property, not something the panel should silently override.

### Fixed

- Fixed the Diet Screen open keybind so pressing it while a Diet Screen (classic or MarieUI) is already open now closes it instead of leaving a duplicate/reopened screen.
- Fixed classic HUD/Diet Screen left-edge resize so shrinking the panel back down actually reduces the reserved left margin instead of getting stuck at the width that created it.
- Fixed classic Eat More panel resize clamping so it cannot grow large enough to push Active Effects below its minimum rendering budget within the fixed left-column layout.
- Fixed The `food_overrides.json` not being wired into the `RuntimeFoodResolver` so overrides were not being applied at runtime.
- `RuntimeFoodResolver` now also checks `ExcludedItemsRegistry.isExcluded(...)` (in addition to `ScannerSpecRegistry`'s
  `excludedItems()`) before running the inference cascade, matching `NutrientClassificationLookup`'s exclusion check.
  Previously an item excluded only via `excluded_items.json` could still enter full inference if resolved directly
  through `RuntimeFoodResolver`.
- Fixed `RecentMealsComponent`'s meal rows overlapping at higher zoom: the zoomed text/icon draw size grew with the
  box's per-box zoom multiplier, but the row-to-row (and header-to-first-row) vertical spacing stayed fixed at the
  unzoomed size, so bigger zoomed rows visually collided into their neighbors instead of spreading apart. The row (and
  header) vertical advance is now stretched by the same ratio zoom grows draw size by, so spacing and content grow
  together; fewer rows now visibly fit at higher zoom, which is expected (the existing `pushClip` already hides the rest
  gracefully). The shared zoom ceiling in `DietScreenModules#zoomedTextIconScale` (`max(widthScale, heightScale)`)
  needed no RecentMeals-specific change: because the row/header advance is derived from the already-clamped draw scale,
  the screen-pixel height header+rows consume at any given scale reduces algebraically to `recentHeight * scale`, making
  the existing `heightScale` already the exact scale at which content fills the box's live height.
- Fixed `ActiveEffectsComponent`'s effect lines overlapping at higher zoom, the same latent bug as
  `RecentMealsComponent` above (header-to-first-line and line-to-line advance now stretch by the same zoom ratio as text
  draw size). The shared zoom ceiling again needed no per-box adjustment, including for this box's dynamic effect count:
  `effectsBoxH` is a fixed per-instance reference captured from the player's _current_ effect count at construction (
  mirroring `recentHeight`), and a fresh instance is built (and `effectsBoxH` re-derived) every render pass, so the same
  algebraic reduction to `effectsBoxH * scale` holds regardless of how many effects are active.
- Fixed Diet Screen edit-mode boxes (Calories/Balance/Recent Meals/Eat more of.../Active Effects) not reflowing live
  while an earlier box in the stack was being dragged or resized: the sibling-stacking chain only ever read a box's last
  _committed_ size, so a box being grown mid-drag visually overlapped whatever came after it, and
  `ActiveEffectsComponent` could appear to vanish mid-drag even with room on screen because its fit check was still
  evaluated against the stale, pre-drag start position. `DietScreenPersistence` now accepts a per-frame live override (
  set by `DietScreenEditTarget` for whichever box is actively dragging, cleared right after) so the module chain sees
  the box's true live bounds.
- Fixed `ActiveEffectsComponent` staying hidden (or losing its effect lines) with visibly empty room left in the panel:
  the left column's sibling-stacking chain reserves a box's full natural height for whatever comes after it based on
  registration order alone, regardless of where that box actually renders. A box dragged sideways out of the
  single-width column — e.g. `EatMoreComponent` repositioned to sit beside `RecentMealsComponent` instead of below it, a
  common manual layout — still reserved its full height as dead space in the chain, pushing Active Effects' start
  position down into that unused gap and past the panel's live bottom edge.
  `DietLeftColumnComponent#nextSiblingStartLocalY` now skips the height reservation for a sibling whose resolved X has
  drifted away from the column's own left edge, since it's no longer occupying a vertical slot in the flow.
- Fixed the per-box zoom scroll range being nearly dead: `DietScreenModules#zoomedTextIconScale`'s clamp floor (
  `min(widthScale, heightScale)`) was mathematically identical to the `fitScale` value already being scaled, so
  scrolling the zoom multiplier below 1.0 always clamped straight back to fit-scale with no visible effect. The floor is
  now a real fraction of `fitScale` (half of it) instead, since shrinking below fit-scale is always safe — it only makes
  content smaller than the box, never clips it. The ceiling (`max(widthScale, heightScale)`) is unchanged.
- Fixed `EatMoreComponent`'s double-click not entering zoom mode: its default (never manually repositioned) Y position
  is chained after `RecentMealsComponent`'s resolved height, which depends on how many recent meals are currently
  tracked — a value that can change frame-to-frame, shifting `EatMoreComponent`'s resolved bounds between the two clicks
  of a double-click and failing the second click's hit-test. `DietZoomController#onClick` now snapshots the bounds a
  box's first click hit-tested against and reuses that same snapshot for a following click within the double-click
  window, instead of re-reading live bounds on each click.
- Fixed `RecentMealsComponent` text/icons overflowing past the box's edges at higher zoom, and per-box zoom appearing
  dead for boxes (like `EatMoreComponent`) that hadn't been resized non-uniformly. All five left-column sub-boxes
  already wrap their entire zoomed draw (header included) in a
  `context.pushClip(bounds.x(), bounds.y(), bounds.width(), bounds.height())` scissor around the box's own live bounds,
  so that clip — not any scale math — is what actually guarantees zoomed content can never paint outside the box,
  however large `scale` gets. `DietScreenModules#zoomedTextIconScale`'s ceiling no longer needs to be derived per-axis
  from `widthScale`/`heightScale` at all (the previous `max(widthScale, heightScale)` was only safe on the height axis,
  and briefly `widthScale` alone, both explored while chasing this) — it's now a flat `fitScale * 3.0` (paired with the
  existing `fitScale * 0.5` floor), giving every box a real, resize-independent 6x zoom range regardless of its aspect
  ratio. `RecentMealsComponent`'s row-name and header truncation (budgeted against each string's actual draw scale, not
  the stale `contentScale`) is kept as a cosmetic nicety — a clean "..." instead of a mid-glyph scissor cut — rather
  than the thing preventing overflow.
- Fixed a Diet Screen sub-box's per-box zoom silently resetting to default on the next drag or resize of that same box:
  `DietScreenEditTarget#toRelativeState` (the shared commit callback for all five left-column sub-boxes) constructed a
  brand-new `ComponentState` from the drag/resize geometry alone, defaulting `contentScale` (and `leftMargin`) back to
  their record defaults instead of preserving whatever was already persisted. It now loads the box's existing
  `ComponentState` first and copies every field it doesn't itself own (`contentScale`, `leftMargin`) through from that
  loaded state, the same read-modify-write pattern `DietScreenPersistence#adjustContentScale` already used correctly.

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
