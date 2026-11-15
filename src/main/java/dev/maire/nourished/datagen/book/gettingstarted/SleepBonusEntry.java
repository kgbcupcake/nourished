package dev.maire.nourished.datagen.book.gettingstarted;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import dev.maire.nourished.core.effect.SleepBonus;
import dev.maire.nourished.datagen.book.GuideEntry;
import net.minecraft.world.item.Items;

import static dev.maire.nourished.datagen.book.GuideColors.*;

public class SleepBonusEntry extends GuideEntry {

    public static final String ID = "sleep_bonus";

    public SleepBonusEntry(CategoryProvider parent) {
        super(parent, ID, "Sleep Bonus", "The reward for going to bed well-nourished.", Items.RED_BED);
    }

    @Override
    protected void generatePages() {
        int percent = Math.round(SleepBonus.THRESHOLD * 100);
        int seconds = SleepBonus.DURATION_TICKS / 20;

        spotlight("sleep_bonus", Items.RED_BED, "Sleep Bonus",
                "A good diet makes for better rest.",
                "Wake up with every food group at least " + hl(GOOD, percent + "% full") + " and you get "
                        + hl(BAD, "Regeneration I") + " for " + seconds + " seconds.");

        text("how_it_works", "How It Works",
                "The check happens the moment you wake up, not when you lie down.",
                "One low group is enough to miss it, so top up whatever is lowest before bed.",
                "Server owners can turn it off with " + hl(MUTED, "enableSleepBonus") + ". The values above are the defaults.");
    }
}
