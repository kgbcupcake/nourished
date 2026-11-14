package dev.maire.nourished.config;

import dev.maire.nourished.core.Nourished;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Moves files from the old flat config/nourished/ layout into the grouped one. Runs once at mod
 * construction, before anything reads config; a no-op on installs that are already migrated.
 */
public final class NourishedConfigLayoutMigration {

    private NourishedConfigLayoutMigration() {}

    public static void run() {
        Path root = NourishedConfigPaths.root();
        if (!Files.isDirectory(root)) {
            return;
        }

        move(root.resolve("nutrients.json"), NourishedConfigPaths.nutrients());
        move(root.resolve("nutrient_curves.json"), NourishedConfigPaths.nutrientCurves());
        move(root.resolve("nutrient_overrides.json"), NourishedConfigPaths.nutrientOverrides());
        move(root.resolve("food_values.json"), NourishedConfigPaths.foodValues());
        move(root.resolve("effects.json"), NourishedConfigPaths.effects());
        move(root.resolve("locks.json"), NourishedConfigPaths.locks());
        move(root.resolve("raw_food.json"), NourishedConfigPaths.rawFood());
        move(root.resolve("tag_audit_report.json"), NourishedConfigPaths.tagAuditReport());
        move(root.resolve("exports"), NourishedConfigPaths.exports());
        move(root.resolve("nourished_nutrients_export"), NourishedConfigPaths.nutrientsExport());

        // food_overrides.json briefly lived loose in overrides/ before Overrides/ existed
        Path overrides = NourishedConfigPaths.overrides();
        move(overrides.resolve("food_overrides.json"), NourishedConfigPaths.foodOverrides());
        deleteQuietly(overrides.resolve("OVERRIDES_README.md"));

        Path activityReadme = NourishedConfigPaths.activityConfig().getParent().resolve("Read_Me");
        move(activityReadme.resolve("ACTIVITY_CONFIG_README.md"),
                NourishedConfigPaths.readme().resolve("ACTIVITY_CONFIG_README.md"));
        deleteIfEmpty(activityReadme);

        retire(NourishedConfigPaths.itemEditor().resolve("Food Overrides"));
        retireBackup(root.resolve("colors.json.bak"), root.resolve("colors.json"));
    }

    private static void move(Path from, Path to) {
        if (!Files.exists(from)) {
            return;
        }
        if (Files.exists(to)) {
            Nourished.LOGGER.warn("[ConfigLayout] Both {} and {} exist, leaving the old one alone", from, to);
            return;
        }
        try {
            Files.createDirectories(to.getParent());
            Files.move(from, to);
            Nourished.LOGGER.info("[ConfigLayout] Moved {} -> {}", relative(from), relative(to));
        } catch (IOException e) {
            Nourished.LOGGER.error("[ConfigLayout] Failed to move {} -> {}", from, to, e);
        }
    }

    // A folder no code reads anymore: dropped if it only holds empty JSON, otherwise parked in generated/legacy/
    private static void retire(Path dir) {
        if (!Files.isDirectory(dir)) {
            return;
        }
        try {
            if (onlyEmptyJson(dir)) {
                deleteTree(dir);
                Nourished.LOGGER.info("[ConfigLayout] Removed unused {}", relative(dir));
            } else {
                move(dir, NourishedConfigPaths.generated().resolve("legacy").resolve(dir.getFileName()));
            }
        } catch (IOException e) {
            Nourished.LOGGER.error("[ConfigLayout] Failed to clean up {}", dir, e);
        }
    }

    private static void retireBackup(Path backup, Path live) {
        if (!Files.isRegularFile(backup)) {
            return;
        }
        try {
            if (Files.isRegularFile(live) && Files.mismatch(backup, live) == -1) {
                Files.delete(backup);
                Nourished.LOGGER.info("[ConfigLayout] Removed {} (same as the live file)", relative(backup));
                return;
            }
        } catch (IOException e) {
            Nourished.LOGGER.warn("[ConfigLayout] Couldn't compare {} with {}", backup, live, e);
        }
        move(backup, NourishedConfigPaths.generated().resolve("legacy").resolve(backup.getFileName()));
    }

    private static boolean onlyEmptyJson(Path dir) throws IOException {
        try (Stream<Path> walk = Files.walk(dir)) {
            for (Path p : walk.filter(Files::isRegularFile).toList()) {
                String text = Files.readString(p).replaceAll("\\s", "");
                if (!text.isEmpty() && !text.equals("[]") && !text.equals("{}")) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void deleteTree(Path dir) throws IOException {
        List<Path> paths;
        try (Stream<Path> walk = Files.walk(dir)) {
            paths = walk.sorted(Comparator.reverseOrder()).toList();
        }
        for (Path p : paths) {
            Files.deleteIfExists(p);
        }
    }

    private static void deleteIfEmpty(Path dir) {
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (Stream<Path> entries = Files.list(dir)) {
            if (entries.findAny().isEmpty()) {
                Files.delete(dir);
            }
        } catch (IOException e) {
            Nourished.LOGGER.warn("[ConfigLayout] Couldn't remove empty {}", dir, e);
        }
    }

    private static void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            Nourished.LOGGER.warn("[ConfigLayout] Couldn't delete {}", file, e);
        }
    }

    private static Path relative(Path p) {
        return NourishedConfigPaths.root().relativize(p);
    }
}
