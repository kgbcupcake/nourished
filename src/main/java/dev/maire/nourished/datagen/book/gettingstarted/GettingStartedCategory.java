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
        // A zigzag quest chain, Thaumonomicon-style.
        return new String[]{
                "_i_______",
                "_________",
                "___h_____",
                "_________",
                "_____d___",
                "_________",
                "_______s_",
                "_________",
                "_______e_",
                "_________",
                "_____t___",
                "_________",
                "___n_____",
                "_________",
                "_y_______",
        };
    }

    @Override
    protected void generateEntries() {
        // Each entry requires the previous one to be read before it unlocks, so the chain plays
        // out in order rather than just being visually connected.
        var introduction = this.add(new IntroductionEntry(this).generate('i'));
        var hud = this.add(new HudEntry(this).generate('h'))
                .withParent(introduction)
                .withCondition(this.condition().entryRead(introduction));
        var dietScreen = this.add(new DietScreenEntry(this).generate('d'))
                .withParent(hud)
                .withCondition(this.condition().entryRead(hud));
        var sleepBonus = this.add(new SleepBonusEntry(this).generate('s'))
                .withParent(dietScreen)
                .withCondition(this.condition().entryRead(dietScreen));
        var effects = this.add(new EffectsEntry(this).generate('e'))
                .withParent(sleepBonus)
                .withCondition(this.condition().entryRead(sleepBonus));
        var foodTooltips = this.add(new FoodTooltipsEntry(this).generate('t'))
                .withParent(effects)
                .withCondition(this.condition().entryRead(effects));
        var notifications = this.add(new NotificationsEntry(this).generate('n'))
                .withParent(foodTooltips)
                .withCondition(this.condition().entryRead(foodTooltips));
        this.add(new FirstDayEntry(this).generate('y'))
                .withParent(notifications)
                .withCondition(this.condition().entryRead(notifications));
    }

    @Override
    protected BookCategoryModel additionalSetup(BookCategoryModel category) {
        // Modonomicon's own nebula/starfield background, layered for parallax scrolling.
        return category.withBackgroundParallaxLayers(
                new BookCategoryBackgroundParallaxLayer(ResourceLocation.parse("modonomicon:textures/gui/parallax/flow/base.png"), 0.7f, -1),
                new BookCategoryBackgroundParallaxLayer(ResourceLocation.parse("modonomicon:textures/gui/parallax/flow/1.png"), 1f, -1),
                new BookCategoryBackgroundParallaxLayer(ResourceLocation.parse("modonomicon:textures/gui/parallax/flow/2.png"), 1.4f, -1)
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
