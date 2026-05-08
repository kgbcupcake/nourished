package dev.maire.nourished.example;

import dev.maire.nourished.api.DietReportProvider;
import dev.maire.nourished.api.NourishedAPI;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Locale;

/**
 * Demonstrates custom report section injection into the `/nourished report` output.
 */
public final class ExampleReportProvider implements DietReportProvider {

    @Override
    public String getSectionId() {
        return "nourished_example:fiber_status";
    }

    @Override
    public Component getSectionTitle() {
        return Component.literal("Fiber Status");
    }

    @Override
    public List<Component> generateReport(Player player) {
        float fiber = NourishedAPI.getNutrientLevel(player, "fiber");
        String text = String.format(Locale.ROOT, "Current fiber level: %.2f (%d%%)", fiber, Math.round(fiber * 100f));
        return List.of(Component.literal(text));
    }
}
