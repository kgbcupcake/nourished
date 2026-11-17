package dev.maire.nourished.datagen.book.nutrition;

import com.klikli_dev.modonomicon.api.datagen.SingleBookSubProvider;
import dev.maire.nourished.datagen.book.GuideCategory;
import dev.maire.nourished.datagen.book.GuideColors;
import net.minecraft.world.item.Items;

public class NutritionCategory extends GuideCategory {

    public static final String ID = GuideColors.NUTRITION;

    public NutritionCategory(SingleBookSubProvider parent) {
        super(parent, ID, "Nutrition", "What Nourished does with the food you eat.", Items.GOLDEN_APPLE);
    }

    @Override
    protected String[] generateEntryMap() {
        return new String[]{
                "______b______",
                "_____________",
                "__h___s___n__",
                "_____________",
                "_____________",
                "_d__r_m_t__c_",
                "_____________",
                "_e____f____u_"
        };
    }

    @Override
    protected void generateEntries() {
        var balance = this.add(new BalanceScoreEntry(this).generate('b'));
        var healthy = this.add(under(balance, new WhenHealthyEntry(this).generate('h')));
        var neglected = this.add(under(balance, new WhenNeglectedEntry(this).generate('n')));
        this.add(under(balance, new StackingEntry(this).generate('s')));
        var multipliers = this.add(under(balance, new MultiplierSystemEntry(this).generate('m')));
        this.add(under(multipliers, new DiminishingReturnsEntry(this).generate('r')));
        this.add(under(multipliers, new StreakPenaltyEntry(this).generate('t')));
        this.add(under(neglected, new NutritionalDebtEntry(this).generate('c')));
        var deficiency = this.add(under(neglected, new DeficiencyEntry(this).generate('f')));
        this.add(under(deficiency, new EmergencyRecoveryEntry(this).generate('u')));
        this.add(under(healthy, new CaloriesEntry(this).generate('d')));
        this.add(under(healthy, new NutrientsEntry(this).generate('e')));
    }
}
