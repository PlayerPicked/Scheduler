package com.playerpicked.scheduler.api;

import com.playerpicked.scheduler.Scheduler;
import com.playerpicked.scheduler.model.ScheduledEvent;
import com.playerpicked.scheduler.utils.CronUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Optional;

public final class SchedulerAPI {
    private static SchedulerAPI instance;

    @NotNull
    public static SchedulerAPI getInstance() {
        if (instance == null) throw new IllegalStateException("SchedulerAPI is not initialized!");
        return instance;
    }

    public static void initialize(@NotNull SchedulerAPI api) {
        if (instance != null) throw new IllegalStateException("SchedulerAPI is already initialized!");
        instance = api;
    }

    @NotNull
    @Unmodifiable
    public List<ScheduledEvent> getEvents() {
        return Scheduler.getInstance().getEventManager().getEvents();
    }

    @NotNull
    public Optional<ScheduledEvent> getEvent(@NotNull String name) {
        return Scheduler.getInstance().getEventManager().get(name);
    }

    public long getNextRun(@NotNull String name) {
        return getEvent(name).map(CronUtil::getNextRun).orElse(-1L);
    }

    public boolean run(@NotNull String name) {
        return getEvent(name)
                .map(event -> Scheduler.getInstance().getSchedulerService().run(event, true))
                .orElse(false);
    }

    public int scheduleOnce(long time, @NotNull List<String> actions, @NotNull String creator) {
        return Scheduler.getInstance().getOneTimeService().add(time, actions, creator).id();
    }

    public boolean cancelOnce(int id) {
        return Scheduler.getInstance().getOneTimeService().cancel(id).isPresent();
    }

    public void setEnabled(@NotNull String name, boolean enabled) {
        getEvent(name).ifPresent(event -> {
            event.setEnabled(enabled);
            Scheduler.getInstance().getEventManager().save(event);
        });
    }
}