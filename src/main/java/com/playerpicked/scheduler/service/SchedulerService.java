package com.playerpicked.scheduler.service;

import com.github.Anon8281.universalScheduler.scheduling.tasks.MyScheduledTask;
import com.playerpicked.scheduler.Scheduler;
import com.playerpicked.scheduler.api.event.ScheduledEventRunEvent;
import com.playerpicked.scheduler.identifiers.keys.ConfigKeys;
import com.playerpicked.scheduler.manager.DataManager;
import com.playerpicked.scheduler.model.ActionContext;
import com.playerpicked.scheduler.model.ScheduledEvent;
import com.playerpicked.scheduler.utils.ConditionUtil;
import com.playerpicked.scheduler.utils.CronUtil;
import com.playerpicked.scheduler.utils.LoggerUtils;
import com.playerpicked.scheduler.utils.TimeParseUtil;
import it.sauronsoftware.cron4j.Predictor;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;

public final class SchedulerService {
    private static final long TICKER_PERIOD = 20L;
    private static final long ALIVE_SAVE_INTERVAL = 60_000L;

    private it.sauronsoftware.cron4j.Scheduler cron;
    private MyScheduledTask ticker;

    private volatile long lastTick;
    private long lastAliveSave;

    public synchronized void start() {
        cron = new it.sauronsoftware.cron4j.Scheduler();
        cron.setDaemon(true);
        cron.setTimeZone(CronUtil.getTimeZone());

        for (ScheduledEvent event : Scheduler.getInstance().getEventManager().getEvents()) {
            if (!event.isEnabled()) continue;

            for (String time : event.getTimes()) {
                if (!CronUtil.isValid(time)) continue;

                String name = event.getName();
                cron.schedule(time.trim(), () -> Scheduler.getScheduler().runTask(() -> trigger(name)));
            }
        }

        cron.start();

        if (ticker == null) {
            lastTick = System.currentTimeMillis();
            ticker = Scheduler.getScheduler().runTaskTimerAsynchronously(this::tick, TICKER_PERIOD, TICKER_PERIOD);
        }
    }

    public synchronized void stop() {
        if (cron != null && cron.isStarted()) cron.stop();
        cron = null;
    }

    public synchronized void restart() {
        stop();
        start();
    }

    public synchronized void shutdown() {
        stop();

        if (ticker != null) ticker.cancel();
        ticker = null;

        DataManager data = Scheduler.getInstance().getDataManager();
        data.setLastAlive(System.currentTimeMillis());
        data.save();
    }

    private void tick() {
        long previous = lastTick;
        long now = System.currentTimeMillis();
        lastTick = now;

        for (ScheduledEvent event : Scheduler.getInstance().getEventManager().getEvents()) {
            if (!event.isEnabled()) continue;

            for (LocalDateTime date : event.getDates()) {
                long time = TimeParseUtil.toMillis(date);
                if (time <= previous || time > now) continue;

                String name = event.getName();
                Scheduler.getScheduler().runTask(() -> trigger(name));
                break;
            }
        }

        Scheduler.getInstance().getOneTimeService().tick(now);

        if (now - lastAliveSave >= ALIVE_SAVE_INTERVAL) {
            lastAliveSave = now;

            DataManager data = Scheduler.getInstance().getDataManager();
            data.setLastAlive(now);
            data.save();
        }
    }

    private void trigger(@NotNull String name) {
        Scheduler.getInstance().getEventManager().get(name)
                .filter(ScheduledEvent::isEnabled)
                .ifPresent(event -> run(event, false));
    }

    public boolean run(@NotNull ScheduledEvent event, boolean manual) {
        ActionService actionService = Scheduler.getInstance().getActionService();
        ActionContext context = ActionContext.of(event);

        if (!manual && !ConditionUtil.test(event.getConditions())) return true;

        if (!manual && event.hasPlayerRequirement()) {
            long online = Bukkit.getOnlinePlayers().stream().filter(event.getFilter()).count();

            if (online < event.getRequiredPlayers()) {
                actionService.execute(context, event.getNotEnoughPlayersActions());
                return true;
            }
        }

        ScheduledEventRunEvent runEvent = new ScheduledEventRunEvent(event, manual);
        Bukkit.getPluginManager().callEvent(runEvent);
        if (runEvent.isCancelled()) return false;

        Scheduler.getInstance().getDataManager().setLastRun(event.getName(), System.currentTimeMillis());
        actionService.execute(context, event.getActions());
        return true;
    }

    public void catchUp() {
        long lastAlive = Scheduler.getInstance().getDataManager().getLastAlive();
        if (lastAlive <= 0) return;

        long now = System.currentTimeMillis();
        long maxAge = TimeParseUtil.parseDuration(ConfigKeys.CATCH_UP_MAX_AGE.getRawString("1h"));
        long since = maxAge > 0 ? Math.max(lastAlive, now - maxAge) : lastAlive;
        boolean def = ConfigKeys.CATCH_UP_ENABLED.getBoolean(false);
        long delay = Math.max(1L, ConfigKeys.CATCH_UP_DELAY.getInt(30) * 20L);

        for (ScheduledEvent event : Scheduler.getInstance().getEventManager().getEvents()) {
            if (!event.isEnabled() || !event.isCatchUp(def) || !missed(event, since, now)) continue;

            String name = event.getName();
            LoggerUtils.info("Event '{}' was missed while the server was offline, running it in {} seconds.", name, delay / 20L);
            Scheduler.getScheduler().runTaskLater(() -> trigger(name), delay);
        }
    }

    private boolean missed(@NotNull ScheduledEvent event, long since, long now) {
        for (String time : event.getTimes()) {
            if (!CronUtil.isValid(time)) continue;

            Predictor predictor = new Predictor(time.trim(), since);
            predictor.setTimeZone(CronUtil.getTimeZone());
            if (predictor.nextMatchingTime() <= now) return true;
        }

        for (LocalDateTime date : event.getDates()) {
            long time = TimeParseUtil.toMillis(date);
            if (time > since && time <= now) return true;
        }

        return false;
    }
}