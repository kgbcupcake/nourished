package dev.maire.nourished.core.diagnostics;

import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.nutrition.ResolutionResult;
import dev.maire.nourished.core.nutrition.RuntimeCascadeStage;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public final class NourishedUnknownFoodLogger {

    private static final ExecutorService WRITE_EXECUTOR =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "nourished-unknown-logger");
                t.setDaemon(true);
                return t;
            });

    private static final Set<ResourceLocation> LOGGED_IDS = ConcurrentHashMap.newKeySet();
    private static final AtomicBoolean SESSION_START_PENDING = new AtomicBoolean(true);
    private static final AtomicBoolean DISABLED = new AtomicBoolean(false);

    private static volatile Path logPath;

    private NourishedUnknownFoodLogger() {}

    public static void log(ResolutionResult result, ResourceLocation itemId) {
        if (DISABLED.get()) {
            return;
        }
        if (FMLEnvironment.dist == Dist.CLIENT) {
            return;
        }
        if (result == null || itemId == null) {
            return;
        }
        if (result.tokens() == null || result.rawScores() == null) {
            return;
        }
        boolean isHardFallback = result.stage() == RuntimeCascadeStage.HARD_FALLBACK;
        float threshold = (float) NourishedConfig.get().scannerConfidenceSpreadThreshold();
        boolean belowThreshold = result.confidence() < threshold;
        if (!isHardFallback && !belowThreshold) {
            return;
        }
        String namespace = itemId.getNamespace();
        String stageName = result.stage().name();
        List<String> tokens = result.tokens();
        Map<String, Float> rawScores = result.rawScores();
        float confidence = result.confidence();

        WRITE_EXECUTOR.execute(() -> runWrite(itemId, namespace, stageName, tokens, rawScores, confidence));
    }

    public static void onReload() {
        LOGGED_IDS.clear();
        SESSION_START_PENDING.set(true);
    }

    private static void runWrite(ResourceLocation itemId, String namespace, String stageName,
                                  List<String> tokens, Map<String, Float> rawScores, float confidence) {
        if (!LOGGED_IDS.add(itemId)) {
            return;
        }

        Path path = getLogPath();
        if (path == null) {
            LOGGED_IDS.remove(itemId);
            return;
        }

        try {
            Files.createDirectories(path.getParent());
        } catch (IOException e) {
            Nourished.LOGGER.warn("[NourishedUnknownFoodLogger] Failed to create log directory: {}", e.getMessage());
            LOGGED_IDS.remove(itemId);
            return;
        }

        StringBuilder sb = new StringBuilder();

        if (SESSION_START_PENDING.compareAndSet(true, false)) {
            LocalDateTime sessionTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
            sb.append("=== Session Start: ").append(sessionTime).append(" ===\n\n");
        }

        sb.append("[UNKNOWN] ").append(itemId).append("\n");
        sb.append("  Namespace : ").append(namespace).append("\n");
        sb.append("  Stage     : ").append(stageName).append("\n");
        sb.append("  Tokens    : ").append(formatTokens(tokens)).append("\n");
        sb.append("  Top Scores: ").append(formatTopScores(rawScores)).append("\n");
        sb.append("  Confidence: ").append(String.format(Locale.ROOT, "%.2f", confidence)).append("\n");
        sb.append("  Time      : ").append(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)).append("\n");
        sb.append("\n");

        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            writer.write(sb.toString());
        } catch (IOException e) {
            Nourished.LOGGER.warn("[NourishedUnknownFoodLogger] Failed to write log entry: {}", e.getMessage());
            LOGGED_IDS.remove(itemId);
            SESSION_START_PENDING.set(true);
        }
    }

    private static Path getLogPath() {
        if (logPath != null) {
            return logPath;
        }
        try {
            logPath = FMLPaths.GAMEDIR.get().resolve("config/nourished/unknown_foods.log");
            return logPath;
        } catch (Exception e) {
            Nourished.LOGGER.warn("[NourishedUnknownFoodLogger] Failed to resolve log path, disabling logger: {}", e.getMessage());
            DISABLED.set(true);
            return null;
        }
    }

    private static String formatTokens(List<String> tokens) {
        if (tokens.isEmpty()) {
            return "[]";
        }
        return "[" + String.join(", ", tokens) + "]";
    }

    private static String formatTopScores(Map<String, Float> rawScores) {
        if (rawScores.isEmpty()) {
            return "";
        }
        List<Map.Entry<String, Float>> sorted = new ArrayList<>(rawScores.entrySet());
        sorted.sort(Comparator.<Map.Entry<String, Float>>comparingDouble(Map.Entry::getValue).reversed());
        int limit = Math.min(3, sorted.size());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < limit; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            Map.Entry<String, Float> entry = sorted.get(i);
            sb.append(entry.getKey()).append("=").append(String.format(Locale.ROOT, "%.2f", entry.getValue()));
        }
        return sb.toString();
    }
}
