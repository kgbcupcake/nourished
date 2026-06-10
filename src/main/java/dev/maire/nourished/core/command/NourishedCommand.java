package dev.maire.nourished.core.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.marie.MariesLib.api.MarieEvents;
import dev.marie.MariesLib.api.registry.ProfileRegistry;
import dev.maire.nourished.config.NourishedConfig;
import dev.marie.MariesLib.data.DatapackDiagnostic;
import dev.marie.MariesLib.data.DatapackDiagnostics;
import dev.marie.MariesLib.data.SchemaDefinition;
import dev.marie.MariesLib.datapack.SchemaTemplateGenerator;
import dev.marie.MariesLib.tracking.TrackingAttachment;
import dev.marie.MariesLib.tracking.TrackingData;
import dev.maire.nourished.core.effect.NutritionEffectApplier;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.network.ModNetworking;
import dev.maire.nourished.core.network.sync.NourishedSyncHandler;
import dev.maire.nourished.core.network.sync.SyncNourishedConfigSnapshot;
import dev.maire.nourished.core.nutrition.FoodNutritionRegistry;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import dev.maire.nourished.core.nutrition.RuntimeFoodResolver;
import dev.maire.nourished.core.handler.ConfigReloadHandler;
import dev.maire.nourished.core.reload.NourishedReloadPipeline;
import dev.marie.MariesLib.util.MarieRegistryUtils;
import dev.marie.MariesLib.util.MarieValidation;
import dev.maire.nourished.modules.RawFood.rawInfo.CookedVersionResolver;
import dev.maire.nourished.modules.RawFood.rawInfo.RawFoodClassifier;
import dev.marie.MariesLib.scanner.ItemScanner;
import dev.maire.nourished.core.nutrition.ClassifiedSourceCollector;
import dev.marie.MariesLib.scanner.analysis.MultiValueAnalysisPipeline;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class NourishedCommand {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int MAX_DIAGNOSTICS_LINES = 100;
    private static final Map<UUID, String> ACTIVE_PROFILES = new ConcurrentHashMap<>();

    private static final SuggestionProvider<CommandSourceStack> NUTRIENT_SUGGESTIONS =
            (ctx, builder) -> SharedSuggestionProvider.suggest(NutrientRegistry.getKeys(), builder);

    private static final SuggestionProvider<CommandSourceStack> PROFILE_SUGGESTIONS =
            (ctx, builder) -> SharedSuggestionProvider.suggest(
                    ProfileRegistry.getAll().stream().map(p -> p.getId()),
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
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp) {
            ACTIVE_PROFILES.remove(sp.getUUID());
        }
    }

    private static final AtomicBoolean RESET_SNAPSHOT_WARN_ONCE = new AtomicBoolean(false);

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        ACTIVE_PROFILES.clear();
        RESET_SNAPSHOT_WARN_ONCE.set(false);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(
                Commands.literal("nourished")
                        .then(Commands.literal("nbt")
                                .executes(this::showNbtPaths))
                        .then(Commands.literal("get_unassigned_foods")
                                .executes(this::showUnassignedFoods))
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
                        .then(Commands.literal("invalidatecache")
                                .requires(s -> s.hasPermission(2))
                                .executes(this::invalidateCache))
                        .then(Commands.literal("repair_generated_datapack")
                                .requires(s -> s.hasPermission(2))
                                .executes(this::repairGeneratedDatapack))
                        .then(Commands.literal("diagnostics")
                                .executes(this::showDiagnostics))
                        .then(Commands.literal("scan_analysis")
                                .requires(s -> s.hasPermission(2))
                                .executes(this::runScanAnalysis))
                        .then(Commands.literal("schema")
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .suggests(SCHEMA_TYPE_SUGGESTIONS)
                                        .executes(this::showSchemaTemplate)))
                        .then(Commands.literal("debug")
                                .requires(s -> s.hasPermission(2))
                                .then(NourishedDebugCommand.registerCache())
                                .then(NourishedDebugCommand.registerHeld())
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(this::debugTarget)))
        );
    }

    private int invalidateCache(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        RuntimeFoodResolver.getInstance().invalidateCache();
        FoodNutritionRegistry.clearScannerClassifications();
        RawFoodClassifier.invalidate();
        CookedVersionResolver.invalidate();
        ItemScanner.scanAndApply(source.getServer().getRecipeManager());
        source.sendSuccess(() -> Component.literal("Nourished cache invalidated. Scanner will reapply on next tick."), false);
        return 1;
    }

    private int repairGeneratedDatapack(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        MinecraftServer server = source.getServer();
        Path worldRoot = server.getWorldPath(LevelResource.ROOT).normalize();
        Path packRoot = server.getWorldPath(LevelResource.DATAPACK_DIR).resolve("nourished-generated").normalize();
        Path tagsDir = packRoot.resolve("data").resolve(Nourished.MODID).resolve("tags").resolve("item").resolve("nutrients");

        try {
            MarieValidation.assertPathUnder(packRoot, worldRoot, "repairGeneratedDatapack");
            if (!Files.isDirectory(tagsDir)) {
                source.sendSuccess(() -> Component.literal("No nourished-generated datapack nutrient tags found."), false);
                return 1;
            }

            RepairSummary summary = repairGeneratedTagDirectory(tagsDir);
            source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                    "Repaired nourished-generated datapack: %d files scanned, %d entries made optional, %d retired files deleted. Run /reload before retesting tags.",
                    summary.filesScanned(), summary.entriesConverted(), summary.filesDeleted())).withStyle(ChatFormatting.GREEN), false);
            return 1;
        } catch (IOException | IllegalArgumentException | com.google.gson.JsonParseException ex) {
            Nourished.LOGGER.error("[Nourished] Failed to repair generated datapack", ex);
            source.sendFailure(Component.literal("Failed to repair nourished-generated datapack: " + ex.getMessage()));
            return 0;
        }
    }

    private static RepairSummary repairGeneratedTagDirectory(Path tagsDir) throws IOException {
        Set<String> activeFiles = NutrientRegistry.getKeys().stream()
                .map(key -> key + ".json")
                .collect(java.util.stream.Collectors.toSet());
        int filesScanned = 0;
        int entriesConverted = 0;
        int filesDeleted = 0;

        try (var stream = Files.list(tagsDir)) {
            for (Path tagFile : stream.toList()) {
                String fileName = tagFile.getFileName().toString();
                if (!Files.isRegularFile(tagFile) || !fileName.endsWith(".json")) {
                    continue;
                }
                if (!activeFiles.contains(fileName)) {
                    Files.delete(tagFile);
                    filesDeleted++;
                    continue;
                }

                filesScanned++;
                JsonObject root = JsonParser.parseString(Files.readString(tagFile, StandardCharsets.UTF_8)).getAsJsonObject();
                JsonArray values = root.has("values") && root.get("values").isJsonArray()
                        ? root.getAsJsonArray("values")
                        : new JsonArray();
                JsonArray repairedValues = new JsonArray();
                boolean changed = false;

                for (JsonElement value : values) {
                    if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                        JsonObject optionalEntry = new JsonObject();
                        optionalEntry.addProperty("id", value.getAsString());
                        optionalEntry.addProperty("required", false);
                        repairedValues.add(optionalEntry);
                        entriesConverted++;
                        changed = true;
                    } else if (value.isJsonObject()) {
                        JsonObject entry = value.getAsJsonObject();
                        if (entry.has("id")) {
                            boolean needsRequired = !entry.has("required") || entry.get("required").getAsBoolean();
                            if (needsRequired) {
                                entry.addProperty("required", false);
                                entriesConverted++;
                                changed = true;
                            }
                        }
                        repairedValues.add(entry);
                    } else {
                        repairedValues.add(value);
                    }
                }

                if (!root.has("replace") || root.get("replace").getAsBoolean()) {
                    root.addProperty("replace", false);
                    changed = true;
                }
                if (changed) {
                    root.add("values", repairedValues);
                    Files.writeString(tagFile, GSON.toJson(root), StandardCharsets.UTF_8);
                }
            }
        }

        return new RepairSummary(filesScanned, entriesConverted, filesDeleted);
    }

    private record RepairSummary(int filesScanned, int entriesConverted, int filesDeleted) {}

    private int reportSelf(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = ctx.getSource().getPlayerOrException();
        return sendReport(ctx.getSource(), target);
    }

    private int showNbtPaths(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String header = "[Nourished] NBT paths for Peak Stamina compat:";
        player.sendSystemMessage(copyableGreenLine(header));

        for (String key : NutrientRegistry.getKeys()) {
            String path = TrackingAttachment.getValueNbtPath(key);
            player.sendSystemMessage(copyableGreenLine(path));
        }

        player.sendSystemMessage(copyableGreenLine(TrackingAttachment.getTotalNbtPath()));
        return 1;
    }

    private int showUnassignedFoods(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemScanner.scanAndApplyNow(ctx.getSource().getServer().getRecipeManager());
        Map<String, List<ResourceLocation>> byNamespace = new TreeMap<>();

        for (Item item : BuiltInRegistries.ITEM) {
            if (item.components().get(net.minecraft.core.component.DataComponents.FOOD) == null) {
                continue;
            }
            ResourceLocation itemId = item.builtInRegistryHolder().key().location();
            if (isClassified(itemId, item.builtInRegistryHolder())) {
                continue;
            }
            byNamespace.computeIfAbsent(itemId.getNamespace(), k -> new ArrayList<>()).add(itemId);
        }

        if (byNamespace.isEmpty()) {
            player.sendSystemMessage(Component.literal("[Nourished] All loaded food items are classified.").withStyle(ChatFormatting.GREEN));
            return 1;
        }

        int totalCount = byNamespace.values().stream().mapToInt(List::size).sum();
        Path outputPath = FMLPaths.CONFIGDIR.get().resolve("nourished/unassigned_foods.txt");
        try {
            Files.createDirectories(outputPath.getParent());
            List<String> lines = new ArrayList<>();
            lines.add("[Nourished] Total unassigned foods: " + totalCount);
            lines.add("");
            for (Map.Entry<String, List<ResourceLocation>> entry : byNamespace.entrySet()) {
                List<ResourceLocation> ids = entry.getValue();
                ids.sort(Comparator.comparing(ResourceLocation::toString));
                lines.add("[" + entry.getKey() + "]");
                for (ResourceLocation id : ids) {
                    lines.add(id.toString());
                }
                lines.add("");
            }
            Files.write(outputPath, lines, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            Nourished.LOGGER.error("[Nourished] Failed to write unassigned foods file", ex);
            player.sendSystemMessage(Component.literal("[Nourished] Failed to write unassigned foods file.").withStyle(ChatFormatting.RED));
            return 0;
        }

        player.sendSystemMessage(
                Component.literal("[Nourished] Written " + totalCount + " unassigned foods to config/nourished/unassigned_foods.txt.")
                        .withStyle(ChatFormatting.GREEN)
        );
        return 1;
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
        try {
            MarieRegistryUtils.requireValueKey(key, "nourished set nutrient command");
        } catch (IllegalArgumentException e) {
            ctx.getSource().sendFailure(Component.literal("Unknown nutrient key: " + key));
            return 0;
        }
        float value = FloatArgumentType.getFloat(ctx, "value");
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        TrackingData data = player.getData(TrackingAttachment.TRACKING.get());
        float old = data.values.getOrDefault(key, 0f);
        data.values.put(key, value);
        player.setData(TrackingAttachment.TRACKING.get(), data);
        ModNetworking.syncDiet(player, data);
        NutritionEffectApplier.apply(player, data);
        NeoForge.EVENT_BUS.post(new MarieEvents.ValueChangedEvent(player, key, old, value));
        ctx.getSource().sendSuccess(() -> Component.literal(String.format(Locale.ROOT, "Set %s for %s: %.2f -> %.2f", key, player.getName().getString(), old, value)), true);
        return 1;
    }

    private int resetPlayer(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        TrackingData data = player.getData(TrackingAttachment.TRACKING.get());
        SyncNourishedConfigSnapshot snapshot = NourishedSyncHandler.getConfigSnapshot();
        float start;
        if (snapshot != null) {
            start = (float) snapshot.startingNutrientValue();
        } else {
            if (RESET_SNAPSHOT_WARN_ONCE.compareAndSet(false, true)) {
                Nourished.LOGGER.warn(
                        "[Nourished] NourishedCommand.resetPlayer: snapshot null, falling back to raw config. Will not warn again until server restart.");
            }
            start = (float) NourishedConfig.get().startingNutrientValue();
        }
        for (String key : NutrientRegistry.getKeys()) {
            float old = data.values.getOrDefault(key, 0f);
            data.values.put(key, start);
            NeoForge.EVENT_BUS.post(new MarieEvents.ValueChangedEvent(player, key, old, start));
        }
        player.setData(TrackingAttachment.TRACKING.get(), data);
        ctx.getSource().sendSuccess(() -> Component.literal("Reset nutrients for " + player.getName().getString()), true);
        return 1;
    }

    private int profileList(CommandContext<CommandSourceStack> ctx) {
        List<String> names = ProfileRegistry.getAll().stream().map(p -> p.getId() + " (" + p.getDisplayName() + ")").toList();
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
                NourishedReloadPipeline.reloadAll();
                ConfigReloadHandler.reloadAndBroadcast(server);
                source.sendSuccess(() -> Component.literal("Nourished data reload complete."), true);
            });
        });
        return 1;
    }

    private int debugTarget(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        TrackingData data = target.getData(TrackingAttachment.TRACKING.get());

        JsonObject root = new JsonObject();
        root.addProperty("player", target.getGameProfile().getName());
        root.addProperty("calories", data.total);
        root.addProperty("maxCalories", data.maxTotal);
        root.addProperty("activeProfile", activeProfile(target));

        JsonObject nutrients = new JsonObject();
        for (Map.Entry<String, Float> entry : new LinkedHashMap<>(data.values).entrySet()) {
            nutrients.addProperty(entry.getKey(), entry.getValue());
        }
        root.add("nutrients", nutrients);

        String[] lines = GSON.toJson(root).split("\n");
        ctx.getSource().sendSuccess(() -> Component.literal("Raw TrackingData for " + target.getName().getString() + ":").withStyle(ChatFormatting.GOLD), false);
        for (String line : lines) {
            ctx.getSource().sendSuccess(() -> Component.literal(line).withStyle(ChatFormatting.GRAY), false);
        }
        return 1;
    }

    private int runScanAnalysis(CommandContext<CommandSourceStack> ctx) {
        MultiValueAnalysisPipeline.runFullRegistry(
                ClassifiedSourceCollector.collectAllClassifiedSources(), 0.15f, 0.35f, 0.10f);
        String outputPath = "config/" + Nourished.MODID + "/scanner_analysis/";
        ctx.getSource().sendSuccess(
                () -> Component.literal("Multi-nutrient analysis written to " + outputPath)
                        .withStyle(ChatFormatting.GREEN),
                true);
        return 1;
    }

    private int showDiagnostics(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        DatapackDiagnostics diagnostics = DatapackDiagnostics.getInstance();
        source.sendSuccess(() -> Component.literal(diagnostics.getSummary()).withStyle(ChatFormatting.GOLD), false);

        List<DatapackDiagnostic> allDiagnostics = diagnostics.getAll();
        int shown = 0;
        for (DatapackDiagnostic diagnostic : allDiagnostics) {
            if (shown >= MAX_DIAGNOSTICS_LINES) {
                break;
            }
            ChatFormatting color = diagnostic.severity() == DatapackDiagnostic.Severity.ERROR
                    ? ChatFormatting.RED
                    : ChatFormatting.YELLOW;
            source.sendSuccess(() -> Component.literal(diagnostic.toString()).withStyle(color), false);
            shown++;
        }

        int hidden = allDiagnostics.size() - shown;
        if (hidden > 0) {
            source.sendSuccess(
                    () -> Component.literal("... truncated " + hidden + " additional diagnostics")
                            .withStyle(ChatFormatting.DARK_GRAY),
                    false
            );
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
            case "values" -> SchemaDefinition::forValue;
            case "source_classifications" -> SchemaDefinition::forSourceClassification;
            case "effects" -> SchemaDefinition::forEffect;
            case "synergies" -> SchemaDefinition::forSynergy;
            case "source_synergies" -> SchemaDefinition::forSourcePairSynergy;
            case "milestones" -> SchemaDefinition::forMilestone;
            case "tracking_profiles" -> SchemaDefinition::forTrackingProfile;
            case "compat" -> SchemaDefinition::forCompat;
            default -> null;
        };
    }

    private int sendReport(CommandSourceStack source, ServerPlayer target) {
        if (!NourishedCommandSource.canTargetOtherPlayer(source, target)) {
            source.sendFailure(Component.literal("You do not have permission to target another player."));
            return 0;
        }
        TrackingData data = target.getData(TrackingAttachment.TRACKING.get());
        SyncNourishedConfigSnapshot snapshot = NourishedSyncHandler.getConfigSnapshot();
        if (snapshot == null) {
            source.sendSystemMessage(Component.literal("[Nourished] Server config not yet synchronized; showing defaults."));
        }
        List<Component> lines = snapshot != null
                ? NourishedCommandSource.buildReportLines(target, data, activeProfile(target), snapshot)
                : NourishedCommandSource.buildReportLines(target, data, activeProfile(target), NourishedConfig.get());
        for (Component line : lines) {
            source.sendSuccess(() -> line, false);
        }
        return 1;
    }

    private int sendNutrientDetail(CommandSourceStack source, String key, ServerPlayer target) {
        try {
            MarieRegistryUtils.requireValueKey(key, "nourished nutrient command");
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal("Unknown nutrient key: " + key));
            return 0;
        }
        if (!NourishedCommandSource.canTargetOtherPlayer(source, target)) {
            source.sendFailure(Component.literal("You do not have permission to target another player."));
            return 0;
        }

        TrackingData data = target.getData(TrackingAttachment.TRACKING.get());
        SyncNourishedConfigSnapshot snapshot = NourishedSyncHandler.getConfigSnapshot();
        if (snapshot == null) {
            source.sendSystemMessage(Component.literal("[Nourished] Server config not yet synchronized; showing defaults."));
        }
        float value = data.values.getOrDefault(key, 0f);
        float decay = snapshot != null
                ? (float) snapshot.decayRateFor(key)
                : (float) NourishedConfig.get().decayRateFor(key);
        float critical = snapshot != null
                ? (float) snapshot.criticalThreshold()
                : (float) NourishedConfig.get().criticalThresholdFor(key);
        float low = snapshot != null
                ? (float) snapshot.lowThreshold()
                : (float) NourishedConfig.get().lowThreshold();
        float excess = snapshot != null
                ? (float) snapshot.excessThreshold()
                : (float) NourishedConfig.get().excessThreshold();
        Component chip = snapshot != null
                ? NourishedCommandSource.statusChip(NourishedCommandSource.statusFor(value, key, snapshot))
                : NourishedCommandSource.statusChip(NourishedCommandSource.statusFor(value, key, NourishedConfig.get()));

        source.sendSuccess(() -> Component.literal("Nutrient: " + key + " (" + target.getName().getString() + ")").withStyle(ChatFormatting.GOLD), false);
        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT, "Current: %.2f (%d%%)", value, Math.round(value * 100f))).withStyle(ChatFormatting.WHITE), false);
        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT, "Decay rate: %.4f", decay)).withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT, "Thresholds: critical=%.2f low=%.2f excess=%.2f", critical, low, excess)).withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("Status: ").withStyle(ChatFormatting.GRAY).append(chip), false);
        return 1;
    }

    private int setProfile(CommandSourceStack source, String profile, ServerPlayer target) {
        if (ProfileRegistry.get(profile) == null) {
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

    private static boolean isClassified(ResourceLocation itemId, Holder<Item> holder) {
        Map<String, Float> external = FoodNutritionRegistry.getExternalClassification(itemId);
        if (external != null && !external.isEmpty()) {
            return true;
        }
        for (NutrientRegistry.NutrientDef def : NutrientRegistry.getAll()) {
            for (String tagStr : def.tags()) {
                TagKey<Item> tagKey = MarieRegistryUtils.itemTagKey(tagStr);
                if (holder.is(tagKey)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Component copyableGreenLine(String text) {
        return Component.literal(text).withStyle(style -> style
                .withColor(ChatFormatting.GREEN)
                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, text)));
    }
}
