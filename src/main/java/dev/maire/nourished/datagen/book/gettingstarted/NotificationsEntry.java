package dev.maire.nourished.datagen.book.gettingstarted;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class NotificationsEntry extends EntryProvider {

    public static final String ID = "notifications";

    public NotificationsEntry(CategoryProvider parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {
        this.page("critical_toasts", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Critical Toasts");
        this.pageText("""
                When a food group drops critically low, Nourished sends a toast notification in the top-right corner of your screen.

                Toasts appear once per group when they cross the critical threshold,  pay attention to them or your stats will suffer.
                """);

        this.page("staying_ahead", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Staying Ahead");
        this.pageText("""
                You can disable notifications in the mod's config if you prefer to manage nutrition manually.

                The HUD mini bars will still change color as groups reach critical levels, giving you a passive visual warning at all times.
                """);
    }

    @Override
    protected String entryName() {
        return "Notifications";
    }

    @Override
    protected String entryDescription() {
        return "Toast warnings when a food group runs critically low.";
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
