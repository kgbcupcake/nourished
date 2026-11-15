package dev.maire.nourished.datagen.book.activity;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import dev.maire.nourished.datagen.book.GuideEntry;
import net.minecraft.world.item.Items;

import static dev.maire.nourished.datagen.book.GuideColors.*;

public class ActivityEntry extends GuideEntry {

    public static final String ID = "activity";

    public ActivityEntry(CategoryProvider parent) {
        super(parent, ID, "Activity", "Hard work burns through your food.", Items.LEATHER_BOOTS);
    }

    @Override
    protected void generatePages() {
        spotlight("activity", Items.LEATHER_BOOTS, "Activity",
                "Your bars don't only drain with time. " + hl(ACTIVITY, "Doing things") + " burns through them too.",
                "Sprinting, swimming, mining and fighting all cost a little from every food group.");

        text("why", "Why It Exists",
                "**Why does mining make me hungry?** A miner and a farmer shouldn't need the same diet.",
                "The more active you are, the more you need to eat to stay balanced.");

        text("tracking", "Real Tracking",
                "Nourished records what you actually do each day: distance sprinted and swum, blocks mined, kills, and times you hit empty.",
                "That record feeds the Activity Log and activity milestones.",
                "Each activity can be switched off on its own by the server.");
    }
}
