# colors.json

Per-value HUD/UI colors. Lets a modpack creator recolor value bars, icons,
and other tracked-value UI without touching Java code.

## Location

`config/<modid>/colors.json`

If the file is missing, `ColorRegistry` writes an empty `[]` array so the
file exists and can be edited in place. There is no bundled default palette
tier for this file — colors fall back to the built-in defaults baked into
the UI whenever a key has no entry here.

## Format

A JSON array of objects:

```json
[
  { "key": "value_a", "argb": "0xFF55FF55" },
  { "key": "value_b", "argb": "0xFF4DD9D9" }
]
```

- `key` — the tracked value's id (matches a registered `ValueDefinition`).
- `argb` — the color to use. Accepts:
  - A `0x`-prefixed hex string, 6 or 8 digits (`0xFF55FF55`, alpha optional).
  - A `#`-prefixed hex string, 6 or 8 digits (`#55FF55`, `#FF55FF55`).
  - A plain JSON number (treated as RGB).

Alpha is always forced to fully opaque (`0xFF`) on load, regardless of what
is supplied — this file only controls hue/brightness, not transparency.

## Override stack (lowest to highest)

1. Built-in UI defaults (used for any `key` absent from this file).
2. `config/<modid>/colors.json` (this file — modpack creator override).
3. Datapack override at `data/<modid>/config/colors.json`, if the
   consuming mod wires up `ColorRegistry.loadFromDatapack`.

Use `ColorRegistry.setArgb(key, argb)` / `ColorRegistry.save()` in-game to
persist changes back to this file instead of hand-editing it, if available.
