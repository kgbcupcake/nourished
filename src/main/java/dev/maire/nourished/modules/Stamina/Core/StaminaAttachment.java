package dev.maire.nourished.modules.Stamina.Core;

import dev.marie.MariesLib.api.ApiStatus;
import dev.maire.nourished.core.Nourished;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * NeoForge attachment registration for stamina data.
 */
@ApiStatus.Internal
public final class StaminaAttachment {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Nourished.MODID);

    public static final Supplier<AttachmentType<StaminaData>> STAMINA =
            ATTACHMENT_TYPES.register("stamina", () ->
                    AttachmentType.builder(StaminaData::new)
                            .serialize(StaminaData.CODEC)
                            .build()
            );

    private StaminaAttachment() {}

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
