package dev.maire.nourished.datagen.book.compatmods;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class OverviewEntry extends EntryProvider {

    public static final String ID = "overview";

    public OverviewEntry(CategoryProvider parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {

        this.page("overview", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Mod Compatibility");

        this.pageText("""
                Nourished automatically detects installed food mods and assigns their foods to the appropriate groups via tag-based classification.

                This chapter contains entries for supported mods. Entries only appear when the relevant mod is installed. If a mod you use is not listed here, its foods may still be classified automatically — check tooltips.
                """);

        this.page("supported_mods", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Supported Mods");

        this.pageText("""
                Mods with dedicated book entries:

                **Croptopia** — fruits, vegetables, nuts, grains.
                **Farmer's Delight** — crops, multi-group cooked meals.
                **Pam's HarvestCraft 2** — hundreds of crops across all groups.
                **Legendary Survival Overhaul** — effects compatibility notes.
                **Spice of Life: Onion** — how it interacts with diminishing returns.

                Entries only appear when the relevant mod is installed.
                """);

        this.page("adding_custom_foods", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Adding Custom Foods");

        this.pageText("""
                If you are a server owner or modpack maker and want to ensure specific modded foods are classified correctly, you can override assignments using datapacks.

                See the **Server Owners** chapter — specifically **Datapack Overrides** — for full instructions on assigning custom foods to groups and adjusting their nutrition values.
                """);
    }

    @Override
    protected String entryName() {
        return "Mod Compatibility";
    }

    @Override
    protected String entryDescription() {
        return "How Nourished detects and classifies foods from other mods.";
    }

    @Override
    protected Pair<Integer, Integer> entryBackground() {
        return EntryBackground.DEFAULT;
    }

    @Override
    protected BookIconModel entryIcon() {
        return BookIconModel.create(Items.CRAFTING_TABLE);
    }

    @Override
    protected String entryId() {
        return ID;
    }
}
