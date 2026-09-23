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

    // Food group colors
    private static final String FRUITS = "[#](F5A623)";
    private static final String VEGETABLES = "[#](7BC96F)";
    private static final String PROTEINS = "[#](E85D75)";
    private static final String GRAINS = "[#](E6C65C)";
    private static final String DAIRY = "[#](6FB8E8)";

    // HUD colors
    private static final String HUD = "[#](8CCCF0)";
    private static final String EDIT_MODE = "[#](B88CFF)";
    private static final String KEYBIND = "[#](F5A623)";
    private static final String POSITION = "[#](7BC96F)";
    private static final String SAVED = "[#](6FB8E8)";

    // Reset
    private static final String RESET = "[#]()";

    public HudEntry(CategoryProvider parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {

        // ---------------------------------------------------------------------
        // Nutrition HUD
        // ---------------------------------------------------------------------

        this.page("hud_image", () -> BookImagePageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText())
                .withImages(
                        ResourceLocation.fromNamespaceAndPath(
                                "nourished",
                                "textures/images/mini_gui.png"
                        )
                ));

        this.pageTitle("Nutrition HUD");

        this.pageText("""
                The %s**Nutrition HUD**%s displays your current nutrition levels at a glance.

                Each bar represents one of your food groups, making it easy to see which parts of your diet need attention without opening a screen.

                """.formatted(
                HUD,
                RESET
        ));

        // ---------------------------------------------------------------------
        // Edit Mode
        // ---------------------------------------------------------------------

        this.page("edit_mode", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Edit Mode");

        this.pageText("""
                Press %s**H**%s to enter %s**Edit Mode**%s. In edit mode you can drag the HUD to any position on your screen.

                Press %s**H**%s again to lock it back in place. Your %s**position**%s is saved between sessions.

                The HUD can be positioned wherever it is most convenient for you while playing.

                """.formatted(
                KEYBIND, RESET,
                EDIT_MODE, RESET,
                KEYBIND, RESET,
                POSITION, RESET
        ));
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