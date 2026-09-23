package dev.maire.nourished.datagen.book.gettingstarted;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class IntroductionEntry extends EntryProvider {

    public static final String ID = "introduction";

    public IntroductionEntry(CategoryProvider parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {
        this.page("welcome", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Welcome to Nourished");
        this.pageText("""
                Nourished adds a nutrition system that encourages eating a **varied diet**. Each food group you consume contributes to your overall health and performance.

                Eat well, sleep soundly, and your body will reward you.
                """);

        this.page("core_idea", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("The Core Idea");
        this.pageText("""
                There are **five food groups**: Fruits, Vegetables, Proteins, Grains, and Dairy.

                Maintaining each group provides bonuses, while neglecting them causes penalties. Balance is everything.
                """);
    }

    @Override
    protected String entryName() {
        return "Introduction";
    }

    @Override
    protected String entryDescription() {
        return "What Nourished is and how it changes the way you eat.";
    }

    @Override
    protected Pair<Integer, Integer> entryBackground() {
        return EntryBackground.CATEGORY_START;
    }

    @Override
    protected BookIconModel entryIcon() {
        return BookIconModel.create(Items.BOOK);
    }

    @Override
    protected String entryId() {
        return ID;
    }
}
