package com.playerpicked.scheduler.guis.impl;

import com.playerpicked.scheduler.Scheduler;
import com.playerpicked.scheduler.data.MenuController;
import com.playerpicked.scheduler.guis.PaginatedMenu;
import com.playerpicked.scheduler.identifiers.keys.ConfigKeys;
import com.playerpicked.scheduler.identifiers.keys.DialogKeys;
import com.playerpicked.scheduler.identifiers.keys.ItemKeys;
import com.playerpicked.scheduler.identifiers.keys.MenuKeys;
import com.playerpicked.scheduler.identifiers.keys.MessageKeys;
import com.playerpicked.scheduler.item.ItemFactory;
import com.playerpicked.scheduler.manager.EventManager;
import com.playerpicked.scheduler.model.ScheduledEvent;
import com.playerpicked.scheduler.utils.EventFormatUtil;
import com.playerpicked.scheduler.utils.SoundUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class EventsMenu extends PaginatedMenu {
    private final EventManager eventManager = Scheduler.getInstance().getEventManager();

    public EventsMenu(@NotNull MenuController menuController) {
        super(menuController);
    }

    @Override
    public void handleMenu(@NotNull InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getClickedInventory() != inventory) return;

        Player player = menuController.owner();
        int slot = event.getSlot();

        List<ScheduledEvent> events = eventManager.getEvents();
        List<Integer> slots = MenuKeys.EVENTS_SLOTS.getIntList();

        if (handlePageClick(slot, events.size(), slots.size(), ItemKeys.EVENTS_PREVIOUS, ItemKeys.EVENTS_NEXT, MenuKeys.EVENTS_SOUND_PAGE.getRawString())) return;

        if (ItemKeys.EVENTS_CLOSE.isSlot(slot)) {
            player.closeInventory();
            return;
        }

        if (ItemKeys.EVENTS_CREATE.isSlot(slot)) {
            SoundUtil.play(player, MenuKeys.EVENTS_SOUND_ACTION.getRawString());
            requestCreate(player);
            return;
        }

        int index = indexAt(slot, events.size(), slots);
        if (index < 0) return;

        ScheduledEvent scheduledEvent = events.get(index);
        ClickType click = event.getClick();

        switch (click) {
            case DROP, CONTROL_DROP -> {
                eventManager.delete(scheduledEvent);
                MessageKeys.EVENT_DELETED.send(player, "{event}", scheduledEvent.getName());
                SoundUtil.play(player, MenuKeys.EVENTS_SOUND_ACTION.getRawString());
                updateMenuItems();
            }
            case SHIFT_RIGHT -> {
                SoundUtil.play(player, MenuKeys.EVENTS_SOUND_ACTION.getRawString());
                Scheduler.getScheduler().runTask(() -> {
                    boolean ran = Scheduler.getInstance().getSchedulerService().run(scheduledEvent, true);
                    (ran ? MessageKeys.EVENT_RUN : MessageKeys.EVENT_RUN_CANCELLED).send(player, "{event}", scheduledEvent.getName());
                });
            }
            case RIGHT -> {
                scheduledEvent.setEnabled(!scheduledEvent.isEnabled());
                eventManager.save(scheduledEvent);

                (scheduledEvent.isEnabled() ? MessageKeys.EVENT_ENABLED : MessageKeys.EVENT_DISABLED).send(player, "{event}", scheduledEvent.getName());
                SoundUtil.play(player, MenuKeys.EVENTS_SOUND_ACTION.getRawString());
                updateMenuItems();
            }
            default -> new EventEditorMenu(MenuController.getMenuUtils(player), scheduledEvent).open();
        }
    }

    private void requestCreate(@NotNull Player player) {
        Scheduler.getInstance().getDialogInputManager().request(player, DialogKeys.CREATE_EVENT, name -> {
            if (!name.matches(ConfigKeys.EDITOR_EVENT_NAME_PATTERN.getRawString("[A-Za-z0-9_-]{1,32}"))) {
                MessageKeys.EVENT_INVALID_NAME.send(player, "{event}", name);
                SoundUtil.play(player, MenuKeys.EVENTS_SOUND_ERROR.getRawString());
                reopen(player);
                return;
            }

            if (eventManager.exists(name)) {
                MessageKeys.EVENT_ALREADY_EXISTS.send(player, "{event}", name);
                SoundUtil.play(player, MenuKeys.EVENTS_SOUND_ERROR.getRawString());
                reopen(player);
                return;
            }

            ScheduledEvent created = eventManager.create(name);
            MessageKeys.EVENT_CREATED.send(player, "{event}", created.getName());
            new EventEditorMenu(MenuController.getMenuUtils(player), created).open();
        }, () -> reopen(player));
    }

    private void reopen(@NotNull Player player) {
        new EventsMenu(MenuController.getMenuUtils(player)).open();
    }

    @Override
    public void setMenuItems() {
        inventory.clear();
        ItemFactory.setItemsForMenu("events.items", inventory);

        renderPage(eventManager.getEvents(), MenuKeys.EVENTS_SLOTS.getIntList(), (scheduledEvent, index) -> {
            ItemStack item = ItemKeys.EVENTS_EVENT.getItem(EventFormatUtil.placeholders(scheduledEvent));
            return item != null && !scheduledEvent.isEnabled() ? item.withType(disabledMaterial()) : item;
        }, ItemKeys.EVENTS_EMPTY, ItemKeys.EVENTS_PREVIOUS, ItemKeys.EVENTS_NEXT);
    }

    @NotNull
    private static Material disabledMaterial() {
        Material material = Material.matchMaterial(Scheduler.getInstance().getGuis().getString("events.event.disabled-material", "GRAY_DYE"));
        return material == null ? Material.GRAY_DYE : material;
    }

    @Override
    public void open() {
        super.open();
        if (inventory != null) SoundUtil.play(menuController.owner(), MenuKeys.EVENTS_SOUND_OPEN.getRawString());
    }

    @NotNull
    @Override
    public String getMenuName() {
        return MenuKeys.EVENTS_TITLE.getRawString();
    }

    @Override
    public int getSlots() {
        return MenuKeys.EVENTS_SIZE.getInt();
    }

    @Override
    public int getMenuTick() {
        return MenuKeys.EVENTS_TICK.getInt();
    }
}