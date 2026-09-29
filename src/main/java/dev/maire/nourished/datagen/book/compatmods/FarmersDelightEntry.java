package dev.maire.nourished.datagen.book.compatmods;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class FarmersDelightEntry extends EntryProvider {

    public static final String ID = "farmers_delight";

    public FarmersDelightEntry(CategoryProvider parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {

        this.page("farmers_delight", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Farmer's Delight");

        this.pageText("""
                Farmer's Delight is a farming and cooking overhaul that adds crops, cooking tools, and a wide variety of prepared meals. Its biggest impact on Nourished is that many of its cooked dishes cover **multiple food groups at once**, making them incredibly efficient for maintaining all six bars.
                """);

        this.page("new_crops", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("New Crops");

        this.pageText("""
                Farmer's Delight adds several new crops that slot into existing groups:

                **Tomato** — Vegetables; fast-growing and versatile.
                **Onion** — Vegetables; distinct allium family.
                **Rice** — Grains; introduces a new grain family separate from wheat.
                **Cabbage** — Vegetables; leafy green family.
                **Beetroot Seeds** (improved) — better yield than vanilla beetroot.
                """);

        this.page("cooked_meals", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Cooked Meals");

        this.pageText("""
                Farmer's Delight meals are where Nourished integration shines. Many dishes cover two or three groups in one item:

                **Stew / Soup** — typically Vegetables + Grains.
                **Roast Chicken** — Proteins + Vegetables.
                **Bacon and Eggs** — Proteins + Dairy.
                **Rice Bowl** — Grains + Vegetables.
                **Stuffed Pumpkin** — Grains + Vegetables + Proteins.
                """);

        this.page("cooking_pot", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Cooking Pot");

        this.pageText("""
                The **Cooking Pot** is Farmer's Delight's crafting station for meals. Investing in one early pays off immediately for Nourished — cooking a pot of stew that covers three groups is far more inventory-efficient than carrying separate foods for each.

                Prioritize recipes that cover your weakest groups. Check the tooltip of any cooked meal to see which Nourished groups it contributes to.
                """);

        this.page("rotation_advice", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Rotation Advice");

        this.pageText("""
                Farmer's Delight meals each have their own food family, so rotating between different dishes avoids diminishing returns even when multiple dishes share a group.

                A good daily rotation might be: **Roast Chicken** for lunch (Proteins + Veg), **Rice Bowl** for dinner (Grains + Veg), and a dairy item when needed. Three meals can cover every group with zero repetition.
                """);
    }

    @Override
    protected String entryName() {
        return "Farmer's Delight";
    }

    @Override
    protected String entryDescription() {
        return "Multi-group meals that cover several bars in one item.";
    }

    @Override
    protected Pair<Integer, Integer> entryBackground() {
        return EntryBackground.DEFAULT;
    }

    @Override
    protected BookIconModel entryIcon() {
        return BookIconModel.create(Items.COOKED_BEEF);
    }

    @Override
    protected String entryId() {
        return ID;
    }
}
