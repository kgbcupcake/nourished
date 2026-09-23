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

public class NotificationsEntry extends EntryProvider {

    public static final String ID = "notifications";

    // Notification colors
    private static final String CRITICAL = "[#](E85D75)";
    private static final String TOAST = "[#](F5A623)";
    private static final String HUD = "[#](6FB8E8)";
    private static final String CONFIG = "[#](B88CFF)";
    private static final String CALORIES = "[#](7BC96F)";

    // Reset
    private static final String RESET = "[#]()";

    public NotificationsEntry(CategoryProvider parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {
        this.page("notification_image", () -> BookImagePageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText())
                .withImages(
                        ResourceLocation.fromNamespaceAndPath(
                                "nourished",
                                "textures/images/notification.png"
                        )
                ));

        this.pageTitle("Notifications");

        this.pageText("""
                Nourished uses %s**notifications**%s to warn you when your nutrition needs attention.

                """.formatted(
                TOAST,
                RESET
        ));

        this.page("critical_toasts", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Critical Toasts");

        this.pageText("""
                When a food group drops %s**critically low**%s, Nourished sends a %s**toast notification**%s in the top-right corner of your screen.

                Toasts appear once per group when they cross the critical threshold. Pay attention to them, or your stats will suffer.

                """.formatted(
                CRITICAL, RESET,
                TOAST, RESET
        ));

        this.page("staying_ahead", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Staying Ahead");

        this.pageText("""
                You can disable %s**notifications**%s in the mod's config if you prefer to manage nutrition manually.

                The %s**HUD mini bars**%s will still change color as groups reach critical levels, giving you a passive visual warning at all times.

                """.formatted(
                CONFIG, RESET,
                HUD, RESET
        ));

        this.page("food_eaten", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Food Eaten");

        this.pageText("""
                Every time you eat, Nourished also shows a small %s**food eaten**%s notification with the item's name and how many %s**calories**%s it added.

                Eating the same food again quickly merges into the same notification instead of stacking a new one each bite — it just updates in place.

                """.formatted(
                TOAST, RESET,
                CALORIES, RESET
        ));
    }

    @Override
    protected String entryName() {
        return "Notifications";
    }

    @Override
    protected String entryDescription() {
        return "Toast warnings and food-eaten calorie notifications.";
    }

    @Override
    protected Pair<Integer, Integer> entryBackground() {
        return EntryBackground.DEFAULT;
    }

    @Override
    protected BookIconModel entryIcon() {
        return BookIconModel.create(Items.BELL);
    }

    @Override
    protected String entryId() {
        return ID;
    }
}