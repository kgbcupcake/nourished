package dev.maire.nourished.core.context;

import dev.marie.framework.api.ApiStatus;
import dev.maire.nourished.config.NourishedConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

@ApiStatus.Internal
public final class NourishedJoinMessage {

    private NourishedJoinMessage() {}

    public static Component line1() {
        return Component.literal("◆ ").withStyle(style -> style.withColor(0xF4C95D))
                .append(Component.literal("NOURISHED").withStyle(style -> style.withColor(0x6FD3FF).withBold(true)))
                .append(Component.literal(" ◆ ").withStyle(style -> style.withColor(0xF4C95D)))
                .append(Component.literal(NourishedConfig.get().joinMessageLine1())
                        .withStyle(style -> style.withColor(0xCFEFFF)));
    }

    public static Component line2() {
        String line2 = NourishedConfig.get().joinMessageLine2();
        int split = line2.indexOf(" - ");
        Component line2Body = split >= 0
                ? Component.literal(line2.substring(0, split + 1))
                        .withStyle(style -> style.withColor(0xFF6B6B).withBold(true))
                        .append(Component.literal(line2.substring(split + 1))
                                .withStyle(style -> style.withColor(0xFFC2C2)))
                : Component.literal(line2).withStyle(style -> style.withColor(0xFFC2C2));
        return Component.literal("⚠ ").withStyle(ChatFormatting.RED).append(line2Body);
    }
}
