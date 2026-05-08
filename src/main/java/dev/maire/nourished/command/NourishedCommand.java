package dev.maire.nourished.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.maire.nourished.api.NourishedEvents;
import dev.maire.nourished.api.registry.DietProfileRegistry;
import dev.maire.nourished.config.LockRegistry;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.config.PresetRegistry;
import dev.maire.nourished.color.ColorRegistry;
import dev.maire.nourished.data.DatapackDiagnostic;
import dev.maire.nourished.data.DatapackDiagnostics;
import dev.maire.nourished.data.SchemaDefinition;
import dev.maire.nourished.data.SchemaTemplateGenerator;
import dev.maire.nourished.diet.DietAttachment;
import dev.maire.nourished.diet.DietData;
import dev.maire.nourished.effect.EffectRegistry;
import dev.maire.nourished.nutrition.FoodOverrideRegistry;
import dev.maire.nourished.nutrition.FoodValueRegistry;
import dev.maire.nourished.nutrition.Nourished;
import dev.maire.nourished.nutrition.NutrientRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class NourishedCommand {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<UUID, String> ACTIVE_PROFILES = new ConcurrentHashMap<>();

    private static final SuggestionProvider<CommandSourceStack> NUTRIENT_SUGGESTIONS =
            (ctx, builder) -> SharedSuggestionProvider.suggest(NutrientRegistry.getKeys(), builder);

    private static final SuggestionProvider<CommandSourceStack> PROFILE_SUGGESTIONS =
            (ctx, builder) -> SharedSuggestionProvider.suggest(
                    DietProfileRegistry.getAll().stream().map(p -> p.getId()),
                    builder
            );

    private static final SuggestionProvider<CommandSourceStack> SCHEMA_TYPE_SUGGESTIONS =
            (ctx, builder) -> SharedSuggestionProvider.suggest(List.of(
                    "nutrients",
                    "food_classifications",
                    "effects",
                    "synergies",
                    "food_synergies",
                    "milestones",
                    "diet_profiles",
                    "compat"
            ), builder);

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(
                Commands.literal("nourished")
                        .then(Commands.literal("report")
                                .executes(this::reportSelf)
                                .then(Commands.argument("player", EntityArgument.player())
                                        .requires(s -> s.hasPermission(2))
                                        .executes(this::reportTarget)))
                        .then(Commands.literal("nutrient")
                                .then(Commands.argument("key", StringArgumentType.word())
                                        .suggests(NUTRIENT_SUGGESTIONS)
                                        .executes(this::nutrientSelf)
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .requires(s -> s.hasPermission(2))
                                                .executes(this::nutrientTarget))))
                        .then(Commands.literal("set")
                                .requires(s -> s.hasPermission(2))
                                .then(Commands.argument("key", StringArgumentType.word())
                                        .suggests(NUTRIENT_SUGGESTIONS)
                                        .then(Commands.argument("value", FloatArgumentType.floatArg(0f, 1f))
                                                .then(Commands.argument("player", EntityArgument.player())
                                                        .executes(this::setNutrient)))))
                        .then(Commands.literal("reset")
                                .requires(s -> s.hasPermission(2))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(this::resetPlayer)))
                        .then(Commands.literal("profile")
                                .then(Commands.literal("list").executes(this::profileList))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("profile", StringArgumentType.word())
                                                .suggests(PROFILE_SUGGESTIONS)
                                                .executes(this::profileSetSelf)
                                                .then(Commands.argument("player", EntityArgument.player())
                                                        .requires(s -> s.hasPermission(2))
                                                        .executes(this::profileSetTarget))))
                                .then(Commands.literal("get")
                                        .executes(this::profileGetSelf)
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .requires(s -> s.hasPermission(2))
                                                .executes(this::profileGetTarget))))
                        .then(Commands.literal("reload")
                                .requires(s -> s.hasPermission(2))
                                .executes(this::reloadAll))
                        .then(Commands.literal("diagnostics")
                                .executes(this::showDiagnostics))
                        .then(Commands.literal("schema")
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .suggests(SCHEMA_TYPE_SUGGESTIONS)
                                        .executes(this::showSchemaTemplate)))
                        .then(Commands.literal("debug")
                                .requires(s -> s.hasPermission(2))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(this::debugTarget)))
        );
    }

    private int reportSelf(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = ctx.getSource().getPlayerOrException();
        return sendReport(ctx.getSource(), target);
    }

    private int reportTarget(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return sendReport(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"));
    }

    private int nutrientSelf(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return sendNutrientDetail(ctx.getSource(), StringArgumentType.getString(ctx, "key"), ctx.getSource().getPlayerOrException());
    }

    private int nutrientTarget(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return sendNutrientDetail(ctx.getSource(), StringArgumentType.getString(ctx, "key"), EntityArgument.getPlayer(ctx, "player"));
    }

    private int setNutrient(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String key = StringArgumentType.getString(ctx, "key");
        if (!NutrientRegistry.getKeys().contains(key)) {
            ctx.getSource().sendFailure(Component.literal("Unknown nutrient key: " + key));
            return 0;
        }
        float value = FloatArgumentType.getFloat(ctx, "value");
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        DietData data = player.getData(DietAttachment.DIET.get());
        float old = data.nutrients.getOrDefault(key, 0f);
        data.nutrients.put(key, value);
        player.setData(DietAttachment.DIET.get(), data);
        NeoForge.EVENT_BUS.post(new NourishedEvents.NutrientChangedEvent(player, key, old, value));
        ctx.getSource().sendSuccess(() -> Component.literal(String.format(Locale.ROOT, "Set %s for %s: %.2f -> %.2f", key, player.getName().getString(), old, value)), true);
        return 1;
    }

    private int resetPlayer(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        DietData data = player.getData(DietAttachment.DIET.get());
        float start = (float) NourishedConfig.get().startingNutrientValue();
        for (String key : NutrientRegistry.getKeys()) {
            float old = data.nutrients.getOrDefault(key, 0f);
            data.nutrients.put(key, start);
            NeoForge.EVENT_BUS.post(new NourishedEvents.NutrientChangedEvent(player, key, old, start));
        }
        player.setData(DietAttachment.DIET.get(), data);
        ctx.getSource().sendSuccess(() -> Component.literal("Reset nutrients for " + player.getName().getString()), true);
        return 1;
    }

    private int profileList(CommandContext<CommandSourceStack> ctx) {
        List<String> names = DietProfileRegistry.getAll().stream().map(p -> p.getId() + " (" + p.getDisplayName() + ")").toList();
        if (names.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal("No diet profiles registered."), false);
            return 1;
        }
        ctx.getSource().sendSuccess(() -> Component.literal("Profiles: " + String.join(", ", names)), false);
        return 1;
    }

    private int profileSetSelf(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return setProfile(ctx.getSource(), StringArgumentType.getString(ctx, "profile"), ctx.getSource().getPlayerOrException());
    }

    private int profileSetTarget(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return setProfile(ctx.getSource(), StringArgumentType.getString(ctx, "profile"), EntityArgument.getPlayer(ctx, "player"));
    }

    private int profileGetSelf(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return getProfile(ctx.getSource(), ctx.getSource().getPlayerOrException());
    }

    private int profileGetTarget(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return getProfile(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"));
    }

    private int reloadAll(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        MinecraftServer server = source.getServer();
        source.sendSuccess(() -> Component.literal("Reloading Nourished data..."), true);
        server.reloadResources(server.getPackRepository().getSelectedIds()).thenRun(() -> {
            server.execute(() -> {
                NutrientRegistry.reload();
                FoodValueRegistry.reload();
                FoodOverrideRegistry.reload();
                EffectRegistry.reload();
                PresetRegistry.reload();
                ColorRegistry.reload();
                LockRegistry.reload();
                source.sendSuccess(() -> Component.literal("Nourished data reload complete."), true);
            });
        });
        return 1;
    }

    private int debugTarget(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        DietData data = target.getData(DietAttachment.DIET.get());

        JsonObject root = new JsonObject();
        root.addProperty("player", target.getGameProfile().getName());
        root.addProperty("calories", data.calories);
        root.addProperty("maxCalories", data.maxCalories);
        root.addProperty("activeProfile", activeProfile(target));

        JsonObject nutrients = new JsonObject();
        for (Map.Entry<String, Float> entry : new LinkedHashMap<>(data.nutrients).entrySet()) {
            nutrients.addProperty(entry.getKey(), entry.getValue());
        }
        root.add("nutrients", nutrients);

        String[] lines = GSON.toJson(root).split("\n");
        ctx.getSource().sendSuccess(() -> Component.literal("Raw DietData for " + target.getName().getString() + ":").withStyle(ChatFormatting.GOLD), false);
        for (String line : lines) {
            ctx.getSource().sendSuccess(() -> Component.literal(line).withStyle(ChatFormatting.GRAY), false);
        }
        return 1;
    }

    private int showDiagnostics(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        DatapackDiagnostics diagnostics = DatapackDiagnostics.getInstance();
        source.sendSuccess(() -> Component.literal(diagnostics.getSummary()).withStyle(ChatFormatting.GOLD), false);

        for (DatapackDiagnostic diagnostic : diagnostics.getAll()) {
            ChatFormatting color = diagnostic.severity() == DatapackDiagnostic.Severity.ERROR
                    ? ChatFormatting.RED
                    : ChatFormatting.YELLOW;
            source.sendSuccess(() -> Component.literal(diagnostic.toString()).withStyle(color), false);
        }
        return 1;
    }

    private int showSchemaTemplate(CommandContext<CommandSourceStack> ctx) {
        String type = StringArgumentType.getString(ctx, "type");
        Supplier<SchemaDefinition> schemaSupplier = schemaSupplier(type);
        if (schemaSupplier == null) {
            ctx.getSource().sendFailure(Component.literal("Unknown schema type: " + type));
            return 0;
        }

        String template = SchemaTemplateGenerator.generate(schemaSupplier.get());
        ctx.getSource().sendSuccess(() -> Component.literal("Schema template for " + type + ":").withStyle(ChatFormatting.GOLD), false);
        for (String line : template.split("\n")) {
            ctx.getSource().sendSuccess(() -> Component.literal(line).withStyle(ChatFormatting.GRAY), false);
        }
        return 1;
    }

    private static Supplier<SchemaDefinition> schemaSupplier(String type) {
        return switch (type) {
            case "nutrients" -> SchemaDefinition::forNutrient;
            case "food_classifications" -> SchemaDefinition::forFoodClassification;
            case "effects" -> SchemaDefinition::forEffect;
            case "synergies" -> SchemaDefinition::forSynergy;
            case "food_synergies" -> SchemaDefinition::forFoodSynergy;
            case "milestones" -> SchemaDefinition::forMilestone;
            case "diet_profiles" -> SchemaDefinition::forDietProfile;
            case "compat" -> SchemaDefinition::forCompat;
            default -> null;
        };
    }

    private int sendReport(CommandSourceStack source, ServerPlayer target) {
        if (!NourishedCommandSource.canTargetOtherPlayer(source, target)) {
            source.sendFailure(Component.literal("You do not have permission to target another player."));
            return 0;
        }
        DietData data = target.getData(DietAttachment.DIET.get());
        List<Component> lines = NourishedCommandSource.buildReportLines(target, data, activeProfile(target), NourishedConfig.get());
        for (Component line : lines) {
            source.sendSuccess(() -> line, false);
        }
        return 1;
    }

    private int sendNutrientDetail(CommandSourceStack source, String key, ServerPlayer target) {
        if (!NutrientRegistry.getKeys().contains(key)) {
            source.sendFailure(Component.literal("Unknown nutrient key: " + key));
            return 0;
        }
        if (!NourishedCommandSource.canTargetOtherPlayer(source, target)) {
            source.sendFailure(Component.literal("You do not have permission to target another player."));
            return 0;
        }

        DietData data = target.getData(DietAttachment.DIET.get());
        NourishedConfig config = NourishedConfig.get();
        float value = data.nutrients.getOrDefault(key, 0f);
        float decay = (float) config.decayRateFor(key);
        float critical = (float) config.criticalThresholdFor(key);
        float low = (float) config.lowThreshold();
        float excess = (float) config.excessThreshold();
        Component chip = NourishedCommandSource.statusChip(NourishedCommandSource.statusFor(value, key, config));

        source.sendSuccess(() -> Component.literal("Nutrient: " + key + " (" + target.getName().getString() + ")").withStyle(ChatFormatting.GOLD), false);
        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT, "Current: %.2f (%d%%)", value, Math.round(value * 100f))).withStyle(ChatFormatting.WHITE), false);
        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT, "Decay rate: %.4f", decay)).withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT, "Thresholds: critical=%.2f low=%.2f excess=%.2f", critical, low, excess)).withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("Status: ").withStyle(ChatFormatting.GRAY).append(chip), false);
        return 1;
    }

    private int setProfile(CommandSourceStack source, String profile, ServerPlayer target) {
        if (DietProfileRegistry.get(profile) == null) {
            source.sendFailure(Component.literal("Unknown profile: " + profile));
            return 0;
        }
        ACTIVE_PROFILES.put(target.getUUID(), profile);
        source.sendSuccess(() -> Component.literal("Set active profile for " + target.getName().getString() + " to " + profile), true);
        return 1;
    }

    private int getProfile(CommandSourceStack source, ServerPlayer target) {
        source.sendSuccess(() -> Component.literal(target.getName().getString() + " active profile: " + activeProfile(target)), false);
        return 1;
    }

    private static String activeProfile(ServerPlayer player) {
        return ACTIVE_PROFILES.getOrDefault(player.getUUID(), "none");
    }
}
