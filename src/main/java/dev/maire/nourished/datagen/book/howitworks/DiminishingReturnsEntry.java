package dev.maire.nourished.datagen.book.howitworks;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class DiminishingReturnsEntry extends EntryProvider {

    public static final String ID = "diminishing_returns";

    // Status colors
    private static final String LESS = "[#](E85D75)";
    private static final String FRESH = "[#](7BC96F)";
    private static final String REDUCED = "[#](E6C65C)";
    private static final String SATURATED = "[#](E85D75)";

    // Reset
    private static final String RESET = "[#]()";

    public DiminishingReturnsEntry(CategoryProvider parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {

        this.page("diminishing_returns", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Diminishing Returns");

        this.pageText("""
                Eating the same food repeatedly gives %sless nutrition each time%s. The first apple of the day fills your Fruits bar noticeably. The fifth apple barely moves it.

                This system exists to reward variety over repetition.
                """.formatted(
                LESS, RESET
        ));

        this.page("three_levels_at_once", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Three Levels at Once");

        this.pageText("""
                Nourished tracks how much you've recently leaned on a food at three levels simultaneously: the **specific item**, its **family**, and its **food group**.

                These aren't three separate penalties stacked on top of each other — they blend into one multiplier. Early on, the item-specific level dominates. The more you've recently eaten broadly from that family or group, the more those broader levels start pulling your multiplier down too.
                """);

        this.page("how_it_fades", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("How It Fades");

        this.pageText("""
                Diminishing returns aren't tied to sleeping or the in-game day — they fade continuously based on real time elapsed since you last ate that food.

                Give something a break for a while and its multiplier recovers on its own. There's no need to wait for morning; just eat something else in the meantime.
                """);

        this.page("the_novelty_bonus", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("The Novelty Bonus");

        this.pageText("""
                Eating something you haven't touched in a while gives a %snovelty bonus%s that pushes the multiplier above 1.0 — genuinely diverse eating gives more nutrition than the safe, familiar choice.

                The tooltip shows %sFresh%s when the novelty bonus is active on a food.
                """.formatted(
                FRESH, RESET,
                FRESH, RESET
        ));

        this.page("checking_your_status", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Checking Your Status");

        this.pageText("""
                The food tooltip shows your current freshness for any item:

                %sFresh%s — full or bonus nutrition, novelty active.
                %sReduced%s — diminishing returns pulling the multiplier down.
                %sSaturated%s — heavily repeated, near the floor.

                Check this before eating if you're trying to efficiently top up a specific bar.
                """.formatted(
                FRESH, RESET,
                REDUCED, RESET,
                SATURATED, RESET
        ));
    }

    @Override
    protected String entryName() {
        return "Diminishing Returns";
    }

    @Override
    protected String entryDescription() {
        return "Why the fifth apple of the day barely moves the bar.";
    }

    @Override
    protected Pair<Integer, Integer> entryBackground() {
        return EntryBackground.DEFAULT;
    }

    @Override
    protected BookIconModel entryIcon() {
        return BookIconModel.create(Items.COOKIE);
    }

    @Override
    protected String entryId() {
        return ID;
    }
}
