# Nourished: Excluded Items

`excluded_items.json` fully excludes specific items from nutrient tracking,
they're treated as if they have no food data at all, no matter what the
scanner, tags, or classifications would otherwise assign them.

## When to use this vs. food_overrides.json

- **food_overrides.json**: corrects an item's nutrient *values* (it still
  counts, just with different numbers)
- **excluded_items.json**: removes an item from nutrient tracking *entirely*
  (decoy items, non-food edibles, anything that shouldn't move any bar)

## Schema

```json
[
  "minecraft:rotten_flesh",
  "farmersdelight:raw_beef"
]
```

A plain JSON array of item registry id strings.

## Which file wins

Only one source applies at a time, they aren't combined:

1. Bundled defaults (ships empty by default)
2. `config/nourished/overrides/excluded_items.json` on disk — edit this one
3. A datapack override at `data/nourished/config/excluded_items.json`, if
   present, replaces #2 entirely

## Notes

- Exclusion is checked before tag matching, external classification, and
  runtime inference: an excluded item never gets scored, even if it also
  matches a nutrient tag
- Excluded items still restore vanilla hunger/saturation normally; only
  nutrient tracking is skipped
- Run `/nourished reload` to pick up changes without restarting
