package dev.maire.nourished.kubejs;

import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;
import dev.latvian.mods.kubejs.script.ScriptManager;
import dev.marie.framework.api.ApiStatus;
import dev.maire.nourished.kubejs.bindings.NourishedKubeBindings;
import dev.maire.nourished.kubejs.internal.NourishedKubeEventBridge;
import dev.maire.nourished.kubejs.internal.NourishedKubeGuard;

@ApiStatus.Experimental
public class NourishedKubePlugin implements KubeJSPlugin {

    @Override
    public void registerEvents(EventGroupRegistry registry) {
        registry.register(NourishedKubeEvents.GROUP);
    }

    @Override
    public void registerBindings(BindingRegistry registry) {
        registry.add("NourishedAPI", NourishedKubeBindings.class);
    }

    @Override
    public void beforeScriptsLoaded(ScriptManager manager) {
        NourishedKubeGuard.invalidateCache();
    }

    @Override
    public void afterScriptsLoaded(ScriptManager manager) {
        NourishedKubeGuard.invalidateCache();
        NourishedKubeEventBridge.register();
    }
}
