package dev.maire.nourished.datagen.book.gettingstarted;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.SingleBookSubProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookCategoryModel;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
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
        var introduction = this.add(new IntroductionEntry(this).generate('i'));
        var hud = this.add(new HudEntry(this).generate('h')).withParent(introduction);
        var dietScreen = this.add(new DietScreenEntry(this).generate('d')).withParent(hud);
        var sleepBonus = this.add(new SleepBonusEntry(this).generate('s')).withParent(dietScreen);
        var effects = this.add(new EffectsEntry(this).generate('e')).withParent(sleepBonus);
        var foodTooltips = this.add(new FoodTooltipsEntry(this).generate('t')).withParent(effects);
        var notifications = this.add(new NotificationsEntry(this).generate('n')).withParent(foodTooltips);
        this.add(new FirstDayEntry(this).generate('y')).withParent(notifications);
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
