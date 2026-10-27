package dev.maire.nourished.client.screen.diet;

import dev.maire.nourished.client.screen.diet.dynamic.persistence.DietSettings;
import dev.marie.framework.client.config.state.MarieClientCache;
import dev.marie.framework.color.MarieColors;
import dev.marie.framework.config.FeatureFlagCache;
import dev.marie.framework.tracking.TrackingData;
import dev.marie.framework.tracking.tracker.MarieTracking;
import dev.marie.framework.ui.component.MarieComponent;
import dev.marie.framework.ui.hub.HubInfoPage;
import dev.marie.framework.ui.hub.HubInfoSection;
import dev.maire.nourished.api.NourishedAPI;
import dev.maire.nourished.client.colors.NourishedColors;
import dev.maire.nourished.client.hud.dynamic.HudDrawHelpers;
import dev.maire.nourished.client.screen.diet.dynamic.modules.ActiveEffectsComponent;
import dev.maire.nourished.client.screen.diet.dynamic.modules.BalanceComponent;
import dev.maire.nourished.config.NourishedClientConfig;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * The "Home" hub page shared by every Diet Screen mode that embeds a {@code HubPanel}: the viewing
 * player in 3D beside a live nutrition summary. Every value is read from the same client-side state
 * the Diet Screen's own boxes draw from, so the summary always matches them. Extracted out of {@link
 * DietScreen} so {@link dev.maire.nourished.client.screen.diet.nutritionalhud.NutritionalHudScreen}
 * can reuse it verbatim instead of duplicating it.
 */
public final class DietHomeSections {

    private DietHomeSections() {}

    public static MarieComponent homePage() {
        return new HubInfoPage("nourished.diet.hub.home",
                () -> Minecraft.getInstance().player,
                () -> {
                    var player = Minecraft.getInstance().player;
                    return player != null ? player.getName().getString()
                            : Component.translatable("nourished.screen.diet.home.no_player").getString();
                },
                Component.translatable("nourished.screen.diet.home.join_world").getString(),
                DietHomeSections::homeSections);
    }

    private static List<HubInfoSection> homeSections() {
        Minecraft mc = Minecraft.getInstance();
        TrackingData data = getClientData();
        String todayTitle = Component.translatable("nourished.screen.diet.today").getString();
        if (mc.player == null || data == null) {
            return List.of(new HubInfoSection(todayTitle, List.of(
                    new HubInfoSection.Row(Component.translatable("nourished.screen.diet.no_player").getString(), ""))));
        }

        List<HubInfoSection.Row> today = new ArrayList<>();
        // Same gate CaloriesComponent uses — no calorie row when that box can't show either.
        if (FeatureFlagCache.enableTotalTracking() && FeatureFlagCache.enableCalorieHistory()) {
            float calories = MarieTracking.getCurrentTrackerValue(mc.player, NourishedAPI.CALORIES_TRACKER_ID);
            today.add(new HubInfoSection.Row(Component.translatable("nourished.screen.diet.calories_label").getString(),
                    (int) calories + " / " + (int) data.maxTotal, MarieColors.resolveColor(NourishedColors.CALORIE_VALUE)));
        }
        String balanceKey = BalanceComponent.getBalanceKey(data);
        int balanceColor = BalanceComponent.balanceColor(balanceKey);
        today.add(new HubInfoSection.Row(Component.translatable("nourished.screen.diet.balance_label").getString(),
                Component.translatable("nourished.screen.diet.balance_state." + balanceKey).getString(), balanceColor));
        today.add(new HubInfoSection.Row(Component.translatable("nourished.screen.diet.home.balance_score").getString(),
                Math.round(MarieClientCache.getBalanceScore() * 5) + " / 5", balanceColor));

        List<HubInfoSection.Row> intake = new ArrayList<>();
        for (String key : DietSettings.barOrder()) {
            float value = data.values.getOrDefault(key, 0f);
            intake.add(new HubInfoSection.Row(NutrientRegistry.getLabelComponent(key).getString(),
                    Math.round(value * 100) + "%", HudDrawHelpers.nutrientColorArgb(key)));
        }

        List<HubInfoSection.Row> effects = new ArrayList<>();
        float tickRate = mc.level != null ? mc.level.tickRateManager().tickrate() : 20f;
        for (MobEffectInstance effect : mc.player.getActiveEffects()) {
            MobEffect type = effect.getEffect().value();
            String name = ActiveEffectsComponent.stripIconGlyphs(Component.translatable(type.getDescriptionId()).getString());
            int amplifier = effect.getAmplifier();
            effects.add(new HubInfoSection.Row(amplifier > 0 ? name + " " + (amplifier + 1) : name,
                    MobEffectUtil.formatDuration(effect, 1.0f, tickRate).getString(),
                    MarieColors.resolveColor(type.isBeneficial() ? NourishedColors.EFFECT_BENEFICIAL : NourishedColors.EFFECT_HARMFUL)));
        }
        if (effects.isEmpty()) {
            effects.add(new HubInfoSection.Row(Component.translatable("nourished.screen.diet.effects_none").getString(), ""));
        }

        return List.of(
                new HubInfoSection(todayTitle, today),
                new HubInfoSection(Component.translatable("nourished.screen.diet.intake").getString(), intake),
                new HubInfoSection(Component.translatable("nourished.screen.diet.effects_label").getString(), effects));
    }

    private static TrackingData getClientData() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return null;
        return MarieClientCache.get();
    }
}
