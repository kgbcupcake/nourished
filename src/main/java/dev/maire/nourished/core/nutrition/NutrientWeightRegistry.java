package dev.maire.nourished.core.nutrition;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.marie.MariesLib.api.ApiStatus;
import dev.maire.nourished.core.Nourished;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApiStatus.Internal
public final class NutrientWeightRegistry {

    private static final Gson GSON = new Gson();

    private static final Map<ResourceLocation, Map<String, Float>> WEIGHTS = new ConcurrentHashMap<>();

    private NutrientWeightRegistry() {}

    public static Map<String, Float> getWeights(ResourceLocation itemId) {
        return WEIGHTS.getOrDefault(itemId, Map.of());
    }

    public static void load() {
        WEIGHTS.clear();
        Nourished.LOGGER.info("[NutrientWeightRegistry] Cleared (no datapack loaded)");
    }

    public static void reload() {
        load();
    }

    public static void loadFromDatapack(ResourceManager resourceManager) {
        WEIGHTS.clear();
        Map<ResourceLocation, Resource> resources = resourceManager.listResources(
                "nourished/config/weights",
                rl -> rl.getPath().endsWith(".json"));

        int fileCount = 0;
        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            Resource resource = entry.getValue();
            try (Reader reader = resource.openAsReader()) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);
                if (root == null) {
                    Nourished.LOGGER.warn("[NutrientWeightRegistry] {} was empty, skipping", fileId);
                    continue;
                }
                for (Map.Entry<String, JsonElement> itemEntry : root.entrySet()) {
                    String key = itemEntry.getKey();
                    if (key == null) continue;
                    ResourceLocation itemId;
                    try {
                        itemId = ResourceLocation.parse(key);
                    } catch (Exception e) {
                        Nourished.LOGGER.warn("[NutrientWeightRegistry] Skipping malformed item key '{}' in {}", key, fileId);
                        continue;
                    }
                    if (!itemEntry.getValue().isJsonObject()) {
                        Nourished.LOGGER.warn("[NutrientWeightRegistry] Skipping entry for '{}' in {}: value is not a JSON object", key, fileId);
                        continue;
                    }
                    JsonObject weightsObj = itemEntry.getValue().getAsJsonObject();
                    Map<String, Float> nutrientWeights = WEIGHTS.computeIfAbsent(itemId, id -> new ConcurrentHashMap<>());
                    for (Map.Entry<String, JsonElement> weightEntry : weightsObj.entrySet()) {
                        try {
                            nutrientWeights.put(weightEntry.getKey(), weightEntry.getValue().getAsFloat());
                        } catch (Exception e) {
                            Nourished.LOGGER.warn("[NutrientWeightRegistry] Skipping malformed weight for '{}/{}' in {}",
                                    key, weightEntry.getKey(), fileId);
                        }
                    }
                }
                fileCount++;
            } catch (IOException e) {
                Nourished.LOGGER.warn("[NutrientWeightRegistry] Failed to read {}: {}", fileId, e.getMessage());
            }
        }
        Nourished.LOGGER.info("[NutrientWeightRegistry] Loaded {} entries from {} weight files", WEIGHTS.size(), fileCount);
    }
}
