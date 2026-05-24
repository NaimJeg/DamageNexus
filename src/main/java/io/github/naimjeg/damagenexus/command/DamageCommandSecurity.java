package io.github.naimjeg.damagenexus.command;

import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.IntSupplier;
import java.util.function.Predicate;

final class DamageCommandSecurity {

    private static final Map<Object, long[]> LAST_EXECUTION_TICKS =
            new WeakHashMap<>();

    private DamageCommandSecurity() {
    }

    static Predicate<CommandSourceStack> adminPermission() {
        return AdminPermissionHolder.INSTANCE;
    }

    static boolean hasAdminPermission(CommandSourceStack source) {
        return source != null && adminPermission().test(source);
    }

    static void verifyTree(CommandNode<CommandSourceStack> root) {
        Predicate<CommandSourceStack> admin = adminPermission();

        for (CommandNode<CommandSourceStack> child : root.getChildren()) {
            if (child.getRequirement() != admin) {
                throw new IllegalStateException(
                        "Non-public command branch is missing the shared "
                                + "level-two administrator permission: "
                                + child.getName()
                );
            }
        }
    }

    static int runWithCooldown(
            CommandSourceStack source,
            ExpensiveAction action,
            IntSupplier command
    ) {
        if (!tryAcquire(
                source.getServer(),
                action,
                source.getServer().getTickCount()
        )) {
            return CommandFeedback.fail(
                    source,
                    action.failureMessage()
            );
        }

        return command.getAsInt();
    }

    static synchronized boolean tryAcquire(
            Object server,
            ExpensiveAction action,
            long currentTick
    ) {
        if (server == null || action == null) {
            return false;
        }

        long[] ticks = LAST_EXECUTION_TICKS.computeIfAbsent(
                server,
                ignored -> {
                    long[] values = new long[ExpensiveAction.values().length];
                    java.util.Arrays.fill(values, Long.MIN_VALUE);
                    return values;
                }
        );
        int index = action.ordinal();
        long previous = ticks[index];

        if (previous != Long.MIN_VALUE
                && currentTick >= previous
                && currentTick - previous < action.cooldownTicks()) {
            return false;
        }

        ticks[index] = currentTick;
        return true;
    }

    enum ExpensiveAction {
        SPAWN_ENTITIES(
                20,
                "entity generation is limited to once per second."
        ),
        CLEANUP(
                100,
                "cleanup is limited to once every five seconds."
        );

        private final int cooldownTicks;
        private final String failureMessage;

        ExpensiveAction(int cooldownTicks, String failureMessage) {
            this.cooldownTicks = cooldownTicks;
            this.failureMessage = failureMessage;
        }

        int cooldownTicks() {
            return cooldownTicks;
        }

        String failureMessage() {
            return failureMessage;
        }
    }

    /*
     * Commands initializes Minecraft registries. Keep the permission predicate
     * lazy so the independent cooldown can be unit-tested without an FML
     * launch, while command registration still uses the version-native API.
     */
    private static final class AdminPermissionHolder {
        private static final Predicate<CommandSourceStack> INSTANCE =
                Commands.hasPermission(Commands.LEVEL_GAMEMASTERS);
    }
}
