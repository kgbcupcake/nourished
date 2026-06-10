# Effects Reference

MarieLib provides a threshold effect system that consuming mods wire to their gameplay. Effects fire automatically when value levels cross configured thresholds.

---

## ⚖️ Threshold Types

Effects are defined via `ThresholdEffect` and registered through `MarieAPI.registerCustomEffect()` or datapacks (loader in progress).

| Type | When it fires |
|---|---|
| `CRITICAL` | Value drops below critical threshold |
| `LOW` | Value drops below low threshold |
| `EXCESS` | Value exceeds excess threshold |
| `BONUS` | Value is within bonus range (e.g. all values above threshold) |

---

## 📝 Datapack Format (schema defined)

Path: `data/<ns>/<modId>/effects/<id>.json`

```json
{
  "marie_schema_version": 1,
  "value_key": "proteins",
  "threshold": 0.2,
  "threshold_type": "LOW",
  "effect_id": "minecraft:weakness",
  "amplifier": 0,
  "duration": 300
}
```

| Field | Required | Description |
|---|---|---|
| `value_key` | ✅ | Which value triggers this effect |
| `threshold` | ✅ | Trigger point (0.0–1.0) |
| `threshold_type` | ✅ | `CRITICAL`, `LOW`, `EXCESS`, or `BONUS` |
| `effect_id` | ✅ | Vanilla or modded effect ID |
| `amplifier` | | Effect level (0 = level I) |
| `duration` | | Duration in ticks (20 ticks = 1 second) |

> The datapack loader for `effects/` is still in progress. Consuming mods may load effects from their own config files until the MarieLib loader is complete.

---

## ☕ Java API

```java
ThresholdEffect effect = ThresholdEffect.builder()
    .valueKey("proteins")
    .threshold(0.25f)
    .thresholdType(ThresholdEffect.ThresholdType.LOW)
    .effectId(ResourceLocation.parse("minecraft:mining_fatigue"))
    .amplifier(0)
    .duration(140)
    .build();

MarieAPI.registerCustomEffect(effect);
```

Consuming mods provide their own effect applier wired through `MarieLibContext`. MarieLib fires threshold crossings; the consuming mod applies potion effects.

---

## 📚 Related Pages

- [API Reference](API-Reference)
- [Datapack Support](Datapack-Support)
- [Configuration](Configuration)
