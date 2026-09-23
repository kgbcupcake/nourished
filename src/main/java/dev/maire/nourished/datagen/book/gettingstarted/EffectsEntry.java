package dev.maire.nourished.datagen.book.gettingstarted;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;

import net.minecraft.world.item.Items;

public class EffectsEntry extends EntryProvider {

    public static final String ID = "effects";

    // Food group colors
    private static final String FRUITS = "[#](F5A623)";
    private static final String VEGETABLES = "[#](7BC96F)";
    private static final String PROTEINS = "[#](E85D75)";
    private static final String GRAINS = "[#](E6C65C)";
    private static final String DAIRY = "[#](6FB8E8)";

    // Bonus effect colors
    private static final String FRUITS_BONUS = "[#](FFB84D)";
    private static final String VEGETABLES_BONUS = "[#](8FE388)";
    private static final String PROTEINS_BONUS = "[#](F0808F)";
    private static final String GRAINS_BONUS = "[#](F2D77C)";
    private static final String DAIRY_BONUS = "[#](8CCCF0)";

    // Penalty effect colors
    private static final String FRUITS_PENALTY = "[#](D98200)";
    private static final String VEGETABLES_PENALTY = "[#](4F9E4F)";
    private static final String PROTEINS_PENALTY = "[#](C43D52)";
    private static final String GRAINS_PENALTY = "[#](B89F32)";
    private static final String DAIRY_PENALTY = "[#](4D91BA)";

    // General effect colors
    private static final String BALANCE_BONUS = "[#](F5A623)";
    private static final String PENALTY = "[#](E85D75)";

    // Reset
    private static final String RESET = "[#]()";

    public EffectsEntry(CategoryProvider parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {

        // ---------------------------------------------------------------------
        // Balance Bonus
        // ---------------------------------------------------------------------

        this.page("balance_bonus", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Balance Bonus");

        this.pageText("""
                When all of the food groups are above their threshold, you gain the %s**Balance Bonus**%s, a persistent buff that improves health, speed, or other stats depending on your configuration.

                Keeping the bonus active should be your primary goal.

                """.formatted(
                BALANCE_BONUS,
                RESET
        ));

        // ---------------------------------------------------------------------
        // Per-Group Bonuses
        // ---------------------------------------------------------------------

        this.page("per_group_bonuses", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Per-Group Bonuses");

        this.pageText("""
                Each food group has its own bonus active when healthy:

                %s**Fruits**%s: %s increased regeneration speed.%s

                %s**Vegetables**%s: %s improved hunger saturation.%s

                %s**Proteins**%s: %s increased max health.%s

                %s**Grains**%s: %s faster experience gain.%s

                %s**Dairy**%s: %s increased armor toughness.%s

                """.formatted(
                FRUITS, RESET, FRUITS_BONUS, RESET,
                VEGETABLES, RESET, VEGETABLES_BONUS, RESET,
                PROTEINS, RESET, PROTEINS_BONUS, RESET,
                GRAINS, RESET, GRAINS_BONUS, RESET,
                DAIRY, RESET, DAIRY_BONUS, RESET
        ));

        // ---------------------------------------------------------------------
        // Penalties
        // ---------------------------------------------------------------------

        this.page("penalties", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Penalties");

        this.pageText("""
                Letting any group fall to zero applies a %s**penalty**%s for that group. Penalties stack: neglecting multiple groups at once causes increasingly severe debuffs.

                Recovery is straightforward: eat foods from the depleted group and the penalty fades as the bar refills.

                """.formatted(
                PENALTY,
                RESET
        ));

        // ---------------------------------------------------------------------
        // Per-Group Penalties
        // ---------------------------------------------------------------------

        this.page("per_group_penalties", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Per-Group Penalties");

        this.pageText("""
                Each food group has its own penalty when depleted:

                %s**Fruits**%s: %s reduced max health.%s

                %s**Vegetables**%s: %s reduced movement speed.%s

                %s**Proteins**%s: %s reduced attack damage.%s

                %s**Grains**%s: %s reduced hunger restoration.%s

                %s**Dairy**%s: %s reduced knockback resistance.%s

                """.formatted(
                FRUITS, RESET, FRUITS_PENALTY, RESET,
                VEGETABLES, RESET, VEGETABLES_PENALTY, RESET,
                PROTEINS, RESET, PROTEINS_PENALTY, RESET,
                GRAINS, RESET, GRAINS_PENALTY, RESET,
                DAIRY, RESET, DAIRY_PENALTY, RESET
        ));

        // ---------------------------------------------------------------------
        // Stacking Effects
        // ---------------------------------------------------------------------

        this.page("stacking_effects", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Stacking Effects");

        this.pageText("""
                Effects from multiple groups stack. Having all bonuses active simultaneously gives a compound advantage across health, damage, speed, regen, XP, and armor.

                Conversely, multiple penalties at once — especially %s**Fruits**%s (health) + %s**Proteins**%s (damage) + %s**Dairy**%s (knockback) — can make combat very dangerous. Prioritize preventing that combination above all else.

                """.formatted(
                FRUITS, RESET,
                PROTEINS, RESET,
                DAIRY, RESET
        ));

        // ---------------------------------------------------------------------
        // Viewing Active Effects
        // ---------------------------------------------------------------------

        this.page("viewing_active_effects", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Viewing Active Effects");

        this.pageText("""
                Your currently active Nourished bonuses and penalties are shown on the left panel of the Diet Screen. Open it to see a full list of current effects and which groups are causing them.

                The HUD bar overlay gives a quick at-a-glance view of bar levels in the game world without opening any screen.

                """);
    }

    @Override
    protected String entryName() {
        return "Effects";
    }

    @Override
    protected String entryDescription() {
        return "Bonuses and penalties tied to each food group.";
    }

    @Override
    protected Pair<Integer, Integer> entryBackground() {
        return EntryBackground.DEFAULT;
    }

    @Override
    protected BookIconModel entryIcon() {
        return BookIconModel.create(Items.POTION);
    }

    @Override
    protected String entryId() {
        return ID;
    }
}
