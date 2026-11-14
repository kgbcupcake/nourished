package dev.maire.nourished.client.micro;

import dev.maire.nourished.core.micro.MicroDef;
import dev.maire.nourished.core.micro.MicroProfileResolver;
import dev.maire.nourished.core.micro.MicroRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;

/** "Nutrients: Omega-3 ●●○ · Fat ●○○" line for food tooltips. */
public final class MicroTooltip {

    private static final int DOTS = 3;

    private MicroTooltip() {}

    public static List<Component> lines(ItemStack stack) {
        var level = Minecraft.getInstance().level;
        if (level == null) return List.of();
        Map<String, Float> profile = MicroProfileResolver.resolve(stack, level);
        if (profile.isEmpty()) return List.of();
        MutableComponent line = Component.translatable("nourished.tooltip.micros").withStyle(ChatFormatting.GRAY);
        boolean first = true;
        for (MicroDef def : MicroRegistry.defs(true)) {
            Float v = profile.get(def.key());
            if (v == null || v <= 0f) continue;
            line.append(Component.literal(first ? " " : " · ").withStyle(ChatFormatting.DARK_GRAY));
            first = false;
            Style color = Style.EMPTY.withColor(def.color());
            line.append(Component.literal(def.displayName() + " ").withStyle(color));
            int filled = Mth.clamp(Mth.ceil(v * DOTS), 1, DOTS);
            line.append(Component.literal("●".repeat(filled)).withStyle(color));
            line.append(Component.literal("○".repeat(DOTS - filled)).withStyle(ChatFormatting.DARK_GRAY));
        }
        return first ? List.of() : List.of(line);
    }
}
