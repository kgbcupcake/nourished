# Getting Started

MarieLib is the shared backbone behind Marie mods. It handles registries, source classification, player value tracking, datapack validation, and compatibility discovery so consuming mods can focus on gameplay.

---

## Requirements

| | |
|---|---|
| **Minecraft** | 1.21.1 |
| **Mod Loader** | NeoForge 21.1.x |
| **Java** | 21 |

---

## Installation

1. Download **MarieLib 1.0.0+** from [Modrinth](https://modrinth.com/mod/marieslib)
2. Place the jar in your `mods/` folder
3. Install the Marie mod you want (for example, [Nourished](https://modrinth.com/mod/nourished)) — it will declare MarieLib as a required dependency

Most launchers resolve the MarieLib dependency automatically. If a Marie mod fails to load, verify MarieLib is installed and up to date.

MarieLib has **no gameplay on its own**. You will not see HUD bars or nutrition mechanics until a consuming mod wires them up.

---

## What MarieLib Provides

| System | Description |
|---|---|
| **Scanner** | Auto-classifies edible items using tags, keywords, recipe inheritance, and confidence validation |
| **Tracking** | Player value bars with memory, decay, debt, streaks, and threshold effects |
| **Compat** | Three-tier compatibility registry with modpack overrides |
| **Datapacks** | Loaders for classifications, compat entries, source families, and module locks |
| **Commands** | Admin commands registered under the **consuming mod's id** (e.g. `/nourished` when used by Nourished) |
| **API** | Stable `MarieAPI` entry point for mod and addon developers |

---

## For Mod Developers

Consuming mods bootstrap MarieLib through `MarieLibContext` at mod initialization. There is no JarJar bundling — declare `marieslib` as a required dependency in `mods.toml` and wire your runtime through the context builder.

```java
// Minimal dependency pattern
dependencies {
    compileOnly "dev.marie.MariesLib:marieslib:1.0.0"
}
```

```toml
[[dependencies.yourmod]]
    modId = "marieslib"
    type = "required"
    versionRange = "[1.0.0,)"
    ordering = "AFTER"
    side = "BOTH"
```

All public API lives under `dev.marie.MariesLib.api`. See [API Reference](API-Reference) for the full contract.

---

## Reference Consumer: Nourished

[Nourished](https://modrinth.com/mod/nourished) is the primary reference implementation. It registers five nutrient value keys (`fruits`, `vegetables`, `proteins`, `grains`, `dairy`) and layers nutrition-specific HUD, effects, and compat on top of MarieLib.

Player-facing nutrition docs live on the [Nourished wiki](https://github.com/kgbcupcake/nourished/wiki).

---

## 📚 Related Pages

- [API Reference](API-Reference)
- [Compatibility Guide](Compatibility-Guide)
- [Datapack Support](Datapack-Support)
- [Commands Reference](Commands-Reference)
