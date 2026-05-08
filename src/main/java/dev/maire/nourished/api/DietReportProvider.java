package dev.maire.nourished.api;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * Extension point allowing other mods to inject custom sections into the
 * {@code /nourished} command's diet report output.
 *
 * <p>Implementations are registered via {@link NourishedAPI#registerReportProvider(DietReportProvider)}
 * and will be invoked in registration order when the report is generated.</p>
 */
@ApiStatus.Experimental
public interface DietReportProvider {

    /**
     * Returns a unique identifier for this report section, used for ordering
     * and deduplication.
     *
     * @return a non-null, unique string identifier (e.g. "mymod:fitness_score")
     */
    String getSectionId();

    /**
     * Returns the display title for this report section, shown as a header
     * in the command output.
     *
     * @return a translatable or literal {@link Component} representing the section header
     */
    Component getSectionTitle();

    /**
     * Generates the report lines for the given player. Each component in the
     * returned list represents one line of output in the report.
     *
     * @param player the player whose diet report is being generated
     * @return an ordered list of {@link Component} lines to display; may be empty but not null
     */
    List<Component> generateReport(Player player);
}
