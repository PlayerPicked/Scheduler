package com.playerpicked.scheduler.identifiers;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public enum ActionListType {
    ACTIONS("actions"),
    NOT_ENOUGH_PLAYERS("not-enough-players-actions");

    private final String path;

    ActionListType(@NotNull String path) {
        this.path = path;
    }
}