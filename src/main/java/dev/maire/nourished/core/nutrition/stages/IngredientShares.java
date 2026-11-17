package dev.maire.nourished.core.nutrition.stages;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

/** What one ingredient adds to an averaged classification, for trace output. */
final class IngredientShares {

    private IngredientShares() {}

    static Map<String, Float> share(@Nullable Map<String, Float> values, int divisor, Set<String> validKeys) {
        Map<String, Float> out = new LinkedHashMap<>();
        if (values == null || divisor <= 0) return out;
        values.forEach((k, v) -> {
            if (validKeys.contains(k) && v > 0f) out.put(k, v / divisor);
        });
        return out;
    }

    /** {@code " → +0.20 proteins, +0.10 grains"}, or empty when there's nothing to show. */
    static String describe(Map<String, Float> share) {
        if (share.isEmpty()) return "";
        StringJoiner joined = new StringJoiner(", ", " → ", "");
        share.entrySet().stream()
                .sorted(Map.Entry.<String, Float>comparingByValue().reversed())
                .forEach(e -> joined.add(String.format(Locale.ROOT, "+%.2f %s", e.getValue(), e.getKey())));
        return joined.toString();
    }
}
