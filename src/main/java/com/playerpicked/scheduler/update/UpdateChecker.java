package com.playerpicked.scheduler.update;

import com.playerpicked.scheduler.Scheduler;
import com.playerpicked.scheduler.identifiers.keys.ConfigKeys;
import com.playerpicked.scheduler.identifiers.keys.MessageKeys;
import com.playerpicked.scheduler.utils.LoggerUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Pattern;

public final class UpdateChecker implements Listener {

    public static final String UPDATE_PERMISSION = "scheduler.update-notify";

    private static final String API_URL = "https://api.polymart.org/v1/getResourceInfoSimple/?key=version&resource_id=";
    private static final String RESOURCE_URL = "https://polymart.org/resource/";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final long CHECK_INTERVAL_TICKS = 20L * 60L * 30L;
    private static final Pattern VERSION_PATTERN = Pattern.compile("\\d+(\\.\\d+)*([-+.][\\w.]+)?");

    private final Scheduler plugin = Scheduler.getInstance();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();
    private final int resourceId;

    private volatile String latestVersion;

    public UpdateChecker(int resourceId) {
        this.resourceId = resourceId;
    }

    public void start() {
        if (!ConfigKeys.UPDATE_CHECKER_ENABLED.getBoolean(true)) return;

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        Scheduler.getScheduler().runTaskTimerAsynchronously(this::check, 1L, CHECK_INTERVAL_TICKS);
    }

    @EventHandler
    public void onJoin(@NotNull PlayerJoinEvent event) {
        String latest = latestVersion;
        if (latest == null) return;

        Player player = event.getPlayer();
        if (player.hasPermission(UPDATE_PERMISSION)) player.sendMessage(message(latest));
    }

    private void check() {
        String latest = fetchLatestVersion();
        if (latest == null || latest.equals(latestVersion)) return;

        if (!isNewer(latest, currentVersion())) return;

        latestVersion = latest;
        notifyUpdate(latest);
    }

    private @Nullable String fetchLatestVersion() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + resourceId))
                .timeout(TIMEOUT)
                .header("User-Agent", "Scheduler-UpdateChecker")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                LoggerUtils.warn("Unable to check for updates: HTTP {}", response.statusCode());
                return null;
            }

            String version = response.body().trim();

            if (!VERSION_PATTERN.matcher(version).matches()) {
                LoggerUtils.warn("Unable to check for updates: unexpected response '{}'", version);
                return null;
            }

            return version;
        } catch (IOException exception) {
            LoggerUtils.warn("Unable to check for updates: {}", exception.getMessage());
            return null;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private void notifyUpdate(@NotNull String latest) {
        LoggerUtils.warn("A new update is available! Current: {}, Latest: {}", currentVersion(), latest);
        LoggerUtils.warn("Download it at: {}{}", RESOURCE_URL, resourceId);

        String message = message(latest);

        Scheduler.getScheduler().runTask(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission(UPDATE_PERMISSION)) player.sendMessage(message);
            }
        });
    }

    @NotNull
    private String message(@NotNull String latest) {
        return MessageKeys.UPDATE_AVAILABLE.getMessage("{current}", currentVersion(), "{new}", latest);
    }

    @NotNull
    private String currentVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    private static boolean isNewer(@NotNull String latest, @NotNull String current) {
        String[] latestParts = numericPart(latest).split("\\.");
        String[] currentParts = numericPart(current).split("\\.");

        for (int index = 0; index < Math.max(latestParts.length, currentParts.length); index++) {
            int latestPart = part(latestParts, index);
            int currentPart = part(currentParts, index);

            if (latestPart != currentPart) return latestPart > currentPart;
        }

        return false;
    }

    @NotNull
    private static String numericPart(@NotNull String version) {
        int end = 0;
        while (end < version.length() && (Character.isDigit(version.charAt(end)) || version.charAt(end) == '.')) end++;
        return version.substring(0, end);
    }

    private static int part(@NotNull String[] parts, int index) {
        if (index >= parts.length || parts[index].isEmpty()) return 0;

        try {
            return Integer.parseInt(parts[index]);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }
}