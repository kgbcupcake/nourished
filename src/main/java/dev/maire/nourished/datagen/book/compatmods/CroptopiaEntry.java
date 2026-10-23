package dev.maire.nourished.datagen.book.compatmods;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class CroptopiaEntry extends EntryProvider {

    public static final String ID = "croptopia";

    public CroptopiaEntry(CategoryProvider parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {

        this.page("croptopia", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Croptopia");

        this.pageText("""
                Croptopia massively expands the crop roster and touches every food group. Its biggest impact is on **Fruits** and **Vegetables**, where it adds dozens of new families that make rotation trivially easy.\s\s
                With Croptopia installed, maintaining a diverse diet becomes natural rather than deliberate.
                """);

        this.page("fruits", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Fruits: Croptopia");

        this.pageText("""
                Croptopia adds many distinct fruit families. Key ones to grow:\s\s
                - **Strawberry**: fast-growing, excellent nutrition.
                - **Peach / Mango / Kiwi**: tree fruits, slower but high value.
                - **Grape**: vine crop; good density per plot.
                - **Pineapple**: tropical; high nutrition per harvest.
                - **Tomato**: fast, pairs well with vegetable rotation.
                """);

        this.page("vegetables", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Vegetables: Croptopia");

        this.pageText("""
                Croptopia vegetable additions include:\s\s
                - **Corn**: high-yield crop; excellent base vegetable.
                - **Onion / Leek**: fast-growing alliums; distinct family.
                - **Spinach / Lettuce**: leafy greens; quick crop cycle.
                - **Pepper / Chili**: spicy vegetables; unique family.
                - **Artichoke / Asparagus**: slow but high nutrition value.
                """);

        this.page("proteins_and_grains", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Proteins & Grains");

        this.pageText("""
                Croptopia also adds to Proteins and Grains:\s\s
                - **Almonds / Peanuts / Cashews**: nut-family proteins; no animal farm needed.
                - **Tofu**: plant-based protein made from soybeans.
                - **Rice**: adds a Grains family distinct from wheat.
                - **Oats / Corn Flour**: alternative grain crops.\s\s
                Nuts make a great supplemental protein for players who prefer less combat-heavy farming.
                """);

        this.page("rotation_advice", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Rotation Advice");

        this.pageText("""
                With Croptopia, the main risk is **over-focusing** on a few favourite crops and ignoring the rest. The mod gives you so many options that it is easy to default to strawberries for Fruits every day.\s\s
                Set up a multi-crop farm covering at least 3 distinct families per group. Croptopia's crop variety is only an advantage if you actually use it.
                """);
    }

    @Override
    protected String entryName() {
        return "Croptopia";
    }

    @Override
    protected String entryDescription() {
        return "A huge crop roster spanning every food group.";
    }

    @Override
    protected Pair<Integer, Integer> entryBackground() {
        return EntryBackground.DEFAULT;
    }

    @Override
    protected BookIconModel entryIcon() {
        return BookIconModel.create(Items.APPLE);
    }

    @Override
    protected String entryId() {
        return ID;
    }
}
