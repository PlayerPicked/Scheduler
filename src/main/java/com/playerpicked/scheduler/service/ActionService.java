package com.playerpicked.scheduler.service;

import com.playerpicked.scheduler.Scheduler;
import com.playerpicked.scheduler.identifiers.ActionType;
import com.playerpicked.scheduler.identifiers.keys.ConfigKeys;
import com.playerpicked.scheduler.model.ActionContext;
import com.playerpicked.scheduler.model.RandomGroup;
import com.playerpicked.scheduler.processor.MessageProcessor;
import com.playerpicked.scheduler.utils.LoggerUtils;
import com.playerpicked.scheduler.utils.PlaceholderUtil;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ActionService {
    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacySection();

    private static final int MAX_RANDOM_DEPTH = 16;

    public void execute(@NotNull List<String> actions) {
        execute(ActionContext.GLOBAL, actions);
    }

    public void execute(@NotNull ActionContext context, @NotNull List<String> actions) {
        execute(context, List.copyOf(actions), 0, 0);
    }

    private void execute(@NotNull ActionContext context, @NotNull List<String> actions, int start, int depth) {
        for (int index = start; index < actions.size(); index++) {
            String raw = actions.get(index);
            ActionType type = ActionType.parse(raw);

            if (type == null) {
                LoggerUtils.warn("Unknown action: {}", raw);
                continue;
            }

            String value = type.strip(raw);

            switch (type) {
                case DELAY -> {
                    long ticks = parseDelay(value);
                    if (ticks <= 0) continue;

                    int next = index + 1;
                    Scheduler.getScheduler().runTaskLater(() -> execute(context, actions, next, depth), ticks);
                    return;
                }
                case RANDOM -> {
                    RandomGroup group = context.randomGroups().get(value.toLowerCase(Locale.ROOT));

                    if (group == null) {
                        LoggerUtils.warn("Unknown random group: {}", value);
                        continue;
                    }

                    if (depth >= MAX_RANDOM_DEPTH) {
                        LoggerUtils.warn("Random group '{}' is nested too deep, skipping it.", value);
                        continue;
                    }

                    List<String> expanded = new ArrayList<>(group.roll());
                    expanded.addAll(actions.subList(index + 1, actions.size()));

                    execute(context, expanded, 0, depth + 1);
                    return;
                }
                case TITLE -> {
                    String subtitle = "";

                    if (index + 1 < actions.size() && ActionType.parse(actions.get(index + 1)) == ActionType.SUBTITLE) {
                        subtitle = ActionType.SUBTITLE.strip(actions.get(++index));
                    }

                    sendTitle(audience(context), value, subtitle);
                }
                case SUBTITLE -> sendTitle(audience(context), "", value);
                case COMMAND -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), PlaceholderUtil.apply(null, value));
                case PLAYER_COMMAND -> audience(context).forEach(player ->
                        Scheduler.getScheduler().runTask(player, () -> player.performCommand(PlaceholderUtil.apply(player, value))));
                case MESSAGE -> audience(context).forEach(player -> player.sendMessage(format(player, value)));
                case ACTIONBAR -> audience(context).forEach(player -> player.sendActionBar(component(player, value)));
                case SOUND -> playSound(audience(context), value);
                case WEBHOOK -> Scheduler.getInstance().getWebhookService().send(value);
            }
        }
    }

    @NotNull
    private static List<Player> audience(@NotNull ActionContext context) {
        return Bukkit.getOnlinePlayers().stream().filter(context.filter()).map(Player.class::cast).toList();
    }

    public static long parseDelay(@NotNull String value) {
        String trimmed = value.trim().toLowerCase(Locale.ROOT);

        try {
            if (trimmed.endsWith("s")) return Math.round(Double.parseDouble(trimmed.substring(0, trimmed.length() - 1)) * 20D);
            return Long.parseLong(trimmed);
        } catch (NumberFormatException exception) {
            LoggerUtils.warn("Invalid delay: {}", value);
            return 0L;
        }
    }

    private void sendTitle(@NotNull List<Player> audience, @NotNull String title, @NotNull String subtitle) {
        Title.Times times = Title.Times.times(
                ticks(ConfigKeys.ACTIONS_TITLE_FADE_IN.getInt(10)),
                ticks(ConfigKeys.ACTIONS_TITLE_STAY.getInt(70)),
                ticks(ConfigKeys.ACTIONS_TITLE_FADE_OUT.getInt(20)));

        for (Player player : audience) {
            player.showTitle(Title.title(component(player, title), component(player, subtitle), times));
        }
    }

    private void playSound(@NotNull List<Player> audience, @NotNull String value) {
        String[] parts = value.split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) return;

        Sound.Type type = resolveSound(parts[0]);
        if (type == null) {
            LoggerUtils.warn("Invalid sound: {}", parts[0]);
            return;
        }

        float volume = parts.length > 1 ? parseFloat(parts[1], ConfigKeys.ACTIONS_SOUND_VOLUME.getFloat(1F)) : ConfigKeys.ACTIONS_SOUND_VOLUME.getFloat(1F);
        float pitch = parts.length > 2 ? parseFloat(parts[2], ConfigKeys.ACTIONS_SOUND_PITCH.getFloat(1F)) : ConfigKeys.ACTIONS_SOUND_PITCH.getFloat(1F);

        Sound sound = Sound.sound(type, Sound.Source.MASTER, volume, pitch);
        audience.forEach(player -> player.playSound(sound, Sound.Emitter.self()));
    }

    @Nullable
    private Sound.Type resolveSound(@NotNull String name) {
        if (name.contains(".") || name.contains(":")) {
            try {
                Key key = Key.key(name.toLowerCase(Locale.ROOT));
                return () -> key;
            } catch (InvalidKeyException exception) {
                return null;
            }
        }

        for (org.bukkit.Sound sound : Registry.SOUNDS) {
            NamespacedKey key = Registry.SOUNDS.getKey(sound);
            if (key != null && key.value().replace('.', '_').equalsIgnoreCase(name)) return sound;
        }

        return null;
    }

    private static float parseFloat(@NotNull String raw, float def) {
        try {
            return Float.parseFloat(raw);
        } catch (NumberFormatException exception) {
            return def;
        }
    }

    @NotNull
    private static Duration ticks(int ticks) {
        return Duration.ofMillis(Math.max(0, ticks) * 50L);
    }

    @NotNull
    private static String format(@Nullable Player player, @NotNull String text) {
        return MessageProcessor.process(PlaceholderUtil.apply(player, text));
    }

    @NotNull
    private static Component component(@Nullable Player player, @NotNull String text) {
        return SERIALIZER.deserialize(format(player, text));
    }
}