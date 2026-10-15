package com.playerpicked.scheduler.hooks.impl.placeholder;

import com.playerpicked.scheduler.Scheduler;
import com.playerpicked.scheduler.model.ScheduledEvent;
import com.playerpicked.scheduler.processor.MessageProcessor;
import com.playerpicked.scheduler.utils.CronUtil;
import com.playerpicked.scheduler.utils.EventFormatUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Optional;

@SuppressWarnings("deprecation")
public final class PlaceholderHook extends PlaceholderExpansion {
    private final Scheduler plugin = Scheduler.getInstance();

    @Override
    public @NotNull String getIdentifier() {
        return "scheduler";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(@Nullable OfflinePlayer player, @NotNull String params) {
        if (params.equalsIgnoreCase("next_event")) return nextEvent().map(ScheduledEvent::getName).orElse(none());
        if (params.equalsIgnoreCase("next_event_time")) return nextEvent().map(CronUtil::formatNextRun).orElse(none());

        if (params.startsWith("nextrun_seconds_")) {
            return event(params, "nextrun_seconds_")
                    .map(CronUtil::getNextRun)
                    .map(next -> String.valueOf(next <= 0 ? -1 : (next - System.currentTimeMillis()) / 1000L))
                    .orElse("-1");
        }

        if (params.startsWith("nextrun_")) return event(params, "nextrun_").map(CronUtil::formatNextRun).orElse(none());
        if (params.startsWith("lastrun_")) return event(params, "lastrun_").map(EventFormatUtil::lastRun).orElse(none());
        if (params.startsWith("enabled_")) return event(params, "enabled_").map(event -> String.valueOf(event.isEnabled())).orElse("false");
        if (params.startsWith("times_")) return event(params, "times_").map(event -> String.valueOf(event.getTimes().size())).orElse("0");

        return null;
    }

    @NotNull
    private Optional<ScheduledEvent> event(@NotNull String params, @NotNull String prefix) {
        return plugin.getEventManager().get(params.substring(prefix.length()));
    }

    @NotNull
    private Optional<ScheduledEvent> nextEvent() {
        return plugin.getEventManager().getEvents().stream()
                .filter(event -> CronUtil.getNextRun(event) > 0)
                .min(Comparator.comparingLong(CronUtil::getNextRun));
    }

    @NotNull
    private String none() {
        return MessageProcessor.process(plugin.getHooks().getString("hooks.placeholderapi.none", "---"));
    }
}