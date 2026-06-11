package dev.maire.nourished.core.context;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.api.MemoryView;
import dev.marie.MariesLib.api.ValueModifierEvent;
import dev.marie.MariesLib.config.ModuleCache;
import dev.marie.MariesLib.core.MarieLibDataProvider;
import dev.marie.MariesLib.tracking.TrackingAttachment;
import dev.marie.MariesLib.tracking.TrackingData;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.effect.NutritionEffectApplier;
import dev.maire.nourished.core.network.ModNetworking;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;

@ApiStatus.Internal
final class NourishedPlayerDataProvider implements MarieLibDataProvider {

    static final NourishedPlayerDataProvider INSTANCE = new NourishedPlayerDataProvider();

    private static final ResourceLocation API_MODIFIER_SOURCE =
            ResourceLocation.fromNamespaceAndPath(Nourished.MODID, "api");

    @Override
    public float getTotal(Player player) {
        return TrackingAttachment.getTotal(player);
    }

    @Override
    public float getValueLevel(Player player, String valueKey) {
        return TrackingAttachment.getValueLevel(player, valueKey);
    }

    @Override
    public MemoryView getSourceMemoryView(Player player) {
        return TrackingAttachment.getSourceMemoryView(player);
    }

    @Override
    public void modifyValue(Player player, String valueKey, float delta) {
        ValueModifierEvent modifierEvent = new ValueModifierEvent(player, API_MODIFIER_SOURCE, valueKey, delta);
        NeoForge.EVENT_BUS.post(modifierEvent);
        if (modifierEvent.isCanceled()) {
            return;
        }
        TrackingData data = player.getData(TrackingAttachment.TRACKING.get());
        data.addValue(valueKey, modifierEvent.getAmount());
        player.setData(TrackingAttachment.TRACKING.get(), data);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        ModNetworking.syncDietDelta(serverPlayer, data);
        if (ModuleCache.enableEffects) {
            NutritionEffectApplier.apply(serverPlayer, data);
        }
    }
}
