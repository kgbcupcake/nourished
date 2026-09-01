# Changelog

<!-- markdownlint-disable MD013 -->

## [ Unreleased ]

[ nourished 0.2.7-beta.1]

### Added

- Eating a food that actually changed calories or nutrients now triggers a client-side notification anchored above the XP bar (MarieLib's `MarieNotifications`/`NotificationRequest` facade), showing the food's display name and its calorie delta. Rapid consecutive eats merge into the same notification slot (`mergeKey`/`mergeWindowTicks`) instead of stacking a new one per food. Threaded through a new nullable `SyncDietDeltaPayload.FoodEatenDelta` (item id, calorie delta, nutrient deltas), populated only in `NourishedFoodTriggerHandler#fireSourceTriggerAndNotifyFoodEaten` when the eat had a real effect — every other diet-delta sync reason (decay tick, login, state refresh) sends it as `null`, so no notification fires on unrelated syncs. `MarieNotifications.registerClientListeners()` — previously uncalled anywhere — is now wired into `ClientEventRegistrar#register`.

- Added `INSTANCE_TAGS_README.md`, bundled and copied by MariesLib into `config/nourished/instance_tags/` on load, documenting the single consolidated `instance_tags.json` file (categories keyed within one JSON object) that folder holds.
- Added activity-driven nutrient modules: sprint/swim decay boosts, per-block mining cost, per-kill combat cost, and a one-time starvation penalty applied when a nutrient crosses into critical. Each module (`SprintDecayModule`, `SwimDecayModule`, `MiningModule`, `CombatModule`, `StarvationModule`) is independently toggleable and dispatched through `ActivityModuleDispatcher`/`ActivityModuleRegistry`.
- Added a config-screen category (`ActivityDrivenNutrientCategory`) for adjusting activity-driven nutrient toggles, costs, and per-module HUD log colors.
- Added the Activity Log HUD panel (`ActivityLogHudPanel`): a draggable/resizable, config-toggleable (`enableActivityLogHud`) on-screen log of recent activity-driven nutrient effects for the local player, with its own edit-mode keybind (default `K`), fed by a small client-side ring buffer (`ActivityLogClientBuffer`) synced per-entry from the server.
- Migrated activity-driven nutrient settings off the old `ModConfig.Type.SERVER` TOML spec onto a JSON registry (`ActivityDrivenNutrientRegistry`) at `config/nourished/modules/activity/activity_config.json`, with its own `ACTIVITY_CONFIG_README.md`, datapack override support, and five per-module ARGB colors (mining/combat/sprint/swim/starvation) used to color each Activity Log HUD line — editable via new swatch+hex+reset rows in the config screen, falling back to the theme's default text color when unset.
- Server→client sync for the activity-driven nutrient registry now goes through MarieLib's new generic `MarieResourcesAPI` config-sync mechanism (`registerConfigSyncSupplier`/`registerConfigSyncClientHandler`/`broadcastConfigSyncReload`/`getConfigSyncState`) instead of NeoForge's built-in `ModConfig.Type.SERVER` sync, matching the JSON-registry pattern the rest of Nourished's config already uses.
- `CalorieHudScreen` and `ActivityLogHudPanel` gained border opacity and background/border shade sliders (`calorieHudBorderOpacity`/`calorieHudBackgroundShade`/`calorieHudBorderShade` and their `activityLogHud` equivalents), using MarieLib's new `MarieColors.withOpacity`/`MarieColors.shade` to darken/lighten and fade each panel's background and border independently.
- `CalorieHudScreen.onRenderGuiPost` and `ActivityLogHudPanel.onRenderGuiPost` now return early when `Minecraft.getInstance().options.hideGui` is set, matching MarieLib's `NotificationRenderer`. `RenderGuiEvent.Post` fires unconditionally from `Gui.render()` regardless of F1/hideGui (only individual vanilla layers are internally gated), so without this check both panels kept drawing over a hidden GUI.
- `CalorieHudScreen` and `ActivityLogHudPanel` gained double-click-to-zoom, mirroring the Diet Screen's `DietZoomController` pattern but simplified for a single box: left-double-clicking the panel enters zoom mode, right-double-click exits it, and while zoomed the scroll wheel adjusts a persisted `contentScale` multiplier (via `ComponentState`, `[0.1, 5.0]`, same clamp range as `DietScreenPersistence#adjustContentScale`) that's layered on top of the existing box-fit shrink scale for both the row font size and row height/spacing. A "zoom x1.50"-style indicator shows below the box while zoomed, matching `DietScreenEditTarget#drawZoomLabel`'s look, drawn only during edit mode.
- Added a new unbound keybind (`OPEN_SCALE_CONFIG`) that ensures the dynamic Diet Screen is open and toggles a MarieLib `ScaleConfigPanel` overlay owned directly by `DietScreen`, wired to the five existing sub-boxes (`CaloriesComponent`/`BalanceComponent`/`RecentMealsComponent`/`EatMoreComponent`/`ActiveEffectsComponent`) so their persisted content-scale/padding can be adjusted with sliders instead of double-click+scroll. The panel renders and handles clicks/scrolls on top of the base Diet Screen without disabling its normal drag-to-reorder behavior. Rebindable from the Diet Screen config category alongside the other diet-screen hotkeys.
- Added `zh_cn.json`, a Simplified Chinese translation covering every key in `en_us.json`, one-to-one. First-pass machine-assisted translation — a native-speaker review pass is recommended before treating it as final.
- Added five new daily MarieLib trackers for the activity-driven nutrient modules — `nourished:activity/mining_blocks`, `activity/combat_kills`, `activity/starvation_crossings`, `activity/sprint_distance`, `activity/swim_distance` (`ActivityTrackerIds`) — registered the same way, at the same call sites, and with the same retention as the existing calories tracker. `MiningModule`, `CombatModule`, and `StarvationModule` each increment their tracker by 1 on trigger. Sprint/swim distance is now tracked in real blocks moved rather than ticks-while-active: a new `ActivityDistanceTracker` keeps a per-player last-known-position cache, updated every server player tick by a new `ActivityDistanceTickListener` (`PlayerTickEvent.Post`), and accumulates the actual distance moved into `sprint_distance`/`swim_distance` while sprinting/swimming — both can accrue in the same tick when sprint-swimming. A first tick, a dimension change, or a single-tick jump past a 20-block sanity cap is treated as a teleport/discontinuity (position cached, no distance recorded) rather than counted as movement; the cache entry is cleared on logout.
- Added a new unbound keybind (`OPEN_COMMAND_CENTER`) that opens MarieLib's new generic `MarieCommandCenter` screen. Registered one `CommandCenterCategory` ("Diet Screen") with one `CommandCenterCard` ("Text Scale & Padding") whose click handler reuses `ClientEvents#openOrToggleScaleConfig` — the same method `OPEN_SCALE_CONFIG`'s own handler calls — so both entry points open/toggle the same Diet Screen scale-config overlay. Rebindable from the HUD & Display config category.
- Command Center is now exclusively a command launcher: added two new `CommandCenterCategory` entries in `ClientEventRegistrar`. "Nourished Tools" has three cards — "Export All" (`nourished export_all`), "Audit Tags" (`nourished audit_tags`), and "Debug Activity Log" (`nourished debug activitylog`). "Framework" has four — "Status" (`marie status`), "Mods" (`marie mods`), "API" (`marie api`), and "Registries" (`marie registries`). All seven `onClick` handlers go through a new private `ClientEventRegistrar#dispatchCommand(String)` helper calling `Minecraft.getInstance().player.connection.sendCommand(...)` — the same `ClientPacketListener#sendCommand` method the chat screen itself calls on command submit — so these run as real network-dispatched commands through the server's actual dispatcher, with `hasPermission(2)` enforced exactly as if typed. No existing Nourished/MarieLib helper for client-side command dispatch was found anywhere in the codebase, so this wraps the vanilla method directly rather than inventing a new mechanism.
- Added six more cards to the "Nourished Tools" Command Center category, using the same `dispatchCommand` network-dispatch pattern as the existing three: "Reload" (`nourished reload`), "Invalidate Cache" (`nourished invalidatecache`), "Unassigned Sources" (`nourished get_unassigned`), "NBT Paths" (`nourished nbt`), "List Profiles" (`nourished profile list`), and "My Profile" (`nourished profile get`) — all six are `MarieConsumerCommandTree`'s per-mod subcommands, registered under the `nourished` root literal alongside `NourishedCommand`'s own subcommands (Brigadier merges multiple `dispatcher.register(literal("nourished")...)` calls into one node), _not_ under a separate `marie` root.
- Added a global "edit all HUDs" toggle via MarieLib's new `EditModeCoordinator`: `CalorieHudScreen` and `ActivityLogHudPanel` are registered with it in `ClientEventRegistrar#register`. A new unbound keybind (`EDIT_ALL_HUDS`) calls `EditModeCoordinator.toggleAll()` from `ClientEvents#onClientTick` — entering or exiting edit mode on every registered HUD panel together, with no HUD-specific logic in the handler itself. Rebindable from the HUD & Display config category alongside `EDIT_HUD`.
- The nutrient HUD joined the same "edit all HUDs" group: `ClientEventRegistrar#register` now also calls `EditModeCoordinator.registerGroupCapable("nourished.hud", NourishedHUD::editTarget, ...)`, targeting the same `HudEditTarget` singleton the H keybind's own `EditModeController` already wraps. `NourishedHUD` gained a minimal public `editTarget()` accessor (mirroring `CalorieHudScreen`/`ActivityLogHudPanel`'s own `instance()`, and how `DietScreen#marieEditModeController()` was exposed earlier for the same kind of cross-class need) that delegates to the existing private `marieEditModeController()` lazy-init instead of building a second `HudEditTarget` independently, so both entry points always share the exact same target instance. This is purely a second entry point onto the existing target — the individual H keybind (`NourishedHUD#onClientTick` → `marieEditModeController().enter()`) is untouched and still works standalone.
- `CalorieHudScreen` and `ActivityLogHudPanel` each gained their own `ScaleConfigPanel` (a single-entry card for their own persisted content-scale/padding, backed by `UiStatePersistence`), auto-shown alongside their respective edit modes — same pattern as the Diet Screen's own `ScaleConfigPanel`/`enterEditModeWithScaleConfig`, just one card instead of five since neither HUD has sub-boxes. Pressing C (`EDIT_CALORIE_HUD`) or K (`EDIT_ACTIVITY_LOG_HUD`) now also flips that panel's own `scaleConfigVisible` on before entering edit mode, via new private `enterEditModeWithScaleConfig()` helpers on each class; both panels forward `mouseClicked`/`mouseScrolled` to their `ScaleConfigPanel` first (falling through to normal drag/resize handling otherwise) and render it top-right during edit mode, without disturbing either panel's existing drag/resize behavior.
- All three HUD panels (`CalorieHudScreen`, `ActivityLogHudPanel`, `HudEditTarget`) now register their live/persisted bounds with MarieLib's new `dev.marie.framework.ui.api.SnapRegistry`, under each panel's own existing `PANEL_ID` constant, and wire that same id into their `DraggableResizable` via the new `setSnapRegistryId` call — so dragging or resizing any one of the three now snaps to the current on-screen edges of the other two (and of any other mod's own `SnapRegistry`-registered component), without excluding itself from its own snap candidates.
- Added `NourishedKubeBindings#registerTrackerMilestone`, exposed to KubeJS scripts as `NourishedAPI.registerTrackerMilestone({...})` (auto-bound, no `NourishedKubePlugin` changes needed). Mirrors `registerNutrient`'s `Map<String, Object>` spec-parsing pattern: `id`, `trackerId`, `goal`, and `scope` (`"lifetime"`/`"current_period"`, mapped to `TrackerMilestoneDefinition.MilestoneScope`) are required, with optional `rewardEffectId`/`rewardAmplifier`/`rewardDuration`/`advancementId` reward fields, before building and registering via `TrackerMilestoneRegistry.register(...)`.
- Migrated the nutrient HUD (`NutrientBarComponent`/`NutrientPanelContainer`) off `hudLayout.scale()`-driven text/icon rendering onto the same `ContentScaleController`/`ScaleConfigPanel` pattern already used by `CalorieHudScreen`/`ActivityLogHudPanel`/the five Diet Screen sub-boxes: `HudEditTarget` gained a single-entry `ScaleConfigPanel` (via `MarieScaleConfig.create`, backed by the same `UiStatePersistence` record `#commit` already writes this panel's position/size into) plus public `persistedContentScale()`/`persistedPaddingScale()` accessors. `NutrientPanelContainer` now resolves `ContentScaleController.resolveContentScale(HudEditTarget.persistedContentScale())` once per frame and passes it down to each `NutrientBarComponent`, which uses it (not `hudLayout.labelScale()`/icon-size-derived scale) for every drawn text/icon call, wrapped in its own `pushClip` for containment; the panel's outer content inset switched from `hudLayout.scaledPad()` to `ContentScaleController.resolvePadding(...)` similarly. `hudLayout`'s own scale (`cc.hudScale()`-derived) is unchanged and still drives every row/column geometry value (row height, icon/bar/label offsets, natural panel size) — same box-geometry/content-scale separation the other 7 modules maintain. Unlike those modules, this one skips the double-click+scroll trigger entirely (card-only editing): `HudEditTarget#commit` was also fixed to carry forward the persisted contentScale/paddingScale so a drag/resize commit no longer resets them, and `NourishedHUD` gained `enterEditModeWithScaleConfig()`/`showScaleConfigOnGroupEntry()` (wired into `ClientEventRegistrar`'s `registerGroupCapable` call) so the card auto-shows alongside edit mode via either the H keybind or the "edit all HUDs" toggle, matching the other panels' behavior.
- Wired the activity-driven nutrient modules into MarieLib's new tracker milestone system: `MiningModule`, `CombatModule`, and `StarvationModule` each now call `TrackerMilestoneTracker.onTrackerIncremented(player, trackerId, amount)` immediately alongside their existing `MarieTracking.incrementTracker(...)` call, with the same player/tracker id/amount. Sprint and swim distance are tracked outside those two modules — in `ActivityDistanceTracker#onPlayerTick`, where the actual `MarieTracking.incrementTracker` calls for `SPRINT_DISTANCE_ID`/`SWIM_DISTANCE_ID` live — so the sibling `onTrackerIncremented` calls were added there instead, at both call sites. Matches the "sibling call, not a replacement" pattern `TrackerMilestoneTracker` was designed around (it isn't wired into `MarieTracking.incrementTracker` itself); for players with no tracker milestones registered, `onTrackerIncremented` is a no-op cost — a lifetime-counter update plus a `TrackerMilestoneRegistry.getForTracker` lookup that finds nothing — so existing behavior is unchanged.
- Tagged createfood's untagged base/sub-ingredient items into `data/nourished/tags/item/nutrients/*` so the recipe-inheritance resolver can see through them when classifying finished dishes. `grains`: every dough variant (`salt_dough`/`_small`, `wheat_dough_small`, `sugar_dough_small`, `butter_dough`/`_small`, `pizza_dough`, `pita_dough`, `pumpernickel_dough`, `ube_sugar_dough`, `chocolate_sugar_dough`/`_small`), the `raw_calzone` shell, `dumpling_wrappers`, `raw_macaroni`, the raw pastry/sweet-roll bases (`raw_pastry_base`, `raw_chocolate_pastry_base`, `raw_sweet_roll_base`, `raw_chocolate_sweet_roll_base`, `raw_cinnamon_sweet_roll_base`), `cake_base`/`chocolate_cake_base`, the `*_batter_bucket` items (`cake_batter_bucket`, `chocolate_cake_batter_bucket`, `waffle_batter_bucket`, `ube_cake_batter_bucket`), and the cocoa/confectionery intermediates (`pressed_cocoa`, `cacao_nibs`, `cacao_butter`, `cacao_mass_bucket`, `cocoa_powder`, `chocolate_chips`, `dark_chocolate_chips`, `caramel_chips`, `toffee_chips`, `butterscotch_chips`). `dairy`: `cheese_block`, `cream_cheese`, `cream_cheese_bucket`, `cheesecake_filling_bucket`, `liquid_cheese_bucket`, `butter`, `heavy_cream_bottle`/`_bucket`, `condensed_milk_bucket`, `milk_powder`, `custard_bucket`. `vegetables`: `taco_sauce_bottle`/`_bucket` and the cut-vegetable prep items (`diced_onion`, `diced_tomato`, `sliced_onion`, `sliced_tomato`, `sliced_carrot`, `sliced_beetroot`, `shredded_carrot`, `shredded_beetroot`). `proteins`: the ground meats (`ground_beef`/`_pork`/`_chicken`/`_mutton`/`_rabbit`/`_sausage`/`_endermite`, `minced_dragon`), raw meatballs/patties (`raw_beef_meatball`, `raw_pork_meatball`, `raw_rabbit_meatball`, `raw_endermite_meatball`, `raw_strider_meatball`, `raw_chicken_patty`, `raw_sausage_patty`), the previously-inconsistent `rabbit_meatball`/`strider_meatball` and their `_stick_1`/`_stick_2` forms, `rabbit_cuts`, `rabbit_jerky`, `gyro_meat_block`, `raw_gyro_meat_block`, and `egg_powder`/`egg_whites_bottle`/`egg_whites_bucket`/`egg_bucket`. Compound items are listed in every applicable file: `raw_cheese_calzone` and `white_chocolate_chips` in `grains`+`dairy`, `pizza_dough_tomato_sauce` in `grains`+`vegetables`, `raw_cheese_pizza` in `grains`+`dairy`+`vegetables`. `gelatin`/`gelatin_mix_bucket`/`gelatin_dessert_block` (all 17 dye-colour variants), `powdered_sugar`/`brown_sugar`/`corn_flour`, and the `*_crumbs` items (`graham_cracker_crumbs`, `chocolate_graham_cracker_crumbs`, `cookie_crumbs`, `bread_crumbs`) were deliberately left untagged as category-neutral staples.

### Changed

- Followed MarieLib's `MarieNotifications`/`MarieCommandCenter`/`EditModeCoordinator` consolidation into the new `dev.marie.framework.ui.api` facade package (moved from `dev.marie.framework.notification`/`dev.marie.framework.ui.commandcenter`/`dev.marie.framework.ui.edit` respectively): updated imports in `ClientNetworkCallbacks` (`MarieNotifications`), `ClientEventRegistrar` (`MarieNotifications`, `EditModeCoordinator`), and `ClientEvents` (`MarieCommandCenter`, `EditModeCoordinator`). `DietScreen`/`CalorieHudScreen`/`ActivityLogHudPanel`/`DietScreenEditTarget` needed no import change here — none of them referenced those three facade classes directly, only the still-unmoved `EditModeController`/`ScaleConfigPanel` types. Also switched all 3 direct `new ScaleConfigPanel(...)` construction sites (`DietScreen`, `CalorieHudScreen`, `ActivityLogHudPanel`) to the new `MarieScaleConfig.create(entries, persistence, anchor)` facade instead of constructing `ScaleConfigPanel` directly; the returned type is unchanged, so nothing downstream (`DietScreenEditTarget`'s `ScaleConfigPanel` field/constructor param included) needed touching.
- `CalorieHudScreen` and `ActivityLogHudPanel`'s `EditModeCoordinator` registration (see Added above) now goes through MarieLib's new `EditModeCoordinator.registerGroupCapable(String, MarieComponent, String, int)` instead of the plain `register(String, EditableComponent)` path, passing each panel's actual singleton (`instance()`, now public on both classes for this) plus its existing hint-banner text and exit key code. Under the plain-`EditableComponent` path, `EditModeCoordinator.enterAll()` drove each panel's own independent `EditModeController.enter()`, and since each one calls `Minecraft.setScreen(...)` with its own single-target overlay, only the last-registered panel's overlay ever ended up actually visible/interactive when toggling edit mode on every HUD at once — the earlier panel's controller reported itself active internally but was never rendered. `registerGroupCapable` instead combines every group-capable registrant into one shared `EditOverlayScreen` (`EditModeController.enterGroup`/`exitGroup`) with a single `setScreen()` call, so both panels are genuinely editable together. Each HUD's individual C/K keybind is unaffected — those still call `editModeController().enter()` directly on that panel's own single-target `EditModeController`, untouched by this change. `CalorieHudScreen`/`ActivityLogHudPanel`'s now-unused `editableComponent()` accessors (and their backing `EditableComponent` import/field) were removed as dead code.
- Simplified the food-eaten notification (see Added above) to the food display name and calorie delta only. The per-nutrient delta lines have been removed entirely; `HudDrawHelpers#nutrientColorArgb`/`#nutrientLabel` are no longer consulted when building this notification.
- The food-eaten notification's name and calorie delta now render as two `TextSegment`s on a single line (e.g. "Veggie Salad +203 Calories") instead of two stacked lines, matching the original layout.
- The Diet Screen's other two edit-mode entry points — the J (`EDIT_DIET_SCREEN`) keybind and clicking the edit-mode toggle button — now also turn the scale-config sliders on when entering edit mode, matching `ClientEvents#openOrToggleScaleConfig`'s behavior, so the scale panel appears alongside edit mode no matter how it's entered. Both call a new private `DietScreen#enterEditModeWithScaleConfig()` helper (`scaleConfigVisible = true;` then `marieEditModeController().enter()`) instead of duplicating that pairing inline. This is deliberately separate from `marieEditModeController()` itself, which `openOrToggleScaleConfig()` still calls directly after managing `scaleConfigVisible` (including toggling it back off) — folding the forced-`true` into `marieEditModeController()` would have stomped that toggle-off. Only entry is affected; exiting edit mode (J/Esc, or the toggle button's exit branch) and the scale panel's own separate show/hide toggle are unchanged.
- `ClientEvents#openOrToggleScaleConfig` (used by both the `OPEN_SCALE_CONFIG` keybind and the Command Center's "Text Scale & Padding" card) now also enters the Diet Screen's edit mode automatically after opening/finding it and toggling the scale-config sliders — same as pressing J — so the panel is immediately draggable without a separate manual step. `DietScreen#marieEditModeController()` is now public so callers outside the screen can drive it; entering is a no-op if the player already pressed J themselves, and exiting edit mode remains untouched by this method.
- `CalorieHudScreen` and `ActivityLogHudPanel` now delegate their double-click-to-adjust zoom to MarieLib's new generic `ContentScaleController` instead of each hand-rolling its own `DoubleClickRecognizer` + `contentScale`-only zoom logic. Both panels keep their existing box-driven proportional scaling (resizing the HUD still scales its content proportionally) unchanged; `ContentScaleController` layers the user's adjustment on top of that proportional scale rather than replacing it, and clamps the result back toward it as a fit-protection measure. Double-left-click still adjusts text/content scale as before; double-right-click now adjusts a new, independent padding multiplier (`ComponentState#paddingScale`) instead of only exiting zoom mode. The edit-mode overlay now reads "TEXT SCALE 110%" or "PADDING 125%" depending on which mode is active, replacing the old "zoom x1.50" label. Dragging or resizing either panel no longer resets its persisted text-scale/padding adjustment back to defaults.
- The Diet Screen's five independently draggable sub-boxes (`BalanceComponent`/`CaloriesComponent`/`RecentMealsComponent`/`EatMoreComponent`/`ActiveEffectsComponent`) now use the same `ContentScaleController` MarieLib gained for the HUD panels above, replacing the hand-rolled `DietZoomController` (now deleted). Each box's existing KDE-style two-axis proportional scaling (`Math.min(widthScale, heightScale)`) is unchanged and still what drives its content size; `ContentScaleController` layers the user's per-box adjustment on top exactly as `DietScreenModules#zoomedTextIconScale` already did (identical `[fitScale*0.5, fitScale*3.0]` clamp), so nobody's saved zoom level changes size on upgrade. Double-right-click on a box now enters an independent padding-adjustment mode instead of only exiting zoom, insetting that box's content from its own top-left corner without affecting its size, position, or its siblings' snapping/stacking (all still handled entirely separately, by `DraggableResizable`/`DietScreenEditTarget`'s snap-target wiring). Each of the five boxes keeps its own persisted `contentScale`/`paddingScale` — nothing is shared or combined across boxes.
- `CalorieHudScreen` and `ActivityLogHudPanel` now draw their outer card in the same filled rounded-rect style as MarieLib's `ScaleConfigPanel` (`RenderContext#drawRoundedRect` with each panel's resolved background/border color, replacing the old flat `fillRect`+`drawBorder` pair) and gained a title header row — "Calorie History" in an orange-red accent, "Activity Log" in a green accent, both picked from `ScaleConfigPanel.ACCENT_PALETTE` and drawn at a fixed, unscaled size matching `ScaleConfigPanel`'s own header treatment. Each panel's natural size grew to fit the new header row above its existing data rows; the data rows themselves (Today/Yesterday/N-days-ago for the calorie panel, per-module log entries for the activity panel) are otherwise unchanged, as is the `MIN_SHRINK_SCALE`/content-scale/`ScaleConfigPanel` wiring both panels already had — purely a visual restyle of the outer card.
- `ActivityLogHudPanel` now reads its five rows (Mining/Combat/Sprint/Swim/Starvation) from the persistent `nourished:activity/*` MarieLib trackers via `MarieTracking.getCurrentTrackerValue` against the local client player, instead of `ActivityLogClientBuffer`'s session-only "last event fired" counts — same client-side-safe accessor `CalorieHudScreen` already uses for its own tracker. Each row now shows that tracker's current-period (today's) accumulated total instead of a "description x count" line: whole-number counts for mining/combat/starvation, one-decimal distance for sprint/swim (e.g. "Sprint: 142.3 blocks"). `ActivityLogClientBuffer`, `SyncActivityLogEntryPayload`, and the server-side dispatch/logging path (`ActivityModuleDispatcher`, `ActivityEffectLog`) are unchanged — the client buffer is simply no longer read by this panel. Row order, per-module HUD log colors (`ActivityDrivenNutrientRegistry#getColor`), and the panel's `ScaleConfigPanel`/edit-mode/shrink-floor behavior are unaffected — this is a data-source change only.
- `CalorieHudScreen` and `ActivityLogHudPanel` rows are now icon + label + colored bar + value, matching the existing nutrient-bar row convention (`NutrientBarComponent`'s horizontal layout), inside the same card chrome both panels already had — no shared code between the two files, each restyled independently as usual. `CalorieHudScreen`'s Today/Yesterday/N-days-ago rows now bar that day's total as a percentage of the player's calorie goal (`TrackingData#maxTotal`, the same value the Diet Screen's Calories box reads); the bar reuses the codebase's one established calorie color (`HudDrawHelpers#CALORIE_COLOR`, the flat green used everywhere else a calorie value is drawn — calories have no graduated per-value color rule to reuse, unlike nutrients) and stays capped at 100% width, shifting to the same red used elsewhere in this codebase for "over threshold" once the day's total exceeds the goal, while the percentage text itself is left uncapped (e.g. "134%") so the exact overflow is still visible as a number. `nourished.hud.calorieHistory.today`/`.yesterday`/`.daysAgo` dropped their baked-in value placeholder (now just "Today"/"Yesterday"/"%1$s days ago") since the value is now drawn separately as the bar+percentage; `zh_cn.json` updated to match. `ActivityLogHudPanel`'s five rows bar fill relative to whichever of the five trackers currently has the highest value today (that one gets a full bar, the rest scale proportionally against it) — flagged explicitly since there's no natural shared target for these metrics the way calories have a goal; isolated to one calculation in `drawPanel`, easy to change later. Both panels' rows use a per-metric icon: `CalorieHudScreen` reuses the same `minecraft:fire_charge` icon `CaloriesComponent` already uses for calories; `ActivityLogHudPanel` has no existing per-tracker icon to reuse (nutrients have a datapack-configurable one, these don't), so each row gets a fixed vanilla-item placeholder (iron pickaxe/iron sword/leather boots/water bucket/rotten flesh for mining/combat/sprint/swim/starvation). Neither panel's `ScaleConfigPanel` integration, edit-mode wiring, or shrink-floor (`MIN_SHRINK_SCALE`/`MAX_MARGIN_MULTIPLIER`) changed — purely a row content/styling change, though each panel's natural width/row-height grew slightly (`CalorieHudScreen` 160→190px, both panels' row height 10→12px) to fit the new icon+bar layout.

### Removed

- Removed the "Diet Screen" Command Center category and its "Text Scale & Padding" card (`ClientEventRegistrar`) — Command Center is now exclusively a command launcher, not a settings/UI shortcut hub. The scale panel stays reachable via edit mode (already wired separately, see Fixed below); it was never paired with a "Reset Box Positions" card in the first place, and the dev-only `/marie resetdietmodules` reset (`DietModuleResetCommand`) remains command-only with no Command Center button. `ClientEvents#openOrToggleScaleConfig` itself is untouched and still used by the `OPEN_SCALE_CONFIG` keybind; the now-orphaned `config.nourished.commandCenter.textScalePadding` translation key was removed from `en_us.json`/`zh_cn.json` (`config.nourished.category.diet_screen` was kept — it's still used by the unrelated Diet Screen config-screen category).
- Removed the double-click/scroll text-scale-padding interaction from all seven modules that still had it (`DietScreenEditTarget`'s five sub-boxes, `CalorieHudScreen`, `ActivityLogHudPanel`) — editing now happens exclusively through each module's `ScaleConfigPanel` card, the same card-only pattern `HudEditTarget` already used. `DietScreenPersistence#adjustScale` (its only caller) was removed along with it. In MarieLib, `ContentScaleController` lost its `Mode` enum and `onClick`/`recognizer`/`toggle`/`activeMode`/`handleScroll` methods and their backing `recognizers`/`pendingClickBounds`/`pendingClickTimeMs` fields — it's now a thin accessor over a component's persisted contentScale/paddingScale, with `resolveContentScale`/`resolvePadding` (the pure pass-through statics the `ScaleConfigPanel` render path depends on) untouched. `CalorieHudScreen`/`ActivityLogHudPanel` no longer construct a `ContentScaleController` instance at all — their `persistedContentScale()`/`persistedPaddingScale()` now read `UiStatePersistence` directly, same as `DietScreenPersistence#contentScale`/`#paddingScale` already did. `DoubleClickRecognizer` (MarieLib) is now unreferenced repo-wide but was left in place rather than deleted speculatively.

### Fixed

- `ActivityDistanceTracker#onPlayerTick` recorded sprint/swim distance and fired tracker-milestone checks unconditionally, ignoring the activity-driven nutrient config toggles that `SprintDecayModule`/`SwimDecayModule` already honour. It now returns early when `ActivityDrivenNutrientConfig.get().enabled()` is `false`, and gates the sprint block on `sprintEnabled()` and the swim block on `swimEnabled()` — matching how each decay module reports its own `enabled()`. The last-known-position cache still updates on every tick while the feature as a whole is enabled (so toggling one of sprint/swim off and back on never counts a phantom cross-map jump — those two flags only gate the increment, never the cache write). While the whole feature is disabled the early return now also drops the player's cache entry (`LAST_POSITION.remove`), so re-enabling takes the same `last == null` cache-and-return path a fresh login uses instead of scoring the entire disabled-period displacement as one tick's movement whenever that displacement happens to fall under the 20-block discontinuity cap.
- The Diet Screen's own `OPEN_SCALE_CONFIG` key handler toggled the scale-config sliders via `toggleScaleConfigVisible()` but never entered edit mode, so the panel opened without becoming draggable — unlike `ClientEvents#openOrToggleScaleConfig` (the Command Center path), which already calls `marieEditModeController().enter()` after the toggle. The handler now makes the same `enter()` call; it's a documented no-op when edit mode is already active, so pressing the key repeatedly still just toggles the sliders.
- Recipe inheritance (`RecipeInheritanceStage`) now walks the whole depth-bounded ingredient graph via MariesLib's new `RecipeInheritanceResolver.collectContributions(rootItemId, nodeClassifier)` instead of a flat single-level `getIngredients()` lookup. The flat lookup only ever saw an item's direct ingredients, so a weak keyword-fallback match on an intermediate (e.g. `createfood:raw_onion_calzone`, classified `vegetables` off the "onion" in its name) was the only signal that reached the merge — the strong tag-based `grains`+`dairy` match on `createfood:raw_cheese_calzone` one hop further down was never visited. `collectContributions` visits every node within the resolver's depth limit with no short-circuit; the stage wraps its existing tag+keyword confirmation logic (`classifyRecipeNode` → `collectConfirmedNutrientTags`, unchanged) as the per-node classifier and feeds every returned `NodeContribution` into the same flat sum-then-average merge multiple direct ingredients always used. The now-richer contribution set flows through the supplement's `rawScores()` into `RuntimeResolutionMerge`, whose honest-confidence recompute over the combined signal now sees a genuine `vegetables`-vs-`grains`+`dairy` contest instead of a `vegetables`-only spread. Items already resolved from directly tag-classified ingredients with no ambiguous intermediate hop are unaffected — the same categories win, corroborated rather than displaced. This is the closing fix for the calzone/createfood multi-value gap.

- `ActivityLogHudPanel`'s five row bars previously scaled against whichever of the five trackers currently had the highest value that day — so mining/combat counts (naturally small numbers) and sprint/swim distances (naturally large numbers) were competing on one shared scale, meaning the small-number trackers read as near-permanently-empty next to a large sprint/swim distance. Each row's bar is now self-relative instead: fill is `todayValue / max(historicalMax, todayValue)`, where `historicalMax` is that specific tracker's own highest value across its retained history (`MarieTracking.getTrackerHistory`, the same call `CalorieHudScreen` already uses for its history rows) — never compared against any other tracker. A tracker with no history yet (day one) falls back to a full/near-full bar for today's value alone via the same formula (`historicalMax` is `0`, so `denom` becomes today's own value), avoiding both a divide-by-zero and a permanently-empty first-day bar.
- `EditModeCoordinator.enterAll()`'s shared group-entry path only called `EditModeController.enterGroup(targets, ...)` — it never touched any registrant's own entry-time side effects. `CalorieHudScreen`/`ActivityLogHudPanel`'s scale-config panel was only ever shown via their individual `enterEditModeWithScaleConfig()` helpers (the C/K keybind paths), which the shared `EDIT_ALL_HUDS` group toggle never calls, so entering edit mode on both HUDs together showed at most one panel's scale sliders — whichever HUD's individual keybind had last been pressed, leaving its `scaleConfigVisible` flag `true` from before. MarieLib's `EditModeCoordinator.registerGroupCapable` now takes an optional `Runnable onGroupEnter`, invoked for each registrant when `enterAll()` runs a group entry; `CalorieHudScreen`/`ActivityLogHudPanel` each gained a public `showScaleConfigOnGroupEntry()` (sets `scaleConfigVisible = true` without itself entering edit mode, since the group path already opens the shared overlay) passed as that callback from `ClientEventRegistrar`. Each HUD's individual C/K keybind path is unchanged — both still call `enterEditModeWithScaleConfig()` directly on their own single-target `EditModeController`.
- `ClientEventRegistrar#register`'s `EditModeCoordinator.registerGroupCapable` calls for `CalorieHudScreen`/`ActivityLogHudPanel` (see Changed above) were passing `CalorieHudScreen.instance()`/`ActivityLogHudPanel.instance()` — eager calls that forced both HUD singletons to construct at mod-init/`register()` time, before either panel's `DraggableResizable`/`Constraint` setup has any real reason to exist yet. Now that MarieLib's `registerGroupCapable` takes a `Supplier<MarieComponent>` (only invoked at the moment a group-entry actually happens, never at registration), both call sites pass `CalorieHudScreen::instance`/`ActivityLogHudPanel::instance` method references instead, restoring the same lazy-construction behavior every other call site (`editModeController()`, the individual C/K keybind handlers) already relies on. `ClientEventRegistrar.register()` now contains no `instance()` invocation anywhere — only supplier/keybind/category registration.

- Entering Diet Screen edit mode while the `ScaleConfigPanel` sliders were toggled visible made them architecturally unreachable: edit mode swaps `mc.screen` from `DietScreen` to a separate `EditOverlayScreen` that only renders `DietScreenEditTarget` (the drag/resize boxes), which had no reference to `ScaleConfigPanel` at all — so the sliders became invisible and unclickable the moment edit mode activated, even though `scaleConfigVisible` was still correctly `true` on the now-inactive `DietScreen` instance. `DietScreenEditTarget` now takes `DietScreen`'s existing `ScaleConfigPanel` instance and a live `BooleanSupplier` reading its visibility (not a second panel or a snapshot, so it can't desync from the persisted slider state the player is actually editing) and renders it in the same top-right position, forwarding `mouseClicked`/`mouseScrolled` to it first (same click-priority pattern `DietScreen` already used) before falling through to normal box drag/resize handling. Since both screens read the same mutable state on the same `DietScreen` instance, closing edit mode and reopening the Diet Screen still shows the panel correctly if it's still toggled on.
- `ContentScaleController.resolveContentScale`/`resolvePadding` (MarieLib) previously combined the box-driven proportional/fit scale and the user's persisted adjustment as a multiplier (`clamp(proportionalScale * userAdjustment, proportionalScale*0.5, proportionalScale*3.0)`), which meant resizing a box changed rendered text/padding size even when the user's adjustment was untouched at its default. An interim fix layered the user's adjustment on top of the box-driven scale as a downward-only safety ceiling; this final correction removes the ceiling entirely — text/padding render size is now always exactly the user's persisted adjustment (sanity-clamped against degenerate values only), full stop. If a box is too small to fit the current scale, its content overflows and is cut off by the box's own clip region instead of being shrunk to fit — box-fit sizing, coordinate mapping, and sibling snapping/stacking are otherwise untouched. Updated all seven consumers (`CalorieHudScreen`, `ActivityLogHudPanel`, and the Diet Screen's `BalanceComponent`/`CaloriesComponent`/`RecentMealsComponent`/`EatMoreComponent`/`ActiveEffectsComponent`) to the simplified single-argument call shape.
- `NourishedHUD.onRenderGuiPost` now returns early when `Minecraft.getInstance().options.hideGui` is set, matching `CalorieHudScreen`/`ActivityLogHudPanel`/MarieLib's `NotificationRenderer`. Since both the classic and MarieUI render paths share this one entry point, the HUD kept drawing over a hidden GUI (F1) either way without this check.
- `CalorieHudScreen` and `ActivityLogHudPanel` could not be dragged smaller than their natural content size, since their resize `Constraint` set `minSize` equal to `preferredSize`. Both now allow shrinking down to half their natural size (`MIN_SHRINK_SCALE`), scaling row height, padding, and text size down smoothly with the box instead of clipping abruptly.
- Editing a nutrient/activity/panel hex color in the config screen now updates the HUD live as you type, instead of only after Save+reopen — `ColorHexRowWidget` (MarieLib) now pushes the in-progress value to a transient preview override that `MarieAPI.resolveColor` reads immediately.
- The HUD colors "Reset All" button no longer closes and reopens the whole config screen to show the cleared colors; it now calls `ColorHexRowWidget.syncFromEffectiveColor()` on each visible row directly.
- Wired up `registerCompatEntry` in `NourishedDatapackCallbacks`. The 34 datapack-driven compat entries under `data/nourished/nourished/compat/` were being parsed on every datapack apply but silently discarded, since the callback had no override and defaulted to a no-op — none of them ever actually took effect. They now register into `ModCompat` and apply as intended.
- `CommunityTagStage` was a hand-written duplicate of MariesLib's `CommunityTagResolutionStage`, but diverged in behavior: it always deposited into the shared community-tag signal and returned `null` instead of returning a result, so the community-tag cascade never actually terminated, and it never ran the instance-tags OR-check at all. It now delegates directly to MariesLib's `CommunityTagResolutionStage`, so a community-tag match (including instance-tags) is correctly recognized as a confirmed classification wherever this stage is used — most notably in recipe-ingredient confirmation during recipe inheritance.
- Saving the config screen crashed the client with `IllegalStateException: cannot register while frozen`. `ensureCalorieTrackerRegistered()` reopened the MarieAPI registration phase before calling `registerCalorieTracker()`, but by the time the config screen can be saved `TrackerRegistry` itself is already frozen, and `TrackerRegistry.register()` throws on any registration attempt while frozen regardless of the API phase state. It now also unfreezes/refreezes `TrackerRegistry` around the call, matching the pattern already used in `MarieContext.reloadBroadcastHook()`.
- `NourishedSourceRules.isHeavyBlocked` blocked nutrient values from any consumed food at or above `heavySourcePropertyThreshold`, regardless of hunger state, silently preventing normal eating from applying nutrients. It now only blocks when the player can't normally eat (`player.canEat(false)` is `false`), matching the intended "hunger bar full" condition.
- `NutrientClassificationLookup.resolveNutrientBars()` was blending an authoritative `SourceRegistry.getExternalClassification()` hit with the live `RuntimeResolver` recipe-inheritance guess via `TagRuntimeBlend.blend()`, diluting clean external classifications (e.g. `minecraft:porkchop`'s `{proteins=1.0}`) with low-confidence resolver noise. External classification now short-circuits straight to the result, matching the intent already preserved in the `resolveBars(Item)` overload; the resolved/blend path only runs when there's no authoritative external classification.
- `NourishedDatapackCallbacks` had no override for `registerTrackerMilestone`, so it defaulted to MarieDataLoader.Callbacks's no-op — datapack-defined tracker milestones were parsed on every apply but never actually registered into `TrackerMilestoneRegistry`. It now overrides `registerTrackerMilestone(TrackerMilestoneDefinition)` and delegates to `TrackerMilestoneRegistry.register(def)`, the same one-line shape as the existing `registerMilestone` override.
- `createfood:chocolate_donut_base` was mistagged in `data/nourished/tags/item/nutrients/fruits.json` — a donut base is a grain product, not a fruit. Removed it from `fruits.json` and added it to `grains.json` alongside `createfood:donut_base` (which was already correctly in `grains.json` only).

### CI / Tooling

- Added `check-marielib-update.yml` GitHub Actions workflow to check MarieLib package updates weekly (Mondays 12:00 UTC) or via manual dispatch.
- Workflow queries GitHub Packages Maven metadata and opens a PR against `dev` when a newer MarieLib version is detected.
- Update process requires `MARIELIB_PACKAGES_TOKEN` (PAT with `read:packages`) and does not auto-merge or target `main`.

---

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
