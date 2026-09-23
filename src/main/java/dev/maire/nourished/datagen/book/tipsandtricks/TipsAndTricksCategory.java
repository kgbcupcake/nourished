package dev.maire.nourished.datagen.book.tipsandtricks;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.SingleBookSubProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookCategoryModel;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.book.BookCategoryBackgroundParallaxLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

public class TipsAndTricksCategory extends CategoryProvider {

    public static final String ID = "tips_and_tricks";

    public TipsAndTricksCategory(SingleBookSubProvider parent) {
        super(parent);
    }

    @Override
    protected String[] generateEntryMap() {
        /*
         * Tips & Tricks is a short, linear staircase of practical advice —
         * daily routine, farming, recovery, multiplayer, then tooltips.
         */
        return new String[]{
                "d________",
                "_________",
                "__f______",
                "_________",
                "____r____",
                "_________",
                "______m__",
                "_________",
                "________t"
        };
    }

    @Override
    protected void generateEntries() {

        // Daily Routine
        var dailyRoutine = this.add(
                new DailyRoutineEntry(this).generate('d')
        );

        // Efficient Farming
        var efficientFarming = this.add(
                new EfficientFarmingEntry(this).generate('f')
        )
                .withParent(dailyRoutine)
                .withCondition(this.condition().entryRead(dailyRoutine));

        // Emergency Recovery
        var emergencyRecovery = this.add(
                new EmergencyRecoveryEntry(this).generate('r')
        )
                .withParent(efficientFarming)
                .withCondition(this.condition().entryRead(efficientFarming));

        // Multiplayer & Adventure
        var multiplayerAdventure = this.add(
                new MultiplayerAdventureEntry(this).generate('m')
        )
                .withParent(emergencyRecovery)
                .withCondition(this.condition().entryRead(emergencyRecovery));

        // Reading Tooltips
        this.add(
                new ReadingTooltipsEntry(this).generate('t')
        )
                .withParent(multiplayerAdventure)
                .withCondition(this.condition().entryRead(multiplayerAdventure));
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
        return "Tips & Tricks";
    }

    @Override
    protected String categoryDescription() {
        return "Practical strategies for staying balanced, recovering quickly, and getting the most out of Nourished.";
    }

    @Override
    protected BookIconModel categoryIcon() {
        return BookIconModel.create(Items.EXPERIENCE_BOTTLE);
    }

    @Override
    public String categoryId() {
        return ID;
    }
}
