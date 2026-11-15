package dev.maire.nourished.datagen.book.food;

import com.klikli_dev.modonomicon.api.datagen.SingleBookSubProvider;
import dev.maire.nourished.datagen.book.GuideCategory;
import dev.maire.nourished.datagen.book.GuideColors;
import net.minecraft.world.item.Items;

public class FoodCategory extends GuideCategory {

    public static final String ID = GuideColors.FOOD;

    public FoodCategory(SingleBookSubProvider parent) {
        super(parent, ID, "Food", "What you eat, and how Nourished reads it.", Items.APPLE);
    }

    @Override
    protected String[] generateEntryMap() {
        return new String[]{
                "______g______",
                "_____________",
                "_a__v_p_r__d_",
                "_____________",
                "___t_____m___",
                "_____________",
                "_e____w____o_"
        };
    }

    @Override
    protected void generateEntries() {
        var groups = this.add(new FoodGroupsEntry(this).generate('g'));
        this.add(under(groups, new FruitsEntry(this).generate('a')));
        this.add(under(groups, new VegetablesEntry(this).generate('v')));
        this.add(under(groups, new ProteinsEntry(this).generate('p')));
        this.add(under(groups, new GrainsEntry(this).generate('r')));
        this.add(under(groups, new DairyEntry(this).generate('d')));
        var tooltips = this.add(under(groups, new ReadingTooltipsEntry(this).generate('t')));
        var families = this.add(under(groups, new FoodFamiliesEntry(this).generate('m')));
        this.add(under(tooltips, new EfficientFarmingEntry(this).generate('e')));
        this.add(under(families, new EatingWhenFullEntry(this).generate('w')));
        this.add(under(families, new FoodCombosEntry(this).generate('o')));
    }
}
