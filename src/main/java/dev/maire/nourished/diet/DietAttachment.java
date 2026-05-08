package dev.maire.nourished.diet;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

import dev.maire.nourished.nutrition.Nourished;

public final class DietAttachment {

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Nourished.MODID);

    public static final Supplier<AttachmentType<DietData>> DIET =
            ATTACHMENT_TYPES.register("diet", () ->
                    AttachmentType.builder(DietData::new)
                            .serialize(DietData.CODEC)
                            .build()
            );

    private DietAttachment() {}

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
