package dev.maire.nourished.core.diet;

import com.mojang.serialization.Codec;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.api.FoodMemoryView;
import dev.maire.nourished.core.impl.DietDataFoodMemoryView;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.minecraft.world.entity.player.Player;

import java.util.function.Supplier;
import java.util.List;

import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.nutrition.NutrientRegistry;

@ApiStatus.Internal
public final class DietAttachment {
    private static final String DIET_ATTACHMENT_NBT_PREFIX = "neoforge:attachments." + Nourished.MODID + ":diet";

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Nourished.MODID);

    public static final Supplier<AttachmentType<DietData>> DIET =
            ATTACHMENT_TYPES.register("diet", () ->
                    AttachmentType.builder(DietData::new)
                            .serialize(DietData.CODEC)
                            .build()
            );

    /**
     * One-time grant of the Patchouli {@code nourished:nourished_guide} book on first server login
     * (replaces unreliable {@code give_on_first_join} on NeoForge 1.21.1 singleplayer).
     */
    public static final Supplier<AttachmentType<Boolean>> RECEIVED_NOURISHED_GUIDE =
            ATTACHMENT_TYPES.register("received_nourished_guide", () ->
                    AttachmentType.<Boolean>builder(() -> Boolean.FALSE)
                            .serialize(Codec.BOOL)
                            .copyOnDeath()
                            .build()
            );

    private DietAttachment() {}

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }

    public static String getNutrientNbtPath(String nutrientKey) {
        return DIET_ATTACHMENT_NBT_PREFIX + ".nutrients." + nutrientKey;
    }

    public static String getCaloriesNbtPath() {
        return DIET_ATTACHMENT_NBT_PREFIX + ".calories";
    }

    public static void logAllNutrientNbtPaths() {
        List<String> nutrientKeys = NutrientRegistry.getKeys();
        for (String nutrientKey : nutrientKeys) {
            Nourished.LOGGER.info("[Nourished] Nutrient NBT path: {}", getNutrientNbtPath(nutrientKey));
        }
    }

    public static float getCalories(Player player) {
        DietData diet = player.getData(DIET.get());
        return diet.calories;
    }

    public static float getNutrientLevel(Player player, String nutrientKey) {
        DietData diet = player.getData(DIET.get());
        Float value = diet.nutrients.get(nutrientKey);
        return value != null ? value : -1.0f;
    }

    public static FoodMemoryView getFoodMemoryView(Player player) {
        DietData diet = player.getData(DIET.get());
        return new DietDataFoodMemoryView(diet);
    }
}
