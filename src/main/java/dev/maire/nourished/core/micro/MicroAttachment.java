package dev.maire.nourished.core.micro;

import com.mojang.serialization.Codec;
import dev.maire.nourished.core.Nourished;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.Map;
import java.util.function.Supplier;

/** Per-player nutrient levels (0..1), stored apart from MariesLib's group-bar tracking data. */
public final class MicroAttachment {

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Nourished.MODID);

    public static final Supplier<AttachmentType<Map<String, Float>>> LEVELS =
            ATTACHMENT_TYPES.register("micro_levels", () ->
                    AttachmentType.<Map<String, Float>>builder(() -> Map.of())
                            .serialize(Codec.unboundedMap(Codec.STRING, Codec.FLOAT))
                            .copyOnDeath()
                            .build()
            );

    /** Ticks each tiered effect rule's condition has held, by rule id. Server-only. */
    public static final Supplier<AttachmentType<Map<String, Integer>>> HELD_TICKS =
            ATTACHMENT_TYPES.register("micro_held_ticks", () ->
                    AttachmentType.<Map<String, Integer>>builder(() -> Map.of())
                            .serialize(Codec.unboundedMap(Codec.STRING, Codec.INT))
                            .copyOnDeath()
                            .build()
            );

    private MicroAttachment() {}

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }

    public static Map<String, Float> get(Player player) {
        return player.getData(LEVELS.get());
    }

    public static float level(Player player, String key) {
        return get(player).getOrDefault(key, 0f);
    }

    public static void set(Player player, Map<String, Float> levels) {
        player.setData(LEVELS.get(), Map.copyOf(levels));
    }
}
