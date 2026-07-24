# Nourished: Raw Food

`raw_food.json` configures the Raw Food module: which items/severities count
as "raw", the penalties applied per severity tier, per-item cookedness
overrides, and the gut recovery simulation.

## Schema

```json
{
  "memory_secs": 120,
  "tokens": {
    "mild": ["fresh", "unripe"],
    "medium": ["raw", "uncooked"],
    "severe": ["rotten", "spoiled", "crude"]
  },
  "overrides": {
    "minecraft:beef": 0.1,
    "minecraft:porkchop": 0.1
  },
  "tiers": {
    "fine": {
      "effect_pool": [],
      "duration_ticks": 0,
      "amplifier": 0,
      "nutrient_penalty": 0.0,
      "missed_opportunity_multiplier": 0.0
    },
    "mild": {
      "effect_pool": ["minecraft:hunger"],
      "duration_ticks": 600,
      "amplifier": 0,
      "nutrient_penalty": -0.03,
      "missed_opportunity_multiplier": 0.15,
      "resistance": {
        "threshold": 0.6,
        "max_resistance": 0.5,
        "nutrient_weights": {
          "vegetables": 0.3,
          "fruits": 0.2
        }
      }
    }
  },
  "gut": {
    "tick_interval": 100,
    "base_recovery_rate": 0.001,
    "cooked_food_recovery_rate": 0.02,
    "diversity_threshold": 0.6,
    "diversity_bonus_rate": 0.0005,
    "sensitivity_decay_rate": 0.0002,
    "max_sensitivity_multiplier": 2.0,
    "sensitivity_increment_per_raw_eat": 0.05
  }
}
```

Top-level fields:

- `memory_secs`: how long (seconds) a raw-food penalty is "remembered" for
  diminishing-returns/resistance purposes
- `tokens`: maps severity name (`mild`, `medium`, `severe`; `fine` is
  omitted since it applies no penalty) to a list of lowercase substrings
  matched against an item's registry path or display name to classify it,
  when the item isn't already tagged via `nourished:raw_source_*`
- `overrides`: per-item cookedness override, keyed by item id, value
  0.0 (fully raw) to 1.0 (fully cooked)
- `tiers`: per-severity (`fine`, `mild`, `medium`, `severe`) penalty
  configuration:
  - `effect_pool`: status effect ids that may be applied when this severity
    triggers
  - `duration_ticks`: applied effect duration
  - `amplifier`: applied effect amplifier level
  - `nutrient_penalty`: flat nutrient point penalty applied on eating
  - `missed_opportunity_multiplier`: fraction of the food's normal nutrition
    that is lost instead of granted
  - `resistance` (optional): gut resistance buildup for this severity —
    `threshold` (fraction before resistance starts reducing penalties),
    `max_resistance` (cap on penalty reduction), `nutrient_weights` (map of
    nutrient key to how strongly high levels of that nutrient build
    resistance; keys must match `nutrients.json`)
- `gut`: gut recovery simulation tuning — `tick_interval` (ticks between
  gut updates), `base_recovery_rate` (passive recovery per update),
  `cooked_food_recovery_rate` (bonus recovery from eating cooked food),
  `diversity_threshold` (fraction of distinct recent foods needed for the
  diversity bonus), `diversity_bonus_rate` (extra recovery when above the
  threshold), `sensitivity_decay_rate` (how fast built-up sensitivity
  fades), `max_sensitivity_multiplier` (cap on penalty amplification from
  sensitivity), `sensitivity_increment_per_raw_eat` (sensitivity gained per
  raw food eaten)

## Which file wins

1. Bundled built-in defaults
2. `config/nourished/raw_food.json` on disk — edit this one
3. `data/nourished/config/raw_food.json` (datapack override)

## Notes

- Run `/nourished reload` to pick up changes without restarting