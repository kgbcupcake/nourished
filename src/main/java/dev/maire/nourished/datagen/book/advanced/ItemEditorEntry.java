package dev.maire.nourished.datagen.book.advanced;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import dev.maire.nourished.datagen.book.GuideEntry;
import net.minecraft.world.item.Items;

import static dev.maire.nourished.datagen.book.GuideColors.*;

public class ItemEditorEntry extends GuideEntry {

    public static final String ID = "item_editor";

    public ItemEditorEntry(CategoryProvider parent) {
        super(parent, ID, "Item Editor", "Tune any single food in game.", Items.ANVIL);
    }

    @Override
    protected void generatePages() {
        spotlight("item_editor", Items.ANVIL, "Item Editor",
                "The " + hl(INFO, "Item Editor") + " lets you see and change how Nourished treats one item, without touching a file.",
                "Open it from the Food Scanner's edit buttons, or bind " + hl(MUTED, "Open Item Editor") + " in Controls.");

        text("pages", "What You Can Change",
                hl(GOOD, "Values") + ": which groups it feeds and how much, with a trace of how it was classified.",
                hl(GOOD, "Nutrients") + ": its Omega-3, Fiber and other nutrient values.",
                hl(GOOD, "Calories") + ": calorie settings for that item.",
                hl(GOOD, "Options") + ": exclude it, turn off diminishing returns, or eat it silently.");

        text("tips", "Tips",
                "Drag items straight in from JEI or EMI.",
                "Browse Items filters by modified, excluded or missing nutrition. Bulk Edit changes several at once.",
                "On a server, edits are sent to the server, so they apply to everyone.");
    }
}
