package dev.maire.nourished.client.screen.diet.classic;

import dev.marie.framework.client.config.state.MarieClientCache;
import dev.marie.framework.client.config.render.MarieValueColors;
import dev.marie.framework.config.FeatureFlagCache;
import dev.marie.framework.tracking.TrackingData;
import dev.maire.nourished.client.screen.diet.dynamic.layout.DietLayout;
import dev.maire.nourished.config.NourishedClientConfig;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.nutrition.NutrientClassificationLookup;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Left-column rendering for the classic Diet Screen (Today header, Calories/Balance boxes, Recent
 * Meals, Eat More of..., Active Effects) — extracted verbatim from the historical monolithic
 * {@code ClassicDietScreen}; no logic changes, only relocated.
 */
final class ClassicDietLeftPanel {

    private ClassicDietLeftPanel() {}

    static void drawLeftPanel(
            GuiGraphics g,
            Minecraft mc,
            TrackingData data,
            int leftPos,
            int topPos,
            double recentMealsScale,
            double eatMoreScale
    ) {
        NourishedClientConfig cc = NourishedClientConfig.get();
        int x  = leftPos + DietLayout.PAD;
        int y  = topPos + 20;
        int bw = DietLayout.SPLIT - DietLayout.PAD * 2;
        int maxY = topPos + DietLayout.HEIGHT - DietLayout.PAD;
        String todayText = Component.translatable("nourished.screen.diet.today").getString();
        int todayW = mc.font.width(todayText);
        int todayGroupW = 16 + 4 + todayW;
        int todayStartX = x + (bw - todayGroupW) / 2;
        g.renderItem(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("minecraft:sunflower"))), todayStartX, y - 8);
        g.drawString(mc.font, Component.translatable("nourished.screen.diet.today"),
                todayStartX + 20, y - 4, ClassicDietDrawHelpers.COL_GOLD, false);
        y += 10;
        if (FeatureFlagCache.enableTotalTracking() && cc.showCaloriesBox()) {
            ClassicDietDrawHelpers.drawRoundedBox(g, x - 2, y - 2, bw + 4, 40);
            g.renderItem(new net.minecraft.world.item.ItemStack(
                            BuiltInRegistries.ITEM.get(ResourceLocation.parse("minecraft:fire_charge"))),
                    x, y + 3);
            g.drawString(mc.font, Component.translatable("nourished.screen.diet.calories_label"),
                    x + 22, y + 4, ClassicDietDrawHelpers.COL_WHITE, false);
            String calStr = (int) data.total + " / " + (int) data.maxTotal;
            g.drawString(mc.font, calStr, x + 22, y + 15, ClassicDietDrawHelpers.COL_GREEN, false);
            float calPct = data.maxTotal > 0 ? Mth.clamp(data.total / data.maxTotal, 0f, 1f) : 0f;
            ClassicDietDrawHelpers.drawSolidBar(g, x, y + 31, bw, 4, calPct, ClassicDietDrawHelpers.COL_GREEN);
            y += 45;
        }
        if (cc.showBalanceBox()) {
            ClassicDietDrawHelpers.drawRoundedBox(g, x - 2, y - 2, bw + 4, 50);
            g.renderItem(new net.minecraft.world.item.ItemStack(
                            BuiltInRegistries.ITEM.get(ResourceLocation.parse("minecraft:comparator"))),
                    x, y + 3);
            g.drawString(mc.font, Component.translatable("nourished.screen.diet.balance_label"),
                    x + 22, y + 4, ClassicDietDrawHelpers.COL_WHITE, false);
            String balKey   = ClassicDietColorLogic.getBalanceKey(data);
            int    balColor = ClassicDietColorLogic.balanceColor(balKey);
            String balText  = Component.translatable("nourished.screen.diet.balance_state." + balKey).getString();
            float balanceScale = 1.2f * (10f / 9f);
            float balTextW = mc.font.width(balText) * balanceScale;
            int bgAlpha = 51;
            int bgColor = (bgAlpha << 24) | (balColor & 0x00FFFFFF);
            g.fill(x + 22 - 2, y + 14, x + 22 + (int) balTextW + 3, y + 25, bgColor);
            PoseStack pose = g.pose();
            pose.pushPose();
            pose.translate(x + 22, y + 15, 0);
            pose.scale(balanceScale, balanceScale, 1f);
            g.drawString(mc.font, balText, 0, 0, balColor, false);
            pose.popPose();
            float balScore = MarieClientCache.getBalanceScore();
            int filledPips = Math.round(balScore * 5);
            int pipTotalW = 5 * 10 + 4 * 3;
            int pipStartX = x + (bw - pipTotalW) / 2;
            for (int i = 0; i < 5; i++) {
                int px = pipStartX + i * 13;
                g.fill(px, y + 38, px + 10, y + 44, i < filledPips ? balColor : ClassicDietDrawHelpers.COL_SEG_EMPTY);
            }
            y += 55;
        }
        List<String> recentIds = MarieClientCache.getRecentSourceIds();
        if (cc.showRecentMeals() && !recentIds.isEmpty()) {
            int rowH = Math.max(1, (int) Math.round(14 * recentMealsScale));
            int recentHeight = 10 + (Math.min(3, recentIds.size()) * rowH);
            if (y + recentHeight <= maxY) {
                ClassicDietDrawHelpers.drawRoundedBox(g, x - 2, y - 2, bw + 4, recentHeight + 4);
                g.drawString(mc.font, Component.translatable("nourished.screen.diet.recent_label"),
                        x, y, ClassicDietDrawHelpers.COL_HEADER, false);
                y += 10;
                int count = 0;
                float iconScale = 0.75f * (float) recentMealsScale;
                int nameOffset = (int) Math.round(16 * recentMealsScale);
                for (String id : recentIds) {
                    if (count >= 3) break;
                    count++;
                    ResourceLocation itemId = ResourceLocation.tryParse(id);
                    if (itemId == null) {
                        continue;
                    }
                    ItemStack recent = new ItemStack(BuiltInRegistries.ITEM.get(itemId));
                    PoseStack poseRecent = g.pose();
                    poseRecent.pushPose();
                    poseRecent.translate(x, y, 0);
                    poseRecent.scale(iconScale, iconScale, 1f);
                    g.renderItem(recent, 0, 0);
                    poseRecent.popPose();
                    String name = recent.getHoverName().getString();
                    String truncated = mc.font.plainSubstrByWidth(name, bw - nameOffset);
                    Map<String, Float> bars = NutrientClassificationLookup.resolveBars(recent.getItem());
                    String nutrientKey = bars.entrySet().stream()
                            .max(Comparator.comparingDouble(entry -> entry.getValue()))
                            .map(Map.Entry::getKey)
                            .orElse(null);
                    int nameColor = nutrientKey != null
                            ? MarieValueColors.baseColorArgb(nutrientKey)
                            : ClassicDietDrawHelpers.COL_WHITE;
                    g.drawString(mc.font, truncated, x + nameOffset, y + 3, nameColor, false);
                    y += rowH;
                }
                y += 4;
            }
        }
        List<String> neglected = MarieClientCache.getNeglectedCategories();
        if (cc.showEatMoreOf() && !neglected.isEmpty()) {
            int eatBoxH = Math.max(1, (int) Math.round(46 * eatMoreScale));
            if (y + eatBoxH <= maxY) {
                ClassicDietDrawHelpers.drawRoundedBox(g, x - 2, y - 2, bw + 4, eatBoxH);
                String suggestionHeader = Component.translatable("nourished.screen.diet.suggestion_label").getString();
                g.drawString(mc.font, mc.font.plainSubstrByWidth(suggestionHeader, bw),
                        x, y, ClassicDietDrawHelpers.COL_HEADER, false);
                y += 10;
                for (int col = 0; col < Math.min(2, neglected.size()); col++) {
                    String categoryKey = neglected.get(col);
                    TagKey<Item> tag = TagKey.create(Registries.ITEM,
                            ResourceLocation.parse(Nourished.MODID + ":nutrients/" + categoryKey));
                    Item exampleItem = null;
                    for (Item item : BuiltInRegistries.ITEM) {
                        if (item.builtInRegistryHolder().is(tag)) {
                            exampleItem = item;
                            break;
                        }
                    }
                    if (exampleItem == null) continue;
                    int suggestionColW = (bw - 4) / 2;
                    int colX = x + col * suggestionColW;
                    g.renderItem(new ItemStack(exampleItem), colX, y);
                }
                y += eatBoxH - 10 + 4;
            }
        }
        if (cc.showActiveEffects()) {
            Minecraft playerMc = mc;
            int effectCount = (playerMc.player != null) ? playerMc.player.getActiveEffects().size() : 0;
            int effectsHeight = 10 + (Math.min(3, effectCount) * 9);
            if (y + effectsHeight <= maxY) {
                drawActiveEffects(g, mc, x, y, bw, maxY);
            }
        }
    }

    private static void drawActiveEffects(GuiGraphics g, Minecraft mc, int x, int y, int bw, int maxY) {
        if (mc.player == null) return;
        g.drawString(mc.font, Component.translatable("nourished.screen.diet.effects_label"),
                x, y, ClassicDietDrawHelpers.COL_HEADER, false);
        y += 10;
        Collection<MobEffectInstance> effects = mc.player.getActiveEffects();
        if (effects.isEmpty()) {
            g.drawString(mc.font, Component.translatable("nourished.screen.diet.effects_none"),
                    x, y, ClassicDietDrawHelpers.COL_GRAY, false);
            return;
        }
        int count = 0;
        for (MobEffectInstance effect : effects) {
            if (count >= 3) break;
            if (y + 9 > maxY) break;
            MobEffect type = effect.getEffect().value();
            String name = Component.translatable(type.getDescriptionId()).getString();
            int amplifier = effect.getAmplifier();
            String label = (amplifier > 0 ? name + " " + (amplifier + 1) : name);
            int color = type.isBeneficial() ? ClassicDietDrawHelpers.COL_GREEN : ClassicDietDrawHelpers.COL_RED;
            String prefix = type.isBeneficial() ? "+ " : "- ";
            g.drawString(mc.font, prefix + label, x, y, color, false);
            y += 9;
            count++;
        }
    }
}
