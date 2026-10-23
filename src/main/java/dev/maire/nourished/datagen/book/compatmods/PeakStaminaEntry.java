package dev.maire.nourished.datagen.book.compatmods;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class PeakStaminaEntry extends EntryProvider {

    public static final String ID = "peak_stamina";

    private static final String GOOD = "[#](52c252)";
    private static final String NEUTRAL = "[#](E8C24F)";
    private static final String BAD = "[#](e05252)";
    private static final String RESET = "[#]()";

    public PeakStaminaEntry(CategoryProvider parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {

        this.page("peak_stamina", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Peak Stamina");

        this.pageText("""
                Peak Stamina adds a stamina system for sprinting, jumping, and combat actions. When both mods are installed, Nourished modifies Peak Stamina's attributes in real time based on your **average nutrition level** across all six food groups.
                """);

        this.page("how_its_calculated", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("How It's Calculated");

        this.pageText("""
                All six nutrition bars are averaged together into a single value. This average drives all stamina modifiers: it is **not** per-group. Keeping all six bars healthy is more important than maxing one or two.\s\s
                %sAbove 75%% average%s: bonuses apply.
                %s25%% – 75%% average%s: neutral, no modifier.
                %sBelow 25%% average%s: penalties apply.
                """.formatted(
                GOOD, RESET,
                NEUTRAL, RESET,
                BAD, RESET
        ));

        this.page("bonuses", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Bonuses (avg > 75%%)");

        this.pageText("""
                When your average nutrition is above 75%%:\s\s
                %s+25%% Stamina Regen%s: stamina recovers significantly faster.
                %s+10%% Max Stamina%s: larger total stamina pool.
                %s-15%% Stamina Usage%s: actions cost less stamina.
                %s+30%% Penalty Decay%s: stamina exhaustion penalties fade faster.\s\s
                All four bonuses are active simultaneously when the threshold is met.
                """.formatted(
                GOOD, RESET,
                GOOD, RESET,
                GOOD, RESET,
                GOOD, RESET
        ));

        this.page("penalties", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Penalties (avg < 25%%)");

        this.pageText("""
                When your average nutrition drops below 25%%:\s\s
                %s-25%% Stamina Regen%s: stamina recovers much slower.
                %s-15%% Max Stamina%s: smaller total stamina pool.
                %s+25%% Stamina Usage%s: actions cost more stamina.
                %s+30%% Exhaustion Duration%s: stamina exhaustion lasts longer.\s\s
                This combination makes low nutrition extremely punishing in combat and exploration.
                """.formatted(
                BAD, RESET,
                BAD, RESET,
                BAD, RESET,
                BAD, RESET
        ));

        this.page("practical_advice", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Practical Advice");

        this.pageText("""
                The 75%% bonus threshold means keeping **all six bars above 75%%** on average, not just one or two at full. A single bar at zero drags the average down significantly.\s\s
                A player with five bars at 100%% and one bar at zero has an average of ~83%%, just above the bonus line. Let **two** bars hit zero and the average drops to ~67%%, losing all bonuses. Keep every bar topped up.
                """);
    }

    @Override
    protected String entryName() {
        return "Peak Stamina";
    }

    @Override
    protected String entryDescription() {
        return "How average nutrition drives stamina bonuses and penalties.";
    }

    @Override
    protected Pair<Integer, Integer> entryBackground() {
        return EntryBackground.DEFAULT;
    }

    @Override
    protected BookIconModel entryIcon() {
        return BookIconModel.create(Items.FEATHER);
    }

    @Override
    protected String entryId() {
        return ID;
    }
}
