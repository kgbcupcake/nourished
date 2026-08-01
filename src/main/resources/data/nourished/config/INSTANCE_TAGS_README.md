# Nourished: Instance Tags

`config/nourished/instance_tags/` lets you pull items into a nutrient category
across every world on this Minecraft instance, without touching any single
world's own datapack tags.

## When to use this vs. a world datapack tag

- **World datapack tag**: applies only to that one save
- **Instance tag file (this folder)**: applies to every save on this
  instance — useful for a preference you want everywhere, not just one world

Both sources are additive, not either/or: an item can be pulled into a
category by a world datapack tag, an instance tag file, or both at once.

## Schema

One file, `config/nourished/instance_tags/instance_tags.json`: a JSON object
keyed by category name, each value an array of item registry id strings.

```json
{
  "grain": [
    "minecraft:bread"

  ],
  "dairy": [
    "minecraft:milk_bucket"
  ]
}
```

## What category means

Each key must match a category already listed under `community_tags` in
`scanner_spec.json` — the same category names used for the scanner's own
tag matching. A key that doesn't match one is silently inert: it's ignored,
no error, no effect.

## Notes

- This is purely additive: it can only add items to a category, never remove
  or override one
- Run `/nourished reload` to pick up changes without restarting
