package com.playerpicked.scheduler.guis.impl;

import com.playerpicked.scheduler.Scheduler;
import com.playerpicked.scheduler.data.MenuController;
import com.playerpicked.scheduler.guis.EventBoundMenu;
import com.playerpicked.scheduler.identifiers.ActionListType;
import com.playerpicked.scheduler.identifiers.ActionType;
import com.playerpicked.scheduler.identifiers.keys.DialogKeys;
import com.playerpicked.scheduler.identifiers.keys.ItemKeys;
import com.playerpicked.scheduler.identifiers.keys.MenuKeys;
import com.playerpicked.scheduler.identifiers.keys.MessageKeys;
import com.playerpicked.scheduler.item.ItemFactory;
import com.playerpicked.scheduler.model.ScheduledEvent;
import com.playerpicked.scheduler.utils.SoundUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public final class ActionsMenu extends EventBoundMenu {
    private final ActionListType type;

    public ActionsMenu(@NotNull MenuController menuController, @NotNull ScheduledEvent event, @NotNull ActionListType type) {
        super(menuController, event);
        this.type = type;
    }

    @Override
    public void handleMenu(@NotNull InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getClickedInventory() != inventory) return;

        ScheduledEvent scheduledEvent = eventOrClose();
        if (scheduledEvent == null) return;

        Player player = menuController.owner();
        int slot = event.getSlot();

        List<String> actions = scheduledEvent.getActions(type);
        List<Integer> slots = MenuKeys.ACTIONS_SLOTS.getIntList();

        if (handlePageClick(slot, actions.size(), slots.size(), ItemKeys.ACTIONS_PREVIOUS, ItemKeys.ACTIONS_NEXT, MenuKeys.ACTIONS_SOUND_PAGE.getRawString())) return;

        if (ItemKeys.ACTIONS_BACK.isSlot(slot)) {
            new EventEditorMenu(MenuController.getMenuUtils(player), scheduledEvent).open();
            return;
        }

        if (ItemKeys.ACTIONS_CREATE.isSlot(slot)) {
            SoundUtil.play(player, MenuKeys.ACTIONS_SOUND_ACTION.getRawString());
            requestAction(player, scheduledEvent, -1);
            return;
        }

        int index = indexAt(slot, actions.size(), slots);
        if (index < 0) return;

        switch (event.getClick()) {
            case DROP, CONTROL_DROP -> {
                String removed = actions.remove(index);
                save(scheduledEvent);

                MessageKeys.ACTION_DELETED.send(player, "{event}", scheduledEvent.getName(), "{action}", removed);
                SoundUtil.play(player, MenuKeys.ACTIONS_SOUND_ACTION.getRawString());
                updateMenuItems();
            }
            case SHIFT_LEFT -> move(scheduledEvent, actions, index, index - 1);
            case SHIFT_RIGHT -> move(scheduledEvent, actions, index, index + 1);
            default -> {
                SoundUtil.play(player, MenuKeys.ACTIONS_SOUND_ACTION.getRawString());
                requestAction(player, scheduledEvent, index);
            }
        }
    }

    private void move(@NotNull ScheduledEvent scheduledEvent, @NotNull List<String> actions, int from, int to) {
        if (to < 0 || to >= actions.size()) {
            SoundUtil.play(menuController.owner(), MenuKeys.ACTIONS_SOUND_ERROR.getRawString());
            return;
        }

        Collections.swap(actions, from, to);
        save(scheduledEvent);

        SoundUtil.play(menuController.owner(), MenuKeys.ACTIONS_SOUND_ACTION.getRawString());
        updateMenuItems();
    }

    private void requestAction(@NotNull Player player, @NotNull ScheduledEvent scheduledEvent, int index) {
        List<String> actions = scheduledEvent.getActions(type);
        String original = index >= 0 && index < actions.size() ? actions.get(index) : null;
        DialogKeys dialog = original == null ? DialogKeys.ADD_ACTION : DialogKeys.EDIT_ACTION;

        Scheduler.getInstance().getDialogInputManager().request(player, dialog, input -> {
            ScheduledEvent current = event();
            if (current == null) return;

            List<String> currentActions = current.getActions(type);

            if (ActionType.parse(input) == null) {
                MessageKeys.ACTION_INVALID.send(player, "{action}", input);
                SoundUtil.play(player, MenuKeys.ACTIONS_SOUND_ERROR.getRawString());
            } else if (original == null) {
                currentActions.add(input);
                save(current);
                MessageKeys.ACTION_ADDED.send(player, "{event}", current.getName(), "{action}", input);
            } else {
                int currentIndex = currentActions.indexOf(original);
                if (currentIndex >= 0) currentActions.set(currentIndex, input);
                else currentActions.add(input);

                save(current);
                MessageKeys.ACTION_EDITED.send(player, "{event}", current.getName(), "{old}", original, "{action}", input);
            }

            reopen(player, current);
        }, () -> reopen(player, scheduledEvent), "{event}", scheduledEvent.getName(), "{value}", original == null ? "" : original);
    }

    private void reopen(@NotNull Player player, @NotNull ScheduledEvent scheduledEvent) {
        ActionsMenu menu = new ActionsMenu(MenuController.getMenuUtils(player), scheduledEvent, type);
        menu.page = page;
        menu.open();
    }

    @Override
    public void setMenuItems() {
        ScheduledEvent scheduledEvent = event();
        if (scheduledEvent == null) return;

        inventory.clear();
        ItemFactory.setItemsForMenu("actions.items", inventory);

        renderPage(scheduledEvent.getActions(type), MenuKeys.ACTIONS_SLOTS.getIntList(), (action, index) -> ItemKeys.ACTIONS_ACTION.getItem(
                "{event}", scheduledEvent.getName(),
                "{action}", action,
                "{action_type}", actionType(action),
                "{index}", String.valueOf(index + 1)
        ), ItemKeys.ACTIONS_EMPTY, ItemKeys.ACTIONS_PREVIOUS, ItemKeys.ACTIONS_NEXT);
    }

    @NotNull
    private static String actionType(@NotNull String action) {
        ActionType type = ActionType.parse(action);
        return type == null ? "Unknown" : type.getDisplayName();
    }

    @NotNull
    private String listName() {
        return switch (type) {
            case ACTIONS -> MenuKeys.ACTIONS_LIST_NAME_ACTIONS.getRawString();
            case NOT_ENOUGH_PLAYERS -> MenuKeys.ACTIONS_LIST_NAME_NOT_ENOUGH_PLAYERS.getRawString();
        };
    }

    @Override
    public void open() {
        super.open();
        if (inventory != null) SoundUtil.play(menuController.owner(), MenuKeys.ACTIONS_SOUND_OPEN.getRawString());
    }

    @NotNull
    @Override
    public String getMenuName() {
        return MenuKeys.ACTIONS_TITLE.getRawString()
                .replace("{event}", eventName)
                .replace("{list}", listName());
    }

    @Override
    public int getSlots() {
        return MenuKeys.ACTIONS_SIZE.getInt();
    }

    @Override
    public int getMenuTick() {
        return MenuKeys.ACTIONS_TICK.getInt();
    }
}