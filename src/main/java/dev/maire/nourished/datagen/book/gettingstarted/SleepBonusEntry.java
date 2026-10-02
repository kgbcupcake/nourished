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

    // Page title colors
    private static final String SLEEP_BONUS = "[#](B88CFF)";
    private static final String HOW_IT_SCALES = "[#](F5A623)";

    // Sleep Bonus concept colors
    private static final String ALL_FIVE_GROUPS = "[#](7BC96F)";
    private static final String HEALTHY_LEVEL = "[#](7BC96F)";
    private static final String MAXIMUM_BONUS = "[#](F2D77C)";
    private static final String REGENERATION = "[#](E85D75)";
    private static final String NUTRIENT_LEVELS = "[#](6FB8E8)";

    // Reset
    private static final String RESET = "[#]()";

    public SleepBonusEntry(CategoryProvider parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {
        this.page("spotlight", () -> BookSpotlightPageModel.create()
                .withItem(Ingredient.of(Items.GOLDEN_APPLE))
                .withText(this.context().pageText()));

        this.pageText("""
                A well-balanced diet makes for better rest. When you sleep, your nutrition determines the strength of your %s**Sleep Bonus**%s when you wake.

                """.formatted(
                SLEEP_BONUS,
                RESET
        ));

        this.page("how_it_scales", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("How It Scales");

        this.pageText("""
                Filling %s**all Five groups**%s to a %s**healthy level**%s before bed gives the %s**maximum bonus**%s — a burst of %s**regeneration**%s and a small boost to your %s**nutrient levels**%s on waking.

                Neglecting your diet means waking up feeling no better than when you went to sleep.

                """.formatted(
                ALL_FIVE_GROUPS, RESET,
                HEALTHY_LEVEL, RESET,
                MAXIMUM_BONUS, RESET,
                REGENERATION, RESET,
                NUTRIENT_LEVELS, RESET
        ));
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