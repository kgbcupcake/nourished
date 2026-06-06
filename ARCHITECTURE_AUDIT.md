# Nourished Architecture Audit

**Date:** 2026-06-06  
**Scope:** Server-authoritative config sync, client/server boundaries, sync correctness, maintainability  
**Codebase state:** Post-S222 config sync implementation

---

## Phase 1 — Source of Truth Audit

### Findings

#### F1-A · `DietData` calls `NourishedConfig.get()` directly in 15+ gameplay methods

`DietData` is shared code used on both server (`DietAttachment`) and client (`ClientDietCache`). It calls `NourishedConfig.get()` directly in every multiplier and memory calculation:

- `peekMultiplier()` — [`DietData.java:419,423,441,491,538,562`](src/main/java/dev/maire/nourished/core/diet/DietData.java)
- `getMultiplierBreakdown()` — [`DietData.java:646,650,651,664,676`](src/main/java/dev/maire/nourished/core/diet/DietData.java)
- `computeBalanceScore()` — [`DietData.java:334`](src/main/java/dev/maire/nourished/core/diet/DietData.java)
- `getStartingValue()` — [`DietData.java:232`](src/main/java/dev/maire/nourished/core/diet/DietData.java)

When `ClientEvents.onItemTooltip` calls `diet.peekMultiplier(...)` to compute tooltip previews, it is using the raw local config file, not the server-authoritative snapshot. A player connected to a server with different config values will see tooltip multipliers that diverge from server reality.

**Severity:** Critical — split source of truth for tooltip preview vs. actual server outcome.

#### F1-B · `ClientNourishedState` is write-only — nothing reads it

`ClientNourishedState.getConfig()` and `ClientNourishedState.isReady()` exist but are never called outside the class itself. The client snapshot store is populated (when a snapshot packet arrives) but never consumed by any gameplay, HUD, or tooltip code.

Cross-ref: [`ModNetworking.java:96`](src/main/java/dev/maire/nourished/core/network/ModNetworking.java) — only write site.

**Severity:** Critical — the entire sync infrastructure built in S221/S222 is inert on the consumption side.

#### F1-C · Multiple sources of truth for threshold values

The following values exist in **three** places simultaneously:
- `NourishedConfig` (raw TOML)  
- `SyncNourishedConfigSnapshot` (server-authoritative wire snapshot)
- `ClientNourishedState` (client-side mirror — but never read)

`NutritionDecayHandler` correctly uses the snapshot. `FoodNutrientPipeline.checkThresholdCrossings()` at [`FoodNutrientPipeline.java:338`](src/main/java/dev/maire/nourished/core/handler/FoodNutrientPipeline.java) reads `NourishedConfig.get()` directly for `criticalThreshold` and `excessThreshold`. These thresholds determine when `NutrientCriticalEvent` and `NutrientExcessEvent` are fired — gameplay-affecting decisions driven by unsynced config.

**Severity:** Important.

---

## Phase 2 — Server Authority Audit

### Findings

#### F2-A · `NourishedSyncHandler.syncOnJoin()` is never called

`syncOnJoin` sends the config snapshot to a joining player. It is defined at [`NourishedSyncHandler.java:25`](src/main/java/dev/maire/nourished/core/network/sync/NourishedSyncHandler.java) but has **zero call sites**.

`DietPlayerEvents.onPlayerJoin` at [`DietPlayerEvents.java:20`](src/main/java/dev/maire/nourished/core/handler/DietPlayerEvents.java) calls `ModNetworking.syncDiet(player, diet)` but does not call `syncOnJoin`. As a result:

- Players joining after server startup never receive the config snapshot.
- `ClientNourishedState` stays `UNINITIALIZED` for all players except those online during a `/nourished reload` broadcast.
- Any code guarded by `ClientNourishedState.isReady()` never executes — even if it were wired up.

This is the root cause of the client snapshot system being non-functional.

**Severity:** Critical — functional regression. The sync system was built but the join hook was not wired.

**Fix:** In `DietPlayerEvents.onPlayerJoin`, replace or augment the `ModNetworking.syncDiet` call with `NourishedSyncHandler.syncOnJoin(player)`, which already handles both config snapshot and diet in one method.

#### F2-B · `NourishedCommand.resetPlayer()` uses raw config

[`NourishedCommand.java:399`](src/main/java/dev/maire/nourished/core/command/NourishedCommand.java):
```java
float start = (float) NourishedConfig.get().startingNutrientValue();
```

Should use `NourishedSyncHandler.getConfigSnapshot()` with fallback to match the pattern established by `NutritionDecayHandler`.

**Severity:** Important — minor inconsistency; low gameplay impact for a reset command.

#### F2-C · `NourishedCommandSource.sendNutrientDetail()` uses raw config for display

[`NourishedCommand.java:569`](src/main/java/dev/maire/nourished/core/command/NourishedCommand.java) and [`NourishedCommandSource.java:40`](src/main/java/dev/maire/nourished/core/command/NourishedCommandSource.java) use `NourishedConfig.get()` for threshold display in `/nourished nutrient` and `/nourished report`. These are display commands, not gameplay decisions, but they will show different values than what the server is actually enforcing when config has been reloaded.

**Severity:** Optional — display only, but confusing for server admins.

---

## Phase 3 — Client/Server Boundary Audit

### Findings

#### F3-A · `ModNetworking` (common code) imports `ClientNourishedState` (client code)

[`ModNetworking.java:4`](src/main/java/dev/maire/nourished/core/network/ModNetworking.java):
```java
import dev.maire.nourished.client.ClientNourishedState;
```

`ModNetworking` lives in `core.network` — common code loaded on both sides. It directly references a class in the `client` package. On a dedicated server:

- The `client` package classes are not classloaded, so the import itself is not immediately fatal because the reference is inside a packet handler lambda that only executes on the client thread.
- However, if the JVM resolves class references eagerly or if a future refactor moves logic out of the lambda, this becomes a dedicated-server crash.
- NeoForge's `IPayloadContext.enqueueWork` typically ensures execution on the correct thread, but the class reference at the import level is fragile.

**Recommendation:** Extract the config snapshot handler to a separate `ClientModNetworking` class (client-only), or use `DistExecutor.safeRunWhenOn(Dist.CLIENT, ...)` to invoke the client update.

**Severity:** Important — classloading risk, not a current crash but a maintenance hazard.

#### F3-B · `ClientNourishedState.isRunningOnDedicatedServer()` uses try/catch heuristic

[`ClientNourishedState.java:49`](src/main/java/dev/maire/nourished/client/ClientNourishedState.java):
```java
private static boolean isRunningOnDedicatedServer() {
    try {
        net.minecraft.client.Minecraft.getInstance();
        return false;
    } catch (Throwable t) {
        return true;
    }
}
```

This works, but it depends on `Minecraft.getInstance()` throwing when not on a client. The correct pattern is:
```java
return FMLEnvironment.dist == Dist.DEDICATED_SERVER;
```

Using `FMLEnvironment.dist` is reliable, zero-cost, and does not suppress unexpected `Throwable` from unrelated causes (e.g., a NPE inside `getInstance()`).

Additionally, `warnedServerAccess` is a static boolean that is never reset — it will suppress the warning for all future accesses after the first, even across logical server restarts in a testing environment.

**Severity:** Important — correctness risk on edge cases; the fix is a one-liner.

#### F3-C · Respawn sends diet delta instead of full diet

[`DietPlayerEvents.onPlayerChangeDimension`](src/main/java/dev/maire/nourished/core/handler/DietPlayerEvents.java#L63) uses `syncDietDelta` instead of `syncDiet`. This is intentional per the comments in `ModNetworking`, but the config snapshot is also not re-sent on dimension change. If a client's `ClientNourishedState` were ever reset on dimension change, it would stay uninitialized.

**Severity:** Optional — low risk given F2-A means snapshots are never populated anyway.

---

## Phase 4 — Sync Consistency Audit

### Findings

#### F4-A · Join synchronization is broken (see F2-A)

Config snapshot is never sent on join. Diet data is sent via `DietPlayerEvents`, but config snapshot is not. The intended join flow — `NourishedSyncHandler.syncOnJoin(player)` — is unhooked.

#### F4-B · Reload synchronization is correct

`ConfigReloadHandler.reloadAndBroadcast` → `NourishedSyncHandler.setConfigSnapshot` + `NourishedSyncHandler.broadcastConfigReload` → sends to all connected players. `NourishedCommand.reloadAll` calls `ConfigReloadHandler.reloadAndBroadcast`. This path is correctly wired.

#### F4-C · No snapshot replacement on reconnect

`ClientNourishedState.reset()` exists but is never called. There is no `PlayerLoggedOutEvent` or `ClientPlayerNetworkEvent.LoggingOut` handler that calls it. If a player disconnects and reconnects, the old snapshot persists in `ClientNourishedState` until overwritten by the next reload broadcast — which, combined with F2-A, never happens.

**Severity:** Important — stale snapshots on reconnect once F2-A is fixed.

#### F4-D · Protocol version mismatch silently drops snapshot

[`ModNetworking.java:91`](src/main/java/dev/maire/nourished/core/network/ModNetworking.java):
```java
if (payload.protocolVersion() != SyncNourishedConfigSnapshot.PROTOCOL_VERSION) {
    Nourished.LOGGER.warn(...);
    return;
}
```

The snapshot is silently discarded. `ClientNourishedState` stays `UNINITIALIZED`. There is no fallback, no retry, and no client notification. If a client/server protocol mismatch occurs, gameplay silently degrades.

**Severity:** Optional — edge case, but worth documenting with a stronger warning or connection refusal.

#### F4-E · Reload broadcast does not re-sync diet data

`NourishedSyncHandler.broadcastConfigReload` sends only the config snapshot. It does not re-send diet data. If a reload changes nutrient definitions (adding/removing nutrient keys), connected clients will have stale diet nutrient maps that no longer match the active key set until the next delta or disconnect/reconnect.

**Severity:** Important — potential desync after nutrient registry changes at runtime.

---

## Phase 5 — Architecture Guardrails

### Recommended assertions and diagnostics

#### G1 · Assert snapshot is non-null before gameplay decisions

In `NutritionDecayHandler`, `NourishedSyncHandler.getConfigSnapshot()` returns null and the handler silently skips. This is defensive but silent. Add a warn-once log:

```java
SyncNourishedConfigSnapshot snapshot = NourishedSyncHandler.getConfigSnapshot();
if (snapshot == null) {
    Nourished.LOGGER.warn("[Nourished] NutritionDecayHandler: config snapshot not yet initialized — decay skipped. " +
        "This is expected briefly after server start; if it persists, check ConfigReloadHandler.onServerStarting.");
    return;
}
```

#### G2 · Assert `syncOnJoin` is called for every player join

Add a debug-mode assertion in `syncOnJoin`:

```java
public static void syncOnJoin(ServerPlayer player) {
    Nourished.LOGGER.debug("[Nourished] Sending config snapshot to joining player: {}", player.getName().getString());
    SyncNourishedConfigSnapshot snapshot = configSnapshot;
    if (snapshot == null) {
        Nourished.LOGGER.warn("[Nourished] syncOnJoin called but configSnapshot is null — player {} will not receive config", 
            player.getName().getString());
    }
    ...
}
```

#### G3 · Detect direct `NourishedConfig.get()` calls in `DietData` via a Checkstyle/ArchUnit rule

The recurring pattern of `NourishedConfig.get()` inside shared `DietData` is the core architectural leakage. A ArchUnit rule (or a comment header) declaring: "DietData must not call NourishedConfig.get() — config values must be passed as parameters" would prevent regression.

#### G4 · `ClientNourishedState` access guard

Add a `PENDING` state for the window between join and snapshot receipt. Change any `isReady()` caller to warn visually if it falls back to defaults:

```java
public enum SyncState { UNINITIALIZED, PENDING, ACTIVE }
```

Transition `UNINITIALIZED → PENDING` when the join event fires on the client side, and `PENDING → ACTIVE` when the snapshot arrives. If a player opens the HUD while `PENDING`, show a "Connecting..." overlay rather than silently showing zero/default values.

#### G5 · Invariant: config snapshot set before players can join

In `ConfigReloadHandler.onServerStarting`, assert that `NourishedSyncHandler.getConfigSnapshot()` is non-null before the server is marked ready:

```java
SyncNourishedConfigSnapshot snap = SyncNourishedConfigSnapshot.fromConfig(NourishedConfig.get());
NourishedSyncHandler.setConfigSnapshot(snap);
Nourished.LOGGER.info("[Nourished] Config snapshot initialized with protocol version {}", snap.protocolVersion());
```

(This already exists — worth adding the log line.)

---

## Phase 6 — Technical Debt Report

### Critical

| # | Issue | Location |
|---|-------|----------|
| TD-1 | `NourishedSyncHandler.syncOnJoin()` exists but is never called — join sync for config is completely broken | `DietPlayerEvents.java:20` |
| TD-2 | `DietData` calls `NourishedConfig.get()` in 15+ methods — shared code bypasses sync architecture | `DietData.java` |
| TD-3 | `ClientNourishedState.getConfig()` / `isReady()` are never consumed — client snapshot store is dead code | `ClientNourishedState.java` |

### Important

| # | Issue | Location |
|---|-------|----------|
| TD-4 | `ModNetworking` (common) imports `ClientNourishedState` (client) — classloading risk on dedicated server | `ModNetworking.java:4` |
| TD-5 | `ClientNourishedState.isRunningOnDedicatedServer()` uses try/catch instead of `FMLEnvironment.dist` | `ClientNourishedState.java:49` |
| TD-6 | `ClientNourishedState.reset()` is never called — snapshot persists across disconnects | no call site |
| TD-7 | Reload broadcast does not re-sync diet data — nutrient registry changes leave clients with stale maps | `NourishedSyncHandler.java:35` |
| TD-8 | `FoodNutrientPipeline.checkThresholdCrossings()` uses raw config for event threshold decisions | `FoodNutrientPipeline.java:338` |
| TD-9 | `NourishedCommand.resetPlayer()` uses raw config for starting value | `NourishedCommand.java:399` |
| TD-10 | `warnedServerAccess` static boolean is never reset between server sessions | `ClientNourishedState.java:17` |

### Optional

| # | Issue | Location |
|---|-------|----------|
| TD-11 | `SyncState` lacks `PENDING` and `DISCONNECTED` states — transition model too coarse | `SyncState.java` |
| TD-12 | Protocol mismatch silently discards snapshot with no client notification | `ModNetworking.java:91` |
| TD-13 | `/nourished report` and `/nourished nutrient` display raw config thresholds instead of snapshot values | `NourishedCommandSource.java:40` |
| TD-14 | `onPlayerChangeDimension` sends diet delta, not full diet — inconsistent with join/respawn | `DietPlayerEvents.java:63` |
| TD-15 | `DietData.getStartingValue()` at [`DietData.java:232`](src/main/java/dev/maire/nourished/core/diet/DietData.java) silently defaults to 0.5 if config returns a value outside `[0,1]` — this clamping is invisible | `DietData.java:232` |

---

## Summary

The config sync infrastructure (S221/S222) is structurally sound — the snapshot payload, codec, and broadcast mechanism are all correct. The critical failure is a missing wire-up: `syncOnJoin` was implemented but never called, so the system never actually activates for any player in a live game. The secondary failure is that `DietData` — shared code — still pulls config values directly rather than using the synced snapshot, meaning even if the join wire were fixed, client-side calculations (tooltip multipliers, balance scores) would still use local config rather than the server-authoritative values.

The two highest-priority fixes are:
1. Wire `NourishedSyncHandler.syncOnJoin(player)` into `DietPlayerEvents.onPlayerJoin`
2. Refactor `DietData` gameplay methods to accept config values as parameters rather than calling `NourishedConfig.get()` internally — or introduce a `DietConfig` value object passed at call sites so the server passes snapshot values and the client passes `ClientNourishedState.getConfig()` values.
