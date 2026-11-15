package dev.maire.nourished.datagen.book.gettingstarted;

import com.klikli_dev.modonomicon.api.datagen.SingleBookSubProvider;
import dev.maire.nourished.datagen.book.GuideCategory;
import dev.maire.nourished.datagen.book.GuideColors;
import net.minecraft.world.item.Items;

public class GettingStartedCategory extends GuideCategory {

    public static final String ID = GuideColors.GETTING_STARTED;

    public GettingStartedCategory(SingleBookSubProvider parent) {
        super(parent, ID, "Getting Started", "A guided introduction. Read these in order.", Items.BOOK);
    }

    @Override
    protected String[] generateEntryMap() {
        return new String[]{
                "i_______",
                "________",
                "__h___d_",
                "________",
                "_____t__",
                "________",
                "__v_____",
                "________",
                "______n_",
                "________",
                "___s____",
                "________",
                "_____y__",
                "________",
                "_f__r__m"
        };
    }

    @Override
    protected void generateEntries() {
        var intro = this.add(new IntroductionEntry(this).generate('i'));
        var hud = this.add(after(intro, new HudEntry(this).generate('h')));
        var diet = this.add(after(hud, new DietScreenEntry(this).generate('d')));
        var tooltips = this.add(after(diet, new FoodTooltipsEntry(this).generate('t')));
        var variety = this.add(after(tooltips, new EatVarietyEntry(this).generate('v')));
        var notifications = this.add(after(variety, new NotificationsEntry(this).generate('n')));
        var sleep = this.add(after(notifications, new SleepBonusEntry(this).generate('s')));
        var firstDay = this.add(after(sleep, new FirstDayEntry(this).generate('y')));
        this.add(after(firstDay, new FirstFarmEntry(this).generate('f')));
        this.add(after(firstDay, new DailyRoutineEntry(this).generate('r')));
        this.add(after(firstDay, new MultiplayerAdventureEntry(this).generate('m')));
    }
}
