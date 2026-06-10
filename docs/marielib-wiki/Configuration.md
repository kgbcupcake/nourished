# Configuration

MarieLib ships with sensible defaults. Consuming mods expose configuration through Cloth Config screens wired via `MarieLibContext`.

---

## 🎮 Opening the Configuration Menu

Configuration is provided by the **consuming mod**. For Nourished:

**Options → Mod Options → Nourished**

MarieLib itself has no standalone config screen.

---

## ⚙️ Module Toggles

Consuming mods can toggle MarieLib-powered features independently:

| Module | Description |
|---|---|
| Source application | Apply value gains when sources are consumed |
| Decay | Values drain over time |
| Effects | Threshold-triggered potion effects |
| HUD | On-screen value bars |
| Toasts | Critical value notifications |
| Memory | Recent source tracking and diminishing returns |

Module state is cached in `ModuleCache` for hot-path performance. Modpack authors can lock modules server-side via [module lock datapacks](Datapack-Support).

---

## 📦 Config Overrides

| File | Purpose |
|---|---|
| `config/<modId>/compat_overrides.json` | Override compat entries |
| `config/<modId>/source_overrides.json` | Override per-item value amounts |
| `config/<modId>/presets/` | Saved configuration presets |

---

## 📤 Import / Export

MarieLib supports compressed config share codes:

1. Export your entire setup as a share code string
2. Share the code with another server or player
3. Import instantly through the consuming mod's config UI

Presets ship as JSON under `config/<modId>/presets/`.

---

## 🔧 Scanner Configuration

Scanner behavior is configured by the consuming mod. Common settings include:

- Confidence spread threshold
- Recipe inheritance enable/disable
- Composite ratio threshold

Generated scanner data is stored in `config/<modId>/`.

---

## 💡 Tips

- After changing scanner settings, run `/<modId> invalidatecache`.
- Use module locks in datapacks for difficulty-locked modpacks.
- Check `config/<modId>/` for scanner output and debug traces.

---

## 📚 Related Pages

- [Datapack Support](Datapack-Support)
- [Server Administration](Server-Administration)
- [Commands Reference](Commands-Reference)
