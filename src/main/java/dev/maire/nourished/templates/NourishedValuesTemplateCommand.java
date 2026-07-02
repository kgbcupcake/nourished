package dev.maire.nourished.templates;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.brigadier.context.CommandContext;
import dev.maire.nourished.core.Nourished;
import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.ValueDefinition;
import dev.marie.framework.api.registry.ValueRegistry;
import dev.marie.framework.data.DatapackSchema;
import dev.marie.framework.util.MarieValidation;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Exports current {@link ValueRegistry} entries as Marie datapack value JSON files under
 * {@code <world>/datapacks/nourished-values-template/}.
 */
@ApiStatus.Internal
public final class NourishedValuesTemplateCommand {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String PACK_FOLDER = Nourished.MODID + "-values-template";

    private NourishedValuesTemplateCommand() {}

    public static int export(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        MinecraftServer server = source.getServer();
        Path worldRoot = server.getWorldPath(LevelResource.ROOT).normalize();
        Path packRoot = server.getWorldPath(LevelResource.DATAPACK_DIR).resolve(PACK_FOLDER).normalize();

        try {
            MarieValidation.assertPathUnder(packRoot, worldRoot, "exportValuesTemplate");

            Path valuesDir = packRoot.resolve("data")
                    .resolve(Nourished.MODID)
                    .resolve(DatapackSchema.root())
                    .resolve(DatapackSchema.VALUES_DIR);
            Files.createDirectories(valuesDir);

            writePackMeta(packRoot.resolve("pack.mcmeta"));

            List<ValueDefinition> defs = ValueRegistry.getAll();
            for (ValueDefinition def : defs) {
                writeValueJson(valuesDir.resolve(def.getId() + ".json"), def);
            }

            int count = defs.size();
            source.sendSuccess(() -> Component.literal(
                            "Exported " + count + " value(s) to " + packRoot.toAbsolutePath()
                                    + ". Run /reload to load the datapack.")
                    .withStyle(ChatFormatting.GREEN), false);
            return 1;
        } catch (IOException | IllegalArgumentException ex) {
            source.sendFailure(Component.literal("Failed to export values template: " + ex.getMessage()));
            return 0;
        }
    }

    private static void writePackMeta(Path path) throws IOException {
        JsonObject root = new JsonObject();
        JsonObject pack = new JsonObject();
        pack.addProperty("pack_format", 48);
        pack.addProperty("description", Nourished.MODID + " values template (exported from ValueRegistry)");
        root.add("pack", pack);
        Files.writeString(path, GSON.toJson(root), StandardCharsets.UTF_8);
    }

    private static void writeValueJson(Path path, ValueDefinition def) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty(DatapackSchema.KEY_SCHEMA_VERSION, 1);
        root.addProperty("_comment_display_name", "shown in HUD bars and tooltips");
        root.addProperty(DatapackSchema.KEY_DISPLAY_NAME, def.getDisplayName());
        root.addProperty("_comment_color",
                "packed ARGB hex integer, e.g. 0xFFRRGGBB (0xFF = full opacity)");
        root.addProperty("color", def.getColor());
        root.addProperty("_comment_default_decay_rate",
                "amount lost per tick — e.g. 0.001 means 0.1% of the bar per tick");
        root.addProperty("default_decay_rate", def.getDefaultDecayRate());
        root.addProperty("_comment_critical_threshold", "0.0 to 1.0 — below this is 'critical'");
        root.addProperty("critical_threshold", def.getCriticalThreshold());
        root.addProperty("_comment_low_threshold", "0.0 to 1.0 — below this is 'low'");
        root.addProperty("low_threshold", def.getLowThreshold());
        root.addProperty("_comment_excess_threshold", "0.0 to 1.0 — above this is 'excess'");
        root.addProperty("excess_threshold", def.getExcessThreshold());
        root.addProperty("_comment_amountScale",
                "how many raw consumed-item amount units equal a full bar (1.0)");
        root.addProperty("amountScale", def.getAmountScale());
        Files.writeString(path, GSON.toJson(root), StandardCharsets.UTF_8);
    }
}
