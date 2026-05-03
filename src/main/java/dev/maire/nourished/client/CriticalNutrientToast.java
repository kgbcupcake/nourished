package dev.maire.nourished.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Toast when a nutrient drops below the critical threshold; panel matches {@code DietScreen} styling.
 */
@OnlyIn(Dist.CLIENT)
public class CriticalNutrientToast implements Toast {

    private static final long DISPLAY_MS = 3000L;
    private static final long FADE_MS = 300L;

    /** Same as {@link dev.maire.nourished.client.screen.DietScreen} inner panels. */
    private static final int COL_PANEL = 0xFF1E1E1E;
    private static final int COL_BORDER = 0xFF3A3A3A;
    private static final int COL_BORDER_LT = 0xFF555555;

    private final String nutrientKey;
    private final Component title;
    private final Component subtitle;
    private final ItemStack icon;

    public CriticalNutrientToast(String nutrientKey, ItemStack iconItem) {
        this.nutrientKey = nutrientKey;
        this.icon = iconItem.copy();
        String name = Component.translatable("nourished.screen.diet.bar." + nutrientKey).getString();
        this.title = Component.translatable("nourished.toast.critical.title", name)
                .withStyle(ChatFormatting.RED);
        this.subtitle = Component.translatable("nourished.toast.critical.subtitle", name)
                .withStyle(ChatFormatting.GRAY);
    }

    @Override
    public Visibility render(GuiGraphics guiGraphics, ToastComponent toastComponent, long timeSinceLastVisible) {
        double mult = toastComponent.getNotificationDisplayTimeMultiplier();
        long fullMs = (long) (DISPLAY_MS * mult);
        long fadeMs = (long) (FADE_MS * mult);
        long totalMs = fullMs + fadeMs;

        float alpha = 1f;
        if (timeSinceLastVisible >= fullMs) {
            alpha = 1f - Mth.clamp((float) (timeSinceLastVisible - fullMs) / (float) fadeMs, 0f, 1f);
        }

        int aByte = Mth.clamp(Mth.floor(alpha * 255f), 0, 255);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);

        drawRoundedPanel(guiGraphics, 0, 0, width(), height(), COL_PANEL, COL_BORDER_LT, COL_BORDER, aByte);

        var font = toastComponent.getMinecraft().font;
        int textAlpha = (aByte << 24) | 0xFFFFFF;
        guiGraphics.drawString(font, title, 30, 7, textAlpha, false);
        guiGraphics.drawString(font, subtitle, 30, 18, textAlpha, false);

        guiGraphics.renderItem(icon, 8, 8);

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();

        return timeSinceLastVisible >= totalMs ? Visibility.HIDE : Visibility.SHOW;
    }

    private static void drawRoundedPanel(
            GuiGraphics g, int x, int y, int w, int h, int fill, int borderLight, int borderDark, int aByte) {
        fill = applyAlphaByte(fill, aByte);
        borderLight = applyAlphaByte(borderLight, aByte);
        borderDark = applyAlphaByte(borderDark, aByte);

        int x2 = x + w;
        int y2 = y + h;

        g.fill(x + 2, y, x2 - 2, y2, fill);
        g.fill(x, y + 2, x2, y2 - 2, fill);

        g.fill(x + 1, y + 1, x + 2, y + 2, fill);
        g.fill(x2 - 2, y + 1, x2 - 1, y + 2, fill);
        g.fill(x + 1, y2 - 2, x + 2, y2 - 1, fill);
        g.fill(x2 - 2, y2 - 2, x2 - 1, y2 - 1, fill);

        g.fill(x + 2, y, x2 - 2, y + 1, borderLight);
        g.fill(x + 2, y2 - 1, x2 - 2, y2, borderDark);
        g.fill(x, y + 2, x + 1, y2 - 2, borderLight);
        g.fill(x2 - 1, y + 2, x2, y2 - 2, borderDark);

        g.fill(x + 1, y + 1, x + 2, y + 2, borderLight);
        g.fill(x2 - 2, y + 1, x2 - 1, y + 2, borderLight);
        g.fill(x + 1, y2 - 2, x + 2, y2 - 1, borderDark);
        g.fill(x2 - 2, y2 - 2, x2 - 1, y2 - 1, borderDark);
    }

    private static int applyAlphaByte(int argb, int aByte) {
        int oldA = (argb >>> 24) & 0xFF;
        int combined = Mth.clamp(Mth.floor(oldA * (aByte / 255f)), 0, 255);
        return (combined << 24) | (argb & 0xFFFFFF);
    }

    @Override
    public Object getToken() {
        return nutrientKey;
    }
}
