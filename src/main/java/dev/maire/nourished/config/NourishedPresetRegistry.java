package dev.maire.nourished.config;

import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.effect.EffectRegistry;
import dev.marie.MariesLib.config.PresetRegistry;
import dev.marie.MariesLib.data.DatapackSchema;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Nourished-specific preset hooks wired into {@link dev.marie.MariesLib.core.MarieLibContext}.
 */
public final class NourishedPresetRegistry {

    private NourishedPresetRegistry() {}

    public static void ensureBuiltInFilesOnDisk() {
        Path dir = PresetRegistry.presetsDirectory();
        try {
            Files.createDirectories(dir);
            for (String stem : PresetRegistry.BUILTIN_STEMS) {
                Path target = dir.resolve(stem + ".json");
                if (Files.exists(target)) {
                    continue;
                }
                String resource = "/data/" + Nourished.MODID + "/" + DatapackSchema.root() + "/presets/" + stem + ".json";
                try (InputStream in = Nourished.class.getResourceAsStream(resource)) {
                    if (in == null) {
                        Nourished.LOGGER.error("[PresetRegistry] Missing built-in resource {}", resource);
                        continue;
                    }
                    Files.copy(in, target);
                    Nourished.LOGGER.info("[PresetRegistry] Wrote built-in preset {}", target.getFileName());
                }
            }
        } catch (IOException e) {
            Nourished.LOGGER.error("[PresetRegistry] Failed to ensure built-in presets", e);
        }
    }

    public static void applyPresetValues(PresetRegistry.PresetValues values) {
        NourishedConfig config = NourishedConfig.get();
        config.setDecayRate(clamp(values.decayRate(), 0.0d, 1.0d));
        config.setCriticalThreshold(clamp(values.criticalThreshold(), 0.0d, 1.0d));
        config.setLowThreshold(clamp(values.lowThreshold(), 0.0d, 1.0d));
        config.setExcessThreshold(clamp(values.excessThreshold(), 0.0d, 1.0d));
        config.setDefaultEffectDurationTicks(clamp(values.defaultEffectDurationTicks(), 20, 72000));
        config.setEnableDecay(values.enableDecay());
        config.setEnableEffects(values.enableEffects());
        NourishedConfig.saveNow();
    }

    public static void enableAllEffects() {
        List<EffectRegistry.EffectDef> current = EffectRegistry.getAll();
        List<EffectRegistry.EffectDef> updated = new ArrayList<>(current.size());
        for (EffectRegistry.EffectDef def : current) {
            updated.add(new EffectRegistry.EffectDef(
                    def.id(),
                    def.effect(),
                    def.nutrient(),
                    def.trigger(),
                    def.threshold(),
                    def.amplifier(),
                    def.durationTicks(),
                    true,
                    def.thresholdMax(),
                    def.ambient(),
                    def.showParticles()
            ));
        }
        try {
            EffectRegistry.saveAll(updated);
        } catch (IOException e) {
            Nourished.LOGGER.warn("[PresetRegistry] Failed to force-enable all effects for Hardcore preset", e);
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.min(max, Math.max(min, value));
    }

    private static int clamp(int value, int min, int max) {
        return Math.min(max, Math.max(min, value));
    }
}
