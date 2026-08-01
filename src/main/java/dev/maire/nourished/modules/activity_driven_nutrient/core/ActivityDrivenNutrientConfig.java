package dev.maire.nourished.modules.activity_driven_nutrient.core;

import dev.maire.nourished.core.Nourished;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server-synced settings for the activity-driven nutrient modules (sprint/swim/mining/combat/
 * starvation). Stored in {@code nourished-activity-driven-nutrient.toml}. Unlike {@link
 * dev.maire.nourished.config.NourishedConfig}, this is a real {@link ModConfig.Type#SERVER} spec:
 * values are unavailable on the client until synced from a joined/started server.
 */
public final class ActivityDrivenNutrientConfig {

    private static ActivityDrivenNutrientConfig INSTANCE;
    private static ModConfigSpec SPEC;
    private static volatile ModConfig boundConfig;

    private final ModConfigSpec.BooleanValue enabled;
    private final ModConfigSpec.BooleanValue sprintEnabled;
    private final ModConfigSpec.BooleanValue swimEnabled;
    private final ModConfigSpec.BooleanValue miningEnabled;
    private final ModConfigSpec.BooleanValue combatEnabled;
    private final ModConfigSpec.BooleanValue starvationEnabled;
    private final ModConfigSpec.DoubleValue miningCostPerBlock;
    private final ModConfigSpec.DoubleValue combatCostPerKill;
    private final ModConfigSpec.DoubleValue sprintDecayBoost;
    private final ModConfigSpec.DoubleValue swimDecayBoost;
    private final ModConfigSpec.DoubleValue starvationPenalty;

    private ActivityDrivenNutrientConfig(ModConfigSpec.Builder builder) {
        builder.push("activity_driven_nutrient");
        enabled = builder
                .comment("Master toggle for the activity-driven nutrient modules")
                .define("enabled", true);
        sprintEnabled = builder
                .comment("When true, sprinting drains/affects nutrients")
                .define("sprintEnabled", true);
        swimEnabled = builder
                .comment("When true, swimming drains/affects nutrients")
                .define("swimEnabled", true);
        miningEnabled = builder
                .comment("When true, mining drains/affects nutrients")
                .define("miningEnabled", true);
        combatEnabled = builder
                .comment("When true, combat drains/affects nutrients")
                .define("combatEnabled", true);
        starvationEnabled = builder
                .comment("When true, starvation drains/affects nutrients")
                .define("starvationEnabled", true);
        // Small placeholder magnitudes — mining fires once per block broken (very frequent) so its
        // per-fire cost is kept tiny; combat/starvation fire much less often so can afford a bigger
        // one-time bite. Same order of magnitude as RawFoodTierDef's -0.03..-0.08 one-time nutrient
        // penalties and NourishedConfig's default 0.001-per-decay-tick passive decay rate. Tune per
        // playtesting — these are conservative starting points, not balanced values.
        miningCostPerBlock = builder
                .comment("Nutrient cost (per bar) applied once per block broken.")
                .defineInRange("miningCostPerBlock", 0.0005d, 0.0d, 0.1d);
        combatCostPerKill = builder
                .comment("Nutrient cost (per bar) applied once per entity killed.")
                .defineInRange("combatCostPerKill", 0.01d, 0.0d, 0.5d);
        sprintDecayBoost = builder
                .comment("Extra nutrient subtraction (per bar) applied on top of passive decay each time the sprint tick-trigger fires while sprinting.")
                .defineInRange("sprintDecayBoost", 0.0004d, 0.0d, 0.1d);
        swimDecayBoost = builder
                .comment("Extra nutrient subtraction (per bar) applied on top of passive decay each time the swim tick-trigger fires while swimming.")
                .defineInRange("swimDecayBoost", 0.0004d, 0.0d, 0.1d);
        starvationPenalty = builder
                .comment("One-time nutrient penalty (per bar) applied when a nutrient crosses into critical.")
                .defineInRange("starvationPenalty", 0.02d, 0.0d, 0.5d);
        builder.pop();
    }

    public static void register(ModContainer modContainer) {
        if (INSTANCE != null) {
            return;
        }
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        INSTANCE = new ActivityDrivenNutrientConfig(builder);
        SPEC = builder.build();
        modContainer.registerConfig(ModConfig.Type.SERVER, SPEC, Nourished.MODID + "-activity-driven-nutrient.toml");
    }

    public static void onModConfigLoading(ModConfigEvent.Loading event) {
        bindIfOurs(event.getConfig());
    }

    public static void onModConfigReloading(ModConfigEvent.Reloading event) {
        bindIfOurs(event.getConfig());
    }

    private static void bindIfOurs(ModConfig config) {
        if (!Nourished.MODID.equals(config.getModId()) || config.getType() != ModConfig.Type.SERVER) {
            return;
        }
        if (config.getSpec() != SPEC) {
            return;
        }
        boundConfig = config;
    }

    public static ActivityDrivenNutrientConfig get() {
        if (INSTANCE == null) {
            throw new IllegalStateException("ActivityDrivenNutrientConfig has not been registered yet.");
        }
        return INSTANCE;
    }

    public static ModConfigSpec spec() {
        return SPEC;
    }

    /**
     * True once this SERVER-type config has been loaded/synced (integrated server bound, or synced
     * from a joined dedicated server). False before that — values still hold in-memory defaults and
     * must not be treated as real.
     */
    public static boolean isSynced() {
        return boundConfig != null;
    }

    public static void saveNow() {
        var cfg = boundConfig;
        if (cfg == null) {
            return;
        }
        var loaded = cfg.getLoadedConfig();
        if (loaded != null) {
            loaded.save();
        }
    }

    public boolean enabled() {
        return enabled.get();
    }

    public void setEnabled(boolean value) {
        enabled.set(value);
    }

    public boolean sprintEnabled() {
        return sprintEnabled.get();
    }

    public void setSprintEnabled(boolean value) {
        sprintEnabled.set(value);
    }

    public boolean swimEnabled() {
        return swimEnabled.get();
    }

    public void setSwimEnabled(boolean value) {
        swimEnabled.set(value);
    }

    public boolean miningEnabled() {
        return miningEnabled.get();
    }

    public void setMiningEnabled(boolean value) {
        miningEnabled.set(value);
    }

    public boolean combatEnabled() {
        return combatEnabled.get();
    }

    public void setCombatEnabled(boolean value) {
        combatEnabled.set(value);
    }

    public boolean starvationEnabled() {
        return starvationEnabled.get();
    }

    public void setStarvationEnabled(boolean value) {
        starvationEnabled.set(value);
    }

    public float miningCostPerBlock() {
        return miningCostPerBlock.get().floatValue();
    }

    public void setMiningCostPerBlock(double value) {
        miningCostPerBlock.set(value);
    }

    public float combatCostPerKill() {
        return combatCostPerKill.get().floatValue();
    }

    public void setCombatCostPerKill(double value) {
        combatCostPerKill.set(value);
    }

    public float sprintDecayBoost() {
        return sprintDecayBoost.get().floatValue();
    }

    public void setSprintDecayBoost(double value) {
        sprintDecayBoost.set(value);
    }

    public float swimDecayBoost() {
        return swimDecayBoost.get().floatValue();
    }

    public void setSwimDecayBoost(double value) {
        swimDecayBoost.set(value);
    }

    public float starvationPenalty() {
        return starvationPenalty.get().floatValue();
    }

    public void setStarvationPenalty(double value) {
        starvationPenalty.set(value);
    }
}
