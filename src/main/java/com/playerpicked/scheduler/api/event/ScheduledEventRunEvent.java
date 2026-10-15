package com.playerpicked.scheduler.api.event;

import com.playerpicked.scheduler.model.ScheduledEvent;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public final class ScheduledEventRunEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final ScheduledEvent event;
    private final boolean manual;

    @Setter private boolean cancelled;

    public ScheduledEventRunEvent(@NotNull ScheduledEvent event, boolean manual) {
        this.event = event;
        this.manual = manual;
    }

    @NotNull
    public String getName() {
        return event.getName();
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}