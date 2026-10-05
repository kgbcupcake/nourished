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

public class DynamicUiEntry extends EntryProvider {

    public static final String ID = "dynamic_ui";

    private static final String EDIT_MODE = "[#](B88CFF)";
    private static final String KEYBIND = "[#](F5A623)";
    private static final String TAB = "[#](8CCCF0)";
    private static final String MODULE = "[#](7BC96F)";

    private static final String RESET = "[#]()";

    public DynamicUiEntry(CategoryProvider parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {

        // ---------------------------------------------------------------------
        // Dynamic HUD
        // ---------------------------------------------------------------------

        this.page("dynamic_hud_image", () -> BookImagePageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText())
                .withImages(
                        ResourceLocation.fromNamespaceAndPath(
                                "nourished",
                                "textures/images/dynamic_hud.png"
                        )
                ));

        this.pageTitle("Dynamic HUD");

        this.pageText("""
                Opening %sEdit Mode%s on the HUD shows this options panel, with a tab for every part of its look and feel.

                """.formatted(
                EDIT_MODE,
                RESET
        ));

        this.page("hud_customization", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Tabs for Everything");

        this.pageText("""
                %sLayout%s covers padding and orientation. %sBehavior%s controls what triggers the HUD to show or hide. %sStyle%s, %sGlow%s, and %sPulse%s shape its look: background, borders, outlines, and an animated pulse on critical bars. %sColors%s recolors anything on it, bar by bar.\s\s
                Every setting has its own %sReset%s, so experimenting never risks losing your other changes.
                """.formatted(
                TAB, RESET, TAB, RESET,
                TAB, RESET, TAB, RESET, TAB, RESET,
                TAB, RESET,
                KEYBIND, RESET
        ));

        // ---------------------------------------------------------------------
        // Dynamic Diet Screen
        // ---------------------------------------------------------------------

        this.page("dynamic_diet_image", () -> BookImagePageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText())
                .withImages(
                        ResourceLocation.fromNamespaceAndPath(
                                "nourished",
                                "textures/images/dynamic_diet_screen.png"
                        )
                ));

        this.pageTitle("Dynamic Diet Screen");

        this.pageText("""
                The Diet Screen is built from independent modules: %sCalories%s, %sBalance%s, %sRecent Meals%s, %sEat More%s, %sActive Effects%s, and the %sIntake Breakdown%s rows.

                """.formatted(
                MODULE, RESET, MODULE, RESET, MODULE, RESET,
                MODULE, RESET, MODULE, RESET, MODULE, RESET
        ));

        this.page("diet_customization", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Every Box, Your Way");

        this.pageText("""
                Each module in the editor's sidebar gets the same %sLayout%s, %sBehavior%s, %sStyle%s, %sGlow%s, %sPulse%s, and %sColors%s tabs the HUD has, so every box on the screen can be dragged, resized, recolored, and restyled on its own.\s\s
                Drag a box anywhere on screen, or leave everything at its default layout. Either way works.
                """.formatted(
                TAB, RESET, TAB, RESET, TAB, RESET,
                TAB, RESET, TAB, RESET, TAB, RESET
        ));
    }

    @Override
    protected String entryName() {
        return "Dynamic UI";
    }

    @Override
    protected String entryDescription() {
        return "Every HUD bar and Diet Screen box can be moved, resized, and restyled.";
    }

    @Override
    protected Pair<Integer, Integer> entryBackground() {
        return EntryBackground.DEFAULT;
    }

    @Override
    protected BookIconModel entryIcon() {
        return BookIconModel.create(Items.PAINTING);
    }

    @Override
    protected String entryId() {
        return ID;
    }
}
