package dev.maire.nourished.datagen.book.progression;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import dev.maire.nourished.datagen.book.GuideEntry;
import net.minecraft.world.item.Items;

import static dev.maire.nourished.datagen.book.GuideColors.*;

public class ActivityMilestonesEntry extends GuideEntry {

    public static final String ID = "activity_milestones";

    public ActivityMilestonesEntry(CategoryProvider parent) {
        super(parent, ID, "Activity Milestones", "Rewards for how much you move, mine and fight.", Items.DIAMOND_PICKAXE);
    }

    @Override
    protected void generatePages() {
        text("movement", "Movement",
                hl(ACTIVITY, "Getting Somewhere") + ": sprint 1,000 blocks. Speed I.",
                hl(ACTIVITY, "Marathoner") + ": sprint 25,000 blocks. Speed II.",
                hl(INFO, "First Swim") + ": swim 500 blocks. Water Breathing.",
                hl(INFO, "Aquatic Explorer") + ": swim 2,000 blocks. Dolphin's Grace.");

        text("work_and_war", "Mining & Combat",
                hl(GOLD, "Stonebreaker") + ": mine 100 blocks. Haste I.",
                hl(GOLD, "Master Miner") + ": mine 10,000 blocks. Haste II.",
                hl(BAD, "First Blood") + ": 10 kills. Strength I.",
                hl(BAD, "Monster Slayer") + ": 1,000 kills. Strength II.",
                "These count over your whole lifetime.");
    }
}
