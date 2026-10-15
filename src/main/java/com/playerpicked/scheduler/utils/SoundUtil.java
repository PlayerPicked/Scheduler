package com.playerpicked.scheduler.utils;

import lombok.experimental.UtilityClass;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;

@UtilityClass
public final class SoundUtil {
    public void play(@Nullable Player player, @Nullable String sound) {
        if (player == null) return;
        if (sound == null || sound.isEmpty()) return;

        player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
    }
}