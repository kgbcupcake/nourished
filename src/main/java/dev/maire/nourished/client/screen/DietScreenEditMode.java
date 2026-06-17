package dev.maire.nourished.client.screen;

import dev.maire.nourished.client.NourishedKeys;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;

public final class DietScreenEditMode {

    public static boolean isActive = false;

    private DietScreenEditMode() {}

    /** Toggle edit mode on or off. Opening activates the overlay screen; closing restores normal input. */
    public static void setActive(boolean active) {
        if (isActive == active) return;
        isActive = active;
        Minecraft mc = Minecraft.getInstance();
        if (active) {
            mc.setScreen(new DietScreenEditScreen());
        } else {
            if (mc.screen instanceof DietScreenEditScreen) {
                mc.setScreen(null);
            }
        }
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            return;
        }
        while (NourishedKeys.EDIT_DIET_SCREEN.consumeClick()) {
            if (!isActive) {
                setActive(true);
            }
        }
    }
}
