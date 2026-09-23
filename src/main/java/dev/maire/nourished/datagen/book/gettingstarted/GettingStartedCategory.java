package dev.maire.nourished.datagen.book.gettingstarted;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.SingleBookSubProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookCategoryModel;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.book.BookCategoryBackgroundParallaxLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

public class GettingStartedCategory extends CategoryProvider {

    public static final String ID = "getting_started";

    public GettingStartedCategory(SingleBookSubProvider parent) {
        super(parent);
    }

    @Override
    protected String[] generateEntryMap() {
        /*
         * Getting Started follows a winding learning path.
         *
         * The player starts at the top-left and works through
         * Nourished's core systems before reaching the final
         * "Your First Day" entry.
         */
        return new String[]{
                "i________",
                "_________",
                "____h____",
                "_________",
                "________d",
                "_________",
                "_____s___",
                "_________",
                "___e_____",
                "_________",
                "_______t_",
                "_________",
                "____n____",
                "_________",
                "________y"
        };
    }

    @Override
    protected void generateEntries() {

        // Introduction
        var introduction = this.add(
                new IntroductionEntry(this).generate('i')
        );

        // HUD
        var hud = this.add(
                new HudEntry(this).generate('h')
        )
                .withParent(introduction)
                .withCondition(this.condition().entryRead(introduction));

        // Diet Screen
        var dietScreen = this.add(
                new DietScreenEntry(this).generate('d')
        )
                .withParent(hud)
                .withCondition(this.condition().entryRead(hud));

        // Sleep Bonus
        var sleepBonus = this.add(
                new SleepBonusEntry(this).generate('s')
        )
                .withParent(dietScreen)
                .withCondition(this.condition().entryRead(dietScreen));

        // Effects
        var effects = this.add(
                new EffectsEntry(this).generate('e')
        )
                .withParent(sleepBonus)
                .withCondition(this.condition().entryRead(sleepBonus));

        // Food Tooltips
        var foodTooltips = this.add(
                new FoodTooltipsEntry(this).generate('t')
        )
                .withParent(effects)
                .withCondition(this.condition().entryRead(effects));

        // Notifications
        var notifications = this.add(
                new NotificationsEntry(this).generate('n')
        )
                .withParent(foodTooltips)
                .withCondition(this.condition().entryRead(foodTooltips));

        // Your First Day
        this.add(
                new FirstDayEntry(this).generate('y')
        )
                .withParent(notifications)
                .withCondition(this.condition().entryRead(notifications));
    }

    @Override
    protected BookCategoryModel additionalSetup(BookCategoryModel category) {
        /*
         * Layered Modonomicon background with different scroll speeds
         * to create a subtle parallax effect while navigating the map.
         */
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
        return "Getting Started";
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