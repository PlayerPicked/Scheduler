package com.playerpicked.scheduler.utils;

import com.playerpicked.scheduler.identifiers.keys.ConfigKeys;
import com.playerpicked.scheduler.identifiers.keys.MessageKeys;
import com.playerpicked.scheduler.identifiers.types.TimeFormatType;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public final class TimeFormatUtil {
    private volatile TimeFormatType type = TimeFormatType.CLOCK_HH_MM_SS;

    public void reload() {
        type = parse(ConfigKeys.FORMATTING_TIME_FORMAT.getString());
    }

    public @NotNull String formatSeconds(long seconds) {
        return format(seconds * 1000L);
    }

    public @NotNull String format(long millis) {
        boolean neg = millis < 0;
        long ms = Math.abs(millis);
        long totalSeconds = ms / 1000L;
        long days = totalSeconds / 86400L;
        long hours = (totalSeconds % 86400L) / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;

        String out = switch (type) {
            case CLOCK_DD_HH_MM_SS -> pad(days) + ":" + pad(hours) + ":" + pad(minutes) + ":" + pad(seconds);
            case CLOCK_HH_MM_SS -> {
                long hh = days * 24L + hours;
                yield pad(hh) + ":" + pad(minutes) + ":" + pad(seconds);
            }
            case CLOCK_MM_SS -> {
                long mm = days * 1440L + hours * 60L + minutes;
                yield pad(mm) + ":" + pad(seconds);
            }
            case WORDS_FULL -> wordsFull(days, hours, minutes, seconds);
            case WORDS_COMPACT -> wordsCompact(days, hours, minutes, seconds);
            case HOURS_ONLY -> (days * 24L + hours) + MessageKeys.TIME_HOUR_SHORT.getMessage();
            case MINUTES_ONLY -> (days * 1440L + hours * 60L + minutes) + MessageKeys.TIME_MINUTE_SHORT.getMessage();
            case SECONDS_ONLY -> (days * 86400L + hours * 3600L + minutes * 60L + seconds) + MessageKeys.TIME_SECOND_SHORT.getMessage();
        };
        return neg ? "-" + out : out;
    }

    private TimeFormatType parse(String s) {
        if (s == null) return TimeFormatType.CLOCK_HH_MM_SS;

        try {
            return TimeFormatType.valueOf(s.trim().toUpperCase());
        } catch (Exception e) {
            return TimeFormatType.CLOCK_HH_MM_SS;
        }
    }

    private @NotNull String wordsFull(long d, long h, long m, long s) {
        StringBuilder builder = new StringBuilder(32);

        if (d > 0) builder.append(d).append(' ').append(unit(d, MessageKeys.TIME_DAY, MessageKeys.TIME_DAY_PLURAL)).append(' ');
        if (h > 0) builder.append(h).append(' ').append(unit(h, MessageKeys.TIME_HOUR, MessageKeys.TIME_HOUR_PLURAL)).append(' ');
        if (m > 0) builder.append(m).append(' ').append(unit(m, MessageKeys.TIME_MINUTE, MessageKeys.TIME_MINUTE_PLURAL)).append(' ');
        if (s > 0 || builder.isEmpty()) builder.append(s).append(' ').append(unit(s, MessageKeys.TIME_SECOND, MessageKeys.TIME_SECOND_PLURAL));

        return trimSpace(builder);
    }


    private @NotNull String wordsCompact(long d, long h, long m, long s) {
        StringBuilder builder = new StringBuilder(24);

        if (d > 0) builder.append(d).append(MessageKeys.TIME_DAY_SHORT.getMessage()).append(' ');
        if (h > 0) builder.append(h).append(MessageKeys.TIME_HOUR_SHORT.getMessage()).append(' ');
        if (m > 0) builder.append(m).append(MessageKeys.TIME_MINUTE_SHORT.getMessage()).append(' ');
        if (s > 0 || builder.isEmpty()) builder.append(s).append(MessageKeys.TIME_SECOND_SHORT.getMessage());

        return trimSpace(builder);
    }

    private @NotNull String pad(long v) {
        if (v < 10) return "0" + v;
        return Long.toString(v);
    }

    private @NotNull String trimSpace(@NotNull StringBuilder b) {
        int len = b.length();
        if (len > 0 && b.charAt(len - 1) == ' ') b.setLength(len - 1);

        return b.toString();
    }

    private @NotNull String unit(long value, MessageKeys singular, MessageKeys plural) {
        return value == 1 ? singular.getMessage() : plural.getMessage();
    }
}