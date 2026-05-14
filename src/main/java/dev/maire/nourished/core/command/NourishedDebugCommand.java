package dev.maire.nourished.core.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.maire.nourished.core.nutrition.CacheStats;
import dev.maire.nourished.core.nutrition.ResolutionResult;
import dev.maire.nourished.core.nutrition.RuntimeFoodResolver;
import dev.maire.nourished.core.util.NourishedRegistryUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class NourishedDebugCommand {

    private static final int CACHE_MAX = 2048;

    private NourishedDebugCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> registerHeld() {
        return Commands.literal("held")
                .requires(s -> s.hasPermission(2))
                .executes(NourishedDebugCommand::debugHeld);
    }

    public static LiteralArgumentBuilder<CommandSourceStack> registerCache() {
        return Commands.literal("cache")
                .requires(s -> s.hasPermission(2))
                .executes(NourishedDebugCommand::executeDebugCache);
    }

    private static int executeDebugCache(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        CacheStats stats = RuntimeFoodResolver.getInstance().getCacheStats();
        sendCacheStatsFeedback(source, stats);
        return 1;
    }

    private static void sendCacheStatsFeedback(CommandSourceStack source, CacheStats stats) {
        source.sendSuccess(() -> Component.literal("[Nourished Cache Stats]").withStyle(ChatFormatting.GOLD), false);
        sendCacheKeyValue(source, "Hits        ", String.valueOf(stats.hits()), ChatFormatting.WHITE);
        sendCacheKeyValue(source, "Misses      ", String.valueOf(stats.misses()), ChatFormatting.WHITE);
        sendHitRatioLine(source, stats);
        sendCacheKeyValue(source, "Cache Size  ", stats.size() + " / " + CACHE_MAX, ChatFormatting.WHITE);
        sendAvgResolveLine(source, stats);
        sendSlowestLine(source, stats);
        sendTimeoutsLine(source, stats);
    }

    private static void sendAvgResolveLine(CommandSourceStack source, CacheStats stats) {
        float ms = stats.avgResolveNanos() / 1_000_000f;
        String msText = String.format(Locale.ROOT, "%.2fms", ms);
        ChatFormatting color;
        if (ms > 5f) {
            color = ChatFormatting.RED;
        } else if (ms > 2f) {
            color = ChatFormatting.YELLOW;
        } else {
            color = ChatFormatting.GREEN;
        }
        MutableComponent line = Component.literal("Avg Resolve : ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(msText).withStyle(color));
        source.sendSuccess(() -> line, false);
    }

    private static void sendSlowestLine(CommandSourceStack source, CacheStats stats) {
        float ms = stats.slowestResolveNanos() / 1_000_000f;
        String msText = String.format(Locale.ROOT, "%.2fms", ms);
        ResourceLocation item = stats.slowestItem();
        MutableComponent line = Component.literal("Slowest     : ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(msText).withStyle(ChatFormatting.WHITE))
                .append(Component.literal("  (").withStyle(ChatFormatting.GRAY));
        if (item == null) {
            line.append(Component.literal("N/A").withStyle(ChatFormatting.GRAY));
        } else {
            line.append(Component.literal(item.toString()).withStyle(ChatFormatting.WHITE));
        }
        line.append(Component.literal(")").withStyle(ChatFormatting.GRAY));
        source.sendSuccess(() -> line, false);
    }

    private static void sendTimeoutsLine(CommandSourceStack source, CacheStats stats) {
        int timeouts = stats.recipeTimeouts();
        ChatFormatting color = timeouts >= 1 ? ChatFormatting.YELLOW : ChatFormatting.GREEN;
        sendCacheKeyValue(source, "Timeouts    ", String.valueOf(timeouts), color);
    }

    private static void sendCacheKeyValue(CommandSourceStack source, String label, String value, ChatFormatting valueColor) {
        MutableComponent line = Component.literal(label + ": ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(value).withStyle(valueColor));
        source.sendSuccess(() -> line, false);
    }

    private static void sendHitRatioLine(CommandSourceStack source, CacheStats stats) {
        int total = stats.hits() + stats.misses();
        MutableComponent line = Component.literal("Hit Ratio   : ").withStyle(ChatFormatting.GRAY);
        if (total == 0) {
            line.append(Component.literal("N/A").withStyle(ChatFormatting.GRAY));
        } else {
            float hitRatio = (float) stats.hits() / total * 100f;
            String ratioText = String.format(Locale.ROOT, "%.2f%%", hitRatio);
            ChatFormatting ratioColor;
            if (hitRatio >= 80f) {
                ratioColor = ChatFormatting.GREEN;
            } else if (hitRatio >= 50f) {
                ratioColor = ChatFormatting.YELLOW;
            } else {
                ratioColor = ChatFormatting.RED;
            }
            line.append(Component.literal(ratioText).withStyle(ratioColor));
        }
        source.sendSuccess(() -> line, false);
    }

    private static int debugHeld(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = player.getMainHandItem();

        if (stack.isEmpty()) {
            source.sendFailure(Component.literal("No item in main hand."));
            return 0;
        }

        RecipeManager recipeManager = source.getServer().getRecipeManager();
        ResolutionResult result = RuntimeFoodResolver.getInstance().resolveWithResult(stack, recipeManager);

        if (result == null) {
            source.sendFailure(Component.literal("Could not classify item.").withStyle(ChatFormatting.RED));
            return 0;
        }

        ResourceLocation itemId = NourishedRegistryUtils.itemKey(stack.getItem());
        String itemName = itemId != null ? itemId.toString() : "unknown";

        sendHeader(source);
        sendKeyValue(source, "Item:       ", itemName);
        sendKeyValue(source, "Stage:      ", result.stage().name());
        sendKeyValue(source, "Confidence: ", fmt(result.confidence()));
        sendKeyValue(source, "Reason:     ", result.debugReason());
        sendCacheLine(source, result.cacheHit());
        sendBlank(source);

        sendSection(source, "Tokens:");
        if (result.tokens().isEmpty()) {
            sendDarkGray(source, "  none");
        } else {
            for (String token : result.tokens()) {
                Float weight = result.tokenWeights().get(token);
                sendTokenLine(source, token, weight != null ? weight : 0f);
            }
        }
        sendBlank(source);

        sendSection(source, "Raw Scores:");
        sendSortedMapDescending(source, result.rawScores());
        sendBlank(source);

        sendSection(source, "Normalized:");
        sendSortedMapDescending(source, result.nutrients());
        sendBlank(source);

        sendSection(source, "Rejected Signals:");
        if (result.rejectedSignals().isEmpty()) {
            sendDarkGray(source, "  none");
        } else {
            for (Map.Entry<String, String> entry : result.rejectedSignals().entrySet()) {
                sendRejectedLine(source, entry.getKey(), entry.getValue());
            }
        }

        return 1;
    }

    private static void sendHeader(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("[Nourished Debug]").withStyle(ChatFormatting.GOLD), false);
    }

    private static void sendSection(CommandSourceStack source, String title) {
        source.sendSuccess(() -> Component.literal(title).withStyle(ChatFormatting.YELLOW), false);
    }

    private static void sendKeyValue(CommandSourceStack source, String label, String value) {
        MutableComponent line = Component.literal(label).withStyle(ChatFormatting.GRAY)
                .append(Component.literal(value).withStyle(ChatFormatting.WHITE));
        source.sendSuccess(() -> line, false);
    }

    private static void sendCacheLine(CommandSourceStack source, boolean hit) {
        String label = "Cache:      ";
        String value = hit ? "HIT" : "MISS";
        ChatFormatting color = hit ? ChatFormatting.GREEN : ChatFormatting.RED;
        MutableComponent line = Component.literal(label).withStyle(ChatFormatting.GRAY)
                .append(Component.literal(value).withStyle(color));
        source.sendSuccess(() -> line, false);
    }

    private static void sendTokenLine(CommandSourceStack source, String token, float weight) {
        String line = String.format(Locale.ROOT, "  %-6s -> %s", token, fmt(weight));
        source.sendSuccess(() -> Component.literal(line).withStyle(ChatFormatting.WHITE), false);
    }

    private static void sendSortedMapDescending(CommandSourceStack source, Map<String, Float> map) {
        if (map.isEmpty()) {
            sendDarkGray(source, "  none");
            return;
        }
        List<Map.Entry<String, Float>> sorted = new ArrayList<>(map.entrySet());
        sorted.sort(Comparator.<Map.Entry<String, Float>>comparingDouble(e -> e.getValue().doubleValue()).reversed());
        for (Map.Entry<String, Float> entry : sorted) {
            String line = String.format(Locale.ROOT, "  %-10s -> %s", entry.getKey(), fmt(entry.getValue()));
            source.sendSuccess(() -> Component.literal(line).withStyle(ChatFormatting.WHITE), false);
        }
    }

    private static void sendRejectedLine(CommandSourceStack source, String nutrient, String reason) {
        MutableComponent line = Component.literal("  ")
                .append(Component.literal(nutrient).withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" -> ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal(reason).withStyle(ChatFormatting.DARK_GRAY));
        source.sendSuccess(() -> line, false);
    }

    private static void sendDarkGray(CommandSourceStack source, String text) {
        source.sendSuccess(() -> Component.literal(text).withStyle(ChatFormatting.DARK_GRAY), false);
    }

    private static void sendBlank(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal(" "), false);
    }

    private static String fmt(float value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
