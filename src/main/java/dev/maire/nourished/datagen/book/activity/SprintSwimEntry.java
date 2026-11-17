package dev.maire.nourished.datagen.book.activity;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import dev.maire.nourished.datagen.book.GuideEntry;
import net.minecraft.world.item.Items;

import static dev.maire.nourished.datagen.book.GuideColors.*;

public class SprintSwimEntry extends GuideEntry {

    public static final String ID = "sprint_swim";

    public SprintSwimEntry(CategoryProvider parent) {
        super(parent, ID, "Sprinting & Swimming", "Moving fast costs food.", Items.FEATHER);
    }

    @Override
    protected void generatePages() {
        text("sprinting", "Sprinting",
                "While you sprint, every bar drains a little " + hl(ACTIVITY, "faster") + " than normal.",
                "Long runs add up. Walk when you're not in a hurry.");

        text("swimming", "Swimming",
                "Swimming works the same way. Long ocean crossings eat into your food.",
                "A boat costs nothing.");

        text("what_to_do", "What Do I Do?",
                "Pack food from every group before a long trip.",
                "Deficiency makes these costs bigger, so a malnourished sprinter drains even faster.");
    }
}
