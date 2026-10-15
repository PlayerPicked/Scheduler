package com.playerpicked.scheduler.guis.impl;

import com.playerpicked.scheduler.Scheduler;
import com.playerpicked.scheduler.data.MenuController;
import com.playerpicked.scheduler.guis.EventBoundMenu;
import com.playerpicked.scheduler.identifiers.ActionListType;
import com.playerpicked.scheduler.identifiers.keys.DialogKeys;
import com.playerpicked.scheduler.identifiers.keys.ItemKeys;
import com.playerpicked.scheduler.identifiers.keys.MenuKeys;
import com.playerpicked.scheduler.identifiers.keys.MessageKeys;
import com.playerpicked.scheduler.item.ItemFactory;
import com.playerpicked.scheduler.model.ScheduledEvent;
import com.playerpicked.scheduler.utils.EventFormatUtil;
import com.playerpicked.scheduler.utils.SoundUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

public final class EventEditorMenu extends EventBoundMenu {

    public EventEditorMenu(@NotNull MenuController menuController, @NotNull ScheduledEvent event) {
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

        if (ItemKeys.EDITOR_BACK.isSlot(slot)) {
            new EventsMenu(MenuController.getMenuUtils(player)).open();
            return;
        }

        if (ItemKeys.EDITOR_TIMES.isSlot(slot)) {
            new TimesMenu(MenuController.getMenuUtils(player), scheduledEvent).open();
            return;
        }

        if (ItemKeys.EDITOR_ACTIONS.isSlot(slot)) {
            new ActionsMenu(MenuController.getMenuUtils(player), scheduledEvent, ActionListType.ACTIONS).open();
            return;
        }

        if (ItemKeys.EDITOR_NOT_ENOUGH_PLAYERS_ACTIONS.isSlot(slot)) {
            new ActionsMenu(MenuController.getMenuUtils(player), scheduledEvent, ActionListType.NOT_ENOUGH_PLAYERS).open();
            return;
        }

        if (ItemKeys.EDITOR_REQUIRED_PLAYERS.isSlot(slot)) {
            SoundUtil.play(player, MenuKeys.EDITOR_SOUND_ACTION.getRawString());

            if (event.isRightClick()) {
                scheduledEvent.setRequiredPlayers(-1);
                save(scheduledEvent);
                MessageKeys.EVENT_REQUIRED_PLAYERS_REMOVED.send(player, "{event}", scheduledEvent.getName());
                updateMenuItems();
                return;
            }

            requestRequiredPlayers(player, scheduledEvent);
            return;
        }

        if (ItemKeys.EDITOR_ENABLED.isSlot(slot) || ItemKeys.EDITOR_DISABLED.isSlot(slot)) {
            scheduledEvent.setEnabled(!scheduledEvent.isEnabled());
            save(scheduledEvent);

            (scheduledEvent.isEnabled() ? MessageKeys.EVENT_ENABLED : MessageKeys.EVENT_DISABLED).send(player, "{event}", scheduledEvent.getName());
            SoundUtil.play(player, MenuKeys.EDITOR_SOUND_ACTION.getRawString());
            updateMenuItems();
            return;
        }

        if (ItemKeys.EDITOR_RUN.isSlot(slot)) {
            SoundUtil.play(player, MenuKeys.EDITOR_SOUND_ACTION.getRawString());
            Scheduler.getScheduler().runTask(() -> {
                boolean ran = Scheduler.getInstance().getSchedulerService().run(scheduledEvent, true);
                (ran ? MessageKeys.EVENT_RUN : MessageKeys.EVENT_RUN_CANCELLED).send(player, "{event}", scheduledEvent.getName());
            });
        }
    }

    private void requestRequiredPlayers(@NotNull Player player, @NotNull ScheduledEvent scheduledEvent) {
        Scheduler.getInstance().getDialogInputManager().request(player, DialogKeys.REQUIRED_PLAYERS, input -> {
            int amount;

            try {
                amount = Integer.parseInt(input);
            } catch (NumberFormatException exception) {
                amount = -1;
            }

            ScheduledEvent current = event();
            if (current == null) return;

            if (amount < 1) {
                MessageKeys.EVENT_INVALID_NUMBER.send(player, "{input}", input);
                SoundUtil.play(player, MenuKeys.EDITOR_SOUND_ERROR.getRawString());
            } else {
                current.setRequiredPlayers(amount);
                save(current);
                MessageKeys.EVENT_REQUIRED_PLAYERS_SET.send(player, "{event}", current.getName(), "{amount}", String.valueOf(amount));
            }

            reopen(player, current);
        }, () -> reopen(player, scheduledEvent), "{event}", scheduledEvent.getName(), "{value}", String.valueOf(Math.max(1, scheduledEvent.getRequiredPlayers())));
    }

    private void reopen(@NotNull Player player, @NotNull ScheduledEvent scheduledEvent) {
        new EventEditorMenu(MenuController.getMenuUtils(player), scheduledEvent).open();
    }

    @Override
    public void setMenuItems() {
        ScheduledEvent scheduledEvent = event();
        if (scheduledEvent == null) return;

        inventory.clear();
        ItemFactory.setItemsForMenu("event-editor.items", inventory);

        String[] placeholders = EventFormatUtil.placeholders(scheduledEvent);

        place(ItemKeys.EDITOR_TIMES, ItemKeys.EDITOR_TIMES.getItem(placeholders));
        place(ItemKeys.EDITOR_ACTIONS, ItemKeys.EDITOR_ACTIONS.getItem(placeholders));
        place(ItemKeys.EDITOR_NOT_ENOUGH_PLAYERS_ACTIONS, ItemKeys.EDITOR_NOT_ENOUGH_PLAYERS_ACTIONS.getItem(placeholders));
        place(ItemKeys.EDITOR_REQUIRED_PLAYERS, ItemKeys.EDITOR_REQUIRED_PLAYERS.getItem(placeholders));
        place(ItemKeys.EDITOR_RUN, ItemKeys.EDITOR_RUN.getItem(placeholders));

        ItemKeys status = scheduledEvent.isEnabled() ? ItemKeys.EDITOR_ENABLED : ItemKeys.EDITOR_DISABLED;
        place(status, status.getItem(placeholders));
    }

    @Override
    public void open() {
        super.open();
        if (inventory != null) SoundUtil.play(menuController.owner(), MenuKeys.EDITOR_SOUND_OPEN.getRawString());
    }

    @NotNull
    @Override
    public String getMenuName() {
        return MenuKeys.EDITOR_TITLE.getRawString().replace("{event}", eventName);
    }

    @Override
    public int getSlots() {
        return MenuKeys.EDITOR_SIZE.getInt();
    }

    @Override
    public int getMenuTick() {
        return MenuKeys.EDITOR_TICK.getInt();
    }
}