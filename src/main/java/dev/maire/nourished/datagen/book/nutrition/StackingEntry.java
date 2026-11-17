package dev.maire.nourished.datagen.book.nutrition;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import dev.maire.nourished.datagen.book.GuideEntry;
import dev.maire.nourished.datagen.book.gettingstarted.DietScreenEntry;
import net.minecraft.world.item.Items;

import static dev.maire.nourished.datagen.book.GuideColors.*;

public class StackingEntry extends GuideEntry {

    public static final String ID = "stacking";

    public StackingEntry(CategoryProvider parent) {
        super(parent, ID, "How Stacking Works", "Several effects at once, and where to see them.", Items.BREWING_STAND);
    }

    @Override
    protected void generatePages() {
        text("stacking", "How Stacking Works",
                "Each low group applies its own penalty, so three low groups means three debuffs at once.",
                "Weakness, Slowness and Mining Fatigue together make almost anything dangerous.",
                "Bonuses and penalties can be active at the same time from different sources, such as a milestone buff while one group is low.");

        text("viewing", "Seeing Your Effects",
                "Nourished effects show up in your normal effect list with vanilla icons.",
                "The " + link("Diet Screen", GETTING_STARTED, DietScreenEntry.ID) + " lists what your diet is doing to you and why.",
                "Malnourished has its own " + hl(BAD, "icon") + " so you can tell it apart.");
    }
}
