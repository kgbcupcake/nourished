package dev.maire.nourished.config;

import dev.marie.framework.ui.PersistenceProvider;
import dev.marie.framework.ui.api.MarieStoredStyle;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.List;

/** The Nutritional Panel's own Diet Screen settings, kept in their own file apart from the dynamic screen's. */
public final class NutritionalPanelConfig {

    private static final double DEFAULT_BACKGROUND_OPACITY = 204.0d / 255.0d;

    private static NutritionalPanelConfig INSTANCE;
    private static ModConfigSpec SPEC;
    private static volatile ModConfig boundConfig;

    // Opacity now lives in the panel's stored style; this is only read once to carry the old value over.
    private final ModConfigSpec.DoubleValue backgroundOpacity;
    private final ModConfigSpec.BooleanValue styleMigrated;
    private final ModConfigSpec.BooleanValue barDragEnabled;
    private final ModConfigSpec.ConfigValue<List<? extends String>> barOrder;

    private NutritionalPanelConfig(ModConfigSpec.Builder builder) {
        builder.push("nutritionalPanel");
        backgroundOpacity = builder.defineInRange("backgroundOpacity", DEFAULT_BACKGROUND_OPACITY, 0.0d, 1.0d);
        styleMigrated = builder.define("styleMigrated", false);
        barDragEnabled = builder.define("barDragEnabled", true);
        barOrder = builder.defineListAllowEmpty("barOrder", List.of(), () -> "", o -> o instanceof String);
        builder.pop();
    }

    public static void register(ModContainer modContainer) {
        if (INSTANCE != null) {
            return;
        }
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        INSTANCE = new NutritionalPanelConfig(builder);
        SPEC = builder.build();
        modContainer.registerConfig(ModConfig.Type.CLIENT, SPEC, Nourished.MODID + "-nutritional-panel.toml");
    }

    public static void onModConfigLoading(ModConfigEvent.Loading event) {
        bindIfOurs(event.getConfig());
    }

    public static void onModConfigReloading(ModConfigEvent.Reloading event) {
        bindIfOurs(event.getConfig());
    }

    private static void bindIfOurs(ModConfig config) {
        if (config.getSpec() == SPEC) {
            boundConfig = config;
        }
    }

    public static NutritionalPanelConfig get() {
        if (INSTANCE == null) {
            throw new IllegalStateException("NutritionalPanelConfig has not been registered yet.");
        }
        return INSTANCE;
    }

    public static void saveNow() {
        var cfg = boundConfig;
        if (cfg != null && cfg.getLoadedConfig() != null) {
            cfg.getLoadedConfig().save();
        }
    }

    /** Copies this file's old opacity into the panel's stored style once, so earlier tweaks survive the move. */
    public void migrateStyleInto(PersistenceProvider store, String panelId) {
        if (styleMigrated.get()) {
            return;
        }
        MarieStoredStyle.setBackgroundOpacity(store, panelId, backgroundOpacity.get());
        styleMigrated.set(true);
        saveNow();
    }

    public boolean barDragEnabled() {
        return barDragEnabled.get();
    }

    public void setBarDragEnabled(boolean value) {
        barDragEnabled.set(value);
    }

    /** Saved order with unknown keys dropped and newly registered nutrients appended, same rules as the dynamic screen's. */
    public List<String> effectiveBarOrder() {
        List<String> registered = NutrientRegistry.getKeys();
        List<String> ordered = new ArrayList<>();
        for (String key : barOrder.get()) {
            if (registered.contains(key) && !ordered.contains(key)) {
                ordered.add(key);
            }
        }
        for (String key : registered) {
            if (!ordered.contains(key)) {
                ordered.add(key);
            }
        }
        return ordered;
    }

    public void setBarOrder(List<String> keys) {
        barOrder.set(new ArrayList<>(keys));
    }

    public void resetBarOrder() {
        barOrder.set(List.of());
    }
}
