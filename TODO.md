# Nutritional Balance — TODO

## 🔧 Dynamic Config System *(next priority)*

Right now adding a new nutrient (like `dairy`) requires touching:
- `DietData.java` — codec fields, constructor, `fromCodec`
- `FoodNutritionRegistry.java` — `DietDelta` record, tag mappings, delta calc
- `FoodEatenHandler.java` — new `addNutrient` call
- `DietScreen.java` — icon map

**Goal:** New nutrients should be defined in a single JSON file and automatically picked up everywhere — UI, codec, effects, sync, and tag mappings.

### What to build

- [ ] `nutrients.json` in `config/nourished/` — defines each nutrient:
  ```json
  {
    "nutrients": [
      { "key": "fruits",     "icon": "minecraft:apple",      "tags": ["c:foods/fruits"] },
      { "key": "vegetables", "icon": "minecraft:carrot",     "tags": ["c:foods/vegetables"] },
      { "key": "proteins",   "icon": "minecraft:cooked_beef","tags": ["c:foods/meats"] },
      { "key": "grains",     "icon": "minecraft:bread",      "tags": ["c:foods/bread"] },
      { "key": "sugars",     "icon": "minecraft:sugar",      "tags": ["c:foods/sweets"] },
      { "key": "dairy",      "icon": "minecraft:milk_bucket","tags": ["c:foods/dairy"] }
    ]
  }
  ```
- [ ] `NutrientRegistry.java` — loads and holds the config at startup, exposes `getKeys()`, `getIcon(key)`, `getTags(key)`
- [ ] Replace hardcoded `BAR_ORDER` in `DietData` with `NutrientRegistry.getKeys()`
- [ ] Replace `RecordCodecBuilder` with a dynamic `MapCodec` keyed by nutrient key strings so no manual field per nutrient
- [ ] Replace hardcoded `TAG_DIET_BAR` entries in `FoodNutritionRegistry` with entries loaded from config
- [ ] Replace hardcoded `NUTRIENT_ITEMS` map in `DietScreen` with `NutrientRegistry.getIcon(key)`
- [ ] Hot-reload support — changes to `nutrients.json` apply on world reload without restarting the game

---

## 🎨 Visual Polish

- [ ] **HUD overlay** — compact bars always visible on screen, no need to open diet screen
- [ ] **Screen fade-in on open** — 150ms alpha transition when DietScreen opens
- [ ] **Bar flash on eat** — smooth highlight when a nutrient just increased
- [ ] **Toast notification when bar goes critical** — fires at <25%, styled like advancement toast

---

## ⚔️ Gameplay Depth

- [ ] **Food memory** — track last N foods eaten, diminishing returns for repeating the same food
- [ ] **Hunger integration** — balanced diet slows natural hunger drain
- [ ] **Sleep bonus** — wake up with a short buff if all bars above 50% when sleeping
- [ ] **Configurable thresholds** — low/high/excess cutoffs tunable per nutrient in config

---

## 📦 Content

- [ ] **Balance potion** — craftable item that temporarily boosts all bars
- [ ] **Diet Journal item** — opens the diet screen without the inventory button
- [ ] **Trinket / enchantment** — slows nutrient decay rate

---

## 🛠 Technical

- [ ] `/nutritionbalance debug <player>` — server command to inspect any player's values
- [ ] **Advancements** — milestones for hitting balance goals (first balanced day, all bars green, etc.)

---

## ✅ Done

- [x] 6 food group bars (fruits, vegetables, proteins, grains, sugars, dairy)
- [x] Tag-based food detection — Pam's HarvestCraft 2 + Farmer's Delight compat
- [x] Passive effects tied to nutrition state
- [x] Nutrient decay over time
- [x] Codec serialization — saves to disk correctly
- [x] Server → client sync on login and after eating
- [x] Diet screen — split layout, animated bars, icons, tooltips, trend arrows, reset timer
- [x] Inventory button injection
- [x] NeoForge 1.21.1 minimum version declared
