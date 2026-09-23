package dev.maire.nourished.core.book;

import com.klikli_dev.modonomicon.item.ModonomiconItem;
import com.klikli_dev.modonomicon.registry.DataComponentRegistry;
import dev.maire.nourished.core.Nourished;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NourishedBookItems {

    private NourishedBookItems() {}

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, Nourished.MODID);

    // Registered under our own namespace (instead of reusing modonomicon:modonomicon) so tooltip
    // mod-name attribution (NeoForge + EMI) reads "Nourished" once, not "Modonomicon" twice.
    public static final DeferredHolder<Item, Item> NOURISHED_BOOK = ITEMS.register("nourished_book",
            () -> new ModonomiconItem(new Item.Properties()
                    .component(DataComponentRegistry.BOOK_ID.get(), ResourceLocation.fromNamespaceAndPath(Nourished.MODID, "nourished_guide"))));

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
