package dev.maire.nourished.modules.Stamina.Action;

import dev.marie.MariesLib.api.ApiStatus;

@ApiStatus.Internal
public record StaminaActionCost(StaminaActionType type, float amount) {}
