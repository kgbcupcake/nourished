## Cumulative sugar tracking (new tracking primitive)

### Summary
Add a cumulative-intake tracking mechanism for sugar, parallel to how calories are tracked today, rather than a decaying 0-1 bar like the existing 5 nutrient groups. Sugar should accumulate over time and trigger effects/warnings once a threshold is crossed, not fill/drain like fruits/vegetables/proteins/grains/dairy.

### Motivation
The dessert/candy classification pass (bundled tags update) surfaced ~759 items across the codebase that are sugar-heavy but currently only tracked through their primary food group (a cookie is just "grains", a chocolate bar is just "dairy" or unclassified). A bar-style nutrient (decay, critical/low/excess thresholds) doesn't model sugar well, sugar isn't something you want to keep topped up, it's something you want to cap. Calories already prove out the "raw accumulating number" pattern; sugar needs the same shape.

### Current state (for reference)
- Calories accumulate via `TrackingData.addTotal()`:  a raw running number, no decay, no thresholds, purely informational (`enableCalorieTracking` gates whether it's called and displayed).
- The 5 existing nutrients (`fruits`, `vegetables`, `proteins`, `grains`, `dairy`) all go through `NutrientRegistry` / `ValueDefinition`: bar-style, 0-1 range, decay rate, critical/low/excess thresholds, threshold-triggered effects.
- Nothing in the current pipeline handles "cumulative total with a threshold-triggered effect." Calories accumulate but don't threshold. Nutrients threshold but don't accumulate unbounded.

### Proposed design

**MarieLib (generic, domain-agnostic):**
- New cumulative-tracking primitive:  e.g. `CumulativeTrackingValue` or similar, that:
  - accumulates a raw float total per player (like calories)
  - supports a rolling window or reset interval (daily-ish, configurable) so it doesn't grow forever
  - supports one or more threshold definitions that fire a callback/event when crossed (not tied to nutrient bonus/penalty effect system specifically,  stays generic)
- MarieLib has zero knowledge this is being used for "sugar",  it just knows it's tracking a named cumulative value with thresholds. Consistent with the existing WHO/WHAT split.

**Nourished (domain-specific):**
- Register a `sugar` cumulative value through the new MarieLib primitive at bootstrap, same place `valueTagScoresProvider` gets injected via `MarieLibContext`.
- New bundled tag file, `sugar.json`, additive (items keep their existing category tags, this is a second tag alongside them, same pattern as the multi-category composite dishes already in the bundled tags).
- Per-item sugar weight resolution, likely reuses the existing tag-scan burst/weight logic, scaled for a cumulative total instead of a bar fill.
- Threshold effect(s): e.g. crossing a daily sugar threshold applies a penalty effect, mirroring how `bonusEffectThreshold` / `penaltyEffectThreshold` work for the 5 bars today, but sourced from the cumulative total instead of bar position.
- Config surface: enable/disable toggle (module lock pattern, same as other modules), threshold value(s), reset window, whether it's shown in the HUD/diet screen.

### Scope
- **Out of scope for this issue:** the actual sugar.json item classification list (759 candidates from the dessert-tree scan): that's a separate data task, not blocked by this.
- **Out of scope:** UI placement/HUD design: dynamic UI already handles new bars, but a cumulative value display is a different shape than a bar and needs its own decision.

### Release order
Per normal pipeline: MarieLib primitive first (published as Maven/GitHub Packages artifact), then Nourished's dependency bump + sugar wiring, then ship.

### Open questions
- Reset window: daily (real time) vs in-game day vs rolling N-hour window?
- Does crossing threshold apply a status effect, block something, or just surface a toast/HUD warning?
- Should sugar have its own `enableSugarTracking` toggle independent of `enableCalorieTracking`, or piggyback on it?