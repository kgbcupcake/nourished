package dev.maire.nourished.datagen.book.tipsandtricks;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class MultiplayerAdventureEntry extends EntryProvider {

    public static final String ID = "multiplayer_adventure";

    // Food group colors
    private static final String PROTEINS = "[#](E85D75)";
    private static final String DAIRY = "[#](6FB8E8)";

    // Reset
    private static final String RESET = "[#]()";

    public MultiplayerAdventureEntry(CategoryProvider parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {

        this.page("traveling_far_from_base", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Traveling Far from Base");

        this.pageText("""
                When leaving base for extended exploration, pack food from all six groups — not just your usual hunger food. A nutrition emergency two thousand blocks from home is much worse than one in your base.

                A good travel kit: cooked beef, carrot, apple, bread, honey bottle, and a milk bucket. That covers all six groups in six inventory slots.
                """);

        this.page("shared_farms", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Shared Farms");

        this.pageText("""
                In multiplayer, a shared farm benefits everyone. Consider assigning different players to maintain different groups:

                **Farmer** — wheat, carrots, potatoes, beetroot.
                **Rancher** — cows, pigs, chickens, bees.
                **Forager** — collects wild fruits, mushrooms, fishing.

                A well-organized community farm makes Nourished invisible for the whole server.
                """);

        this.page("long_dungeon_runs", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Long Dungeon Runs");

        this.pageText("""
                For long dungeon or nether runs where you cannot eat freely between fights, prioritize **pre-loading** your bars before entering:

                — All six groups above 60%%
                — %sProteins%s and %sDairy%s at or above bonus threshold
                — Milk bucket in hotbar for emergency debuff clear

                Bars decay slowly enough that a two-hour session rarely causes a problem if you start fully topped up.
                """.formatted(
                PROTEINS, RESET,
                DAIRY, RESET
        ));

        this.page("trading_for_nutrition", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Trading for Nutrition");

        this.pageText("""
                Village trading is an underrated nutrition tool. Farmer villagers often sell:

                **Bread** — cheap Grains in bulk.
                **Apple** — easy early Fruits.
                **Cooked Chicken** — decent Proteins trade.
                **Pumpkin Pie** — excellent value Grains.

                Early-game trading can cover multiple nutrition groups before your own farm is established.
                """);
    }

    @Override
    protected String entryName() {
        return "Multiplayer & Adventure";
    }

    @Override
    protected String entryDescription() {
        return "Staying nourished on the road and with a crew.";
    }

    @Override
    protected Pair<Integer, Integer> entryBackground() {
        return EntryBackground.DEFAULT;
    }

    @Override
    protected BookIconModel entryIcon() {
        return BookIconModel.create(Items.COMPASS);
    }

    @Override
    protected String entryId() {
        return ID;
    }
}
