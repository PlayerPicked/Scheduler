package com.playerpicked.scheduler.identifiers.keys;

import com.playerpicked.scheduler.Scheduler;
import com.playerpicked.scheduler.config.Config;
import com.playerpicked.scheduler.processor.MessageProcessor;
import org.jetbrains.annotations.NotNull;

public enum ConfigKeys {
    TIMEZONE("timezone"),

    UPDATE_CHECKER_ENABLED("update-checker.enabled"),

    FORMATTING_TIME_FORMAT("formatting.time-format"),

    ACTIONS_TITLE_FADE_IN("actions.title.fade-in"),
    ACTIONS_TITLE_STAY("actions.title.stay"),
    ACTIONS_TITLE_FADE_OUT("actions.title.fade-out"),

    ACTIONS_SOUND_VOLUME("actions.sound.volume"),
    ACTIONS_SOUND_PITCH("actions.sound.pitch"),

    CATCH_UP_ENABLED("catch-up.enabled"),
    CATCH_UP_MAX_AGE("catch-up.max-age"),
    CATCH_UP_DELAY("catch-up.delay"),

    ONE_TIME_RUN_MISSED("one-time.run-missed"),

    EDITOR_EVENT_NAME_PATTERN("editor.event-name-pattern");

    private static final Config config = Scheduler.getInstance().getConfiguration();
    private final String path;

    ConfigKeys(@NotNull String path) {
        this.path = path;
    }

    public @NotNull String getString() {
        return MessageProcessor.process(config.getString(path));
    }

    public @NotNull String getRawString(@NotNull String def) {
        String value = config.getString(path, def);
        return value == null ? def : value;
    }

    public boolean getBoolean(boolean def) {
        return config.getBoolean(path, def);
    }

    public int getInt(int def) {
        return config.getInt(path, def);
    }

    public float getFloat(float def) {
        return config.getFloat(path, def);
    }
}