package dev.maire.nourished.templates;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.brigadier.context.CommandContext;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.effect.EffectRegistry;
import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.data.DatapackSchema;
import dev.marie.MariesLib.util.MarieValidation;
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
 * Exports current {@link EffectRegistry} entries as Marie datapack effect JSON files under
 * {@code <world>/datapacks/nourished-effects-template/}.
 */
@ApiStatus.Internal
public final class NourishedEffectsTemplateCommand {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String PACK_FOLDER = Nourished.MODID + "-effects-template";

    private NourishedEffectsTemplateCommand() {}

    public static int export(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        MinecraftServer server = source.getServer();
        Path worldRoot = server.getWorldPath(LevelResource.ROOT).normalize();
        Path packRoot = server.getWorldPath(LevelResource.DATAPACK_DIR).resolve(PACK_FOLDER).normalize();

        try {
            MarieValidation.assertPathUnder(packRoot, worldRoot, "exportEffectsTemplate");

            Path effectsDir = packRoot.resolve("data")
                    .resolve(Nourished.MODID)
                    .resolve(DatapackSchema.root())
                    .resolve(DatapackSchema.EFFECTS_DIR);
            Files.createDirectories(effectsDir);

            writePackMeta(packRoot.resolve("pack.mcmeta"));

            List<EffectRegistry.EffectDef> defs = EffectRegistry.getAll();
            for (EffectRegistry.EffectDef def : defs) {
                writeEffectJson(effectsDir.resolve(def.id() + ".json"), def);
            }

            int count = defs.size();
            source.sendSuccess(() -> Component.literal(
                            "Exported " + count + " effect(s) to " + packRoot.toAbsolutePath()
                                    + ". Run /reload to load the datapack.")
                    .withStyle(ChatFormatting.GREEN), false);
            return 1;
        } catch (IOException | IllegalArgumentException ex) {
            source.sendFailure(Component.literal("Failed to export effects template: " + ex.getMessage()));
            return 0;
        }
    }

    private static void writePackMeta(Path path) throws IOException {
        JsonObject root = new JsonObject();
        JsonObject pack = new JsonObject();
        pack.addProperty("pack_format", 48);
        pack.addProperty("description", Nourished.MODID + " effects template (exported from EffectRegistry)");
        root.add("pack", pack);
        Files.writeString(path, GSON.toJson(root), StandardCharsets.UTF_8);
    }

    private static void writeEffectJson(Path path, EffectRegistry.EffectDef def) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty(DatapackSchema.KEY_SCHEMA_VERSION, 1);
        root.addProperty("_comment_value_key",
                "one of the registered nutrient keys — see export_values_template output for the full list");
        root.addProperty(DatapackSchema.KEY_VALUE_KEY, def.nutrient());
        root.addProperty("_comment_threshold",
                "0.0 to 1.0 — normalized nutrient level that triggers this effect");
        root.addProperty(DatapackSchema.KEY_THRESHOLD, def.threshold());
        root.addProperty("_comment_threshold_type",
                "LOW or CRITICAL = triggers when below threshold; EXCESS = triggers when above threshold; "
                        + "BONUS = triggers when above threshold AND all other tracked nutrients are also above their thresholds");
        root.addProperty(DatapackSchema.KEY_THRESHOLD_TYPE, thresholdTypeFromTrigger(def.trigger()));
        root.addProperty("_comment_effect_id",
                "any vanilla or modded status effect id, e.g. minecraft:mining_fatigue");
        root.addProperty(DatapackSchema.KEY_EFFECT_ID, def.effect());
        root.addProperty("_comment_amplifier",
                "0-indexed effect level — 0 = level I, 1 = level II, 2 = level III, etc.");
        root.addProperty(DatapackSchema.KEY_AMPLIFIER, def.amplifier());
        root.addProperty("_comment_duration", "ticks — 20 ticks = 1 second");
        root.addProperty(DatapackSchema.KEY_DURATION, def.durationTicks());
        Files.writeString(path, GSON.toJson(root), StandardCharsets.UTF_8);
    }

    private static String thresholdTypeFromTrigger(String trigger) {
        return switch (trigger) {
            case "above" -> "EXCESS";
            case "all_above" -> "BONUS";
            default -> "LOW";
        };
    }
}
