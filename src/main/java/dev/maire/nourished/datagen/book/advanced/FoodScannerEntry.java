package dev.maire.nourished.datagen.book.advanced;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import dev.maire.nourished.datagen.book.GuideEntry;
import net.minecraft.world.item.Items;

import static dev.maire.nourished.datagen.book.GuideColors.*;

public class FoodScannerEntry extends GuideEntry {

    public static final String ID = "food_scanner";

    public FoodScannerEntry(CategoryProvider parent) {
        super(parent, ID, "Food Scanner", "Find every food and see how it was classified.", Items.SPYGLASS);
    }

    @Override
    protected void generatePages() {
        spotlight("food_scanner", Items.SPYGLASS, "Food Scanner",
                "Nourished works out most modded foods on its own, from tags, recipes and even item names.",
                "The scanner shows you what it decided, and what it couldn't.");

        text("running_it", "Running It",
                "Run " + hl(MUTED, "/nourished scan_analysis") + ". Reports are written to "
                        + hl(MUTED, "config/nourished/scanner_analysis/") + ":",
                hl(GOOD, "multi_value_recommendations.json") + ": foods it placed confidently.",
                hl(BAD, "unassigned_sources.txt") + ": foods that need a human.",
                hl(INFO, "scanner_metrics.txt") + ": totals.");

        text("fixing", "Fixing What It Missed",
                "Copy the recommendations into a datapack tag to make them permanent, or fix single items in the Item Editor.",
                "The Food Scanner config screen lists every scanned item with an edit button.");
    }
}
