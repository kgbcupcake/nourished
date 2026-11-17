package dev.maire.nourished.datagen.book.gettingstarted;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import dev.maire.nourished.datagen.book.GuideEntry;
import dev.maire.nourished.datagen.book.guthealth.RawFoodEntry;
import dev.maire.nourished.datagen.book.nutrition.DiminishingReturnsEntry;
import net.minecraft.world.item.Items;

import static dev.maire.nourished.datagen.book.GuideColors.*;

public class FirstFarmEntry extends GuideEntry {

    public static final String ID = "first_farm";

    public FirstFarmEntry(CategoryProvider parent) {
        super(parent, ID, "Your First Farm", "A small farm that covers every food group.", Items.WHEAT);
    }

    @Override
    protected void generatePages() {
        spotlight("setting_up_a_farm", Items.WHEAT, "Setting Up a Farm",
                "Plant " + hl(GRAINS, "wheat") + ", " + hl(VEGETABLES, "carrots") + ", " + hl(VEGETABLES, "potatoes")
                        + " and " + hl(VEGETABLES, "beetroot") + " early.",
                "Add a cow pen for milk and beef and a couple of apple trees or berry bushes. That covers all five groups.");

        text("cooking_matters", "Cooking Matters",
                "Raw meat still counts as " + hl(PROTEINS, "Proteins") + ", but eating it can upset your stomach.",
                "Cook it first. See " + link("Raw Food", GUT_HEALTH, RawFoodEntry.ID) + " for what can go wrong.");

        text("rotate", "Grow More Than One Thing",
                "Eating the same food over and over gives less each time.",
                "A farm with two or three crops per group lets you rotate. See "
                        + link("Diminishing Returns", NUTRITION, DiminishingReturnsEntry.ID) + ".");
    }
}
