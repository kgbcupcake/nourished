package dev.maire.nourished.core.network;

import dev.maire.nourished.core.network.sync.SyncNourishedConfigSnapshot;
import dev.maire.nourished.core.diet.DietData;
import dev.maire.nourished.core.diet.FoodMemoryEntry;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.modules.RawFood.Gut.GutHealthData;
import dev.maire.nourished.modules.RawFood.Gut.GutHealthSyncPayload;
import dev.maire.nourished.modules.Stamina.Core.StaminaData;
import dev.maire.nourished.modules.Stamina.Core.StaminaSyncPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import io.netty.buffer.ByteBuf;

import io.netty.handler.codec.DecoderException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ModNetworking {

    public static void register(RegisterPayloadHandlersEvent event) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            return;
        }

        PayloadRegistrar registrar = event.registrar("1");

        // Dedicated server: register outbound playToClient payloads with no-op handlers.
        // Client-side receive handlers are registered in ClientNetworkCallbacks.

        registrar.playToClient(
                SyncDietPayload.TYPE,
                SyncDietPayload.STREAM_CODEC,
                (payload, context) -> {}
        );

        registrar.playToClient(
                SyncDietDeltaPayload.TYPE,
                SyncDietDeltaPayload.STREAM_CODEC,
                (payload, context) -> {}
        );

        registrar.playToClient(
                SyncNourishedConfigSnapshot.TYPE,
                SyncNourishedConfigSnapshot.STREAM_CODEC,
                (payload, context) -> {}
        );

        registrar.playToClient(
                GutHealthSyncPayload.TYPE,
                GutHealthSyncPayload.STREAM_CODEC,
                (payload, context) -> {}
        );

        // registrar.playToClient( // STAMINA_SHELVED
        //         StaminaSyncPayload.TYPE, // STAMINA_SHELVED
        //         StaminaSyncPayload.STREAM_CODEC, // STAMINA_SHELVED
        //         (payload, context) -> {} // STAMINA_SHELVED
        // ); // STAMINA_SHELVED
    }

    /** Send lightweight client sync. Call on every food eat and decay tick. */
    public static void syncDietDelta(ServerPlayer player, DietData diet) {
        PacketDistributor.sendToPlayer(player, diet.toDeltaPayload());
    }

    /** Send full DietData. Call on login, respawn, dimension change, command only. */
    public static void syncDiet(ServerPlayer player, DietData diet) {
        PacketDistributor.sendToPlayer(player, new SyncDietPayload(diet));
    }

    /** Send gut health sync to client. Call on raw food eat, cooked food recovery, and gut tick. */
    public static void syncGutHealth(ServerPlayer player, GutHealthData gut) {
        PacketDistributor.sendToPlayer(player, new GutHealthSyncPayload(gut.getGutHealth(), gut.getSensitivity()));
    }

    /** Send stamina sync to client. Call when stamina state changes. */
    public static void syncStamina(ServerPlayer player, StaminaData data) {
        PacketDistributor.sendToPlayer(player, new StaminaSyncPayload(
                data.getPhysicalStamina(),
                data.getPhysicalMax(),
                data.getPhysicalFatiguePenalty(),
                data.getPhysicalBonusStamina(),
                data.getPhysicalDebt(),
                data.getMentalStamina(),
                data.getMentalMax(),
                data.getMentalFatiguePenalty(),
                data.getMentalBonusStamina(),
                data.getMentalDebt()
        ));
    }

    private static void encodeFoodMemoryMap(FriendlyByteBuf buf, Map<String, FoodMemoryEntry> map) {
        buf.writeVarInt(map.size());
        for (Map.Entry<String, FoodMemoryEntry> e : map.entrySet()) {
            buf.writeUtf(e.getKey());
            FoodMemoryEntry entry = e.getValue();
            buf.writeFloat(entry.eatCount());
            buf.writeLong(entry.lastEatenTick());
        }
    }

    private static LinkedHashMap<String, FoodMemoryEntry> decodeLinkedFoodMemoryMap(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        if (size > 256) throw new DecoderException("food memory map exceeds 256 entries: " + size);
        LinkedHashMap<String, FoodMemoryEntry> map = new LinkedHashMap<>(Math.max(16, size * 2));
        for (int i = 0; i < size; i++) {
            map.put(buf.readUtf(), new FoodMemoryEntry(buf.readFloat(), buf.readLong()));
        }
        return map;
    }

    private static HashMap<String, FoodMemoryEntry> decodeHashFoodMemoryMap(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        if (size > 256) throw new DecoderException("food memory map exceeds 256 entries: " + size);
        HashMap<String, FoodMemoryEntry> map = new HashMap<>(Math.max(16, size * 2));
        for (int i = 0; i < size; i++) {
            map.put(buf.readUtf(), new FoodMemoryEntry(buf.readFloat(), buf.readLong()));
        }
        return map;
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
            List<String> fatiguedFamilies,
            Map<String, FoodMemoryEntry> foodMemory,
            Map<String, FoodMemoryEntry> categoryMemory,
            Map<String, FoodMemoryEntry> familyMemory,
            long lastTickTime
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
                            if (size > 64) throw new DecoderException("nutrient map exceeds 64 entries: " + size);
                            Map<String, Float> map = new LinkedHashMap<>(size);
                            for (int i = 0; i < size; i++) {
                                map.put(buf.readUtf(), buf.readFloat());
                            }
                            return map;
                        }
                );

        private static final StreamCodec<ByteBuf, List<String>> RECENT_FOOD_IDS_CODEC =
                ByteBufCodecs.<ByteBuf, String>list(8).apply(ByteBufCodecs.STRING_UTF8);
        private static final StreamCodec<ByteBuf, List<String>> NEGLECTED_CATEGORIES_CODEC =
                ByteBufCodecs.<ByteBuf, String>list(8).apply(ByteBufCodecs.STRING_UTF8);
        private static final StreamCodec<ByteBuf, List<String>> FATIGUED_FAMILIES_CODEC =
                ByteBufCodecs.<ByteBuf, String>list(8).apply(ByteBufCodecs.STRING_UTF8);

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
                            ModNetworking.encodeFoodMemoryMap(buf, payload.foodMemory());
                            ModNetworking.encodeFoodMemoryMap(buf, payload.categoryMemory());
                            ModNetworking.encodeFoodMemoryMap(buf, payload.familyMemory());
                            buf.writeLong(payload.lastTickTime());
                        },
                        buf -> new SyncDietDeltaPayload(
                                NUTRIENT_MAP_CODEC.decode(buf),
                                NUTRIENT_MAP_CODEC.decode(buf),
                                buf.readFloat(),
                                buf.readFloat(),
                                buf.readFloat(),
                                RECENT_FOOD_IDS_CODEC.decode(buf),
                                NEGLECTED_CATEGORIES_CODEC.decode(buf),
                                FATIGUED_FAMILIES_CODEC.decode(buf),
                                ModNetworking.decodeLinkedFoodMemoryMap(buf),
                                ModNetworking.decodeHashFoodMemoryMap(buf),
                                ModNetworking.decodeHashFoodMemoryMap(buf),
                                buf.readLong()
                        )
                );

        @Override
        public CustomPacketPayload.Type<SyncDietDeltaPayload> type() {
            return TYPE;
        }
    }
}
