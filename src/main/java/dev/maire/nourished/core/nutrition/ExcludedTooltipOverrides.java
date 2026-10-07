package dev.maire.nourished.core.nutrition;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.tooltips.TooltipColorRegistry;
import dev.marie.framework.tooltips.TooltipMessageRegistry;
import dev.marie.framework.util.MarieValidation;
import dev.maire.nourished.core.Nourished;
import net.neoforged.fml.loading.FMLPaths;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Read/write access to the per-item {@code byItem} entries of MariesLib's own
 * {@code tooltip_messages.json}/{@code tooltip_colors.json} (see TOOLTIP_MESSAGES_README.md/
 * TOOLTIP_COLORS_README.md), for the item editor's Exclude Tooltip page. Neither {@link
 * TooltipMessageRegistry} nor {@link TooltipColorRegistry} exposes a write method for that tier
 * (by design — it's meant for hand/modpack editing), so this writes the same file format directly
 * and calls their {@code reload} to bust the in-memory cache, rather than forking a parallel
 * storage layer or editing MariesLib itself.
 *
 * <p>Note: a {@code byItem} entry is a single flat override for that item, not scoped to the
 * "excluded" key specifically — see the override stack docs in either README. In practice this is
 * harmless here, since an excluded item never simultaneously shows a nutrient-value tooltip line
 * for the same item, but it means the override set here would also resurface if the item were
 * later un-excluded while a nutrient-tooltip message/color lookup ran for it.
 */
@ApiStatus.Internal
public final class ExcludedTooltipOverrides {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ExcludedTooltipOverrides() {}

    public static Optional<String> getMessageOverride(String itemId) {
        return readByItem(messagesFile(), itemId);
    }

    public static void setMessageOverride(String itemId, @Nullable String message) {
        writeByItem(messagesFile(), itemId, message != null && !message.isBlank() ? message.trim() : null);
        TooltipMessageRegistry.reload(Nourished.MODID);
    }

    public static Optional<Integer> getColorOverride(String itemId) {
        return readByItem(colorsFile(), itemId).flatMap(ExcludedTooltipOverrides::parseHex);
    }

    public static void setColorOverride(String itemId, @Nullable Integer rgb) {
        writeByItem(colorsFile(), itemId, rgb != null ? String.format("#%06X", rgb & 0xFFFFFF) : null);
        TooltipColorRegistry.reload(Nourished.MODID);
    }

    private static Path tooltipsDir() {
        return FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID).resolve("tooltips");
    }

    private static Path messagesFile() {
        return tooltipsDir().resolve("tooltip_messages.json");
    }

    private static Path colorsFile() {
        return tooltipsDir().resolve("tooltip_colors.json");
    }

    private static Optional<String> readByItem(Path file, String itemId) {
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        try (Reader r = Files.newBufferedReader(file)) {
            JsonObject root = GSON.fromJson(r, JsonObject.class);
            if (root == null || !root.has("byItem") || !root.get("byItem").isJsonObject()) {
                return Optional.empty();
            }
            JsonObject byItem = root.getAsJsonObject("byItem");
            if (!byItem.has(itemId) || !byItem.get(itemId).isJsonPrimitive()) {
                return Optional.empty();
            }
            String value = byItem.get(itemId).getAsString();
            return value == null || value.isBlank() ? Optional.empty() : Optional.of(value);
        } catch (IOException | RuntimeException e) {
            Nourished.LOGGER.warn("[ExcludedTooltipOverrides] Failed to read {}", file, e);
            return Optional.empty();
        }
    }

    private static void writeByItem(Path file, String itemId, @Nullable String value) {
        try {
            Files.createDirectories(file.getParent());
            JsonObject root = null;
            if (Files.exists(file)) {
                try (Reader r = Files.newBufferedReader(file)) {
                    root = GSON.fromJson(r, JsonObject.class);
                } catch (RuntimeException e) {
                    Nourished.LOGGER.warn("[ExcludedTooltipOverrides] {} failed to parse, rewriting from scratch", file, e);
                }
            }
            if (root == null) {
                root = new JsonObject();
            }
            if (!root.has("byKey") || !root.get("byKey").isJsonObject()) {
                root.add("byKey", new JsonObject());
            }
            if (!root.has("byItem") || !root.get("byItem").isJsonObject()) {
                root.add("byItem", new JsonObject());
            }
            JsonObject byItem = root.getAsJsonObject("byItem");
            if (value != null) {
                byItem.addProperty(itemId, value);
            } else {
                byItem.remove(itemId);
            }
            MarieValidation.assertPathUnder(file, FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID), "ExcludedTooltipOverrides");
            try (Writer w = Files.newBufferedWriter(file)) {
                GSON.toJson(root, w);
            }
        } catch (IOException e) {
            Nourished.LOGGER.error("[ExcludedTooltipOverrides] Failed to write {}", file, e);
        }
    }

    private static Optional<Integer> parseHex(String raw) {
        String s = raw.trim();
        try {
            if (s.startsWith("0x") || s.startsWith("0X")) {
                s = s.substring(2);
            } else if (s.startsWith("#")) {
                s = s.substring(1);
            }
            if (s.length() == 6) {
                return Optional.of((int) (Long.parseLong(s, 16) & 0xFFFFFF));
            }
            if (s.length() == 8) {
                return Optional.of((int) (Long.parseLong(s, 16) & 0xFFFFFF));
            }
        } catch (NumberFormatException ignored) {
        }
        return Optional.empty();
    }
}
