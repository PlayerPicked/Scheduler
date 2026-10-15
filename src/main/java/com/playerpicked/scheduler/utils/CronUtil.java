package com.playerpicked.scheduler.utils;

import com.playerpicked.scheduler.identifiers.keys.ConfigKeys;
import com.playerpicked.scheduler.identifiers.keys.MessageKeys;
import com.playerpicked.scheduler.model.ScheduledEvent;
import it.sauronsoftware.cron4j.Predictor;
import it.sauronsoftware.cron4j.SchedulingPattern;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.TimeZone;

@UtilityClass
public class CronUtil {

    public boolean isValid(@NotNull String pattern) {
        return SchedulingPattern.validate(pattern.trim());
    }

    @NotNull
    public TimeZone getTimeZone() {
        return TimeZone.getTimeZone(ConfigKeys.TIMEZONE.getRawString("Europe/London"));
    }

    public long getNextRun(@NotNull String pattern) {
        if (!isValid(pattern)) return -1L;

        Predictor predictor = new Predictor(pattern.trim());
        predictor.setTimeZone(getTimeZone());
        return predictor.nextMatchingTime();
    }

    public long getNextRun(@NotNull ScheduledEvent event) {
        if (!event.isEnabled()) return -1L;

        long next = Long.MAX_VALUE;
        for (String time : event.getTimes()) {
            long run = getNextRun(time);
            if (run > 0) next = Math.min(next, run);
        }

        long now = System.currentTimeMillis();
        for (LocalDateTime date : event.getDates()) {
            long run = TimeParseUtil.toMillis(date);
            if (run > now) next = Math.min(next, run);
        }

        return next == Long.MAX_VALUE ? -1L : next;
    }

    @NotNull
    public String formatNextRun(long nextRun) {
        if (nextRun <= 0) return MessageKeys.NEXT_RUN_NEVER.getMessage();
        return TimeFormatUtil.format(Math.max(0L, nextRun - System.currentTimeMillis()));
    }

    @NotNull
    public String formatNextRun(@NotNull ScheduledEvent event) {
        return formatNextRun(getNextRun(event));
    }

    @NotNull
    public String formatNextRun(@NotNull String pattern) {
        return formatNextRun(getNextRun(pattern));
    }
}