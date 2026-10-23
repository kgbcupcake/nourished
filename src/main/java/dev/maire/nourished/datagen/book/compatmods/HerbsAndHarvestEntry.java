package dev.maire.nourished.datagen.book.compatmods;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class HerbsAndHarvestEntry extends EntryProvider {

    public static final String ID = "herbs_and_harvest";

    public HerbsAndHarvestEntry(CategoryProvider parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {

        this.page("herbs_and_harvest", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Herbs & Harvest");

        this.pageText("""
                Herbs & Harvest focuses on artisanal food production: cured meats, aged cheeses, fermented products, and herb-seasoned dishes. Its biggest contribution to Nourished is **Dairy**, where it finally gives that group rotation options beyond plain milk.
                """);

        this.page("dairy", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Dairy: Aged Cheeses");

        this.pageText("""
                Herbs & Harvest adds aged cheese varieties, each a distinct dairy family:\s\s
                - **Aged Swiss**: medium aging time; balanced nutrition.
                - **Aged Cheddar**: sharp flavour; high nutrition density.
                - **Aged Brie**: slow to produce but very high dairy value.\s\s
                All three significantly outperform plain milk bucket in nutrition per item. Worth crafting once your dairy farm is established.
                """);

        this.page("proteins", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Proteins: Cured Meats");

        this.pageText("""
                Herbs & Harvest adds cured and seasoned meats that count as Proteins:\s\s
                - **Cured Ham**: aged pork; high protein density.
                - **Smoked Salmon**: a distinct fish family separate from plain cooked salmon.
                - **Herb-Seasoned Beef**: cooked beef with herbs; improved nutrition over plain.\s\s
                These give Proteins players more rotation variety and higher per-item nutrition.
                """);

        this.page("herbs_and_seasoning", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Herbs & Seasoning");

        this.pageText("""
                The herb system in Herbs & Harvest can apply to many foods across groups. Seasoned variants of plain foods often have:\s\s
                - Higher nutrition per item
                - Slower diminishing returns (as a distinct family)
                - Additional minor effects (saturation, speed)\s\s
                Investing in herb farming pays off in higher food quality across multiple groups simultaneously.
                """);

        this.page("getting_started", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Getting Started");

        this.pageText("""
                The earliest Herbs & Harvest addition to pursue for Nourished is the cheese process: milk a cow, follow the cheese aging recipe chain, and unlock the first multi-family dairy rotation.\s\s
                Parallel to that, planting an herb garden gives seasoning options that improve almost every food group. A small herb plot is one of the best early investments when this mod is installed.
                """);
    }

    @Override
    protected String entryName() {
        return "Herbs & Harvest";
    }

    @Override
    protected String entryDescription() {
        return "Artisanal cheeses, cured meats, and herb seasoning.";
    }

    @Override
    protected Pair<Integer, Integer> entryBackground() {
        return EntryBackground.DEFAULT;
    }

    @Override
    protected BookIconModel entryIcon() {
        return BookIconModel.create(Items.MILK_BUCKET);
    }

    @Override
    protected String entryId() {
        return ID;
    }
}
