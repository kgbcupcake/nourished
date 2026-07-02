package dev.maire.nourished.core.diet;

import com.mojang.serialization.Codec;

import dev.marie.framework.api.ApiStatus;
import dev.maire.nourished.core.Nourished;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * Nourished-specific attachments not owned by MarieLib.
 */
@ApiStatus.Internal
public final class DietAttachment {

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Nourished.MODID);

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
}
