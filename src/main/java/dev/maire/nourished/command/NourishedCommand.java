package dev.maire.nourished.command;

import com.mojang.brigadier.CommandDispatcher;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.templates.NourishedColorsTemplateCommand;
import dev.maire.nourished.templates.NourishedEffectsTemplateCommand;
import dev.maire.nourished.templates.NourishedValuesTemplateCommand;
import dev.marie.MariesLib.api.ApiStatus;
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
        );
    }
}
