package dev.maire.nourished.core.micro;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.maire.nourished.config.NourishedConfigPaths;
import dev.maire.nourished.core.Nourished;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reads and edits per-item nutrient values in config/nourished/nutrition/micros.json and applies them live. */
public final class MicroConfigFile {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private MicroConfigFile() {}

    /** The values micros.json sets for this exact item id, or null if it has no entry. */
    public static @Nullable Map<String, Float> itemEntry(ResourceLocation id) {
        JsonObject root;
        try {
            root = read();
        } catch (IOException ex) {
            return null;
        }
        JsonObject values = root.has("values") && root.get("values").isJsonObject() ? root.getAsJsonObject("values") : null;
        if (values == null || !values.has(id.toString()) || !values.get(id.toString()).isJsonObject()) {
            return null;
        }
        Map<String, Float> out = new LinkedHashMap<>();
        for (var e : values.getAsJsonObject(id.toString()).entrySet()) {
            if (e.getValue().isJsonPrimitive() && e.getValue().getAsJsonPrimitive().isNumber()) {
                out.put(e.getKey(), e.getValue().getAsFloat());
            }
        }
        return out;
    }

    /** Item ids (not tags) that micros.json gives explicit values. */
    public static List<String> itemIds() {
        try {
            JsonObject root = read();
            if (!root.has("values") || !root.get("values").isJsonObject()) return List.of();
            List<String> out = new ArrayList<>();
            for (String key : root.getAsJsonObject("values").keySet()) {
                if (!key.startsWith("#")) out.add(key);
            }
            return out;
        } catch (IOException ex) {
            return List.of();
        }
    }

    /**
     * Writes (or with null, removes) the item's entry, rebuilds the registry and resyncs everyone.
     * Call on the server thread.
     */
    public static void setItem(MinecraftServer server, ResourceLocation id, @Nullable Map<String, Float> values) throws IOException {
        JsonObject root = read();
        JsonObject all = root.has("values") && root.get("values").isJsonObject() ? root.getAsJsonObject("values") : new JsonObject();
        if (values == null) {
            all.remove(id.toString());
        } else {
            JsonObject entry = new JsonObject();
            values.forEach((k, v) -> {
                if (v > 0f) entry.addProperty(k, Math.round(v * 100f) / 100f);
            });
            all.add(id.toString(), entry);
        }
        root.add("values", all);
        Path path = NourishedConfigPaths.micros();
        Files.createDirectories(path.getParent());
        Files.writeString(path, GSON.toJson(root), StandardCharsets.UTF_8);
        reload(server);
    }

    /** Re-reads micros.json into the live registry and pushes it to every player. */
    public static void reload(MinecraftServer server) {
        List<MicroEffect> effects = new ArrayList<>();
        MicroRegistry.replaceConfigLayer(MicroDataLoader.readConfigLayer(effects));
        MicroEffects.setConfig(effects);
        server.getPlayerList().getPlayers().forEach(MicroNetworking::syncData);
    }

    /** Throws rather than returning an empty object for a broken file, so an edit never wipes it. */
    private static JsonObject read() throws IOException {
        Path path = NourishedConfigPaths.micros();
        if (Files.exists(path)) {
            JsonElement el;
            try {
                el = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), JsonElement.class);
            } catch (RuntimeException ex) {
                Nourished.LOGGER.warn("[Nourished] Could not read {}: {}", path, ex.getMessage());
                throw new IOException(path.getFileName() + " is not valid JSON: " + ex.getMessage());
            }
            if (el != null && el.isJsonObject()) return el.getAsJsonObject();
            if (el != null) throw new IOException(path.getFileName() + " must be a JSON object");
        }
        JsonObject empty = new JsonObject();
        empty.add("micros", new JsonObject());
        empty.add("values", new JsonObject());
        return empty;
    }
}
