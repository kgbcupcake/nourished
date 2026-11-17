package dev.maire.nourished.datagen.book.gettingstarted;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import dev.maire.nourished.datagen.book.GuideEntry;
import net.minecraft.world.item.Items;

import static dev.maire.nourished.datagen.book.GuideColors.*;

public class FirstDayEntry extends GuideEntry {

    public static final String ID = "first_day";

    public FirstDayEntry(CategoryProvider parent) {
        super(parent, ID, "Your First Day", "A survival guide for your first day with Nourished active.", Items.COOKED_BEEF);
    }

    @Override
    protected void generatePages() {
        text("your_first_day", "Your First Day",
                "Your food group bars start at a moderate level. You won't feel anything yet, but they slowly drain as you play.",
                "Your only job on day one: " + hl(GOOD, "eat a variety of foods") + " before any bar runs low.");

        text("early_food_sources", "Early Food Sources",
                hl(FRUITS, "Fruits") + ": apples, sweet berries, melon slices.",
                hl(VEGETABLES, "Vegetables") + ": carrots, potatoes and beetroot from villages.",
                hl(PROTEINS, "Proteins") + ": cooked chicken, beef, pork or fish.");

        text("more_early_sources", "More Early Sources",
                hl(GRAINS, "Grains") + ": bread from village chests, or plant wheat right away.",
                hl(DAIRY, "Dairy") + ": a bucket of milk from any cow covers Dairy in a pinch.");

        text("end_of_day_one", "End of Day One",
                "Go to bed with every bar at least half full and you wake up with the "
                        + link("Sleep Bonus", GETTING_STARTED, SleepBonusEntry.ID) + ".",
                "Eat a little of everything, sleep well, repeat. That's the whole loop.");

        text("next_steps", "Next Steps",
                "Day one done. The rest of the book is a reference library, open whenever you need it:",
                link("Your First Farm", GETTING_STARTED, FirstFarmEntry.ID) + ": a small farm that covers every group.",
                link("Daily Routine", GETTING_STARTED, DailyRoutineEntry.ID) + ": the loop that keeps you balanced.",
                "The " + hl(INFO, "Food") + ", " + hl(INFO, "Nutrition") + " and " + hl(INFO, "Activity") + " chapters explain each system in depth.");
    }
}
