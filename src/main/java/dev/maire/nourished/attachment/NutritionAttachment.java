package dev.maire.nourished.attachment;

import dev.maire.nourished.Nourished;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class NutritionAttachment {

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Nourished.MODID);

    public static final Supplier<AttachmentType<NutritionData>> NUTRITION =
            ATTACHMENT_TYPES.register("nutrition", () ->
                    AttachmentType.builder(NutritionData::new)
                            .serialize(NutritionData.CODEC)
                            .build()
            );

    private NutritionAttachment() {}

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
