# Diagnostics & Tracing

MarieLib includes extensive diagnostic tooling for investigating classification decisions, datapack issues, and runtime performance.

---

## 🍎 Held Item Inspector

```
/<modId> debug held
```

Runs the complete classification pipeline against the currently held source item.

**Output file:**

```
config/<modId>/debug/trace_dump.txt
```

**Shows:**

- Every pipeline stage (`TraceStepId`) and its status
- Value tag matches and runtime resolver scores
- Tag-vs-runtime blend precedence
- Confidence calculations
- Final value assignment and dominant key

This is the primary tool for determining why a source was classified the way it was.

---

## 📈 Cache Statistics

```
/<modId> debug cache
```

Displays runtime resolver cache statistics:

| Metric | Description |
|---|---|
| Hits | Cache hits |
| Misses | Cache misses |
| Hit ratio | Percentage of cached resolutions |
| Cache size | Current entries / max (2048) |
| Avg resolve time | Average resolution time in ms |
| Slowest item | Item with longest resolution |
| Recipe timeouts | Recipe lookup timeouts |

Useful for performance investigations on large modpacks.

---

## 🧬 Raw TrackingData

```
/<modId> debug <player>
```

Dumps raw `TrackingData` JSON for a player — all value levels, total, and active profile.

---

## 📋 Datapack Diagnostics

```
/<modId> diagnostics
```

Runs a full datapack validation pass. See [Validation Reports](Validation-Reports).

---

## 🔎 Unassigned Sources

```
/<modId> get_unassigned
```

Outputs sources that could not be confidently classified:

```
config/<modId>/unassigned_sources.txt
```

Grouped by namespace for easy review.

---

## 📐 Schema Templates

```
/<modId> schema <type>
```

Prints a JSON schema template for a datapack format. Supported types:

```
values
source_classifications
effects
synergies
source_synergies
milestones
tracking_profiles
compat
```

---

## 🛠️ Maintenance Commands

| Command | Purpose |
|---|---|
| `/<modId> reload` | Reload all MarieLib data |
| `/<modId> invalidatecache` | Clear runtime resolver cache and re-run scanner |
| `/<modId> repair_generated_datapack` | Repair auto-generated value tag files |

---

## 📚 Related Pages

- [Classification Pipeline](Classification-Pipeline)
- [Validation Reports](Validation-Reports)
- [Commands Reference](Commands-Reference)
- [Server Administration](Server-Administration)
