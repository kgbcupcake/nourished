package dev.maire.nourished.datagen.book.progression;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import dev.maire.nourished.datagen.book.GuideEntry;
import net.minecraft.world.item.Items;

import static dev.maire.nourished.datagen.book.GuideColors.*;

public class PerfectlyBalancedEntry extends GuideEntry {

    public static final String ID = "perfectly_balanced";

    public PerfectlyBalancedEntry(CategoryProvider parent) {
        super(parent, ID, "Perfectly Balanced", "The hidden milestone.", Items.ENCHANTED_GOLDEN_APPLE);
    }

    @Override
    protected void generatePages() {
        spotlight("perfectly_balanced", Items.ENCHANTED_GOLDEN_APPLE, "Perfectly Balanced",
                "A hidden milestone for players who keep " + hl(GOLD, "every group") + " going together over the long run.",
                "Reward: " + hl(GOOD, "Absorption II") + " for 5 minutes.");

        text("how", "How to Get It",
                "You can't rush it by eating one group. Every group has to keep pace with the others.",
                "Follow the daily loop and it arrives on its own.");
    }
}
