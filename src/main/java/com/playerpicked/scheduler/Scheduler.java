package com.playerpicked.scheduler;

import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import com.playerpicked.pickly.paper.PaperMetrics;
import com.playerpicked.scheduler.api.SchedulerAPI;
import com.playerpicked.scheduler.config.Config;
import com.playerpicked.scheduler.hooks.HookManager;
import com.playerpicked.scheduler.manager.DataManager;
import com.playerpicked.scheduler.manager.DialogInputManager;
import com.playerpicked.scheduler.manager.EventManager;
import com.playerpicked.scheduler.service.ActionService;
import com.playerpicked.scheduler.service.OneTimeService;
import com.playerpicked.scheduler.service.SchedulerService;
import com.playerpicked.scheduler.service.WebhookService;
import com.playerpicked.scheduler.update.UpdateChecker;
import com.playerpicked.scheduler.utils.LegacyMigrationUtil;
import com.playerpicked.scheduler.utils.LoggerUtils;
import com.playerpicked.scheduler.utils.RegisterUtils;
import com.playerpicked.scheduler.utils.TimeFormatUtil;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import lombok.Getter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import revxrsal.zapper.ZapperJavaPlugin;

import java.io.File;

public final class Scheduler extends ZapperJavaPlugin {

    @Getter private static Scheduler instance;
    @Getter private static TaskScheduler scheduler;

    @Getter private Config language;
    @Getter private Config guis;
    @Getter private Config hooks;
    @Getter private Config dialogs;
    @Getter private Config webhooks;
    @Getter private Config events;
    Config config;

    @Getter private DataManager dataManager;
    @Getter private EventManager eventManager;
    @Getter private ActionService actionService;
    @Getter private WebhookService webhookService;
    @Getter private SchedulerService schedulerService;
    @Getter private OneTimeService oneTimeService;
    @Getter private DialogInputManager dialogInputManager;
    @Getter private HookManager hookManager;

    private boolean migrationFailed;
    private PaperMetrics metrics;

    @Override
    public void onLoad() {
        instance = this;
        scheduler = UniversalScheduler.getScheduler(this);
        migrationFailed = !LegacyMigrationUtil.migrate(getDataFolder().toPath());
    }

    @Override
    public void onEnable() {
        if (migrationFailed) {
            LoggerUtils.error("Disabling plugin to protect legacy data. Move the mc-Scheduler folder contents into the Scheduler folder manually.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        initializeComponents();

        TimeFormatUtil.reload();

        dataManager = new DataManager();
        eventManager = new EventManager();
        actionService = new ActionService();
        webhookService = new WebhookService();
        schedulerService = new SchedulerService();
        oneTimeService = new OneTimeService();
        dialogInputManager = new DialogInputManager();
        hookManager = new HookManager();

        dataManager.load();
        eventManager.load();
        oneTimeService.load();
        schedulerService.start();
        schedulerService.catchUp();

        SchedulerAPI.initialize(new SchedulerAPI());

        RegisterUtils.registerCommands();
        RegisterUtils.registerListeners();

        LoggerUtils.printStartup();
        hookManager.load();

        metrics = new PaperMetrics(this, "pck_5FeirLBKIUKffVJxU2K2Q94b");
        new UpdateChecker(121232).start();
    }

    @Override
    public void onDisable() {
        if (migrationFailed) return;
        if (metrics != null) metrics.shutdown();
        if (schedulerService != null) schedulerService.shutdown();
        if (hookManager != null) hookManager.unload();
    }

    public Config getConfiguration() {
        return config;
    }

    public void reload() {
        config.reload();
        language.reload();
        guis.reload();
        hooks.reload();
        dialogs.reload();
        webhooks.reload();
        events.reload();

        TimeFormatUtil.reload();

        eventManager.load();
        schedulerService.restart();
    }

    private void initializeComponents() {
        final GeneralSettings generalSettings = GeneralSettings.builder()
                .setUseDefaults(false)
                .build();

        final LoaderSettings loaderSettings = LoaderSettings.builder()
                .setAutoUpdate(true)
                .build();

        final UpdaterSettings updaterSettings = UpdaterSettings.builder()
                .setKeepAll(true)
                .setVersioning(new BasicVersioning("version"))
                .build();

        config = loadConfig("config.yml", generalSettings, loaderSettings, updaterSettings);
        language = loadConfig("messages.yml", generalSettings, loaderSettings, updaterSettings);
        guis = loadConfig("guis.yml", generalSettings, loaderSettings, updaterSettings);
        hooks = loadConfig("hooks.yml", generalSettings, loaderSettings, updaterSettings);
        dialogs = loadConfig("dialogs.yml", generalSettings, loaderSettings, updaterSettings);
        webhooks = loadConfig("webhooks.yml", generalSettings, LoaderSettings.DEFAULT, UpdaterSettings.DEFAULT);
        events = loadConfig("events.yml", generalSettings, LoaderSettings.DEFAULT, UpdaterSettings.DEFAULT);
    }

    @NotNull
    @Contract("_, _, _, _ -> new")
    private Config loadConfig(@NotNull String fileName, @NotNull GeneralSettings generalSettings, @NotNull LoaderSettings loaderSettings, @NotNull UpdaterSettings updaterSettings) {
        return new Config(
                new File(getDataFolder(), fileName),
                getResource(fileName),
                generalSettings,
                loaderSettings,
                DumperSettings.DEFAULT,
                updaterSettings
        );
    }
}