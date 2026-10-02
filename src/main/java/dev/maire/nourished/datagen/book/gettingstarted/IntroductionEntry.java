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

    // Page title colors
    private static final String WELCOME = "[#](B88CFF)";
    private static final String INTRODUCTION = "[#](F5A623)";

    // Food group colors
    private static final String FRUITS = "[#](F5A623)";
    private static final String VEGETABLES = "[#](7BC96F)";
    private static final String PROTEINS = "[#](E85D75)";
    private static final String GRAINS = "[#](E6C65C)";
    private static final String DAIRY = "[#](6FB8E8)";

    // Core concept colors
    private static final String VARIED_DIET = "[#](B88CFF)";
    private static final String HEALTH = "[#](7BC96F)";
    private static final String PERFORMANCE = "[#](F5A623)";
    private static final String BONUSES = "[#](6FB8E8)";
    private static final String PENALTIES = "[#](E85D75)";
    private static final String BALANCE = "[#](F2D77C)";

    // Reset
    private static final String RESET = "[#]()";

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
                %s**Welcome to Nourished**%s

                Nourished adds a nutrition system that encourages eating a %s**varied diet**%s. Each food group you consume contributes to your overall %s**health**%s and %s**performance**%s.

                Eat well, sleep soundly, and your body will reward you.

                """.formatted(
                WELCOME, RESET,
                VARIED_DIET, RESET,
                HEALTH, RESET,
                PERFORMANCE, RESET
        ));

        this.page("core_idea", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("The Core Idea");

        this.pageText("""
                %s**The Core Idea**%s

                There are five food groups:

                %s**Fruits**%s
                %s**Vegetables**%s
                %s**Proteins**%s
                %s**Grains**%s
                %s**Dairy**%s

                Maintaining each group provides %s**bonuses**%s, while neglecting them causes %s**penalties**%s. %s**Balance**%s is everything.

                """.formatted(
                INTRODUCTION, RESET,
                FRUITS, RESET,
                VEGETABLES, RESET,
                PROTEINS, RESET,
                GRAINS, RESET,
                DAIRY, RESET,
                BONUSES, RESET,
                PENALTIES, RESET,
                BALANCE, RESET
        ));
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