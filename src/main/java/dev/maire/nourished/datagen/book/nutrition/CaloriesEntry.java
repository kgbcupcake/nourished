package dev.maire.nourished.datagen.book.nutrition;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import dev.maire.nourished.datagen.book.GuideEntry;
import net.minecraft.world.item.Items;

import static dev.maire.nourished.datagen.book.GuideColors.*;

public class CaloriesEntry extends GuideEntry {

    public static final String ID = "calories";

    public CaloriesEntry(CategoryProvider parent) {
        super(parent, ID, "Calories", "How much energy you ate today.", Items.CAKE);
    }

    @Override
    protected void generatePages() {
        spotlight("calories", Items.CAKE, "Calories",
                "Food groups track " + hl(INFO, "what") + " you eat. Calories track " + hl(GOLD, "how much") + ".",
                "Every food adds to today's total, and you get a quick toast showing the change.");

        text("daily_goal", "Your Daily Goal",
                "Your total is shown as a share of a daily goal, " + hl(GOLD, "2000") + " by default.",
                "The display changes once you go over, so you can see when you've overdone it.",
                "Totals reset each day and the last week is kept.");

        text("where_to_look", "Where to Look",
                "The Diet Screen's Calories box shows today at a glance.",
                "The Nutritional Panel's " + hl(INFO, "Calorie History") + " tab shows today, yesterday and the days before.");

        text("too_few", "Eating Too Little",
                "By default, a low calorie day has no penalty.",
                "Servers can turn on calorie deficiency, which counts a few low days in a row like a neglected food group. See "
                        + link("Deficiency", NUTRITION, DeficiencyEntry.ID) + ".");
    }
}
