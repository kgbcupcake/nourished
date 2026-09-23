package dev.maire.nourished.datagen.book.howitworks;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.SingleBookSubProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookCategoryModel;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.book.BookCategoryBackgroundParallaxLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

public class HowItWorksCategory extends CategoryProvider {

    public static final String ID = "how_it_works";

    public HowItWorksCategory(SingleBookSubProvider parent) {
        super(parent);
    }

    @Override
    protected String[] generateEntryMap() {
        /*
         * Diminishing Returns and its two closest relatives (Streak, Food
         * Families) branch off the top before the chain rejoins for Balance
         * Score, Nutritional Debt, and the closing Multiplier System summary.
         */
        return new String[]{
                "d________",
                "_________",
                "__s______",
                "_________",
                "____f____",
                "_________",
                "______b__",
                "_________",
                "_______n_",
                "_________",
                "________m"
        };
    }

    @Override
    protected void generateEntries() {

        // Diminishing Returns
        var diminishingReturns = this.add(
                new DiminishingReturnsEntry(this).generate('d')
        );

        // Streak Penalty
        var streakPenalty = this.add(
                new StreakPenaltyEntry(this).generate('s')
        )
                .withParent(diminishingReturns)
                .withCondition(this.condition().entryRead(diminishingReturns));

        // Food Families
        var foodFamilies = this.add(
                new FoodFamiliesEntry(this).generate('f')
        )
                .withParent(streakPenalty)
                .withCondition(this.condition().entryRead(streakPenalty));

        // Balance Score
        var balanceScore = this.add(
                new BalanceScoreEntry(this).generate('b')
        )
                .withParent(foodFamilies)
                .withCondition(this.condition().entryRead(foodFamilies));

        // Nutritional Debt
        var nutritionalDebt = this.add(
                new NutritionalDebtEntry(this).generate('n')
        )
                .withParent(balanceScore)
                .withCondition(this.condition().entryRead(balanceScore));

        // Multiplier System
        this.add(
                new MultiplierSystemEntry(this).generate('m')
        )
                .withParent(nutritionalDebt)
                .withCondition(this.condition().entryRead(nutritionalDebt));
    }

    @Override
    protected BookCategoryModel additionalSetup(BookCategoryModel category) {
        return category.withBackgroundParallaxLayers(
                new BookCategoryBackgroundParallaxLayer(
                        ResourceLocation.parse(
                                "modonomicon:textures/gui/parallax/flow/base.png"
                        ),
                        0.7f,
                        -1
                ),
                new BookCategoryBackgroundParallaxLayer(
                        ResourceLocation.parse(
                                "modonomicon:textures/gui/parallax/flow/1.png"
                        ),
                        1.0f,
                        -1
                ),
                new BookCategoryBackgroundParallaxLayer(
                        ResourceLocation.parse(
                                "modonomicon:textures/gui/parallax/flow/2.png"
                        ),
                        1.4f,
                        -1
                )
        );
    }

    @Override
    protected String categoryName() {
        return "How It Works";
    }

    @Override
    protected String categoryDescription() {
        return "A deeper look at the systems behind Nourished.";
    }

    @Override
    protected BookIconModel categoryIcon() {
        return BookIconModel.create(Items.BOOK);
    }

    @Override
    public String categoryId() {
        return ID;
    }
}
