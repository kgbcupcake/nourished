package dev.maire.nourished.datagen.book.howitworks;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class MultiplierSystemEntry extends EntryProvider {

    public static final String ID = "multiplier_system";

    // Accent colors
    private static final String BONUS = "[#](7BC96F)";
    private static final String PENALTY = "[#](E85D75)";

    // Reset
    private static final String RESET = "[#]()";

    public MultiplierSystemEntry(CategoryProvider parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {

        this.page("the_bonuses", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("The Bonuses");

        this.pageText("""
                Everything in this chapter feeds into one question: how much nutrition does this specific meal actually give you? Two things can push that number up:

                %sCooking:%s eating something cooked avoids the raw food penalty entirely — see the Food Safety chapter.
                %sNovelty:%s eating a food you haven't touched in a while gives a one-time boost on top of its normal value.
                """.formatted(
                BONUS, RESET,
                BONUS, RESET
        ));

        this.page("the_penalties", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("The Penalties");

        this.pageText("""
                And several things can push it down:

                %sDiminishing Returns:%s repeating the same item, family, or group recently reduces its multiplier.
                %sStreak:%s eating the exact same item again soon makes that drop steeper.
                %sNutritional Debt:%s overeating one group past a threshold drains whichever group you've neglected most.
                """.formatted(
                PENALTY, RESET,
                PENALTY, RESET,
                PENALTY, RESET
        ));

        this.page("balance_and_gut", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Balance and Gut Health");

        this.pageText("""
                Your **Balance Score** doesn't multiply nutrition directly — instead, a well-balanced diet speeds up passive Gut Health recovery, and Gut Health itself amplifies every raw-food penalty you take when it's low. See the previous entry and the Food Safety chapter for both.
                """);

        this.page("putting_it_together", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Putting It Together");

        this.pageText("""
                None of these systems need to be memorized in detail. The practical takeaway is consistent across all of them: cook your food, rotate what you eat across items and families, and spread your meals across groups instead of running one at a time.

                Do that, and most of this chapter simply never comes up.
                """);
    }

    @Override
    protected String entryName() {
        return "Multiplier System";
    }

    @Override
    protected String entryDescription() {
        return "How every bonus and penalty in this chapter fits together.";
    }

    @Override
    protected Pair<Integer, Integer> entryBackground() {
        return EntryBackground.DEFAULT;
    }

    @Override
    protected BookIconModel entryIcon() {
        return BookIconModel.create(Items.NETHER_STAR);
    }

    @Override
    protected String entryId() {
        return ID;
    }
}
