package com.playerpicked.scheduler.identifiers.keys;

import com.playerpicked.scheduler.Scheduler;
import com.playerpicked.scheduler.config.Config;
import com.playerpicked.scheduler.processor.MessageProcessor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public enum MenuKeys {

    EVENTS_TITLE("events.title"),
    EVENTS_SIZE("events.size"),
    EVENTS_TICK("events.tick"),
    EVENTS_SLOTS("events.event-slots"),
    EVENTS_SOUND_OPEN("events.sounds.open"),
    EVENTS_SOUND_ERROR("events.sounds.error"),
    EVENTS_SOUND_ACTION("events.sounds.action"),
    EVENTS_SOUND_PAGE("events.sounds.page"),

    EDITOR_TITLE("event-editor.title"),
    EDITOR_SIZE("event-editor.size"),
    EDITOR_TICK("event-editor.tick"),
    EDITOR_SOUND_OPEN("event-editor.sounds.open"),
    EDITOR_SOUND_ERROR("event-editor.sounds.error"),
    EDITOR_SOUND_ACTION("event-editor.sounds.action"),

    TIMES_TITLE("times.title"),
    TIMES_SIZE("times.size"),
    TIMES_TICK("times.tick"),
    TIMES_SLOTS("times.time-slots"),
    TIMES_SOUND_OPEN("times.sounds.open"),
    TIMES_SOUND_ERROR("times.sounds.error"),
    TIMES_SOUND_ACTION("times.sounds.action"),
    TIMES_SOUND_PAGE("times.sounds.page"),

    ACTIONS_TITLE("actions.title"),
    ACTIONS_SIZE("actions.size"),
    ACTIONS_TICK("actions.tick"),
    ACTIONS_SLOTS("actions.action-slots"),
    ACTIONS_LIST_NAME_ACTIONS("actions.list-names.actions"),
    ACTIONS_LIST_NAME_NOT_ENOUGH_PLAYERS("actions.list-names.not-enough-players-actions"),
    ACTIONS_SOUND_OPEN("actions.sounds.open"),
    ACTIONS_SOUND_ERROR("actions.sounds.error"),
    ACTIONS_SOUND_ACTION("actions.sounds.action"),
    ACTIONS_SOUND_PAGE("actions.sounds.page");

    private static final Config config = Scheduler.getInstance().getGuis();
    private final String path;

    MenuKeys(@NotNull String path) {
        this.path = path;
    }

    public @NotNull String getString() {
        return MessageProcessor.process(config.getString(path));
    }

    public @NotNull String getRawString() {
        String value = config.getString(path);
        return value == null ? "" : value;
    }

    public int getInt() {
        return config.getInt(path);
    }

    public List<Integer> getIntList() {
        return config.getIntList(path);
    }
}