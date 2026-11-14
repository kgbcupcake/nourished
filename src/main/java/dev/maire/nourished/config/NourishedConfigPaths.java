package dev.maire.nourished.config;

import dev.maire.nourished.core.Nourished;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;

/**
 * Every file Nourished itself owns under config/nourished/. MariesLib writes the rest at fixed paths
 * (colors.json, scanner_spec.json, presets/, auto_compat/, scanner_analysis/, instance_tags/, tooltips/,
 * item_editor/Source Classification/, overrides/Overrides/excluded_items.json), so those stay put.
 */
public final class NourishedConfigPaths {

    private NourishedConfigPaths() {}

    public static Path root() {
        return FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID);
    }

    public static Path readme() {
        return root().resolve("Read_Me");
    }

    public static Path nutrition() {
        return root().resolve("nutrition");
    }

    public static Path gameplay() {
        return root().resolve("gameplay");
    }

    public static Path overrides() {
        return root().resolve("overrides");
    }

    public static Path modules() {
        return root().resolve("modules");
    }

    public static Path itemEditor() {
        return root().resolve("item_editor");
    }

    public static Path tooltips() {
        return root().resolve("tooltips");
    }

    public static Path generated() {
        return root().resolve("generated");
    }

    public static Path nutrients() {
        return nutrition().resolve("nutrients.json");
    }

    public static Path micros() {
        return nutrition().resolve("micros.json");
    }

    public static Path nutrientCurves() {
        return nutrition().resolve("nutrient_curves.json");
    }

    public static Path nutrientOverrides() {
        return nutrition().resolve("nutrient_overrides.json");
    }

    public static Path foodValues() {
        return nutrition().resolve("food_values.json");
    }

    public static Path effects() {
        return gameplay().resolve("effects.json");
    }

    public static Path locks() {
        return gameplay().resolve("locks.json");
    }

    public static Path foodOverrides() {
        return overrides().resolve("Overrides").resolve("food_overrides.json");
    }

    public static Path rawFood() {
        return modules().resolve("raw_food").resolve("raw_food.json");
    }

    public static Path activityConfig() {
        return modules().resolve("activity").resolve("activity_config.json");
    }

    public static Path tagAuditReport() {
        return generated().resolve("tag_audit_report.json");
    }

    public static Path exports() {
        return generated().resolve("exports");
    }

    public static Path nutrientsExport() {
        return generated().resolve("nutrients_export");
    }
}
