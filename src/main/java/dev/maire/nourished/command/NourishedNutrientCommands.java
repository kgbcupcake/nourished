package dev.maire.nourished.command;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.maire.nourished.core.micro.MicroAttachment;
import dev.maire.nourished.core.micro.MicroConfigFile;
import dev.maire.nourished.core.micro.MicroDef;
import dev.maire.nourished.core.micro.MicroNetworking;
import dev.maire.nourished.core.micro.MicroProfileResolver;
import dev.maire.nourished.core.micro.MicroRegistry;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** /nourished nutrients ... : inspect and edit nutrient levels and per-item nutrient values. */
final class NourishedNutrientCommands {

    private static final SuggestionProvider<CommandSourceStack> NUTRIENTS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(MicroRegistry.server().defs().keySet(), builder);

    private NourishedNutrientCommands() {}

    static LiteralArgumentBuilder<CommandSourceStack> build(CommandBuildContext context) {
        return Commands.literal("nutrients")
                .executes(ctx -> levels(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                .then(Commands.literal("levels")
                        .then(Commands.argument("player", EntityArgument.player())
                                .requires(s -> s.hasPermission(2))
                                .executes(ctx -> levels(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("item")
                        .executes(ctx -> describe(ctx.getSource(), ctx.getSource().getPlayerOrException().getMainHandItem()))
                        .then(Commands.argument("item", ItemArgument.item(context))
                                .executes(ctx -> describe(ctx.getSource(),
                                        ItemArgument.getItem(ctx, "item").createItemStack(1, false)))))
                .then(Commands.literal("set")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("nutrient", StringArgumentType.word()).suggests(NUTRIENTS)
                                        .then(Commands.argument("level", FloatArgumentType.floatArg(0f, 1f))
                                                .executes(NourishedNutrientCommands::setLevel)))))
                .then(Commands.literal("reset")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> reset(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("value")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.literal("set")
                                .then(Commands.argument("item", ItemArgument.item(context))
                                        .then(Commands.argument("nutrient", StringArgumentType.word()).suggests(NUTRIENTS)
                                                .then(Commands.argument("amount", FloatArgumentType.floatArg(0f, 10f))
                                                        .executes(NourishedNutrientCommands::setValue)))))
                        .then(Commands.literal("clear")
                                .then(Commands.argument("item", ItemArgument.item(context))
                                        .executes(NourishedNutrientCommands::clearValue))))
                .then(Commands.literal("source")
                        .requires(s -> s.hasPermission(2))
                        .then(sourceEdit("pin", "pin", true))
                        .then(sourceEdit("unpin", "pin", false))
                        .then(sourceEdit("hide", "hide", true))
                        .then(sourceEdit("unhide", "hide", false))
                        .then(Commands.literal("list")
                                .then(Commands.argument("nutrient", StringArgumentType.word()).suggests(NUTRIENTS)
                                        .executes(NourishedNutrientCommands::listSources))))
                .then(Commands.literal("reload")
                        .requires(s -> s.hasPermission(2))
                        .executes(ctx -> {
                            MicroConfigFile.reload(ctx.getSource().getServer());
                            ctx.getSource().sendSuccess(() -> Component.literal("Reloaded micros.json"), true);
                            return 1;
                        }));
    }

    /** {@code source <verb> <nutrient> <item id or #tag>}: edits the Best sources pin/hide lists in micros.json. */
    private static LiteralArgumentBuilder<CommandSourceStack> sourceEdit(String verb, String list, boolean add) {
        return Commands.literal(verb)
                .then(Commands.argument("nutrient", StringArgumentType.word()).suggests(NUTRIENTS)
                        .then(Commands.argument("entry", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    String key = nutrient(ctx);
                                    if (key == null) return 0;
                                    String entry = StringArgumentType.getString(ctx, "entry").trim();
                                    String id = entry.startsWith("#") ? entry.substring(1) : entry;
                                    if (ResourceLocation.tryParse(id) == null) {
                                        ctx.getSource().sendFailure(Component.literal("Not an item id or #tag: " + entry));
                                        return 0;
                                    }
                                    try {
                                        boolean changed = MicroConfigFile.editSource(ctx.getSource().getServer(), key, list, entry, add);
                                        ctx.getSource().sendSuccess(() -> Component.literal(changed
                                                ? verb + " " + entry + " for " + key
                                                : "No change: " + entry + (add ? " is already" : " isn't") + " in " + key + "'s " + list + " list"), true);
                                        return changed ? 1 : 0;
                                    } catch (IOException ex) {
                                        ctx.getSource().sendFailure(Component.literal("Could not write micros.json: " + ex.getMessage()));
                                        return 0;
                                    }
                                })));
    }

    private static int listSources(CommandContext<CommandSourceStack> ctx) {
        String key = nutrient(ctx);
        if (key == null) return 0;
        var rule = MicroRegistry.server().sourceRule(key);
        ctx.getSource().sendSuccess(() -> Component.literal(key + " best sources\n pinned: "
                + (rule.pin().isEmpty() ? "-" : String.join(", ", rule.pin()))
                + "\n hidden: " + (rule.hide().isEmpty() ? "-" : String.join(", ", rule.hide()))), false);
        return 1;
    }

    private static int levels(CommandSourceStack source, ServerPlayer player) {
        Map<String, Float> levels = MicroAttachment.get(player);
        StringBuilder sb = new StringBuilder(player.getGameProfile().getName()).append("'s nutrients:");
        for (MicroDef def : MicroRegistry.server().defs().values()) {
            sb.append("\n ").append(def.displayName()).append(": ").append(percent(levels.getOrDefault(def.key(), 0f)));
        }
        source.sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    private static int describe(CommandSourceStack source, ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        MicroProfileResolver.Profile profile = MicroProfileResolver.resolveProfile(stack, source.getLevel());
        if (profile.values().isEmpty()) {
            source.sendSuccess(() -> Component.literal(id + " has no nutrients"), false);
            return 0;
        }
        StringBuilder sb = new StringBuilder(id.toString()).append(" (")
                .append(profile.source().name().toLowerCase(Locale.ROOT)).append("):");
        Map<String, MicroDef> defs = MicroRegistry.server().defs();
        profile.values().forEach((k, v) -> {
            MicroDef def = defs.get(k);
            sb.append("\n ").append(def != null ? def.displayName() : k).append(": ")
                    .append(String.format(Locale.ROOT, "%.2f", v));
        });
        source.sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    private static int setLevel(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        String key = nutrient(ctx);
        if (key == null) return 0;
        float level = FloatArgumentType.getFloat(ctx, "level");
        Map<String, Float> levels = new HashMap<>(MicroAttachment.get(player));
        levels.put(key, level);
        MicroAttachment.set(player, levels);
        MicroNetworking.syncLevels(player);
        ctx.getSource().sendSuccess(() -> Component.literal("Set " + key + " to " + percent(level)
                + " for " + player.getGameProfile().getName()), true);
        return 1;
    }

    private static int reset(CommandSourceStack source, ServerPlayer player) {
        MicroAttachment.set(player, Map.of());
        MicroNetworking.syncLevels(player);
        source.sendSuccess(() -> Component.literal("Reset nutrients for " + player.getGameProfile().getName()), true);
        return 1;
    }

    private static int setValue(CommandContext<CommandSourceStack> ctx) {
        String key = nutrient(ctx);
        if (key == null) return 0;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(ItemArgument.getItem(ctx, "item").getItem());
        float amount = FloatArgumentType.getFloat(ctx, "amount");
        Map<String, Float> existing = MicroConfigFile.itemEntry(id);
        Map<String, Float> values = existing != null ? new LinkedHashMap<>(existing) : new LinkedHashMap<>();
        values.put(key, amount);
        return write(ctx.getSource(), id, values, "Set " + key + " = " + amount + " on " + id);
    }

    private static int clearValue(CommandContext<CommandSourceStack> ctx) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(ItemArgument.getItem(ctx, "item").getItem());
        return write(ctx.getSource(), id, null, "Cleared micros.json values for " + id);
    }

    private static int write(CommandSourceStack source, ResourceLocation id, Map<String, Float> values, String message) {
        try {
            MicroConfigFile.setItem(source.getServer(), id, values);
        } catch (IOException ex) {
            source.sendFailure(Component.literal("Could not write micros.json: " + ex.getMessage()));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(message), true);
        return 1;
    }

    private static String nutrient(CommandContext<CommandSourceStack> ctx) {
        String key = StringArgumentType.getString(ctx, "nutrient");
        if (!MicroRegistry.server().defs().containsKey(key)) {
            ctx.getSource().sendFailure(Component.literal("Unknown nutrient '" + key + "'"));
            return null;
        }
        return key;
    }

    private static String percent(float v) {
        return Math.round(v * 100f) + "%";
    }
}
