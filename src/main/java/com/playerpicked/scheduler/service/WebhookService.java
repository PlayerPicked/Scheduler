package com.playerpicked.scheduler.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.playerpicked.scheduler.Scheduler;
import com.playerpicked.scheduler.utils.LoggerUtils;
import com.playerpicked.scheduler.utils.PlaceholderUtil;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;

public final class WebhookService {
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();

    public void send(@NotNull String name) {
        Section section = Scheduler.getInstance().getWebhooks().getSection("webhooks." + name);
        if (section == null) {
            LoggerUtils.warn("Unknown webhook: {}", name);
            return;
        }

        String url = section.getString("url", section.getString("webhook-url", ""));
        if (url == null || url.isBlank()) {
            LoggerUtils.warn("Webhook '{}' has no url set.", name);
            return;
        }

        String body = build(section).toString();

        Scheduler.getScheduler().runTaskAsynchronously(() -> post(name, url, body));
    }

    @NotNull
    private JsonObject build(@NotNull Section section) {
        JsonObject message = new JsonObject();

        addIfPresent(message, "content", section.getString("content", ""));
        addIfPresent(message, "username", section.getString("username", ""));
        addIfPresent(message, "avatar_url", section.getString("avatar-url", ""));

        Section embedSection = section.getSection("embed");
        if (embedSection == null) return message;

        JsonObject embed = new JsonObject();
        addIfPresent(embed, "title", embedSection.getString("title", ""));
        addIfPresent(embed, "description", embedSection.getString("description", ""));
        addIfPresent(embed, "url", embedSection.getString("url", ""));
        embed.addProperty("color", parseColor(embedSection.getString("color", "#56C9CD")));

        String footerText = embedSection.isSection("footer")
                ? embedSection.getString("footer.text", "")
                : embedSection.getString("footer", "");

        if (!footerText.isEmpty()) {
            JsonObject footer = new JsonObject();
            footer.addProperty("text", placeholders(footerText));
            addIfPresent(footer, "icon_url", embedSection.getString("footer.icon-url", ""));
            embed.add("footer", footer);
        }

        String thumbnail = embedSection.getString("thumbnail", "");
        if (!thumbnail.isEmpty()) {
            JsonObject image = new JsonObject();
            image.addProperty("url", thumbnail);
            embed.add("thumbnail", image);
        }

        if (embedSection.getBoolean("timestamp", false)) embed.addProperty("timestamp", Instant.now().toString());

        JsonArray embeds = new JsonArray();
        embeds.add(embed);
        message.add("embeds", embeds);

        return message;
    }

    private void post(@NotNull String name, @NotNull String url, @NotNull String body) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(TIMEOUT)
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "Scheduler-Webhook")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();

            if (status == 429) LoggerUtils.warn("Webhook '{}' is rate limited. Retry-After: {}", name, response.headers().firstValue("Retry-After").orElse("?"));
            else if (status >= 400) LoggerUtils.warn("Failed to send webhook '{}': HTTP {}", name, status);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (Exception exception) {
            LoggerUtils.warn("Failed to send webhook '{}': {}", name, exception.getMessage());
        }
    }

    private void addIfPresent(@NotNull JsonObject object, @NotNull String key, @Nullable String value) {
        if (value == null || value.isEmpty()) return;
        object.addProperty(key, placeholders(value));
    }

    @NotNull
    private String placeholders(@NotNull String value) {
        return PlaceholderUtil.apply(null, value);
    }

    private int parseColor(@Nullable String raw) {
        if (raw == null) return 0x56C9CD;

        try {
            return Integer.parseInt(raw.replace("#", "").trim(), 16);
        } catch (NumberFormatException exception) {
            return 0x56C9CD;
        }
    }
}