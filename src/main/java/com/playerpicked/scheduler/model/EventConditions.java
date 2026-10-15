package com.playerpicked.scheduler.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.List;

public record EventConditions(@NotNull List<String> placeholders, @Nullable LocalDateTime from, @Nullable LocalDateTime to) {

    public static final EventConditions NONE = new EventConditions(List.of(), null, null);
}