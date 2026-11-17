package dev.maire.nourished.core.micro;

import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * Modpack control over one nutrient's "Best sources" list: {@code pin} entries (item ids or
 * {@code #tags}) go first in the given order, {@code hide} entries never show. Hide wins over pin.
 */
public record MicroSourceRule(List<String> pin, List<String> hide) {

    public static final MicroSourceRule NONE = new MicroSourceRule(List.of(), List.of());

    public MicroSourceRule {
        pin = List.copyOf(pin);
        hide = List.copyOf(hide);
    }

    /** Later layers add to earlier ones. */
    public MicroSourceRule plus(MicroSourceRule other) {
        List<String> p = new ArrayList<>(pin);
        other.pin.forEach(e -> { if (!p.contains(e)) p.add(e); });
        List<String> h = new ArrayList<>(hide);
        other.hide.forEach(e -> { if (!h.contains(e)) h.add(e); });
        return new MicroSourceRule(p, h);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeCollection(pin, FriendlyByteBuf::writeUtf);
        buf.writeCollection(hide, FriendlyByteBuf::writeUtf);
    }

    public static MicroSourceRule read(FriendlyByteBuf buf) {
        return new MicroSourceRule(buf.readList(FriendlyByteBuf::readUtf), buf.readList(FriendlyByteBuf::readUtf));
    }
}
