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

    // Food group colors
    private static final String FRUITS = "[#](F5A623)";
    private static final String VEGETABLES = "[#](7BC96F)";
    private static final String PROTEINS = "[#](E85D75)";
    private static final String GRAINS = "[#](E6C65C)";
    private static final String DAIRY = "[#](6FB8E8)";

    // Guide concept colors
    private static final String SLEEP_BONUS = "[#](B88CFF)";
    private static final String DAILY_ROUTINE = "[#](F5A623)";
    private static final String EFFICIENT_FARMING = "[#](7BC96F)";
    private static final String EMERGENCY_RECOVERY = "[#](E85D75)";
    private static final String READING_TOOLTIPS = "[#](6FB8E8)";

    // Reset
    private static final String RESET = "[#]()";

    public FirstDayEntry(CategoryProvider parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {

        // ---------------------------------------------------------------------
        // Your First Day
        // ---------------------------------------------------------------------

        this.page("your_first_day", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Your First Day");

        this.pageText("""
                When you first spawn, all the nutrition bars start at a moderate level. You won't feel any effects immediately, but they will begin to decay.

                Your priority on day one is to gather a variety of foods before the bars run dry.

                """);

        // ---------------------------------------------------------------------
        // Early Food Sources
        // ---------------------------------------------------------------------

        this.page("early_food_sources", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Early Food Sources");

        this.pageText("""
                %s**Fruits**%s: Berry bushes, apple-bearing trees, and melon patches.

                %s**Vegetables**%s: Carrots, potatoes, and beetroot from villages or the ground.

                %s**Proteins**%s: Cooked chicken, beef, or fish from nearby animals and rivers.

                """.formatted(
                FRUITS, RESET,
                VEGETABLES, RESET,
                PROTEINS, RESET
        ));

        // ---------------------------------------------------------------------
        // More Early Sources
        // ---------------------------------------------------------------------

        this.page("more_early_sources", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("More Early Sources");

        this.pageText("""
                %s**Grains**%s: Bread from village chests, or seeds to plant wheat immediately.

                %s**Dairy**%s: A bucket of milk from any cow covers the dairy group in a pinch.

                """.formatted(
                GRAINS, RESET,
                DAIRY, RESET
        ));

        // ---------------------------------------------------------------------
        // Setting Up a Farm
        // ---------------------------------------------------------------------

        this.page("setting_up_a_farm", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Setting Up a Farm");

        this.pageText("""
                A small mixed farm is the most reliable long-term strategy. Plant %s**wheat**%s, %s**carrots**%s, %s**potatoes**%s, and %s**beetroot**%s early.

                Keep a cow pen nearby for milk and cooked beef. Even a tiny farm can cover most of your core food groups with minimal effort.

                """.formatted(
                GRAINS, RESET,
                VEGETABLES, RESET,
                VEGETABLES, RESET,
                VEGETABLES, RESET
        ));

        // ---------------------------------------------------------------------
        // Cooking Matters
        // ---------------------------------------------------------------------

        this.page("cooking_matters", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Cooking Matters");

        this.pageText("""
                Raw meats count as %s**Proteins**%s, but their nutrition values are lower than their cooked equivalents. Always cook your meat when possible.

                Some foods only register as their group when prepared. Check tooltips if a food isn't filling the bar you expect.

                """.formatted(
                PROTEINS, RESET
        ));

        // ---------------------------------------------------------------------
        // Diminishing Returns
        // ---------------------------------------------------------------------

        this.page("diminishing_returns", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Diminishing Returns");

        this.pageText("""
                Eating the same food repeatedly gives less nutrition each time due to diminishing returns. Rotate through different foods within each group to maximize efficiency.

                See the How It Works chapter for the full details.

                """);

        // ---------------------------------------------------------------------
        // End of Day One
        // ---------------------------------------------------------------------

        this.page("end_of_day_one", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("End of Day One");

        this.pageText("""
                Sleep when all five bars are above their threshold and wake up with the %s**Sleep Bonus**%s. This is the ideal daily rhythm Nourished is designed around.

                With a bit of preparation, staying balanced becomes second nature.

                """.formatted(
                SLEEP_BONUS, RESET
        ));

        // ---------------------------------------------------------------------
        // Next Steps
        // ---------------------------------------------------------------------

        this.page("next_steps", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Next Steps");

        this.pageText("""
                Once you are past day one, check out the Tips & Tricks chapter for deeper strategies:

                %s**Daily Routine**%s: the ideal loop for keeping all bars healthy.

                %s**Efficient Farming**%s: the minimum farm that covers all five groups.

                %s**Emergency Recovery**%s: what to do when multiple bars crash.

                %s**Reading Tooltips**%s: food tooltips and JEI tag search.

                """.formatted(
                DAILY_ROUTINE, RESET,
                EFFICIENT_FARMING, RESET,
                EMERGENCY_RECOVERY, RESET,
                READING_TOOLTIPS, RESET
        ));
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