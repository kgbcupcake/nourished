---
created: 2026-07-04T14:56:41.391Z
title: Align hudScale cap with 0.3-3.0 preview clamp
area: hud
files:
  - src/main/java/dev/maire/nourished/config/NourishedClientConfig.java:76
  - src/main/java/dev/maire/nourished/client/hud/NourishedHUD.java:116
  - src/main/java/dev/maire/nourished/client/hud/NourishedHUD.java:223
  - src/main/java/dev/maire/nourished/client/hud/HudEditTarget.java
---

## Problem

`NourishedClientConfig.hudScale` is defined via `builder.defineInRange("hudScale", ..., 0.5d, 1.5d)`
(NourishedClientConfig.java:76), but both the legacy HUD resize path (`NourishedHUD.java:116` and
:223, `Mth.clamp(..., 0.3d, 3.0d)`) and the newer MarieUI `HudEditTarget` Constraint allow the live
drag preview to visually scale the HUD panel across 0.3-3.0. Since `setHudScale()` has no clamp of
its own — it relies on `ModConfigSpec.DoubleValue.set()` silently clamping to the defined 0.5-1.5
range — a user can drag-resize the panel to e.g. 2.5x, release, and watch it snap back to 1.5x with
no visual feedback that the commit was truncated. This is a pre-existing legacy quirk (confirmed
during a HudEditTarget MarieUI-port review), not something newly introduced by HudEditTarget, but
it affects both paths since they share the same config-backed persistence.

## Solution

Widen `NourishedClientConfig`'s `hudScale` `defineInRange` cap from 0.5-1.5 to match the 0.3-3.0
preview-clamp constants used by the resize logic, so committing a resize never silently
snaps back below what the live drag preview showed. Do this for both the legacy
HUDEditScreen/NourishedHUD path and the new HudEditTarget path together, since they share
NourishedClientConfig's hudScale field as their single source of truth — decide as one change,
not two.

## Resolution (2026-07-04)

Widened `NourishedClientConfig.java:74`'s `hudScale` `defineInRange` from `(0.5d, 1.5d)` to
`(0.3d, 3.0d)`, matching the existing preview-clamp constants exactly (chose to widen the config
rather than narrow the preview). Also updated `HudAndDisplayCategory.java`'s Cloth Config slider,
which hardcoded its own separate `0.5d, 1.5d` bounds — otherwise the in-game settings slider would
have visually capped below what drag-resize can now commit.

Before changing anything, checked `HudLayout.compute` at both extremes: all dimensions that could
go to zero/negative already have `Math.max` floors (iconSize, verticalBarW/H, scaledPad,
columnGap) and `barW` is fully clamped `(20, 200)`, so nothing breaks numerically. At `scale=3.0`
panel width grows from ~211px (old max 1.5) to ~414px (horizontal layout, typical label) — real
but bounded risk of right-edge clipping on unusually small GUI-scaled windows only, not a crash or
overlap; `panelX`/`panelY` clamping already prevents the panel from going negative or vanishing.
Vertical-layout mode with many nutrients is already wide at today's 1.5 cap (~426px with 8 keys) —
not a new problem introduced by this change. Since `HudLayout.compute(mc, keys, scale)` at 0.3-3.0
is the exact same call the live drag preview has used all along, no new rendering path was
introduced — proceeded with the full range as originally planned rather than narrowing it.
