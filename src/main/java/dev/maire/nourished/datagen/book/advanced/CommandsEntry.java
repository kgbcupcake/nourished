package dev.maire.nourished.datagen.book.advanced;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import dev.maire.nourished.datagen.book.GuideEntry;
import net.minecraft.world.item.Items;

import static dev.maire.nourished.datagen.book.GuideColors.*;

public class CommandsEntry extends GuideEntry {

    public static final String ID = "commands";

    public CommandsEntry(CategoryProvider parent) {
        super(parent, ID, "Commands", "The /nourished commands worth knowing.", Items.REPEATING_COMMAND_BLOCK);
    }

    @Override
    protected void generatePages() {
        text("for_everyone", "For Everyone",
                hl(MUTED, "/nourished deficiency") + ": your deficiency status.",
                hl(MUTED, "/nourished nutrients") + ": your nutrient levels.",
                hl(MUTED, "/nourished nutrients item") + ": what the held item gives.");

        text("for_admins", "For Admins",
                hl(MUTED, "/nourished reload") + ": reload config and data.",
                hl(MUTED, "/nourished diagnostics") + ": report broken files.",
                hl(MUTED, "/nourished scan_analysis") + ": run the Food Scanner.",
                hl(MUTED, "/nourished registrations") + ": what's registered.",
                hl(MUTED, "/nourished deficiency set") + " and " + hl(MUTED, "clear") + ": adjust a player.");
    }
}
