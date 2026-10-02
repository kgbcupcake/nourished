package dev.maire.nourished.datagen.book.tipsandtricks;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class EfficientFarmingEntry extends EntryProvider {

    public static final String ID = "efficient_farming";

    // Food group colors
    private static final String GRAINS = "[#](E6C65C)";
    private static final String VEGETABLES = "[#](7BC96F)";
    private static final String FRUITS = "[#](F5A623)";
    private static final String DAIRY = "[#](6FB8E8)";
    private static final String PROTEINS = "[#](E85D75)";

    // Reset
    private static final String RESET = "[#]()";

    public EfficientFarmingEntry(CategoryProvider parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {

        this.page("minimal_farm", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("The Minimal Farm");

        this.pageText("""
                You do not need a mega-farm to stay healthy. A surprisingly small setup covers all five groups:

                **Wheat patch** — covers %sGrains%s via bread.
                **Carrot + potato rows** — covers %sVegetables%s.
                **Melon or berry patch** — covers %sFruits%s.
                **Cow pen (2+)** — covers %sDairy%s.
                **Chicken or pig pen** — covers %sProteins%s.
                """.formatted(
                GRAINS, RESET,
                VEGETABLES, RESET,
                FRUITS, RESET,
                DAIRY, RESET,
                PROTEINS, RESET
        ));

        this.page("high_value_crops", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("High-Value Crops");

        this.pageText("""
                If you can only tend a few crops, prioritize these:

                **Wheat** — most versatile; makes bread, cake, and cookies.
                **Pumpkin** — makes pumpkin pie (high-value Grains).
                **Melon** — the easiest renewable Fruit at scale.
                **Carrots** — fast-growing, no replanting needed, renewable golden carrots.
                """);

        this.page("animal_priority", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Animal Priority");

        this.pageText("""
                If you can only keep one or two animal pens early on, choose:

                **Cows** — cover both Dairy (milk) and Proteins (cooked beef). The single most efficient animal for Nourished.
                **Bees** — passive honey supply with zero daily effort once hives are set up. Useful for recipes even without a separate sweets group.

                Chickens are lower priority since their protein value is lower, but they breed quickly and are easy to maintain.
                """);

        this.page("automation_ideas", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Automation Ideas");

        this.pageText("""
                A few simple automations make Nourished almost invisible:

                **Auto-wheat harvester** — keeps bread in constant supply.
                **Dispenser + water harvester** — hands-off carrot and potato farming.
                **Auto honey collector** — dispensers with bottles harvest hives automatically when full.
                **Auto fish farm** — provides passive protein with no mobs required.
                """);

        this.page("storing_supplies", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Storing Supplies");

        this.pageText("""
                Keep a dedicated **nutrition chest** near your base — a small chest stocked with one stack each of your six key foods.

                Refill it during farm harvests and draw from it when a bar is low. This prevents emergency scrambles and means you always have something from each group on hand when you need it.
                """);
    }

    @Override
    protected String entryName() {
        return "Efficient Farming";
    }

    @Override
    protected String entryDescription() {
        return "A small, well-planned farm covers every food group.";
    }

    @Override
    protected Pair<Integer, Integer> entryBackground() {
        return EntryBackground.DEFAULT;
    }

    @Override
    protected BookIconModel entryIcon() {
        return BookIconModel.create(Items.WHEAT);
    }

    @Override
    protected String entryId() {
        return ID;
    }
}
