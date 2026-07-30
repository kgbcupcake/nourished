## Nourished Documentation Discovery Prompt (Read-Only)

**Repo:** `~/RiderProjects/Nourished`
**Goal:** Produce an accurate inventory of Nourished's real public API surface, module list, and config/datapack paths, to be used afterward for rewriting `README.md` and `API.md`. Discovery only — no file edits, no doc rewrites, no code changes.

### Context

`ARCHITECTURE.md`, `PHILOSOPHY.md`, and `RoadMap.md` are being deleted outright (same decision already made for MariesLib) — only `README.md` and `API.md` will be kept and rewritten afterward. `CONTRIBUTING.md` is being kept as-is and is out of scope for this pass. Do not touch any `.md` file in this pass.

The old docs assume:
- A `NourishedAPI` facade at `dev.maire.nourished.api.NourishedAPI` (note: possibly a typo'd package, "maire" not "marie" — confirm the real package)
- Bootstrap via `MariesLibBootstrap.attach(...)` — already confirmed WRONG on the MariesLib side; the real call there is `MarieBootstrap.attach(modId, modEventBus)` in package `dev.marie.framework.core`. Confirm what Nourished's actual `Nourished.java` constructor calls today.
- A dependency pin of `MarieLib v0.1.0-beta.1` — confirmed stale; Nourished now hard-depends on MariesLib 0.1.1-beta.4+.

Do not assume any of the old docs' class names, package paths, or version pins are correct. Verify everything against real source.

### What to do

1. **Real bootstrap.** Read `Nourished.java`'s `@Mod` constructor. Confirm the exact `MarieBootstrap`/`MarieContext` calls made, matching against what MariesLib's own discovery pass already confirmed real (`MarieBootstrap.attach("nourished", modEventBus)` is the expected pattern — confirm or correct).

2. **Real `NourishedAPI`.** Find the actual facade class Nourished exposes to addon mods (if one exists at all — confirm it's real, not aspirational). Record: full package path, every public method signature, `@ApiStatus` tier per method/class. Cross-check against the old docs' claimed methods (`getValueLevel`, `registerValue`, `registerSourceClassification`).

3. **Real module list.** Confirm which gameplay modules actually exist in the current codebase (Raw Food / Gut Health, Stamina, others?) and how each is toggled (config keys). Note any module referenced in old docs that no longer exists, and any real module not mentioned in old docs.

4. **Real config/datapack paths.** List every real file Nourished reads for food classification, overrides, effects, colors, presets — cross-reference against `CHANGELOG.md`'s `[Unreleased]` and `[nourished 0.2.7-beta]` sections (override folder now under `config/nourished/overrides/`, further split into `overrides/Overrides/` + `overrides/Read_Me/` for `food_overrides.json`/`source_classifications.json`/`excluded_items.json` per tonight's release). Confirm the datapack tag paths under `data/nourished/tags/item/nutrients/` (per `CONTRIBUTING.md`) are still accurate.

5. **Real dynamic UI config.** Confirm the actual config keys for `hudClassicMode` / `dietScreenClassicMode` (added tonight per CHANGELOG) and the Diet Screen icon-hide toggle, since none of this exists in the old docs at all — this is genuinely new content to document, not a correction.

6. **KubeJS.** Confirm whether `NourishedKubeEvents`/`NourishedEvents` (old docs call it both names inconsistently) is real, its actual package, and its real event list. Old docs claim `nutrientChanged`, `foodEaten`, `gutHealthChanged`, `rawFoodPenalty` — verify each against source.

7. **Compat list accuracy.** For the README's mod-compatibility table (Farmer's Delight, Pam's HarvestCraft 2, Create: Food, Croptopia, etc., all marked "✅ Full"), spot-check whether these are still driven by real `CompatDefinition`/`ModCompat` entries in source, or whether the list has drifted from what's actually registered. Flag any mod in the table with no matching compat registration found, and any real compat registration not reflected in the table.

8. **Dead/misleading references.** Same treatment as the MariesLib pass: for `dev.maire.nourished.api.NourishedAPI`, `MariesLibBootstrap`, `NourishedContextBuilder`, `NourishedTrackingData`, `NourishedLifecycle`, `ReloadPipeline`, confirm each is either accurate as named, renamed, or doesn't exist — don't assume gone just because the package prefix pattern was wrong elsewhere.

### Output format

Same structure as the MariesLib report:
1. Real bootstrap summary
2. Real `NourishedAPI` inventory (full method signatures, tiers)
3. Real module list
4. Real config/datapack path list
5. Dynamic UI config keys (new content)
6. KubeJS findings
7. Compat list accuracy findings
8. Dead-reference list

### Constraints

- Read-only. Do not edit, create, or delete any file.
- Do not rewrite or draft replacement content for API.md or README.md in this pass.
- Do not touch ARCHITECTURE.md, PHILOSOPHY.md, RoadMap.md, or CONTRIBUTING.md.
- Verify every claim against actual source — do not infer from CHANGELOG.md prose alone where the real class/config is available to read directly.
- If something is ambiguous or unconfirmable, list it explicitly as unresolved rather than guessing.
