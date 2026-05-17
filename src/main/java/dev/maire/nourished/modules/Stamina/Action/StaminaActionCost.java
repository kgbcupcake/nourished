package dev.maire.nourished.modules.Stamina.Action;

import dev.maire.nourished.api.ApiStatus;

@ApiStatus.Internal
public record StaminaActionCost(StaminaActionType type, float amount) {}
