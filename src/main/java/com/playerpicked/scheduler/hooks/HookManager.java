package com.playerpicked.scheduler.hooks;

import com.playerpicked.scheduler.Scheduler;
import com.playerpicked.scheduler.hooks.impl.placeholder.PlaceholderHook;
import com.playerpicked.scheduler.utils.LoggerUtils;
import com.playerpicked.scheduler.utils.PlaceholderUtil;

public final class HookManager {

    private final Scheduler plugin = Scheduler.getInstance();

    private PlaceholderHook placeholderHook;

    public void load() {
        loadPlaceholderAPI();
    }

    public void unload() {
        if (placeholderHook != null) placeholderHook.unregister();
        placeholderHook = null;
    }

    private void loadPlaceholderAPI() {
        if (!plugin.getHooks().getBoolean("hooks.placeholderapi.enabled", true)) return;

        if (!PlaceholderUtil.isAvailable()) {
            LoggerUtils.warn("   [Hook] PlaceholderAPI not found, skipping.");
            return;
        }

        placeholderHook = new PlaceholderHook();
        placeholderHook.register();
        LoggerUtils.info("\u001B[32m   [Hook] PlaceholderAPI successfully enabled.\u001B[0m");
    }
}