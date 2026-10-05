package dev.maire.nourished.datagen.book.compatmods;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class FarmAndCharmEntry extends EntryProvider {

    public static final String ID = "farm_and_charm";

    public FarmAndCharmEntry(CategoryProvider parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {

        this.page("farm_and_charm", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Farm & Charm");

        this.pageText("""
                Farm & Charm adds a cozy farming and cooking experience with new crops, animals, and processed foods. It integrates deeply with Nourished, expanding **Vegetables** and **Grains** the most, with smaller contributions to Fruits and Proteins.
                """);

        this.page("vegetables", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Vegetables: Farm & Charm");

        this.pageText("""
                Key Farm & Charm vegetables:\s\s
                - **Tomato**: fast-growing; distinct family from root vegetables.
                - **Onion**: quick crop; allium family.
                - **Pepper**: nightshade family alongside tomatoes.
                **Pumpkin** (new variants): expands the squash family.
                **Leek / Radish**: root vegetables distinct from carrot and potato.
                """);

        this.page("grains", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Grains: Farm & Charm");

        this.pageText("""
                Farm & Charm adds new grain options:\s\s
                - **Oats**: a distinct grain family; can be used in porridge recipes.
                - **Rye Bread**: made from rye flour; higher nutrition than wheat bread.
                - **Cornbread**: requires corn; another distinct baked grain family.\s\s
                These give Grains more rotation depth and make it easier to avoid diminishing returns on plain wheat bread.
                """);

        this.page("processed_foods", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Processed Foods");

        this.pageText("""
                Farm & Charm's cooking system produces multi-ingredient meals that often provide higher nutrition than their raw components and may count for **multiple food groups at once**.\s\s
                A vegetable stew might cover Vegetables and Grains in one item. Check tooltips on cooked meals to see all groups they contribute to; meals are often more efficient than eating ingredients separately.
                """);

        this.page("getting_started", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Getting Started");

        this.pageText("""
                The earliest Farm & Charm crops to plant for Nourished are **tomatoes** and **onions**: both fast-growing and immediately useful for expanding Vegetable rotation.\s\s
                Once you have a cooking station, prioritize recipes that cover multiple groups. A single stew that covers Vegetables + Proteins is more inventory-efficient than carrying separate items for each group.
                """);
    }

    @Override
    protected String entryName() {
        return "Farm & Charm";
    }

    @Override
    protected String entryDescription() {
        return "A cozy farming overhaul focused on Vegetables and Grains.";
    }

    @Override
    protected Pair<Integer, Integer> entryBackground() {
        return EntryBackground.DEFAULT;
    }

    @Override
    protected BookIconModel entryIcon() {
        return BookIconModel.create(Items.CARROT);
    }

    @Override
    protected String entryId() {
        return ID;
    }
}
