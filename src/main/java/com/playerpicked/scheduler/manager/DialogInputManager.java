package com.playerpicked.scheduler.manager;

import com.playerpicked.scheduler.Scheduler;
import com.playerpicked.scheduler.config.Config;
import com.playerpicked.scheduler.identifiers.keys.DialogKeys;
import com.playerpicked.scheduler.identifiers.keys.MessageKeys;
import com.playerpicked.scheduler.processor.MessageProcessor;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

@SuppressWarnings("all")
public final class DialogInputManager {
    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacySection();
    private static final String INPUT_KEY = "input";

    public void request(@NotNull Player player, @NotNull DialogKeys key, @NotNull Consumer<String> callback, @NotNull Runnable onCancel, @NotNull String... replacements) {
        Config config = Scheduler.getInstance().getDialogs();
        String path = key.getPath();

        String[] placeholders = new String[replacements.length + 2];
        placeholders[0] = "{player}";
        placeholders[1] = player.getName();
        System.arraycopy(replacements, 0, placeholders, 2, replacements.length);

        ClickCallback.Options options = ClickCallback.Options.builder()
                .uses(1)
                .lifetime(Duration.ofSeconds(Math.max(1, config.getLong("settings.timeout", 300L))))
                .build();

        DialogActionCallback confirmAction = (view, audience) -> {
            String text = view.getText(INPUT_KEY);
            String value = text == null ? "" : text.trim();

            Scheduler.getScheduler().runTask(player, () -> {
                if (value.isEmpty()) cancelled(player, onCancel);
                else callback.accept(value);
            });
        };

        DialogActionCallback cancelAction = (view, audience) -> Scheduler.getScheduler().runTask(player, () -> cancelled(player, onCancel));

        DialogBase base = DialogBase.builder(text(config.getString(path + ".title", ""), placeholders))
                .externalTitle(optional(config.getString(path + ".external-title", ""), placeholders))
                .canCloseWithEscape(config.getBoolean(path + ".can-close-with-escape", true))
                .pause(config.getBoolean(path + ".pause", false))
                .afterAction(afterAction(config.getString(path + ".after-action", "CLOSE")))
                .body(body(config, path, placeholders))
                .inputs(List.of(input(config, path + ".input", placeholders)))
                .build();

        Dialog dialog = Dialog.create(factory -> factory.empty()
                .base(base)
                .type(DialogType.confirmation(
                        button(config, path + ".confirm", DialogAction.customClick(confirmAction, options), placeholders),
                        button(config, path + ".cancel", DialogAction.customClick(cancelAction, options), placeholders))));

        player.closeInventory();
        player.showDialog(dialog);
    }

    private void cancelled(@NotNull Player player, @NotNull Runnable onCancel) {
        if (Scheduler.getInstance().getDialogs().getBoolean("settings.cancel-message", true)) MessageKeys.INPUT_CANCELLED.send(player);
        onCancel.run();
    }

    @NotNull
    private @Unmodifiable List<DialogBody> body(@NotNull Config config, @NotNull String path, @NotNull String[] placeholders) {
        Object raw = config.get(path + ".body");

        List<String> lines;
        if (raw instanceof List<?> list) lines = list.stream().map(String::valueOf).toList();
        else if (raw instanceof String string && !string.isEmpty()) lines = List.of(string);
        else return List.of();

        if (lines.isEmpty()) return List.of();

        Component contents = text(String.join("\n", lines), placeholders);
        return List.of(DialogBody.plainMessage(contents, width(config.getInt(path + ".body-width", 200))));
    }

    @NotNull
    private TextDialogInput input(@NotNull Config config, @NotNull String path, @NotNull String[] placeholders) {
        TextDialogInput.Builder builder = DialogInput.text(INPUT_KEY, text(config.getString(path + ".label", ""), placeholders))
                .labelVisible(config.getBoolean(path + ".label-visible", true))
                .width(width(config.getInt(path + ".width", 200)))
                .maxLength(Math.max(1, config.getInt(path + ".max-length", 32)))
                .initial(replace(stripped(config.getString(path + ".initial", ""), new String[0]), placeholders));

        if (config.getBoolean(path + ".multiline.enabled", false)) {
            builder.multiline(TextDialogInput.MultilineOptions.create(
                    Math.max(1, config.getInt(path + ".multiline.max-lines", 3)),
                    Math.clamp(config.getInt(path + ".multiline.height", 64), 1, 512)));
        }

        return builder.build();
    }

    @NotNull
    private ActionButton button(@NotNull Config config, @NotNull String path, @NotNull DialogAction action, @NotNull String[] placeholders) {
        return ActionButton.builder(text(config.getString(path + ".label", ""), placeholders))
                .tooltip(optional(config.getString(path + ".tooltip", ""), placeholders))
                .width(width(config.getInt(path + ".width", 100)))
                .action(action)
                .build();
    }

    @NotNull
    private static DialogBase.DialogAfterAction afterAction(@NotNull String raw) {
        try {
            return DialogBase.DialogAfterAction.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return DialogBase.DialogAfterAction.CLOSE;
        }
    }

    private static int width(int width) {
        return Math.clamp(width, 1, 1024);
    }

    @NotNull
    private static Component text(@Nullable String raw, @NotNull String[] placeholders) {
        return SERIALIZER.deserialize(MessageProcessor.process(replace(raw, placeholders)));
    }

    @Nullable
    private static Component optional(@Nullable String raw, @NotNull String[] placeholders) {
        return raw == null || raw.isEmpty() ? null : text(raw, placeholders);
    }

    @NotNull
    private static String stripped(@Nullable String raw, @NotNull String[] placeholders) {
        return SERIALIZER.serialize(text(raw, placeholders)).replaceAll("§.", "");
    }

    @NotNull
    private static String replace(@Nullable String raw, @NotNull String[] placeholders) {
        if (raw == null) return "";

        String result = raw;
        for (int index = 0; index + 1 < placeholders.length; index += 2) result = result.replace(placeholders[index], placeholders[index + 1]);
        return result;
    }
}