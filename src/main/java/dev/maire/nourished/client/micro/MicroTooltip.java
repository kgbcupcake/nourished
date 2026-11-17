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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** "Nutrients: Omega-3 ●●○ · Fat ●○○" line for food tooltips. */
public final class MicroTooltip {

    private static final int DOTS = 3;
    private static final int PER_LINE = 3;

    private MicroTooltip() {}

    public static List<Component> lines(ItemStack stack) {
        var level = Minecraft.getInstance().level;
        if (level == null) return List.of();
        Map<String, Float> profile = MicroProfileResolver.resolve(stack, level);
        if (profile.isEmpty()) return List.of();
        List<MicroDef> shown = new ArrayList<>();
        for (MicroDef def : MicroRegistry.defs(true)) {
            if (profile.getOrDefault(def.key(), 0f) > 0f) shown.add(def);
        }
        if (shown.isEmpty()) return List.of();
        // strongest first, a few per line so seven nutrients don't make one huge line
        shown.sort(Comparator.comparingDouble((MicroDef d) -> profile.get(d.key())).reversed());
        List<Component> out = new ArrayList<>();
        MutableComponent line = Component.translatable("nourished.tooltip.micros").withStyle(ChatFormatting.GRAY);
        for (int i = 0; i < shown.size(); i++) {
            if (i > 0 && i % PER_LINE == 0) {
                out.add(line);
                line = Component.literal("  ");
            }
            MicroDef def = shown.get(i);
            line.append(Component.literal(i % PER_LINE == 0 ? " " : " · ").withStyle(ChatFormatting.DARK_GRAY));
            Style color = Style.EMPTY.withColor(def.color());
            line.append(Component.literal(def.displayName() + " ").withStyle(color));
            int filled = Mth.clamp(Mth.ceil(profile.get(def.key()) * DOTS), 1, DOTS);
            line.append(Component.literal("●".repeat(filled)).withStyle(color));
            line.append(Component.literal("○".repeat(DOTS - filled)).withStyle(ChatFormatting.DARK_GRAY));
        }
        out.add(line);
        return out;
    }
}
