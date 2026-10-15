package com.playerpicked.scheduler.utils;

import com.playerpicked.scheduler.Scheduler;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Matcher;

@UtilityClass
public class LegacyMigrationUtil {
    private final String LEGACY_FOLDER = "mc-Scheduler";
    private final String BACKUP_FOLDER = "mc-Scheduler-old";

    public boolean migrate(@NotNull Path dataFolder) {
        Path legacyFolder = dataFolder.resolveSibling(LEGACY_FOLDER);
        if (!Files.isDirectory(legacyFolder)) return true;

        LoggerUtils.info("Found legacy {} folder, migrating to {}...", LEGACY_FOLDER, dataFolder.getFileName());

        try {
            Files.createDirectories(dataFolder);

            Path legacyConfig = legacyFolder.resolve("config.yml");
            if (Files.exists(legacyConfig)) {
                YamlDocument legacy = YamlDocument.create(legacyConfig.toFile());

                migrateEvents(legacy, dataFolder.resolve("events.yml"));
                migrateTimezone(legacy, dataFolder.resolve("config.yml"));
            }

            Path legacyWebhooks = legacyFolder.resolve("webhooks.yml");
            Path webhooks = dataFolder.resolve("webhooks.yml");
            if (Files.exists(legacyWebhooks) && !Files.exists(webhooks)) Files.copy(legacyWebhooks, webhooks);

            Path backup = legacyFolder.resolveSibling(BACKUP_FOLDER);
            if (Files.exists(backup)) backup = legacyFolder.resolveSibling(BACKUP_FOLDER + "-" + System.currentTimeMillis());
            Files.move(legacyFolder, backup, StandardCopyOption.ATOMIC_MOVE);

            LoggerUtils.info("Legacy data migrated successfully. The old folder was kept as {}.", backup.getFileName());
            return true;
        } catch (IOException exception) {
            LoggerUtils.error("Failed to migrate legacy {} folder: {}", LEGACY_FOLDER, exception.toString());
            return false;
        }
    }

    private void migrateEvents(@NotNull YamlDocument legacy, @NotNull Path target) throws IOException {
        Section legacyEvents = legacy.getSection("Events");
        if (legacyEvents == null || Files.exists(target)) return;

        YamlDocument events = YamlDocument.create(target.toFile());

        for (String name : legacyEvents.getRoutesAsStrings(false)) {
            Section event = legacyEvents.getSection(name);
            if (event == null) continue;

            String base = "events." + name;
            events.set(base + ".enabled", true);
            events.set(base + ".required-players", event.getInt("required_players", -1));
            events.set(base + ".times", event.getStringList("times"));
            events.set(base + ".actions", event.getStringList("commands"));
            events.set(base + ".not-enough-players-actions", event.getStringList("no_enough_player_commands"));
        }

        events.save();
        LoggerUtils.info("Migrated {} event(s) into events.yml.", legacyEvents.getRoutesAsStrings(false).size());
    }

    private void migrateTimezone(@NotNull YamlDocument legacy, @NotNull Path target) throws IOException {
        String timezone = legacy.getString("Settings.timezone");
        if (timezone == null || Files.exists(target)) return;

        try (InputStream defaults = Scheduler.getInstance().getResource("config.yml")) {
            if (defaults == null) return;

            String content = new String(defaults.readAllBytes(), StandardCharsets.UTF_8)
                    .replaceFirst("timezone: \"[^\"]*\"", Matcher.quoteReplacement("timezone: \"" + timezone + "\""));

            Files.writeString(target, content, StandardCharsets.UTF_8);
        }
    }
}