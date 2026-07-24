# Nourished: Source Classifications

`source_classifications.json` you can manually assign nutrient values to specific
items, bypassing the scanner and tag pipeline entirely. Use this when an
item's automatic classification is wrong and you want to correct it directly.

If you want an item to contribute nothing at all, use
`excluded_items.json`: that removes an item from nutrient tracking entirely
rather than reassigning its values.

## Schema

```json
[
  {
    "source_id": "minecraft:steak",
    "values": {
      "proteins": 0.8
    },
    "total": 1.0,
    "enabled": true
  }
]
```

- `source_id`: the item's registry id (e.g. `minecraft:steak`,
  `farmersdelight:chicken_soup`)
- `values`: nutrient key to weight. Keys must match your registered
  nutrients (see `/marieslib status` for the current list)
- `total`: optional total value override; omit or set to `0` to let the
  system calculate the total normally
- `enabled`: set to `false` to disable an entry without deleting it

## Getting starting values

Run `/marieslib dump nourished_nutrients` to export current live nutrient
values as a reference before writing entries, easier to copy and adjust
than to write values from scratch.

## Which file wins

`config/nourished/overrides/source_classifications.json` is the file you
edit. A datapack override at
`data/nourished/config/source_classifications.json`, if present, replaces it
entirely: entries aren't combined between the two.

## Migration

If you had entries in `source_values.json` or `source_overrides.json` from a
previous version, they were automatically merged into this file on first
launch. You can safely delete the old files.
