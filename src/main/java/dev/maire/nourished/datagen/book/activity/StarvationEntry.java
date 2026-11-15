package dev.maire.nourished.datagen.book.activity;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import dev.maire.nourished.datagen.book.GuideEntry;
import net.minecraft.world.item.Items;

import static dev.maire.nourished.datagen.book.GuideColors.*;

public class StarvationEntry extends GuideEntry {

    public static final String ID = "starvation";

    public StarvationEntry(CategoryProvider parent) {
        super(parent, ID, "Starvation", "Hitting empty has a knock-on cost.", Items.ROTTEN_FLESH);
    }

    @Override
    protected void generatePages() {
        text("starvation", "Starvation",
                "When any group drops into its " + hl(BAD, "critical") + " range, every other bar takes a one-time hit too, about 2% by default.",
                "One empty group drags the rest down with it.");

        text("what_to_do", "What Do I Do?",
                "Don't let a group hit empty. A critical toast warns you before it happens.",
                "If you see the toast, eat from that group right away.");
    }
}
