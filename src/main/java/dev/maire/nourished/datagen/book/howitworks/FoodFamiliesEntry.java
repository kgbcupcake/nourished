package dev.maire.nourished.datagen.book.howitworks;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class FoodFamiliesEntry extends EntryProvider {

    public static final String ID = "food_families";

    public FoodFamiliesEntry(CategoryProvider parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {

        this.page("food_families", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Food Families");

        this.pageText("""
                Within each food group, foods are organized into **families**. All apple-type foods, for example, share the Apple family.

                Family is the middle of the three levels Diminishing Returns tracks — between the specific item and the whole food group. Lean on one family for a while and it starts dragging down everything in that family, not just the one item.
                """);

        this.page("why_it_matters", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Why It Matters");

        this.pageText("""
                You can check a food's family in its tooltip. Ideally you want to rotate across **multiple families** within each group, not just multiple foods.

                Eating an apple, then a golden apple, then an enchanted golden apple is still all Apple family — real variety means switching to melons or berries instead.
                """);
    }

    @Override
    protected String entryName() {
        return "Food Families";
    }

    @Override
    protected String entryDescription() {
        return "The middle layer between a single food and its whole group.";
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
