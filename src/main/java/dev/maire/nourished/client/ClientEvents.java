package dev.maire.nourished.client;

import java.util.Locale;
import java.util.Map;

import com.mojang.blaze3d.systems.RenderSystem;

import dev.maire.nourished.client.screen.DietScreen;
import dev.maire.nourished.config.ModuleCache;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.client.config.NourishedConfigScreen;
import dev.maire.nourished.core.diet.DietData;
import dev.maire.nourished.core.nutrition.FoodFamilyResolver;
import dev.maire.nourished.core.nutrition.FoodNutritionRegistry;
import dev.maire.nourished.core.nutrition.FoodNutritionRegistry.DietDelta;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import dev.maire.nourished.core.util.NourishedItemTags;
import dev.maire.nourished.core.util.NourishedRegistryUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public final class ClientEvents {

    private static final ResourceLocation DIET_BUTTON_TEXTURE =
        ResourceLocation.fromNamespaceAndPath("nourished", "textures/gui/diet_button.png");

    private ClientEvents() {}

    private static final class InventoryDietButton extends Button {
        InventoryDietButton(int x, int y) {
            super(x, y, 20, 20, Component.empty(),
                    b -> Minecraft.getInstance().setScreen(new DietScreen()),
                    b -> Component.empty());
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            graphics.blit(DIET_BUTTON_TEXTURE, getX() + 2, getY() + 2, 0, 0, 16, 16, 16, 16);
            RenderSystem.disableBlend();
            if (mouseX >= getX() && mouseX <= getX() + width && mouseY >= getY() && mouseY <= getY() + height) {
                graphics.renderTooltip(Minecraft.getInstance().font,
                        Component.translatable("nourished.screen.diet.tooltip.nourish"),
                        mouseX, mouseY);
            }
        }
    }

    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen screen)) return;
        if (!ModuleCache.enableDietScreen) return;

        int x = screen.getGuiLeft() - 26;
        int y = screen.getGuiTop() + 60;

        event.addListener(new InventoryDietButton(x, y));
    }

    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!ModuleCache.enableFoodTooltips) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) {
            return;
        }
        Player player = event.getEntity();
        if (player == null) {
            player = mc.player;
        }
        FoodProperties food = FoodNutritionRegistry.foodPropertiesForNutrition(stack, player);
        if (food == null) {
            return;
        }

        Map<String, Float> matchedBars = FoodNutritionRegistry.resolveNutrientBars(stack, false, level);
        DietDelta delta = FoodNutritionRegistry.computeDietDelta(
                stack, level, food.nutrition(), food.saturation(), matchedBars);

        final float minLine = 0.02f;
        String highestKey = null;
        float highestValue = Float.NEGATIVE_INFINITY;
        if (!matchedBars.isEmpty()) {
            for (String key : NutrientRegistry.getKeys()) {
                float v = delta.nutrients().getOrDefault(key, 0f);
                if (v > highestValue) {
                    highestValue = v;
                    highestKey = key;
                }
            }
        }

        var lines = event.getToolTip();
        lines.add(Component.empty());
        lines.add(Component.literal("✦ Nourished").withStyle(ChatFormatting.GOLD));

        if (matchedBars.isEmpty()) {
            lines.add(Component.translatable("nourished.tooltip.unclassified").withStyle(ChatFormatting.GRAY));
        }

        String itemId = NourishedRegistryUtils.itemKey(stack).toString();
        DietData diet = ClientDietCache.get();
        String dominantCategory = matchedBars.isEmpty()
                ? null
                : matchedBars.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse(null);
        String familyKey = FoodFamilyResolver.resolve(
                NourishedRegistryUtils.itemKey(stack));
        long gameTimeMs = ClientDietCache.get().lastTickTime > 0
                ? ClientDietCache.get().lastTickTime
                : 0L;
        float multiplier = diet.peekMultiplier(itemId, dominantCategory, familyKey, gameTimeMs);

        if (multiplier < 1.0f) {
            String pct = (int)(multiplier * 100) + "%";
            lines.add(Component.translatable("nourished.tooltip.diminished", pct)
                    .withStyle(ChatFormatting.YELLOW));
        } else {
            lines.add(Component.translatable("nourished.tooltip.fresh")
                    .withStyle(ChatFormatting.GREEN));
        }
        if (ModuleCache.enableDebugLogging) {
            var breakdown = diet.getMultiplierBreakdown(itemId, dominantCategory, familyKey, gameTimeMs);
            float fin = breakdown.finalMultiplier();
            String itemPct = fin > 1e-6f ? (int) ((breakdown.itemContribution() / fin) * 100) + "%" : "0%";
            String categoryPct = fin > 1e-6f ? (int) ((breakdown.categoryContribution() / fin) * 100) + "%" : "0%";
            String familyPct = fin > 1e-6f ? (int) ((breakdown.familyContribution() / fin) * 100) + "%" : "0%";

            lines.add(Component.empty());
            lines.add(Component.literal("  Weights: item " + String.format(Locale.ROOT, "%.2f", breakdown.itemWeight())
                            + "  category " + String.format(Locale.ROOT, "%.2f", breakdown.categoryWeight())
                            + "  family " + String.format(Locale.ROOT, "%.2f", breakdown.familyWeight()))
                    .withStyle(Style.EMPTY.withColor(0xFF666666)));
            lines.add(Component.literal("  Item memory:     " + itemPct)
                    .withStyle(Style.EMPTY.withColor(0xFF666666)));
            lines.add(Component.literal("  Category (" + (dominantCategory != null ? dominantCategory : "none") + "): " + categoryPct)
                    .withStyle(Style.EMPTY.withColor(0xFF666666)));
            lines.add(Component.literal("  Family (" + (familyKey != null ? familyKey : "none") + "): " + familyPct)
                    .withStyle(Style.EMPTY.withColor(0xFF666666)));
            lines.add(Component.literal("  Novelty on item path: x" + String.format(Locale.ROOT, "%.3f", breakdown.noveltyContribution()))
                    .withStyle(Style.EMPTY.withColor(0xFF666666)));
            lines.add(Component.literal("  Blend: item " + String.format(Locale.ROOT, "%.3f", breakdown.itemContribution())
                            + "  cat " + String.format(Locale.ROOT, "%.3f", breakdown.categoryContribution())
                            + "  fam " + String.format(Locale.ROOT, "%.3f", breakdown.familyContribution()))
                    .withStyle(Style.EMPTY.withColor(0xFF666666)));
            lines.add(Component.literal("  \u2192 " + (int) (breakdown.finalMultiplier() * 100) + "% nutrition gain")
                    .withStyle(breakdown.finalMultiplier() < 1.0f ? ChatFormatting.GOLD : ChatFormatting.GREEN));
        } else if (NourishedConfig.get().debugMemoryLogging()) {
            var breakdown = diet.getMultiplierBreakdown(itemId, dominantCategory, familyKey, gameTimeMs);
            String itemPct = (int) ((breakdown.itemContribution() / breakdown.finalMultiplier()) * 100) + "%";
            String categoryPct = (int) ((breakdown.categoryContribution() / breakdown.finalMultiplier()) * 100) + "%";
            String familyPct = (int) ((breakdown.familyContribution() / breakdown.finalMultiplier()) * 100) + "%";

            lines.add(Component.empty());
            lines.add(Component.literal("  Item memory:     " + itemPct)
                    .withStyle(Style.EMPTY.withColor(0xFF666666)));
            lines.add(Component.literal("  Category (" + (dominantCategory != null ? dominantCategory : "none") + "): " + categoryPct)
                    .withStyle(Style.EMPTY.withColor(0xFF666666)));
            lines.add(Component.literal("  Family (" + (familyKey != null ? familyKey : "none") + "): " + familyPct)
                    .withStyle(Style.EMPTY.withColor(0xFF666666)));
            lines.add(Component.literal("  \u2192 " + (int) (breakdown.finalMultiplier() * 100) + "% nutrition gain")
                    .withStyle(breakdown.finalMultiplier() < 1.0f ? ChatFormatting.GOLD : ChatFormatting.GREEN));
        }

        String fmt = multiplier < 1.0f ? "%.2f" : "%.1f";
        boolean renderedAny = false;
        for (String key : NutrientRegistry.getKeys()) {
            float base = delta.nutrients().getOrDefault(key, 0f);
            if (base < minLine) {
                continue;
            }
            float display = base * multiplier;
            renderedAny = true;
            String label = NourishedRegistryUtils.capitalizeFirst(key);
            String gain = String.format(Locale.ROOT, fmt, display);
            String text = "  " + label + "  +" + gain;
            int color = NutrientUiColors.baseColorArgb(key);
            MutableComponent line = Component.literal(text).withStyle(Style.EMPTY.withColor(color));
            lines.add(line);
        }

        if (!renderedAny && highestKey != null && highestValue > 0f) {
            float base = Math.max(0f, delta.nutrients().getOrDefault(highestKey, 0f));
            float display = base * multiplier;
            String label = NourishedRegistryUtils.capitalizeFirst(highestKey);
            String gain = String.format(Locale.ROOT, fmt, display);
            String text = "  " + label + "  +" + gain;
            int color = NutrientUiColors.baseColorArgb(highestKey);
            MutableComponent line = Component.literal(text).withStyle(Style.EMPTY.withColor(color));
            lines.add(line);
        }

        if (ModuleCache.enableDecay && ModuleCache.enableNutritionEating) {
            boolean bypassEligible = (food.nutrition() <= 2 || stack.is(NourishedItemTags.LIGHT_FOOD)) && !stack.is(NourishedItemTags.MEAL);
            if (bypassEligible) {
                lines.add(Component.translatable("nourished.tooltip.light_food")
                        .withStyle(ChatFormatting.GRAY));
            }
        }
    }

    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return;
        if (NourishedKeys.OPEN_CONFIG.consumeClick()) {
            mc.setScreen(NourishedConfigScreen.create(null));
        }
    }
}
