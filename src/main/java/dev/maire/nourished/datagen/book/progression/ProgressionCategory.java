package dev.maire.nourished.datagen.book.progression;

import com.klikli_dev.modonomicon.api.datagen.SingleBookSubProvider;
import dev.maire.nourished.datagen.book.GuideCategory;
import dev.maire.nourished.datagen.book.GuideColors;
import net.minecraft.world.item.Items;

public class ProgressionCategory extends GuideCategory {

    public static final String ID = GuideColors.PROGRESSION;

    public ProgressionCategory(SingleBookSubProvider parent) {
        super(parent, ID, "Progression", "Milestones and long-term goals.", Items.NETHER_STAR);
    }

    @Override
    protected String[] generateEntryMap() {
        return new String[]{
                "____m____",
                "_________",
                "_g_____a_",
                "_________",
                "__p______"
        };
    }

    @Override
    protected void generateEntries() {
        var milestones = this.add(new MilestonesEntry(this).generate('m'));
        var groups = this.add(under(milestones, new FoodGroupMilestonesEntry(this).generate('g')));
        this.add(under(milestones, new ActivityMilestonesEntry(this).generate('a')));
        this.add(under(groups, new PerfectlyBalancedEntry(this).generate('p')));
    }
}
