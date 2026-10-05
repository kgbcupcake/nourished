package dev.maire.nourished.datagen.book.compatmods;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class PamsHarvestcraftEntry extends EntryProvider {

    public static final String ID = "pams_harvestcraft";

    public PamsHarvestcraftEntry(CategoryProvider parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {

        this.page("pams_harvestcraft", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Pam's HarvestCraft 2");

        this.pageText("""
                Pam's HarvestCraft 2 is one of the largest food content mods available, adding hundreds of crops, trees, and recipes across every food group. With Pam's installed, every Nourished group has so many options that diminishing returns become almost impossible to trigger.
                """);

        this.page("fruits", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Fruits: Pam's");

        this.pageText("""
                Pam's adds an enormous fruit roster: strawberries, raspberries, blueberries, blackberries, grapes, mangoes, papayas, figs, dates, and many more. Each is a distinct family.\s\s
                With this many fruit options, the Fruits bar is one of the easiest to maintain. Grow a small orchard of 4–5 different fruit trees and you will never need to think about this group again.
                """);

        this.page("vegetables", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Vegetables: Pam's");

        this.pageText("""
                Pam's vegetable list is equally massive: corn, tomatoes, peppers, cucumbers, zucchini, eggplant, leeks, scallions, and dozens more. Most are fast-growing row crops.\s\s
                A Pam's vegetable garden covering 6–8 different crops will completely eliminate any risk of diminishing returns on Vegetables and keep the bar perpetually full.
                """);

        this.page("proteins_and_dairy", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Proteins & Dairy: Pam's");

        this.pageText("""
                Pam's expands animal products significantly:\s\s
                **Proteins:** tofu, various nut-based proteins, additional fish varieties, and egg dishes provide plant-based alternatives to meat.
                **Dairy:** butter, cheese, cream, and yoghurt give the Dairy group rotation options comparable to what Herbs & Harvest provides: multiple distinct families beyond plain milk.
                """);

        this.page("grains", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Grains: Pam's");

        this.pageText("""
                **Grains:** Pam's adds oats, rye, corn flour, and a range of baked goods (muffins, pies, pancakes), each a distinct grain family.
                """);

        this.page("getting_started", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Getting Started");

        this.pageText("""
                With hundreds of options, the risk with Pam's is **analysis paralysis**: too many choices leading to no clear plan. Keep it simple:\s\s
                Pick **one crop per group** to start with and build from there. A strawberry bush, a tomato plant, some oat seeds, and a cheese recipe covers four groups immediately and gives you a foundation to expand from.
                """);
    }

    @Override
    protected String entryName() {
        return "Pam's HarvestCraft 2";
    }

    @Override
    protected String entryDescription() {
        return "Hundreds of crops and recipes across every food group.";
    }

    @Override
    protected Pair<Integer, Integer> entryBackground() {
        return EntryBackground.DEFAULT;
    }

    @Override
    protected BookIconModel entryIcon() {
        return BookIconModel.create(Items.WHEAT);
    }

    @Override
    protected String entryId() {
        return ID;
    }
}
