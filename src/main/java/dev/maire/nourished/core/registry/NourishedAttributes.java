package dev.maire.nourished.core.registry;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.core.Nourished;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.core.Holder;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@ApiStatus.Internal
public final class NourishedAttributes {

    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(Registries.ATTRIBUTE, Nourished.MODID);

    public static final DeferredHolder<Attribute, Attribute> NUTRIENT_REGEN_MULTIPLIER =
            ATTRIBUTES.register(
                    "nutrient_regen_multiplier",
                    () -> new RangedAttribute("attribute.name.nourished.nutrient_regen_multiplier", 1.0, 0.01, 10.0)
                            .setSyncable(true)
            );

    public static final DeferredHolder<Attribute, Attribute> NUTRIENT_DECAY_MULTIPLIER =
            ATTRIBUTES.register(
                    "nutrient_decay_multiplier",
                    () -> new RangedAttribute("attribute.name.nourished.nutrient_decay_multiplier", 1.0, 0.01, 10.0)
                            .setSyncable(true)
            );

    private NourishedAttributes() {}

    public static void register(IEventBus modEventBus) {
        ATTRIBUTES.register(modEventBus);
        modEventBus.addListener(NourishedAttributes::onEntityAttributeModification);
    }

    private static void onEntityAttributeModification(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, NUTRIENT_REGEN_MULTIPLIER);
        event.add(EntityType.PLAYER, NUTRIENT_DECAY_MULTIPLIER);
    }

    public static float nutrientRegenMultiplier(ServerPlayer player) {
        return attributeMultiplier(player, NUTRIENT_REGEN_MULTIPLIER);
    }

    public static float nutrientDecayMultiplier(ServerPlayer player) {
        return attributeMultiplier(player, NUTRIENT_DECAY_MULTIPLIER);
    }

    private static float attributeMultiplier(ServerPlayer player, Holder<Attribute> attribute) {
        var inst = player.getAttribute(attribute);
        return inst == null ? 1.0f : (float) inst.getValue();
    }
}
