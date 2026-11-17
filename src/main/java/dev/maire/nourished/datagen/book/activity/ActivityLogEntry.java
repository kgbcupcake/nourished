package dev.maire.nourished.datagen.book.activity;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import dev.maire.nourished.datagen.book.GuideEntry;
import net.minecraft.world.item.Items;

import static dev.maire.nourished.datagen.book.GuideColors.*;

public class ActivityLogEntry extends GuideEntry {

    public static final String ID = "activity_log";

    public ActivityLogEntry(CategoryProvider parent) {
        super(parent, ID, "Activity Log", "Where to see what you did today.", Items.WRITABLE_BOOK);
    }

    @Override
    protected void generatePages() {
        text("activity_log", "Activity Log",
                "The " + hl(INFO, "Activity Log") + " is a tab in the Nutritional Panel.",
                "It shows today's total for each activity, with a bar measured against " + hl(GOOD, "your own best day") + ".");

        text("reading_it", "Reading It",
                "Each activity has its own color. A full bar means you matched your record.",
                "Deficiency changes show up here too, so you can see when a group got worse or recovered.");
    }
}
