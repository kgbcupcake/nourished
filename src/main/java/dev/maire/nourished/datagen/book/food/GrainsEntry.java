package dev.maire.nourished.datagen.book.foodgroups;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookSpotlightPageModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

public class GrainsEntry extends EntryProvider {

    public static final String ID = "grains";

    private static final String PENALTY = "[#](e05252)";
    private static final String BONUS = "[#](52c252)";
    private static final String RESET = "[#]()";

    public GrainsEntry(CategoryProvider parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {

        this.page("spotlight", () -> BookSpotlightPageModel.create()
                .withItem(Ingredient.of(Items.BREAD))
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Grains");

        this.pageText("""
                Grains are staple foods that form the backbone of any diet. %sPenalty:%s reduced hunger restoration when depleted. %sBonus:%s faster experience gain when maintained. Bread is your most reliable source.
                """.formatted(
                PENALTY, RESET,
                BONUS, RESET
        ));

        this.page("vanilla_sources", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Vanilla Grain Sources");

        this.pageText("""
                - **Bread**: crafted from 3 wheat; the most cost-efficient grain per crop.
                - **Cookie**: 8 cookies per craft; high volume, lower nutrition per piece.
                - **Cake**: placed block; each slice gives grain nutrition. Good for group eating.
                - **Pumpkin Pie**: excellent nutrition value; worth the pumpkin investment.
                """);

        this.page("baked_goods", () -> BookSpotlightPageModel.create()
                .withItem(Ingredient.of(Items.PUMPKIN_PIE))
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Grains: Baked Goods");

        this.pageText("""
                Pumpkin pie and other baked goods also count toward Grains. They provide higher nutrition values than plain bread, making them worth crafting once you can supply the ingredients reliably.\s\s
                Keep a pumpkin farm and you always have access to pie as a high-value grain top-up.
                """);

        this.page("farming_tips", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Farming Tips");

        this.pageText("""
                Wheat is the most fundamental farm crop and the backbone of the Grains group. A moderate wheat farm covers this group indefinitely with bread alone.\s\s
                Automating a bread farm (harvest wheat, craft bread, store) is one of the easiest and most impactful nutrition automations you can build early in a world.
                """);

        this.page("rotation_strategy", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Rotation Strategy");

        this.pageText("""
                Bread, cookie, and pumpkin pie are all distinct grain families. Rotating between them gives better nutrition than eating only bread each day.\s\s
                Cake is a special case: each slice of the same cake is the same item, so eating an entire cake counts as eating the same food repeatedly. Pair cake slices with other grain foods on the same day for best efficiency.
                """);

        this.page("experience_bonus", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Experience Bonus");

        this.pageText("""
                The Grains bonus (faster experience gain) is one of the most impactful in the game for combat and enchanting progression.\s\s
                Prioritizing Grains early is especially worthwhile if you plan to enchant gear or grind XP. The bonus stacks with other XP sources and applies passively while it is active.
                """);
    }

    @Override
    protected String entryName() {
        return "Grains";
    }

    @Override
    protected String entryDescription() {
        return "Staple foods that speed up experience gain.";
    }

    @Override
    protected Pair<Integer, Integer> entryBackground() {
        return EntryBackground.DEFAULT;
    }

    @Override
    protected BookIconModel entryIcon() {
        return BookIconModel.create(Items.BREAD);
    }

    @Override
    protected String entryId() {
        return ID;
    }
}
