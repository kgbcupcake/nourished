# Nourished: Effects

`effects.json` maps nutrient thresholds to status effects — the potion
effects players get for running critically low, low, or excess on a
nutrient.

## Schema

```json
[
  {
    "id": "protein_penalty",
    "effect": "minecraft:weakness",
    "nutrient": "proteins",
    "trigger": "below",
    "threshold": 0.25,
    "amplifier": 0,
    "duration_ticks": 140,
    "enabled": true,
    "threshold_max": 1.0,
    "ambient": true,
    "show_particles": false
  }
]
```

A JSON array of effect objects. Fields:

- `id`: unique identifier for this effect entry
- `effect`: the status effect id to apply (e.g. `minecraft:weakness`)
- `nutrient`: the nutrient key this effect is tied to (must match a key in
  `nutrients.json`, or `all` for an all-nutrients trigger)
- `trigger`: `below`, `above`, or `all_above` — when the effect fires
  relative to `threshold`
- `threshold`: the fraction (0.0–1.0) that triggers the effect
- `amplifier`: potion effect amplifier level (0 = level I)
- `duration_ticks`: how long the applied effect lasts, in ticks
- `enabled`: whether this entry is active
- `threshold_max`: upper bound fraction for the trigger band
- `ambient`: whether the effect is rendered as ambient (softer particles)
- `show_particles`: whether potion particles are shown

## Which file wins

1. Bundled built-in defaults (`protein_penalty`, `carbs_penalty`,
   `vitamins_penalty`, `hydration_penalty`, `all_high_health`,
   `all_high_regen`)
2. `config/nourished/effects.json` on disk — edit this one

## Notes

- Effects registered via the API (`registerCustomEffect`) or KubeJS are
  merged in at runtime and persisted back to this file
- Run `/nourished reload` to pick up changes without restarting