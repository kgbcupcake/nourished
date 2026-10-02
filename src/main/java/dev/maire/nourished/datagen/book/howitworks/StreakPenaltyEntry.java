package dev.maire.nourished.datagen.book.howitworks;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class StreakPenaltyEntry extends EntryProvider {

    public static final String ID = "streak_penalty";

    // Status colors
    private static final String STREAK = "[#](E85D75)";

    // Reset
    private static final String RESET = "[#]()";

    public StreakPenaltyEntry(CategoryProvider parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {

        this.page("streak_penalty", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Streak Penalty");

        this.pageText("""
                Eating the same food back-to-back applies a %sStreak Penalty%s. Three meals of plain bread in a row? The third gives noticeably less grain nutrition than the first.

                A streak isn't a separate system bolted onto Diminishing Returns — it's what makes that item's diminishing-returns count climb faster than normal. Eating it again soon after adds a bigger hit to the same counter instead of a normal-sized one.
                """.formatted(
                STREAK, RESET
        ));

        this.page("breaking_the_streak", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Breaking the Streak");

        this.pageText("""
                Eating any **different food** breaks the streak immediately. You don't need to switch groups — just switch items.

                The streak counter shows in the food tooltip when it's active, so you always know before you take the hit.
                """);
    }

    @Override
    protected String entryName() {
        return "Streak Penalty";
    }

    @Override
    protected String entryDescription() {
        return "Why the third bread in a row hits harder than the first.";
    }

    @Override
    protected Pair<Integer, Integer> entryBackground() {
        return EntryBackground.DEFAULT;
    }

    @Override
    protected BookIconModel entryIcon() {
        return BookIconModel.create(Items.BARRIER);
    }

    @Override
    protected String entryId() {
        return ID;
    }
}
