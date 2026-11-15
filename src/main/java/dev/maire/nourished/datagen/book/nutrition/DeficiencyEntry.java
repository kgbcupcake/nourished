package dev.maire.nourished.datagen.book.nutrition;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import dev.maire.nourished.datagen.book.GuideEntry;
import net.minecraft.world.item.Items;

import static dev.maire.nourished.datagen.book.GuideColors.*;

public class DeficiencyEntry extends GuideEntry {

    public static final String ID = "deficiency";

    public DeficiencyEntry(CategoryProvider parent) {
        super(parent, ID, "Deficiency", "What happens when a group stays low for a long time.", Items.BONE);
    }

    @Override
    protected void generatePages() {
        spotlight("deficiency", Items.BONE, "Deficiency",
                "A low bar for a moment is a penalty. A low bar for " + hl(BAD, "hours") + " is a deficiency.",
                "While a group sits near empty, pressure builds over play time and you become " + hl(BAD, "Malnourished") + ".");

        text("tiers", "Three Levels",
                hl(GOLD, "Level 1") + " after 20 minutes: activity drains you faster, natural regen slows.",
                hl(BAD, "Level 2") + " after 60 minutes: food fills you less, and you move and mine slower.",
                hl(BAD, "Level 3") + " after 120 minutes: all of the above, worse, plus weaker attacks.",
                "Your worst group sets your level.");

        text("recovery", "Recovering",
                "Get the group back to a healthy level and the pressure drains, twice as fast as it built.",
                "In between, it holds. One snack won't undo hours of neglect, but it won't get worse either.",
                "Milk doesn't clear it.");

        text("where_to_look", "Where to Look",
                "The Nutritional Panel's " + hl(INFO, "Deficiency") + " tab shows pressure per group, time to the next level, and when you last ate each group.",
                "You get a toast when a group gets worse and when you recover.",
                "Type " + hl(MUTED, "/nourished deficiency") + " for your status in chat.");
    }
}
