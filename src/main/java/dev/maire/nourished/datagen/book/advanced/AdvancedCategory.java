package dev.maire.nourished.datagen.book.advanced;

import com.klikli_dev.modonomicon.api.datagen.SingleBookSubProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookEntryModel;
import com.klikli_dev.modonomicon.api.datagen.book.condition.BookModLoadedConditionModel;
import dev.maire.nourished.datagen.book.GuideCategory;
import dev.maire.nourished.datagen.book.GuideColors;
import dev.maire.nourished.datagen.book.advanced.compat.*;
import net.minecraft.world.item.Items;

public class AdvancedCategory extends GuideCategory {

    public static final String ID = GuideColors.ADVANCED;

    public AdvancedCategory(SingleBookSubProvider parent) {
        super(parent, ID, "Advanced", "Customizing and extending Nourished.", Items.COMPARATOR);
    }

    @Override
    protected String[] generateEntryMap() {
        return new String[]{
                "______c______",
                "_____________",
                "_u__i___e__x_",
                "_____________",
                "____s___o____",
                "_____________",
                "_d__k________",
                "_____________",
                "______v______",
                "_____________",
                "_1_2_3_5_6_7_"
        };
    }

    @Override
    protected void generateEntries() {
        var config = this.add(new ConfigurationEntry(this).generate('c'));
        this.add(under(config, new DynamicUiEntry(this).generate('u')));
        var editor = this.add(under(config, new ItemEditorEntry(this).generate('i')));
        this.add(under(config, new ConfiguringEffectsEntry(this).generate('e')));
        this.add(under(config, new CommandsEntry(this).generate('x')));
        this.add(under(editor, new FoodScannerEntry(this).generate('s')));
        var overrides = this.add(under(editor, new FoodOverridesEntry(this).generate('o')));
        var datapacks = this.add(under(overrides, new DatapacksEntry(this).generate('d')));
        this.add(under(datapacks, new KubeJsEntry(this).generate('k')));
        var compat = this.add(under(overrides, new OverviewEntry(this).generate('v')));
        mod(compat, new CroptopiaEntry(this).generate('1'), "croptopia");
        mod(compat, new FarmersDelightEntry(this).generate('2'), "farmersdelight");
        mod(compat, new PamsHarvestcraftEntry(this).generate('3'), "pamhc2crops");
        mod(compat, new HerbsAndHarvestEntry(this).generate('5'), "herbsandharvest");
        mod(compat, new FarmAndCharmEntry(this).generate('6'), "farm_and_charm");
        mod(compat, new ButcheryEntry(this).generate('7'), "butchery");
    }

    private void mod(BookEntryModel parent, BookEntryModel entry, String modId) {
        this.add(under(parent, entry).withCondition(BookModLoadedConditionModel.create().withModId(modId)));
    }
}
