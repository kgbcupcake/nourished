package dev.maire.nourished.datagen.book.compatmods;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class ButcheryEntry extends EntryProvider {

    public static final String ID = "butchery";

    public ButcheryEntry(CategoryProvider parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {

        this.page("butchery", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Butchery");

        this.pageText("""
                Butchery is a meat processing mod that adds detailed cuts, organs, and exotic meats from nearly every mob in the game. Every Butchery item is fully classified as **Proteins** in Nourished, making it the single largest expansion to that food group in any supported mod.
                """);

        this.page("conventional_cuts", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Conventional Cuts");

        this.pageText("""
                Butchery adds proper butcher cuts from vanilla animals: ribeye, sirloin, T-bone, rump steak, chuck steak, pork belly, pork loin, lamb loin, lamb rib, leg of lamb, chicken leg, chicken wing, ham, sausage, and more.\s\s
                Each cut is a distinct protein family, meaning rotating through different cuts of the same animal still gives better nutrition than eating the same cut repeatedly.
                """);

        this.page("organs_and_offal", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Organs & Offal");

        this.pageText("""
                Butchery adds organ meats including heart, liver, kidney, lungs, stomach, and intestines, all classified as Proteins.\s\s
                Organ meats are distinct families from muscle cuts, so mixing organs into your protein rotation alongside steaks and chops gives excellent variety and keeps diminishing returns low.
                """);

        this.page("exotic_meats", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Exotic Meats");

        this.pageText("""
                Butchery lets you process almost any mob, including hostile and boss mobs. These exotic meats are all classified as Proteins:\s\s
                - **Cooked Enderman Steak / Liver / Kidney**: hard to obtain but very high nutrition.
                - **Cooked Warden Meat**: extremely rare; treat as a luxury protein.
                - **Cooked Dragon Meat**: the ultimate exotic protein source.
                - **Cooked Creeper, Spider, Shulker**: common hostile mob proteins.
                """);

        this.page("rotation_strategy", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Rotation Strategy");

        this.pageText("""
                With Butchery installed, the Proteins group has more variety than any other group. The risk is **defaulting to one favourite cut** and losing the benefit of all that variety.\s\s
                Aim to rotate across at least three distinct families per day, for example a lamb cut, a fish fillet, and an organ meat. Use JEI and search **nourished:nutrients/proteins** to see the full list of what's available.
                """);
    }

    @Override
    protected String entryName() {
        return "Butchery";
    }

    @Override
    protected String entryDescription() {
        return "Detailed cuts, organs, and exotic meats for Proteins.";
    }

    @Override
    protected Pair<Integer, Integer> entryBackground() {
        return EntryBackground.DEFAULT;
    }

    @Override
    protected BookIconModel entryIcon() {
        return BookIconModel.create(Items.COOKED_PORKCHOP);
    }

    @Override
    protected String entryId() {
        return ID;
    }
}
