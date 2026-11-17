package dev.maire.nourished.datagen.book.food;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import dev.maire.nourished.datagen.book.GuideEntry;
import net.minecraft.world.item.Items;

import static dev.maire.nourished.datagen.book.GuideColors.*;

public class EatingWhenFullEntry extends GuideEntry {

    public static final String ID = "eating_when_full";

    public EatingWhenFullEntry(CategoryProvider parent) {
        super(parent, ID, "Eating When Full", "Snacking for nutrients when your hunger bar is full.", Items.SWEET_BERRIES);
    }

    @Override
    protected void generatePages() {
        spotlight("eating_when_full", Items.SWEET_BERRIES, "Eating When Full",
                "Vanilla won't let you eat with a full hunger bar. Nourished will, so you can still top up a group.",
                "You get the nutrition, not the hunger.");

        text("light_and_heavy", "Light and Heavy Foods",
                hl(GOOD, "Light") + " foods like berries and snacks can be eaten when full by default.",
                hl(BAD, "Heavy") + " meals still follow the vanilla rule.",
                "Server owners control this with " + hl(MUTED, "enableBlockHeavySources") + " and "
                        + hl(MUTED, "enableBlockLightSource") + ".");

        text("placed_food", "Placed Food",
                "Food eaten straight off a block, like a cake or a modded pie, counts too.",
                "Each bite is treated like eating the item.");
    }
}
