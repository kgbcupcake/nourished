package dev.maire.nourished.config;

import dev.maire.nourished.core.Nourished;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Client-side settings for the in-game nutrient HUD. */
public final class MicroClientConfig {

    public enum Corner { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

    private static MicroClientConfig INSTANCE;
    private static ModConfigSpec SPEC;

    private final ModConfigSpec.BooleanValue hudEnabled;
    private final ModConfigSpec.BooleanValue hudOnlyWhenLow;
    private final ModConfigSpec.EnumValue<Corner> hudCorner;
    private final ModConfigSpec.IntValue hudOffsetX;
    private final ModConfigSpec.IntValue hudOffsetY;
    private final ModConfigSpec.DoubleValue hudScale;

    private MicroClientConfig(ModConfigSpec.Builder builder) {
        builder.push("nutrientHud");
        hudEnabled = builder
                .comment("Show a small nutrient bar overlay in game. The 'Toggle Nutrient HUD' key flips this.")
                .define("enabled", false);
        hudOnlyWhenLow = builder
                .comment("Only show the overlay while at least one nutrient is low.")
                .define("onlyWhenLow", false);
        hudCorner = builder
                .comment("Screen corner the overlay is anchored to.")
                .defineEnum("corner", Corner.TOP_LEFT);
        hudOffsetX = builder.defineInRange("offsetX", 4, 0, 4000);
        hudOffsetY = builder.defineInRange("offsetY", 4, 0, 4000);
        hudScale = builder.defineInRange("scale", 1.0d, 0.25d, 4.0d);
        builder.pop();
    }

    public static void register(ModContainer container) {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        INSTANCE = new MicroClientConfig(builder);
        SPEC = builder.build();
        container.registerConfig(ModConfig.Type.CLIENT, SPEC, Nourished.MODID + "-nutrients-client.toml");
    }

    public static MicroClientConfig get() {
        return INSTANCE;
    }

    private static boolean loaded() {
        return SPEC != null && SPEC.isLoaded();
    }

    public boolean hudEnabled() {
        return loaded() && hudEnabled.get();
    }

    public void toggleHud() {
        if (!loaded()) return;
        hudEnabled.set(!hudEnabled.get());
        hudEnabled.save();
    }

    public boolean hudOnlyWhenLow() {
        return loaded() && hudOnlyWhenLow.get();
    }

    public Corner hudCorner() {
        return loaded() ? hudCorner.get() : Corner.TOP_LEFT;
    }

    public int hudOffsetX() {
        return loaded() ? hudOffsetX.get() : 4;
    }

    public int hudOffsetY() {
        return loaded() ? hudOffsetY.get() : 4;
    }

    public float hudScale() {
        return loaded() ? hudScale.get().floatValue() : 1f;
    }
}
