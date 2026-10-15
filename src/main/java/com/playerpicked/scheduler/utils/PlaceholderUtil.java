package com.playerpicked.scheduler.utils;

import lombok.experimental.UtilityClass;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@UtilityClass
public class PlaceholderUtil {

    public boolean isAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    @NotNull
    public String apply(@Nullable OfflinePlayer player, @NotNull String text) {
        if (!isAvailable() || text.indexOf('%') == -1) return text;
        return PlaceholderAPI.setPlaceholders(player, text);
    }
}