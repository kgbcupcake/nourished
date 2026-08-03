package dev.maire.nourished.command;

import com.mojang.brigadier.context.CommandContext;
import dev.marie.framework.api.ApiStatus;
import dev.maire.nourished.modules.activity_driven_nutrient.handler.ActivityEffectLog;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

/** Dumps {@link ActivityEffectLog}'s buffered entries to chat — temporary diagnostic for Phase 3a. */
@ApiStatus.Internal
public final class NourishedActivityLogCommand {

    private NourishedActivityLogCommand() {}

    public static int run(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        List<ActivityEffectLog.Entry> entries = ActivityEffectLog.snapshot();

        if (entries.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Activity effect log is empty."), false);
            return 1;
        }

        source.sendSuccess(() -> Component.literal("[Activity Effect Log] " + entries.size() + " entries")
                .withStyle(ChatFormatting.GOLD), false);
        for (ActivityEffectLog.Entry entry : entries) {
            String line = DateTimeFormatter.ISO_INSTANT.format(entry.timestamp())
                    + " " + entry.playerName() + " - " + entry.description();
            source.sendSuccess(() -> Component.literal(line).withStyle(ChatFormatting.WHITE), false);
        }
        return 1;
    }
}
