# Nourished — Food Overrides

`food_overrides.json` lets you override nutrient values and calories for any specific item, regardless of how Nourished classified it elsewhere.

## Schema

```json
[
  {
    "item": "minecraft:steak",
    "nutrients": {
      "proteins": 0.8,
      "fats": 0.2
    },
    "calories": 60,
    "enabled": true
  }
]
```

- `item`: the item's registry id (e.g. `minecraft:steak`, `farmersdelight:onion`)
- `nutrients`: nutrient key to weight (matches the keys shown in `/marieslib status` or your registered nutrients)
- `calories`: integer calorie value for this item
- `enabled`: set to `false` to disable an override without deleting it

## Getting starting values

Run `/marieslib dump nourished_nutrients` (or use the "Export All Foods" button in the Scanner tab of the config screen) to write `nourished_nutrients_export/`: a folder of read-only reference files, one per nutrient category (`fruits.json`, `proteins.json`, `vegetables.json`, etc.), each listing every item Nourished currently resolves into that category, with its live nutrient values and calories.

To turn an export entry into an override, copy it from the relevant category file into `food_overrides.json` and reshape it from:

```json
{ "item": "minecraft:steak", "nutrients": { "proteins": 0.8 }, "calories": 60 }
```

to:

```json
{ "item": "minecraft:steak", "nutrients": { "proteins": 0.8 }, "calories": 60, "enabled": true }
```

(the export entries already include the `item` field — just add `"enabled": true`)

The export files are reference only — editing them does nothing on their own. Only entries actually present in `food_overrides.json` take effect.
