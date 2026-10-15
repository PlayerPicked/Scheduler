package com.playerpicked.scheduler.utils;

import com.playerpicked.scheduler.Scheduler;
import com.playerpicked.scheduler.command.SchedulerCommand;
import com.playerpicked.scheduler.handler.CommandExceptionHandler;
import com.playerpicked.scheduler.listener.MenuListener;
import lombok.experimental.UtilityClass;
import revxrsal.commands.bukkit.BukkitLamp;

@UtilityClass
public class RegisterUtils {

    private final Scheduler plugin = Scheduler.getInstance();

    public void registerListeners() {
        plugin.getServer().getPluginManager().registerEvents(new MenuListener(), plugin);
    }

    public void registerCommands() {
        var lamp = BukkitLamp.builder(plugin)
                .exceptionHandler(new CommandExceptionHandler())
                .build();

        lamp.register(new SchedulerCommand());
    }
}