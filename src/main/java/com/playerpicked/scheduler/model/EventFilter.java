package com.playerpicked.scheduler.model;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Predicate;

public record EventFilter(@NotNull List<String> worlds, @NotNull String permission) implements Predicate<Player> {

    public static final EventFilter NONE = new EventFilter(List.of(), "");

    @Override
    public boolean test(@NotNull Player player) {
        if (!worlds.isEmpty() && worlds.stream().noneMatch(world -> world.equalsIgnoreCase(player.getWorld().getName()))) return false;
        return permission.isEmpty() || player.hasPermission(permission);
    }
}