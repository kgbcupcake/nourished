// Nourished KubeJS Event Examples
// Place scripts in kubejs/server_scripts/

// ----------------------------------------------------------------
// nutrientChanged — fires whenever a nutrient value changes
// ----------------------------------------------------------------
NourishedEvents.nutrientChanged(event => {
    // event.player      — the ServerPlayer
    // event.nutrientKey — which nutrient changed (e.g. 'proteins')
    // event.oldValue    — previous value (0.0–1.0)
    // event.newValue    — new value (0.0–1.0)

    if (event.nutrientKey === 'proteins' && event.newValue < 0.25) {
        event.player.tell(Text.of('Proteins are running low!').red())
    }
})

// ----------------------------------------------------------------
// nutrientCritical — fires once when a nutrient drops below critical
// ----------------------------------------------------------------
NourishedEvents.nutrientCritical(event => {
    // event.player      — the ServerPlayer
    // event.nutrientKey — which nutrient went critical

    event.player.tell(Text.of('CRITICAL: ').red().bold()
    .append(Text.of(event.nutrientKey).gold().bold())
    .append(Text.of(' is critically low!').darkRed().bold()))
})

// ----------------------------------------------------------------
// foodEaten — fires after a player eats and gains nutrition
// ----------------------------------------------------------------
NourishedEvents.foodEaten(event => {
    // event.player      — the ServerPlayer
    // event.foodId      — item ID string (e.g. 'minecraft:apple')
    // event.nutrientKey — nutrient gained
    // event.amount      — how much was gained

    console.log(`${event.player.username} ate ${event.foodId} (+${event.amount} ${event.nutrientKey})`)
})

// ----------------------------------------------------------------
// nutrientModifier — fires BEFORE a nutrient gain is applied (cancellable)
// ----------------------------------------------------------------
NourishedEvents.nutrientModifier(event => {
    // event.player      — the ServerPlayer
    // event.foodId      — item ID string
    // event.nutrientKey — nutrient being applied
    // event.amount      — gain amount (read/write)
    // event.cancelled   — set true to block the gain entirely

    // Example: double vegetable gains
    if (event.nutrientKey === 'vegetables') {
        event.amount *= 2
    }

    // Example: block gains from a specific food
    // if (event.foodId === 'minecraft:poisonous_potato') {
    //     event.cancelled = true
    // }
})
