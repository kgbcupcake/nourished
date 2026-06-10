Welcome to the MarieLib wiki!

![MariesLib Banner](https://raw.githubusercontent.com/kgbcupcake/MarieLib/main/Assets/MariesLib_Banner.png)

> MarieLib is a NeoForge 1.21.1 shared library for Marie mods. It provides source classification, player value tracking, datapack tooling, compatibility discovery, diagnostics, and a stable public API — so consuming mods like [Nourished](https://modrinth.com/mod/nourished) can focus on gameplay.

MarieLib is **infrastructure, not a gameplay mod**. Install it alongside any Marie mod that depends on it (for example, Nourished requires **MarieLib 1.0.0+** as a separate jar).

---

## 📖 Wiki Pages

### For Server Owners & Modpack Makers

- [Getting Started](Getting-Started) — What MarieLib is and how to install it
- [Configuration](Configuration) — Module toggles, presets, import/export
- [Commands Reference](Commands-Reference) — Administrative and diagnostic commands
- [Server Administration](Server-Administration) — Reload, module locks, server setup
- [Multiplayer & Sync](Multiplayer-and-Sync) — Server-authoritative tracking sync

### For Datapack Authors

- [Datapack Support](Datapack-Support) — Source classifications, compat, families, module locks
- [Community Tags](Community-Tags) — `c:foods/*` tags and value tag conventions
- [Effects Reference](Effects-Reference) — Threshold effects and trigger types
- [Validation Reports](Validation-Reports) — Datapack diagnostics and validation output
- [Classification Pipeline](Classification-Pipeline) — How source classification works

### For Mod Developers

- [API Reference](API-Reference) — The `MarieAPI` public interface
- [KubeJS Scripting](KubeJS-Scripting) — Event hooks and scripting examples
- [Compatibility Guide](Compatibility-Guide) — Three-tier compat and auto-discovery
- [Diagnostics & Tracing](Diagnostics-and-Tracing) — Classification traces and debug tooling
- [HUD & Display](HUD-and-Display) — Framework HUD hooks and `ValueRenderer`

---

| Resource | Link |
|---|---|
| 📦 Modrinth | [modrinth.com/mod/marieslib](https://modrinth.com/mod/marieslib) |
| 💻 Source | [github.com/kgbcupcake/MarieLib](https://github.com/kgbcupcake/MarieLib) |
| 🍽️ Nourished | [Nourished wiki](https://github.com/kgbcupcake/nourished/wiki) |
| 🐛 Issues | [Report a bug](https://github.com/kgbcupcake/MarieLib/issues) |

---

## 🚧 Current Status

MarieLib is actively developed alongside consuming mods.

**Working now**

- ✔ Source scanner and auto-classification
- ✔ Classification traces and debug tooling
- ✔ Three-tier compat system with modpack overrides
- ✔ Player tracking (memory, decay, debt, streaks)
- ✔ Datapack loaders: `source_classifications`, `compat`, `source_families`, `module_locks`
- ✔ KubeJS scripting support
- ✔ JEI / REI / EMI tooltip integration
- ✔ Config import/export and presets

**In progress**

- 🔄 Datapack loaders: `values`, `effects`, `synergies`, `source_synergies`, `milestones`, `tracking_profiles`
- 🔄 Network sync infrastructure expansion
- 🔄 Additional validation and diagnostic tooling

---

_LGPL-3.0-only — Marie mods that depend on MarieLib must comply with the library license._
