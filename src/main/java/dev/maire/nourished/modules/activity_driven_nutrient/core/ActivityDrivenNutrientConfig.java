package dev.maire.nourished.modules.activity_driven_nutrient.core;

import dev.marie.framework.network.SyncState;
import dev.marie.framework.resources.api.MarieResourcesAPI;

/**
 * Thin facade over {@link ActivityDrivenNutrientRegistry}, kept so
 * {@code ActivityModuleDispatcher}, {@code StarvationModule}, and the module classes don't need to
 * change their {@code ActivityDrivenNutrientConfig.get().xEnabled()/xCostPerY()} call sites.
 * Formerly a real {@link net.neoforged.neoforge.common.ModConfigSpec}-backed
 * {@code ModConfig.Type.SERVER} spec; now backed by the JSON registry and marie-resources's generic
 * config-sync mechanism.
 */
public final class ActivityDrivenNutrientConfig {

    private static final ActivityDrivenNutrientConfig INSTANCE = new ActivityDrivenNutrientConfig();

    private ActivityDrivenNutrientConfig() {}

    public static ActivityDrivenNutrientConfig get() {
        return INSTANCE;
    }

    /**
     * True once the client has received its first synced snapshot for this registry (or, on the
     * logical server itself, always true since values are loaded locally).
     */
    public static boolean isSynced() {
        return MarieResourcesAPI.getConfigSyncState(ActivityDrivenNutrientRegistry.SYNC_ID) == SyncState.ACTIVE;
    }

    public static void saveNow() {
        ActivityDrivenNutrientRegistry.save();
    }

    public boolean enabled() {
        return ActivityDrivenNutrientRegistry.enabled();
    }

    public void setEnabled(boolean value) {
        ActivityDrivenNutrientRegistry.setEnabled(value);
    }

    public boolean sprintEnabled() {
        return ActivityDrivenNutrientRegistry.sprintEnabled();
    }

    public void setSprintEnabled(boolean value) {
        ActivityDrivenNutrientRegistry.setSprintEnabled(value);
    }

    public boolean swimEnabled() {
        return ActivityDrivenNutrientRegistry.swimEnabled();
    }

    public void setSwimEnabled(boolean value) {
        ActivityDrivenNutrientRegistry.setSwimEnabled(value);
    }

    public boolean miningEnabled() {
        return ActivityDrivenNutrientRegistry.miningEnabled();
    }

    public void setMiningEnabled(boolean value) {
        ActivityDrivenNutrientRegistry.setMiningEnabled(value);
    }

    public boolean combatEnabled() {
        return ActivityDrivenNutrientRegistry.combatEnabled();
    }

    public void setCombatEnabled(boolean value) {
        ActivityDrivenNutrientRegistry.setCombatEnabled(value);
    }

    public boolean starvationEnabled() {
        return ActivityDrivenNutrientRegistry.starvationEnabled();
    }

    public void setStarvationEnabled(boolean value) {
        ActivityDrivenNutrientRegistry.setStarvationEnabled(value);
    }

    public float miningCostPerBlock() {
        return (float) ActivityDrivenNutrientRegistry.miningCostPerBlock();
    }

    public void setMiningCostPerBlock(double value) {
        ActivityDrivenNutrientRegistry.setMiningCostPerBlock(value);
    }

    public float combatCostPerKill() {
        return (float) ActivityDrivenNutrientRegistry.combatCostPerKill();
    }

    public void setCombatCostPerKill(double value) {
        ActivityDrivenNutrientRegistry.setCombatCostPerKill(value);
    }

    public float sprintDecayBoost() {
        return (float) ActivityDrivenNutrientRegistry.sprintDecayBoost();
    }

    public void setSprintDecayBoost(double value) {
        ActivityDrivenNutrientRegistry.setSprintDecayBoost(value);
    }

    public float swimDecayBoost() {
        return (float) ActivityDrivenNutrientRegistry.swimDecayBoost();
    }

    public void setSwimDecayBoost(double value) {
        ActivityDrivenNutrientRegistry.setSwimDecayBoost(value);
    }

    public float starvationPenalty() {
        return (float) ActivityDrivenNutrientRegistry.starvationPenalty();
    }

    public void setStarvationPenalty(double value) {
        ActivityDrivenNutrientRegistry.setStarvationPenalty(value);
    }
}
