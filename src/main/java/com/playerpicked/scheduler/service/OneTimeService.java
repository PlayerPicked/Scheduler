package com.playerpicked.scheduler.service;

import com.playerpicked.scheduler.Scheduler;
import com.playerpicked.scheduler.identifiers.keys.ConfigKeys;
import com.playerpicked.scheduler.manager.DataManager;
import com.playerpicked.scheduler.model.OneTimeTask;
import com.playerpicked.scheduler.utils.LoggerUtils;
import com.playerpicked.scheduler.utils.TimeParseUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class OneTimeService {
    private final Map<Integer, OneTimeTask> tasks = new ConcurrentHashMap<>();

    public void load() {
        DataManager data = Scheduler.getInstance().getDataManager();
        tasks.clear();

        long now = System.currentTimeMillis();
        long maxAge = TimeParseUtil.parseDuration(ConfigKeys.CATCH_UP_MAX_AGE.getRawString("1h"));
        boolean runMissed = ConfigKeys.ONE_TIME_RUN_MISSED.getBoolean(true);
        long delay = Math.max(1L, ConfigKeys.CATCH_UP_DELAY.getInt(30) * 20L);

        for (OneTimeTask task : data.getOneTimeTasks()) {
            if (task.time() > now) {
                tasks.put(task.id(), task);
                continue;
            }

            data.removeOneTimeTask(task.id());

            if (runMissed && (maxAge <= 0 || now - task.time() <= maxAge)) {
                LoggerUtils.info("One-time task #{} was missed while the server was offline, running it in {} seconds.", task.id(), delay / 20L);
                Scheduler.getScheduler().runTaskLater(() -> Scheduler.getInstance().getActionService().execute(task.actions()), delay);
            } else {
                LoggerUtils.warn("One-time task #{} expired while the server was offline ({}), it was removed.", task.id(), TimeParseUtil.format(task.time()));
            }
        }

        data.save();
    }

    @NotNull
    public OneTimeTask add(long time, @NotNull List<String> actions, @NotNull String creator) {
        DataManager data = Scheduler.getInstance().getDataManager();

        OneTimeTask task = new OneTimeTask(data.nextOneTimeId(), time, List.copyOf(actions), creator);
        tasks.put(task.id(), task);

        data.putOneTimeTask(task);
        data.save();
        return task;
    }

    @NotNull
    public Optional<OneTimeTask> cancel(int id) {
        OneTimeTask task = tasks.remove(id);
        if (task == null) return Optional.empty();

        DataManager data = Scheduler.getInstance().getDataManager();
        data.removeOneTimeTask(id);
        data.save();
        return Optional.of(task);
    }

    @NotNull
    @Unmodifiable
    public List<OneTimeTask> getTasks() {
        return tasks.values().stream().sorted(Comparator.comparingLong(OneTimeTask::time)).toList();
    }

    void tick(long now) {
        for (OneTimeTask task : tasks.values()) {
            if (task.time() > now || tasks.remove(task.id()) == null) continue;

            DataManager data = Scheduler.getInstance().getDataManager();
            data.removeOneTimeTask(task.id());
            data.save();

            Scheduler.getScheduler().runTask(() -> Scheduler.getInstance().getActionService().execute(task.actions()));
        }
    }
}