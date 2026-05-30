package dev.maire.nourished.config;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.util.NourishedJsonUtils;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

final class ConfigDefaultsLoader {

    private static final Gson GSON = new Gson();

    private ConfigDefaultsLoader() {}

    static JsonObject loadOrEmpty(String resourcePath) {
        try (InputStream in = ConfigDefaultsLoader.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                return new JsonObject();
            }
            try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                JsonObject obj = GSON.fromJson(reader, JsonObject.class);
                return obj != null ? obj : new JsonObject();
            }
        } catch (Exception ex) {
            Nourished.LOGGER.warn("[Nourished] Failed to load config defaults from {}", resourcePath, ex);
            return new JsonObject();
        }
    }

    static double getDouble(JsonObject obj, String key, double fallback) {
        return NourishedJsonUtils.getOptionalDouble(obj, key, fallback);
    }

    static int getInt(JsonObject obj, String key, int fallback) {
        return NourishedJsonUtils.getOptionalInt(obj, key, fallback);
    }

    static boolean getBoolean(JsonObject obj, String key, boolean fallback) {
        return NourishedJsonUtils.getOptionalBoolean(obj, key, fallback);
    }

    static String getString(JsonObject obj, String key, String fallback) {
        return NourishedJsonUtils.getOptionalString(obj, key, fallback);
    }
}
