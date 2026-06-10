# Community Tags

Tags are the fastest and most reliable way to get source items classified correctly in MarieLib. This page covers which tags MarieLib reads and how to use them.

---

## 🏷️ How Tags Work in Classification

Tags are the highest-priority signal in the classification pipeline. A tagged item will almost always be classified correctly, regardless of its name or recipe.

| Signal | Priority Multiplier |
|---|---|
| Value tags (`<modId>:values/*` or consumer-specific) | Highest |
| Community tags (`c:foods/*`) | 5× |
| Namespace grouping | 4× |
| Name suffix match | 3× |
| Keyword match | 2× |
| Recipe inheritance | 1× |

See [Classification Pipeline](Classification-Pipeline) for the full trace output.

---

## 🌿 Community Tags — `c:foods/*`

The `c:` namespace is a cross-mod convention for categorizing items.

MarieLib reads the following `c:foods/` suffixes:

### Fruits
```
c:foods/fruit
c:foods/fruits
c:foods/berry
c:foods/berries
```

### Vegetables
```
c:foods/vegetable
c:foods/vegetables
c:foods/crop
c:foods/crops
```

### Proteins
```
c:foods/meat
c:foods/meats
c:foods/cooked_meat
c:foods/fish
c:foods/egg
c:foods/eggs
```

### Grains
```
c:foods/grain
c:foods/grains
c:foods/bread
c:foods/breads
```

### Dairy
```
c:foods/dairy
c:foods/cheese
```

Exact tag-to-value mappings are configurable by the consuming mod. The list above reflects common defaults.

---

## 🔖 Consumer Value Tags

Consuming mods register value tags for direct classification. For Nourished:

```
nourished:nutrients/fruits
nourished:nutrients/vegetables
nourished:nutrients/proteins
nourished:nutrients/grains
nourished:nutrients/dairy
```

File location: `data/nourished/tags/item/nutrients/<key>.json`

For a generic Marie mod, the pattern is:

```
<modId>:values/<valueKey>
```

---

## 📦 Adding Tags via Datapack

```
my_datapack/
└── data/
    └── nourished/
        └── tags/
            └── item/
                └── nutrients/
                    ├── fruits.json
                    ├── vegetables.json
                    └── ...
```

Each file follows standard Minecraft tag format:

```json
{
  "values": [
    "mymod:item_one",
    "mymod:item_two",
    "#mymod:some_item_tag"
  ]
}
```

---

## 💡 Tips

- `c:foods/*` tags benefit every mod that reads them — not just MarieLib.
- Consumer value tags (`nourished:nutrients/*`) are for precision overrides.
- Tags propagate through `#` references.
- For Nourished-specific behavior tags (`raw_food`, `meal`, `light_food`), see the [Nourished Community Tags page](https://github.com/kgbcupcake/nourished/wiki/Community-Tags).

---

## 📚 Related Pages

- [Classification Pipeline](Classification-Pipeline)
- [Compatibility Guide](Compatibility-Guide)
- [Datapack Support](Datapack-Support)
