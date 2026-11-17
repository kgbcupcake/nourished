package dev.maire.nourished.client.micro;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Client-only memory of recent nutrient levels and meals, for the detail screen's trend and sources. */
public final class MicroClientHistory {

    public record Meal(ItemStack stack, Map<String, Float> profile, long gameTime) {}

    private static final int MAX_SAMPLES = 40;
    private static final int MAX_MEALS = 24;

    private static final Map<String, Deque<Float>> SAMPLES = new HashMap<>();
    private static final Deque<Meal> MEALS = new ArrayDeque<>();

    private MicroClientHistory() {}

    public static synchronized void recordLevels(Map<String, Float> levels) {
        for (var e : levels.entrySet()) {
            Deque<Float> ring = SAMPLES.computeIfAbsent(e.getKey(), k -> new ArrayDeque<>());
            Float last = ring.peekLast();
            if (last != null && Math.abs(last - e.getValue()) < 1.0e-4f) continue;
            ring.addLast(e.getValue());
            while (ring.size() > MAX_SAMPLES) ring.removeFirst();
        }
    }

    public static synchronized void recordMeal(ItemStack stack, Map<String, Float> profile, long gameTime) {
        if (profile.isEmpty()) return;
        MEALS.addFirst(new Meal(stack.copyWithCount(1), Map.copyOf(profile), gameTime));
        while (MEALS.size() > MAX_MEALS) MEALS.removeLast();
    }

    public static synchronized List<Float> samples(String key) {
        Deque<Float> ring = SAMPLES.get(key);
        return ring == null ? List.of() : new ArrayList<>(ring);
    }

    /** Newest first; only meals that gave {@code key}. */
    public static synchronized List<Meal> mealsWith(String key) {
        List<Meal> out = new ArrayList<>();
        for (Meal m : MEALS) {
            if (m.profile().getOrDefault(key, 0f) > 0f) out.add(m);
        }
        return out;
    }

    public static synchronized void clear() {
        SAMPLES.clear();
        MEALS.clear();
    }
}
