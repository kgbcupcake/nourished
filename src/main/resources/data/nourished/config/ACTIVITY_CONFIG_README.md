# Nourished: Activity-Driven Nutrient Config

`activity_config.json` (under `config/nourished/modules/activity/`) controls the
sprint/swim/mining/combat/starvation activity-driven nutrient modules. Unlike most
Nourished config, these values are server-authoritative: a dedicated server's copy is
the real one, and it's synced down to connecting clients for display in Cloth Config.

## Schema

```json
{
  "enabled": true,
  "sprintEnabled": true,
  "swimEnabled": true,
  "miningEnabled": true,
  "combatEnabled": true,
  "starvationEnabled": true,
  "miningCostPerBlock": 0.0005,
  "combatCostPerKill": 0.01,
  "sprintDecayBoost": 0.0004,
  "swimDecayBoost": 0.0004,
  "starvationPenalty": 0.02,
  "colors": {
    "mining": "0xFF8B6F47",
    "combat": "0xFFCC3333",
    "sprint": "0xFF33CC66",
    "swim": "0xFF3399CC",
    "starvation": "0xFFCC8833"
  }
}
```

- `enabled`: master toggle for every activity-driven nutrient module
- `<module>Enabled`: per-module toggle (sprint/swim/mining/combat/starvation)
- `miningCostPerBlock` / `combatCostPerKill` / `sprintDecayBoost` / `swimDecayBoost` /
  `starvationPenalty`: nutrient cost (per bar) each module applies
- `colors`: ARGB hex color (`0xAARRGGBB` or `#RRGGBB`) used by the Activity Log HUD
  panel for each module's log lines

## Which file wins

1. Hardcoded Java defaults (fallback only)
2. `config/nourished/modules/activity/activity_config.json` on disk — edit this one
3. `data/nourished/config/modules/activity/activity_config.json` (datapack override)

## Notes

- Run `/nourished reload` to pick up changes without restarting
- Clients connected to a dedicated server see the server's synced values in Cloth
  Config, not their own local copy of this file
