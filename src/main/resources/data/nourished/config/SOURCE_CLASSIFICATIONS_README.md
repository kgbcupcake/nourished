# Nourished — Source Classifications

`source_classifications.json` lets you manually assign nutrient values to
specific items, bypassing the scanner and tag pipeline entirely.

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

- `source_id` — the item's registry id (e.g. `minecraft:steak`,
  `farmersdelight:chicken_soup`)
- `values` — nutrient key to weight. Keys must match your registered nutrients
- `total` — optional total value override. Omit or set to 0 to skip
- `enabled` — set to false to disable an entry without deleting it

## Notes

- Entries here take full precedence over scanner classification and tag inference
- Datapack authors can override this file via
  `data/nourished/config/source_classifications.json`
- Run `/marie dump nourished_nutrients` to export current live values as a
  reference before writing entries

## Migration

If you had entries in `source_values.json` or `source_overrides.json` from a
previous version, they were automatically merged into this file on first launch.
You can safely delete the old files.
