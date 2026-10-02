package dev.maire.nourished.datagen.book.howitworks;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class BalanceScoreEntry extends EntryProvider {

    public static final String ID = "balance_score";

    // Accent colors
    private static final String BALANCE_SCORE = "[#](B88CFF)";
    private static final String EVEN = "[#](7BC96F)";
    private static final String UNEVEN = "[#](E85D75)";

    // Reset
    private static final String RESET = "[#]()";

    public BalanceScoreEntry(CategoryProvider parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {

        this.page("balance_score", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Balance Score");

        this.pageText("""
                The %sBalance Score%s measures how evenly spread your nutrition is across all six groups — not how full any single bar is.

                It compares each bar to the average of all of them. Five groups full and one empty scores worse than all six sitting at a moderate, even level. It's shown on the left panel of the diet screen.
                """.formatted(
                BALANCE_SCORE, RESET
        ));

        this.page("even_vs_uneven", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Even vs. Uneven");

        this.pageText("""
                The score isn't a fixed pass/fail band — it's continuous. The further any bar drifts from the average of the rest, the more the score drops, whether that bar is unusually low or unusually high.

                %sEven%s coverage across all six groups keeps the score high. %sLopsided%s eating — maxing one or two bars while others sit empty — drags it down even if your total nutrition looks fine on paper.
                """.formatted(
                EVEN, RESET,
                UNEVEN, RESET
        ));
    }

    @Override
    protected String entryName() {
        return "Balance Score";
    }

    @Override
    protected String entryDescription() {
        return "How evenly spread your nutrition is, not how full any one bar is.";
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
