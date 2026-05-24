package io.github.naimjeg.damagenexus.command;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public final class CommandFeedback {

    public static final String PREFIX = "[DamageNexus] ";

    private CommandFeedback() {
    }

    public static int success(CommandSourceStack source, String message) {
        source.sendSuccess(
                () -> Component.literal(PREFIX + message),
                true
        );
        return 1;
    }

    public static int silentSuccess(CommandSourceStack source, String message) {
        source.sendSuccess(
                () -> Component.literal(PREFIX + message),
                false
        );
        return 1;
    }

    public static int fail(CommandSourceStack source, String message) {
        source.sendFailure(Component.literal(PREFIX + message));
        return 0;
    }

    public static Optional<ServerPlayer> requirePlayer(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return Optional.of(player);
        }

        source.sendFailure(Component.literal(PREFIX + "This command must be run by a player."));
        return Optional.empty();
    }
}
