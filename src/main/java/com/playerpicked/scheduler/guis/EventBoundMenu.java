package com.playerpicked.scheduler.guis;

import com.playerpicked.scheduler.Scheduler;
import com.playerpicked.scheduler.data.MenuController;
import com.playerpicked.scheduler.model.ScheduledEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class EventBoundMenu extends PaginatedMenu {
    protected final String eventName;

    public EventBoundMenu(@NotNull MenuController menuController, @NotNull ScheduledEvent event) {
        super(menuController);
        this.eventName = event.getName();
    }

    @Nullable
    protected ScheduledEvent event() {
        return Scheduler.getInstance().getEventManager().get(eventName).orElse(null);
    }

    @Nullable
    protected ScheduledEvent eventOrClose() {
        ScheduledEvent event = event();
        if (event == null) Scheduler.getScheduler().runTask(menuController.owner(), () -> menuController.owner().closeInventory());
        return event;
    }

    protected void save(@NotNull ScheduledEvent event) {
        Scheduler.getInstance().getEventManager().save(event);
    }
}