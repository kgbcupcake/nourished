# scanner_spec.json

Drives the item classification scanner (`ItemClassifier` / `ScannerSpecRegistry`):
every signal multiplier, weight map, archetype pattern, and stemmer rule the
scanner uses to guess which tracked value an item contributes to. Nothing about
classification is hardcoded in Java — it all comes from this file.

## Location

`config/<modid>/scanner_spec.json`

## Override stack (lowest to highest)

1. Bundled defaults at `data/<modid>/<modid>/scanner/scanner_spec.json`.
2. `config/<modid>/scanner_spec.json` (this file — modpack creator override).
3. Datapack override at `data/<ns>/<modid>/scanner/scanner_spec.json`, if the
   consuming mod wires up `ScannerSpecRegistry.loadFromDatapack`.

If this file is missing, empty, or fails to parse, the bundled defaults are
used instead.

## Top-level fields

- `multipliers` — global weight multipliers applied per signal type before
  scores are summed. Missing fields fall back to their listed default:
  - `community_tag` (default `5.0`) — weight for community-tag matches.
  - `namespace` (default `4.0`) — weight for namespace matches.
  - `suffix` (default `3.0`) — weight for item-id suffix matches.
  - `keyword` (default `2.0`) — weight for keyword matches.
  - `archetype` (default `2.0`) — weight for archetype pattern matches.
  - `recipe_inheritance` (default `1.0`) — weight applied when a score is
    inherited from a crafting ingredient/recipe relationship.
  - `namespace_peer` (default `0.5`) — weight for scores borrowed from other
    items sharing the same namespace.
  - `secondary_suffix` (default `0.5`) — weight for a second, weaker suffix
    match on the same item.
  - `namespace_peer_average_weight` (default `0.5`) — weight given to the
    *average* score of namespace peers (as opposed to individual peer scores).

- `community_tags` — `{ "<tag>": { "<value_key>": <weight> } }`. Item/block
  tags (e.g. Forge/NeoForge community tags) mapped to the tracked value they
  contribute to and how strongly.

- `namespaces` — `{ "<namespace>": { "<value_key>": <weight> } }`. Mod
  namespaces (e.g. `minecraft`, `farmersdelight`) mapped to value contributions.

- `suffixes` — `{ "<suffix>": { "<value_key>": <weight> } }`. Item-id path
  suffixes (e.g. `_seeds`, `_ore`) mapped to value contributions.

- `keywords` — `{ "<keyword>": { "<value_key>": <weight> } }`. Free-text
  keywords matched against the stemmed item path. Keys are run through the
  stemmer at load time, so entries should be written in their natural form.

- `negative_keywords` — same shape as `keywords`, but the matched weight is
  subtracted instead of added (used to suppress false positives). Also stemmed
  at load time.

- `archetypes` — array of `{ "pattern": "<substring>", "contributions": { "<value_key>": <weight> } }`.
  Each entry is a case-sensitive substring match against the item path; on
  match, every listed value gets its contribution added.

- `excluded_items` — array of item id strings never scored by the classifier,
  regardless of any other signal (mirrors `excluded_items.json` /
  `ExcludedItemsRegistry`, but scoped to this spec file).

- `stemmer_dictionary` — array of known whole words the stemmer should
  recognize as-is (skipped from suffix stripping).

- `stemmer_compound_splits` — `{ "<word>": ["<part1>", "<part2>", ...] }`.
  Compound words the stemmer should split into their parts before matching.

- `stemmer_irregular_forms` — `{ "<form>": "<base>" }`. Irregular word forms
  mapped to their stemmed base (for words the default stemming rules get wrong).

- `stemmer_stop_words` — array of words the stemmer ignores entirely.

- `stemmer_noise_suffixes` — array of low-signal suffixes (e.g. plurals) the
  stemmer strips before matching keywords/negative keywords.
