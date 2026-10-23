package dev.maire.nourished.core.book;

import com.klikli_dev.modonomicon.item.ModonomiconItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class NourishedBookItem extends ModonomiconItem {

    private static final TextColor HEADER = TextColor.fromRgb(0x7BC96F);
    private static final TextColor ACCENT = TextColor.fromRgb(0xF5A623);

    // Order mirrors the in-book category order (NourishedGuideBook#generateCategories).
    private static final String[] CONTENTS_KEYS = {
            "getting_started", "food_groups", "how_it_works",
            "food_safety", "tips_and_tricks", "compat_mods", "server_owners"
    };

    public NourishedBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {
        super.appendHoverText(stack, context, list, flag);

        if (!Screen.hasShiftDown()) {
            list.add(Component.translatable("item.nourished.nourished_book.tooltip.hold_shift")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            return;
        }

        list.add(Component.empty());
        list.add(Component.translatable("item.nourished.nourished_book.tooltip.header")
                .withStyle(style -> style.withColor(HEADER).withBold(true)));

        for (String key : CONTENTS_KEYS) {
            // Lang value is "Label|description" so each bullet can color the two halves differently.
            String raw = Component.translatable("item.nourished.nourished_book.tooltip." + key).getString();
            String[] parts = raw.split("\\|", 2);

            MutableComponent line = Component.literal(" - ").withStyle(ChatFormatting.DARK_GRAY)
                    .append(Component.literal(parts[0]).withStyle(style -> style.withColor(ACCENT)));
            if (parts.length > 1) {
                line = line.append(Component.literal(": " + parts[1]).withStyle(ChatFormatting.GRAY));
            }
            list.add(line);
        }

        list.add(Component.empty());
        list.add(Component.translatable("item.nourished.nourished_book.tooltip.hint")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
