#Advancements  #Achievements

## Goal

> Advancement support and progression tracking tied to nutrition milestones and dietary goals.


## Completed (beta.1)
- [x] Milestone system wired (registerReloadListener fix)
- [x] Per-nutrient beginner/journeyman/master chains (5 groups × 3 tiers)
- [x] perfectly_balanced cross-group milestone
- [x] Tooltip "Counts toward: X beginner" integration
- [x] Advancement chain display in advancements tab
- [x] Cumulative intake tracking via MilestoneProgressAttachment

## beta.3 Scope
- [ ] Chained permanent passive buffs per tier (not just one-time effects)
- [ ] Streak milestones — eat X different foods in one in-game day
- [ ] Cross-group combo advancements (Carnivore, Herbivore, etc.)
- [ ] Prestige/reset track — master all groups, reset for unique reward
- [ ] Dynamic tooltip progress bar toward next milestone tier
- [ ] Secret/hidden advancements from unusual eating patterns
- [ ] KubeJS hook for player title/suffix based on milestone tier

## Acceptance Criteria
- [x] Nutrition milestones trigger correctly
- [x] Advancement conditions function reliably
- [x] Custom advancement support available
- [x] Testing confirms expected progression behavior
- [ ] beta.3 features implemented and tested