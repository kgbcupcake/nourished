package dev.maire.nourished.datagen.book.compatmods;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class LegendarySurvivalOverhaulEntry extends EntryProvider {

    public static final String ID = "legendary_survival_overhaul";

    private static final String WARNING = "[#](e05252)";
    private static final String RESET = "[#]()";

    public LegendarySurvivalOverhaulEntry(CategoryProvider parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {

        this.page("legendary_survival_overhaul", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Legendary Survival Overhaul");

        this.pageText("""
                Legendary Survival Overhaul (LSO) is a survival mechanics overhaul that manages its own status effect system. Because LSO and Nourished would conflict when both try to apply effects simultaneously, %sNourished's bonus and penalty effects are disabled%s when LSO is installed.
                """.formatted(
                WARNING, RESET
        ));

        this.page("what_still_works", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("What Still Works");

        this.pageText("""
                Even with effects disabled, the following Nourished systems work exactly as normal:

                **All six nutrition bars** — track and decay as usual.
                **Diet Screen** — fully functional.
                **HUD overlay** — fully functional.
                **Food tooltips** — freshness, family, group all shown.
                **Diminishing returns** — still applies to nutrition gain.
                **Sleep Bonus** — still applies if configured.
                """);

        this.page("what_is_disabled", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("What Is Disabled");

        this.pageText("""
                The following are suppressed to avoid conflicts with LSO:

                **Per-group bonuses** — e.g. Proteins max health, Grains XP gain.
                **Per-group penalties** — e.g. Fruits health reduction, Dairy knockback.
                **Balance Bonus** — the combined all-groups buff.

                LSO handles survival effects through its own systems instead. Keeping your bars healthy still matters — it feeds into LSO's own calculations.
                """);

        this.page("why_keep_bars_healthy", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Why Keep Bars Healthy?");

        this.pageText("""
                Even without Nourished's direct effects, your nutrition bars still feed data into LSO's survival systems. A well-balanced diet contributes positively to LSO's stamina, resilience, and recovery mechanics.

                Think of Nourished as the **nutrition tracking layer** and LSO as the **effects layer** — they work together rather than duplicating each other.
                """);
    }

    @Override
    protected String entryName() {
        return "Legendary Survival Overhaul";
    }

    @Override
    protected String entryDescription() {
        return "How Nourished's effects step aside for LSO's own systems.";
    }

    @Override
    protected Pair<Integer, Integer> entryBackground() {
        return EntryBackground.DEFAULT;
    }

    @Override
    protected BookIconModel entryIcon() {
        return BookIconModel.create(Items.SHIELD);
    }

    @Override
    protected String entryId() {
        return ID;
    }
}
