package com.playerpicked.scheduler.utils;

import com.playerpicked.scheduler.Scheduler;
import com.playerpicked.scheduler.identifiers.keys.MessageKeys;
import com.playerpicked.scheduler.model.ScheduledEvent;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class EventFormatUtil {

    @NotNull
    public String[] placeholders(@NotNull ScheduledEvent event) {
        return new String[]{
                "{event}", event.getName(),
                "{status}", status(event),
                "{next_run}", CronUtil.formatNextRun(event),
                "{times}", String.valueOf(event.getTimes().size()),
                "{actions}", String.valueOf(event.getActions().size()),
                "{not_enough_players_actions}", String.valueOf(event.getNotEnoughPlayersActions().size()),
                "{required_players}", requiredPlayers(event),
                "{last_run}", lastRun(event)
        };
    }

    @NotNull
    public String lastRun(@NotNull ScheduledEvent event) {
        long lastRun = Scheduler.getInstance().getDataManager().getLastRun(event.getName());
        if (lastRun <= 0) return MessageKeys.LAST_RUN_NEVER.getMessage();

        return MessageKeys.LAST_RUN_FORMAT.getMessage("{time}", TimeFormatUtil.format(Math.max(0L, System.currentTimeMillis() - lastRun)));
    }

    @NotNull
    public String status(@NotNull ScheduledEvent event) {
        return event.isEnabled() ? MessageKeys.STATUS_ENABLED.getMessage() : MessageKeys.STATUS_DISABLED.getMessage();
    }

    @NotNull
    public String requiredPlayers(@NotNull ScheduledEvent event) {
        return event.hasPlayerRequirement() ? String.valueOf(event.getRequiredPlayers()) : MessageKeys.REQUIRED_PLAYERS_NONE.getMessage();
    }
}