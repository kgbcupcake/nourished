package dev.maire.nourished.core.micro;

import dev.maire.nourished.config.MicroConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Nutrient definitions plus per-item/per-tag nutrient values. Server builds it from bundled data,
 * config/nourished/nutrition/micros.json, datapacks and the API (later layers win); clients get a
 * copy through {@link MicroNetworking}.
 */
public final class MicroRegistry {

    public record Settings(boolean enabled, boolean recipeDerivation, float recipeScale, float recipeMax) {
        public static Settings fromConfig() {
            MicroConfig c = MicroConfig.get();
            return new Settings(c.enabled(), c.recipeDerivation(), c.recipeScale(), c.recipeMax());
        }
    }

    public record Snapshot(
            Map<String, MicroDef> defs,
            Map<ResourceLocation, Map<String, Float>> itemValues,
            Map<ResourceLocation, Map<String, Float>> tagValues,
            Map<String, MicroSourceRule> sources,
            Settings settings
    ) {
        static final Snapshot EMPTY = new Snapshot(Map.of(), Map.of(), Map.of(), Map.of(), new Settings(true, true, 0.75f, 2f));

        public MicroSourceRule sourceRule(String key) {
            return sources.getOrDefault(key, MicroSourceRule.NONE);
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(defs.size());
            defs.values().forEach(d -> d.write(buf));
            writeValues(buf, itemValues);
            writeValues(buf, tagValues);
            buf.writeVarInt(sources.size());
            sources.forEach((k, r) -> {
                buf.writeUtf(k);
                r.write(buf);
            });
            buf.writeBoolean(settings.enabled());
            buf.writeBoolean(settings.recipeDerivation());
            buf.writeFloat(settings.recipeScale());
            buf.writeFloat(settings.recipeMax());
        }

        public static Snapshot read(FriendlyByteBuf buf) {
            int count = MicroCodecs.readSize(buf);
            Map<String, MicroDef> defs = new LinkedHashMap<>();
            for (int i = 0; i < count; i++) {
                MicroDef def = MicroDef.read(buf);
                defs.put(def.key(), def);
            }
            var items = readValues(buf);
            var tags = readValues(buf);
            int ruleCount = MicroCodecs.readSize(buf);
            Map<String, MicroSourceRule> sources = new LinkedHashMap<>();
            for (int i = 0; i < ruleCount; i++) {
                sources.put(buf.readUtf(), MicroSourceRule.read(buf));
            }
            Settings settings = new Settings(buf.readBoolean(), buf.readBoolean(), buf.readFloat(), buf.readFloat());
            return new Snapshot(defs, items, tags, sources, settings);
        }

        private static void writeValues(FriendlyByteBuf buf, Map<ResourceLocation, Map<String, Float>> values) {
            buf.writeVarInt(values.size());
            for (var e : values.entrySet()) {
                buf.writeResourceLocation(e.getKey());
                MicroCodecs.writeFloatMap(buf, e.getValue());
            }
        }

        private static Map<ResourceLocation, Map<String, Float>> readValues(FriendlyByteBuf buf) {
            int size = MicroCodecs.readSize(buf);
            Map<ResourceLocation, Map<String, Float>> out = new LinkedHashMap<>();
            for (int i = 0; i < size; i++) {
                out.put(buf.readResourceLocation(), MicroCodecs.readFloatMap(buf));
            }
            return out;
        }
    }

    /** One loaded layer: definitions plus item ids and tag ids mapped to nutrient values. */
    public record Layer(
            Map<String, MicroDef> defs,
            Map<ResourceLocation, Map<String, Float>> itemValues,
            Map<ResourceLocation, Map<String, Float>> tagValues,
            Map<String, MicroSourceRule> sources
    ) {
        public static Layer empty() {
            return new Layer(new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>());
        }

        public void addSourceRule(String key, MicroSourceRule rule) {
            sources.merge(key, rule, MicroSourceRule::plus);
        }
    }

    private static volatile Snapshot server = Snapshot.EMPTY;
    private static volatile Snapshot client = null;
    private static final Map<String, MicroDef> EXTERNAL = new LinkedHashMap<>();
    private static final Layer EXTERNAL_VALUES = Layer.empty();
    private static final int CONFIG_LAYER = 1;
    private static volatile List<Layer> lastLayers = List.of();

    private MicroRegistry() {}

    /** Merges layers in order (later wins per key) plus API-registered defs, and publishes the result. */
    static void rebuild(List<Layer> layers) {
        lastLayers = List.copyOf(layers);
        Map<String, MicroDef> defs = new LinkedHashMap<>();
        Map<ResourceLocation, Map<String, Float>> items = new LinkedHashMap<>();
        Map<ResourceLocation, Map<String, Float>> tags = new LinkedHashMap<>();
        Map<String, MicroSourceRule> sources = new LinkedHashMap<>();
        for (Layer layer : layers) {
            defs.putAll(layer.defs());
            layer.sources().forEach((k, r) -> sources.merge(k, r, MicroSourceRule::plus));
            mergeValues(items, layer.itemValues());
            mergeValues(tags, layer.tagValues());
        }
        synchronized (EXTERNAL) {
            defs.putAll(EXTERNAL);
            mergeValues(items, EXTERNAL_VALUES.itemValues());
            mergeValues(tags, EXTERNAL_VALUES.tagValues());
            EXTERNAL_VALUES.sources().forEach((k, r) -> sources.merge(k, r, MicroSourceRule::plus));
        }
        server = new Snapshot(Collections.unmodifiableMap(defs), Collections.unmodifiableMap(items),
                Collections.unmodifiableMap(tags), Collections.unmodifiableMap(sources), Settings.fromConfig());
        MicroProfileResolver.clearCache();
    }

    private static void mergeValues(Map<ResourceLocation, Map<String, Float>> into,
                                    Map<ResourceLocation, Map<String, Float>> layer) {
        for (var e : layer.entrySet()) {
            into.merge(e.getKey(), Map.copyOf(e.getValue()), (a, b) -> {
                Map<String, Float> merged = new LinkedHashMap<>(a);
                merged.putAll(b);
                return Map.copyOf(merged);
            });
        }
    }

    /** Swaps in a freshly read micros.json layer without a full datapack reload. */
    static void replaceConfigLayer(Layer config) {
        List<Layer> layers = new java.util.ArrayList<>(lastLayers);
        if (layers.size() <= CONFIG_LAYER) {
            layers.add(config);
        } else {
            layers.set(CONFIG_LAYER, config);
        }
        rebuild(layers);
    }

    /** API hook for addons; takes effect on the next datapack reload. */
    public static void registerExternal(MicroDef def) {
        synchronized (EXTERNAL) {
            EXTERNAL.put(def.key(), def);
        }
    }

    /** API hook: nutrient values for an item id or {@code #tag}; takes effect on the next datapack reload. */
    public static void registerExternalValues(String itemOrTag, Map<String, Float> values) {
        boolean tag = itemOrTag.startsWith("#");
        ResourceLocation id = ResourceLocation.tryParse(tag ? itemOrTag.substring(1) : itemOrTag);
        if (id == null) {
            throw new IllegalArgumentException("Invalid item or tag id '" + itemOrTag + "'");
        }
        synchronized (EXTERNAL) {
            (tag ? EXTERNAL_VALUES.tagValues() : EXTERNAL_VALUES.itemValues()).put(id, Map.copyOf(values));
        }
    }

    /** API hook: pin ({@code hide = false}) or hide an item/{@code #tag} in a nutrient's Best sources; next reload. */
    public static void registerExternalSource(String key, String itemOrTag, boolean hide) {
        MicroSourceRule rule = hide ? new MicroSourceRule(List.of(), List.of(itemOrTag)) : new MicroSourceRule(List.of(itemOrTag), List.of());
        synchronized (EXTERNAL) {
            EXTERNAL_VALUES.addSourceRule(key, rule);
        }
    }

    /** Re-reads the TOML-backed settings into the server snapshot, e.g. after a config edit. */
    public static void refreshSettings() {
        Snapshot s = server;
        server = new Snapshot(s.defs(), s.itemValues(), s.tagValues(), s.sources(), Settings.fromConfig());
        MicroProfileResolver.clearCache();
    }

    public static Snapshot server() {
        return server;
    }

    public static Snapshot forSide(boolean clientSide) {
        Snapshot c = client;
        return clientSide && c != null ? c : server;
    }

    public static void acceptClientSnapshot(Snapshot snapshot) {
        client = snapshot;
        MicroProfileResolver.clearCache();
    }

    public static void clearClientSnapshot() {
        client = null;
    }

    public static Collection<MicroDef> defs(boolean clientSide) {
        return forSide(clientSide).defs().values();
    }
}
