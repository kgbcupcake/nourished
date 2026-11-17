package dev.maire.nourished.datagen.book.gettingstarted;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import dev.maire.nourished.datagen.book.GuideEntry;
import dev.maire.nourished.datagen.book.nutrition.WhenHealthyEntry;
import dev.maire.nourished.datagen.book.activity.MiningCombatEntry;
import net.minecraft.world.item.Items;

import static dev.maire.nourished.datagen.book.GuideColors.*;

public class DailyRoutineEntry extends GuideEntry {

    public static final String ID = "daily_routine";

    public DailyRoutineEntry(CategoryProvider parent) {
        super(parent, ID, "Daily Routine", "A simple loop that keeps every bar healthy.", Items.CLOCK);
    }

    @Override
    protected void generatePages() {
        text("ideal_loop", "The Ideal Daily Loop",
                "**Morning:** eat something from your lowest group before leaving base.",
                "**During the day:** eat for hunger, but rotate what you grab.",
                "**Evening:** top up anything under half before bed.",
                "**Sleep:** wake up with the Sleep Bonus.");

        text("hud_is_your_friend", "The HUD is Your Friend",
                "Bars drop slowly. Glance at the HUD now and then instead of waiting for a debuff.",
                hl(BAD, "Red") + " means eat from that group now. " + hl(GOLD, "Yellow") + " means before bed.");

        text("batch_eating", "Batch Eating",
                "You don't need all five groups every meal. Two or three per sitting, rotated, is plenty.",
                "**Morning:** " + hl(FRUITS, "Fruits") + " + " + hl(PROTEINS, "Proteins"),
                "**Midday:** " + hl(GRAINS, "Grains") + " + " + hl(VEGETABLES, "Vegetables"),
                "**Evening:** " + hl(DAIRY, "Dairy"));

        text("before_a_fight", "Before a Fight",
                "Low " + hl(GRAINS, "Grains") + " gives Weakness and low " + hl(VEGETABLES, "Vegetables") + " gives Slowness. Neither belongs in a boss fight.",
                "Better still, go in with every group above 75% for " + link("Health Boost and Regeneration", NUTRITION, WhenHealthyEntry.ID) + ".");

        text("before_mining", "Before a Mining Trip",
                "Low " + hl(PROTEINS, "Proteins") + " gives Mining Fatigue. Eat some cooked meat before you head down.",
                "Mining also drains your bars a little per block, so pack food. See " + link("Mining & Combat", ACTIVITY_CAT, MiningCombatEntry.ID) + ".");
    }
}
