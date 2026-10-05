package dev.maire.nourished.datagen.book.compatmods;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class SpiceOfLifeEntry extends EntryProvider {

    public static final String ID = "spice_of_life";

    private static final String SATURATED = "[#](e05252)";
    private static final String NEW_ENTRY = "[#](52c252)";
    private static final String RESET = "[#]()";

    public SpiceOfLifeEntry(CategoryProvider parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {

        this.page("spice_of_life", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Spice of Life: Onion");

        this.pageText("""
                Spice of Life: Onion is a food variety mod that independently tracks food history and rewards eating a diverse diet. It works alongside Nourished rather than replacing it; both systems run simultaneously and reinforce the same core goal: **eat variety, not repetition**.
                """);

        this.page("how_they_interact", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("How They Interact");

        this.pageText("""
                Both mods track food history, but they do so independently:\s\s
                **Nourished** tracks variety at the item, family, and group level and applies it as a nutrition multiplier.
                **Spice of Life** tracks a rolling history of unique foods eaten and grants rewards for diversity.\s\s
                Eating a varied diet satisfies both systems at once: the same rotation that maximizes Nourished efficiency also earns SoL rewards.
                """);

        this.page("diminishing_returns_and_sol", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Diminishing Returns & SoL");

        this.pageText("""
                Nourished's diminishing returns and SoL's food history are separate counters. A food that is %ssaturated%s in Nourished (low nutrition multiplier) may still count as a %snew entry%s in SoL's history if you have not eaten it recently.\s\s
                This means you can strategically eat a wider range of foods to top up SoL's diversity counter while also rotating Nourished families; they reward the same behaviour.
                """.formatted(
                SATURATED, RESET,
                NEW_ENTRY, RESET
        ));

        this.page("practical_advice", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Practical Advice");

        this.pageText("""
                With both mods installed, the optimal strategy is simply to eat as many **different foods** as possible each day rather than defaulting to a small rotation.\s\s
                Instead of eating bread + apple + steak every day, try rotating through 10–15 different foods across the week. Both mods reward this heavily: Nourished via freshness bonuses and SoL via its diversity rewards.
                """);
    }

    @Override
    protected String entryName() {
        return "Spice of Life: Onion";
    }

    @Override
    protected String entryDescription() {
        return "How two independent variety systems reinforce each other.";
    }

    @Override
    protected Pair<Integer, Integer> entryBackground() {
        return EntryBackground.DEFAULT;
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
