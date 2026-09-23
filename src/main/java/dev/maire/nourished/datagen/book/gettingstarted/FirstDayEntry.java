package dev.maire.nourished.datagen.book.gettingstarted;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class FirstDayEntry extends EntryProvider {

    public static final String ID = "first_day";

    public FirstDayEntry(CategoryProvider parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {
        this.page("your_first_day", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Your First Day");
        this.pageText("""
                When you first spawn, all six nutrition bars start at a moderate level. You won't feel any effects immediately — but they will begin to decay.

                Your priority on day one is to gather a variety of foods before the bars run dry.
                """);

        this.page("early_food_sources", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Early Food Sources");
        this.pageText("""
                **Fruits:** Berry bushes, apple-bearing trees, and melon patches.
                **Vegetables:** Carrots, potatoes, and beetroot from villages or the ground.
                **Proteins:** Cooked chicken, beef, or fish from nearby animals and rivers.
                """);

        this.page("more_early_sources", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("More Early Sources");
        this.pageText("""
                **Grains:** Bread from village chests, or seeds to plant wheat immediately.
                **Dairy:** A bucket of milk from any cow covers the dairy group in a pinch.
                """);

        this.page("setting_up_a_farm", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Setting Up a Farm");
        this.pageText("""
                A small mixed farm is the most reliable long-term strategy. Plant wheat, carrots, potatoes, and beetroot early.

                Keep a cow pen nearby for milk and cooked beef. Even a tiny farm covers four of the six groups with minimal effort.
                """);

        this.page("cooking_matters", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Cooking Matters");
        this.pageText("""
                Raw meats count as Proteins, but their nutrition values are lower than their cooked equivalents. Always cook your meat when possible.

                Some foods only register as their group when prepared — check tooltips if a food isn't filling the bar you expect.
                """);

        this.page("diminishing_returns", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Diminishing Returns");
        this.pageText("""
                Eating the same food repeatedly gives less nutrition each time due to diminishing returns. Rotate through different foods within each group to maximize efficiency.

                See the How It Works chapter for the full details.
                """);

        this.page("end_of_day_one", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("End of Day One");
        this.pageText("""
                Sleep when all six bars are above their threshold and wake up with the Sleep Bonus. This is the ideal daily rhythm Nourished is designed around.

                With a bit of preparation, staying balanced becomes second nature.
                """);

        this.page("next_steps", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Next Steps");
        this.pageText("""
                Once you are past day one, check out the Tips & Tricks chapter for deeper strategies:

                **Daily Routine** — the ideal loop for keeping all bars healthy.
                **Efficient Farming** — the minimum farm that covers all six groups.
                **Emergency Recovery** — what to do when multiple bars crash.
                **Reading Tooltips** — food tooltips and JEI tag search.
                """);
    }

    @Override
    protected String entryName() {
        return "Your First Day";
    }

    @Override
    protected String entryDescription() {
        return "A survival guide for your first day with Nourished active.";
    }

    @Override
    protected Pair<Integer, Integer> entryBackground() {
        return EntryBackground.DEFAULT;
    }

    @Override
    protected BookIconModel entryIcon() {
        return BookIconModel.create(Items.COOKED_BEEF);
    }

    @Override
    protected String entryId() {
        return ID;
    }
}
