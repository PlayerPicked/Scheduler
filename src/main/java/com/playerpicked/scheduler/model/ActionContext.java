package com.playerpicked.scheduler.model;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public record ActionContext(@NotNull EventFilter filter, @NotNull Map<String, RandomGroup> randomGroups) {

    public static final ActionContext GLOBAL = new ActionContext(EventFilter.NONE, Map.of());

    @NotNull
    public static ActionContext of(@NotNull ScheduledEvent event) {
        return new ActionContext(event.getFilter(), event.getRandomGroups());
    }
}