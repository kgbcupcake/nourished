package dev.maire.nourished.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.maire.nourished.Nourished;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = Nourished.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class NourishedKeys {

    public static final KeyMapping EDIT_HUD = new KeyMapping(
            "key.nourished.editHUD",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            "key.categories.nourished"
    );

    public static final KeyMapping OPEN_CONFIG = new KeyMapping(
            "key.nourished.openConfig",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            "key.categories.nourished"
    );

    private NourishedKeys() {}

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(EDIT_HUD);
        event.register(OPEN_CONFIG);
    }
}
