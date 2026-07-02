package dev.maire.nourished.client;

import dev.marie.framework.client.ImportExportToast;
import net.minecraft.network.chat.Component;

/**
 * Client-only feedback for post-load config validation.
 */
public final class NourishedValidationClient {

    private NourishedValidationClient() {}

    public static void showFailureToast() {
        ImportExportToast.show(Component.translatable("command.nourished.validate.failToast"));
    }
}
