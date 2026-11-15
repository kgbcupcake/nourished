package dev.maire.nourished.datagen.book.advanced;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import dev.maire.nourished.datagen.book.GuideEntry;
import net.minecraft.world.item.Items;

import static dev.maire.nourished.datagen.book.GuideColors.*;

public class KubeJsEntry extends GuideEntry {

    public static final String ID = "kubejs";

    public KubeJsEntry(CategoryProvider parent) {
        super(parent, ID, "KubeJS", "Scripting hooks for modpack authors.", Items.COMMAND_BLOCK);
    }

    @Override
    protected void generatePages() {
        text("events", "Server Events",
                "With KubeJS installed, server scripts can listen with " + hl(MUTED, "NourishedEvents") + ":",
                hl(INFO, "nutrientChanged") + ", " + hl(INFO, "nutrientModifier") + ", " + hl(INFO, "foodEaten"),
                hl(INFO, "nutrientCritical") + ", " + hl(INFO, "nutrientExcess"),
                hl(INFO, "rawFoodPenalty") + ", " + hl(INFO, "gutHealthChanged"),
                "nutrientModifier can change how much a meal gives.");

        text("bindings", "Startup and Bindings",
                "Startup scripts can add a whole new food group with " + hl(MUTED, "NourishedAPI.registerNutrient") + ".",
                hl(MUTED, "NourishedNutrients") + " reads and sets nutrient levels and item values.",
                "Activity milestones can be added from scripts too.");

        text("more", "More",
                "The full event and binding list is in API.md on the Nourished GitHub.");
    }
}
