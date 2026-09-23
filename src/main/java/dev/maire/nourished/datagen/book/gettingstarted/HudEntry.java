package dev.maire.nourished.datagen.book.gettingstarted;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookImagePageModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

public class HudEntry extends EntryProvider {

    public static final String ID = "hud";

    public HudEntry(CategoryProvider parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {
        this.page("hud_image", () -> BookImagePageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText())
                .withImages(ResourceLocation.fromNamespaceAndPath("nourished", "textures/patchouli/mini_gui.png")));
        this.pageTitle("Nutrition HUD");
        this.pageText("The mini HUD displays your current nutrition levels at a glance.");

        this.page("edit_mode", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Edit Mode");
        this.pageText("""
                Press **H** to enter edit mode. In edit mode you can drag the HUD to any position on your screen.

                Press **H** again to lock it back in place. Your position is saved between sessions.
                """);
    }

    @Override
    protected String entryName() {
        return "The HUD";
    }

    @Override
    protected String entryDescription() {
        return "Reading the on-screen nutrition bars.";
    }

    @Override
    protected Pair<Integer, Integer> entryBackground() {
        return EntryBackground.DEFAULT;
    }

    @Override
    protected BookIconModel entryIcon() {
        return BookIconModel.create(Items.FILLED_MAP);
    }

    @Override
    protected String entryId() {
        return ID;
    }
}
