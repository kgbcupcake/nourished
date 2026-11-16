package dev.maire.nourished.config;

import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.micro.MicroNetworking;
import dev.maire.nourished.core.micro.MicroRegistry;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Settings for the nutrient (Omega-3, Fiber, ...) layer, kept apart from the main config. */
public final class MicroConfig {

    private static MicroConfig INSTANCE;
    private static ModConfigSpec SPEC;

    private final ModConfigSpec.BooleanValue enabled;
    private final ModConfigSpec.BooleanValue recipeDerivation;
    private final ModConfigSpec.DoubleValue recipeScale;
    private final ModConfigSpec.DoubleValue recipeMax;
    private final ModConfigSpec.DoubleValue gainScale;
    private final ModConfigSpec.IntValue decayIntervalTicks;
    private final ModConfigSpec.BooleanValue builtinEffects;

    private MicroConfig(ModConfigSpec.Builder builder) {
        builder.push("nutrients");
        enabled = builder
                .comment("Track nutrients (Omega-3, Fiber, ...) alongside the five food group bars.")
                .define("enabled", true);
        recipeDerivation = builder
                .comment("Work out a food's nutrients from its recipe ingredients when it has no explicit entry.")
                .define("recipeDerivation", true);
        recipeScale = builder
                .comment("Multiplier applied to the summed ingredient nutrients of a crafted or cooked food.")
                .defineInRange("recipeScale", 0.75d, 0.0d, 10.0d);
        recipeMax = builder
                .comment("Cap for any single nutrient on a recipe-derived food.")
                .defineInRange("recipeMax", 2.0d, 0.0d, 10.0d);
        gainScale = builder
                .comment("Multiplier on how much each bite fills the nutrient levels.")
                .defineInRange("gainScale", 1.0d, 0.0d, 10.0d);
        decayIntervalTicks = builder
                .comment("Ticks between nutrient decay steps; each step subtracts the nutrient's decay_rate.")
                .defineInRange("decayIntervalTicks", 1200, 20, 72000);
        builtinEffects = builder
                .comment("Turn on Nourished's bundled nutrient effects (e.g. low Omega-3 -> Mining Fatigue).",
                        "Effects added by other datapacks or micros.json always run.")
                .define("builtinEffects", false);
        builder.pop();
    }

    public static void register(ModContainer container) {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        INSTANCE = new MicroConfig(builder);
        SPEC = builder.build();
        container.registerConfig(ModConfig.Type.COMMON, SPEC, Nourished.MODID + "-nutrients.toml");
    }

    public static void onModConfigReloading(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SPEC) {
            MicroRegistry.refreshSettings();
            // clients show gains with the synced gainScale, so push the new settings out
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                server.execute(() -> server.getPlayerList().getPlayers().forEach(MicroNetworking::syncData));
            }
        }
    }

    public static MicroConfig get() {
        return INSTANCE;
    }

    private static boolean loaded() {
        return SPEC != null && SPEC.isLoaded();
    }

    public boolean enabled() {
        return !loaded() || enabled.get();
    }

    public boolean recipeDerivation() {
        return !loaded() || recipeDerivation.get();
    }

    public float recipeScale() {
        return loaded() ? recipeScale.get().floatValue() : 0.75f;
    }

    public float recipeMax() {
        return loaded() ? recipeMax.get().floatValue() : 2.0f;
    }

    public float gainScale() {
        return loaded() ? gainScale.get().floatValue() : 1.0f;
    }

    public boolean builtinEffects() {
        return loaded() && builtinEffects.get();
    }

    public int decayIntervalTicks() {
        return loaded() ? decayIntervalTicks.get() : 1200;
    }
}
