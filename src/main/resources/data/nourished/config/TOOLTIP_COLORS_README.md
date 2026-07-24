# tooltip_colors.json

Per-value tooltip color overrides. Lets a modpack creator recolor tooltip
text for tracked values without touching Java code.

## Location

`config/<modid>/tooltips/tooltip_colors.json`

If the file is missing, `TooltipColorRegistry` writes an empty
`{"byKey": {}, "byItem": {}}` object so the file exists and can be edited
in place. There is no bundled default palette tier for this file — colors
fall back to whatever default the calling mod uses whenever neither
section has a matching entry.

## Format

A JSON object with two sections, `byKey` and `byItem`, each a flat
`String -> String` map:

```json
{
  "byKey": {
    "value_a": "#55FF55",
    "excluded": "#808080"
  },
  "byItem": {
    "minecraft:apple": "#FFAA00"
  }
}
```

- **`byKey`** — keyed by an arbitrary value key, looked up via
  `TooltipColorRegistry.get(modId, key)` or as the fallback tier of
  `getForItem`. `excluded` is a reserved key: it colors the excluded-item
  tooltip line, and falls back to dark gray (`ChatFormatting.DARK_GRAY`)
  if left unset.
- **`byItem`** — keyed by a full item id (e.g. `"minecraft:apple"`).
  Looked up via `TooltipColorRegistry.getForItem(modId, itemId, key)` and
  takes priority over `byKey` when both could apply to the same tooltip
  line — see Override stack below.
- **Value** — the color to use. Accepts:
  - `#RRGGBB` / `0xRRGGBB` (6 digits) — recommended. Alpha is implicitly
    full opacity.
  - `#AARRGGBB` / `0xAARRGGBB` (8 digits) — alpha is the **first** byte,
    e.g. `#80FF0000` is 50%-alpha red. **Alpha has no visual effect on
    tooltip text color** (Minecraft's text color type masks it off before
    rendering), so this form exists only for input-format compatibility.
    Prefer 6-digit `#RRGGBB` for this file.
  - A blank or missing value is treated as "no override".

## Override stack

For `getForItem(modId, itemId, key)`, resolution order is (highest
priority first):

1. `data/<modid>/marie/tooltips/tooltip_colors.json` (datapack) — `byItem[itemId]`
2. `config/<modid>/tooltips/tooltip_colors.json` (this file) — `byItem[itemId]`
3. `data/<modid>/marie/tooltips/tooltip_colors.json` (datapack) — `byKey[key]`
4. `config/<modid>/tooltips/tooltip_colors.json` (this file) — `byKey[key]`
5. No override (caller falls back to its own default)

In other words: an item-specific entry always beats a key entry,
regardless of which tier it comes from; within the same specificity
(item or key), the datapack tier beats the config tier. The datapack
tier only applies if the consuming mod wires up
`TooltipColorRegistry.loadFromDatapack`.

For the plain `get(modId, key)` lookup (no item id), only the `byKey`
steps (3–5 above) apply.
