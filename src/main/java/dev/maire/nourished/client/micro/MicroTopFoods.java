package dev.maire.nourished.client.micro;

import dev.maire.nourished.core.micro.MicroProfileResolver;
import dev.maire.nourished.core.micro.MicroRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Best foods per nutrient by gain per serving, computed once per synced registry snapshot. */
public final class MicroTopFoods {

    public record Entry(ItemStack stack, float amount) {}

    private static final int LIMIT = 8;
    private static MicroRegistry.Snapshot cachedFor;
    private static Map<String, List<Entry>> cached = Map.of();

    private MicroTopFoods() {}

    public static synchronized List<Entry> top(String key) {
        MicroRegistry.Snapshot snap = MicroRegistry.forSide(true);
        if (snap != cachedFor) {
            cached = compute();
            cachedFor = snap;
        }
        return cached.getOrDefault(key, List.of());
    }

    private static Map<String, List<Entry>> compute() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return Map.of();
        Map<String, List<Entry>> all = new HashMap<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack stack = new ItemStack(item);
            FoodProperties food = stack.getFoodProperties(null);
            if (food == null) continue;
            float bite = MicroProfileResolver.biteGain(food);
            MicroProfileResolver.resolve(stack, mc.level).forEach((k, v) ->
                    all.computeIfAbsent(k, x -> new ArrayList<>()).add(new Entry(stack, v * bite)));
        }
        Map<String, List<Entry>> out = new HashMap<>();
        all.forEach((k, list) -> {
            list.sort(Comparator.comparingDouble(Entry::amount).reversed());
            out.put(k, List.copyOf(list.subList(0, Math.min(LIMIT, list.size()))));
        });
        return out;
    }
}
