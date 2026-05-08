package dev.maire.nourished.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.maire.nourished.Nourished;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.diet.DietAttachment;
import dev.maire.nourished.diet.DietData;
import dev.maire.nourished.nutrition.NutrientRegistry;
import dev.maire.nourished.nutrition.UnassignedFoodScanner;
import dev.maire.nourished.nutrition.scanner.ScanCache;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class NourishedCommand {

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
                Commands.literal("nourished")
                        .then(Commands.literal("debug")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(this::executeDebug)
                                )
                                .executes(this::executeDebugSelf)
                        )
                        .then(Commands.literal("get_unassigned_foods")
                                .requires(source -> source.hasPermission(2))
                                .executes(this::executeGetUnassignedFoods)
                        )
                        .then(Commands.literal("scan_foods")
                                .requires(source -> source.hasPermission(2))
                                .executes(ctx -> executeScanFoods(ctx, true, true))
                                .then(Commands.literal("full")
                                        .executes(ctx -> executeScanFoods(ctx, true, true))
                                )
                                .then(Commands.literal("quick")
                                        .executes(ctx -> executeScanFoods(ctx, false, false))
                                )
                                .then(Commands.literal("json")
                                        .executes(ctx -> executeScanFoods(ctx, true, false))
                                )
                                .then(Commands.literal("recommendations")
                                        .executes(ctx -> executeScanFoods(ctx, false, true))
                                )
                        )
                        .then(Commands.literal("scan_cache")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.literal("clear")
                                        .executes(this::executeClearCache)
                                )
                                .then(Commands.literal("status")
                                        .executes(this::executeCacheStatus)
                                )
                        )
        );
    }

    private int executeDebugSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        return showDebugInfo(context.getSource(), player, player);
    }

    private int executeDebug(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
        ServerPlayer executorPlayer = context.getSource().getPlayer();

        if (executorPlayer != null && !context.getSource().hasPermission(2)) {
            if (!targetPlayer.getUUID().equals(executorPlayer.getUUID())) {
                context.getSource().sendFailure(Component.literal("You can only view your own nutrition data."));
                return 0;
            }
        }

        return showDebugInfo(context.getSource(), targetPlayer, executorPlayer);
    }

    private int showDebugInfo(CommandSourceStack source, ServerPlayer target, ServerPlayer executor) {
        DietData diet = target.getData(DietAttachment.DIET);
        NourishedConfig config = NourishedConfig.get();

        StringBuilder sb = new StringBuilder();
        sb.append("=== Nourished Debug: ").append(target.getName().getString()).append(" ===\n");
        sb.append(String.format(Locale.ROOT, "Calories: %.0f / %.0f\n", diet.calories, diet.maxCalories));

        List<String> keys = NutrientRegistry.getKeys();
        for (String key : keys) {
            float value = diet.nutrients.getOrDefault(key, 0f);
            String bar = buildBar(value);
            double criticalThreshold = config.criticalThresholdFor(key);
            String critical = value < criticalThreshold ? "  ⚠ CRITICAL" : "";
            sb.append(String.format(Locale.ROOT, "%-12s %.2f  [%s]%s\n", key + ":", value, bar, critical));
        }

        source.sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    private String buildBar(float value) {
        int filled = Math.round(Math.min(1f, Math.max(0f, value)) * 10);
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            bar.append(i < filled ? "▓" : "░");
        }
        return bar.toString();
    }

    /**
     * Legacy command - backward compatible simple scan.
     */
    private int executeGetUnassignedFoods(CommandContext<CommandSourceStack> context) {
        List<String> unassignedFoods = new ArrayList<>();
        for (UnassignedFoodScanner.ScanHit hit : UnassignedFoodScanner.scan()) {
            unassignedFoods.add(String.format(Locale.ROOT, "%-40s [fallback: %s]", hit.itemId(), hit.fallbackNutrient()));
        }

        Path outputDir = FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID);
        Path outputFile = outputDir.resolve("unassigned_foods.txt");

        try {
            Files.createDirectories(outputDir);
            try (Writer writer = Files.newBufferedWriter(outputFile)) {
                writer.write("# Nourished — Unassigned Foods\n");
                writer.write("# Generated: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "\n");
                writer.write("# These items have no nourished:nutrients/* tag and defaulted to the fallback nutrient.\n");
                writer.write("# Add them to data/nourished/tags/items/nutrients/<category>.json to classify them.\n");
                writer.write("\n");
                for (String line : unassignedFoods) {
                    writer.write(line + "\n");
                }
            }
            context.getSource().sendSuccess(
                    () -> Component.literal("Wrote " + unassignedFoods.size() + " unassigned foods to config/nourished/unassigned_foods.txt"),
                    false
            );
            return 1;
        } catch (IOException e) {
            Nourished.LOGGER.error("[NourishedCommand] Failed to write unassigned_foods.txt", e);
            context.getSource().sendFailure(Component.literal("Failed to write unassigned foods file: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * New full-featured scan command.
     */
    private int executeScanFoods(CommandContext<CommandSourceStack> context, boolean writeReports, boolean writeRecommendations) {
        CommandSourceStack source = context.getSource();
        NourishedConfig config = NourishedConfig.get();

        source.sendSuccess(() -> Component.literal("Starting food classification scan..."), false);

        UnassignedFoodScanner.ScanOptions options = UnassignedFoodScanner.ScanOptions.defaults()
                .withRecipeInheritance(config.scannerEnableRecipeInheritance())
                .withThreshold((float) config.scannerConfidenceSpreadThreshold())
                .withReports(writeReports)
                .withRecommendations(writeRecommendations)
                .withProgressCallback(msg -> source.sendSuccess(() -> Component.literal("[Scanner] " + msg), false));

        if (source.getServer() != null) {
            options = options.withRecipeManager(source.getServer().getRecipeManager());
        }

        final UnassignedFoodScanner.ScanOptions finalOptions = options;

        CompletableFuture.supplyAsync(() -> UnassignedFoodScanner.scanFull(finalOptions))
                .thenAccept(result -> {
                    source.getServer().execute(() -> {
                        ScanCache.ScanSummary summary = result.summary();

                        StringBuilder sb = new StringBuilder();
                        sb.append("\n=== Food Classification Complete ===\n");
                        sb.append(String.format(Locale.ROOT, "  Total scanned:   %d\n", summary.totalScanned()));
                        sb.append(String.format(Locale.ROOT, "  Auto-classified: %d (confident)\n", summary.autoClassified()));
                        sb.append(String.format(Locale.ROOT, "  Uncertain:       %d (needs review)\n", summary.uncertain()));
                        sb.append(String.format(Locale.ROOT, "  Already tagged:  %d (skipped)\n", summary.alreadyTagged()));

                        if (result.diff() != null && result.diff().hasChanges()) {
                            sb.append(String.format(Locale.ROOT, "\nChanges from last scan:\n"));
                            sb.append(String.format(Locale.ROOT, "  Added:   %d\n", result.diff().added().size()));
                            sb.append(String.format(Locale.ROOT, "  Changed: %d\n", result.diff().changed().size()));
                            sb.append(String.format(Locale.ROOT, "  Removed: %d\n", result.diff().removed().size()));
                        }

                        if (writeReports) {
                            sb.append("\nReports written to config/nourished/");
                        }
                        if (writeRecommendations) {
                            sb.append("\nTag recommendations written to config/nourished/tag_recommendations.json");
                        }

                        source.sendSuccess(() -> Component.literal(sb.toString()), false);
                    });
                })
                .exceptionally(ex -> {
                    source.getServer().execute(() -> {
                        Nourished.LOGGER.error("[NourishedCommand] Scan failed", ex);
                        source.sendFailure(Component.literal("Scan failed: " + ex.getMessage()));
                    });
                    return null;
                });

        return 1;
    }

    private int executeClearCache(CommandContext<CommandSourceStack> context) {
        UnassignedFoodScanner.invalidateCache();
        context.getSource().sendSuccess(
                () -> Component.literal("Food scanner cache cleared. Next scan will perform a full analysis."),
                false
        );
        return 1;
    }

    private int executeCacheStatus(CommandContext<CommandSourceStack> context) {
        ScanCache cache = UnassignedFoodScanner.getCache();
        if (cache == null || cache.isEmpty()) {
            context.getSource().sendSuccess(
                    () -> Component.literal("Scanner cache is empty. Run /nourished scan_foods to populate."),
                    false
            );
        } else {
            String hash = cache.getModListHash();
            int size = cache.size();
            boolean valid = cache.isValid();

            StringBuilder sb = new StringBuilder();
            sb.append("=== Scanner Cache Status ===\n");
            sb.append(String.format(Locale.ROOT, "  Entries: %d\n", size));
            sb.append(String.format(Locale.ROOT, "  Mod hash: %s\n", hash.substring(0, Math.min(12, hash.length())) + "..."));
            sb.append(String.format(Locale.ROOT, "  Valid: %s\n", valid ? "Yes" : "No (mod list changed)"));

            ScanCache.ScanSummary lastSummary = cache.getLastSummary();
            if (lastSummary != null) {
                sb.append(String.format(Locale.ROOT, "\nLast scan:\n"));
                sb.append(String.format(Locale.ROOT, "  Total: %d, Confident: %d, Uncertain: %d\n",
                        lastSummary.totalScanned(), lastSummary.autoClassified(), lastSummary.uncertain()));
            }

            context.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
        }
        return 1;
    }
}
