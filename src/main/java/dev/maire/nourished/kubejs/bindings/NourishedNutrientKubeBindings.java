package dev.maire.nourished.kubejs.bindings;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.marie.framework.api.ApiStatus;
import dev.maire.nourished.core.micro.MicroAttachment;
import dev.maire.nourished.core.micro.MicroDef;
import dev.maire.nourished.core.micro.MicroNetworking;
import dev.maire.nourished.core.micro.MicroProfileResolver;
import dev.maire.nourished.core.micro.MicroRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * KubeJS {@code NourishedNutrients} binding for the Omega-3/Fiber/... layer. Definitions and values
 * registered from startup or server scripts apply on the next datapack reload.
 */
@ApiStatus.Experimental
public final class NourishedNutrientKubeBindings {

    private NourishedNutrientKubeBindings() {}

    /** {@code register({id: 'iron', display_name: 'Iron', color: '#A0522D', group_weights: {proteins: 0.4}})} */
    public static void register(Map<String, Object> spec) {
        Object id = spec.get("id");
        if (id == null) {
            throw new IllegalArgumentException("NourishedNutrients.register: 'id' is required");
        }
        MicroRegistry.registerExternal(MicroDef.fromJson(String.valueOf(id), toJson(spec).getAsJsonObject()));
    }

    /** {@code setValues('#c:foods/raw_fish', {omega3: 1.0})} or an item id. */
    public static void setValues(String itemOrTag, Map<String, Object> values) {
        Map<String, Float> out = new LinkedHashMap<>();
        values.forEach((k, v) -> out.put(k, v instanceof Number n ? n.floatValue() : Float.parseFloat(String.valueOf(v))));
        MicroRegistry.registerExternalValues(itemOrTag, out);
    }

    /** Puts an item id or {@code #tag} first in a nutrient's Best sources list. */
    public static void pinSource(String key, String itemOrTag) {
        MicroRegistry.registerExternalSource(key, itemOrTag, false);
    }

    /** Keeps an item id or {@code #tag} out of a nutrient's Best sources list. */
    public static void hideSource(String key, String itemOrTag) {
        MicroRegistry.registerExternalSource(key, itemOrTag, true);
    }

    public static float getLevel(Player player, String key) {
        return MicroAttachment.level(player, key);
    }

    public static Map<String, Float> getLevels(Player player) {
        return MicroAttachment.get(player);
    }

    /** Sets a level (clamped 0..1) and syncs it; server side only. */
    public static void setLevel(Player player, String key, float value) {
        if (!(player instanceof ServerPlayer sp)) return;
        Map<String, Float> levels = new HashMap<>(MicroAttachment.get(sp));
        levels.put(key, Math.max(0f, Math.min(1f, value)));
        MicroAttachment.set(sp, levels);
        MicroNetworking.syncLevels(sp);
    }

    public static Map<String, Float> getProfile(Player player, ItemStack stack) {
        return MicroProfileResolver.resolve(stack, player.level());
    }

    public static List<String> getKeys() {
        return List.copyOf(MicroRegistry.server().defs().keySet());
    }

    private static JsonElement toJson(Object value) {
        if (value instanceof Map<?, ?> map) {
            JsonObject obj = new JsonObject();
            map.forEach((k, v) -> obj.add(String.valueOf(k), toJson(v)));
            return obj;
        }
        if (value instanceof List<?> list) {
            JsonArray arr = new JsonArray();
            list.forEach(v -> arr.add(toJson(v)));
            return arr;
        }
        if (value instanceof Number n) return new JsonPrimitive(n);
        if (value instanceof Boolean b) return new JsonPrimitive(b);
        return new JsonPrimitive(String.valueOf(value));
    }
}
