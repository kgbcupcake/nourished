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

    public EffectsEntry(CategoryProvider parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {
        this.page("balance_bonus", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Balance Bonus");
        this.pageText("""
                When all of the food groups are above their threshold, you gain the Balance Bonus, a persistent buff that improves health, speed, or other stats depending on your configuration.

                Keeping the bonus active should be your primary goal.
                """);

        this.page("per_group_bonuses", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Per-Group Bonuses");
        this.pageText("""
                Each food group has its own bonus active when healthy:

                **Fruits**: increased regeneration speed.
                **Vegetables**: improved hunger saturation.
                **Proteins**: increased max health.
                **Grains**: faster experience gain.
                **Dairy**: increased armor toughness.
                """);

        this.page("penalties", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Penalties");
        this.pageText("""
                Letting any group fall to zero applies a penalty for that group. Penalties stack: neglecting multiple groups at once causes increasingly severe debuffs.

                Recovery is straightforward: eat foods from the depleted group and the penalty fades as the bar refills.
                """);

        this.page("per_group_penalties", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Per-Group Penalties");
        this.pageText("""
                Each food group has its own penalty when depleted:

                **Fruits**: reduced max health.
                **Vegetables**: reduced movement speed.
                **Proteins**: reduced attack damage.
                **Grains**: reduced hunger restoration.
                **Dairy**: reduced knockback resistance.
                """);

        this.page("stacking_effects", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Stacking Effects");
        this.pageText("""
                Effects from multiple groups stack. Having all six bonuses active simultaneously gives a compound advantage across health, damage, speed, regen, XP, and armor.

                Conversely, multiple penalties at once — especially Fruits (health) + Proteins (damage) + Dairy (knockback),  can make combat very dangerous. Prioritize preventing that combination above all else.
                """);

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
