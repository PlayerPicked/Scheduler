package com.playerpicked.scheduler.guis.impl;

import com.playerpicked.scheduler.Scheduler;
import com.playerpicked.scheduler.data.MenuController;
import com.playerpicked.scheduler.guis.EventBoundMenu;
import com.playerpicked.scheduler.identifiers.keys.DialogKeys;
import com.playerpicked.scheduler.identifiers.keys.ItemKeys;
import com.playerpicked.scheduler.identifiers.keys.MenuKeys;
import com.playerpicked.scheduler.identifiers.keys.MessageKeys;
import com.playerpicked.scheduler.item.ItemFactory;
import com.playerpicked.scheduler.model.ScheduledEvent;
import com.playerpicked.scheduler.utils.CronUtil;
import com.playerpicked.scheduler.utils.SoundUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class TimesMenu extends EventBoundMenu {

    public TimesMenu(@NotNull MenuController menuController, @NotNull ScheduledEvent event) {
        super(menuController, event);
    }

    @Override
    public void handleMenu(@NotNull InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getClickedInventory() != inventory) return;

        ScheduledEvent scheduledEvent = eventOrClose();
        if (scheduledEvent == null) return;

        Player player = menuController.owner();
        int slot = event.getSlot();

        List<String> times = scheduledEvent.getTimes();
        List<Integer> slots = MenuKeys.TIMES_SLOTS.getIntList();

        if (handlePageClick(slot, times.size(), slots.size(), ItemKeys.TIMES_PREVIOUS, ItemKeys.TIMES_NEXT, MenuKeys.TIMES_SOUND_PAGE.getRawString())) return;

        if (ItemKeys.TIMES_BACK.isSlot(slot)) {
            new EventEditorMenu(MenuController.getMenuUtils(player), scheduledEvent).open();
            return;
        }

        if (ItemKeys.TIMES_CREATE.isSlot(slot)) {
            SoundUtil.play(player, MenuKeys.TIMES_SOUND_ACTION.getRawString());
            requestTime(player, scheduledEvent, null);
            return;
        }

        int index = indexAt(slot, times.size(), slots);
        if (index < 0) return;

        String time = times.get(index);

        if (event.getClick() == ClickType.DROP || event.getClick() == ClickType.CONTROL_DROP) {
            times.remove(index);
            save(scheduledEvent);

            MessageKeys.TIME_DELETED.send(player, "{event}", scheduledEvent.getName(), "{time}", time);
            SoundUtil.play(player, MenuKeys.TIMES_SOUND_ACTION.getRawString());
            updateMenuItems();
            return;
        }

        SoundUtil.play(player, MenuKeys.TIMES_SOUND_ACTION.getRawString());
        requestTime(player, scheduledEvent, time);
    }

    private void requestTime(@NotNull Player player, @NotNull ScheduledEvent scheduledEvent, @Nullable String original) {
        DialogKeys dialog = original == null ? DialogKeys.ADD_TIME : DialogKeys.EDIT_TIME;

        Scheduler.getInstance().getDialogInputManager().request(player, dialog, input -> {
            ScheduledEvent current = event();
            if (current == null) return;

            List<String> times = current.getTimes();

            if (!CronUtil.isValid(input)) {
                MessageKeys.TIME_INVALID.send(player, "{time}", input);
                SoundUtil.play(player, MenuKeys.TIMES_SOUND_ERROR.getRawString());
            } else if (times.contains(input) && !input.equals(original)) {
                MessageKeys.TIME_ALREADY_EXISTS.send(player, "{time}", input);
                SoundUtil.play(player, MenuKeys.TIMES_SOUND_ERROR.getRawString());
            } else if (original == null) {
                times.add(input);
                save(current);
                MessageKeys.TIME_ADDED.send(player, "{event}", current.getName(), "{time}", input, "{next_run}", CronUtil.formatNextRun(input));
            } else {
                int index = times.indexOf(original);
                if (index >= 0) times.set(index, input);
                else times.add(input);

                save(current);
                MessageKeys.TIME_EDITED.send(player, "{event}", current.getName(), "{old}", original, "{time}", input, "{next_run}", CronUtil.formatNextRun(input));
            }

            reopen(player, current);
        }, () -> reopen(player, scheduledEvent), "{event}", scheduledEvent.getName(), "{value}", original == null ? "" : original);
    }

    private void reopen(@NotNull Player player, @NotNull ScheduledEvent scheduledEvent) {
        TimesMenu menu = new TimesMenu(MenuController.getMenuUtils(player), scheduledEvent);
        menu.page = page;
        menu.open();
    }

    @Override
    public void setMenuItems() {
        ScheduledEvent scheduledEvent = event();
        if (scheduledEvent == null) return;

        inventory.clear();
        ItemFactory.setItemsForMenu("times.items", inventory);

        renderPage(scheduledEvent.getTimes(), MenuKeys.TIMES_SLOTS.getIntList(), (time, index) -> ItemKeys.TIMES_TIME.getItem(
                "{event}", scheduledEvent.getName(),
                "{time}", time,
                "{time_next_run}", CronUtil.formatNextRun(time),
                "{index}", String.valueOf(index + 1)
        ), ItemKeys.TIMES_EMPTY, ItemKeys.TIMES_PREVIOUS, ItemKeys.TIMES_NEXT);
    }

    @Override
    public void open() {
        super.open();
        if (inventory != null) SoundUtil.play(menuController.owner(), MenuKeys.TIMES_SOUND_OPEN.getRawString());
    }

    @NotNull
    @Override
    public String getMenuName() {
        return MenuKeys.TIMES_TITLE.getRawString().replace("{event}", eventName);
    }

    @Override
    public int getSlots() {
        return MenuKeys.TIMES_SIZE.getInt();
    }

    @Override
    public int getMenuTick() {
        return MenuKeys.TIMES_TICK.getInt();
    }
}