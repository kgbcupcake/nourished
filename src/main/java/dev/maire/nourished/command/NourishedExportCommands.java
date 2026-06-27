package dev.maire.nourished.command;

import com.mojang.brigadier.context.CommandContext;
import dev.marie.MariesLib.api.ApiStatus;
import dev.maire.nourished.core.nutrition.NutrientFullExporter;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

@ApiStatus.Internal
public final class NourishedExportCommands {

    private NourishedExportCommands() {}

    public static int exportAll(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        NutrientFullExporter.Result result = NutrientFullExporter.run();
        if (result.success()) {
            source.sendSuccess(() -> Component.literal(
                "Exported " + result.filesWritten() + " category file(s) to "
                + result.exportDir().toAbsolutePath() + ".")
                .withStyle(ChatFormatting.GREEN), false);
            return 1;
        }
        if (result.error() != null) {
            source.sendFailure(Component.literal(
                "Failed to export nutrients: " + result.error().getMessage()));
        } else {
            source.sendFailure(Component.literal(
                "Failed to export nutrients: no data produced"));
        }
        return 0;
    }
}
