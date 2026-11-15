package dev.maire.nourished.datagen.book.nutrition;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import dev.maire.nourished.datagen.book.GuideEntry;
import net.minecraft.world.item.Items;

import static dev.maire.nourished.datagen.book.GuideColors.*;

public class WhenNeglectedEntry extends GuideEntry {

    public static final String ID = "when_neglected";

    public WhenNeglectedEntry(CategoryProvider parent) {
        super(parent, ID, "When You Need Something", "Penalties for letting a group run low.", Items.ROTTEN_FLESH);
    }

    @Override
    protected void generatePages() {
        text("penalties", "Food Group Penalties",
                "Let a group drop " + hl(BAD, "below 25%") + " and you feel it:",
                hl(GRAINS, "Grains") + ": Weakness",
                hl(VEGETABLES, "Vegetables") + ": Slowness",
                hl(PROTEINS, "Proteins") + ": Mining Fatigue",
                hl(FRUITS, "Fruits") + ": Unluck",
                hl(DAIRY, "Dairy") + ": nothing by default");

        text("why", "Why It Exists",
                "**Why punish a low bar?** It's your body telling you what it's missing.",
                "Each penalty points at the group to eat next. Fix the bar and the effect goes away on its own.");

        text("what_to_do", "What Do I Do?",
                "Eat anything from that group. The penalty lifts once the bar is back above 25%.",
                "Ignore it for too long and you become Malnourished, which is harder to shake. See "
                        + link("Deficiency", NUTRITION, DeficiencyEntry.ID) + ".");
    }
}
