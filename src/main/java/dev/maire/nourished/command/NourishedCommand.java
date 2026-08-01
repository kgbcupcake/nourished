package dev.maire.nourished.command;

import com.mojang.brigadier.CommandDispatcher;
import dev.marie.framework.api.ApiStatus;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.templates.NourishedColorsTemplateCommand;
import dev.maire.nourished.templates.NourishedEffectsTemplateCommand;
import dev.maire.nourished.templates.NourishedValuesTemplateCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

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
                    .executes(NourishedExportCommands::exportAll))
                .then(Commands.literal("audit_tags")
                    .requires(s -> s.hasPermission(2))
                    .executes(NourishedTagAuditCommand::run))
                .then(Commands.literal("audit")
                    .requires(s -> s.hasPermission(2))
                    .executes(NourishedTagAuditCommand::run))
                .then(Commands.literal("tag")
                    .requires(s -> s.hasPermission(2))
                    .executes(NourishedTagAuditCommand::run))
                .then(Commands.literal("debug")
                    .requires(s -> s.hasPermission(2))
                    .then(Commands.literal("activitylog")
                        .requires(s -> s.hasPermission(2))
                        .executes(NourishedActivityLogCommand::run)))
        );
    }
}
