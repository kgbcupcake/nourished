package dev.maire.nourished.datagen.book.nutrition;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import dev.maire.nourished.datagen.book.GuideEntry;
import net.minecraft.world.item.Items;

import static dev.maire.nourished.datagen.book.GuideColors.*;

public class NutrientsEntry extends GuideEntry {

    public static final String ID = "nutrients";

    public NutrientsEntry(CategoryProvider parent) {
        super(parent, ID, "Nutrient Levels", "Omega-3, Fiber, Vitamins and the rest.", Items.SALMON);
    }

    @Override
    protected void generatePages() {
        spotlight("nutrients", Items.SALMON, "Nutrient Levels",
                "Underneath the five groups, foods also carry " + hl(INFO, "nutrients") + ":",
                "Omega-3, Fiber, Vitamins, Minerals, Fat, Protein and Carbs.",
                "They fill when you eat and drain slowly, just like the group bars.");

        text("where_from", "Where They Come From",
                "Salmon is rich in Omega-3, vegetables bring Fiber, bread brings Carbs.",
                "Cooked and crafted foods inherit nutrients from their ingredients, so a stew carries what went into it.");

        text("effects", "What They Do",
                "On their own, nutrients are information: they show which foods round out your diet.",
                "Servers can turn on nutrient effects with " + hl(MUTED, "builtinEffects") + ". Then every nutrient above half gives Haste, high Omega-3 gives Dolphin's Grace, and a very low one gives a penalty that "
                        + hl(BAD, "grows stronger") + " the longer it lasts.");

        text("where_to_look", "Where to Look",
                "Hold " + hl(MUTED, "Shift") + " on a food tooltip to see its nutrients.",
                "The Diet Screen has a Nutrients box, and the Nutritional Panel has a Nutrients tab.",
                "Bind " + hl(MUTED, "Open Nutrients") + " in Controls for the detail screen: trends, recent foods and the best sources for each one.");
    }
}
