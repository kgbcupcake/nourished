package dev.maire.nourished.datagen.book.activity;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import dev.maire.nourished.datagen.book.GuideEntry;
import net.minecraft.world.item.Items;

import static dev.maire.nourished.datagen.book.GuideColors.*;

public class MiningCombatEntry extends GuideEntry {

    public static final String ID = "mining_combat";

    public MiningCombatEntry(CategoryProvider parent) {
        super(parent, ID, "Mining & Combat", "Every block and every kill has a small cost.", Items.IRON_PICKAXE);
    }

    @Override
    protected void generatePages() {
        text("mining", "Mining",
                "Each block you break costs a " + hl(ACTIVITY, "tiny") + " amount from every bar.",
                "One block is nothing. A whole strip mine is a meal.");

        text("combat", "Combat",
                "Each kill costs a bit more than a block, about " + hl(ACTIVITY, "1%") + " of every bar by default.",
                "Mob farms and long fights will pull your bars down.");

        text("what_to_do", "What Do I Do?",
                "Bring food on mining trips and eat between fights.",
                "Low Proteins gives Mining Fatigue and low Grains gives Weakness, so those two matter most here.");
    }
}
