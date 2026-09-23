package dev.maire.nourished.datagen.book.tipsandtricks;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class ReadingTooltipsEntry extends EntryProvider {

    public static final String ID = "reading_tooltips";

    // Status colors
    private static final String FRESH = "[#](7BC96F)";
    private static final String REDUCED = "[#](E6C65C)";
    private static final String SATURATED = "[#](E85D75)";
    private static final String STREAK_WARNING = "[#](E85D75)";

    // Reset
    private static final String RESET = "[#]()";

    public ReadingTooltipsEntry(CategoryProvider parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {

        this.page("food_tooltips", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Food Tooltips");

        this.pageText("""
                Hover over any food item to see Nourished tooltip information below the item name:

                **Food Group** — which bar this food fills.
                **Nutrition Value** — how much it fills the bar under ideal conditions.
                **Food Family** — which family it belongs to for diminishing returns.
                **Freshness** — whether this food currently gives full, reduced, or bonus nutrition.
                """);

        this.page("freshness_indicator", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Freshness Indicator");

        this.pageText("""
                The %sfreshness indicator%s tells you how much nutrition you will actually receive:

                %sFresh%s — first time eating this food today; full or bonus nutrition.
                %sReduced%s — diminishing returns active; lower nutrition than base.
                %sSaturated%s — heavily repeated; minimal nutrition gain.

                If a food shows Saturated, switch to a different family within that group for better returns.
                """.formatted(
                FRESH, RESET,
                FRESH, RESET,
                REDUCED, RESET,
                SATURATED, RESET
        ));

        this.page("streak_warning", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Streak Warning");

        this.pageText("""
                When the streak penalty is active, the tooltip shows a %sstreak counter%s. This appears after eating the same food consecutively without switching.

                If you see this, eat one different food before continuing with your usual diet. Even a single item from a different food family resets the streak and restores full multipliers.
                """.formatted(
                STREAK_WARNING, RESET
        ));

        this.page("jei_tag_search", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("JEI Tag Search");

        this.pageText("""
                If you have JEI installed, you can filter the entire item list by food group. Type any of these into the JEI search bar:

                **nourished:nutrients/fruits**
                **nourished:nutrients/vegetables**
                **nourished:nutrients/proteins**
                **nourished:nutrients/grains**
                **nourished:nutrients/dairy**
                """);

        this.page("jei_tag_search_cont", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("JEI Tag Search (cont.)");

        this.pageText("""
                This is the fastest way to answer the question **"what can I eat right now?"** when a bar is low.

                JEI will show every item on the server assigned to that group — including modded foods — so you can see at a glance what you have in your inventory or nearby chests that qualifies.
                """);

        this.page("unrecognized_foods", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Unrecognized Foods");

        this.pageText("""
                If a food item shows **Unclassified** or no Nourished tooltip at all, it has not been assigned to a food group on this server.

                For modded foods this is usually resolved by the server's datapack configuration. If you believe a food should belong to a specific group, contact your server admin or check the Nourished datapack documentation in the **Server Owners** chapter.
                """);
    }

    @Override
    protected String entryName() {
        return "Reading Tooltips";
    }

    @Override
    protected String entryDescription() {
        return "Reading a food's group, nutrition, and freshness at a glance.";
    }

    @Override
    protected Pair<Integer, Integer> entryBackground() {
        return EntryBackground.DEFAULT;
    }

    @Override
    protected BookIconModel entryIcon() {
        return BookIconModel.create(Items.PAPER);
    }

    @Override
    protected String entryId() {
        return ID;
    }
}
