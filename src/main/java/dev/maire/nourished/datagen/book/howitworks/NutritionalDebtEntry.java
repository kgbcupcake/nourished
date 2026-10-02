package dev.maire.nourished.datagen.book.howitworks;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class NutritionalDebtEntry extends EntryProvider {

    public static final String ID = "nutritional_debt";

    // Accent colors
    private static final String DEBT = "[#](E85D75)";
    private static final String NEGLECTED = "[#](6FB8E8)";

    // Reset
    private static final String RESET = "[#]()";

    public NutritionalDebtEntry(CategoryProvider parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {

        this.page("nutritional_debt", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Nutritional Debt");

        this.pageText("""
                %sNutritional Debt%s isn't about letting a bar sit empty — it's the cost of eating too narrowly. Lean hard on one food group over and over, past a configurable threshold, and the system starts looking for payback.
                """.formatted(
                DEBT, RESET
        ));

        this.page("who_pays_for_it", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Who Pays for It");

        this.pageText("""
                Once that threshold is crossed, Nourished finds whichever group you've been %sneglecting%s the most — your lowest bar, excluding the one you're currently overeating — and drains it a little.

                In other words: bingeing on Proteins doesn't just fail to help your other bars, it can actively cost you whichever one you've been ignoring.
                """.formatted(
                NEGLECTED, RESET
        ));

        this.page("avoiding_it", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Avoiding It");

        this.pageText("""
                The fix is the same as everything else in this chapter: rotate. Spreading your meals across groups instead of running one group at a time keeps your eat-count on any single group below the threshold, so debt never triggers in the first place.

                A high Balance Score and staying out of Nutritional Debt go hand in hand — both reward the same even, varied diet.
                """);
    }

    @Override
    protected String entryName() {
        return "Nutritional Debt";
    }

    @Override
    protected String entryDescription() {
        return "The cost of leaning on one food group for too long.";
    }

    @Override
    protected Pair<Integer, Integer> entryBackground() {
        return EntryBackground.DEFAULT;
    }

    @Override
    protected BookIconModel entryIcon() {
        return BookIconModel.create(Items.ROTTEN_FLESH);
    }

    @Override
    protected String entryId() {
        return ID;
    }
}
