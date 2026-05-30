package dev.maire.nourished.core.registry;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.core.Nourished;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Centralizes the reset → load → validate → freeze lifecycle for Nourished's static registries.
 *
 * <p>Registries are processed in registration order; dependencies must register before dependents.
 * Each entry has a name (for logging), a mandatory {@link Runnable} for initial load, a mandatory
 * {@link Runnable} for in-game reload, and an optional {@link Consumer} for datapack-aware reload
 * driven by {@link ResourceManager}.</p>
 *
 * <p>Registration happens once at mod construction (see {@code Nourished} bootstrap). Subsequent
 * {@link #loadAll()}, {@link #reloadAll()}, and {@link #loadAll(ResourceManager)} calls reuse the
 * same entry list.</p>
 */
@ApiStatus.Internal
public final class RegistryLifecycleManager {

    private record Entry(
            String name,
            Runnable load,
            Runnable reload,
            Consumer<ResourceManager> datapackLoad
    ) {}

    private static final List<Entry> ENTRIES = new ArrayList<>();

    private static volatile boolean reloadInProgress;

    private RegistryLifecycleManager() {}

    /**
     * Returns {@code true} while an in-game registry reload is underway.
     * Gameplay code should skip registry reads and use safe defaults when this is set.
     */
    public static boolean isReloadInProgress() {
        return reloadInProgress;
    }

    /**
     * Registers a registry without a datapack-aware loader.
     */
    public static void registerRegistry(String name, Runnable load, Runnable reload) {
        registerRegistry(name, load, reload, null);
    }

    /**
     * Registers a registry with all three lifecycle hooks. The {@code datapackLoad} is invoked by
     * {@link #loadAll(ResourceManager)}; if {@code null}, that registry is skipped during the
     * datapack pass.
     */
    public static void registerRegistry(
            String name,
            Runnable load,
            Runnable reload,
            Consumer<ResourceManager> datapackLoad
    ) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Registry name must be non-blank");
        }
        if (load == null) {
            throw new IllegalArgumentException("load runnable must be non-null for " + name);
        }
        if (reload == null) {
            throw new IllegalArgumentException("reload runnable must be non-null for " + name);
        }
        ENTRIES.add(new Entry(name, load, reload, datapackLoad));
    }

    /**
     * Calls {@code load} on every registered registry in registration order.
     * Failures are logged and rethrown so bootstrap fails fast.
     */
    public static void loadAll() {
        for (Entry entry : ENTRIES) {
            Nourished.LOGGER.info("[RegistryLifecycleManager] Loading {}", entry.name());
            try {
                entry.load().run();
            } catch (RuntimeException e) {
                Nourished.LOGGER.error("[RegistryLifecycleManager] Failed to load {}", entry.name(), e);
                throw e;
            }
            Nourished.LOGGER.info("[RegistryLifecycleManager] Loaded {}", entry.name());
        }
    }

    /**
     * Calls {@code reload} on every registered registry in registration order.
     * Failures are logged and rethrown so callers can surface them.
     */
    public static void reloadAll() {
        reloadInProgress = true;
        try {
            for (Entry entry : ENTRIES) {
                Nourished.LOGGER.info("[RegistryLifecycleManager] Reloading {}", entry.name());
                try {
                    entry.reload().run();
                } catch (RuntimeException e) {
                    Nourished.LOGGER.error("[RegistryLifecycleManager] Failed to reload {}", entry.name(), e);
                    throw e;
                }
                Nourished.LOGGER.info("[RegistryLifecycleManager] Reloaded {}", entry.name());
            }
        } finally {
            reloadInProgress = false;
        }
    }

    /**
     * Calls each entry's datapack loader in registration order. Entries without a datapack loader
     * are skipped. Per-registry failures are logged and rethrown.
     */
    public static void loadAll(ResourceManager resourceManager) {
        reloadInProgress = true;
        try {
            for (Entry entry : ENTRIES) {
                Consumer<ResourceManager> datapack = entry.datapackLoad();
                if (datapack == null) {
                    continue;
                }
                Nourished.LOGGER.info("[RegistryLifecycleManager] Datapack-loading {}", entry.name());
                try {
                    datapack.accept(resourceManager);
                } catch (RuntimeException e) {
                    Nourished.LOGGER.error("[RegistryLifecycleManager] Failed datapack-load of {}", entry.name(), e);
                    throw e;
                }
                Nourished.LOGGER.info("[RegistryLifecycleManager] Datapack-loaded {}", entry.name());
            }
        } finally {
            reloadInProgress = false;
        }
    }

    /**
     * Returns an immutable, ordered view of registered registry names for diagnostics.
     */
    public static List<String> registeredNames() {
        List<String> names = new ArrayList<>(ENTRIES.size());
        for (Entry e : ENTRIES) {
            names.add(e.name());
        }
        return Collections.unmodifiableList(names);
    }
}
