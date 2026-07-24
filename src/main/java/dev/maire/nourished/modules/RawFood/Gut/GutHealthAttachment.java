package dev.maire.nourished.modules.RawFood.Gut;

import dev.marie.framework.api.ApiStatus;
import dev.maire.nourished.core.Nourished;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * NeoForge attachment registration for gut health data.
 */
@ApiStatus.Internal
public final class GutHealthAttachment {

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Nourished.MODID);

    public static final Supplier<AttachmentType<GutHealthData>> GUT =
            ATTACHMENT_TYPES.register("gut", () ->
                    AttachmentType.builder(GutHealthData::new)
                            .serialize(GutHealthData.CODEC)
                            .build()
            );

    private GutHealthAttachment() {}

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
