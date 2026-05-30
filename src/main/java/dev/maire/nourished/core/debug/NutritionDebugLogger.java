package dev.maire.nourished.core.debug;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dev.maire.nourished.config.ModuleCache;
import dev.maire.nourished.core.Nourished;
import net.neoforged.fml.loading.FMLPaths;

/**
 * Async structured nutrition eat logging under {@code config/nourished/debug/}.
 * Effective log cap is ~2 MB (1 MB active + 1 MB rotated). Two auxiliary files
 * (classifier_accuracy.json, unclassified_foods.json) are written on each eat event.
 */
public final class NutritionDebugLogger {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Gson GSON_COMPACT = new Gson();
    private static final long MAX_LOG_BYTES = 1_000_000L;
    private static final int QUEUE_CAPACITY = 1024;

    /** Rate-limit: skip re-logging the same item within 5 000 ms. */
    private static final ConcurrentHashMap<String, Long> LAST_LOGGED_MS = new ConcurrentHashMap<>();
    private static final long RATE_LIMIT_MS = 5_000L;

    private static final ArrayBlockingQueue<Runnable> WORK_QUEUE = new ArrayBlockingQueue<>(QUEUE_CAPACITY);

    private static final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS,
            WORK_QUEUE,
            r -> {
                Thread t = new Thread(r, "Nourished-NutritionDebugLog");
                t.setDaemon(true);
                return t;
            },
            new ThreadPoolExecutor.DiscardOldestPolicy()
    );

    private NutritionDebugLogger() {}

    public static void submitEatLog(JsonObject entry) {
        if (!ModuleCache.enableDebugLogging) {
            return;
        }
        String jsonLine = GSON_COMPACT.toJson(entry);
        String classifierPath = entry.has("classifier_path") ? entry.get("classifier_path").getAsString() : "";
        JsonObject itemObj = entry.has("item") && entry.get("item").isJsonObject()
                ? entry.getAsJsonObject("item")
                : new JsonObject();
        String itemId = itemObj.has("id") ? itemObj.get("id").getAsString() : "";
        String namespace = itemObj.has("namespace") ? itemObj.get("namespace").getAsString() : "";

        // Per-item rate limiting: skip if same item was logged within RATE_LIMIT_MS
        if (!itemId.isEmpty()) {
            long now = System.currentTimeMillis();
            Long last = LAST_LOGGED_MS.get(itemId);
            if (last != null && now - last < RATE_LIMIT_MS) {
                return;
            }
            LAST_LOGGED_MS.put(itemId, now);
        }

        int depth = WORK_QUEUE.size();
        if (depth >= QUEUE_CAPACITY * 80 / 100) {
            Nourished.LOGGER.warn("[Nourished] Debug log queue at {}% capacity", depth * 100 / QUEUE_CAPACITY);
        }

        EXECUTOR.execute(() -> {
            try {
                writeNutritionLine(jsonLine);
                updateAccuracy(classifierPath);
                if ("UNCLASSIFIED".equals(classifierPath) && !itemId.isEmpty()) {
                    mergeUnclassifiedItem(itemId, namespace);
                }
            } catch (Exception e) {
                Nourished.LOGGER.error("[Nourished] Nutrition debug log write failed", e);
            }
        });
    }

    /** Returns the current number of tasks queued in the executor. */
    public static int getQueueDepth() {
        return WORK_QUEUE.size();
    }

    private static Path debugDir() throws IOException {
        Path dir = FMLPaths.GAMEDIR.get().resolve("config").resolve(Nourished.MODID).resolve("debug");
        Files.createDirectories(dir);
        return dir;
    }

    private static void writeNutritionLine(String jsonLine) throws IOException {
        Path dir = debugDir();
        Path logPath = dir.resolve("nutrition_log.json");
        rotateIfNeeded(logPath);
        try (BufferedWriter w = Files.newBufferedWriter(
                logPath,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND)) {
            w.write(jsonLine);
            w.newLine();
        }
    }

    private static void rotateIfNeeded(Path logPath) throws IOException {
        if (!Files.exists(logPath)) {
            return;
        }
        long size = Files.size(logPath);
        if (size <= MAX_LOG_BYTES) {
            return;
        }
        Path old = logPath.resolveSibling("nutrition_log_old.json");
        Files.move(logPath, old, StandardCopyOption.REPLACE_EXISTING);
    }

    private static void updateAccuracy(String classifierPath) throws IOException {
        if (classifierPath == null || classifierPath.isEmpty()) {
            return;
        }
        Path dir = debugDir();
        Path path = dir.resolve("classifier_accuracy.json");
        JsonObject root;
        if (Files.exists(path)) {
            try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                root = JsonParser.parseReader(reader).getAsJsonObject();
            }
        } else {
            root = new JsonObject();
        }
        String key = switch (classifierPath) {
            case "TAG_HIT", "TAG_AND_RUNTIME_BLEND", "CACHE_HIT", "CLASSIFIER_RUN", "RUNTIME_RESOLVER", "UNCLASSIFIED", "FOOD_OVERRIDE" -> classifierPath;
            default -> "OTHER";
        };
        int n = root.has(key) ? root.get(key).getAsInt() : 0;
        root.addProperty(key, n + 1);
        try (var w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(root, w);
        }
    }

    private static void mergeUnclassifiedItem(String itemId, String namespace) throws IOException {
        Path dir = debugDir();
        Path path = dir.resolve("unclassified_foods.json");
        JsonObject root;
        if (Files.exists(path)) {
            try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                root = JsonParser.parseReader(reader).getAsJsonObject();
            }
        } else {
            root = new JsonObject();
        }
        JsonObject items = root.has("items") && root.get("items").isJsonObject()
                ? root.getAsJsonObject("items")
                : new JsonObject();
        if (!items.has(itemId)) {
            JsonObject o = new JsonObject();
            o.addProperty("firstSeen", Instant.now().toString());
            o.addProperty("namespace", namespace == null || namespace.isEmpty() ? inferNamespace(itemId) : namespace);
            items.add(itemId, o);
        }
        root.add("items", items);
        try (var w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(root, w);
        }
    }

    private static String inferNamespace(String itemId) {
        int i = itemId.indexOf(':');
        return i > 0 ? itemId.substring(0, i) : "unknown";
    }

    /**
     * ISO-8601 UTC timestamp for log entries.
     */
    public static String isoTimestamp() {
        return Instant.now().toString();
    }

    public static JsonObject floatMapToJson(Map<String, Float> map) {
        JsonObject o = new JsonObject();
        if (map == null) {
            return o;
        }
        for (Map.Entry<String, Float> e : map.entrySet()) {
            o.addProperty(e.getKey(), round4(e.getValue()));
        }
        return o;
    }

    public static float round4(float v) {
        return Math.round(v * 10_000f) / 10_000f;
    }

    public static String pctOfFinal(float part, float finalMult) {
        if (finalMult <= 1e-6f) {
            return "0";
        }
        return String.format(Locale.ROOT, "%.1f", (part / finalMult) * 100f);
    }
}
