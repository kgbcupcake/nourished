package dev.maire.nourished.modules.Stamina.Action;

import dev.marie.MariesLib.api.ApiStatus;

@ApiStatus.Internal
public enum StaminaActionType {
    // Physical
    SPRINT,
    JUMP,
    ATTACK,
    MISSED_ATTACK,
    ELYTRA,
    SWIM,
    CLIMB,
    TAKE_DAMAGE,

    // Mental
    MINE,
    PLACE,
    FISH,
    EAT,
    EAT_RAW,
    USE_ITEM;

    public boolean isPhysical() {
        return switch (this) {
            case SPRINT, JUMP, ATTACK, MISSED_ATTACK, ELYTRA, SWIM, CLIMB, TAKE_DAMAGE -> true;
            default -> false;
        };
    }

    public boolean isMental() {
        return !isPhysical();
    }
}
