package dev.maire.nourished.datagen.book.guthealth;

import com.klikli_dev.modonomicon.api.datagen.SingleBookSubProvider;
import dev.maire.nourished.datagen.book.GuideCategory;
import dev.maire.nourished.datagen.book.GuideColors;
import net.minecraft.world.item.Items;

public class GutHealthCategory extends GuideCategory {

    public static final String ID = GuideColors.GUT_HEALTH;

    public GutHealthCategory(SingleBookSubProvider parent) {
        super(parent, ID, "Gut Health", "Raw food, food tiers, resistance and recovery.", Items.BEEF);
    }

    @Override
    protected String[] generateEntryMap() {
        return new String[]{
                "r___",
                "____",
                "__g_"
        };
    }

    @Override
    protected void generateEntries() {
        var raw = this.add(new RawFoodEntry(this).generate('r'));
        this.add(under(raw, new GutHealthEntry(this).generate('g')));
    }
}
