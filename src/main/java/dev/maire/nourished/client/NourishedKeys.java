package dev.maire.nourished.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public final class NourishedKeys {

     public static final KeyMapping EDIT_HUD = new KeyMapping(
        "key.nourished.editHUD",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            "key.categories.nourished"
    );

    public static final KeyMapping OPEN_DIET_SCREEN = new KeyMapping(
            "key.nourished.openDietScreen",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_N,
            "key.categories.nourished"
    );

    public static final KeyMapping EDIT_DIET_SCREEN = new KeyMapping(
            "key.nourished.editDietScreen",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            "key.categories.nourished"
    );

    private NourishedKeys() {}

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(EDIT_HUD);
        event.register(OPEN_DIET_SCREEN);
        event.register(EDIT_DIET_SCREEN);
    }
}
