package com.playerpicked.scheduler.model;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public record RandomGroup(int pick, @NotNull List<Entry> entries) {

    public record Entry(@NotNull String name, double weight, @NotNull List<String> actions) {}

    @NotNull
    public List<String> roll() {
        List<Entry> pool = new ArrayList<>(entries.stream().filter(entry -> entry.weight() > 0).toList());
        List<String> result = new ArrayList<>();

        for (int round = 0; round < pick && !pool.isEmpty(); round++) {
            double total = pool.stream().mapToDouble(Entry::weight).sum();
            double roll = ThreadLocalRandom.current().nextDouble(total);

            for (int index = 0; index < pool.size(); index++) {
                Entry entry = pool.get(index);
                roll -= entry.weight();

                if (roll < 0 || index == pool.size() - 1) {
                    result.addAll(entry.actions());
                    pool.remove(index);
                    break;
                }
            }
        }

        return result;
    }
}