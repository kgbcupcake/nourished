package dev.maire.nourished.modules.Stamina.Action;

import dev.marie.MariesLib.api.ApiStatus;
import dev.maire.nourished.config.NourishedModuleCache;
import dev.maire.nourished.modules.Stamina.Core.StaminaConfig;

@ApiStatus.Internal
public final class StaminaCostResolver {

    private StaminaCostResolver() {}

    /**
     * Returns the stamina cost for an action, or 0.0f if the action is disabled.
     */
    public static float resolve(StaminaActionType type) {
        return switch (type) {
            case SPRINT -> NourishedModuleCache.enableStamina && StaminaConfig.enableSprint()
                    ? StaminaConfig.sprintCost() : 0f;
            case JUMP -> NourishedModuleCache.enableStamina && StaminaConfig.enableJump()
                    ? StaminaConfig.jumpCost() : 0f;
            case ATTACK -> NourishedModuleCache.enableStamina && StaminaConfig.enableAttack()
                    ? StaminaConfig.attackCost() : 0f;
            case MISSED_ATTACK -> NourishedModuleCache.enableStamina && StaminaConfig.enableMissedAttack()
                    ? StaminaConfig.missedAttackCost() : 0f;
            case ELYTRA -> NourishedModuleCache.enableStamina && StaminaConfig.enableElytra()
                    ? StaminaConfig.elytraCost() : 0f;
            case SWIM -> NourishedModuleCache.enableStamina && StaminaConfig.enableSwim()
                    ? StaminaConfig.swimCost() : 0f;
            case CLIMB -> NourishedModuleCache.enableStamina && StaminaConfig.enableClimb()
                    ? StaminaConfig.climbCost() : 0f;
            case TAKE_DAMAGE -> NourishedModuleCache.enableStamina && StaminaConfig.enableTakeDamage()
                    ? StaminaConfig.takeDamageCost() : 0f;
            case MINE -> NourishedModuleCache.enableStamina && StaminaConfig.enableMine()
                    ? StaminaConfig.mineCost() : 0f;
            case PLACE -> NourishedModuleCache.enableStamina && StaminaConfig.enablePlace()
                    ? StaminaConfig.placeCost() : 0f;
            case FISH -> NourishedModuleCache.enableStamina && StaminaConfig.enableFish()
                    ? StaminaConfig.fishCost() : 0f;
            case EAT -> NourishedModuleCache.enableStamina && StaminaConfig.enableEat()
                    ? StaminaConfig.eatCost() : 0f;
            case EAT_RAW -> NourishedModuleCache.enableStamina && StaminaConfig.enableRawEatPenalty()
                    ? StaminaConfig.eatCost() * StaminaConfig.rawEatCostMultiplier() : 0f;
            case USE_ITEM -> NourishedModuleCache.enableStamina && StaminaConfig.enableUseItem()
                    ? StaminaConfig.useItemCost() : 0f;
        };
    }
}
