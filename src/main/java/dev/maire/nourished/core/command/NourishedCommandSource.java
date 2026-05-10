package dev.maire.nourished.core.command;

import dev.maire.nourished.api.DietReportProvider;
import dev.maire.nourished.api.registry.ReportProviderRegistry;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.core.diet.DietData;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared formatting and permission utilities for /nourished commands.
 */
public final class NourishedCommandSource {

    private static final int BAR_WIDTH = 8;

    private NourishedCommandSource() {}

    public enum NutrientStatus {
        CRITICAL,
        LOW,
        OK,
        EXCESS
    }

    public static boolean canTargetOtherPlayer(CommandSourceStack source, ServerPlayer target) {
        ServerPlayer self = source.getPlayer();
        if (self == null) {
            return true;
        }
        return self.getUUID().equals(target.getUUID()) || source.hasPermission(2);
    }

    public static NutrientStatus statusFor(float value, String key, NourishedConfig config) {
        float critical = (float) config.criticalThresholdFor(key);
        float low = (float) config.lowThreshold();
        float excess = (float) config.excessThreshold();
        if (value < critical) return NutrientStatus.CRITICAL;
        if (value < low) return NutrientStatus.LOW;
        if (value > excess) return NutrientStatus.EXCESS;
        return NutrientStatus.OK;
    }

    public static Component statusChip(NutrientStatus status) {
        return switch (status) {
            case CRITICAL -> chip("CRITICAL", ChatFormatting.RED);
            case LOW -> chip("LOW", ChatFormatting.YELLOW);
            case OK -> chip("OK", ChatFormatting.GREEN);
            case EXCESS -> chip("EXCESS", ChatFormatting.AQUA);
        };
    }

    public static List<Component> buildReportLines(ServerPlayer target, DietData diet, String activeProfile, NourishedConfig config) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("=== Nourished Report ===").withStyle(ChatFormatting.GOLD));
        lines.add(Component.literal("Player: ").withStyle(ChatFormatting.GRAY)
                .append(target.getName().copy().withStyle(ChatFormatting.WHITE)));
        lines.add(Component.literal("Active Profile: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(activeProfile).withStyle(ChatFormatting.LIGHT_PURPLE)));
        lines.add(Component.literal("Total Calories: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.format(java.util.Locale.ROOT, "%.0f", diet.calories)).withStyle(ChatFormatting.WHITE)));
        lines.add(Component.literal(" "));

        for (String key : diet.nutrients.keySet()) {
            float value = diet.nutrients.getOrDefault(key, 0f);
            float decayRate = (float) config.decayRateFor(key);
            NutrientStatus status = statusFor(value, key, config);

            MutableComponent line = Component.literal(key + ": ").withStyle(ChatFormatting.WHITE)
                    .append(Component.literal(bar(value) + " "))
                    .append(Component.literal(String.format(java.util.Locale.ROOT, "%3d%% ", Math.round(value * 100f))).withStyle(ChatFormatting.GRAY))
                    .append(statusChip(status))
                    .append(Component.literal("  decay=" + String.format(java.util.Locale.ROOT, "%.4f", decayRate)).withStyle(ChatFormatting.DARK_GRAY));
            lines.add(line);
        }

        for (DietReportProvider provider : ReportProviderRegistry.getAll()) {
            lines.add(Component.literal(" "));
            lines.add(provider.getSectionTitle().copy().withStyle(ChatFormatting.GOLD));
            List<Component> sectionLines = provider.generateReport(target);
            for (Component sectionLine : sectionLines) {
                lines.add(Component.literal("- ").withStyle(ChatFormatting.DARK_GRAY).append(sectionLine.copy()));
            }
        }

        return lines;
    }

    private static Component chip(String text, ChatFormatting color) {
        return Component.literal("[" + text + "]").withStyle(color);
    }

    private static String bar(float value) {
        int filled = Math.round(Math.max(0f, Math.min(1f, value)) * BAR_WIDTH);
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < BAR_WIDTH; i++) {
            sb.append(i < filled ? "█" : "░");
        }
        sb.append("]");
        return sb.toString();
    }
}
