package com.playerpicked.scheduler.data;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MenuController {

    private static final ConcurrentHashMap<UUID, MenuController> menuMap = new ConcurrentHashMap<>();

    private final Player owner;
    private volatile Class<?> lastMenu;

    public MenuController(@NotNull Player owner) {
        this.owner = owner;
    }

    @NotNull
    public Player owner() {
        return owner;
    }

    public synchronized boolean canOpen(@NotNull Class<?> menuClass) {
        if (menuClass == lastMenu) return false;
        lastMenu = menuClass;

        return true;
    }

    public static MenuController getMenuUtils(@NotNull Player player) {
        return menuMap.computeIfAbsent(player.getUniqueId(), uuid -> new MenuController(player));
    }

    public static void remove(@NotNull Player player) {
        menuMap.remove(player.getUniqueId());
    }
}