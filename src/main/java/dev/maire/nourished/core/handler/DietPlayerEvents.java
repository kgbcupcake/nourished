package dev.maire.nourished.core.handler;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.config.ModuleCache;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.core.diet.DietAttachment;
import dev.maire.nourished.core.diet.DietData;
import dev.maire.nourished.core.effect.NutritionEffectApplier;
import dev.maire.nourished.core.network.ModNetworking;
import dev.maire.nourished.core.network.sync.NourishedSyncHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@ApiStatus.Internal
public class DietPlayerEvents {

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        DietData diet = player.getData(DietAttachment.DIET.get());
        diet.tick();
        player.setData(DietAttachment.DIET.get(), diet);
        NourishedSyncHandler.syncOnJoin(player);
        if (ModuleCache.enableEffects) {
            NutritionEffectApplier.apply(player, diet);
        }
        if (NourishedConfig.get().showJoinMessage()) {
            player.sendSystemMessage(
                    Component.literal("◆ ").withStyle(style -> style.withColor(0xF4C95D))
                            .append(Component.literal("NOURISHED").withStyle(style -> style.withColor(0x6FD3FF).withBold(true)))
                            .append(Component.literal(" ◆ ").withStyle(style -> style.withColor(0xF4C95D)))
                            .append(Component.literal(NourishedConfig.get().joinMessageLine1()).withStyle(style -> style.withColor(0xCFEFFF)))
            );
            String line2 = NourishedConfig.get().joinMessageLine2();
            int split = line2.indexOf(" - ");
            Component line2Body = split >= 0
                    ? Component.literal(line2.substring(0, split + 1))
                            .withStyle(style -> style.withColor(0xFF6B6B).withBold(true))
                            .append(Component.literal(line2.substring(split + 1)).withStyle(style -> style.withColor(0xFFC2C2)))
                    : Component.literal(line2).withStyle(style -> style.withColor(0xFFC2C2));
            player.sendSystemMessage(
                    Component.literal("⚠ ").withStyle(ChatFormatting.RED)
                            .append(line2Body)
            );
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        DietData diet = player.getData(DietAttachment.DIET.get());
        diet.tick();
        player.setData(DietAttachment.DIET.get(), diet);
        NourishedSyncHandler.syncOnJoin(player);
        if (ModuleCache.enableEffects) {
            NutritionEffectApplier.apply(player, diet);
        }
    }

    @SubscribeEvent
    public void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        DietData diet = player.getData(DietAttachment.DIET.get());
        ModNetworking.syncDietDelta(player, diet);
        if (ModuleCache.enableEffects) {
            NutritionEffectApplier.apply(player, diet);
        }
    }
}
