# Nourished: Food Values

`food_values.json` defines how a food category's total nutrition points get
split across the five underlying nutrient values (protein, carbs, fats,
vitamins, hydration) before being mapped onto whatever nutrient bars are
active.

## Schema

```json
[
  {
    "category": "vegetables",
    "protein": 0.1,
    "carbs": 0.2,
    "fats": 0.05,
    "vitamins": 0.55,
    "hydration": 0.1
  }
]
```

A JSON array of category objects. Fields:

- `category`: the food category key (matched by the scanner/classifier)
- `protein`, `carbs`, `fats`, `vitamins`, `hydration`: multipliers applied
  to a food's total nutrition points to split it across the five underlying
  values; an item worth 10 points in the `vegetables` category above would
  yield 1.0 protein, 2.0 carbs, 0.5 fats, 5.5 vitamins, 1.0 hydration

Categories not listed here fall back to an even 0.2 split across all five
values.

## Which file wins

1. Hardcoded Java defaults (used only as a last-resort fallback)
2. `config/nourished/food_values.json` on disk — edit this one
3. `data/nourished/config/food_values.json` (datapack override)

## Notes

- Run `/nourished reload` to pick up changes without restarting