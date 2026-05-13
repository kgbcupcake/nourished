package dev.maire.nourished.core.util;

import com.google.gson.JsonArray;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Map;

/**
 * Static input validation helpers. Every throwing method logs the violation at WARN before throwing.
 */
public final class NourishedValidation {

    private static final Logger LOGGER = LogUtils.getLogger();

    private NourishedValidation() {}

    public static float requireFinite(float v, float min, float max, String context) {
        if (!Float.isFinite(v) || v < min || v > max) {
            LOGGER.warn("[NourishedValidation] {} — invalid float value: {} (expected [{}, {}])", context, v, min, max);
            throw new IllegalArgumentException(context + ": invalid float value " + v);
        }
        return v;
    }

    public static double requireFiniteDouble(double v, double min, double max, String context) {
        if (!Double.isFinite(v) || v < min || v > max) {
            LOGGER.warn("[NourishedValidation] {} — invalid double value: {} (expected [{}, {}])", context, v, min, max);
            throw new IllegalArgumentException(context + ": invalid double value " + v);
        }
        return v;
    }

    public static ResourceLocation requireNonNullId(ResourceLocation id, String context) {
        if (id == null) {
            LOGGER.warn("[NourishedValidation] {} — ResourceLocation is null", context);
            throw new IllegalArgumentException(context + ": ResourceLocation must not be null");
        }
        return id;
    }

    public static void requireBoundedArray(JsonArray arr, int maxSize, String context) {
        if (arr.size() > maxSize) {
            LOGGER.warn("[NourishedValidation] {} — array size {} exceeds max {}", context, arr.size(), maxSize);
            throw new IllegalArgumentException(context + ": array size " + arr.size() + " exceeds max " + maxSize);
        }
    }

    public static void requireBoundedMap(Map<?, ?> map, int maxSize, String context) {
        if (map.size() > maxSize) {
            LOGGER.warn("[NourishedValidation] {} — map size {} exceeds max {}", context, map.size(), maxSize);
            throw new IllegalArgumentException(context + ": map size " + map.size() + " exceeds max " + maxSize);
        }
    }

    public static void requireBoundedString(String s, int maxLength, String context) {
        if (s != null && s.length() > maxLength) {
            LOGGER.warn("[NourishedValidation] {} — string length {} exceeds max {}", context, s.length(), maxLength);
            throw new IllegalArgumentException(context + ": string length " + s.length() + " exceeds max " + maxLength);
        }
    }

    /** Returns true if modId matches [a-z0-9_]{1,64}. */
    public static boolean sanitizeModId(String modId) {
        if (modId == null || modId.isEmpty() || modId.length() > 64) return false;
        for (int i = 0; i < modId.length(); i++) {
            char c = modId.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_')) return false;
        }
        return true;
    }

    public static void assertPathUnder(Path child, Path root, String context) {
        Path nc = child.normalize();
        Path nr = root.normalize();
        if (!nc.startsWith(nr)) {
            LOGGER.warn("[NourishedValidation] {} — path traversal: {} is not under {}", context, nc, nr);
            throw new IllegalArgumentException(context + ": path traversal detected: " + nc + " is not under " + nr);
        }
    }
}
