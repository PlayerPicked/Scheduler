package com.playerpicked.scheduler.guis.service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MenuService {

    private static final long DEFAULT_COOLDOWN_MS = 400;

    private static final ConcurrentHashMap<UUID, Long> lastOpen = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Long> openingLock = new ConcurrentHashMap<>();

    private MenuService() {}

    public static boolean canOpen(UUID uuid) {
        long now = System.currentTimeMillis();

        Long lockedUntil = openingLock.get(uuid);
        if (lockedUntil != null && lockedUntil > now) {
            return false;
        }

        Long last = lastOpen.get(uuid);
        if (last != null && (now - last) < DEFAULT_COOLDOWN_MS) {
            return false;
        }

        lastOpen.put(uuid, now);
        openingLock.put(uuid, now + 50);
        return true;
    }

    public static void clear(UUID uuid) {
        openingLock.remove(uuid);
    }
}