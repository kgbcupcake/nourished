package dev.maire.nourished.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.maire.nourished.nutrition.NutrientRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Advancement-style toast when a nutrient drops below the critical threshold.
 */
@OnlyIn(Dist.CLIENT)
public class CriticalNutrientToast implements Toast {

    private static final ResourceLocation BACKGROUND_SPRITE =
            ResourceLocation.withDefaultNamespace("toast/advancement");

    private static final long DISPLAY_MS = 3000L;
    private static final long FADE_MS = 400L;

    private static final int COL_TITLE = 0xFFFF5555;
    private static final int COL_SUBTITLE = 0xFF888888;

    private final String nutrientKey;
    private final Component title;
    private final Component subtitle;
    private final ItemStack icon;

    public CriticalNutrientToast(String nutrientKey) {
        this.nutrientKey = nutrientKey;
        String name = Component.translatable("nourished.screen.diet.bar." + nutrientKey).getString();
        this.title = Component.translatable("nourished.toast.critical.title", name);
        this.subtitle = Component.translatable("nourished.toast.critical.subtitle", name);
        this.icon = new ItemStack(
                BuiltInRegistries.ITEM.get(ResourceLocation.parse(NutrientRegistry.getIcon(nutrientKey))));
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

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        int aByte = Mth.clamp(Mth.floor(alpha * 255f), 0, 255);
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
        guiGraphics.blitSprite(BACKGROUND_SPRITE, 0, 0, width(), height());

        var font = toastComponent.getMinecraft().font;
        int titleArgb = (aByte << 24) | (COL_TITLE & 0xFFFFFF);
        int subArgb = (aByte << 24) | (COL_SUBTITLE & 0xFFFFFF);
        guiGraphics.drawString(font, title, 30, 7, titleArgb, false);
        guiGraphics.drawString(font, subtitle, 30, 18, subArgb, false);

        guiGraphics.renderFakeItem(icon, 8, 8);

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();

        return timeSinceLastVisible >= totalMs ? Visibility.HIDE : Visibility.SHOW;
    }

    @Override
    public Object getToken() {
        return nutrientKey;
    }
}
