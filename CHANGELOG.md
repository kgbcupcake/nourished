# Changelog

<!-- markdownlint-disable MD013 -->

## [ Unreleased ]

### Added

- The Diet Screen's Intake Breakdown panel (right column) is now fully dynamic, matching how the left column (Calories/Balance/Recent Meals/Eat more of.../Active Effects) already works: the header, each nutrient row and the bottom legend are independent, individually drag-resizable modules registered against MariesLib's `ModuleRegistry` under a new `"nourished.diet.right"` key, instead of one hand-drawn block in `DietRightColumnComponent`. Each has its own options panel (Layout/Behavior/Appearance, plus a Colors tab for the header and legend) reached the same way as every other Diet Screen box, and its own drag/resize handles in edit mode. Per-nutrient bar fill colors are unchanged — still driven by the existing global nutrient color system. Default (unedited) positions reproduce the previous fixed layout exactly (header at local Y 30, 26px per row, an 8px gap before the legend). Each row's bar still eases toward the real value instead of jumping to it (same ~300ms convergence as before, now backed by MariesLib's new `AnimatedFloat`) and still flashes briefly over the bar when a nutrient is gained (`MarieClientCache#flashAlpha`, same as before) — both carried over unchanged from the classic hand-drawn renderer, not lost in the rewrite. Built on new MariesLib generic widgets (`BarRowComponent`/`LegendComponent`/`TitleBarComponent`), the new `dev.marie.framework.ui.animations.AnimatedFloat`, and a new `DietScreenPersistence#resolveRelativeToRightColumn`.

### Fixed

- On a fresh install (no saved HUD positions yet), the Activity Log and Calorie History HUD boxes spawned overlapping each other: both default to the same left column, only 52px apart (`ActivityLogHudPanel` at y=8, `CalorieHudScreen` at y=60), and Activity Log's natural height — it grows with however many activities are being tracked — regularly exceeds that gap with as few as 3 tracked activities. Since both boxes' editors (drag/resize handles, and the small settings window each box opens) read this same default position, editing either box in that state showed the same overlap. `CalorieHudScreen`'s default Y now stacks below Activity Log's *actual* current bottom edge (with a small gap) whenever the two share the same horizontal column, instead of a fixed 52px assumption; it falls back to the old fixed default once either box has been dragged out of that column, or if Activity Log is disabled.

### Added

- The Active Effects box's "Move Header" outline hugged the effect lines below the header instead of the header itself — its "Active Effects" title is drawn as a separate call with its own offset, not through the same recorded draw path as the lines, so MariesLib's outline machinery never saw where it actually was (new MariesLib `ModuleExtents.Kind.HEADER` / `MarieModuleSettings.recordHeaderExtent`, now called after the header draws). "Move All"'s outline now covers the header too; it already moved the header, just never showed it.

### Added

- The Nutrient HUD's Behavior tab now has "Hide Bars" and "Hide Text" toggles in its Hide group, alongside "Hide Icons" (new MariesLib feature — every module's Hide group gets them). "Hide Bars" hides each row's bar and its percentage number (icon and label keep their place); "Hide Text" hides the nutrient name label (icon and bar keep their place). Wired in both the dynamic renderer (`NutrientBarComponent`) and the classic (pre-MarieUI) one. While at it, fixed the classic renderer's "Hide Icons": it never checked the toggle at all (the dynamic renderer already did), so it kept drawing icons regardless.
- Every module's Hide group now also has a "Hide Window" toggle (new MariesLib feature) that hides the box entirely — background, border, glow, text, icons and bars together — instead of hiding those individually. Wired into the Nutrient HUD (both the dynamic `NutrientPanelContainer` and the classic renderer), Calorie History and Activity Log: each now checks `MarieModuleSettings.isWindowHidden` before drawing anything, since none of the three draw through MariesLib's `withDisplaySettings` (same reason "Hide Icons" needed its own manual check on these boxes, below).

### Changed

- Editing a Diet Screen box with a move mode on (Move Text / Icons / Bars / All) now outlines just the part being dragged — the icons, the text, the bars, or all of them — hugging where the box drew it and following it as it moves, like the HUD boxes, instead of dashing the whole box in every mode. It falls back to the whole box until the box has drawn something of that kind.
- Removed the Calories and Recent Meals boxes' own "Show icons" toggle (and the `DietModuleIcons` store behind it): the generic "Hide Icons" toggle in each box's Behavior tab replaces it, and now hides Recent Meals' per-row icons as well. Any earlier "Show icons" setting is not carried over.
- The Diet Screen window is now built through MariesLib's `standardPanel` like every other module window, so its tabs match: Layout (panel and box sizes), Behavior (drag bars, reset bar order, a collapsible "Visibility" group for the show/hide-box toggles, Reset This Tab), Style (collapsible Background and Brightness groups), Colors and Shared. It uses the new `withoutPadding`/`withoutMoveAndHide`/`withoutSizes` options, so no Move, Hide or Size rows appear that would do nothing for a whole screen. The tab formerly called Visibility is now part of Behavior; every setting, config key and default is unchanged.
- All Nourished module windows now use MariesLib's one generic options layout: the Nutrient HUD window is built through `MarieModuleSettings.standardPanel` like Calorie History and Activity Log, so its Style tab has the same collapsible groups — Sizes, Brightness, Background (opacity, shade) and Border (opacity, shade) — instead of loose border/shade sliders below them (its reveal/hide/threshold options sit under a collapsible "Visibility" group on Behavior), and Calorie History and Activity Log show their border/shade sliders in those same groups. The Diet Screen window's tabs are ordered Layout, Visibility, Style, Colors with Style grouped into Background and Brightness. Values, config keys and defaults are unchanged; the hand-written `HudStyleRows` is gone.

### Fixed

- The Balance box's Behavior tab had the same two problems as Recent Meals below, for its own bar-equivalent content (the five balance pips) and its "Balance" header: the pips were drawn as plain `fillRect` calls, so they never reported an extent and "Move Bars"/"Move All" always fell back to outlining the whole box instead of hugging the pips (each row now reports its own bar extent via the new MariesLib `MarieModuleSettings.recordBarExtent`, matching the pips' offset math, which was already correct); and the "Balance" header text was drawn through a plain brightness-only context with no offset applied at all — unlike the Calories and Eat More boxes, where the header moves with "Move Text" — so it never moved under any move mode, including "Move All". The header now draws through the module's display-settings context like Calories' and Eat More's, so "Move Text"/"Move All" move it too.
- The Recent Meals box's Behavior tab had three problems for its per-row content: "Hide Icons" hid the item icon but left that item's nutrient-glyph icon in the name text; "Move Icons" did nothing, because the item icon was drawn through a brightness-only context instead of the module's display-settings context that offsets icons; and "Move Bars" also did nothing, because the rows (this box's stand-in for bars) never reported an extent for MariesLib's move-outline/offset machinery to act on. The item icon now draws through the same `withDisplaySettings` context as every other box, so Move Icons, Hide Icons and icon brightness all apply to it; Hide Icons now also strips the private-use glyph characters from the row name; and each row now reports its own bar extent (new MariesLib `MarieModuleSettings.recordBarExtent`), so Move Bars drags the rows like it already does elsewhere.
- The "Hide Icons" toggle (Behavior tab) did nothing on the Nutrient HUD, Calorie History and Activity Log boxes: those draw their icons themselves rather than through MariesLib's `withDisplaySettings`, which is where the toggle is enforced. Each now checks `MarieModuleSettings.isIconsHidden` before drawing its icon, so turning it on hides the icons (the rows keep their layout, so nothing shifts).

- Resizing the Nutrient HUD vertically no longer snaps back to the default height when you leave the editor. `HudEditTarget.resolvedLayout` discarded the saved height unconditionally; it now honors a manually-resized height (`heightManual`), the same as Calorie History and the Activity Log. A shrunk height always applies (rows past it scroll); an enlarged one applies while every bar is visible. When only some bars show (e.g. reveal-on-gain after a meal, zero bars hidden), the height is capped at what those bars need, so a full-size box never pops up around a single apple bar.

- The Nutrient HUD drew its content in different places in edit mode and in game after an edit: edit mode re-clamped the text, icon and bar offsets against the box on every frame (rewriting the shared offsets as a side effect of merely opening the editor), while the in-game path never clamped, so labels and bars could land shifted or under each other once you left the editor. Offsets are now clamped only while being dragged, never at draw time, so both paths read identical values and a box collapsed to a sliver no longer squashes them permanently (the panel clip hides whatever overflows).

- Resizing the Nutrient HUD, Calorie History or Activity Log box from its left edge or bottom-left corner no longer drags the icons, text and bars along with the edge: the content now stays where it is on screen and the extra room appears on the left, mirroring how a right-edge resize leaves room on the right. All three boxes now share one rule (MarieLib's `AutoGrowPanelContainer.leftMarginAfterResize`), persisted in `ComponentState.leftMargin` and applied to the content origin; `CalorieHudScreen` and `ActivityLogHudPanel`, which previously always saved a margin of 0, gained it, and `HudEditTarget` (whose margin had been hardcoded to 0) uses it again. The three boxes can also now be dragged down to a 16 px sliver (previously the Nutrient HUD could not shrink below its content at all and the other two stopped at half size): content is clipped away as the box closes up, nothing overflows, and dragging the edge back out reopens it with the content unmoved — the left margin may go negative for this instead of stopping at 0 and dragging the content along.

- Diminishing returns now has a plain "free bites" setting: the existing `diminishingMidpoint` key (kept, so configs and presets carry over) is shown as "Free Bites Before Diminishing" — how many times the same food can be eaten at full value before it starts being reduced. Its description was wrong (it claimed the multiplier is 0.5 at the midpoint; it is 1.0 there), and streak weighting used to make each free bite count more than one, so the default of 2 gave roughly one free bite. Free bites now count as real eats and streak weighting only starts after them (MariesLib `TrackingData.updateMemory`). The config allows 0 (reduce from the first repeat) to 10, and the config-screen slider is a whole-number 0–10 slider. Requires the matching MariesLib build. Everything else — floor, steepness, streak window and weight, novelty bonus and cap, memory window, and the master `enableDiminishingReturns` switch — is unchanged and still configurable.

- The dynamic Diet Screen's Balance box lost its "Balance" header (dropped in the recent color/layout refactor), leaving only the state word and pips. `BalanceComponent` draws the header again, in the same theme text color as the Calories box's header. The header is drawn outside the box's Move Text offset (which moves the state word) and clamped inside the box, so a saved text offset can no longer push it out of view; press Reset Positions on the Balance box if the state word still overlaps it from an old offset. Every Diet box header now has its own "Header text" color in that box's Colors tab (Calories, Balance, Recent Meals, Eat More, Active Effects: new keys `diet.<box>.header`), instead of the shared `text.header` role, which now only colors the right column's "Intake Breakdown" header (relabelled so in the Shared tab). Defaults match what each header used to look like, but a `text.header` color you had customized no longer carries over to the box headers.

- The daily calorie tracker ("Today", plus the activity-log daily totals) now resets when a player sleeps through the night or time is changed with `/time`. It used to roll over on the world's monotonic game-tick counter, which neither sleeping nor `/time` touches, so it only reset after 24000 ticks of actual play. The fix is in MariesLib's `TrackerManager`, which now keys DAILY/WEEKLY/MONTHLY periods on the world day (`getDayTime() / 24000`); Nourished needed no code change. Existing saved periods are re-based onto the day clock once on load, keeping the value in progress. Requires the matching MariesLib build. With the `doDaylightCycle` gamerule off the world day never advances, so these trackers stop rolling over.

- The Nutrient HUD panel's height now always fits the bars currently visible. Previously a manually-resized height (`heightManual` in `nourished-ui-state.json`) was kept even when reveal-on-gain showed only one or two bars, so the whole oversized box popped up on every meal. Width is still honored as saved; `HudEditTarget.resolvedLayout` uses the natural height whenever only a subset of bars is visible (a manual height is honored again while every bar shows — see the resize fix below).

- The "Today" calories display never went down or reset because it rendered `TrackingData.total`, the lifetime accumulator. The classic Diet Screen and dynamic `CaloriesComponent` Calories boxes and the `CalorieHudScreen` "Today" row now read the calorie tracker's daily accumulator (`MarieTracking.getCurrentTrackerValue` on `NourishedAPI.CALORIES_TRACKER_ID`), which resets each game day. The diet delta sync (`onDietDelta`) also writes its `todayCalorieTrackerValue` into `ClientTrackerCache`, so an eat shows immediately instead of waiting on the tracker's throttled live sync. No payload change. Known and not fixed: a multi-day offline gap records a single history entry.

- With `enableCalorieHistory` off, the daily calorie tracker is neither registered nor fed, so the Diet Screen Calories box (classic and dynamic) now hides instead of showing a false 0, and the modules below it close up with no gap (`ClassicDietEditTarget`'s stack height matches). Gated on `FeatureFlagCache.enableCalorieHistory()`, as `CalorieHudScreen` already was.

### Added

- The Nutrient HUD scrolls when its box is too short for every row (horizontal layout): the mouse wheel over the box in edit mode moves the first visible row, the in-game HUD honors that position, and a thin scroll thumb on the right edge marks hidden rows (dynamic renderer only), like the Calorie History box. Implemented in `HudEditTarget#scrolledKeys`, which every draw path now goes through; the vertical layout's columns are not scrolled.

- All three HUD panels — `CalorieHudScreen`, `ActivityLogHudPanel`, and the Nutrient HUD (`HudEditTarget`) — gained a "Move Text and Icons" toggle in their edit-mode `ScaleConfigPanel` editor (new row under Padding, from MarieLib), letting the player drag their icon/label/bar/pct content as a translated whole, separately from dragging the panel itself. Earlier attempts at this used a separately draggable/glowing sub-region layered on top of the panel (first a hand-rolled tracker, then MarieLib's `ContentModule` primitive, then a header-row button on `CalorieHudScreen` alone) — all dropped: these panels' content fills essentially their entire interior, so a second hit-tested region for it always competed with the panel's own drag for every click inside the panel body, and the two also repeatedly drifted out of sync in size/position. The shipped design instead uses a single boolean mode, read fresh each frame from `ScaleConfigPanel.isMoveContentEnabled(componentId)`: while off (the default each session), dragging the panel body moves the panel, exactly as before this feature existed; while on, dragging instead adds to a plain `contentOffsetX`/`contentOffsetY` pair (persisted per panel under its own `#contentOffset`-style key: `nourished.calorieHud.contentOffset`, `nourished.activityLogHud.contentOffset`, `nourished.hud.contentOffset`) that every content-drawing origin in that panel is nudged by, with a dashed border shown around the content as a live drag affordance and the panel's own resize handles hidden while the toggle is on (dragging is exclusively routed to the content offset then, leaving them inert). For the Nutrient HUD specifically, the offset threads through `HudLayout.Layout`'s new `contentOffsetX`/`contentOffsetY` fields into both render backends (`NutrientPanelContainer` and `ClassicHudPanelRenderer`), since that panel's own class doesn't draw its content directly. While at it, fixed a pre-existing staleness bug shared by `ActivityLogHudPanel`/`CalorieHudScreen`: each panel's own drag tracker (`drag`) built its `Constraint` once in the constructor from `naturalSize(currentRows().size())` and never refreshed it, so a later change in row count left the drag's min/preferred/max clamp stale — `render()` now rebuilds it fresh every frame via `drag.setConstraint(...)`, the same per-frame refresh pattern `HudEditTarget` already uses for its own panel drag. Also fixed, for `ActivityLogHudPanel` specifically: its previous sub-region tracker (`rowsDrag`) persisted an offset and rendered a glow outline for it, but the actual icon/label/bar drawing in `drawPanel` never read that offset at all — dragging the old sub-box moved only the (now-removed) glow, never the content underneath it. `contentOffsetX`/`contentOffsetY` are now clamped (new `clampContentOffsetX`/`clampContentOffsetY` per panel) so content's own origin can never be dragged outside the panel's own bounds — re-applied at draw time too, not just while actively dragging, so a stale offset self-corrects if the panel it was valid against changes underneath it (a resize, a row count change). The clamp only constrains the origin corner, not the content's whole footprint, and is based on the panel's own _current_ size rather than content's natural size — so a bigger panel gives correspondingly more room to drag, all the way to its far edge, and dragging is never blocked just because content already fills a natural-sized panel; whatever spills past the panel's edge is handled by clipping, not by refusing the drag. The Nutrient HUD's two render backends (`NutrientPanelContainer`, `ClassicHudPanelRenderer`) also gained their own clip/scissor around content drawing for exactly that clipping, matching `CalorieHudScreen`/`ActivityLogHudPanel`'s existing `pushClip`/`popClip` — previously neither backend clipped its content to the panel at all, so dragged-out content rendered fully detached, floating free over the game world instead of being cut off at the panel's edge.

- `NourishedAPI.registerNutrient` (KubeJS) now accepts optional `icon` (string) and `tags` (array of strings) spec keys, parsed the same way as the existing `color`/`tooltipColor` keys, wired through to MarieLib's `ValueDefinition.Builder#icon`/`#tags`. A `tags` value that isn't a list of strings throws the same style of `IllegalArgumentException` as the method's other validation. `NutrientRegistry.NutrientDef.fromDefinition` now prefers a KubeJS-supplied icon/tags over the previous always-on `resolveIcon`/single-hardcoded-tag fallback, and `toValueDefinition` round-trips both back onto the builder.

- The dynamic UI's right-side module boxes now expose the HUD and Diet screen config options in-game, built only through MarieLib's `MarieToolbox` (`HudOptionsPanel`, `DietOptionsPanel`). The Nutrient HUD box hosts three tabs: Layout (Text Scale, Padding, Move Text and Icons — moved from the built-in rows, same UI-state store and `ScaleConfigPanel.isMoveContentEnabled` contract — plus Vertical HUD layout and HUD Corner), Behavior (Reveal HUD on nutrient gain, Hide above threshold, Show above threshold, greyed out while Hide is 1.0), and Appearance (Background opacity). A new sixth "Diet Screen" box on the Diet Screen holds Appearance (Background opacity) and Visibility (Show recent meals, Show "eat more of...", Show calorie box, Show balance box, Show inventory button). Every control reads/writes the existing `NourishedClientConfig` fields (`hudVerticalLayout`, `hudAnchor`, `hudRevealOnNutrientGain`, `hudHideAboveThreshold`, `hudShowAboveThreshold`, `hudBackgroundOpacity`, `dietBackgroundOpacity`, `showRecentMeals`, `showEatMoreOf`, `showCaloriesBox`, `showBalanceBox`, `showDietScreenButton`) with the same ranges as the Cloth screen, persisting via `NourishedClientConfig.saveNow()` — no new config keys, so existing settings carry over and hand-editing the config file still works. Sliders preview live while dragged and save once on release. The classic UI and its options are unchanged. Requires MariesLib 0.1.2-beta (the toolbox).

- The Nutrient HUD, Calorie History and Activity Log panels now have properly rounded corners (radius 4, `HudDrawHelpers.PANEL_CORNER_RADIUS`) instead of the old one-pixel notch, via MarieLib's new `RenderContext.drawRoundedRect` overload with a radius. The classic HUD and the Diet screen are unchanged. Requires MariesLib 0.1.2-beta.

- Module box cleanup: the Nutrient HUD's "HUD Corner" cycle is removed from its dynamic-UI box (the box is dragged into place, so it is not needed; `hudAnchor` itself is unchanged), and "Move Text and Icons" now lives on a Behavior tab on all three HUD boxes (Nutrient HUD, Calorie History, Activity Log) — still the same `#moveContent` UI-state flag the box polls each frame — instead of the Layout tab (see the Move Bars entry below). The Calorie History and Activity Log boxes now carry the same options as the Nutrient HUD box that apply to them: Layout (Padding), Behavior (Move Text and Icons) and Appearance (Background opacity over the existing `calorieHudBackgroundOpacity`/`activityLogHudBackgroundOpacity`, plus text and icon size/brightness — see the entry below); the Nutrient HUD's vertical layout, reveal-on-gain and threshold options are about nutrient bars and don't apply to them. The classic Nutrient HUD renderer (`HudDrawHelpers.renderIcon`/`drawScaledLabel`) now also honors the text/icon brightness settings, so they take effect with `hudClassicMode` on as well as off. Hosted module boxes can be resized smaller again: they open at a height that fits their tallest tab but are no longer held to it, and a tab that no longer fits scrolls with the mouse wheel (thin scroll thumb on the right edge).

- Text and icon appearance sliders on every dynamic-UI module box (Nutrient HUD, Calorie History, Activity Log; the Diet Screen box gets the brightness pair only). Text and icons now have independent settings, all on the box's Appearance tab (Padding, and the Nutrient HUD's Vertical HUD layout, stay on Layout; the old combined Text Scale is now "Text size"): **Text size** and **Icon size** (10%-500%, 5% steps, the old Text Scale's range), **Text brightness** and **Icon brightness** (20%-200%, default 100% = unchanged; above 100% text colors are scaled up and capped at 255, washing toward white, and icons are tinted with a shader color above 1.0), and Background opacity. Text size is still the panel's persisted `contentScale`; Icon size is stored beside it under `<panel key>#iconScale` and follows the text size until it is first set (the first Text size edit pins it at its current value), so existing profiles look unchanged. In Calorie History/Activity Log the icon is sized by its own multiplier and a row grows to the taller of its text line and its icon, so a big icon makes its row taller instead of overlapping (scroll capacity uses the same height). Brightness is stored in new client config keys, dynamic UI only (no Cloth Config entry; the classic HUD honors the Nutrient HUD pair): `hudTextBrightness`/`hudIconBrightness`, `calorieHudTextBrightness`/`calorieHudIconBrightness`, `activityLogHudTextBrightness`/`activityLogHudIconBrightness`, `dietTextBrightness`/`dietIconBrightness`. Calorie History, Activity Log and the Diet screen draw through MariesLib's `MarieModuleSettings.withBrightness` wrapper that dims `drawText` colors and tints `drawItem` icons independently while passing fills/bars/borders through (and is skipped entirely at 100%/100%). The Calorie History and Activity Log boxes now host a toolbox panel instead of the built-in rows. The Diet Screen's own text/icon sizes are not adjustable from its box: each Diet sub-box keeps its own built-in Text Scale/Padding rows. Short-lived single `*ContentBrightness` keys from earlier builds are removed from the config file on load.

- "Move Text and Icons" is now split in two on the Behavior tab of all three HUD boxes (Nutrient HUD, Calorie History, Activity Log): **Move Text and Icons** drags only the icons and name labels (the offset the toggle always used, `<panel>.contentOffset`), and the new **Move Bars** drags the bars together with their percentage/value text, so a long name that overflows into its bar can be pulled apart from it. The two are mutually exclusive (turning one on turns the other off) and the box's own resize handles stay hidden while either is on. The bar offset is a new UI-state value saved under `<panel key>#barOffset` (its mode flag under `<panel key>.bars#moveContent`), held in memory by MariesLib's `MarieModuleSettings` and written once when the drag ends, and clamped into the box like the text offset. This supersedes the earlier whole-content drag described above (that feature had not shipped): the text offset no longer moves the bars. In the Nutrient HUD the per-row clip is skipped while an offset is non-zero so moved content isn't cut off at its row slot (the panel-wide clip still applies); the classic HUD renderer follows the same split (icons and names by the text offset, bars and percentages by the bar offset) and uses the independent icon size.

- Calorie History and Activity Log now respond to Text size and Icon size the way the Nutrient HUD does: only the drawn text and icons resize, while the row height, icon slot, label column, bar size and percentage column stay at their natural size (before, Text size also stretched the bars and rows, and a large Icon size made rows taller). Enlarged text and icons are clipped at their own row unless a "Move Text and Icons"/"Move Bars" offset is set, which is how to re-place them; scroll capacity now depends only on the box height.

- Bar size, Move Icons, and a number that follows its bar. On all three HUD boxes (Nutrient HUD, Calorie History, Activity Log): a new **Bar size** slider on the Appearance tab scales the bars (length and thickness) and, with them, the number at the end of each bar — that percentage/value text is no longer sized by Text size, it grows and moves with the bar; a new **Move Icons** toggle on the Behavior tab drags the icons on their own (stored under `<panel key>#iconOffset`), and **Move Text** now moves only the name labels (it used to move icons and names together) — Move Text / Move Icons / Move Bars are mutually exclusive, and the saved Bar size lives under `<panel key>#barScale`. The Nutrient HUD's classic renderer follows the same rules. The options window opens tall enough for the six Appearance sliders and, like the other hosted windows, has no maximum size any more — it can be dragged as large as the screen. A fourth Behavior toggle, **Move All**, drags text, icons and bars together (each offset shifts by the same amount from where it started, and all three are saved on release); like the other move modes it is exclusive with them. A **Reset Positions** button under the move toggles on all three HUD boxes puts the icons, bars and text offset back to their default places (saved) and switches every move mode off.

- The Diet Screen's five sub-box editors (Calories, Balance, Recent Meals, Eat more of..., Active Effects) now get the same options as the HUD boxes, minus the bar ones (these boxes have no bars): Layout (Padding), Behavior (Move Text, Move Icons, Move All, Reset Positions) and Appearance (Text size, Icon size, Text brightness, Icon brightness). Text size is the box's existing Text Scale; Icon size follows it until first set, exactly as on the HUD boxes; the move offsets, icon size and brightness are all saved in the box's own UI-state entries (`<box key>#textOffset`, `#iconOffset`, `#iconScale`, `#textBrightness`, `#iconBrightness`), so nothing new goes in the config file. Each box draws through MariesLib's new `MarieModuleSettings.withDisplaySettings` wrapper, which applies those settings to everything it draws without touching its own draw calls; while a move mode is on, dragging inside that box (in edit mode) moves its content instead of the box, with a dashed outline in place of the resize handles. The Diet Screen box's whole-panel background opacity and brightness are unchanged and still apply on top.

- Diet Screen module boxes follow the Activity Log model: resizing a box (Calories, Balance, Recent Meals, Eat more of..., Active Effects) no longer rescales what is inside it — the content keeps a fixed size that follows only the Diet panel's own scale, the box grows or shrinks around it, extra room stays empty and less room is clipped by the box's own clip; text and icon sizes come from their sliders alone (before, each box scaled its positions and bars by the smaller of its width/height ratios, so content moved and stretched as you dragged a corner). Calories and Balance now also carry the bar options in their editors — Bar size and Move Bars (Move All includes it): the Calories progress bar and the Balance pips scale from their start and can be moved on their own. Every editor tab now has a reset button: "Reset This Tab" on Layout and Appearance (each option goes back to its default; icon size goes back to following the text size) alongside Behavior's Reset Positions, on the three HUD boxes, the five Diet module editors and the Diet Screen box; the Nutrient HUD box's Behavior tab also resets its config options to their defaults through the same mechanism. The temporary `/marie resetdietmodules` command stays for now: Reset Positions puts a box's content offsets back, not the box's own saved position and size.

- Dragging a Diet module box out from its left (or top) edge now grows the box on that side while its text, icons and bars stay where they were on screen, instead of the content travelling with the edge and the extra room appearing only on the right. The box's content offsets are adjusted by the edge's movement while you drag and saved on release.

- Active Effects editor: a new Move Header toggle moves the "Active Effects" title on its own, Move Text now moves only the effect lines (before, it moved the title and the lines together), and Move Icons is gone from this box because it draws no icons of its own.

- Diet screen tidy-up: the Balance box drops its "Balance" label and the green highlight behind the state text, leaving the pips and the coloured "Balanced"/"Low"/"Excess" text; the Recent Meals box header now reads "Recent" and the suggestion box header reads "Eat More". In the Recent editor, Move Text now moves only the header — the meal rows (icon + name) are that box's "bars", so the new Bar size and Move Bars options (Move All included) scale and move them. Diet module boxes now also show a bottom-left corner handle (resize from the left, growing leftward), not just the bottom-right one. The width caps are lifted: the Diet screen can be widened to the right up to the screen edge, and module boxes have no width cap of their own (they stop only at the panel edge and the column divider, so widening the panel leftward lets them grow with it). Widening the Diet screen from its left edge is no longer capped at 1.5× the base width: it can keep growing leftward until the screen edge, like the right side.

- HUD label cleanup: the Calorie History rows read "1 day", "2 days", "3 days"... instead of "Yesterday", "2 days ago"..., and the Nutrient HUD shows "Veggies" instead of "Vegetables" (which was being cut off). The short name comes from a new HUD-only `nourished.hud.label.<key>` lang entry that `HudDrawHelpers.nutrientLabel` checks first, so the Diet screen and config screens keep the full name and any nutrient without a short entry keeps its normal label.

- In-game color editing on the dynamic-UI module boxes, through MariesLib's new toolbox color tab (`colorTab`/`color`, `StandardPanelBuilder.extraTabs`): a Colors tab on the Nutrient HUD and Diet Screen boxes with one slot per registered nutrient (built at runtime from `NutrientRegistry`, bound to `nourished:nutrient.<key>`, the color the HUD bars and Diet-screen rows already read), and on the Calorie History (Background, Text) and Activity Log (Background, Text, plus Mining, Combat, Sprint, Swim and Starvation row colors, `nourished:activity.<id>`) boxes. Clicking a slot opens one shared picker window. No new config keys and no second color store: `client.colors.NourishedColorSlots` previews live through `ColorPreviewOverrides` and, on release, writes `ColorRegistry` (removing the entry when it equals the default) and saves it — the same flow the Cloth hex row uses, so Cloth and the picker agree. Colors are stored opaque; panel backgrounds keep their separate opacity sliders and the border/shade options are unchanged. New `nourished.options.color.*` lang keys. Requires MariesLib with the color picker.

- Every fixed color the dynamic HUDs and Diet screen draw is now a registered, editable color instead of a constant in a renderer (`client.colors.NourishedColors`, registered at init and on reload beside the existing nutrient/panel/text/activity colors, so they resolve through `MarieColors.resolveColor` and can be overridden in `colors.json`). Defaults equal the old constants, so nothing looks different until a color is edited. New keys: Nutrient HUD `panel.nutrient_hud`, `hud.label`, `hud.bar_background`, `hud.bar_low`, `hud.bar_critical`, `hud.pct_good`, `hud.pct_low`, `hud.pct_critical`, `hud.flash`, `hud.calorie`, `hud.notification_text`; `calorie_hud.over_goal`, `calorie_hud.accent`, `activity_log_hud.accent`; Diet screen `diet.panel_background`, `right_panel_background`, `row_background`, `border`, `border_light`, `divider`, `title`, `muted_text`, `header_text`, `text`, `today_text`, `good`, `warn`, `bad`, `segment_empty`, `flash`, `legend_text`, `legend_low`, `toggle_on`, `toggle_off`, `toggle_border`, `toggle_housing`, `toggle_lever`; edit-mode `edit.accent`, `edit.label_text`, `edit.label_shadow`. Colors are stored opaque: the translucent bar track (`hud.bar_background`, alpha 0x99) and the dimmed legend percentages (alpha 0x99) keep their alpha as a fixed value in the renderer, and the panel/row backgrounds keep their existing opacity sliders. The threshold colors (`hud.bar_low`/`hud.bar_critical`) still override a nutrient's color outside its normal range, as before. The Calorie History and Activity Log Colors tabs gained slots for the ones they draw (calorie bar, over-goal bar, bar track, accent), and the Activity Log's row-color slots now come from `ActivityDrivenNutrientRegistry` (which gained an ordered `colorModuleIds()` and a public `colorKey()`) instead of a list in the UI code. `ColorsValidator` accepts any registered `nourished:` color key. The classic Diet screen and classic HUD renderer files are untouched (they read the shared `HudDrawHelpers` colors, so they now follow these overrides too). A picker closed or moved to another slot before release now drops its uncommitted preview (slot cancel callback).

- The 13 Command Center card accents are now registered colors, one per card (`command_center.tools.<card>`, `command_center.framework.<card>`, defaults unchanged), passed to MariesLib's `CommandCenterCard` as a supplier that resolves through `MarieColors.resolveColor` each time the card is drawn — so an override in `colors.json` applies without a restart. Removed dead code from `HudDrawHelpers` that nothing called (the old `drawResizeHandle`, `drawEditBanner`, `editOverlayColor`, `hoverBorderColor`, `dashedPreviewColor`, `handleActiveColor` and their `COL_EDIT_*`/`COL_HOVER_BORDER`/`COL_HANDLE_*`/`COL_DASHED_PREVIEW` constants — the live edit chrome draws through MariesLib's `RenderContext`) and the unused `DietRightColumnComponent.colBorder()`. Known behavior: `hud.notification_text` is read when a food-eaten notification is built, so a changed color applies from the next notification, not to one already showing.

- Colors are now named by function and derived from what they belong to, superseding the fixed-hue color keys added in the entry above. A nutrient's own registered color (`nourished:nutrient.<key>`) is the source of everything its row draws: the bar fill and percent text are that color in the normal band, and `MarieColors.shade` of it when low (`-0.30`) or critical (`-0.55`); the empty track is a `-0.95` shade (at the HUD's existing fixed track alpha); the gain flash is a `+0.60` shade toward white (alpha unchanged); the Diet arrows are the nutrient color (up) and its low shade (down); the Diet legend swatches apply the same low/critical steps to the shared `text` color. The steps are named once (`NourishedColors.LOW_SHADE`, `CRITICAL_SHADE`, `TRACK_SHADE`, `FLASH_SHADE`, `SURFACE_SHADE`) and were chosen so a nutrient's normal, low and critical fills and its track stay distinguishable for every shipped nutrient. Function-named shared colors default from the dark `ThemeKey` RGB (alpha not taken, so every renderer keeps its own opacity handling) and stay editable per module: `text`, `text.muted`, `text.header`, `text.nutrient_hud`, `border`, `divider`, `bar.track`, `balance.balanced` / `balance.low` / `balance.excess`, `effect.beneficial` / `effect.harmful`, `toggle.on` / `toggle.off` / `toggle.border` / `toggle.housing` / `toggle.lever`, `edit.outline`, `edit.label.text`, `edit.label.shadow`. Roles with no counterpart keep their own default: `panel.nutrient_hud`, `panel.diet`, `diet.title`, `diet.today_text`, `calorie.value`, `calorie_hud.over_goal` and the panel accents. Diet inner surfaces (rows, inner panels) are the Diet panel color shaded `+0.05` instead of separate keys. Visible changes: sub-box borders and dividers use the theme border, muted/header text and the toggle lever are the theme's secondary text color, text and notification text are the theme's primary text color, edit outlines use the theme's dashed-preview color, the toggle lights and housing use theme bar/panel colors, the Diet legend swatches are shades of the text color, and the low/critical/track/flash looks follow the nutrient. The classic HUD reads the same shared helpers, so its bar fills and percent text follow the nutrient too. The Nutrient HUD Colors tab gained Background and Label text; Calorie History and Activity Log kept their Bar track slot (now `bar.track`) and Calorie History's calorie slot is `calorie.value`. A one-time `colors.json` cleanup (`RetiredColorKeys`) moves an override from a renamed key to its successor (`hud.calorie` -> `calorie.value`, `hud.label` -> `text.nutrient_hud`, `diet.panel_background` -> `panel.diet`, and the other one-for-one role renames) and drops overrides on keys that are now derived, logging once.

- Threshold tinting is gone, superseding the shade-step scheme in the entry above: a nutrient is drawn in its own registered color (`nourished:nutrient.<key>`) everywhere — HUD and Diet bar fills, percent text, the Diet arrows and the gain flash (its color at the flash's own fading alpha) — in every state, and a player changes it through that nutrient's slot. `HudDrawHelpers.barFillColor`/`pctColor` now return the nutrient color whatever the value (kept for callers that still pass it, including the classic HUD), and the Diet column's threshold color function and the low/critical/track/flash shade constants are removed. The empty bar track is the theme's bar-background color (`bar.track`) for every bar, with each renderer's existing alpha; the Diet legend swatches are drawn in the shared `text` color. `panel.nutrient_hud` and `panel.diet` now default from the theme's panel-background RGB, and the Diet inner-surface shade is `+0.05` of `panel.diet`. Nutrient thresholds still drive everything that is not color (balance state text, the balance buffs and penalties, HUD reveal/hide).

- No color value is written anywhere in Nourished any more: `NourishedColors` now holds key names and shade-step constants only, and every registered role color defaults from a `ThemeKey` of the dark theme (RGB only) or a named shade of one, superseding the literal defaults in the entries above. Defaults: `calorie.value`, `calorie_hud.accent`, `activity_log_hud.accent`, `balance.balanced`, `effect.beneficial`, `toggle.on`, `diet.title`, `diet.today_text` and the nine Command Center tools cards use the theme's primary text color; `calorie_hud.over_goal`, `balance.low`, `balance.excess`, `effect.harmful` and the four Command Center framework cards use its secondary text color; `toggle.off` uses its border color. `panel.diet` is the theme panel background shaded `+0.02` (`141414`, the previous value) and the new `diet.bar_track` (the empty track of the Diet screen's nutrient bars, Calories bar and Balance pips) is the theme border shaded `-0.27` (`2A2A2A`, the previous value), so those two look as before; the Diet inner surface, `+0.05` of `panel.diet`, is `1F1F1F` (was `1E1E1E`). Overrides players already set on these keys keep working; no key was renamed. The Diet screen's intake legend (its three swatches and the hardcoded "Good"/"Low"/"Critical" range text) is removed along with the threshold colors it explained; the `diet.legend_good`/`diet.legend_low` lang keys are dropped, while `legend`, `legend_bad` and `legend_bad_range` stay because the classic Diet screen still draws its own legend. The Balance box and the server-side thresholds are unchanged.

- The Diet screen's colors are now editable in game. The Diet Screen box keeps Style and Visibility and gains a Colors tab (one slot per registered nutrient, built at runtime like the Nutrient HUD's, plus Panel = `panel.diet`, Title = `diet.title`, Today text = `diet.today_text`) and a Shared tab holding the roles several Diet boxes draw: Text (`text`), Header text (`text.header`), Muted text (`text.muted`), Border (`border`), Divider (`divider`), Bar track (`diet.bar_track`), Toggle on (`toggle.on`) and Toggle off (`toggle.off`). Each sub-box that draws its own role color gets a Colors tab: Calories (Calorie value, `calorie.value`, also on Calorie History since the same key), Balance (Balanced, Low, Excess: `balance.*`) and Active Effects (Beneficial, Harmful: `effect.*`); Recent Meals and Eat More draw only shared roles and have none. Every shared key is on exactly one Diet tab. `toggle.housing`, `toggle.border` and `toggle.lever` are left to `colors.json`. Slots bind like the other Colors tabs (live `ColorPreviewOverrides`, `ColorRegistry` write-or-remove plus save on release, preview dropped on cancel). New `nourished.options.color.*` labels and `nourished.options.tab.shared`.

- The dynamic-UI option boxes now cover more of the client config, over the same `NourishedClientConfig` fields, ranges and `saveNow()` commits as the Cloth screen (no new config keys; the classic UI is unchanged). Nutrient HUD > Behavior: **Show empty bars** (`hudShowZeroBars`). Diet Screen box: a new Layout tab with **Diet panel size** (`dietScale`), **Recent Meals box size** (`recentMealsBoxScale`), **Eat More box size** (`eatMoreBoxScale`) (each 50%-150%), **Drag bars to reorder** (`dietBarDragEnabled`) and a **Reset bar order** button (`resetDietBarOrder()`), plus **Show active effects** (`showActiveEffects`) on Visibility. Calorie History and Activity Log > Style: **Border opacity**, **Background shade** and **Border shade** (`calorieHud*`/`activityLogHud*`, 0-100% and -100%-100%). Not exposed (`hudScale`, `hudBarWidth` and `hudReservedBottom` were added to the Nutrient HUD Layout tab and then removed again: the Nutrient HUD box is sized and placed by dragging): `hudScale`, `hudBarWidth`, `hudReservedBottom`, `hudOffsetX/Y` and `dietOffsetX/Y` (a saved panel position overrides them, so they do nothing after the first drag), `hudDraggable` (nothing reads it), `enableCalorieHistoryHud`/`enableActivityLogHud` (turning a HUD off from its own box would also disable the key that reopens it). Requires the MariesLib build with `intSlider`/`button`/`styleRows`.

- The Recent Meals and Eat More boxes on the Diet Screen now have a Colors tab (header text, meal text and border; header text and border), and the Calories box's Colors tab gained label text, bar track and border next to the calorie value. Text, header text, bar track and border are shared roles: editing one in any box changes it everywhere it is drawn (see the Shared tab). No new color keys.

- Removed the "Bar track" color slot from the Calorie History, Activity Log and Diet Screen boxes (Calories box, Shared tab): it is one shared color (`bar.track` / `diet.bar_track`), so editing it in one box recolored every bar track. The color keys themselves are unchanged and still editable in `colors.json`; the unused `nourished.options.color.bar_track` lang key is kept.

### Changed

- The Nutrient HUD box's Appearance tab gained **Border opacity**, **Background shade** and **Border shade** sliders, like the Calorie History and Activity Log boxes (new dynamic-UI-only client config keys `hudBorderOpacity`, `hudBackgroundShade`, `hudBorderShade`; defaults 1.0 / 0.0 / 0.0), built by `HudStyleRows.nutrientHud` and covered by "Reset This Tab". At the defaults the panel looks exactly as before (border in the panel's own color); the sliders tint the fill and border from there. The classic renderer is unchanged.

- `ActivityLogHudPanel`/`CalorieHudScreen` now scroll instead of clipping mid-row when the box is dragged smaller than its natural size, matching the Nutrient HUD's (`HudEditTarget`) pinned-content behavior: icon/row/bar/text sizing stays driven by the user's own persisted `contentScale` alone, never by box size — a resize only changes the box itself, not the content inside it. (A shrink-together approach using the box's own live size was tried and reverted — it diverged from how the Nutrient HUD panel actually behaves, which is the reference for all three panels.) A new `mouseScrolled` handler (only reachable while edit mode has the panel's input wired up) moves a row-based `scrollOffset`, `drawPanel` only draws the rows currently in view (`visibleRowCapacity`, computed purely from `contentScale`), and a small track+thumb indicator on the box's right edge shows there's more when `capacity < rowCount`.

- Removed the unused `nourished.options.hud.corner*` lang keys left behind when the HUD Corner cycle left the dynamic UI (`hudAnchor` itself is unchanged and still set from the Cloth screen).

### Fixed

- The Nutrient HUD drew only its empty panel (no icons, labels, bars or percentages) with "Classic HUD renderer" off. `NourishedHUD` already filters the nutrient keys through `HudVisibility.visibleKeys`, but each `NutrientBarComponent` then applied a second, differently-behaved visibility rule (`ThresholdVisibility`/`AnyOf`), so `Container.render` skipped rows the first filter had kept. The per-row rule is removed; the already-filtered key list is the single source of truth, as it is for the classic renderer. The "show empty bars" dimming (empty rows at 40% alpha), which only the classic renderer had, now also applies to the MarieUI renderer.

- Resizing the Nutrient HUD box left its icons, labels and bars behind instead of moving them with the box. Content was centred in the box (vertically, and per row horizontally) and shifted by a persisted left margin on left-edge drags. It is now pinned to the box's top-left plus padding, exactly like the Calorie History and Activity Log boxes, so it moves with the box on resize. The old persisted `leftMargin` is ignored.

- Whole-screen flicker while eating, introduced with the HUD content clipping: MarieLib's `GuiGraphicsRenderContext#popClip` re-called `enableScissor(parent)` when closing a nested clip instead of `disableScissor()`, leaving a stale entry on `GuiGraphics`' own scissor stack for every nested pop (the Nutrient HUD's per-bar clip nests inside the panel clip), so the GL scissor stayed clamped and later clears only covered a small rect. `popClip` now always pops once and `resetClip` pops once per outstanding clip.
- HUD freeze/stutter while eating in dynamic mode: `HudEditTarget`'s "Move Text and Icons" content-offset lookup (`persistedContentOffsetX`/`persistedContentOffsetY`) re-read the shared, synchronized `UiStatePersistence` on every render frame. The offset is now loaded once lazily into a small in-memory cache and only re-synced when a content-move drag is committed (`persistContentOffset`).
- `ActivityLogHudPanel`/`CalorieHudScreen` rows became an unreadable overlapping mess at a low Text Scale — `lineHeight` shrinks with `contentScale` and has no floor of its own, but `iconSize` was floored at a fixed 8px regardless of scale, so once `lineHeight` dropped below 8 the icon overflowed into the rows above/below. `iconSize` is now also capped at the current `lineHeight` (still floored at 1px, still scaling proportionally with `contentScale` above that point), so it can never exceed the row it's drawn in.
- `ActivityLogHudPanel`'s Sprint/Swim rows dropped their `" blocks"` suffix — a shrunk box could clip the suffix mid-word (e.g. `"0.0 blo"`), and `VALUE_RESERVE` is sized for the digits-only case, so the suffix was always liable to run past it. `currentRows` now formats distance trackers as plain `"%.1f"`.
- `ActivityLogHudPanel`/`CalorieHudScreen`'s bars could shrink to zero width at high Text Scale: `barW` was pinned to the panel's unscaled natural width (a fixed right edge), while `iconSize`/label width/value-reserve all grow with `contentScale` — past a certain Text Scale those grown elements ate the entire fixed budget and the bar visually disappeared. `barW` is now itself part of the scaling content: a `naturalBarW` reference (how much width is left for the bar at scale `1.0`, using `context.textWidth(row.label(), 1f)` for the label reservation) is computed once, unaffected by the live box size, then multiplied by `contentScale` like everything else in the row.
- `visibleRowCapacity`'s `rowCount` parameter went dead in both panels once the shrink-together approach above it was reverted — it stopped being read anywhere in the method body. Dropped from the signature; both call sites updated.
- `ActivityLogHudPanel`/`CalorieHudScreen` didn't mimic the Nutrient HUD's (`HudEditTarget`) resize behavior: both computed their bar width from the live, drag-resizable `bounds.width()` and clamped how many rows to draw from `bounds.height() / lineHeight`, so dragging either panel's corner handle stretched its bars to fill the new width and could silently drop rows off the bottom — where the Nutrient HUD panel keeps content pinned to a fixed natural size and only grows/shrinks the surrounding blank margin on resize (per `HudEditTarget`'s own class doc), relying on `RenderContext#pushClip` to visually cut off anything that doesn't fit rather than deciding how much content to draw. Both panels' `drawPanel` now compute `barW` against `naturalSize(rows.size()).width()` instead of the live `bounds.width()`, and draw every row unconditionally (letting the existing `pushClip`/`popClip` region clip an undersized box) instead of computing a `maxRows` cutoff from `bounds.height()`.
- The config screen's left-sidebar category nav (`NourishedConfigLeftCardsLayout`) only adapted to narrow screen widths and never accounted for screen height, so at high GUI Scale (where Minecraft's scaled screen dimensions shrink) the sidebar's fixed-height category list — 14 categories at a fixed row height/gap — could require more vertical space than the scaled screen actually had, running items off the bottom with no way to reach them. `computeMetrics` now also takes screen height and tab count: nav row height/gap continuously scale down (floored so text never clips) based on available vertical space instead of a hard width-only breakpoint, and nav width now interpolates smoothly between its full and minimum sizes instead of snapping at a single 500px cutoff. `SidebarNavWidget` also gained its own independent scroll (mouse wheel, scissor-clipped rendering, a scrollbar indicator, viewport-bounded hit-testing) for the case where even the scaled-down list still can't fit, matching the scrolling the content pane already had via `cloth.listWidget`.
- Investigated a brief screen flash/corruption glitch reported when eating food. Every `RenderContext#pushClip`/`popClip` pair across the HUD render code (`CalorieHudScreen`, `ActivityLogHudPanel`, `NutrientPanelContainer`, `NutrientBarComponent`, and the Diet Screen's five sub-box components) already wraps its render body in `try`/`finally` with `popClip()` in `finally`, and each panel's row/tracker data is rebuilt into a fresh, unshared `List` on every frame (`currentRows()`), so no exception mid-render can desync from a concurrently-mutated backing collection either. As defense-in-depth against any future/unanticipated exception during a HUD render pass still leaving the GL scissor rect clamped, MarieLib's `GuiGraphicsRenderContext` gained a new `resetClip()` method (clears the whole scissor stack and disables scissoring unconditionally, regardless of how many `pushClip` calls are outstanding), and every per-frame `GuiGraphicsRenderContext` construction site (`NourishedHUD#drawHudPanelViaMarieUI`, `CalorieHudScreen#onRenderGuiPost`, `ActivityLogHudPanel#onRenderGuiPost`, and both `DietScreen#drawPanelViaMarieUI`/scale-config-overlay render calls) now calls it in a `finally` block around that frame's render call.
- Found the actual cause of the eating-triggered full-screen black flash the scissor-clip investigation above didn't explain: the classic (pre-MarieUI) Nutrient HUD renderer's `HudDrawHelpers.renderIcon`/`drawScaledLabel` called `PoseStack.pushPose()`, then `translate`/`scale`/the draw call, then a bare `popPose()` — no `try`/`finally`. The `GuiGraphics` `PoseStack` these methods push onto is the same shared instance every `RenderGuiEvent.Post` subscriber draws through that frame, and `GuiGraphics.fill()` applies whatever pose transform is currently active to its quad. If `g.renderItem(...)`/`g.drawString(...)` threw partway through — plausible for a nutrient icon resolved mid-sync while values update rapidly, i.e. exactly while eating — the matching `popPose()` never ran, leaving that row's `translate`+`scale` baked into the shared `PoseStack` for every remaining fill/text/item draw call for the rest of the frame; a small bar/icon fill elsewhere could then render stretched into a full-screen black quad, self-healing on the next successful draw and re-corrupting on the next throw — the reported rapid strobing. Both methods now wrap their transform + draw call in `try { ... } finally { pose.popPose(); }`. MarieLib's `GuiGraphicsRenderContext.drawText`/`drawItem` (the MarieUI-path equivalent, used when the Nutrient HUD isn't in classic mode) had the identical unguarded pattern, fixed the same way.
- Closed the actual hazard the `popPose()` fix above only contained rather than removed: both HUD render backends re-resolved a nutrient's icon string into an `ItemStack` from scratch on every single frame, for every visible bar — `NutrientBarComponent#resolveIconStack` and `HudDrawHelpers#renderIcon` each ran `ResourceLocation.tryParse(NutrientRegistry.getIcon(key))` followed by a `BuiltInRegistries.ITEM.getOptional(...)` lookup inline in the render path, at up to 60fps per bar, instead of resolving/validating the icon once. `NutrientRegistry` gained `getIconItem(String)`, which resolves and caches the backing `Item` in a `ConcurrentHashMap` keyed by icon id string (falling back to `Items.APPLE` for a null/malformed/unregistered id, same as before) — parsed and looked up only once per distinct icon id for the process lifetime, not per frame per bar. Both render call sites now build their `ItemStack` from that cached `Item` instead of re-parsing/re-querying the registry themselves.

- Opening edit mode ("edit all") crashed with `IllegalStateException: an index default only applies to a cycle` while building the Nutrient HUD box: its new Base bar width and Bottom margin sliders set their reset value with the `int` overload of `defaultValue`, which is the cycle-only one. They now use the `double` overload. The other two HUD boxes crashed too only because edit-all builds all three at once.

[ nourished 0.2.7-beta.2]

> Do Not Add Anything Here, This Is A Placeholder For The Next Release Notes, everything in this section is automatically generated from the commit history and will be replaced on release.Keep All New Work In The Section Up Above This One In #Unreleased And Only Move It Down When The Release Is Ready To Ship.

[ nourished 0.2.7-beta.1-Hotfix]

### Fixed

- `HudEditTarget`'s drag/resize tracker (`panelDrag`) set its `Constraint` (the min/preferred/max size `DraggableResizable` clamps against) once, in the constructor, and never refreshed it. Since `HudEditTarget` is a session-lifetime singleton (built once by `NourishedHUD`, unlike `DietScreenEditTarget` which rebuilds fresh each time the Diet Screen reopens), that `Constraint` stayed frozen at whatever `hudScale`/`hudBarWidth`/`hudVerticalLayout`/visible-nutrient-count produced the first time HUD edit mode was entered — any later change to those in the HUD & Display config category (Cloth Config) left the drag tracker clamping against a stale size while every other layout computation (`resolvedLayout`, `matchedLayoutFor`) correctly recomputed live, so dragging/resizing the HUD panel after touching that config category desynced from what was actually rendered (overlapping bars, unresponsive/misbehaving handles). New private `HudEditTarget#constraintFor(HudLayout.Layout)` rebuilds the `Constraint` fresh from a given natural layout, called once in the constructor and again every frame in `render()` via `panelDrag.setConstraint(...)` — the same per-frame refresh pattern `DietScreenEditTarget` already used for all six of its own drag trackers, and the exact usage `DraggableResizable#setConstraint`'s own javadoc calls for.

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
