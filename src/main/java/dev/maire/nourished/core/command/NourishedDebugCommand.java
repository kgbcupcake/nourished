package dev.maire.nourished.core.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marie.MariesLib.config.ModuleCache;
import dev.maire.nourished.core.Nourished;
import dev.marie.MariesLib.scan.CacheStats;
import dev.maire.nourished.core.nutrition.FoodNutritionRegistry;
import dev.maire.nourished.core.nutrition.NutrientResolutionTrace;
import dev.marie.MariesLib.scan.ResolutionResult;
import dev.marie.MariesLib.scan.ResolutionStage;
import dev.marie.MariesLib.scan.RuntimeCascadeStage;
import dev.maire.nourished.core.nutrition.RuntimeFoodResolver;
import dev.maire.nourished.core.nutrition.TagRuntimeBlend;
import dev.marie.MariesLib.util.MarieRegistryUtils;
import dev.marie.MariesLib.classification.ClassificationTrace;
import dev.marie.MariesLib.classification.ClassificationTraceFormatter;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.fml.loading.FMLPaths;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class NourishedDebugCommand {

    private static final int CACHE_MAX = 2048;
    private static final String DEBUG_SUBDIR = "debug";

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
            source.sendSuccess(() -> Component.literal("No item in main hand."), false);
            return 0;
        }

        RecipeManager recipeManager = source.getServer().getRecipeManager();
        NutrientResolutionTrace trace = FoodNutritionRegistry.resolveHeldItemTrace(stack, recipeManager);
        ClassificationTrace classTrace = FoodNutritionRegistry.resolveHeldItemClassificationTrace(stack, recipeManager);
        String inspectorOutput = ClassificationTraceFormatter.format(classTrace, stack);

        ResourceLocation itemId = MarieRegistryUtils.itemKey(stack.getItem());
        String itemName = itemId != null ? itemId.toString() : "unknown";

        Instant dumpedAt = Instant.now();
        String fullTrace = "Item ID: " + itemName
                + "\nDisplay Name: " + singleLineForDump(stack.getHoverName().getString())
                + "\nTimestamp: " + DateTimeFormatter.ISO_INSTANT.format(dumpedAt)
                + "\n\n" + trace.format()
                + "\n\n---\n\n" + inspectorOutput;

        try {
            Path dir = FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID).resolve(DEBUG_SUBDIR);
            Files.createDirectories(dir);
            Path file = dir.resolve("trace_dump.txt");
            Files.writeString(file, fullTrace);
            source.sendSuccess(() -> Component.literal("Trace written to: " + file.toString()), false);
        } catch (IOException e) {
            Nourished.LOGGER.warn("[NourishedDebugCommand] Failed to write trace dump: {}", e.getMessage());
            source.sendSuccess(() -> Component.literal("Trace write failed: " + e.getMessage()), false);
        }

        return 1;
    }

    private static void sendTraceDetails(CommandSourceStack source, NutrientResolutionTrace trace) {
        sendSection(source, "Resolution Trace:");

        sendKeyValue(source, "  Tag Raw:    ", mapKeysString(trace.tagDerivedRaw()));
        sendKeyValue(source, "  Tag Filter: ", mapKeysString(trace.tagDerivedFiltered()));
        if (!trace.strippedByCompat().isEmpty()) {
            sendKeyValue(source, "  Stripped:   ", String.join(", ", trace.strippedByCompat()));
        }
        sendKeyValue(source, "  External:   ", trace.externalSource().name() + " " + mapKeysString(trace.externalMap()));
        sendKeyValue(source, "  Resolver:   ", mapKeysString(trace.resolverNutrients()));

        if (!trace.blendPrecedence().isEmpty()) {
            sendBlank(source);
            sendSection(source, "Blend Decisions:");
            for (Map.Entry<String, TagRuntimeBlend.Precedence> e : trace.blendPrecedence().entrySet()) {
                sendKeyValue(source, "  " + e.getKey() + ": ", e.getValue().name());
            }
        }

        if (!trace.blendDiscardedResolver().isEmpty()) {
            sendBlank(source);
            sendSection(source, "Discarded (tag precedence):");
            for (Map.Entry<String, Float> e : trace.blendDiscardedResolver().entrySet()) {
                sendKeyValue(source, "  " + e.getKey() + ": ", fmt(e.getValue()));
            }
        }
    }

    private static String mapKeysString(Map<String, Float> map) {
        if (map.isEmpty()) return "(none)";
        return String.join(", ", map.keySet());
    }

    private static Path writeTraceDump(NutrientResolutionTrace trace, ClassificationTrace classTrace,
                                       String inspectorOutput, String itemIdStr, String displayName) {
        try {
            Path debugDir = FMLPaths.GAMEDIR.get().resolve("config").resolve(Nourished.MODID).resolve("debug");
            Files.createDirectories(debugDir);

            Instant dumpedAt = Instant.now();
            String sanitizedId = sanitizeForFilename(itemIdStr);
            String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                    .withZone(ZoneOffset.UTC)
                    .format(dumpedAt);
            String filename = "held_" + sanitizedId + "_" + timestamp + ".txt";
            Path path = debugDir.resolve(filename);

            try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                writer.write("Item ID: ");
                writer.write(itemIdStr);
                writer.write("\nDisplay Name: ");
                writer.write(singleLineForDump(displayName));
                writer.write("\nTimestamp: ");
                writer.write(DateTimeFormatter.ISO_INSTANT.format(dumpedAt));
                writer.write("\n\n");
                writer.write(trace.format());
                writer.write("\n\n---\n\n");
                writer.write(inspectorOutput);
            }

            return path;
        } catch (IOException e) {
            Nourished.LOGGER.warn("[NourishedDebugCommand] Failed to write trace dump: {}", e.getMessage());
            return null;
        }
    }

    /** Collapses line breaks so trace dump header stays single-line per field. */
    private static String singleLineForDump(String s) {
        return s == null ? "" : s.replace('\r', ' ').replace('\n', ' ');
    }

    private static String sanitizeForFilename(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            if (Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '.') {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        return sb.toString();
    }

    private static void sendPipelineStageLine(CommandSourceStack source, ResolutionStage stage) {
        ChatFormatting color = switch (stage) {
            case TAG_MATCH -> ChatFormatting.GREEN;
            case BLENDED -> ChatFormatting.AQUA;
            case SCANNER_CLASSIFIED -> ChatFormatting.YELLOW;
            case RUNTIME_RESOLVER -> ChatFormatting.WHITE;
            case UNCLASSIFIED -> ChatFormatting.RED;
        };
        MutableComponent line = Component.literal("Stage:      ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(stage.name()).withStyle(color));
        source.sendSuccess(() -> line, false);
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

    private static void sendCascadeStageLine(CommandSourceStack source, RuntimeCascadeStage stage) {
        ChatFormatting color = stage == RuntimeCascadeStage.COMPOSITE || stage == RuntimeCascadeStage.COMPOSITE_RECIPE
                ? ChatFormatting.AQUA
                : ChatFormatting.WHITE;
        MutableComponent line = Component.literal("Cascade:    ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(stage.displayName()).withStyle(color));
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
