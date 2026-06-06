# Nourished Architecture Hardening Audit

Perform a full architecture audit of the current codebase.

This is NOT a feature implementation task.

This is an architecture-hardening and maintenance task.

## Goal

Identify areas where Nourished's new server-authoritative architecture can be strengthened, simplified, or protected from future regressions.

Focus on:

* Synchronization correctness
* Ownership boundaries
* Source-of-truth enforcement
* Client/server separation
* Future maintainability
* Architecture consistency

---

## Phase 1 — Source Of Truth Audit

Trace all reads and writes involving:

* NourishedConfig
* SyncNourishedConfigSnapshot
* ClientNourishedState
* DietData
* ClientDietCache
* SyncDietPayload
* SyncDietDeltaPayload
* NourishedSyncHandler

Identify any code path where:

* Client gameplay logic can bypass synced state
* Client reads config directly when synced data should be used
* Multiple sources of truth exist for the same value
* State can become stale

Produce a report.

---

## Phase 2 — Server Authority Audit

Verify that all gameplay-affecting decisions originate from:

* Server-side DietData
* Server-side SyncNourishedConfigSnapshot

Flag any location where:

* Client state influences gameplay logic
* Client values can become authoritative
* Config values are used directly instead of snapshot values

Produce a report.

---

## Phase 3 — Client/Server Boundary Audit

Identify:

* Any client class referenced from common code
* Any dedicated-server crash risks
* Any classloading risks
* Any side checks relying on fragile heuristics

Evaluate ClientNourishedState specifically.

Recommend safer patterns where appropriate.

---

## Phase 4 — Sync Consistency Audit

Review:

* Join synchronization
* Reload synchronization
* Delta synchronization
* Snapshot replacement

Verify:

* Latest snapshot always wins
* No stale state survives reloads
* No double-sync paths exist
* No race conditions exist

Produce findings.

---

## Phase 5 — Architecture Guardrails

Recommend:

* Assertions
* Validation checks
* Runtime diagnostics
* Debug logging
* Invariant enforcement

that would make future architectural regressions immediately visible.

Examples:

* Snapshot missing after server startup
* Client accessing gameplay config while UNINITIALIZED
* Unexpected direct config access
* Duplicate sync sources

---

## Phase 6 — Technical Debt Report

List:

* Architectural debt
* Cleanup opportunities
* Legacy systems
* Redundant abstractions
* Potential simplifications

Rank:

* Critical
* Important
* Optional

---

## Constraints

Do NOT add gameplay features.

Do NOT redesign the nutrition system.

Do NOT redesign classification.

Focus exclusively on architectural correctness, maintainability, synchronization, validation, and future-proofing.

Deliver findings as a structured architecture review document.
