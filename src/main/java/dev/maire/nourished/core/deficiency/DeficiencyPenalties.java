package dev.maire.nourished.core.deficiency;

import dev.maire.nourished.core.Nourished;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Tier attribute modifiers, natural-regen scaling and the saturation cut on eating. */
public final class DeficiencyPenalties {

    private static final Map<UUID, Map<String, DeficiencySettings.AttributePenalty>> APPLIED = new ConcurrentHashMap<>();

    private DeficiencyPenalties() {}

    public static void register(IEventBus bus) {
        bus.addListener(EventPriority.LOW, DeficiencyPenalties::onFoodEaten);
        bus.addListener(DeficiencyPenalties::onLogout);
    }

    /** Whether deficiency penalties currently apply to this player at all. */
    public static boolean active(ServerPlayer player, DeficiencySettings s) {
        if (!s.enabled() || s.tiers().isEmpty()) return false;
        if (player.isCreative() || player.isSpectator()) return false;
        return s.affectPeaceful() || player.level().getDifficulty() != Difficulty.PEACEFUL;
    }

    private static DeficiencySettings.Tier currentTier(Player player) {
        if (!(player instanceof ServerPlayer sp)) return null;
        DeficiencySettings s = DeficiencyConfig.get();
        if (!active(sp, s)) return null;
        return s.tier(DeficiencyTracker.overallTier(sp));
    }

    /** Multiplier for vanilla food-driven regeneration; called from the FoodData mixin. */
    public static float regenMultiplier(Player player) {
        DeficiencySettings.Tier t = currentTier(player);
        return t == null ? 1f : t.regenMultiplier();
    }

    /** Brings the player's modifiers in line with {@code tier}; tier 0 removes them all. */
    public static void applyAttributes(ServerPlayer player, int tier, DeficiencySettings s) {
        DeficiencySettings.Tier t = s.tier(tier);
        Map<String, DeficiencySettings.AttributePenalty> wanted = t == null ? Map.of() : t.attributes();
        Map<String, DeficiencySettings.AttributePenalty> had = APPLIED.getOrDefault(player.getUUID(), Map.of());
        if (wanted.equals(had) && !wanted.isEmpty() && allPresent(player, wanted)) return;
        if (wanted.isEmpty() && had.isEmpty()) return;
        for (String key : had.keySet()) {
            if (!wanted.containsKey(key)) remove(player, key);
        }
        Map<String, DeficiencySettings.AttributePenalty> applied = new HashMap<>();
        wanted.forEach((key, penalty) -> {
            if (apply(player, key, penalty)) applied.put(key, penalty);
        });
        if (applied.isEmpty()) APPLIED.remove(player.getUUID());
        else APPLIED.put(player.getUUID(), Map.copyOf(applied));
    }

    private static boolean allPresent(ServerPlayer player, Map<String, DeficiencySettings.AttributePenalty> wanted) {
        // a respawned player is a new entity, so its transient modifiers are gone
        for (String key : wanted.keySet()) {
            AttributeInstance inst = instance(player, key);
            if (inst != null && inst.getModifier(modifierId(key)) == null) return false;
        }
        return true;
    }

    private static boolean apply(ServerPlayer player, String key, DeficiencySettings.AttributePenalty penalty) {
        AttributeInstance inst = instance(player, key);
        if (inst == null) return false;
        ResourceLocation id = modifierId(key);
        AttributeModifier existing = inst.getModifier(id);
        AttributeModifier.Operation op = operation(penalty.operation());
        if (existing != null && existing.amount() == penalty.amount() && existing.operation() == op) return true;
        inst.removeModifier(id);
        inst.addTransientModifier(new AttributeModifier(id, penalty.amount(), op));
        return true;
    }

    private static void remove(ServerPlayer player, String key) {
        AttributeInstance inst = instance(player, key);
        if (inst != null) inst.removeModifier(modifierId(key));
    }

    private static AttributeInstance instance(ServerPlayer player, String key) {
        ResourceLocation id = ResourceLocation.tryParse(key);
        if (id == null) return null;
        Holder<Attribute> holder = BuiltInRegistries.ATTRIBUTE.getHolder(id).orElse(null);
        return holder == null ? null : player.getAttribute(holder);
    }

    private static ResourceLocation modifierId(String key) {
        String path = key.toLowerCase(Locale.ROOT).replace(':', '/');
        ResourceLocation id = ResourceLocation.tryBuild(Nourished.MODID, "deficiency/" + path);
        return id != null ? id : ResourceLocation.fromNamespaceAndPath(Nourished.MODID, "deficiency/unknown");
    }

    private static AttributeModifier.Operation operation(String name) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "add_value", "addition", "add" -> AttributeModifier.Operation.ADD_VALUE;
            case "add_multiplied_base", "multiply_base" -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
            default -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
        };
    }

    private static void onFoodEaten(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        DeficiencySettings.Tier t = currentTier(player);
        if (t == null || t.saturationMultiplier() >= 1f) return;
        ItemStack stack = event.getItem();
        if (stack == null || stack.isEmpty()) return;
        FoodProperties food = stack.getFoodProperties(player);
        if (food == null) return;
        FoodData data = player.getFoodData();
        float cut = food.saturation() * (1f - t.saturationMultiplier());
        data.setSaturation(Math.max(0f, data.getSaturationLevel() - cut));
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        APPLIED.remove(event.getEntity().getUUID());
    }
}
