# Nourished Roadmap

Nourished began as a nutrition mod and grew into a data-driven diet framework. The engine now lives in [MariesLib](https://github.com/kgbcupcake/MarieLib/blob/main/RoadMap.md). This roadmap is nutrition gameplay only.

---

## Balancing and UX

- Nutrient decay tuning
- HUD customization and edit-mode polish
- Diet Screen improvements
- Multiplayer nutrition sync UX

---

## Datapacks

- Expanding what modpack authors can drive without Java
- Better scanner output → datapack workflow
- More preset configs for common server types

---

## Ecosystem integrations

### Farmers Delight

- Food classification coverage
- Nutrition balancing for FD items
- Datapack compatibility

### Quest and progression

- Quest triggers on nutrient thresholds
- Nutrition objectives for quest mods

### Advancements

- Balanced diet milestones
- Neglect / recovery achievements

### Accessories

- Curios support
- Equipment-based nutrition effects

### Compat mods

- Peak Stamina, Spice of Life Onion, Legendary Survival Overhaul (ongoing)
- More popular food mods

---

## Modules

- Stamina module polish
- Raw food / gut health balancing
- Hydration (future module, separate from nutrition core)

---

## Classification investigation

### CLS-017

Investigate unexpected protein contribution in Create Food ice cream sandwich classifications.

**Priority:** Low | **Status:** Open

While reviewing runtime traces, `createfood:chorus_fruit_ice_cream_sandwich` showed a protein signal that wasn't immediately obvious from recipe composition. Final classification is acceptable — no player-facing bug. Want ingredient-level trace attribution eventually so these are faster to diagnose.

**Example trace:**

- Runtime: proteins 0.33, fruits 0.26, grains 0.41
- Tag: dairy 1.0
- Blend: dairy 0.67, proteins 0.11, grains 0.14, fruits 0.09

**Future improvement:** ClassificationTrace with per-ingredient signal attribution (e.g. `milk → +0.20`).

---

## Infrastructure

Sync, validation, compiler tooling, and remaining datapack loaders are tracked in the [MariesLib RoadMap](https://github.com/kgbcupcake/MarieLib/blob/main/RoadMap.md).
