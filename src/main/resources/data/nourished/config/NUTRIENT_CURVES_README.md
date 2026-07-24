# Nourished: Nutrient Curves

`nutrient_curves.json` assigns a response-curve shape to each nutrient,
controlling how food intensity and scanner confidence scale a food's
contribution to that nutrient's bar. Nutrients with no explicit entry fall
back to the configured global default preset (`FLAT` if that is also unset).

## Schema

A preset entry, and a custom grid entry:

```json
[
  { "nutrient": "protein", "preset": "DIMINISHING" },
  {
    "nutrient": "fiber",
    "preset": "custom",
    "grid": {
      "xCells": 2,
      "yCells": 2,
      "multipliers": [1.0, 1.0, 1.0, 0.8, 0.9, 1.0, 0.6, 0.8, 1.0]
    }
  }
]
```

Each entry has:

- `nutrient`: the nutrient key this curve applies to
- `preset`: one of `FLAT`, `DIMINISHING`, `CONFIDENCE_GATED`, `SYNERGY`, or
  `custom` to supply a hand-authored `grid`

For `preset: "custom"`, `grid` is a `(xCells+1) * (yCells+1)` flattened
multiplier lookup table over (intensity, confidence):

- `xCells` / `yCells`: number of grid cells along each axis
- `multipliers`: flattened `(xCells+1) * (yCells+1)` array of contribution
  multipliers, indexed `x * (yCells+1) + y`

## Built-in presets

- `FLAT`: uniform 1.0 everywhere — identical to the legacy flat scale/clamp
  math, and the default for every nutrient until changed
- `DIMINISHING`: multiplier decreases as intensity rises, regardless of
  confidence — prevents one large meal from disproportionately filling a bar
- `CONFIDENCE_GATED`: scales contribution down when scanner confidence is
  low
- `SYNERGY`: rewards high intensity and high confidence together

## Which file wins

1. Hardcoded `FLAT` preset for every nutrient (fallback only)
2. `config/nourished/nutrient_curves.json` on disk — edit this one
3. `data/nourished/config/nutrient_curves.json` (datapack override)
4. KubeJS runtime registration (highest — survives reload cycles)

## Notes

- Run `/nourished reload` to pick up changes without restarting