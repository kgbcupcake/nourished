package dev.maire.nourished.datagen.book.progression;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import dev.maire.nourished.datagen.book.GuideEntry;
import net.minecraft.world.item.Items;

import static dev.maire.nourished.datagen.book.GuideColors.*;

public class MilestonesEntry extends GuideEntry {

    public static final String ID = "milestones";

    public MilestonesEntry(CategoryProvider parent) {
        super(parent, ID, "Milestones", "Long-term goals for eating well.", Items.NETHER_STAR);
    }

    @Override
    protected void generatePages() {
        spotlight("milestones", Items.NETHER_STAR, "Milestones",
                "Nourished keeps a running total of everything you eat and do.",
                "Hit a target and you earn a " + hl(GOLD, "milestone") + ": a temporary buff and an advancement.");

        text("why", "Why It Exists",
                "**What am I working toward?** Eating well every day is a habit. Milestones reward the habit, not one good meal.",
                "Progress never goes backwards, even if a bar drops.");

        text("where_to_look", "Where to Look",
                "Milestones show up in your " + hl(INFO, "Advancements") + " tab under Nourished.",
                "Modpacks can change the goals and rewards, or add their own through datapacks or KubeJS.");
    }
}
