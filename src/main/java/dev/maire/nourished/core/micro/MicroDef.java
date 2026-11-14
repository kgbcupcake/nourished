package dev.maire.nourished.core.micro;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;

import java.util.LinkedHashMap;
import java.util.Map;

/** One tracked nutrient (Omega-3, Fiber, ...) that lives alongside the five food-group bars. */
public record MicroDef(
        String key,
        String displayName,
        int color,
        float decayRate,
        float lowThreshold,
        float highThreshold,
        String icon,
        Map<String, Float> groupWeights
) {

    public MicroDef {
        groupWeights = Map.copyOf(groupWeights);
    }

    public static MicroDef fromJson(String key, JsonObject obj) {
        Map<String, Float> groups = new LinkedHashMap<>();
        if (obj.has("group_weights") && obj.get("group_weights").isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : obj.getAsJsonObject("group_weights").entrySet()) {
                groups.put(e.getKey(), e.getValue().getAsFloat());
            }
        }
        return new MicroDef(
                key,
                obj.has("display_name") ? obj.get("display_name").getAsString() : key,
                obj.has("color") ? color(obj.get("color")) : 0xFFFFFF,
                obj.has("decay_rate") ? obj.get("decay_rate").getAsFloat() : 0.01f,
                obj.has("low_threshold") ? obj.get("low_threshold").getAsFloat() : 0.2f,
                obj.has("high_threshold") ? obj.get("high_threshold").getAsFloat() : 0.9f,
                obj.has("icon") ? obj.get("icon").getAsString() : "minecraft:apple",
                groups
        );
    }

    private static int color(JsonElement el) {
        if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isNumber()) {
            return el.getAsInt() & 0xFFFFFF;
        }
        return parseColor(el.getAsString());
    }

    static int parseColor(String raw) {
        String hex = raw.startsWith("#") ? raw.substring(1) : raw.startsWith("0x") ? raw.substring(2) : raw;
        try {
            return Integer.parseInt(hex, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return 0xFFFFFF;
        }
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(key);
        buf.writeUtf(displayName);
        buf.writeInt(color);
        buf.writeFloat(decayRate);
        buf.writeFloat(lowThreshold);
        buf.writeFloat(highThreshold);
        buf.writeUtf(icon);
        MicroCodecs.writeFloatMap(buf, groupWeights);
    }

    public static MicroDef read(FriendlyByteBuf buf) {
        return new MicroDef(buf.readUtf(), buf.readUtf(), buf.readInt(), buf.readFloat(), buf.readFloat(),
                buf.readFloat(), buf.readUtf(), MicroCodecs.readFloatMap(buf));
    }
}
