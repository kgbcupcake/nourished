package dev.maire.nourished.client.hud;

import net.minecraft.client.Minecraft;

public final class HUDEditMode {

    public static boolean isActive = false;

    private HUDEditMode() {}

    /** Toggle edit mode on or off. Opening activates the overlay screen; closing restores normal input. */
    public static void setActive(boolean active) {
        if (isActive == active) return;
        isActive = active;
        Minecraft mc = Minecraft.getInstance();
        if (active) {
            mc.setScreen(new HUDEditScreen());
        } else {
            if (mc.screen instanceof HUDEditScreen) {
                mc.setScreen(null);
            }
        }
    }
}
