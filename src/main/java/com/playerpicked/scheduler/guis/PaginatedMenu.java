package com.playerpicked.scheduler.guis;

import com.playerpicked.scheduler.data.MenuController;
import com.playerpicked.scheduler.identifiers.keys.ItemKeys;
import com.playerpicked.scheduler.utils.SoundUtil;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BiFunction;

public abstract class PaginatedMenu extends Menu {
    protected int page = 0;

    public PaginatedMenu(@NotNull MenuController menuController) {
        super(menuController);
    }

    protected <T> void renderPage(@NotNull List<T> entries, @NotNull List<Integer> slots, @NotNull BiFunction<T, Integer, ItemStack> mapper,
                                  @NotNull ItemKeys empty, @NotNull ItemKeys previous, @NotNull ItemKeys next) {
        if (slots.isEmpty()) return;

        int lastPage = lastPage(entries.size(), slots.size());
        page = Math.clamp(page, 0, lastPage);

        if (entries.isEmpty()) {
            place(empty, empty.getItem());
            return;
        }

        int start = page * slots.size();
        for (int index = 0; index < slots.size() && start + index < entries.size(); index++) {
            int slot = slots.get(index);
            if (slot < 0 || slot >= inventory.getSize()) continue;

            inventory.setItem(slot, mapper.apply(entries.get(start + index), start + index));
        }

        String[] placeholders = {"{page}", String.valueOf(page + 1), "{pages}", String.valueOf(lastPage + 1)};
        if (page > 0) place(previous, previous.getItem(placeholders));
        if (page < lastPage) place(next, next.getItem(placeholders));
    }

    protected boolean handlePageClick(int slot, int total, int perPage, @NotNull ItemKeys previous, @NotNull ItemKeys next, @Nullable String sound) {
        int lastPage = lastPage(total, perPage);

        if (page > 0 && previous.isSlot(slot)) page--;
        else if (page < lastPage && next.isSlot(slot)) page++;
        else return false;

        SoundUtil.play(menuController.owner(), sound);
        updateMenuItems();
        return true;
    }

    protected int indexAt(int slot, int total, @NotNull List<Integer> slots) {
        int position = slots.indexOf(slot);
        if (position < 0) return -1;

        int index = page * slots.size() + position;
        return index < total ? index : -1;
    }

    protected void place(@NotNull ItemKeys key, @Nullable ItemStack item) {
        if (item == null) return;

        for (int slot : key.getSlots()) {
            if (slot >= 0 && slot < inventory.getSize()) inventory.setItem(slot, item);
        }
    }

    private static int lastPage(int total, int perPage) {
        if (perPage <= 0 || total <= 0) return 0;
        return (total - 1) / perPage;
    }
}