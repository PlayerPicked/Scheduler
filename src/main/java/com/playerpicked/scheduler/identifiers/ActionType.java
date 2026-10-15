package com.playerpicked.scheduler.identifiers;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

@Getter
public enum ActionType {
    COMMAND("[COMMAND]", "Console command"),
    PLAYER_COMMAND("[PLAYER_COMMAND]", "Player command"),
    MESSAGE("[MESSAGE]", "Message"),
    ACTIONBAR("[ACTIONBAR]", "Actionbar"),
    TITLE("[TITLE]", "Title"),
    SUBTITLE("[SUBTITLE]", "Subtitle"),
    SOUND("[SOUND]", "Sound"),
    WEBHOOK("[WEBHOOK]", "Webhook"),
    RANDOM("[RANDOM]", "Random group"),
    DELAY("[DELAY]", "Delay");

    private final String prefix;
    private final String displayName;

    ActionType(@NotNull String prefix, @NotNull String displayName) {
        this.prefix = prefix;
        this.displayName = displayName;
    }

    @Nullable
    public static ActionType parse(@NotNull String action) {
        String upper = action.trim().toUpperCase(Locale.ROOT);

        for (ActionType type : values()) {
            if (upper.startsWith(type.prefix)) return type;
        }

        return null;
    }

    @NotNull
    public String strip(@NotNull String action) {
        return action.trim().substring(prefix.length()).trim();
    }
}