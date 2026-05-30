package dev.maire.nourished.compat.kubejs;

import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugins;
import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.script.BindingRegistry;
import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.core.Nourished;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;
import java.util.stream.Stream;

/**
 * Nourished KubeJS plugin entrypoint.
 *
 * <p>KubeJS 2101 discovers plugins from each mod jar's {@code kubejs.plugins.txt} (not
 * {@code META-INF/services}). When that file or service-loader discovery fails (e.g. under
 * Architectury), {@link #bootstrap()} registers this plugin via {@link KubeJSPlugins} internal
 * {@code loadFromFile}.</p>
 */
@ApiStatus.Internal
public final class NourishedKubeJSPlugin implements KubeJSPlugin {

    private static final String PLUGIN_LINE = NourishedKubeJSPlugin.class.getName() + " " + Nourished.MODID;

    private static boolean manualRegistrationAttempted;

    /**
     * Ensures this plugin is present in {@link KubeJSPlugins} when automatic discovery failed.
     * KubeJS 2101 has no public {@code register()} API; the supported manual path is
     * {@code kubejs.plugins.txt} plus this {@code loadFromFile} fallback.
     */
    public static void bootstrap() {
        if (!ModList.get().isLoaded("kubejs")) {
            return;
        }
        if (isRegistered()) {
            return;
        }
        if (manualRegistrationAttempted) {
            return;
        }
        manualRegistrationAttempted = true;

        try {
            registerViaKubeJSPluginsLoadFromFile();
            if (isRegistered()) {
                Nourished.LOGGER.info(
                        "[Nourished] Manually registered KubeJS plugin (kubejs.plugins.txt / service-loader discovery missed).");
                initializePluginLifecycle();
            } else {
                Nourished.LOGGER.warn("[Nourished] Manual KubeJS plugin registration did not add NourishedKubeJSPlugin.");
            }
        } catch (Throwable t) {
            Nourished.LOGGER.warn("[Nourished] Failed to manually register KubeJS plugin.", t);
        }
    }

    public static boolean isRegistered() {
        for (KubeJSPlugin plugin : KubeJSPlugins.getAll()) {
            if (plugin.getClass() == NourishedKubeJSPlugin.class) {
                return true;
            }
        }
        return false;
    }

    /**
     * Uses the same code path as {@code kubejs.plugins.txt} parsing inside KubeJS 2101.
     */
    private static void registerViaKubeJSPluginsLoadFromFile() throws ReflectiveOperationException {
        Method loadFromFile = KubeJSPlugins.class.getDeclaredMethod(
                "loadFromFile", Stream.class, String.class, boolean.class);
        loadFromFile.setAccessible(true);
        loadFromFile.invoke(null, Stream.of(PLUGIN_LINE), Nourished.MODID, false);
    }

    /**
     * If registration happened after KubeJS already ran plugin init, run init hooks on this plugin.
     */
    private static void initializePluginLifecycle() {
        for (KubeJSPlugin plugin : KubeJSPlugins.getAll()) {
            if (plugin.getClass() == NourishedKubeJSPlugin.class) {
                plugin.init();
                plugin.initStartup();
                return;
            }
        }
    }

    @Override
    public void registerBindings(BindingRegistry bindings) {
        if (!ModList.get().isLoaded("kubejs")) {
            return;
        }
        bindings.add(NourishedKubeJSBindings.API_BINDING, NourishedKubeJSBindings.createBindingObject());
        bindings.add(NourishedKubeJSBindings.EVENTS_BINDING, NourishedKubeJSBindings.createEventsBindingObject(bindings.type()));
    }

    @Override
    public void registerEvents(EventGroupRegistry registry) {
        if (!ModList.get().isLoaded("kubejs")) {
            return;
        }
        registry.register(NourishedKubeJSEvents.GROUP);
        NourishedKubeJSEventBridge.register();
        NourishedKubeJSStartupEvents.register(registry);
    }
}
