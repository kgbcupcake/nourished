package dev.maire.nourished.client;

import dev.maire.nourished.core.Nourished;
import dev.marie.framework.client.config.state.MarieClientCache;
import dev.marie.framework.client.config.state.MarieClientState;
import dev.marie.framework.client.config.toast.MarieToastManager;
import dev.marie.framework.tracking.TrackingAttachment;
import dev.marie.framework.tracking.TrackingData;
import dev.maire.nourished.core.network.ModNetworking;
import dev.maire.nourished.core.network.sync.SyncNourishedConfigSnapshot;
import dev.maire.nourished.modules.RawFood.Gut.GutHealthAttachment;
import dev.maire.nourished.modules.RawFood.Gut.GutHealthData;
import dev.maire.nourished.modules.RawFood.Gut.GutHealthSyncPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = Nourished.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientNetworkCallbacks {

    private ClientNetworkCallbacks() {}

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToClient(
                ModNetworking.SyncDietPayload.TYPE,
                ModNetworking.SyncDietPayload.STREAM_CODEC,
                ClientNetworkCallbacks::onFullDiet
        );

        registrar.playToClient(
                ModNetworking.SyncDietDeltaPayload.TYPE,
                ModNetworking.SyncDietDeltaPayload.STREAM_CODEC,
                ClientNetworkCallbacks::onDietDelta
        );

        registrar.playToClient(
                SyncNourishedConfigSnapshot.TYPE,
                SyncNourishedConfigSnapshot.STREAM_CODEC,
                ClientNetworkCallbacks::onConfigSnapshot
        );

        registrar.playToClient(
                GutHealthSyncPayload.TYPE,
                GutHealthSyncPayload.STREAM_CODEC,
                ClientNetworkCallbacks::onGutHealth
        );
    }

    public static void onConfigSnapshot(SyncNourishedConfigSnapshot payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (payload.protocolVersion() != SyncNourishedConfigSnapshot.PROTOCOL_VERSION) {
                Nourished.LOGGER.warn(
                        "[Nourished] Ignoring config snapshot: protocol version mismatch (got {}, expected {})",
                        payload.protocolVersion(),
                        SyncNourishedConfigSnapshot.PROTOCOL_VERSION
                );
                return;
            }
            MarieClientState.setConfig(payload);
        });
    }

    public static void onFullDiet(ModNetworking.SyncDietPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            TrackingData next = payload.diet();
            MarieToastManager.onTrackingUpdated(next);
            MarieClientCache.onFullTrackingSync(next);
            MarieClientState.onFullTrackingSynced();
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                player.setData(TrackingAttachment.TRACKING.get(), next);
            }
        });
    }

    public static void onDietDelta(ModNetworking.SyncDietDeltaPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            TrackingData prev = MarieClientCache.get();
            TrackingData nextDiet = TrackingData.copySnapshot(prev);
            nextDiet.values.clear();
            nextDiet.values.putAll(payload.nutrients());
            nextDiet.lastValues.clear();
            nextDiet.lastValues.putAll(payload.lastNutrients());
            nextDiet.total = payload.calories();
            nextDiet.maxTotal = payload.maxCalories();
            nextDiet.sourceMemory.clear();
            nextDiet.sourceMemory.putAll(payload.foodMemory());
            nextDiet.categoryMemory.clear();
            nextDiet.categoryMemory.putAll(payload.categoryMemory());
            nextDiet.familyMemory.clear();
            nextDiet.familyMemory.putAll(payload.familyMemory());
            nextDiet.lastTickTime = payload.lastTickTime();

            MarieClientCache.onTrackingDelta(
                    nextDiet,
                    payload.recentFoodIds(),
                    payload.neglectedCategories(),
                    payload.fatiguedFamilies());
            MarieToastManager.onTrackingDelta(payload.nutrients());
        });
    }

    public static void onGutHealth(GutHealthSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                GutHealthData gut = GutHealthData.fromSync(payload.gutHealth(), payload.sensitivity());
                player.setData(GutHealthAttachment.GUT.get(), gut);
            }
        });
    }
}
