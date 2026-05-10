package dev.maire.nourished.core.network;

import dev.maire.nourished.client.ClientDietCache;
import dev.maire.nourished.client.NourishedToastManager;
import dev.maire.nourished.core.diet.DietAttachment;
import dev.maire.nourished.core.diet.DietData;
import dev.maire.nourished.core.Nourished;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import io.netty.buffer.ByteBuf;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ModNetworking {

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        // Full sync — login, respawn, dimension change, commands only
        registrar.playToClient(
                SyncDietPayload.TYPE,
                SyncDietPayload.STREAM_CODEC,
                ModNetworking::handleSyncDiet
        );

        // Lightweight delta — every food eat and decay tick
        registrar.playToClient(
                SyncDietDeltaPayload.TYPE,
                SyncDietDeltaPayload.STREAM_CODEC,
                ModNetworking::handleSyncDietDelta
        );
    }

    /** Send lightweight display-only update. Call on every food eat and decay tick. */
    public static void syncDietDelta(ServerPlayer player, DietData diet) {
        PacketDistributor.sendToPlayer(player, diet.toDeltaPayload());
    }

    /** Send full DietData. Call on login, respawn, dimension change, command only. */
    public static void syncDiet(ServerPlayer player, DietData diet) {
        PacketDistributor.sendToPlayer(player, new SyncDietPayload(diet));
    }

    private static void handleSyncDiet(SyncDietPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            DietData next = payload.diet();
            NourishedToastManager.onClientDietUpdated(next);
            ClientDietCache.set(next);
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                player.setData(DietAttachment.DIET.get(), next);
            }
        });
    }

    private static void handleSyncDietDelta(SyncDietDeltaPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientDietCache.applyDelta(payload);
            NourishedToastManager.onClientDietUpdated(payload);
        });
    }

    // ── Full Sync Payload ────────────────────────────────────────────────────────

    public record SyncDietPayload(DietData diet) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<SyncDietPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Nourished.MODID, "sync_diet"));

        public static final StreamCodec<FriendlyByteBuf, SyncDietPayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.fromCodec(DietData.CODEC),
                        SyncDietPayload::diet,
                        SyncDietPayload::new
                );

        @Override
        public CustomPacketPayload.Type<SyncDietPayload> type() {
            return TYPE;
        }
    }

    // ── Lightweight Delta Payload ────────────────────────────────────────────────

    public record SyncDietDeltaPayload(
            Map<String, Float> nutrients,
            Map<String, Float> lastNutrients,
            float calories,
            float maxCalories,
            float balanceScore,
            List<String> recentFoodIds,
            List<String> neglectedCategories,
            List<String> fatiguedFamilies
    ) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<SyncDietDeltaPayload> TYPE =
                new CustomPacketPayload.Type<>(
                        ResourceLocation.fromNamespaceAndPath(Nourished.MODID, "sync_diet_delta"));

        private static final StreamCodec<FriendlyByteBuf, Map<String, Float>> NUTRIENT_MAP_CODEC =
                StreamCodec.of(
                        (buf, map) -> {
                            buf.writeVarInt(map.size());
                            map.forEach((k, v) -> {
                                buf.writeUtf(k);
                                buf.writeFloat(v);
                            });
                        },
                        buf -> {
                            int size = buf.readVarInt();
                            Map<String, Float> map = new LinkedHashMap<>(size);
                            for (int i = 0; i < size; i++) {
                                map.put(buf.readUtf(), buf.readFloat());
                            }
                            return map;
                        }
                );

        private static final StreamCodec<ByteBuf, List<String>> RECENT_FOOD_IDS_CODEC =
                ByteBufCodecs.<ByteBuf, String>list().apply(ByteBufCodecs.STRING_UTF8);
        private static final StreamCodec<ByteBuf, List<String>> NEGLECTED_CATEGORIES_CODEC =
                ByteBufCodecs.<ByteBuf, String>list().apply(ByteBufCodecs.STRING_UTF8);
        private static final StreamCodec<ByteBuf, List<String>> FATIGUED_FAMILIES_CODEC =
                ByteBufCodecs.<ByteBuf, String>list().apply(ByteBufCodecs.STRING_UTF8);

        public static final StreamCodec<FriendlyByteBuf, SyncDietDeltaPayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, payload) -> {
                            NUTRIENT_MAP_CODEC.encode(buf, payload.nutrients());
                            NUTRIENT_MAP_CODEC.encode(buf, payload.lastNutrients());
                            buf.writeFloat(payload.calories());
                            buf.writeFloat(payload.maxCalories());
                            buf.writeFloat(payload.balanceScore());
                            RECENT_FOOD_IDS_CODEC.encode(buf, payload.recentFoodIds());
                            NEGLECTED_CATEGORIES_CODEC.encode(buf, payload.neglectedCategories());
                            FATIGUED_FAMILIES_CODEC.encode(buf, payload.fatiguedFamilies());
                        },
                        buf -> new SyncDietDeltaPayload(
                                NUTRIENT_MAP_CODEC.decode(buf),
                                NUTRIENT_MAP_CODEC.decode(buf),
                                buf.readFloat(),
                                buf.readFloat(),
                                buf.readFloat(),
                                RECENT_FOOD_IDS_CODEC.decode(buf),
                                NEGLECTED_CATEGORIES_CODEC.decode(buf),
                                FATIGUED_FAMILIES_CODEC.decode(buf)
                        )
                );

        @Override
        public CustomPacketPayload.Type<SyncDietDeltaPayload> type() {
            return TYPE;
        }
    }
}
