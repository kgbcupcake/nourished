package dev.maire.nourished.templates;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.context.CommandContext;
import dev.maire.nourished.core.Nourished;
import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.ValueDefinition;
import dev.marie.framework.api.registry.ValueRegistry;
import dev.marie.framework.client.config.render.MarieValueColors;
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
import java.util.Locale;

/**
 * Exports resolved HUD/tooltip colors as a datapack {@code config/colors.json} under
 * {@code <world>/datapacks/nourished-colors-template/}.
 */
@ApiStatus.Internal
public final class NourishedColorsTemplateCommand {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String PACK_FOLDER = Nourished.MODID + "-colors-template";

    private NourishedColorsTemplateCommand() {}

    public static int export(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        MinecraftServer server = source.getServer();
        Path worldRoot = server.getWorldPath(LevelResource.ROOT).normalize();
        Path packRoot = server.getWorldPath(LevelResource.DATAPACK_DIR).resolve(PACK_FOLDER).normalize();

        try {
            MarieValidation.assertPathUnder(packRoot, worldRoot, "exportColorsTemplate");

            Path colorsFile = packRoot.resolve("data")
                    .resolve(Nourished.MODID)
                    .resolve("config")
                    .resolve("colors.json");
            Files.createDirectories(colorsFile.getParent());

            writePackMeta(packRoot.resolve("pack.mcmeta"));

            List<ValueDefinition> defs = ValueRegistry.getAll();
            JsonArray arr = new JsonArray();
            for (ValueDefinition def : defs) {
                arr.add(buildColorEntry(def.getId()));
            }
            Files.writeString(colorsFile, GSON.toJson(arr), StandardCharsets.UTF_8);

            int count = defs.size();
            source.sendSuccess(() -> Component.literal(
                            "Exported " + count + " HUD/tooltip color(s) to " + packRoot.toAbsolutePath()
                                    + ". Run /reload to load the datapack.")
                    .withStyle(ChatFormatting.GREEN), false);
            return 1;
        } catch (IOException | IllegalArgumentException ex) {
            source.sendFailure(Component.literal("Failed to export colors template: " + ex.getMessage()));
            return 0;
        }
    }

    private static void writePackMeta(Path path) throws IOException {
        JsonObject root = new JsonObject();
        JsonObject pack = new JsonObject();
        pack.addProperty("pack_format", 48);
        pack.addProperty("description", Nourished.MODID + " HUD & tooltip colors template");
        root.add("pack", pack);
        Files.writeString(path, GSON.toJson(root), StandardCharsets.UTF_8);
    }

    private static JsonObject buildColorEntry(String key) {
        int argb = MarieValueColors.baseColorArgb(key);
        JsonObject obj = new JsonObject();
        obj.addProperty("_comment_key", "nutrient/value key — must match a registered value id");
        obj.addProperty("key", key);
        obj.addProperty("_comment_argb",
                "packed ARGB color for HUD bars and food tooltip lines, e.g. 0xFFRRGGBB "
                        + "(0xFF = full opacity). Overrides nutrient definition colors when loaded.");
        obj.addProperty("argb", String.format(Locale.ROOT, "0x%08X", argb));
        return obj;
    }
}
