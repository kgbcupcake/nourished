package dev.maire.nourished.datagen.book.gettingstarted;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import dev.maire.nourished.datagen.book.GuideEntry;
import dev.maire.nourished.datagen.book.food.FoodGroupsEntry;
import dev.maire.nourished.datagen.book.nutrition.BalanceScoreEntry;
import dev.maire.nourished.datagen.book.nutrition.DiminishingReturnsEntry;
import net.minecraft.world.item.Items;

import static dev.maire.nourished.datagen.book.GuideColors.*;

public class EatVarietyEntry extends GuideEntry {

    public static final String ID = "eat_variety";

    public EatVarietyEntry(CategoryProvider parent) {
        super(parent, ID, "Eat a Variety", "Food groups, variety and balance in one page.", Items.APPLE);
    }

    @Override
    protected void generatePages() {
        text("food_groups", "Five Food Groups",
                "Every food feeds one or more groups: " + hl(FRUITS, "Fruits") + ", " + hl(VEGETABLES, "Vegetables") + ", "
                        + hl(PROTEINS, "Proteins") + ", " + hl(GRAINS, "Grains") + " and " + hl(DAIRY, "Dairy") + ".",
                "Each group has its own bar. " + link("Food Groups", FOOD, FoodGroupsEntry.ID) + " lists what counts.");

        text("variety", "Why Variety?",
                "**Why does my diet change?** Your body doesn't stay nourished forever. Bars slowly drain, so filling one up once and forgetting it doesn't work.",
                "Eating the same thing again also gives less each time. Rotate. See "
                        + link("Diminishing Returns", NUTRITION, DiminishingReturnsEntry.ID) + ".");

        text("balance", "Balance",
                "Nourished cares about how " + hl(GOOD, "even") + " your bars are, not just how full.",
                "Keep all five healthy and you get bonuses. Let one slide and you get a penalty. See "
                        + link("Balance", NUTRITION, BalanceScoreEntry.ID) + ".");
    }
}
