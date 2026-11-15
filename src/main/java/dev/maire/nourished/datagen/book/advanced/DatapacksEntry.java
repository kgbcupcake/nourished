package dev.maire.nourished.datagen.book.advanced;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import dev.maire.nourished.datagen.book.GuideEntry;
import net.minecraft.world.item.Items;

import static dev.maire.nourished.datagen.book.GuideColors.*;

public class DatapacksEntry extends GuideEntry {

    public static final String ID = "datapacks";

    public DatapacksEntry(CategoryProvider parent) {
        super(parent, ID, "Datapacks", "Ship changes with a pack instead of a config.", Items.CHEST);
    }

    @Override
    protected void generatePages() {
        text("tags", "Food Group Tags",
                "Put a food in a group with an item tag:",
                hl(MUTED, "data/nourished/tags/item/nutrients/vegetables.json"),
                "Use " + hl(MUTED, "\"replace\": false") + " so your entries add to Nourished's instead of replacing them.");

        text("data_folders", "Data Folders",
                "Under " + hl(MUTED, "data/<namespace>/nourished/") + ":",
                hl(MUTED, "effects/") + ", " + hl(MUTED, "milestones/") + ", " + hl(MUTED, "tracker_milestones/"),
                hl(MUTED, "source_synergies/") + " for food combos",
                hl(MUTED, "micros/") + ", " + hl(MUTED, "micro_values/") + ", " + hl(MUTED, "micro_effects/") + " for nutrients",
                hl(MUTED, "food_overrides/") + " for per-food values");

        text("config_overrides", "Config Overrides",
                "Many config files can also be overridden from a datapack under " + hl(MUTED, "data/<namespace>/config/")
                        + ", for example " + hl(MUTED, "config/gameplay/deficiency.json") + ".",
                "Run " + hl(MUTED, "/nourished diagnostics") + " to see any datapack file that failed to load.");
    }
}
