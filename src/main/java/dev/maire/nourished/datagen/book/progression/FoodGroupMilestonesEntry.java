package dev.maire.nourished.datagen.book.progression;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import dev.maire.nourished.datagen.book.GuideEntry;
import net.minecraft.world.item.Items;

import static dev.maire.nourished.datagen.book.GuideColors.*;

public class FoodGroupMilestonesEntry extends GuideEntry {

    public static final String ID = "food_group_milestones";

    public FoodGroupMilestonesEntry(CategoryProvider parent) {
        super(parent, ID, "Food Group Milestones", "Three tiers for every food group.", Items.GOLDEN_CARROT);
    }

    @Override
    protected void generatePages() {
        text("tiers", "Three Tiers",
                "Every group has its own three milestones, counted by how much of that group you've eaten in total:",
                hl(GOOD, "Beginner") + ": half a bar. Level I buff for 1 minute.",
                hl(GOLD, "Journeyman") + ": 2 full bars. Level II for 2 minutes.",
                hl(BAD, "Master") + ": 5 full bars. Level III for 5 minutes.");

        text("rewards", "Rewards",
                hl(FRUITS, "Fruits") + ": Luck",
                hl(VEGETABLES, "Vegetables") + ": Health Boost",
                hl(PROTEINS, "Proteins") + ": Strength",
                hl(GRAINS, "Grains") + ": Haste",
                hl(DAIRY, "Dairy") + ": Resistance");
    }
}
