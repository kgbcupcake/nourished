package dev.maire.nourished.client.hud;

/**
 * Simple flag class toggled by the edit-HUD hotkey.
 * Drag state lives here so NourishedHUD can read it without circular deps.
 */
public final class HUDEditMode {

    /** True while the player is in HUD edit mode (hotkey toggled). */
    public static boolean isActive = false;

    private HUDEditMode() {}
}
