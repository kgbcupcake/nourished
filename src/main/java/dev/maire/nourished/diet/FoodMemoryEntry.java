package dev.maire.nourished.diet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record FoodMemoryEntry(int eatCount, long lastEatenMs) {
    public static final Codec<FoodMemoryEntry> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.INT.fieldOf("eatCount").forGetter(FoodMemoryEntry::eatCount),
            Codec.LONG.fieldOf("lastEatenMs").forGetter(FoodMemoryEntry::lastEatenMs)
        ).apply(instance, FoodMemoryEntry::new)
    );

    public FoodMemoryEntry withEat() {
        return new FoodMemoryEntry(eatCount + 1, System.currentTimeMillis());
    }

    public boolean isExpired(long windowMs) {
        return System.currentTimeMillis() - lastEatenMs > windowMs;
    }
}
