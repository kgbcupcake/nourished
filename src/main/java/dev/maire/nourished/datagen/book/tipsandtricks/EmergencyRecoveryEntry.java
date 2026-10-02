package dev.maire.nourished.datagen.book.tipsandtricks;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class EmergencyRecoveryEntry extends EntryProvider {

    public static final String ID = "emergency_recovery";

    // Food group colors
    private static final String PROTEINS = "[#](E85D75)";
    private static final String DAIRY = "[#](6FB8E8)";
    private static final String FRUITS = "[#](F5A623)";
    private static final String VEGETABLES = "[#](7BC96F)";

    // Status colors
    private static final String CRITICAL = "[#](E85D75)";
    private static final String GOOD = "[#](7BC96F)";

    // Reset
    private static final String RESET = "[#]()";

    public EmergencyRecoveryEntry(CategoryProvider parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {

        this.page("when_things_go_wrong", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("When Things Go Wrong");

        this.pageText("""
                If multiple bars hit critical at once — maybe after a long trip far from base — do not panic. Address groups one at a time, starting with the penalty that hurts most in your current situation.

                %sIn combat or exploring:%s fix Proteins (attack) and Dairy (knockback) first.
                %sBack at base:%s fix Fruits (health regen) and Vegetables (saturation) for recovery.
                """.formatted(
                CRITICAL, RESET,
                GOOD, RESET
        ));

        this.page("quick_fix_foods", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Quick Fix Foods");

        this.pageText("""
                Keep these high-nutrition emergency foods in your inventory for quick recovery:

                **Cooked Beef** — fastest %sProteins%s top-up per item.
                **Golden Carrot** — fast %sVegetables%s recovery.
                **Apple** — widely available, decent %sFruits%s nutrition.
                **Milk Bucket** — instant %sDairy%s coverage and also removes debuffs.
                """.formatted(
                PROTEINS, RESET,
                VEGETABLES, RESET,
                FRUITS, RESET,
                DAIRY, RESET
        ));

        this.page("the_milk_trick", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("The Milk Trick");

        this.pageText("""
                **Milk Bucket** not only fills the Dairy bar — it also removes all active status effects including nutrition penalties.

                If you are stacking multiple penalties at once, drinking a milk bucket gives you a clean slate and buys time to properly address the underlying bar levels. Keep at least one in your hotbar when venturing far from home.
                """);

        this.page("nutritional_debt", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Nutritional Debt");

        this.pageText("""
                If a bar has been at zero for a sustained period, you may be in %sNutritional Debt%s — a stronger debuff state. Refilling the bar does not immediately clear debt.

                To recover: keep the group above zero consistently. Do not let it drop back to empty while recovering. Eat from that group multiple times over the next in-game day to work off the debt.
                """.formatted(
                CRITICAL, RESET
        ));

        this.page("sleep_to_recover", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Sleep to Recover");

        this.pageText("""
                Sleep is the most powerful recovery tool in Nourished. Even if not all bars are above the bonus threshold, sleeping with all bars above **zero** prevents debt accumulation and gives a partial recovery.

                If you are in a rough state, prioritize getting every bar above the critical line, then sleep. The next morning you will be in a much better position to top up properly.
                """);
    }

    @Override
    protected String entryName() {
        return "Emergency Recovery";
    }

    @Override
    protected String entryDescription() {
        return "How to recover quickly when multiple bars hit critical.";
    }

    @Override
    protected Pair<Integer, Integer> entryBackground() {
        return EntryBackground.DEFAULT;
    }

    @Override
    protected BookIconModel entryIcon() {
        return BookIconModel.create(Items.GOLDEN_APPLE);
    }

    @Override
    protected String entryId() {
        return ID;
    }
}
