package dev.maire.nourished.config;

/**
 * Which Diet Screen implementation opens for {@link dev.maire.nourished.client.NourishedKeys#OPEN_DIET_SCREEN}.
 *
 * <p>The old {@code CLASSIC} (pre-MarieUI) mode was removed in 0.2.8-beta — {@code DYNAMIC} is now
 * the recommended/default interface. Anyone previously on Classic migrates to Dynamic automatically
 * (see {@link dev.maire.nourished.config.NourishedClientConfig#dietScreenMode()}).
 */
public enum DietScreenMode {
    /** MarieUI panel with independently draggable/resizable sub-boxes, stacked together on one screen. */
    DYNAMIC,
    /** MarieUI hub window: a sidebar of tabs (Home, Calories, Balance, Recent Meals, Eat More, Active Effects, Intake Breakdown), each showing that module full-pane. */
    HUB
}
