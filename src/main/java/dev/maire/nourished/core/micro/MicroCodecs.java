package dev.maire.nourished.core.micro;

import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;

import java.util.LinkedHashMap;
import java.util.Map;

final class MicroCodecs {

    private static final int MAX_ENTRIES = 4096;

    private MicroCodecs() {}

    static void writeFloatMap(FriendlyByteBuf buf, Map<String, Float> map) {
        buf.writeVarInt(map.size());
        for (Map.Entry<String, Float> e : map.entrySet()) {
            buf.writeUtf(e.getKey());
            buf.writeFloat(e.getValue());
        }
    }

    static Map<String, Float> readFloatMap(FriendlyByteBuf buf) {
        int size = readSize(buf);
        Map<String, Float> map = new LinkedHashMap<>(Math.max(4, size * 2));
        for (int i = 0; i < size; i++) {
            map.put(buf.readUtf(), buf.readFloat());
        }
        return map;
    }

    static int readSize(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        if (size < 0 || size > MAX_ENTRIES) {
            throw new DecoderException("micro map exceeds " + MAX_ENTRIES + " entries: " + size);
        }
        return size;
    }
}
