package com.playerpicked.scheduler.identifiers.keys;

import com.playerpicked.scheduler.Scheduler;
import com.playerpicked.scheduler.config.Config;
import com.playerpicked.scheduler.processor.MessageProcessor;
import lombok.Getter;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

@Getter
public enum MessageKeys {
    RELOAD("messages.reload"),
    UPDATE_AVAILABLE("messages.update-available"),
    NO_PERMISSION("messages.no-permission"),
    PLAYER_REQUIRED("messages.player-required"),
    MISSING_ARGUMENT("messages.missing-argument"),
    INPUT_CANCELLED("messages.input-cancelled"),
    HELP("messages.help"),

    EVENT_NOT_FOUND("messages.event.not-found"),
    EVENT_CREATED("messages.event.created"),
    EVENT_DELETED("messages.event.deleted"),
    EVENT_ALREADY_EXISTS("messages.event.already-exists"),
    EVENT_INVALID_NAME("messages.event.invalid-name"),
    EVENT_ENABLED("messages.event.enabled"),
    EVENT_DISABLED("messages.event.disabled"),
    EVENT_RUN("messages.event.run"),
    EVENT_RUN_CANCELLED("messages.event.run-cancelled"),
    EVENT_REQUIRED_PLAYERS_SET("messages.event.required-players-set"),
    EVENT_REQUIRED_PLAYERS_REMOVED("messages.event.required-players-removed"),
    EVENT_INVALID_NUMBER("messages.event.invalid-number"),

    TIME_ADDED("messages.time.added"),
    TIME_EDITED("messages.time.edited"),
    TIME_DELETED("messages.time.deleted"),
    TIME_INVALID("messages.time.invalid"),
    TIME_ALREADY_EXISTS("messages.time.already-exists"),

    ACTION_ADDED("messages.action.added"),
    ACTION_EDITED("messages.action.edited"),
    ACTION_DELETED("messages.action.deleted"),
    ACTION_INVALID("messages.action.invalid"),

    LIST_HEADER("messages.list.header"),
    LIST_ENTRY("messages.list.entry"),
    LIST_EMPTY("messages.list.empty"),

    ONCE_ADDED("messages.once.added"),
    ONCE_INVALID_TIME("messages.once.invalid-time"),
    ONCE_IN_PAST("messages.once.in-past"),
    ONCE_CANCELLED("messages.once.cancelled"),
    ONCE_NOT_FOUND("messages.once.not-found"),
    ONCE_LIST_HEADER("messages.once.list.header"),
    ONCE_LIST_ENTRY("messages.once.list.entry"),
    ONCE_LIST_EMPTY("messages.once.list.empty"),

    NEXT("messages.next"),
    NEXT_NONE("messages.next-none"),

    STATUS_ENABLED("messages.status.enabled"),
    STATUS_DISABLED("messages.status.disabled"),
    REQUIRED_PLAYERS_NONE("messages.required-players-none"),
    NEXT_RUN_NEVER("messages.next-run-never"),
    LAST_RUN_NEVER("messages.last-run-never"),
    LAST_RUN_FORMAT("messages.last-run-format"),

    TIME_DAY("messages.time-units.day"),
    TIME_DAY_PLURAL("messages.time-units.day-plural"),
    TIME_HOUR("messages.time-units.hour"),
    TIME_HOUR_PLURAL("messages.time-units.hour-plural"),
    TIME_MINUTE("messages.time-units.minute"),
    TIME_MINUTE_PLURAL("messages.time-units.minute-plural"),
    TIME_SECOND("messages.time-units.second"),
    TIME_SECOND_PLURAL("messages.time-units.second-plural"),

    TIME_DAY_SHORT("messages.time-units.short.day"),
    TIME_HOUR_SHORT("messages.time-units.short.hour"),
    TIME_MINUTE_SHORT("messages.time-units.short.minute"),
    TIME_SECOND_SHORT("messages.time-units.short.second");

    private final String path;
    private static final Config config = Scheduler.getInstance().getLanguage();

    MessageKeys(@NotNull String path) {
        this.path = path;
    }

    public @NotNull String getMessage() {
        return MessageProcessor.process(config.getString(path))
                .replace("%prefix%", MessageProcessor.process(config.getString("prefix")));
    }

    public @NotNull String getMessage(@NotNull String... replacements) {
        return replace(getMessage(), replacements);
    }

    @NotNull
    public @Unmodifiable List<String> getMessages(@NotNull String... replacements) {
        String prefix = MessageProcessor.process(config.getString("prefix"));

        return config.getStringList(path)
                .stream()
                .map(MessageProcessor::process)
                .map(line -> replace(line.replace("%prefix%", prefix), replacements))
                .toList();
    }

    public void send(@NotNull CommandSender sender, @NotNull String... replacements) {
        String message = getMessage(replacements);
        if (message.isEmpty()) return;

        sender.sendMessage(message);
    }

    public void sendList(@NotNull CommandSender sender, @NotNull String... replacements) {
        getMessages(replacements).forEach(sender::sendMessage);
    }

    @NotNull
    private static String replace(@NotNull String message, @NotNull String... replacements) {
        for (int index = 0; index + 1 < replacements.length; index += 2) message = message.replace(replacements[index], replacements[index + 1]);
        return message;
    }
}