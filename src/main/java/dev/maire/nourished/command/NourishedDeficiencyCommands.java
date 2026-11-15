package dev.maire.nourished.command;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.marie.framework.resources.api.MarieResourcesAPI;
import dev.maire.nourished.core.deficiency.DeficiencyCalories;
import dev.maire.nourished.core.deficiency.DeficiencyConfig;
import dev.maire.nourished.core.deficiency.DeficiencyNetworking;
import dev.maire.nourished.core.deficiency.DeficiencySettings;
import dev.maire.nourished.core.deficiency.DeficiencyTracker;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** /nourished deficiency ... : inspect and override diet deficiency pressure. */
final class NourishedDeficiencyCommands {

    private static final SuggestionProvider<CommandSourceStack> GROUPS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(groupKeys(), builder);

    private static java.util.List<String> groupKeys() {
        java.util.List<String> keys = new java.util.ArrayList<>(NutrientRegistry.getKeys());
        if (DeficiencyConfig.get().calories().enabled() && !keys.contains(DeficiencyCalories.KEY)) {
            keys.add(DeficiencyCalories.KEY);
        }
        return keys;
    }

    private NourishedDeficiencyCommands() {}

    static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("deficiency")
                .executes(ctx -> show(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                .then(Commands.literal("get")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> show(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("set")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("group", StringArgumentType.word()).suggests(GROUPS)
                                        .then(Commands.argument("tier", IntegerArgumentType.integer(0, 3))
                                                .executes(NourishedDeficiencyCommands::setTier))
                                        .then(Commands.literal("minutes")
                                                .then(Commands.argument("minutes", FloatArgumentType.floatArg(0f))
                                                        .executes(NourishedDeficiencyCommands::setMinutes))))))
                .then(Commands.literal("clear")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> clear(ctx, null))
                                .then(Commands.argument("group", StringArgumentType.word()).suggests(GROUPS)
                                        .executes(ctx -> clear(ctx, StringArgumentType.getString(ctx, "group"))))))
                .then(Commands.literal("reload")
                        .requires(s -> s.hasPermission(2))
                        .executes(NourishedDeficiencyCommands::reload));
    }

    private static int show(CommandSourceStack source, ServerPlayer player) {
        DeficiencySettings s = DeficiencyConfig.get();
        if (!s.enabled()) {
            source.sendSuccess(() -> Component.literal("Diet deficiency is disabled."), false);
            return 0;
        }
        int overall = DeficiencyTracker.overallTier(player);
        StringBuilder out = new StringBuilder(player.getGameProfile().getName())
                .append(": overall L").append(overall)
                .append(String.format(" (activity drain x%.2f)", s.activityMultiplier(overall)));
        for (NutrientRegistry.NutrientDef def : NutrientRegistry.getAll()) {
            if (!DeficiencyTracker.tracked(s, def)) continue;
            int pressure = DeficiencyTracker.pressure(player, def.key());
            out.append("\n  ").append(def.key()).append(": L").append(s.tierFor(pressure))
                    .append(" (").append(minutes(pressure)).append(" / ").append(minutes(s.maxPressure())).append(" min)");
        }
        if (s.calories().enabled()) {
            int pressure = DeficiencyTracker.pressure(player, DeficiencyCalories.KEY);
            out.append("\n  ").append(DeficiencyCalories.KEY).append(": L").append(s.tierFor(pressure))
                    .append(" (").append(minutes(pressure)).append(" / ").append(minutes(s.maxPressure())).append(" min)");
        }
        String text = out.toString();
        source.sendSuccess(() -> Component.literal(text), false);
        return overall;
    }

    private static int setTier(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        String group = group(ctx);
        int tier = IntegerArgumentType.getInteger(ctx, "tier");
        DeficiencyTracker.setPressure(player, group, DeficiencyTracker.pressureForTier(tier));
        return report(ctx.getSource(), player, group);
    }

    private static int setMinutes(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        String group = group(ctx);
        float minutes = FloatArgumentType.getFloat(ctx, "minutes");
        DeficiencyTracker.setPressure(player, group, Math.round(minutes * 1200f));
        return report(ctx.getSource(), player, group);
    }

    private static int clear(CommandContext<CommandSourceStack> ctx, String group) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        if (group != null) group = group(ctx);
        DeficiencyTracker.clear(player, group);
        String what = group == null ? "all groups" : group;
        ctx.getSource().sendSuccess(() -> Component.literal("Cleared deficiency for "
                + player.getGameProfile().getName() + " (" + what + ")"), true);
        return 1;
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        DeficiencyConfig.reload();
        MarieResourcesAPI.broadcastConfigSyncReload(source.getServer(), DeficiencyConfig.SYNC_ID);
        source.getServer().getPlayerList().getPlayers().forEach(p -> DeficiencyNetworking.sync(p, false));
        source.sendSuccess(() -> Component.literal("Reloaded deficiency.json"), true);
        return 1;
    }

    private static String group(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String group = StringArgumentType.getString(ctx, "group");
        if (!groupKeys().contains(group)) {
            throw new com.mojang.brigadier.exceptions.SimpleCommandExceptionType(
                    Component.literal("Unknown food group: " + group)).create();
        }
        return group;
    }

    private static int report(CommandSourceStack source, ServerPlayer player, String group) {
        DeficiencySettings s = DeficiencyConfig.get();
        int pressure = DeficiencyTracker.pressure(player, group);
        source.sendSuccess(() -> Component.literal(player.getGameProfile().getName() + " " + group + ": L"
                + s.tierFor(pressure) + " (" + minutes(pressure) + " min)"), true);
        return s.tierFor(pressure);
    }

    private static String minutes(int ticks) {
        return String.format("%.1f", ticks / 1200f);
    }
}
