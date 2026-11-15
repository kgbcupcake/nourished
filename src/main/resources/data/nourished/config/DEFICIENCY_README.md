# Diet Deficiency (`gameplay/deficiency.json`)

Food groups that stay low build up **deficiency pressure** over online play time. Pressure moves a
group through tiers; the worst group sets the player's overall tier. Creative and spectator players
never build pressure.

- Below `detect_threshold` pressure rises.
- At or above `recover_threshold` pressure falls, `recovery_speed` times as fast.
- In between, pressure holds, so one snack doesn't wipe a long deficiency.

Datapack override: `data/<namespace>/config/gameplay/deficiency.json` (same shape).

## Fields

| Field | Default | Meaning |
|---|---|---|
| `enabled` | `true` | Master switch. |
| `check_interval_ticks` | `80` | How often pressure updates (min 20). |
| `detect_threshold` | group's `critical_threshold` | Bar level below which pressure builds. |
| `recover_threshold` | group's `low_threshold` | Bar level at or above which pressure drains. |
| `group_thresholds` | `{}` | Per group: `{ "vegetables": { "detect_threshold": 0.1, "recover_threshold": 0.4 } }` |
| `recovery_speed` | `2.0` | Drain speed relative to build-up. `0` = never recovers on its own. |
| `exempt_groups` | `[]` | Groups that never cause deficiency. |
| `required_groups` | `[]` | If not empty, only these groups count. |
| `reset_on_death` | `false` | Clear all pressure on respawn. |
| `affect_peaceful` | `true` | Whether pressure builds on Peaceful. |
| `buffer_minutes` | `20` | Extra pressure past the last tier, so recovery from it takes time. |
| `notify_on_worsen` | `true` | Toast when a group reaches a higher tier (also needs `enableToasts`). |
| `notify_on_recover` | `true` | Toast when the overall tier drops. |
| `calories` | off | Optional calorie input, see below. |
| `tiers` | see below | Ordered by `after_minutes`. |

### Tiers

```json
{
  "after_minutes": 60,
  "activity_multiplier": 1.35,
  "regen_multiplier": 0.75,
  "saturation_multiplier": 0.85,
  "attributes": {
    "minecraft:generic.movement_speed": -0.05,
    "minecraft:player.block_break_speed": { "amount": -0.15, "operation": "add_multiplied_total" }
  },
  "effects": [ { "effect": "nourished:malnourished", "amplifier": 1, "duration_ticks": 200 } ]
}
```

- `after_minutes`: play time a group must stay deficient to reach this tier.
- `activity_multiplier`: scales nutrient drain from sprinting, swimming, mining, combat and starvation.
- `regen_multiplier`: scales vanilla food-driven natural regeneration only. Potions and other healing are untouched.
- `saturation_multiplier`: share of a food's saturation the player actually gets.
- `attributes`: any attribute id. A bare number is `add_multiplied_total` (-0.05 = -5%). Otherwise use
  `{ "amount", "operation" }` with `add_value`, `add_multiplied_base` or `add_multiplied_total`. These are
  applied directly, so milk doesn't remove them, and they're cleared in creative/spectator or when the tier ends.
- `effects`: refreshed while the tier holds. Never strips the same effect from other sources.
  `nourished:malnourished` only shows the level in the effect list. Remove it to hide the icon.

Defaults:

| Level | After | Drain | Regen | Saturation | Attributes |
|---|---|---|---|---|---|
| L1 | 20 min | x1.15 | -10% | – | – |
| L2 | 60 min | x1.35 | -25% | -15% | move -5%, mining -15% |
| L3 | 120 min | x1.6 | -50% | -30% | move -12%, mining -30%, attack -15% |

A `deficiency.json` written by an earlier build keeps its own tiers. Delete it, or add these fields, to get the new defaults.

Players see their status in the Nutritional Panel's **Deficiency** tab: a pressure bar per group with tier
marks, and the time to the next tier or to recovery. Tier colors are the `nourished:deficiency.*` color keys.

## effects.json trigger

`effects.json` rules can use `"trigger": "deficiency"`. `nutrient` is a group key or `"any"`, and
`threshold` is the minimum tier (1-3):

```json
{ "id": "veg_l2_hunger", "effect": "minecraft:hunger", "nutrient": "vegetables",
  "trigger": "deficiency", "threshold": 2, "amplifier": 0, "duration_ticks": 200 }
```

## Calories (optional)

```json
"calories": { "enabled": true, "min_fraction": 0.5, "recover_fraction": 0.8 }
```

When enabled, **Calories** becomes an extra deficiency row that uses the same tiers:

- It builds while yesterday's total was under `min_fraction` of the daily max **and** today's total is still under it.
- It recovers once today's (or yesterday's) total reaches `recover_fraction`.
- Needs `enableCalorieTracking` and `enableCalorieHistory`, because it reads yesterday's total.
- In commands and KubeJS the group key is `calories`.

## Commands

| Command | Who | Does |
|---|---|---|
| `/nourished deficiency` | anyone | Your overall level and each group's progress. |
| `/nourished deficiency get <player>` | op | Same, for another player. |
| `/nourished deficiency set <player> <group> <0-3>` | op | Put a group at the start of a level. |
| `/nourished deficiency set <player> <group> minutes <n>` | op | Set a group's accumulated minutes. |
| `/nourished deficiency clear <player> [group]` | op | Clear one group or all. |
| `/nourished deficiency reload` | op | Reload `deficiency.json` and resync clients. |

## KubeJS

```js
// steer or block natural tier changes (commands and API calls skip this)
NourishedEvents.deficiencyTierChanged(event => {
  if (event.group === 'dairy' && event.newTier > 1) event.newTier = 1
})

// scale build-up / recovery per check; delta is in ticks (+ builds, - recovers)
NourishedEvents.deficiencyPressure(event => {
  if (event.player.isInWater() && event.delta > 0) event.delta = 0
})

NourishedDeficiency.getTier(player, 'vegetables')   // 0-3, 'any' = worst group
NourishedDeficiency.getOverallTier(player)
NourishedDeficiency.getTiers(player)                // { vegetables: 2, ... }
NourishedDeficiency.getMinutes(player, 'vegetables')
NourishedDeficiency.setMinutes(player, 'vegetables', 30)
NourishedDeficiency.setTier(player, 'vegetables', 2)
NourishedDeficiency.clear(player, null)              // or a group
NourishedDeficiency.getActivityMultiplier(player)
```

## API

`dev.maire.nourished.api.NourishedDeficiencyAPI`: `getTier`, `getOverallTier`, `getPressure`,
`setPressure`, `setTier`, `clear`, `getActivityMultiplier`.
