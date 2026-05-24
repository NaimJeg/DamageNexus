package io.github.naimjeg.damagenexus.command;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.function.IntSupplier;

public final class CommandFeedback {
    private static final ThreadLocal<Integer> SUPPRESSED_SUCCESS_DEPTH =
            ThreadLocal.withInitial(() -> 0);

    private CommandFeedback() {
    }

    public static int success(
            CommandSourceStack source,
            String translationKey,
            Object... arguments
    ) {
        if (SUPPRESSED_SUCCESS_DEPTH.get() == 0) {
            source.sendSuccess(
                    () -> message(translationKey, arguments),
                    false
            );
        }
        return 1;
    }

    public static int successCount(
            CommandSourceStack source,
            int result,
            String translationKey,
            Object... arguments
    ) {
        if (SUPPRESSED_SUCCESS_DEPTH.get() == 0) {
            source.sendSuccess(
                    () -> message(translationKey, arguments),
                    false
            );
        }
        return result;
    }

    public static int fail(
            CommandSourceStack source,
            String translationKey,
            Object... arguments
    ) {
        source.sendFailure(message(translationKey, arguments));
        return 0;
    }

    public static int withSuppressedSuccess(IntSupplier action) {
        int previous = SUPPRESSED_SUCCESS_DEPTH.get();
        SUPPRESSED_SUCCESS_DEPTH.set(previous + 1);
        try {
            return action.getAsInt();
        } finally {
            if (previous == 0) {
                SUPPRESSED_SUCCESS_DEPTH.remove();
            } else {
                SUPPRESSED_SUCCESS_DEPTH.set(previous);
            }
        }
    }

    public static Optional<ServerPlayer> requirePlayer(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return Optional.of(player);
        }
        source.sendFailure(message("command.damagenexus.player_required"));
        return Optional.empty();
    }

    private static Component message(String key, Object... arguments) {
        return Component.translatable(
                "command.damagenexus.feedback",
                Component.translatable(key, arguments)
        );
    }
}
