package dev.maire.nourished.client.screen.diet.dynamic.edit;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import dev.maire.nourished.client.screen.diet.dynamic.modules.ActiveEffectsComponent;
import dev.maire.nourished.client.screen.diet.dynamic.modules.EatMoreComponent;
import dev.maire.nourished.client.screen.diet.dynamic.modules.RecentMealsComponent;
import dev.maire.nourished.client.screen.diet.dynamic.persistence.DietScreenPersistence;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

/**
 * TEMPORARY dev-only command — {@code /marie resetdietmodules} clears persisted drag/resize state
 * for exactly {@link RecentMealsComponent#ID}, {@link EatMoreComponent#ID}, and {@link
 * ActiveEffectsComponent#ID} via {@link DietScreenPersistence}, forcing those three back to their
 * default stacked position/size on the next render. Deliberately does NOT touch {@link
 * CaloriesComponent#ID}/{@link BalanceComponent#ID} or any right-column state.
 *
 * <p>Lives in this package (not {@code dev.maire.nourished.command}) so it can call {@link
 * DietScreenPersistence#get()} directly without widening that class's package-private access.
 * Registered client-side only via {@link RegisterClientCommandsEvent} — {@link
 * DietScreenPersistence} is itself a client-only class, so this command could never work
 * server-side anyway. No permission gating: this is a short-lived dev tool, not a permanent admin
 * command — delete this file and its {@link #register} call in {@code ClientEventRegistrar} once
 * it's no longer needed.
 */
public final class DietModuleResetCommand {

    private DietModuleResetCommand() {}

    public static void register(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(
                Commands.literal("marie")
                        .then(Commands.literal("resetdietmodules")
                                .executes(DietModuleResetCommand::run))
        );
    }

    private static int run(CommandContext<CommandSourceStack> ctx) {
        DietScreenPersistence.get().remove(RecentMealsComponent.ID);
        DietScreenPersistence.get().remove(EatMoreComponent.ID);
        DietScreenPersistence.get().remove(ActiveEffectsComponent.ID);
        ctx.getSource().sendSuccess(() -> Component.literal(
                        "Reset Recent Meals / Eat more of... / Active Effects to default position/size.")
                .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }
}
