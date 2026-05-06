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

}
