package com.playerpicked.scheduler.identifiers.keys;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public enum DialogKeys {
    CREATE_EVENT("dialogs.create-event"),
    ADD_TIME("dialogs.add-time"),
    EDIT_TIME("dialogs.edit-time"),
    ADD_ACTION("dialogs.add-action"),
    EDIT_ACTION("dialogs.edit-action"),
    REQUIRED_PLAYERS("dialogs.required-players");

    private final String path;

    DialogKeys(@NotNull String path) {
        this.path = path;
    }
}