package dev.maire.nourished.client;

import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.diet.DietAttachment;
import dev.maire.nourished.core.diet.DietData;
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

        // registrar.playToClient( // STAMINA_SHELVED
        //         StaminaSyncPayload.TYPE, // STAMINA_SHELVED
        //         StaminaSyncPayload.STREAM_CODEC, // STAMINA_SHELVED
        //         ClientNetworkCallbacks::onStamina // STAMINA_SHELVED
        // ); // STAMINA_SHELVED
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
            ClientNourishedState.setConfig(payload);
        });
    }

    public static void onFullDiet(ModNetworking.SyncDietPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            DietData next = payload.diet();
            NourishedToastManager.onClientDietUpdated(next);
            ClientDietCache.set(next);
            ClientNourishedState.onFullDietSynced();
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                player.setData(DietAttachment.DIET.get(), next);
            }
        });
    }

    public static void onDietDelta(ModNetworking.SyncDietDeltaPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientDietCache.applyDelta(payload);
            NourishedToastManager.onClientDietUpdated(payload);
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

    // @SuppressWarnings("unused") // STAMINA_SHELVED
    // public static void onStamina(StaminaSyncPayload payload, IPayloadContext context) { // STAMINA_SHELVED
    //     context.enqueueWork(() -> { // STAMINA_SHELVED
    //         StaminaHUD.updateFromPayload(payload); // STAMINA_SHELVED
    //         LocalPlayer player = Minecraft.getInstance().player; // STAMINA_SHELVED
    //         if (player != null) { // STAMINA_SHELVED
    //             StaminaData stamina = StaminaData.fromSync( // STAMINA_SHELVED
    //                     payload.physicalStamina(), // STAMINA_SHELVED
    //                     payload.physicalMax(), // STAMINA_SHELVED
    //                     payload.physicalFatiguePenalty(), // STAMINA_SHELVED
    //                     payload.physicalBonusStamina(), // STAMINA_SHELVED
    //                     payload.physicalDebt(), // STAMINA_SHELVED
    //                     payload.mentalStamina(), // STAMINA_SHELVED
    //                     payload.mentalMax(), // STAMINA_SHELVED
    //                     payload.mentalFatiguePenalty(), // STAMINA_SHELVED
    //                     payload.mentalBonusStamina(), // STAMINA_SHELVED
    //                     payload.mentalDebt() // STAMINA_SHELVED
    //             ); // STAMINA_SHELVED
    //             player.setData(StaminaAttachment.STAMINA.get(), stamina); // STAMINA_SHELVED
    //         } // STAMINA_SHELVED
    //     }); // STAMINA_SHELVED
    // } // STAMINA_SHELVED
}
