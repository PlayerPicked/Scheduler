package com.playerpicked.scheduler.manager;

import com.playerpicked.scheduler.Scheduler;
import com.playerpicked.scheduler.config.Config;
import com.playerpicked.scheduler.identifiers.ActionListType;
import com.playerpicked.scheduler.model.EventConditions;
import com.playerpicked.scheduler.model.EventFilter;
import com.playerpicked.scheduler.model.RandomGroup;
import com.playerpicked.scheduler.model.ScheduledEvent;
import com.playerpicked.scheduler.utils.CronUtil;
import com.playerpicked.scheduler.utils.LoggerUtils;
import com.playerpicked.scheduler.utils.TimeParseUtil;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class EventManager {
    private static final String ROOT = "events";

    private final Map<String, ScheduledEvent> events = Collections.synchronizedMap(new LinkedHashMap<>());

    public void load() {
        Config config = Scheduler.getInstance().getEvents();
        Section root = config.getSection(ROOT);

        events.clear();
        if (root == null) return;

        for (String name : root.getRoutesAsStrings(false)) {
            Section section = root.getSection(name);
            if (section == null) continue;

            ScheduledEvent event = new ScheduledEvent(
                    name,
                    section.getBoolean("enabled", true),
                    section.getInt("required-players", -1),
                    section.getStringList("times"),
                    section.getStringList(ActionListType.ACTIONS.getPath()),
                    section.getStringList(ActionListType.NOT_ENOUGH_PLAYERS.getPath())
            );

            event.getTimes().stream()
                    .filter(time -> !CronUtil.isValid(time))
                    .forEach(time -> LoggerUtils.warn("Invalid cron expression '{}' in event '{}', it will be ignored.", time, name));

            event.setDates(loadDates(section, name));
            event.setCatchUp(section.contains("catch-up") ? section.getBoolean("catch-up") : null);
            event.setConditions(loadConditions(section, name));
            event.setFilter(new EventFilter(section.getStringList("filters.worlds"), section.getString("filters.permission", "")));
            event.setRandomGroups(loadRandomGroups(section.getSection("random-groups"), name));

            events.put(name.toLowerCase(), event);
        }
    }

    @NotNull
    private List<LocalDateTime> loadDates(@NotNull Section section, @NotNull String name) {
        List<LocalDateTime> dates = new ArrayList<>();

        for (String raw : section.getStringList("at")) {
            LocalDateTime date = TimeParseUtil.parseDateTime(raw);

            if (date == null) LoggerUtils.warn("Invalid date '{}' in event '{}', use the yyyy-MM-dd HH:mm format.", raw, name);
            else dates.add(date);
        }

        return List.copyOf(dates);
    }

    @NotNull
    private EventConditions loadConditions(@NotNull Section section, @NotNull String name) {
        Section conditions = section.getSection("conditions");
        if (conditions == null) return EventConditions.NONE;

        String rawFrom = conditions.getString("date.from", "");
        String rawTo = conditions.getString("date.to", "");
        LocalDateTime from = TimeParseUtil.parseDateTime(rawFrom);
        LocalDateTime to = TimeParseUtil.parseDateTime(rawTo);

        if (!rawFrom.isEmpty() && from == null) LoggerUtils.warn("Invalid conditions.date.from '{}' in event '{}'.", rawFrom, name);
        if (!rawTo.isEmpty() && to == null) LoggerUtils.warn("Invalid conditions.date.to '{}' in event '{}'.", rawTo, name);

        if (to != null && rawTo.trim().length() == 10) to = to.plusDays(1).minusNanos(1);

        return new EventConditions(conditions.getStringList("placeholders"), from, to);
    }

    @NotNull
    private Map<String, RandomGroup> loadRandomGroups(@Nullable Section section, @NotNull String name) {
        if (section == null) return Map.of();

        Map<String, RandomGroup> groups = new HashMap<>();

        for (String groupName : section.getRoutesAsStrings(false)) {
            Section group = section.getSection(groupName);
            Section entriesSection = group == null ? null : group.getSection("entries");
            if (entriesSection == null) {
                LoggerUtils.warn("Random group '{}' in event '{}' has no entries.", groupName, name);
                continue;
            }

            List<RandomGroup.Entry> entries = new ArrayList<>();
            for (String entryName : entriesSection.getRoutesAsStrings(false)) {
                entries.add(new RandomGroup.Entry(
                        entryName,
                        entriesSection.getDouble(entryName + ".weight", 1D),
                        entriesSection.getStringList(entryName + ".actions")));
            }

            groups.put(groupName.toLowerCase(), new RandomGroup(Math.max(1, group.getInt("pick", 1)), List.copyOf(entries)));
        }

        return Map.copyOf(groups);
    }

    @NotNull
    public Optional<ScheduledEvent> get(@NotNull String name) {
        return Optional.ofNullable(events.get(name.toLowerCase()));
    }

    public boolean exists(@NotNull String name) {
        return events.containsKey(name.toLowerCase());
    }

    @NotNull
    @Unmodifiable
    public List<ScheduledEvent> getEvents() {
        synchronized (events) {
            return List.copyOf(events.values());
        }
    }

    @NotNull
    public Collection<String> getNames() {
        return getEvents().stream().map(ScheduledEvent::getName).toList();
    }

    @NotNull
    public ScheduledEvent create(@NotNull String name) {
        ScheduledEvent event = ScheduledEvent.empty(name);
        events.put(name.toLowerCase(), event);
        save(event);
        return event;
    }

    public void delete(@NotNull ScheduledEvent event) {
        events.remove(event.getName().toLowerCase());

        Config config = Scheduler.getInstance().getEvents();
        config.remove(ROOT + "." + event.getName());
        config.save();

        Scheduler.getInstance().getSchedulerService().restart();
    }

    public void save(@NotNull ScheduledEvent event) {
        Config config = Scheduler.getInstance().getEvents();
        String base = ROOT + "." + event.getName();

        config.set(base + ".enabled", event.isEnabled());
        config.set(base + ".required-players", event.getRequiredPlayers());
        config.set(base + ".times", new ArrayList<>(event.getTimes()));
        config.set(base + "." + ActionListType.ACTIONS.getPath(), new ArrayList<>(event.getActions()));
        config.set(base + "." + ActionListType.NOT_ENOUGH_PLAYERS.getPath(), new ArrayList<>(event.getNotEnoughPlayersActions()));
        config.save();

        Scheduler.getInstance().getSchedulerService().restart();
    }
}