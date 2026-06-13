package dev.maire.nourished.core.handler;

import dev.marie.MariesLib.api.ApiStatus;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.diet.DietAttachment;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.List;

/**
 * Grants {@code nourished:nourished_guide} once per player, tracked with a persistent attachment
 * (and skips duplicate grants if the book is already present in main, offhand, or ender inventory).
 */
@ApiStatus.Internal
public final class NourishedGuideJoinHandler {

    private static final ResourceLocation NOURISHED_GUIDE_ID =
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

        if (!BuiltInRegistries.ITEM.containsKey(NOURISHED_GUIDE_ID)) {
            player.setData(DietAttachment.RECEIVED_NOURISHED_GUIDE.get(), Boolean.TRUE);
            return;
        }

        Item guideItem = BuiltInRegistries.ITEM.get(NOURISHED_GUIDE_ID);
        if (playerAlreadyHasGuide(player, guideItem)) {
            player.setData(DietAttachment.RECEIVED_NOURISHED_GUIDE.get(), Boolean.TRUE);
            return;
        }

        ItemStack stack = new ItemStack(guideItem);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        player.setData(DietAttachment.RECEIVED_NOURISHED_GUIDE.get(), Boolean.TRUE);
    }

    private static boolean playerAlreadyHasGuide(ServerPlayer player, Item guideItem) {
        if (containsGuide(player.getInventory().items, guideItem)) {
            return true;
        }
        if (player.getOffhandItem().is(guideItem)) {
            return true;
        }
        if (enderChestContains(player, guideItem)) {
            return true;
        }
        return false;
    }

    private static boolean enderChestContains(ServerPlayer player, Item guideItem) {
        var inv = player.getEnderChestInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(guideItem)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsGuide(List<ItemStack> stacks, Item guideItem) {
        for (ItemStack stack : stacks) {
            if (stack.is(guideItem)) {
                return true;
            }
        }
        return false;
    }
}
