# Nutrients (Omega-3, Fiber, ...)

Nutrients are tracked separately from the five food-group bars. They never change the bars, food overrides or the balance score.

## Defining a nutrient

Add `data/<namespace>/nourished/micros/<key>.json`. The file name is the key.

```json
{
  "display_name": "Omega-3",
  "color": "#4FA3D9",
  "decay_rate": 0.01,
  "low_threshold": 0.2,
  "high_threshold": 0.9,
  "icon": "minecraft:cod",
  "group_weights": { "proteins": 0.2 }
}
```

- `decay_rate`: how much is lost every `decayIntervalTicks` (see `nourished-nutrients.toml`). Levels run from 0 to 1.
- `group_weights` (optional): used for foods with no explicit value and no recipe. It gives each food-group share of the food this much of the nutrient.

## Giving foods nutrients

Add `data/<namespace>/nourished/micro_values/<anything>.json`. Keys are item ids or `#tags`. Item ids win over tags.

```json
{
  "#c:foods/cooked_fish": { "omega3": 1.0, "fat": 0.3 },
  "mymod:salmon_sushi": { "omega3": 1.2, "fiber": 0.2 }
}
```

Values are per serving: 1.0 is a "full" serving. Each bite adds `value × bite size × gainScale`, where bite size grows with the food's nutrition and saturation.

## How a food's nutrients are found

1. An explicit item or tag entry.
2. Otherwise, the sum of its recipe ingredients' nutrients, times `recipeScale`, capped at `recipeMax` (`recipeDerivation` turns this off).
3. Otherwise, its food-group classification through each nutrient's `group_weights`.

## Server config file

`config/nourished/nutrition/micros.json` takes the same data in one file, `{ "micros": { "<key>": { ... } }, "values": { ... } }`. It overrides Nourished's bundled data. Other datapacks override it.

## Effects

Add `data/<namespace>/nourished/micro_effects/<name>.json`:

```json
{ "nutrient": "omega3", "trigger": "below", "threshold": 0.15,
  "effect": "minecraft:mining_fatigue", "amplifier": 0, "duration_ticks": 200 }
```

- `trigger`: `below`, `above`, or `all_above` with `"nutrient": "all"` (every nutrient above the threshold).
- Checked every 4 seconds. Effects are short and refreshed while the condition holds, then run out on their own.
- `below` never fires for a nutrient the player hasn't eaten yet.
- The same objects can go in the `effects` array of `micros.json`.

Nourished ships six of these (low Omega-3/Fiber/Vitamins/Minerals, high Omega-3, all balanced). They are off until `builtinEffects = true` in `nourished-nutrients.toml`. Effects from other datapacks or `micros.json` always run. The main `enableEffects` switch turns all of them off.

The regular effect rules in `effects.json` can also target a nutrient with `"nutrient": "micro:omega3"`.

## Commands

- `/nourished nutrients`: your own levels.
- `/nourished nutrients item [item]`: a food's nutrients and where they came from (explicit, recipe or group).
- `/nourished nutrients levels <player>`, `set <player> <nutrient> <0-1>`, `reset <player>` (op).
- `/nourished nutrients value set <item> <nutrient> <amount>` / `value clear <item>` (op): edits `micros.json` and applies it right away.
- `/nourished nutrients reload` (op): re-reads `micros.json`.

## Editing in game

The item editor has a **Nutrients** page. Turn on "Custom Nutrient Values" to save that item's values to `micros.json`. Singleplayer or LAN host only, like the other editor pages.

## Detail screen

Click the Nutrients box in the Diet Screen (or bind "Open Nutrients") to see each nutrient's level, trend, what you've recently eaten for it, and the best foods for it.
