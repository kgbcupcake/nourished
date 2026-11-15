package dev.maire.nourished.datagen.book.food;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import dev.maire.nourished.datagen.book.GuideEntry;
import net.minecraft.world.item.Items;

import static dev.maire.nourished.datagen.book.GuideColors.*;

public class FoodCombosEntry extends GuideEntry {

    public static final String ID = "food_combos";

    public FoodCombosEntry(CategoryProvider parent) {
        super(parent, ID, "Food Combos", "Some foods are better eaten together.", Items.COOKED_CHICKEN);
    }

    @Override
    protected void generatePages() {
        spotlight("food_combos", Items.COOKED_CHICKEN, "Food Combos",
                "Eat two foods that go together within a few seconds of each other and you get a small " + hl(GOOD, "bonus") + ".",
                "The second food tops up an extra group and gives a little more for a few seconds.");

        text("default_combos", "Default Combos",
                hl(PROTEINS, "Cooked Chicken") + " + " + hl(VEGETABLES, "Carrot") + ": extra Vegetables",
                hl(GRAINS, "Bread") + " + " + hl(DAIRY, "Milk") + ": extra Dairy",
                hl(PROTEINS, "Steak") + " + " + hl(GRAINS, "Bread") + ": extra Proteins",
                "Order doesn't matter.");

        text("custom_combos", "Custom Combos",
                "Modpacks can add their own through datapacks in " + hl(MUTED, "nourished/source_synergies/") + ".",
                "Server owners can switch combos off with " + hl(MUTED, "enableSynergies") + ".");
    }
}
