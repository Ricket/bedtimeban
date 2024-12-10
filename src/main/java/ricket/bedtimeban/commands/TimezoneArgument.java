package ricket.bedtimeban.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TimezoneArgument implements ArgumentType<ZoneId> {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String KEY = "timezone";

    private static final SimpleCommandExceptionType ERROR_TIMEZONE_NOT_FOUND = new SimpleCommandExceptionType(Component.translatable("commands.bedtimeban.timezone.notfound"));

    public static TimezoneArgument timezone()
    {
        return new TimezoneArgument();
    }

    @Override
    public ZoneId parse(StringReader reader) throws CommandSyntaxException {
        String userEnteredTimezone = reader.getString();
        try {
            return ZoneId.of(userEnteredTimezone);
        } catch (Exception e) {
            LOGGER.debug("Failed to lookup ZoneId {}", userEnteredTimezone, e);
            throw ERROR_TIMEZONE_NOT_FOUND.create();
        }
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(ZoneId.getAvailableZoneIds(), builder);
    }

    @Override
    public Collection<String> getExamples() {
        return List.of(
                "US/Pacific",
                "America/Chicago",
                "EDT"
        );
    }
}
