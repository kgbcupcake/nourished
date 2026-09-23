package dev.maire.nourished.datagen.book.tipsandtricks;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class DailyRoutineEntry extends EntryProvider {

    public static final String ID = "daily_routine";

    // Food group colors
    private static final String FRUITS = "[#](F5A623)";
    private static final String VEGETABLES = "[#](7BC96F)";
    private static final String PROTEINS = "[#](E85D75)";
    private static final String GRAINS = "[#](E6C65C)";
    private static final String DAIRY = "[#](6FB8E8)";

    // Status colors
    private static final String CRITICAL = "[#](E85D75)";
    private static final String WARNING = "[#](E6C65C)";

    // Reset
    private static final String RESET = "[#]()";

    public DailyRoutineEntry(CategoryProvider parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {

        this.page("ideal_loop", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("The Ideal Daily Loop");

        this.pageText("""
                Nourished is designed around a simple daily rhythm. Follow this loop and you will rarely suffer a penalty:

                **Morning:** eat one food from any low group before leaving base.
                **During the day:** eat normally for hunger; rotate food types.
                **Evening:** top up any bars below 50% before sleeping.
                **Sleep:** with all bars healthy to earn the Sleep Bonus.
                """);

        this.page("hud_is_your_friend", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("The HUD is Your Friend");

        this.pageText("""
                Glance at the HUD bar overlay regularly — not just when you feel effects. Bars drop slowly and you want to catch them before they hit the critical threshold, not after.

                If a bar is in the %sred range%s, address it immediately. If it is in the %syellow range%s, eat something from that group before your next sleep.
                """.formatted(
                CRITICAL, RESET,
                WARNING, RESET
        ));

        this.page("batch_eating", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Batch Eating");

        this.pageText("""
                You do not need to eat from all five groups at every meal. Instead, eat 2–3 groups per sitting and rotate which groups you cover each time.

                Example rotation:
                **Morning:** %sFruits%s + %sProteins%s
                **Midday:** %sGrains%s + %sVegetables%s
                **Evening:** %sDairy%s

                This keeps all bars healthy without ever feeling like a chore.
                """.formatted(
                FRUITS, RESET, PROTEINS, RESET,
                GRAINS, RESET, VEGETABLES, RESET,
                DAIRY, RESET
        ));

        this.page("before_a_fight", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Before a Fight");

        this.pageText("""
                Before entering a dungeon, raid, or boss fight, check that these two groups are above their good threshold:

                %sProteins%s — bonus max health helps survive burst damage.
                %sDairy%s — bonus armor toughness reduces effective incoming damage.

                If either is low, eat a cooked steak and bucket a cow before you go in. The difference in survivability is significant.
                """.formatted(
                PROTEINS, RESET,
                DAIRY, RESET
        ));

        this.page("before_an_xp_session", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Before an XP Session");

        this.pageText("""
                Planning to enchant gear or grind a mob farm? Make sure your %sGrains%s group is well above the bonus threshold before you start.

                The Grains bonus — faster experience gain — passively increases all XP earned while active. A pumpkin pie and a loaf of bread before your session can meaningfully speed up an enchanting or leveling run.
                """.formatted(
                GRAINS, RESET
        ));
    }

    @Override
    protected String entryName() {
        return "Daily Routine";
    }

    @Override
    protected String entryDescription() {
        return "A simple daily rhythm that keeps every bar healthy.";
    }

    @Override
    protected Pair<Integer, Integer> entryBackground() {
        return EntryBackground.DEFAULT;
    }

    @Override
    protected BookIconModel entryIcon() {
        return BookIconModel.create(Items.CLOCK);
    }

    @Override
    protected String entryId() {
        return ID;
    }
}
