package dev.maire.nourished.core.deficiency;

import com.mojang.serialization.Codec;
import dev.maire.nourished.core.Nourished;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.Map;
import java.util.function.Supplier;

/** Per-group deficiency pressure in ticks. Server-only state. */
public final class DeficiencyAttachment {

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Nourished.MODID);

    public static final Supplier<AttachmentType<Map<String, Integer>>> PRESSURE =
            ATTACHMENT_TYPES.register("deficiency_pressure", () ->
                    AttachmentType.<Map<String, Integer>>builder(() -> Map.of())
                            .serialize(Codec.unboundedMap(Codec.STRING, Codec.INT))
                            .copyOnDeath()
                            .build()
            );

    private DeficiencyAttachment() {}

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }

    public static Map<String, Integer> get(Player player) {
        return player.getData(PRESSURE.get());
    }

    public static void set(Player player, Map<String, Integer> pressure) {
        player.setData(PRESSURE.get(), Map.copyOf(pressure));
    }
}
