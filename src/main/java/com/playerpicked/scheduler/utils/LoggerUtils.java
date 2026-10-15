package com.playerpicked.scheduler.utils;

import com.playerpicked.scheduler.Scheduler;
import lombok.experimental.UtilityClass;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class LoggerUtils {
    private final Logger logger = LogManager.getLogger("Scheduler");

    public void info(@NotNull String msg, @NotNull Object... objs) {
        logger.info(msg, objs);
    }

    public void warn(@NotNull String msg, @NotNull Object... objs) {
        logger.warn(msg, objs);
    }

    public void error(@NotNull String msg, @NotNull Object... objs) {
        logger.error(msg, objs);
    }

    public void printStartup() {
        String main = "\u001B[38;2;86;201;205m";
        String reset = "\u001B[0m";
        String software = Scheduler.getInstance().getServer().getName();
        String version = Scheduler.getInstance().getServer().getVersion();

        info("");
        info("{}    ____   ____ _   _ _____ ____  _   _ _     _____ ____  {}", main, reset);
        info("{}   / ___| / ___| | | | ____|  _ \\| | | | |   | ____|  _ \\ {}", main, reset);
        info("{}   \\___ \\| |   | |_| |  _| | | | | | | | |   |  _| | |_) |{}", main, reset);
        info("{}    ___) | |___|  _  | |___| |_| | |_| | |___| |___|  _ < {}", main, reset);
        info("{}   |____/ \\____|_| |_|_____|____/ \\___/|_____|_____|_| \\_\\{}", main, reset);
        info("");
        info("{}   The plugin successfully started.{}", main, reset);
        info("{}   Scheduler {} {}{}", main, software, version, reset);
        info("{}   Discord @ dc.playerpicked.com{}", main, reset);
        info("");
        info("\u001B[33m   [Events] Loaded {} scheduled event(s).\u001B[0m", Scheduler.getInstance().getEventManager().getEvents().size());
    }
}