package com.playerpicked.scheduler.handler;

import com.playerpicked.scheduler.identifiers.keys.MessageKeys;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.bukkit.exception.BukkitExceptionHandler;
import revxrsal.commands.bukkit.exception.SenderNotPlayerException;
import revxrsal.commands.exception.MissingArgumentException;
import revxrsal.commands.exception.NoPermissionException;
import revxrsal.commands.node.ParameterNode;

public final class CommandExceptionHandler extends BukkitExceptionHandler {

    @Override
    public void onSenderNotPlayer(@NotNull SenderNotPlayerException exception, @NotNull BukkitCommandActor actor) {
        MessageKeys.PLAYER_REQUIRED.send(actor.sender());
    }

    @Override
    public void onNoPermission(@NotNull NoPermissionException exception, @NotNull BukkitCommandActor actor) {
        MessageKeys.NO_PERMISSION.send(actor.sender());
    }

    @Override
    public void onMissingArgument(@NotNull MissingArgumentException exception, @NotNull BukkitCommandActor actor, @NotNull ParameterNode<BukkitCommandActor, ?> parameter) {
        MessageKeys.MISSING_ARGUMENT.send(actor.sender(), "{argument}", parameter.name());
    }
}