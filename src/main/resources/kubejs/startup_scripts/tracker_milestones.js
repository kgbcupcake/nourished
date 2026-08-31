NourishedAPI.registerTrackerMilestone({
  id: 'nourished:activity/mining/tunnel_rat',
  trackerId: 'nourished:activity/mining_blocks',
  goal: 500.0,
  scope: 'current_period',
  rewardEffectId: 'minecraft:night_vision',
  rewardAmplifier: 0,
  rewardDuration: 600
})

NourishedAPI.registerTrackerMilestone({
  id: 'nourished:activity/combat/rampage',
  trackerId: 'nourished:activity/combat_kills',
  goal: 25.0,
  scope: 'current_period',
  rewardEffectId: 'minecraft:strength',
  rewardAmplifier: 0,
  rewardDuration: 300
})

NourishedAPI.registerTrackerMilestone({
  id: 'nourished:activity/sprint/cant_stop_me',
  trackerId: 'nourished:activity/sprint_distance',
  goal: 2000.0,
  scope: 'current_period',
  rewardEffectId: 'minecraft:speed',
  rewardAmplifier: 0,
  rewardDuration: 400
})

NourishedAPI.registerTrackerMilestone({
  id: 'nourished:activity/swim/aquatic_marathon',
  trackerId: 'nourished:activity/swim_distance',
  goal: 1000.0,
  scope: 'current_period',
  rewardEffectId: 'minecraft:dolphins_grace',
  rewardAmplifier: 0,
  rewardDuration: 400
})

onEvent('MarieEvents.trackerMilestoneTriggered', event => {
  console.log(`${event.playerId} hit milestone: ${event.milestoneId}`)
})
