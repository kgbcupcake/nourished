package dev.maire.nourished.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.nutrition.NutrientFullExporter;
import dev.maire.nourished.templates.NourishedColorsTemplateCommand;
import dev.maire.nourished.templates.NourishedEffectsTemplateCommand;
import dev.maire.nourished.templates.NourishedValuesTemplateCommand;
import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.config.validation.Finding;
import dev.marie.MariesLib.config.validation.ValidationResult;
import dev.marie.MariesLib.config.validation.ValidationRunner;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.List;

@ApiStatus.Internal
public final class NourishedCommand {

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(
                Commands.literal(Nourished.MODID)
                        .then(Commands.literal("export_effects_template")
                                .requires(s -> s.hasPermission(2))
                                .executes(NourishedEffectsTemplateCommand::export))
                        .then(Commands.literal("export_values_template")
                                .requires(s -> s.hasPermission(2))
                                .executes(NourishedValuesTemplateCommand::export))
                        .then(Commands.literal("export_colors_template")
                                .requires(s -> s.hasPermission(2))
                                .executes(NourishedColorsTemplateCommand::export))
                        .then(Commands.literal("export_all")
                                .requires(s -> s.hasPermission(2))
                                .executes(NourishedCommand::exportAll))
                        .then(Commands.literal("validate")
                                .requires(s -> s.hasPermission(2))
                                .executes(NourishedCommand::validate))
                        .then(Commands.literal("audit_tags")
                                .requires(s -> s.hasPermission(2))
                                .executes(NourishedTagAuditCommand::run))
                        .then(Commands.literal("audit")
                                .requires(s -> s.hasPermission(2))
                                .executes(NourishedTagAuditCommand::run))
                        .then(Commands.literal("tag")
                                .requires(s -> s.hasPermission(2))
                                .executes(NourishedTagAuditCommand::run))
        );
    }

    private static int exportAll(CommandContext<CommandSourceStack> ctx) {
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
            source.sendFailure(Component.literal("Failed to export nutrients: " + result.error().getMessage()));
        } else {
            source.sendFailure(Component.literal("Failed to export nutrients: no data produced"));
        }
        return 0;
    }

    private static int validate(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        List<ValidationResult> results = ValidationRunner.runForMod(Nourished.MODID);

        source.sendSuccess(() -> Component.literal("[Nourished Validate]").withStyle(ChatFormatting.GOLD), false);

        if (results.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No config validators registered.").withStyle(ChatFormatting.GRAY), false);
            return 1;
        }

        for (ValidationResult result : results) {
            ChatFormatting statusColor = switch (result.status()) {
                case PASS -> ChatFormatting.GREEN;
                case WARN -> ChatFormatting.YELLOW;
                case FAIL -> ChatFormatting.RED;
            };
            int findingCount = result.findings().size();
            source.sendSuccess(() -> Component.literal("[" + result.validatorId() + "] "
                            + result.status().name() + " (" + findingCount + " findings)")
                    .withStyle(statusColor), false);

            if (result.status() != ValidationResult.Status.PASS) {
                for (Finding finding : result.findings()) {
                    String detail = finding.key() != null
                            ? finding.file() + ": " + finding.key() + " — " + finding.message()
                            : finding.file() + " — " + finding.message();
                    source.sendSuccess(() -> Component.literal("  " + detail).withStyle(ChatFormatting.GRAY), false);
                }
            }
        }

        return 1;
    }
}
