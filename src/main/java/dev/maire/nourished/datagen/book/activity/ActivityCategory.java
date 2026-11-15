package dev.maire.nourished.datagen.book.activity;

import com.klikli_dev.modonomicon.api.datagen.SingleBookSubProvider;
import dev.maire.nourished.datagen.book.GuideCategory;
import dev.maire.nourished.datagen.book.GuideColors;
import net.minecraft.world.item.Items;

public class ActivityCategory extends GuideCategory {

    public static final String ID = GuideColors.ACTIVITY_CAT;

    public ActivityCategory(SingleBookSubProvider parent) {
        super(parent, ID, "Activity", "How what you do changes what you need.", Items.LEATHER_BOOTS);
    }

    @Override
    protected String[] generateEntryMap() {
        return new String[]{
                "____a____",
                "_________",
                "_s__m__t_",
                "_________",
                "____l____"
        };
    }

    @Override
    protected void generateEntries() {
        var activity = this.add(new ActivityEntry(this).generate('a'));
        this.add(under(activity, new SprintSwimEntry(this).generate('s')));
        this.add(under(activity, new MiningCombatEntry(this).generate('m')));
        this.add(under(activity, new StarvationEntry(this).generate('t')));
        this.add(under(activity, new ActivityLogEntry(this).generate('l')));
    }
}
