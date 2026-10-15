package com.playerpicked.scheduler.item;

import com.playerpicked.scheduler.processor.MessageProcessor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("all")
public class ItemBuilder implements ItemFactory {
    private final ItemStack is;
    private final ItemMeta meta;
    private int slot;
    private boolean finished = false;

    public ItemBuilder(@NotNull ItemStack item) {
        is = item;
        meta = item.getItemMeta();
    }

    ItemBuilder(@NotNull Material type) {
        this(type, 1);
    }

    public ItemBuilder(@NotNull Material type, @Range(from = 0, to = 64) int amount) {
        this.is = new ItemStack(type, Math.max(1, Math.min(64, amount)));
        this.meta = is.getItemMeta();
    }

    public ItemBuilder(@NotNull Material type, @Range(from = 0, to = 64) int amount, short damage) {
        this(type, amount);
        applyDamageSafely(damage);
    }

    public ItemBuilder(@NotNull Material type, @Range(from = 0, to = 64) int amount, short damage, @Nullable Byte data) {
        this(type, amount, damage);
    }

    private void applyDamageSafely(int damage) {
        if (damage <= 0) return;
        if (meta instanceof Damageable dmg) {
            dmg.setDamage(damage);
        }
    }

    @Override
    public ItemBuilder setType(@NotNull Material material) {
        is.setType(material);
        return this;
    }

    @Override
    public ItemBuilder setCount(@Range(from = 0, to = 64) int newCount) {
        is.setAmount(newCount);
        return this;
    }

    @Override
    public ItemBuilder setName(@NotNull String name) {
        meta.setDisplayName(MessageProcessor.process(name));
        return this;
    }

    @Override
    public ItemBuilder setLore(@NotNull List<String> lore) {
        meta.setLore(lore);
        return this;
    }

    @Override
    public void addEnchantment(@NotNull Enchantment enchantment, int level) {
        meta.addEnchant(enchantment, level, true);
    }

    @Override
    public ItemBuilder addLore(@NotNull String... lores) {
        List<String> loreList = Arrays.asList(lores);
        List<String> currentLores = meta.getLore();
        currentLores = currentLores == null ? Collections.synchronizedList(new ArrayList<>()) : currentLores;

        currentLores.addAll(loreList);
        meta.setLore(currentLores);

        return this;
    }

    @Override
    public ItemBuilder setUnbreakable() {
        meta.setUnbreakable(true);

        return this;
    }

    public ItemBuilder addFlag(@NotNull ItemFlag flag) {
        meta.addItemFlags(flag);

        return this;
    }

    @Override
    public ItemBuilder removeLore(int line) {
        List<String> lores = meta.getLore();
        lores = lores == null ? Collections.synchronizedList(new ArrayList<>()) : lores;

        lores.remove(Math.min(line, lores.size()));
        meta.setLore(lores);

        return this;
    }

    @Override
    public ItemStack finish() {
        is.setItemMeta(meta);

        finished = true;
        return is;
    }

    @Override
    public boolean isFinished() {
        return finished;
    }
}