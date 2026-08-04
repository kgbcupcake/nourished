package dev.maire.nourished.core.diet;

import com.mojang.serialization.Codec;

import dev.marie.framework.api.ApiStatus;
import dev.maire.nourished.core.Nourished;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Nourished-specific attachments not owned by MarieLib.
 */
@ApiStatus.Internal
public final class DietAttachment {

    private static final int RECENT_MEALS_LIMIT = 3;

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

    /**
     * Most-recent-first log of the last {@value #RECENT_MEALS_LIMIT} item ids the player actually
     * gained nutrition from, independent of MarieLib's {@code TrackingData} attachment so it has
     * its own persisted codec — see {@link #recordRecentMeal}.
     */
    public static final Supplier<AttachmentType<List<String>>> RECENT_MEALS =
            ATTACHMENT_TYPES.register("recent_meals", () ->
                    AttachmentType.<List<String>>builder(() -> List.of())
                            .serialize(Codec.STRING.listOf())
                            .copyOnDeath()
                            .build()
            );

    private DietAttachment() {}

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }

    public static List<String> getRecentMeals(Player player) {
        return player.getData(RECENT_MEALS.get());
    }

    /** Pushes {@code itemId} to the front of the recent-meals log, capped at {@value #RECENT_MEALS_LIMIT}. */
    public static void recordRecentMeal(Player player, String itemId) {
        List<String> current = player.getData(RECENT_MEALS.get());
        List<String> updated = new ArrayList<>(RECENT_MEALS_LIMIT);
        updated.add(itemId);
        for (String id : current) {
            if (updated.size() >= RECENT_MEALS_LIMIT) {
                break;
            }
            updated.add(id);
        }
        player.setData(RECENT_MEALS.get(), List.copyOf(updated));
    }
}
