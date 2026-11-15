package dev.maire.nourished.datagen.book.advanced;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import dev.maire.nourished.datagen.book.GuideEntry;
import net.minecraft.world.item.Items;

import static dev.maire.nourished.datagen.book.GuideColors.*;

public class ConfigurationEntry extends GuideEntry {

    public static final String ID = "configuration";

    public ConfigurationEntry(CategoryProvider parent) {
        super(parent, ID, "Configuration", "Where every setting lives.", Items.COMPARATOR);
    }

    @Override
    protected void generatePages() {
        spotlight("configuration", Items.COMPARATOR, "Configuration",
                "Almost everything in this book can be changed. Every number here is a " + hl(GOLD, "default") + ".",
                "Start in game, then go to the files if you need more.");

        text("in_game", "In Game",
                hl(INFO, "Nourished Settings") + ": the main gameplay toggles, by feature.",
                hl(INFO, "Command Center") + ": one place for every Nourished screen, including the full config list.",
                "Both have keybinds in Controls, unbound by default.");

        text("files", "Files",
                "Everything is saved under " + hl(MUTED, "config/nourished/") + ":",
                hl(MUTED, "nutrition/") + ": groups, curves, food values",
                hl(MUTED, "gameplay/") + ": effects, deficiency, locks",
                hl(MUTED, "overrides/") + ": per-food overrides",
                hl(MUTED, "modules/") + ": raw food, activity",
                "Each folder has a Read_Me explaining its files.");

        text("server_client", "Server and Client",
                "Gameplay settings belong to the " + hl(BAD, "server") + ". On a dedicated server its copy wins and is synced to players.",
                "HUD layout, colors and other display settings are " + hl(GOOD, "yours") + " and stay on your machine.",
                "Run " + hl(MUTED, "/nourished reload") + " after editing a file.");

        text("sharing", "Sharing a Setup",
                "Whole configurations can be saved and shared as a single share code from the config screen.",
                "Handy for passing a server's setup to players or another pack.");
    }
}
