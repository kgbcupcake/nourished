package dev.maire.nourished.core;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.api.ValueModifierContext;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

import java.util.Map;

/**
 * Optional KubeJS integration entry points. Uses reflection so Nourished loads
 * without KubeJS on the runtime classpath.
 */
@ApiStatus.Internal
public final class NourishedKubeIntegration {

    private static final boolean PRESENT = ModList.get().isLoaded("kubejs");
    private static final String EVENT_BRIDGE =
            "dev.maire.nourished.kubejs.internal.NourishedKubeEventBridge";

    private NourishedKubeIntegration() {}

    public static void register() {
        if (!PRESENT) {
            return;
        }
        invokeStatic(EVENT_BRIDGE, "register");
    }

    public static void fireGutHealthChanged(
            String playerId,
            float oldValue,
            float newValue,
            String cause
    ) {
        if (!PRESENT) {
            return;
        }
        invokeStatic(
                EVENT_BRIDGE,
                "fireGutHealthChanged",
                new Class<?>[] {String.class, float.class, float.class, String.class},
                playerId,
                oldValue,
                newValue,
                cause
        );
    }

    public static void fireFoodEaten(ServerPlayer player, String itemId, Map<String, Float> nutrientDeltas) {
        if (!PRESENT) {
            return;
        }
        invokeStatic(
                EVENT_BRIDGE,
                "fireFoodEaten",
                new Class<?>[] {ServerPlayer.class, String.class, Map.class},
                player,
                itemId,
                nutrientDeltas
        );
    }

    public static boolean isRawFoodPenaltyCancelled(String playerId, String itemId, String tier) {
        if (!PRESENT) {
            return false;
        }
        Object result = invokeStatic(
                EVENT_BRIDGE,
                "fireRawFoodPenalty",
                new Class<?>[] {String.class, String.class, String.class},
                playerId,
                itemId,
                tier
        );
        return result instanceof Boolean cancelled && cancelled;
    }

    public static float fireNutrientModifier(ValueModifierContext ctx, float amount) {
        if (!PRESENT) {
            return amount;
        }
        Object result = invokeStatic(
                EVENT_BRIDGE,
                "fireNutrientModifier",
                new Class<?>[] {ValueModifierContext.class, float.class},
                ctx,
                amount
        );
        return result instanceof Float value ? value : amount;
    }

    private static void invokeStatic(String className, String method) {
        invokeStatic(className, method, new Class<?>[0]);
    }

    private static Object invokeStatic(String className, String method, Class<?>[] paramTypes, Object... args) {
        try {
            Class<?> type = Class.forName(className);
            return type.getMethod(method, paramTypes).invoke(null, args);
        } catch (ReflectiveOperationException e) {
            Nourished.LOGGER.warn("[Nourished] KubeJS integration call failed: {}.{}", className, method, e);
            return null;
        }
    }
}
