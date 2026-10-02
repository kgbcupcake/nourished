package dev.maire.nourished.command;

import com.mojang.brigadier.context.CommandContext;
import dev.maire.nourished.api.impl.RegistrationPhase;
import dev.maire.nourished.core.nutrition.FoodNutritionRegistry;
import dev.maire.nourished.core.nutrition.NutrientClassificationLookup;
import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.registry.MilestoneRegistry;
import dev.marie.framework.runtime.SourceRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;

/** In-game checks for NourishedRegisterEvent and explicit-vs-inferred food mappings. */
@ApiStatus.Internal
public final class NourishedRegistrationDebugCommand {

    private NourishedRegistrationDebugCommand() {}

    public static int report(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        RegistrationPhase.Report report = RegistrationPhase.lastReport();
        say(source, "[NourishedRegisterEvent]", ChatFormatting.GOLD);
        if (report.applied().isEmpty() && report.rejected().isEmpty()) {
            say(source, "No mod registered anything through the event this launch.", ChatFormatting.GRAY);
        }
        list(source, "Applied", report.applied(), ChatFormatting.GREEN);
        list(source, "Rejected", report.rejected(), ChatFormatting.RED);
        say(source, "Milestones registered now: " + MilestoneRegistry.getAll().size(), ChatFormatting.AQUA);
        return 1;
    }

    public static int food(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Run this as a player holding a food item."));
            return 0;
        }
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            source.sendFailure(Component.literal("Hold an item in your main hand."));
            return 0;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        boolean explicit = SourceRegistry.hasAuthoritativeClassification(id);
        Map<String, Float> mapped = SourceRegistry.getExternalClassification(id);
        Map<String, Float> tags = FoodNutritionRegistry.getNutrientTagScores(stack.getItem());

        say(source, "[Food mapping] " + id, ChatFormatting.GOLD);
        say(source, (explicit ? "Explicit (API) mapping: " : "Scanner mapping: ") + format(mapped),
                explicit ? ChatFormatting.GREEN : ChatFormatting.WHITE);
        say(source, "Tag inference: " + format(tags), ChatFormatting.WHITE);
        say(source, "Eating gives: " + format(NutrientClassificationLookup.resolveNutrientBars(stack, false, player.level()))
                + (explicit ? "  (explicit mapping wins, tags ignored)" : ""), ChatFormatting.AQUA);
        return 1;
    }

    private static String format(Map<String, Float> values) {
        return values == null || values.isEmpty() ? "none" : values.toString();
    }

    private static void list(CommandSourceStack source, String title, List<String> lines, ChatFormatting color) {
        if (lines.isEmpty()) {
            return;
        }
        say(source, title + " (" + lines.size() + "):", color);
        lines.forEach(line -> say(source, "  " + line, color));
    }

    private static void say(CommandSourceStack source, String text, ChatFormatting color) {
        source.sendSuccess(() -> Component.literal(text).withStyle(color), false);
    }
}
