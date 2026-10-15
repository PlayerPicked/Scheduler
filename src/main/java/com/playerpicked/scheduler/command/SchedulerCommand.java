package com.playerpicked.scheduler.command;

import com.playerpicked.scheduler.Scheduler;
import com.playerpicked.scheduler.command.suggestion.EventSuggestion;
import com.playerpicked.scheduler.data.MenuController;
import com.playerpicked.scheduler.guis.impl.EventEditorMenu;
import com.playerpicked.scheduler.guis.impl.EventsMenu;
import com.playerpicked.scheduler.identifiers.ActionType;
import com.playerpicked.scheduler.identifiers.keys.MessageKeys;
import com.playerpicked.scheduler.model.OneTimeTask;
import com.playerpicked.scheduler.model.ScheduledEvent;
import com.playerpicked.scheduler.utils.CronUtil;
import com.playerpicked.scheduler.utils.EventFormatUtil;
import com.playerpicked.scheduler.utils.TimeParseUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.CommandPlaceholder;
import revxrsal.commands.annotation.Optional;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.annotation.SuggestWith;
import revxrsal.commands.bukkit.annotation.CommandPermission;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Command({"scheduler"})
@SuppressWarnings("unused")
public class SchedulerCommand {

    private final Scheduler plugin = Scheduler.getInstance();

    @CommandPlaceholder
    public void root(@NotNull CommandSender sender) {
        MessageKeys.HELP.sendList(sender);
    }

    @Subcommand("help")
    public void help(@NotNull CommandSender sender) {
        MessageKeys.HELP.sendList(sender);
    }

    @Subcommand("reload")
    @CommandPermission("scheduler.reload")
    public void reload(@NotNull CommandSender sender) {
        plugin.reload();
        MessageKeys.RELOAD.send(sender);
    }

    @Subcommand("editor")
    @CommandPermission("scheduler.editor")
    public void editor(@NotNull Player player, @Optional @SuggestWith(EventSuggestion.class) @Nullable String event) {
        if (event == null) {
            new EventsMenu(MenuController.getMenuUtils(player)).open();
            return;
        }

        plugin.getEventManager().get(event).ifPresentOrElse(
                found -> new EventEditorMenu(MenuController.getMenuUtils(player), found).open(),
                () -> MessageKeys.EVENT_NOT_FOUND.send(player, "{event}", event));
    }

    @Subcommand("list")
    @CommandPermission("scheduler.list")
    public void list(@NotNull CommandSender sender) {
        List<ScheduledEvent> events = plugin.getEventManager().getEvents();

        if (events.isEmpty()) {
            MessageKeys.LIST_EMPTY.send(sender);
            return;
        }

        MessageKeys.LIST_HEADER.send(sender, "{count}", String.valueOf(events.size()));

        for (ScheduledEvent event : events) {
            MessageKeys.LIST_ENTRY.send(sender,
                    "{event}", event.getName(),
                    "{status}", EventFormatUtil.status(event),
                    "{next_run}", CronUtil.formatNextRun(event),
                    "{times}", String.valueOf(event.getTimes().size()),
                    "{actions}", String.valueOf(event.getActions().size()));
        }
    }

    @Subcommand("next")
    @CommandPermission("scheduler.next")
    public void next(@NotNull CommandSender sender) {
        plugin.getEventManager().getEvents().stream()
                .filter(event -> CronUtil.getNextRun(event) > 0)
                .min(Comparator.comparingLong(CronUtil::getNextRun))
                .ifPresentOrElse(
                        event -> MessageKeys.NEXT.send(sender, "{event}", event.getName(), "{next_run}", CronUtil.formatNextRun(event)),
                        () -> MessageKeys.NEXT_NONE.send(sender));
    }

    @Subcommand("run")
    @CommandPermission("scheduler.run")
    public void run(@NotNull CommandSender sender, @SuggestWith(EventSuggestion.class) @NotNull String event) {
        plugin.getEventManager().get(event).ifPresentOrElse(found -> Scheduler.getScheduler().runTask(() -> {
            if (plugin.getSchedulerService().run(found, true)) MessageKeys.EVENT_RUN.send(sender, "{event}", found.getName());
            else MessageKeys.EVENT_RUN_CANCELLED.send(sender, "{event}", found.getName());
        }), () -> MessageKeys.EVENT_NOT_FOUND.send(sender, "{event}", event));
    }

    @Subcommand("once add")
    @CommandPermission("scheduler.once")
    public void onceAdd(@NotNull CommandSender sender, @NotNull String time, @NotNull String actions) {
        long moment = TimeParseUtil.parseMoment(time);

        if (moment < 0) {
            MessageKeys.ONCE_INVALID_TIME.send(sender, "{time}", time);
            return;
        }

        if (moment <= System.currentTimeMillis()) {
            MessageKeys.ONCE_IN_PAST.send(sender, "{time}", time);
            return;
        }

        List<String> parsed = Arrays.stream(actions.split("\\s+\\|\\s+"))
                .map(String::trim)
                .filter(action -> !action.isEmpty())
                .toList();

        for (String action : parsed) {
            ActionType type = ActionType.parse(action);
            if (type != null && type != ActionType.RANDOM) continue;

            MessageKeys.ACTION_INVALID.send(sender, "{action}", action);
            return;
        }

        OneTimeTask task = plugin.getOneTimeService().add(moment, parsed, sender.getName());
        MessageKeys.ONCE_ADDED.send(sender,
                "{id}", String.valueOf(task.id()),
                "{date}", TimeParseUtil.format(task.time()),
                "{remaining}", CronUtil.formatNextRun(task.time()),
                "{actions}", String.valueOf(parsed.size()));
    }

    @Subcommand("once list")
    @CommandPermission("scheduler.once")
    public void onceList(@NotNull CommandSender sender) {
        List<OneTimeTask> tasks = plugin.getOneTimeService().getTasks();

        if (tasks.isEmpty()) {
            MessageKeys.ONCE_LIST_EMPTY.send(sender);
            return;
        }

        MessageKeys.ONCE_LIST_HEADER.send(sender, "{count}", String.valueOf(tasks.size()));

        for (OneTimeTask task : tasks) {
            MessageKeys.ONCE_LIST_ENTRY.send(sender,
                    "{id}", String.valueOf(task.id()),
                    "{date}", TimeParseUtil.format(task.time()),
                    "{remaining}", CronUtil.formatNextRun(task.time()),
                    "{creator}", task.creator(),
                    "{actions}", String.join(" | ", task.actions()));
        }
    }

    @Subcommand("once cancel")
    @CommandPermission("scheduler.once")
    public void onceCancel(@NotNull CommandSender sender, int id) {
        plugin.getOneTimeService().cancel(id).ifPresentOrElse(
                task -> MessageKeys.ONCE_CANCELLED.send(sender, "{id}", String.valueOf(id)),
                () -> MessageKeys.ONCE_NOT_FOUND.send(sender, "{id}", String.valueOf(id)));
    }

    @Subcommand("toggle")
    @CommandPermission("scheduler.toggle")
    public void toggle(@NotNull CommandSender sender, @SuggestWith(EventSuggestion.class) @NotNull String event) {
        plugin.getEventManager().get(event).ifPresentOrElse(found -> {
            found.setEnabled(!found.isEnabled());
            plugin.getEventManager().save(found);

            (found.isEnabled() ? MessageKeys.EVENT_ENABLED : MessageKeys.EVENT_DISABLED).send(sender, "{event}", found.getName());
        }, () -> MessageKeys.EVENT_NOT_FOUND.send(sender, "{event}", event));
    }
}