# Nourished: Nutrients

`nutrients.json` defines the nutrient bars tracked by Nourished (built-in:
`fruits`, `vegetables`, `proteins`, `grains`, `dairy`), plus any custom
nutrients you add.

## Schema

```json
[
  {
    "key": "vegetables",
    "display_name": "Vegetables",
    "color": -13851166,
    "default_decay_rate": 0.0,
    "critical_threshold": 0.0,
    "low_threshold": 0.0,
    "excess_threshold": 1.0,
    "beneficial": true,
    "icon": "minecraft:carrot",
    "tags": ["nourished:nutrients/vegetables"]
  }
]
```

A JSON array of nutrient objects. Fields:

- `key`: unique nutrient id (matched against `food_values.json`, effects, and
  item tags)
- `display_name`: label shown in the HUD/Diet Screen
- `color`: ARGB int used for the nutrient's bar color
- `default_decay_rate`: passive decay applied per tick when not otherwise
  configured
- `critical_threshold` / `low_threshold` / `excess_threshold`: fractions
  (0.0–1.0) marking the bar's critical, low, and excess bands
- `beneficial`: whether high values are good (`true`) or bad (`false`) for
  display/threshold interpretation only — it does not affect effect
  triggers, which are configured explicitly in `effects.json`
- `icon`: item id used as the nutrient's icon
- `tags`: item tags scored toward this nutrient

## Which file wins

1. Hardcoded built-in defaults (used only if the config file is missing or
   invalid)
2. `config/nourished/nutrients.json` on disk — edit this one

## Notes

- Adding an entry with a new `key` registers a new nutrient bar
- Removing a built-in nutrient's entry causes it to fall back to its
  hardcoded default the next time the file is regenerated
- Run `/nourished reload` to pick up changes without restarting