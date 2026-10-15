package com.playerpicked.scheduler.model;

import com.playerpicked.scheduler.identifiers.ActionListType;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
public final class ScheduledEvent {
    private final String name;

    @Setter private boolean enabled;
    @Setter private int requiredPlayers;

    private final List<String> times;
    private final List<String> actions;
    private final List<String> notEnoughPlayersActions;

    @Setter @NotNull private List<LocalDateTime> dates = List.of();
    @Setter @Nullable private Boolean catchUp;
    @Setter @NotNull private EventConditions conditions = EventConditions.NONE;
    @Setter @NotNull private EventFilter filter = EventFilter.NONE;
    @Setter @NotNull private Map<String, RandomGroup> randomGroups = Map.of();

    public ScheduledEvent(@NotNull String name, boolean enabled, int requiredPlayers, @NotNull List<String> times, @NotNull List<String> actions, @NotNull List<String> notEnoughPlayersActions) {
        this.name = name;
        this.enabled = enabled;
        this.requiredPlayers = requiredPlayers;
        this.times = new CopyOnWriteArrayList<>(times);
        this.actions = new CopyOnWriteArrayList<>(actions);
        this.notEnoughPlayersActions = new CopyOnWriteArrayList<>(notEnoughPlayersActions);
    }

    @NotNull
    public static ScheduledEvent empty(@NotNull String name) {
        return new ScheduledEvent(name, true, -1, List.of(), List.of(), List.of());
    }

    public boolean hasPlayerRequirement() {
        return requiredPlayers > 0;
    }

    public boolean isCatchUp(boolean def) {
        return catchUp == null ? def : catchUp;
    }

    @NotNull
    public List<String> getActions(@NotNull ActionListType type) {
        return switch (type) {
            case ACTIONS -> actions;
            case NOT_ENOUGH_PLAYERS -> notEnoughPlayersActions;
        };
    }
}