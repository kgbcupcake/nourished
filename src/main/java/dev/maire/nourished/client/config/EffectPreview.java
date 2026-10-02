package dev.maire.nourished.client.config;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Client-only effect previews from the effect editor. Vanilla only removes expired effects
 * server-side, so without this a preview never leaves the client's active effect list.
 */
public final class EffectPreview {

    private static final Map<Holder<MobEffect>, MobEffectInstance> ACTIVE = new HashMap<>();

    private EffectPreview() {}

    public static void show(Holder<MobEffect> effect, int durationTicks, int amplifier) {
        Minecraft mc = Minecraft.getInstance();
        // Don't clobber a real effect the server is tracking
        if (mc.player == null || (mc.player.hasEffect(effect) && !ACTIVE.containsKey(effect))) {
            return;
        }
        MobEffectInstance instance = new MobEffectInstance(effect, durationTicks, amplifier, false, true, true);
        mc.player.forceAddEffect(instance, null);
        ACTIVE.put(effect, mc.player.getEffect(effect));
    }

    public static void tick() {
        if (ACTIVE.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            ACTIVE.clear();
            return;
        }
        Iterator<Map.Entry<Holder<MobEffect>, MobEffectInstance>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Holder<MobEffect>, MobEffectInstance> entry = it.next();
            MobEffectInstance current = mc.player.getEffect(entry.getKey());
            // The server replaced it with a real effect, so it's no longer ours to remove
            if (current != entry.getValue()) {
                it.remove();
            } else if (current.getDuration() <= 0) {
                mc.player.removeEffectNoUpdate(entry.getKey());
                it.remove();
            }
        }
    }
}
