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

    public static final KeyMapping EDIT_ACTIVITY_LOG_HUD = new KeyMapping(
            "key.nourished.editActivityLogHud",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "key.categories.nourished"
    );

    public static final KeyMapping EDIT_CALORIE_HUD = new KeyMapping(
            "key.nourished.editCalorieHud",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            "key.categories.nourished"
    );

    public static final KeyMapping EDIT_ALL_HUDS = new KeyMapping(
            "key.nourished.editAllHuds",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            "key.categories.nourished"
    );

    public static final KeyMapping OPEN_SCALE_CONFIG = new KeyMapping(
            "key.nourished.openScaleConfig",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            "key.categories.nourished"
    );

    public static final KeyMapping OPEN_COMMAND_CENTER = new KeyMapping(
            "key.nourished.openCommandCenter",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            "key.categories.nourished"
    );

    public static final KeyMapping OPEN_ITEM_EDITOR = new KeyMapping(
            "key.nourished.openItemEditor",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            "key.categories.nourished"
    );

    public static final KeyMapping SHOW_TOOLTIP_DETAILS = new KeyMapping(
            "key.nourished.showTooltipDetails",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_SHIFT,
            "key.categories.nourished"
    );

    private NourishedKeys() {}

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(EDIT_HUD);
        event.register(OPEN_DIET_SCREEN);
        event.register(EDIT_DIET_SCREEN);
        event.register(EDIT_ACTIVITY_LOG_HUD);
        event.register(EDIT_CALORIE_HUD);
        event.register(EDIT_ALL_HUDS);
        event.register(OPEN_SCALE_CONFIG);
        event.register(OPEN_COMMAND_CENTER);
        event.register(OPEN_ITEM_EDITOR);
        event.register(SHOW_TOOLTIP_DETAILS);
    }
}
