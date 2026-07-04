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
