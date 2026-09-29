package dev.maire.nourished.datagen.book.compatmods;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.SingleBookSubProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookCategoryModel;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.condition.BookModLoadedConditionModel;
import com.klikli_dev.modonomicon.book.BookCategoryBackgroundParallaxLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

public class CompatModsCategory extends CategoryProvider {

    public static final String ID = "compat_mods";

    public CompatModsCategory(SingleBookSubProvider parent) {
        super(parent);
    }

    @Override
    protected String[] generateEntryMap() {
        /*
         * The Overview sits at the top; the nine mod entries fan out below
         * it across two staggered rows, each gated on the Overview being
         * read and the relevant mod being installed.
         */
        return new String[]{
                "____o____",
                "_________",
                "c_f_p_l_h",
                "_________",
                "_a_b_s_k_"
        };
    }

    @Override
    protected void generateEntries() {

        // Overview
        var overview = this.add(
                new OverviewEntry(this).generate('o')
        );

        // Croptopia
        this.add(
                new CroptopiaEntry(this).generate('c')
        )
                .withParent(overview)
                .withCondition(this.condition().and(
                        this.condition().entryRead(overview),
                        BookModLoadedConditionModel.create().withModId("croptopia")
                ));

        // Farmer's Delight
        this.add(
                new FarmersDelightEntry(this).generate('f')
        )
                .withParent(overview)
                .withCondition(this.condition().and(
                        this.condition().entryRead(overview),
                        BookModLoadedConditionModel.create().withModId("farmersdelight")
                ));

        // Pam's HarvestCraft 2
        this.add(
                new PamsHarvestcraftEntry(this).generate('p')
        )
                .withParent(overview)
                .withCondition(this.condition().and(
                        this.condition().entryRead(overview),
                        BookModLoadedConditionModel.create().withModId("pamhc2crops")
                ));

        // Legendary Survival Overhaul
        this.add(
                new LegendarySurvivalOverhaulEntry(this).generate('l')
        )
                .withParent(overview)
                .withCondition(this.condition().and(
                        this.condition().entryRead(overview),
                        BookModLoadedConditionModel.create().withModId("legendarysurvivaloverhaul")
                ));

        // Herbs & Harvest
        this.add(
                new HerbsAndHarvestEntry(this).generate('h')
        )
                .withParent(overview)
                .withCondition(this.condition().and(
                        this.condition().entryRead(overview),
                        BookModLoadedConditionModel.create().withModId("herbsandharvest")
                ));

        // Farm & Charm
        this.add(
                new FarmAndCharmEntry(this).generate('a')
        )
                .withParent(overview)
                .withCondition(this.condition().and(
                        this.condition().entryRead(overview),
                        BookModLoadedConditionModel.create().withModId("farm_and_charm")
                ));

        // Butchery
        this.add(
                new ButcheryEntry(this).generate('b')
        )
                .withParent(overview)
                .withCondition(this.condition().and(
                        this.condition().entryRead(overview),
                        BookModLoadedConditionModel.create().withModId("butchery")
                ));

        // Spice of Life: Onion
        this.add(
                new SpiceOfLifeEntry(this).generate('s')
        )
                .withParent(overview)
                .withCondition(this.condition().and(
                        this.condition().entryRead(overview),
                        BookModLoadedConditionModel.create().withModId("spiceoflife")
                ));

        // Peak Stamina
        this.add(
                new PeakStaminaEntry(this).generate('k')
        )
                .withParent(overview)
                .withCondition(this.condition().and(
                        this.condition().entryRead(overview),
                        BookModLoadedConditionModel.create().withModId("peakstamina")
                ));
    }

    @Override
    protected BookCategoryModel additionalSetup(BookCategoryModel category) {
        return category.withBackgroundParallaxLayers(
                new BookCategoryBackgroundParallaxLayer(
                        ResourceLocation.parse(
                                "modonomicon:textures/gui/parallax/flow/base.png"
                        ),
                        0.7f,
                        -1
                ),
                new BookCategoryBackgroundParallaxLayer(
                        ResourceLocation.parse(
                                "modonomicon:textures/gui/parallax/flow/1.png"
                        ),
                        1.0f,
                        -1
                ),
                new BookCategoryBackgroundParallaxLayer(
                        ResourceLocation.parse(
                                "modonomicon:textures/gui/parallax/flow/2.png"
                        ),
                        1.4f,
                        -1
                )
        );
    }

    @Override
    protected String categoryName() {
        return "Compat Mods";
    }

    @Override
    protected String categoryDescription() {
        return "How popular food mods integrate with Nourished and what they add to each food group.";
    }

    @Override
    protected BookIconModel categoryIcon() {
        return BookIconModel.create(Items.CRAFTING_TABLE);
    }

    @Override
    public String categoryId() {
        return ID;
    }
}
