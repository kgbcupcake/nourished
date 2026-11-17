package dev.maire.nourished.client.micro;

import dev.maire.nourished.core.micro.MicroProfileResolver;
import dev.maire.nourished.core.micro.MicroRegistry;
import dev.maire.nourished.core.micro.MicroSourceRule;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Best foods per nutrient by gain per serving, computed once per synced registry snapshot. Each
 * nutrient's {@link MicroSourceRule} pins entries to the front and hides others.
 */
public final class MicroTopFoods {

    public record Entry(ItemStack stack, float amount) {}

    private static final int LIMIT = 8;
    private static MicroRegistry.Snapshot cachedFor;
    private static Map<String, List<Entry>> cached = Map.of();

    private MicroTopFoods() {}

    public static synchronized List<Entry> top(String key) {
        MicroRegistry.Snapshot snap = MicroRegistry.forSide(true);
        if (snap != cachedFor) {
            cached = compute(snap);
            cachedFor = snap;
        }
        return cached.getOrDefault(key, List.of());
    }

    private static Map<String, List<Entry>> compute(MicroRegistry.Snapshot snap) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return Map.of();
        Map<String, Map<Item, Float>> amounts = new HashMap<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack stack = new ItemStack(item);
            FoodProperties food = stack.getFoodProperties(null);
            if (food == null) continue;
            float bite = MicroProfileResolver.biteGain(food, true);
            MicroProfileResolver.resolve(stack, mc.level).forEach((k, v) ->
                    amounts.computeIfAbsent(k, x -> new HashMap<>()).put(item, v * bite));
        }
        Map<String, List<Entry>> out = new HashMap<>();
        for (String key : snap.defs().keySet()) {
            out.put(key, ranked(amounts.getOrDefault(key, Map.of()), snap.sourceRule(key)));
        }
        return out;
    }

    private static List<Entry> ranked(Map<Item, Float> amounts, MicroSourceRule rule) {
        Map<Item, Float> picked = new LinkedHashMap<>();
        for (String pin : rule.pin()) {
            List<Item> items = new ArrayList<>(resolve(pin));
            items.sort(Comparator.comparingDouble((Item i) -> amounts.getOrDefault(i, 0f)).reversed());
            for (Item item : items) {
                if (!hidden(item, rule)) picked.putIfAbsent(item, amounts.getOrDefault(item, 0f));
            }
        }
        amounts.entrySet().stream()
                .sorted(Map.Entry.<Item, Float>comparingByValue().reversed())
                .filter(e -> !hidden(e.getKey(), rule))
                .forEach(e -> picked.putIfAbsent(e.getKey(), e.getValue()));
        List<Entry> list = new ArrayList<>();
        for (var e : picked.entrySet()) {
            if (list.size() >= LIMIT) break;
            list.add(new Entry(new ItemStack(e.getKey()), e.getValue()));
        }
        return List.copyOf(list);
    }

    private static boolean hidden(Item item, MicroSourceRule rule) {
        for (String h : rule.hide()) {
            if (matches(item, h)) return true;
        }
        return false;
    }

    private static boolean matches(Item item, String entry) {
        if (entry.startsWith("#")) {
            ResourceLocation tag = ResourceLocation.tryParse(entry.substring(1));
            return tag != null && item.builtInRegistryHolder().is(TagKey.create(Registries.ITEM, tag));
        }
        ResourceLocation id = ResourceLocation.tryParse(entry);
        return id != null && id.equals(BuiltInRegistries.ITEM.getKey(item));
    }

    private static List<Item> resolve(String entry) {
        if (entry.startsWith("#")) {
            ResourceLocation tag = ResourceLocation.tryParse(entry.substring(1));
            if (tag == null) return List.of();
            List<Item> out = new ArrayList<>();
            BuiltInRegistries.ITEM.getTagOrEmpty(TagKey.create(Registries.ITEM, tag)).forEach(h -> out.add(h.value()));
            return out;
        }
        ResourceLocation id = ResourceLocation.tryParse(entry);
        Item item = id != null ? BuiltInRegistries.ITEM.get(id) : Items.AIR;
        return item == Items.AIR ? List.of() : List.of(item);
    }
}
