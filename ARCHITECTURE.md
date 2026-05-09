# Nourished — Architecture Reference

## 1. Terminology

This section defines the core concepts in Nourished and how they relate to each other. When in doubt, refer back here before introducing new names in code or datapacks.

---

### Nutrient

A single tracked value in a player's diet, represented as a float between `0.0` and `1.0`. Each nutrient corresponds to one of the six food groups: **Fruits, Vegetables, Proteins, Grains, Sugars, and Dairy**. These are the bars displayed on the Diet Screen. Nutrients are defined in `NutrientRegistry` and drive all buff and debuff calculations.

> A nutrient is a bar. It goes up when you eat the right food. It decays over time.

---

### Category

A nutrient key used as a logical label when processing food. When a food is eaten, it is assigned a **dominant category** — the nutrient it contributes to most strongly (e.g. `"proteins"`). Category is used in two places: adding to the nutrient bar, and tracking category-level fatigue in `DietData.categoryMemory`.

> Category and nutrient refer to the same six groups, but from different angles. Nutrient is the value. Category is the label attached to a food.

---

### Family

A grouping of foods that are similar in kind but may span multiple categories. Examples: `"fish"`, `"bread"`, `"leafy_greens"`. Family is used exclusively for **fatigue tracking** — if a player eats salmon, tuna, and cod in quick succession, their `familyMemory` for `"fish"` accumulates, reducing the nutritional return of further fish even if the specific items differ.

Family is nullable. Not every food needs one.

> Family answers the question: "have I been eating the same *kind* of thing too much?" Category answers: "have I been eating the same *nutrient group* too much?"

---

### Classification

The **process** by which the scanner pipeline examines a food item and determines its category and family. Classification is not a data structure you store — it is a pipeline stage. The scanner reads item tags, namespaces, keywords, and other signals (`ClassificationSignal`) and produces a score map that resolves into a dominant category and an optional family.

> Classification is the verb. Category and family are the nouns it produces.

---

### ClassificationSignal

A single piece of evidence collected during classification. Each signal has a type (e.g. `COMMUNITY_TAG`, `KEYWORD`, `NAMESPACE`), a source (what triggered it), and a map of nutrient score contributions. Multiple signals are aggregated by the scanner pipeline to determine the final classification.

> A signal is one clue. Classification is the conclusion drawn from all the clues.

---

### The Pipeline (how these connect)

```
Food Item
    │
    ▼
Scanner reads item tags, name, namespace
    │
    ▼
ClassificationSignals are collected
    │
    ▼
Scores aggregated → dominant Category + optional Family assigned
    │
    ▼
Player eats food → DietData.recordEat(itemId, category, family, time)
    │
    ├─→ nutrients map updated (the bar goes up)
    ├─→ categoryMemory updated (category fatigue tracked)
    └─→ familyMemory updated (family fatigue tracked)
```

---

### What "group" means (and why it doesn't exist as a term)

The word **group** is intentionally avoided in Nourished's API and documentation. It is ambiguous — it could mean nutrient group, food family, or category depending on context. If you find "group" appearing in code, treat it as a bug in naming and prefer `category` or `family` depending on what it actually represents.
