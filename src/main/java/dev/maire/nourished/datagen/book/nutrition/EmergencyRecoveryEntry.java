package dev.maire.nourished.datagen.book.nutrition;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import dev.maire.nourished.datagen.book.GuideEntry;
import net.minecraft.world.item.Items;

import static dev.maire.nourished.datagen.book.GuideColors.*;

public class EmergencyRecoveryEntry extends GuideEntry {

    public static final String ID = "emergency_recovery";

    public EmergencyRecoveryEntry(CategoryProvider parent) {
        super(parent, ID, "Emergency Recovery", "What to do when several bars crash at once.", Items.GOLDEN_CARROT);
    }

    @Override
    protected void generatePages() {
        text("when_things_go_wrong", "When Things Go Wrong",
                "Back from a long trip with several bars in the red? Fix them one at a time, starting with whatever hurts most right now.",
                hl(BAD, "Fighting:") + " Grains first (Weakness).",
                hl(BAD, "Mining:") + " Proteins first (Mining Fatigue).",
                hl(BAD, "Travelling:") + " Vegetables first (Slowness).");

        text("quick_fix_foods", "Quick Fix Foods",
                "Good foods to keep on you:",
                hl(PROTEINS, "Cooked Beef") + ": a big Proteins top-up.",
                hl(VEGETABLES, "Golden Carrot") + ": a big Vegetables top-up.",
                hl(FRUITS, "Apple") + ": easy to find, decent Fruits.",
                hl(DAIRY, "Milk Bucket") + ": covers Dairy in one go.");

        text("the_milk_trick", "The Milk Trick",
                "Milk clears status effects, including Nourished's penalties.",
                "It only buys you a few seconds, though. Penalties come back while the bar is still low. Eat first, then drink.");

        text("long_term", "Long-Term Neglect",
                "Leave a group low for a long time and you become " + hl(BAD, "Malnourished") + ". One meal won't fix it.",
                "Keep the group healthy and it wears off. See " + link("Deficiency", NUTRITION, DeficiencyEntry.ID) + ".");
    }
}
