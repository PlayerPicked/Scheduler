package com.playerpicked.scheduler.model;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public record OneTimeTask(int id, long time, @NotNull List<String> actions, @NotNull String creator) {}