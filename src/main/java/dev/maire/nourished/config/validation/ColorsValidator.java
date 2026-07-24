package dev.maire.nourished.config.validation;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.marie.framework.api.ConfigValidator;
import dev.marie.framework.config.validation.Finding;
import dev.marie.framework.config.validation.ValidationResult;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Reads color keys from {@code config/nourished/colors.json} because {@link dev.marie.framework.color.ColorRegistry}
 * has no bulk key accessor ({@link dev.marie.framework.color.ColorRegistry#getArgb(String)} only).
 */
public final class ColorsValidator implements ConfigValidator {

    private static final Gson GSON = new Gson();
    private static final String FILE = "colors.json";

    @Override
    public String modId() {
        return Nourished.MODID;
    }

    @Override
    public String validatorId() {
        return "nourished_colors";
    }

    @Override
    public ValidationResult validate() {
        Set<String> validKeys = Set.copyOf(NutrientRegistry.getKeys());
        List<Finding> findings = new ArrayList<>();
        Path file = FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID).resolve(FILE);

        if (!Files.isRegularFile(file)) {
            return ValidationResult.pass(validatorId());
        }

        try (Reader reader = Files.newBufferedReader(file)) {
            JsonArray arr = GSON.fromJson(reader, JsonArray.class);
            if (arr == null || arr.isEmpty()) {
                return ValidationResult.pass(validatorId());
            }
            Set<String> seen = new HashSet<>();
            for (JsonElement el : arr) {
                if (!el.isJsonObject()) {
                    continue;
                }
                JsonObject obj = el.getAsJsonObject();
                if (!obj.has("key") || !obj.get("key").isJsonPrimitive()) {
                    continue;
                }
                String key = obj.get("key").getAsString();
                if (!seen.add(key)) {
                    continue;
                }
                if (!validKeys.contains(key)) {
                    findings.add(new Finding(
                            ValidationResult.Status.WARN,
                            FILE,
                            key,
                            "Color key is not a registered nutrient/value key (color will be inert)"
                    ));
                }
            }
        } catch (IOException e) {
            findings.add(new Finding(
                    ValidationResult.Status.WARN,
                    FILE,
                    null,
                    "Could not read colors.json for validation: " + e.getMessage()
            ));
        }

        return ValidationResults.fromFindings(validatorId(), findings);
    }
}
