package dev.maire.nourished.datagen.book.food;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import dev.maire.nourished.datagen.book.GuideEntry;
import dev.maire.nourished.datagen.book.gettingstarted.FoodTooltipsEntry;
import net.minecraft.world.item.Items;

import static dev.maire.nourished.datagen.book.GuideColors.*;

public class FoodGroupsEntry extends GuideEntry {

    public static final String ID = "food_groups";

    public FoodGroupsEntry(CategoryProvider parent) {
        super(parent, ID, "Food Groups", "Quick reference: the five groups and what feeds them.", Items.BOWL);
    }

    @Override
    protected void generatePages() {
        spotlight("overview", Items.BOWL, "Food Groups",
                "Every food Nourished recognises feeds one or more of five groups.",
                "Each group has its own bar that fills when you eat and slowly drains over time.");

        text("what_counts", "What Counts",
                hl(FRUITS, "Fruits") + ": apples, berries, melon",
                hl(VEGETABLES, "Vegetables") + ": carrots, potatoes, beetroot",
                hl(PROTEINS, "Proteins") + ": meat, fish, eggs",
                hl(GRAINS, "Grains") + ": bread, cookies, pie",
                hl(DAIRY, "Dairy") + ": milk, cheese",
                "Mixed meals like stews can count toward several groups at once.");

        text("when_low", "When a Group Runs Low",
                "Below 25%, most groups give a penalty by default:",
                hl(FRUITS, "Fruits") + ": Unluck",
                hl(VEGETABLES, "Vegetables") + ": Slowness",
                hl(PROTEINS, "Proteins") + ": Mining Fatigue",
                hl(GRAINS, "Grains") + ": Weakness",
                hl(DAIRY, "Dairy") + ": none");

        text("need_to_improve", "Need to Improve a Group?",
                "Check a food's tooltip before you eat it. It shows exactly which groups it feeds and by how much.",
                "See " + link("Food Tooltips", GETTING_STARTED, FoodTooltipsEntry.ID) + " and "
                        + link("Reading Tooltips", FOOD, ReadingTooltipsEntry.ID) + ".");
    }
}
