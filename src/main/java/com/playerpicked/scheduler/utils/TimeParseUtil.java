package com.playerpicked.scheduler.utils;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class TimeParseUtil {
    private final Pattern DURATION_PART = Pattern.compile("(\\d+)([dhms])");
    private final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @NotNull
    public ZoneId zone() {
        return CronUtil.getTimeZone().toZoneId();
    }

    @Nullable
    public LocalDateTime parseDateTime(@Nullable String raw) {
        if (raw == null || raw.isBlank()) return null;

        String normalized = raw.trim().replace('@', ' ').replace('T', ' ');

        try {
            if (normalized.length() == 10) return LocalDate.parse(normalized).atStartOfDay();
            return LocalDateTime.parse(normalized, DATE_TIME);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    public long parseDuration(@NotNull String raw) {
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty()) return -1L;

        Matcher matcher = DURATION_PART.matcher(value);
        long total = 0L;
        int end = 0;

        while (matcher.find()) {
            if (matcher.start() != end) return -1L;
            end = matcher.end();

            long amount = Long.parseLong(matcher.group(1));
            total += switch (matcher.group(2)) {
                case "d" -> amount * 86_400_000L;
                case "h" -> amount * 3_600_000L;
                case "m" -> amount * 60_000L;
                default -> amount * 1_000L;
            };
        }

        return end == value.length() ? total : -1L;
    }

    public long parseMoment(@NotNull String raw) {
        long duration = parseDuration(raw);
        if (duration > 0) return System.currentTimeMillis() + duration;

        ZoneId zone = zone();

        try {
            LocalTime time = LocalTime.parse(raw.trim());
            LocalDateTime today = LocalDate.now(zone).atTime(time);
            LocalDateTime moment = today.isAfter(LocalDateTime.now(zone)) ? today : today.plusDays(1);
            return toMillis(moment);
        } catch (DateTimeParseException ignored) {}

        LocalDateTime dateTime = parseDateTime(raw);
        return dateTime == null ? -1L : toMillis(dateTime);
    }

    public long toMillis(@NotNull LocalDateTime dateTime) {
        return dateTime.atZone(zone()).toInstant().toEpochMilli();
    }

    @NotNull
    public String format(long millis) {
        return DISPLAY.format(Instant.ofEpochMilli(millis).atZone(zone()));
    }
}