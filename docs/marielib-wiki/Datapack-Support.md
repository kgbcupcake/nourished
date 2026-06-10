# Datapack Support

MarieLib is built to be extended through standard Minecraft datapacks. Consuming mods expose datapack directories under their mod id.

All files live under:

```
data/<your_namespace>/<modId>/
```

For Nourished (`modId = nourished`):

```
data/mypack/nourished/source_classifications/my_foods.json
```

---

## 📁 Directory Overview

| Directory | Status | What it does |
|---|---|---|
| `source_classifications/` | ✅ Working | Map items or tags to a value key |
| `compat/` | ✅ Working | Register mod compatibility entries |
| `source_families/` | ✅ Working | Extend keyword-to-value mappings |
| `module_locks/` | ✅ Working | Lock config modules server-side |
| `values/` | 🔄 In progress | Add custom value definitions |
| `effects/` | 🔄 In progress | Add threshold-triggered effects |
| `synergies/` | 🔄 In progress | Two-value synergy interactions |
| `source_synergies/` | 🔄 In progress | Source pair combo bonuses |
| `milestones/` | 🔄 In progress | Cumulative value goals |
| `tracking_profiles/` | 🔄 In progress | Named tracking profile presets |

Include `"marie_schema_version": 1` in all files.

---

## 🥕 Source Classifications

Path: `data/<ns>/<modId>/source_classifications/<id>.json`

Maps a specific item or item tag directly to a value key.

### By item

```json
{
  "marie_schema_version": 1,
  "value_key": "vegetables",
  "item": "farmersdelight:tomato",
  "amount": 0.08
}
```

### By tag

```json
{
  "marie_schema_version": 1,
  "value_key": "proteins",
  "tag": "#farmersdelight:foods/cooked_meat",
  "amount": 0.10
}
```

| Field | Required | Description |
|---|---|---|
| `value_key` | ✅ | Target value key |
| `item` | One of | Specific item ID |
| `tag` | One of | Item tag (prefix with `#`) |
| `amount` | ✅ | Contribution per consumption (0.0–1.0) |

---

## 🔌 Compat Entries

Path: `data/<ns>/<modId>/compat/<id>.json`

```json
{
  "marie_schema_version": 1,
  "mod_id": "examplemod",
  "category": "FOOD_MOD",
  "mappings": {
    "examplemod:mystery_fruit": "fruits",
    "examplemod:protein_bar": "proteins"
  }
}
```

---

## 🌿 Source Families

Path: `data/<ns>/<modId>/source_families/<id>.json`

Extend keyword classification with custom keyword-to-value mappings:

```json
{
  "marie_schema_version": 1,
  "families": {
    "vegetables": ["gourd", "squash", "yam"],
    "proteins": ["jerky", "patty", "schnitzel"]
  }
}
```

---

## 🔒 Module Locks

Path: `data/<ns>/<modId>/module_locks/<id>.json`

Lock config modules server-side so players cannot override them:

```json
{
  "marie_schema_version": 1,
  "locked": ["enableEffects", "enableDecay"],
  "server_only": ["bonusEffectThreshold"]
}
```

---

## 💡 Tips

- Test with `/reload` then `/<modId> reload` — no restart required.
- Prefer tags over individual items when possible.
- Run `/<modId> diagnostics` after adding datapack content.
- Use `/<modId> schema <type>` to print valid JSON templates.

---

## 📚 Related Pages

- [Community Tags](Community-Tags)
- [Effects Reference](Effects-Reference)
- [Validation Reports](Validation-Reports)
- [API Reference](API-Reference)
