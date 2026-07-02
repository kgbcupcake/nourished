package dev.maire.nourished.command;

import com.mojang.brigadier.context.CommandContext;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.tagaudit.NourishedTagAuditContext;
import dev.maire.nourished.core.tagaudit.NourishedTagAuditReportWriter;
import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.tagaudit.TagScanner;
import dev.marie.framework.tagaudit.model.TagReport;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;

@ApiStatus.Internal
public final class NourishedTagAuditCommand {

    private NourishedTagAuditCommand() {}

    public static int run(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        TagReport report = TagScanner.scan(NourishedTagAuditContext.get());
        Path reportPath = NourishedTagAuditReportWriter.write(report);
        if (reportPath != null) {
            source.sendSuccess(() -> Component.literal(
                            "Tag audit report written to " + reportPath.toAbsolutePath() + ".")
                    .withStyle(ChatFormatting.GREEN), false);
            return 1;
        }
        source.sendFailure(Component.literal("Failed to write tag audit report."));
        return 0;
    }
}
