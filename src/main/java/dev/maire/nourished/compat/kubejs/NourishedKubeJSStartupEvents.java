package dev.maire.nourished.compat.kubejs;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.event.KubeEvent;

/**
 * Startup event hook holder for Nourished KubeJS registration flows.
 */
public final class NourishedKubeJSStartupEvents {

    public static final String REGISTER_NUTRIENTS = "nourished.startup.register_nutrients";
    public static final String REGISTER_PROFILES = "nourished.startup.register_profiles";
    public static final String REGISTER_MILESTONES = "nourished.startup.register_milestones";

    private static final EventGroup GROUP = EventGroup.of("NourishedStartupEvents");

    private NourishedKubeJSStartupEvents() {}

    public static void register(EventGroupRegistry registry) {
        GROUP.startup(REGISTER_NUTRIENTS, () -> RegisterNutrientsEvent.class);
        GROUP.startup(REGISTER_PROFILES, () -> RegisterProfilesEvent.class);
        GROUP.startup(REGISTER_MILESTONES, () -> RegisterMilestonesEvent.class);
        registry.register(GROUP);
    }

    public static final class RegisterNutrientsEvent implements KubeEvent {
        private final NourishedKubeJSBindings.ScriptApi api = new NourishedKubeJSBindings.ScriptApi();
        public void registerNutrient(com.google.gson.JsonObject data) { api.registerNutrient(data); }
        public void registerFoodClassification(String itemId, String nutrientKey, float amount) { api.registerFoodClassification(itemId, nutrientKey, amount); }
    }

    public static final class RegisterProfilesEvent implements KubeEvent {
        private final NourishedKubeJSBindings.ScriptApi api = new NourishedKubeJSBindings.ScriptApi();
        public void registerDietProfile(com.google.gson.JsonObject data) { api.registerDietProfile(data); }
        public void registerFoodSynergy(String foodA, String foodB, int windowSeconds, String nutrientKey, float amount) { api.registerFoodSynergy(foodA, foodB, windowSeconds, nutrientKey, amount); }
    }

    public static final class RegisterMilestonesEvent implements KubeEvent {
        private final NourishedKubeJSBindings.ScriptApi api = new NourishedKubeJSBindings.ScriptApi();
        public void registerMilestone(com.google.gson.JsonObject data) { api.registerMilestone(data); }
    }
}
