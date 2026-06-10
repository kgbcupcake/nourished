package dev.maire.nourished.modules.Stamina.Action;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.config.ModuleCache;
import dev.maire.nourished.modules.Stamina.Core.StaminaConfig;

@ApiStatus.Internal
public final class StaminaCostResolver {

    private StaminaCostResolver() {}

    /**
     * Returns the stamina cost for an action, or 0.0f if the action is disabled.
     */
    public static float resolve(StaminaActionType type) {
        return switch (type) {
            case SPRINT -> ModuleCache.enableStamina && StaminaConfig.enableSprint()
                    ? StaminaConfig.sprintCost() : 0f;
            case JUMP -> ModuleCache.enableStamina && StaminaConfig.enableJump()
                    ? StaminaConfig.jumpCost() : 0f;
            case ATTACK -> ModuleCache.enableStamina && StaminaConfig.enableAttack()
                    ? StaminaConfig.attackCost() : 0f;
            case MISSED_ATTACK -> ModuleCache.enableStamina && StaminaConfig.enableMissedAttack()
                    ? StaminaConfig.missedAttackCost() : 0f;
            case ELYTRA -> ModuleCache.enableStamina && StaminaConfig.enableElytra()
                    ? StaminaConfig.elytraCost() : 0f;
            case SWIM -> ModuleCache.enableStamina && StaminaConfig.enableSwim()
                    ? StaminaConfig.swimCost() : 0f;
            case CLIMB -> ModuleCache.enableStamina && StaminaConfig.enableClimb()
                    ? StaminaConfig.climbCost() : 0f;
            case TAKE_DAMAGE -> ModuleCache.enableStamina && StaminaConfig.enableTakeDamage()
                    ? StaminaConfig.takeDamageCost() : 0f;
            case MINE -> ModuleCache.enableStamina && StaminaConfig.enableMine()
                    ? StaminaConfig.mineCost() : 0f;
            case PLACE -> ModuleCache.enableStamina && StaminaConfig.enablePlace()
                    ? StaminaConfig.placeCost() : 0f;
            case FISH -> ModuleCache.enableStamina && StaminaConfig.enableFish()
                    ? StaminaConfig.fishCost() : 0f;
            case EAT -> ModuleCache.enableStamina && StaminaConfig.enableEat()
                    ? StaminaConfig.eatCost() : 0f;
            case EAT_RAW -> ModuleCache.enableStamina && StaminaConfig.enableRawEatPenalty()
                    ? StaminaConfig.eatCost() * StaminaConfig.rawEatCostMultiplier() : 0f;
            case USE_ITEM -> ModuleCache.enableStamina && StaminaConfig.enableUseItem()
                    ? StaminaConfig.useItemCost() : 0f;
        };
    }
}
