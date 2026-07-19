# tooltip_messages.json

Per-value tooltip message overrides. Lets a modpack creator rewrite
tooltip text for tracked values without touching Java code.

## Location

`config/<modid>/tooltips/tooltip_messages.json`

If the file is missing, `TooltipMessageRegistry` writes an empty
`{"byKey": {}, "byItem": {}}` object so the file exists and can be edited
in place. There is no bundled default message tier for this file —
messages fall back to whatever default the calling mod uses whenever
neither section has a matching entry.

## Format

A JSON object with two sections, `byKey` and `byItem`, each a flat
`String -> String` map:

```json
{
  "byKey": {
    "value_a": "A custom tooltip message.",
    "excluded": "This item is excluded from tracking."
  },
  "byItem": {
    "minecraft:apple": "Apples are always fresh!"
  }
}
```

- **`byKey`** — keyed by an arbitrary message key, looked up via
  `TooltipMessageRegistry.get(modId, key)` or as the fallback tier of
  `getForItem`. `excluded` is a reserved key used for the excluded-item
  tooltip line; if left unset, the caller falls back to the
  `<modid>.tooltip.excluded` translation key.
- **`byItem`** — keyed by a full item id (e.g. `"minecraft:apple"`).
  Looked up via `TooltipMessageRegistry.getForItem(modId, itemId, key)`
  and takes priority over `byKey` when both could apply to the same
  tooltip line — see Override stack below.
- **Value** — the tooltip message text to display. A blank or missing
  value is treated as "no override".

## Override stack

For `getForItem(modId, itemId, key)`, resolution order is (highest
priority first):

1. `data/<modid>/marie/tooltips/tooltip_messages.json` (datapack) — `byItem[itemId]`
2. `config/<modid>/tooltips/tooltip_messages.json` (this file) — `byItem[itemId]`
3. `data/<modid>/marie/tooltips/tooltip_messages.json` (datapack) — `byKey[key]`
4. `config/<modid>/tooltips/tooltip_messages.json` (this file) — `byKey[key]`
5. No override (caller falls back to its own default)

In other words: an item-specific entry always beats a key entry,
regardless of which tier it comes from; within the same specificity
(item or key), the datapack tier beats the config tier. The datapack
tier only applies if the consuming mod wires up
`TooltipMessageRegistry.loadFromDatapack`.

For the plain `get(modId, key)` lookup (no item id), only the `byKey`
steps (3–5 above) apply.
