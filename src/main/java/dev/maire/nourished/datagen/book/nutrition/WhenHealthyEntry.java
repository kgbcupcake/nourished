package dev.maire.nourished.datagen.book.nutrition;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import dev.maire.nourished.datagen.book.GuideEntry;
import dev.maire.nourished.datagen.book.progression.FoodGroupMilestonesEntry;
import net.minecraft.world.item.Items;

import static dev.maire.nourished.datagen.book.GuideColors.*;

public class WhenHealthyEntry extends GuideEntry {

    public static final String ID = "when_healthy";

    public WhenHealthyEntry(CategoryProvider parent) {
        super(parent, ID, "When You're Healthy", "The Balance Bonus and what it gives you.", Items.GOLDEN_APPLE);
    }

    @Override
    protected void generatePages() {
        spotlight("balance_bonus", Items.GOLDEN_APPLE, "Balance Bonus",
                "Keep " + hl(GOOD, "all five groups above 75%") + " and you get:",
                hl(BAD, "Health Boost I"),
                hl(BAD, "Regeneration I"),
                "Both stay active as long as you stay balanced.");

        text("why", "Why It Exists",
                "**Why reward balance?** Nourished wants you to eat like a real diet, not max one group and ignore the rest.",
                "The bonus is the payoff for keeping everything topped up.");

        text("more_rewards", "More Rewards",
                "Eating a group consistently over time also earns " + link("milestones", PROGRESSION, FoodGroupMilestonesEntry.ID)
                        + ", each with its own temporary buff.",
                "Server owners can change every effect here. These are the defaults.");
    }
}
