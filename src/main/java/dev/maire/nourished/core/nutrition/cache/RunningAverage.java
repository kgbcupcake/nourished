package dev.maire.nourished.core.nutrition.cache;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe mutable holder that accumulates per-nutrient sums and produces an average map on demand.
 */
public final class RunningAverage {

    private volatile int count;
    private final ConcurrentHashMap<String, Float> sumPerNutrient = new ConcurrentHashMap<>();

    public void add(Map<String, Float> nutrients) {
        for (Map.Entry<String, Float> e : nutrients.entrySet()) {
            sumPerNutrient.merge(e.getKey(), e.getValue(), Float::sum);
        }
        count++;
    }

    public int count() { return count; }

    public Map<String, Float> average() {
        int c = count;
        if (c == 0) return Map.of();
        Map<String, Float> avg = new HashMap<>();
        for (Map.Entry<String, Float> e : sumPerNutrient.entrySet()) {
            avg.put(e.getKey(), e.getValue() / c);
        }
        return avg;
    }
}
