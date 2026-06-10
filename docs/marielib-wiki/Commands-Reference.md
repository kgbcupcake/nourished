# Commands Reference

MarieLib registers commands under the **consuming mod's id**. When used by Nourished, all commands are under `/nourished`. When used by another Marie mod, substitute that mod's id.

Most administrative commands require **OP Level 2**.

---

## 👤 Player Commands

### Tracking Report

```
/<modId> report [player]
```

Displays a complete tracking report: current value levels, status (Critical / Low / OK / Excess), active profile, and total.

### Value Details

```
/<modId> value <key> [player]
```

Displays detailed information for a specific value: current level, decay rate, thresholds, and status classification.

---

## 🛠️ Administrative Commands

### Set Value

```
/<modId> set <key> <0-1> [player]
```

Sets a value level directly.

```
/<modId> set fruits 1.0
```

### Reset Values

```
/<modId> reset [player]
```

Resets all values to their starting fill level.

### Tracking Profiles

```
/<modId> profile list
/<modId> profile set <profile> [player]
/<modId> profile get [player]
```

### Reload

```
/<modId> reload
```

Reloads value definitions, source classifications, effects, datapack configuration, and compatibility data.

### Invalidate Cache

```
/<modId> invalidatecache
```

Clears the runtime source resolution cache and re-runs the scanner.

### Repair Generated Datapack

```
/<modId> repair_generated_datapack
```

Repairs auto-generated value tag files when classifications become corrupted.

---

## 📦 Datapack & Validation Tools

### Diagnostics

```
/<modId> diagnostics
```

Runs datapack validation. See [Validation Reports](Validation-Reports).

### Scan Analysis

```
/<modId> scan_analysis
```

Runs multi-value analysis across all loaded sources. Output: `config/<modId>/scanner_analysis/`

### Unassigned Sources

```
/<modId> get_unassigned
```

Outputs unclassified sources to `config/<modId>/unassigned_sources.txt`.

### Schema Template

```
/<modId> schema <type>
```

Supported types: `values`, `source_classifications`, `effects`, `synergies`, `source_synergies`, `milestones`, `tracking_profiles`, `compat`

---

## 🔍 Debug Commands

### Held Item Inspector

```
/<modId> debug held
```

Full classification trace for held item. See [Diagnostics & Tracing](Diagnostics-and-Tracing).

### Cache Statistics

```
/<modId> debug cache
```

### Raw TrackingData

```
/<modId> debug <player>
```

### NBT Paths

```
/<modId> nbt
```

Lists NBT paths for all registered value keys.

---

## 🔐 Permissions

| Command | Permission |
|---|---|
| `/<modId> report` (self) | Any player |
| `/<modId> value` (self) | Any player |
| All other commands | OP Level 2 |

---

## 📚 Related Pages

- [Diagnostics & Tracing](Diagnostics-and-Tracing)
- [Server Administration](Server-Administration)
- [Classification Pipeline](Classification-Pipeline)
