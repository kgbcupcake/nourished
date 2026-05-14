# Contributing to `scanner_spec.json`

Welcome! This guide is for **modpack players** and **mod authors** who want to help Nourished classify more foods automatically. You do not need to read any game code. If you can edit JSON, you can contribute.

Nourished targets **NeoForge 1.21.1** with mod id `nourished`.

---

## 1. What is `scanner_spec.json`?

`scanner_spec.json` is a **data file** that tells Nourished’s automatic food classifier how to map foods to the six nutrient categories (fruits, vegetables, proteins, grains, sugars, dairy).

- If a food item already has an explicit `nourished:nutrients/*` item tag, that tag wins and the scanner is not needed for that item.
- If there is **no** such tag, Nourished uses **weighted signals** from this file (keywords, suffixes, namespaces, archetypes, and a few other rules) to infer the best match at **runtime**.

**Runtime and reloads**

- You do **not** need to recompile the mod to try changes.
- For quick local testing, copy your edited file to **`config/nourished/scanner_spec.json`** and run **`/nourished reload`** in-game (cheats enabled; operator-level permission on a server).
- Official contributions ship in the repository copy at  
  `src/main/resources/data/nourished/nourished/scanner/scanner_spec.json`  
  (and are also written to the config path on first run so players can start from the same defaults).

**`unknown_foods.log` — how discoveries turn into PRs**

- Location: **`config/nourished/unknown_foods.log`** (next to your other Nourished config files).
- When the game sees a food it still cannot classify confidently, it can append an **`[UNKNOWN]`** block for that item.
- Treat that file as a **to-do list**: each entry is a candidate for a new keyword, suffix, namespace tweak, or archetype in `scanner_spec.json`.
- Entries may also appear when scores exist but confidence is still low; those lines help you tune weights without starting from zero.

---

## 2. Nutrient categories

All six groups used by the scanner:

| Key | Meaning |
|-----|--------|
| **fruits** | Fresh fruits, juices, smoothies |
| **vegetables** | Vegetables, salads, herbs |
| **proteins** | Meats, fish, eggs, legumes |
| **grains** | Bread, pasta, rice, cereals |
| **sugars** | Sweets, candy, desserts, honey |
| **dairy** | Milk, cheese, cream, yogurt |

---

## 3. How to read `unknown_foods.log`

Here is a **realistic example** of a hard miss (field names match the log; your file may also include a `Time` line with a timestamp):

```text
[UNKNOWN] croptopia:ajvar
  Namespace : croptopia
  Stage     : HARD_FALLBACK
  Tokens    : [ajvar]
  Top Scores:
  Confidence: 0.00
```

What each part means:

- **`[UNKNOWN] croptopia:ajvar`** — The item id (`namespace:path`) that needs better coverage.
- **`Namespace`** — The mod id (`croptopia` here). Useful when deciding between a **namespace** bump vs a **keyword** for one dish.
- **`Stage`** — How far classification got. **`HARD_FALLBACK`** means **no usable signal** was found: nothing in the spec (and related rules) produced a confident result. This is the clearest signal that a **new keyword** (or another signal type) is needed.
- **`Tokens`** — The name was split into these pieces for matching. You usually add coverage that speaks to **these** tokens.
- **`Top Scores`** — Best raw nutrient scores after combining signals. Empty or very small values mean the classifier had almost nothing to work with.
- **`Confidence`** — How decisive the winner was. **`0.00`** here lines up with a total miss.

---

## 4. The four ways to add coverage

### A — Keywords (most common)

**What they are:** A map from a **single text token** (usually one word from the item path) to **nutrient weights**.

**When to use:** Simple ingredients, obvious dish names, or any case where one token strongly hints the food type.

**Example:**

```json
"meatball": { "proteins": 3.0 }
```

**Weight guide**

- **3.0** — Strong, clear signal (for example beef → proteins).
- **2.0** — Moderate signal (for example corn → grains, even though corn is vegetable-adjacent in real life).
- **1.0** — Weak supporting signal.

### B — Suffixes

**What they are:** The **last segment** of the item path (after splitting on `_`) maps to nutrient weights. Think “dish shape,” not full sentences.

**When to use:** Shapes of items that almost always imply a category, regardless of mod naming quirks.

**Example:**

```json
"stew": { "vegetables": 2.0, "proteins": 1.0 }
```

### C — Namespaces

**What they are:** The **mod id** maps to nutrient weights applied to **every food item** from that mod.

**When to use:** Mods that are overwhelmingly one kind of food (for example a berries mod or a fish-farming mod).

**Warning:** Namespace rules are **broad**. Keep weights modest (often **2.0–4.0** max) so one odd item in the mod is not dragged into the wrong group.

**Example:**

```json
"aquaculture": { "proteins": 4.0 }
```

### D — Archetypes

**What they are:** A **pattern** substring and a **contributions** map that can credit **several** nutrients at once.

**When to use:** Culturally specific dishes where a single keyword would lie about the meal (for example something that is genuinely both grains and proteins).

**Example:**

```json
{
  "pattern": "enchilada",
  "contributions": { "grains": 2.0, "proteins": 2.0 }
}
```

---

## 5. Composite foods

**Composite** resolution is the friendly name for “this meal really is two groups at once.”

When the **second-highest** nutrient score is **within 50%** of the **top** score (by ratio), the result is treated as **composite** instead of forcing a single winner. The exact threshold can be tuned in the main mod config, but **0.50** is the default.

**Example:** A item like `pasta_with_meatballs` might end up with something like **grains 0.63** and **proteins 0.37** after normalization — two groups matter.

**What this means for you:** When you assign weights, aim for **honest proportions** (what the dish is mostly made of), not for artificially crushing every meal into one bar.

---

## 6. What not to do

- **Do not** add a **namespace** entry for a **mixed** cooking mod. Farmer’s Delight–style mods span proteins, grains, vegetables, and more; a namespace-only rule will misclassify a large slice of the catalog.
- **Do not** push **keyword** weights above **4.0**. The file also defines **multipliers** per signal type; large weights stack and amplify more than newcomers expect.
- **Do not** add **`negative_keywords`** unless you are very sure. They **subtract** from categories and can create surprising failures elsewhere.
- **Do not** spend effort on **pure preparation** tokens (`cooked`, `roasted`, `fried`, and similar). Many of these are **normalized** (for example `-ed` forms often collapse toward a root) or otherwise carry little stable signal; prefer the **food word** itself (`meatball`, `steak`, `toast`).
- **Do not** duplicate keys. Search existing **`keywords`**, **`suffixes`**, **`namespaces`**, and **`archetypes`** before adding a new line.

---

## 7. Contribution checklist

Before you open a pull request:

- [ ] You reproduced the miss using **`unknown_foods.log`** (or you can explain why the item is miscategorized even without a log line).
- [ ] You searched for **conflicts** with existing keywords, suffixes, namespaces, and archetypes.
- [ ] You verified the result in-game with **`/nourished debug held`** while holding the food (cheats on).
- [ ] Weights follow the **1.0 / 2.0 / 3.0** spirit (and respect the “no keyword above 4.0” rule).
- [ ] JSON is valid: **no trailing commas**, **no comments**, UTF-8 text, keys quoted as normal JSON strings.
- [ ] If plural or variant forms matter, you followed **section 8** (stem and irregular notes in the PR body when automation is not enough).

---

## 8. Stem and plural handling

Before keyword lookup, item names are **normalized**: common English plurals and variants are folded toward a **canonical root** so you usually only define the singular keyword (`meatballs` → `meatball`, `tamales` → work with the same root as `tamale` when the rules line up).

**When to add a PR note**

- If play-testing shows that a plural or loanword **does not** land on the keyword you expect, add a short subsection to your PR titled something like **“Stem / irregular forms”** listing **from → to** pairs you need (for example unusual plurals not covered by the built-in rules).
- Maintainers can then extend the shared normalization list; you do **not** duplicate the same keyword under every plural spelling in JSON unless there is a good reason.

---

## 9. Testing your changes

1. Copy your edited `scanner_spec.json` into **`config/nourished/`** (replace the generated file there while testing).
2. Join a world with cheats enabled (or operator rights on a server).
3. Run **`/nourished reload`** so Nourished reloads data and picks up the file.
4. Hold the food item in your main hand.
5. Run **`/nourished debug held`**.
6. Confirm **Stage**, **Confidence**, and **normalized** nutrient breakdowns look sensible for that item.

---

Thank you for helping players enjoy balanced diets in more modpacks. Every careful line of JSON makes the experience smoother for someone else.
