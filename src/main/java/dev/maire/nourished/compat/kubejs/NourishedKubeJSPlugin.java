package dev.maire.nourished.compat.kubejs;

import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.script.BindingRegistry;
import dev.maire.nourished.api.ApiStatus;
import net.neoforged.fml.ModList;

/**
 * Nourished KubeJS plugin entrypoint.
 */
@ApiStatus.Internal
public final class NourishedKubeJSPlugin implements KubeJSPlugin {

    public static void bootstrap() {
        if (!ModList.get().isLoaded("kubejs")) {
            return;
        }
    }

    @Override
    public void registerBindings(BindingRegistry bindings) {
        if (!ModList.get().isLoaded("kubejs")) {
            return;
        }
        bindings.add("NourishedAPI", NourishedKubeJSBindings.createBindingObject());
    }

    @Override
    public void registerEvents(EventGroupRegistry registry) {
        if (!ModList.get().isLoaded("kubejs")) {
            return;
        }
        NourishedKubeJSEvents.register(registry);
        NourishedKubeJSEvents.register();
        NourishedKubeJSStartupEvents.register(registry);
    }
}
