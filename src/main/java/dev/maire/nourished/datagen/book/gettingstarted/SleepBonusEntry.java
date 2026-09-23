package dev.maire.nourished.datagen.book.gettingstarted;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookSpotlightPageModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

public class SleepBonusEntry extends EntryProvider {

    public static final String ID = "sleep_bonus";

    public SleepBonusEntry(CategoryProvider parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {
        this.page("spotlight", () -> BookSpotlightPageModel.create()
                .withItem(Ingredient.of(Items.GOLDEN_APPLE))
                .withText(this.context().pageText()));
        this.pageText("A well-balanced diet makes for better rest. When you sleep, your nutrition determines the strength of your Sleep Bonus when you wake.");

        this.page("how_it_scales", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("How It Scales");
        this.pageText("""
                Filling **all Five groups** to a healthy level before bed gives the maximum bonus — a burst of regeneration and a small boost to your nutrient levels on waking.

                Neglecting your diet means waking up feeling no better than when you went to sleep.
                """);
    }

    @Override
    protected String entryName() {
        return "Sleep Bonus";
    }

    @Override
    protected String entryDescription() {
        return "The reward for going to bed well-nourished.";
    }

    @Override
    protected Pair<Integer, Integer> entryBackground() {
        return EntryBackground.DEFAULT;
    }

    @Override
    protected BookIconModel entryIcon() {
        return BookIconModel.create(Items.GOLDEN_APPLE);
    }

    @Override
    protected String entryId() {
        return ID;
    }
}
