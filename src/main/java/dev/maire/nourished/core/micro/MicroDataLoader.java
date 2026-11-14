package dev.maire.nourished.core.micro;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.maire.nourished.config.NourishedConfigPaths;
import dev.maire.nourished.core.Nourished;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Loads data/&lt;ns&gt;/nourished/micros/*.json and data/&lt;ns&gt;/nourished/micro_values/*.json. */
public final class MicroDataLoader extends SimplePreparableReloadListener<MicroDataLoader.Loaded> {

    record Loaded(List<MicroRegistry.Layer> layers, List<MicroEffect> packEffects, List<MicroEffect> configEffects) {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final FileToIdConverter DEFS = FileToIdConverter.json("nourished/micros");
    private static final FileToIdConverter VALUES = FileToIdConverter.json("nourished/micro_values");
    private static final FileToIdConverter EFFECTS = FileToIdConverter.json("nourished/micro_effects");
    private static final FileToIdConverter SOURCES = FileToIdConverter.json("nourished/micro_sources");
    private static final String CONFIG_TEMPLATE = """
            {
              "micros": {},
              "values": {},
              "effects": [],
              "sources": {}
            }
            """;

    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new MicroDataLoader());
    }

    @Override
    protected Loaded prepare(ResourceManager manager, ProfilerFiller profiler) {
        MicroRegistry.Layer bundled = MicroRegistry.Layer.empty();
        MicroRegistry.Layer packs = MicroRegistry.Layer.empty();
        for (var e : DEFS.listMatchingResources(manager).entrySet()) {
            ResourceLocation id = DEFS.fileToId(e.getKey());
            JsonObject obj = readJson(e.getKey(), e.getValue());
            if (obj == null) continue;
            String key = id.getPath().substring(id.getPath().lastIndexOf('/') + 1);
            try {
                layerFor(id, bundled, packs).defs().put(key, MicroDef.fromJson(key, obj));
            } catch (RuntimeException ex) {
                Nourished.LOGGER.warn("[Nourished] Skipping invalid nutrient {}: {}", e.getKey(), ex.getMessage());
            }
        }
        for (var e : VALUES.listMatchingResources(manager).entrySet()) {
            ResourceLocation id = VALUES.fileToId(e.getKey());
            JsonObject obj = readJson(e.getKey(), e.getValue());
            if (obj == null) continue;
            JsonElement values = obj.has("values") ? obj.get("values") : obj;
            if (values.isJsonObject()) {
                readValues(values.getAsJsonObject(), layerFor(id, bundled, packs));
            }
        }
        for (var e : SOURCES.listMatchingResources(manager).entrySet()) {
            JsonObject obj = readJson(e.getKey(), e.getValue());
            if (obj != null) readSources(obj, layerFor(SOURCES.fileToId(e.getKey()), bundled, packs));
        }
        List<MicroEffect> effects = new ArrayList<>();
        for (var e : EFFECTS.listMatchingResources(manager).entrySet()) {
            ResourceLocation id = EFFECTS.fileToId(e.getKey());
            JsonObject obj = readJson(e.getKey(), e.getValue());
            if (obj == null) continue;
            try {
                effects.add(MicroEffect.fromJson(id.toString(), obj, Nourished.MODID.equals(id.getNamespace())));
            } catch (RuntimeException ex) {
                Nourished.LOGGER.warn("[Nourished] Skipping invalid nutrient effect {}: {}", e.getKey(), ex.getMessage());
            }
        }
        List<MicroEffect> configEffects = new ArrayList<>();
        MicroRegistry.Layer config = readConfigLayer(configEffects);
        return new Loaded(List.of(bundled, config, packs), effects, configEffects);
    }

    @Override
    protected void apply(Loaded loaded, ResourceManager manager, ProfilerFiller profiler) {
        MicroRegistry.rebuild(loaded.layers());
        MicroEffects.set(loaded.packEffects(), loaded.configEffects());
        Nourished.LOGGER.info("[Nourished] Loaded {} nutrients, {} item and {} tag nutrient entries.",
                MicroRegistry.server().defs().size(), MicroRegistry.server().itemValues().size(),
                MicroRegistry.server().tagValues().size());
    }

    private static MicroRegistry.Layer layerFor(ResourceLocation id, MicroRegistry.Layer bundled, MicroRegistry.Layer packs) {
        return Nourished.MODID.equals(id.getNamespace()) ? bundled : packs;
    }

    private static JsonObject readJson(ResourceLocation file, Resource resource) {
        try (Reader reader = resource.openAsReader()) {
            return GSON.fromJson(reader, JsonObject.class);
        } catch (Exception ex) {
            Nourished.LOGGER.warn("[Nourished] Skipping malformed nutrient file {}: {}", file, ex.getMessage());
            return null;
        }
    }

    /** {@code {"protein": {"pin": ["minecraft:cooked_beef"], "hide": ["#c:foods/candy"]}}} */
    static void readSources(JsonObject obj, MicroRegistry.Layer layer) {
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            if (!entry.getValue().isJsonObject()) continue;
            JsonObject rule = entry.getValue().getAsJsonObject();
            layer.addSourceRule(entry.getKey(), new MicroSourceRule(strings(rule, "pin"), strings(rule, "hide")));
        }
    }

    private static List<String> strings(JsonObject obj, String key) {
        List<String> out = new ArrayList<>();
        if (obj.has(key) && obj.get(key).isJsonArray()) {
            for (JsonElement el : obj.getAsJsonArray(key)) {
                if (el.isJsonPrimitive()) out.add(el.getAsString());
            }
        }
        return out;
    }

    static void readValues(JsonObject obj, MicroRegistry.Layer layer) {
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            if (!entry.getValue().isJsonObject()) continue;
            String raw = entry.getKey();
            boolean tag = raw.startsWith("#");
            ResourceLocation id = ResourceLocation.tryParse(tag ? raw.substring(1) : raw);
            if (id == null) {
                Nourished.LOGGER.warn("[Nourished] Invalid nutrient value key '{}'", raw);
                continue;
            }
            Map<String, Float> values = new LinkedHashMap<>();
            for (var v : entry.getValue().getAsJsonObject().entrySet()) {
                if (v.getValue().isJsonPrimitive() && v.getValue().getAsJsonPrimitive().isNumber()) {
                    values.put(v.getKey(), v.getValue().getAsFloat());
                }
            }
            (tag ? layer.tagValues() : layer.itemValues()).put(id, values);
        }
    }

    /** Reads config/nourished/nutrition/micros.json (writing the empty template if it's missing). */
    static MicroRegistry.Layer readConfigLayer(List<MicroEffect> effects) {
        MicroRegistry.Layer layer = MicroRegistry.Layer.empty();
        Path path = NourishedConfigPaths.micros();
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path.getParent());
                Files.writeString(path, CONFIG_TEMPLATE, StandardCharsets.UTF_8);
                return layer;
            }
            JsonObject obj = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), JsonObject.class);
            if (obj == null) return layer;
            if (obj.has("micros") && obj.get("micros").isJsonObject()) {
                for (var e : obj.getAsJsonObject("micros").entrySet()) {
                    if (e.getValue().isJsonObject()) {
                        layer.defs().put(e.getKey(), MicroDef.fromJson(e.getKey(), e.getValue().getAsJsonObject()));
                    }
                }
            }
            if (obj.has("values") && obj.get("values").isJsonObject()) {
                readValues(obj.getAsJsonObject("values"), layer);
            }
            if (obj.has("sources") && obj.get("sources").isJsonObject()) {
                readSources(obj.getAsJsonObject("sources"), layer);
            }
            if (obj.has("effects") && obj.get("effects").isJsonArray()) {
                int i = 0;
                for (JsonElement el : obj.getAsJsonArray("effects")) {
                    i++;
                    if (!el.isJsonObject()) continue;
                    JsonObject eff = el.getAsJsonObject();
                    String id = eff.has("id") ? eff.get("id").getAsString() : "config_" + i;
                    try {
                        effects.add(MicroEffect.fromJson(id, eff, false));
                    } catch (RuntimeException ex) {
                        Nourished.LOGGER.warn("[Nourished] Skipping invalid nutrient effect '{}' in {}: {}", id, path, ex.getMessage());
                    }
                }
            }
        } catch (Exception ex) {
            Nourished.LOGGER.warn("[Nourished] Could not read {}: {}", path, ex.getMessage());
        }
        return layer;
    }
}
