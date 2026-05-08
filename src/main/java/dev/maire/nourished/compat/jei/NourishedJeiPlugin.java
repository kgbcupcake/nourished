package dev.maire.nourished.compat.jei;

import dev.maire.nourished.api.ApiStatus;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
@ApiStatus.Internal
public final class NourishedJeiPlugin implements IModPlugin {

    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("nourished", "jei_plugin");

    public static void bootstrap() {
        // Intentionally empty. Called reflectively from client init to keep optional class loading safe.
    }

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

}
