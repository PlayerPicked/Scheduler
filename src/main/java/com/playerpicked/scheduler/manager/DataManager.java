package com.playerpicked.scheduler.manager;

import com.playerpicked.scheduler.Scheduler;
import com.playerpicked.scheduler.model.OneTimeTask;
import com.playerpicked.scheduler.utils.LoggerUtils;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class DataManager {
    private YamlDocument data;

    public synchronized void load() {
        try {
            data = YamlDocument.create(new File(Scheduler.getInstance().getDataFolder(), "data.yml"));
        } catch (IOException exception) {
            LoggerUtils.error("Failed to load data.yml: {}", exception.getMessage());
        }
    }

    public synchronized void save() {
        if (data == null) return;

        try {
            data.save();
        } catch (IOException exception) {
            LoggerUtils.error("Failed to save data.yml: {}", exception.getMessage());
        }
    }

    public synchronized long getLastAlive() {
        return data == null ? -1L : data.getLong("last-alive", -1L);
    }

    public synchronized void setLastAlive(long time) {
        if (data != null) data.set("last-alive", time);
    }

    public synchronized long getLastRun(@NotNull String event) {
        return data == null ? -1L : data.getLong("last-runs." + event, -1L);
    }

    public synchronized void setLastRun(@NotNull String event, long time) {
        if (data != null) data.set("last-runs." + event, time);
    }

    public synchronized int nextOneTimeId() {
        int id = data.getInt("one-time.next-id", 1);
        data.set("one-time.next-id", id + 1);
        return id;
    }

    @NotNull
    public synchronized List<OneTimeTask> getOneTimeTasks() {
        List<OneTimeTask> tasks = new ArrayList<>();
        Section section = data == null ? null : data.getSection("one-time.tasks");
        if (section == null) return tasks;

        for (String key : section.getRoutesAsStrings(false)) {
            try {
                tasks.add(new OneTimeTask(
                        Integer.parseInt(key),
                        section.getLong(key + ".time"),
                        section.getStringList(key + ".actions"),
                        section.getString(key + ".creator", "?")));
            } catch (NumberFormatException exception) {
                LoggerUtils.warn("Invalid one-time task id in data.yml: {}", key);
            }
        }

        return tasks;
    }

    public synchronized void putOneTimeTask(@NotNull OneTimeTask task) {
        String base = "one-time.tasks." + task.id();
        data.set(base + ".time", task.time());
        data.set(base + ".actions", new ArrayList<>(task.actions()));
        data.set(base + ".creator", task.creator());
    }

    public synchronized void removeOneTimeTask(int id) {
        data.remove("one-time.tasks." + id);
    }
}