package dev.maire.nourished.core.util;

import dev.maire.nourished.core.Nourished;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Loads JSON (or other text) from {@code <modid>:<relativePath>} in the active datapack stack.
 */
public final class NourishedResourceLoader {

    private NourishedResourceLoader() {}

    /**
     * Resolves {@code ResourceLocation.fromNamespaceAndPath(Nourished.MODID, relativePath)},
     * opens the resource as {@link InputStreamReader}, calls {@code consumer}, logs success,
     * catches {@link IOException}, logs error, then calls {@code fallback}.
     * <p>
     * When the datapack resource is missing, or parsing fails without throwing from
     * {@code consumer}, {@code fallback} runs. When {@code fallbackInfoLog} is non-null it is
     * logged after {@code fallback} runs (including when the resource was absent).
     *
     * @param fallbackInfoLog log line after fallback, or {@code null} to skip that log
     */
    public static void loadFromModConfig(
            ResourceManager resourceManager,
            String relativePath,
            Consumer<Reader> consumer,
            Runnable fallback,
            String successMessage,
            String errorMessage,
            String fallbackInfoLog
    ) {
        loadFromModConfig(
                resourceManager,
                relativePath,
                reader -> {
                    consumer.accept(reader);
                    return true;
                },
                fallback,
                successMessage,
                errorMessage,
                fallbackInfoLog
        );
    }

    /**
     * Like {@link #loadFromModConfig(ResourceManager, String, Consumer, Runnable, String, String, String)}
     * but the reader callback returns {@code false} to indicate the datapack payload was not
     * applied (caller should run {@code fallback} without treating it as an I/O error).
     */
    public static void loadFromModConfig(
            ResourceManager resourceManager,
            String relativePath,
            DatapackReaderAction action,
            Runnable fallback,
            String successMessage,
            String errorMessage,
            String fallbackInfoLog
    ) {
        ResourceLocation path = ResourceLocation.fromNamespaceAndPath(Nourished.MODID, relativePath);
        Optional<Resource> resource = resourceManager.getResource(path);
        if (resource.isEmpty()) {
            fallback.run();
            if (fallbackInfoLog != null) {
                Nourished.LOGGER.info(fallbackInfoLog);
            }
            return;
        }
        try (InputStream is = resource.get().open();
             Reader r = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            if (action.apply(r)) {
                Nourished.LOGGER.info(successMessage);
                return;
            }
        } catch (IOException e) {
            Nourished.LOGGER.error(errorMessage, e);
        }
        fallback.run();
        if (fallbackInfoLog != null) {
            Nourished.LOGGER.info(fallbackInfoLog);
        }
    }

    /** Reader callback for datapack JSON; return {@code true} when datapack load finished successfully. */
    @FunctionalInterface
    public interface DatapackReaderAction {
        boolean apply(Reader reader) throws IOException;
    }
}
