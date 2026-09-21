package dev.maire.nourished.client.screen.diet.dynamic.persistence;

import dev.marie.framework.ui.component.ComponentState;

/** Per-module "show icons" switch, kept in the Diet Screen's UI state; icons are shown until turned off. */
public final class DietModuleIcons {

    private static final String SUFFIX = "#hideIcons";

    private DietModuleIcons() {}

    public static boolean isShown(String moduleId) {
        return !DietScreenPersistence.get().load(moduleId + SUFFIX).map(ComponentState::collapsed).orElse(false);
    }

    public static void setShown(String moduleId, boolean shown) {
        DietScreenPersistence.get().save(moduleId + SUFFIX, new ComponentState(0, 0, 0, 0, !shown, false, false, 0));
    }
}
