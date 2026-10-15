package com.playerpicked.scheduler.identifiers.keys;

import com.playerpicked.scheduler.Scheduler;
import com.playerpicked.scheduler.item.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
public enum ItemKeys {

    EVENTS_EVENT("events.event"),
    EVENTS_EMPTY("events.empty"),
    EVENTS_PREVIOUS("events.previous"),
    EVENTS_NEXT("events.next"),
    EVENTS_CREATE("events.items.create"),
    EVENTS_CLOSE("events.items.close"),

    EDITOR_TIMES("event-editor.times"),
    EDITOR_ACTIONS("event-editor.actions"),
    EDITOR_NOT_ENOUGH_PLAYERS_ACTIONS("event-editor.not-enough-players-actions"),
    EDITOR_REQUIRED_PLAYERS("event-editor.required-players"),
    EDITOR_ENABLED("event-editor.enabled"),
    EDITOR_DISABLED("event-editor.disabled"),
    EDITOR_RUN("event-editor.run"),
    EDITOR_BACK("event-editor.items.back"),

    TIMES_TIME("times.time"),
    TIMES_EMPTY("times.empty"),
    TIMES_PREVIOUS("times.previous"),
    TIMES_NEXT("times.next"),
    TIMES_CREATE("times.items.create"),
    TIMES_BACK("times.items.back"),

    ACTIONS_ACTION("actions.action"),
    ACTIONS_EMPTY("actions.empty"),
    ACTIONS_PREVIOUS("actions.previous"),
    ACTIONS_NEXT("actions.next"),
    ACTIONS_CREATE("actions.items.create"),
    ACTIONS_BACK("actions.items.back");

    private final String path;

    ItemKeys(@NotNull final String path) {
        this.path = path;
    }

    public List<Integer> getSlots() {
        var config = Scheduler.getInstance().getGuis();
        Object raw = config.get(path + ".slot");

        if (raw instanceof Integer i) {
            return List.of(i);
        }

        if (raw instanceof List<?> list) {
            return list.stream()
                    .filter(Integer.class::isInstance)
                    .map(Integer.class::cast)
                    .toList();
        }

        return List.of();
    }

    public boolean isSlot(int slot) {
        return getSlots().contains(slot);
    }

    @Nullable
    public ItemStack getItem(@NotNull String... replacements) {
        ItemStack item = ItemFactory.createItemFromString(path, Scheduler.getInstance().getGuis()).orElse(null);
        if (item == null || replacements.length < 2) return item;

        item.editMeta(meta -> applyReplacements(meta, replacements));
        return item;
    }

    private static void applyReplacements(@NotNull ItemMeta meta, @NotNull String... replacements) {
        if (meta.hasDisplayName()) meta.setDisplayName(replace(meta.getDisplayName(), replacements));

        List<String> lore = meta.getLore();
        if (lore == null) return;

        List<String> replaced = new ArrayList<>(lore.size());
        for (String line : lore) replaced.add(replace(line, replacements));
        meta.setLore(replaced);
    }

    @NotNull
    private static String replace(@NotNull String text, @NotNull String... replacements) {
        for (int index = 0; index + 1 < replacements.length; index += 2) text = text.replace(replacements[index], replacements[index + 1]);
        return text;
    }
}