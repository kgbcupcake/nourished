package dev.maire.nourished.core.handler;

import com.klikli_dev.modonomicon.registry.DataComponentRegistry;
import com.klikli_dev.modonomicon.registry.ItemRegistry;
import dev.marie.framework.api.ApiStatus;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.diet.DietAttachment;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.List;
import java.util.Objects;

/**
 * Grants the Nourished guide book (a {@code modonomicon:modonomicon} item carrying the
 * {@code nourished:nourished_guide} book id component) once per player, tracked with a persistent
 * attachment (and skips duplicate grants if the book is already present in main, offhand, or ender
 * inventory).
 */
@ApiStatus.Internal
public final class NourishedGuideJoinHandler {

    private static final ResourceLocation NOURISHED_GUIDE_BOOK_ID =
            ResourceLocation.fromNamespaceAndPath(Nourished.MODID, "nourished_guide");

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        tryGrantStarterGuide(player);
    }

    private static void tryGrantStarterGuide(ServerPlayer player) {
        if (Boolean.TRUE.equals(player.getData(DietAttachment.RECEIVED_NOURISHED_GUIDE.get()))) {
            return;
        }

        if (playerAlreadyHasGuide(player)) {
            player.setData(DietAttachment.RECEIVED_NOURISHED_GUIDE.get(), Boolean.TRUE);
            return;
        }

        Item bookItem = ItemRegistry.MODONOMICON.get();
        ItemStack stack = new ItemStack(bookItem);
        stack.set(DataComponentRegistry.BOOK_ID.get(), NOURISHED_GUIDE_BOOK_ID);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        player.setData(DietAttachment.RECEIVED_NOURISHED_GUIDE.get(), Boolean.TRUE);
    }

    private static boolean playerAlreadyHasGuide(ServerPlayer player) {
        if (containsGuide(player.getInventory().items)) {
            return true;
        }
        if (isGuide(player.getOffhandItem())) {
            return true;
        }
        return enderChestContains(player);
    }

    private static boolean isGuide(ItemStack stack) {
        return stack.is(ItemRegistry.MODONOMICON.get())
                && Objects.equals(stack.get(DataComponentRegistry.BOOK_ID.get()), NOURISHED_GUIDE_BOOK_ID);
    }

    private static boolean enderChestContains(ServerPlayer player) {
        var inv = player.getEnderChestInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (isGuide(inv.getItem(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsGuide(List<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (isGuide(stack)) {
                return true;
            }
        }
        return false;
    }
}
