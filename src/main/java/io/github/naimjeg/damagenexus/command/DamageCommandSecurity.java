package io.github.naimjeg.damagenexus.command;

import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.IntSupplier;
import java.util.function.Predicate;

final class DamageCommandSecurity {

    private static final Map<Object, Map<Object, long[]>> LAST_EXECUTION_TICKS =
            new WeakHashMap<>();
    private static final Object SERVER_WIDE_OWNER = new Object();

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
                cooldownOwner(source),
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
            Object owner,
            ExpensiveAction action,
            long currentTick
    ) {
        if (server == null || owner == null || action == null) {
            return false;
        }

        Map<Object, long[]> byOwner = LAST_EXECUTION_TICKS.computeIfAbsent(
                server,
                ignored -> new HashMap<>()
        );
        long[] ticks = byOwner.computeIfAbsent(
                owner,
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

    static synchronized boolean tryAcquire(
            Object server,
            ExpensiveAction action,
            long currentTick
    ) {
        return tryAcquire(
                server,
                SERVER_WIDE_OWNER,
                action,
                currentTick
        );
    }

    private static Object cooldownOwner(CommandSourceStack source) {
        Entity entity = source.getEntity();
        if (entity != null) {
            return new EntityCooldownOwner(entity.getUUID());
        }
        return new PositionedCooldownOwner(
                source.getLevel().dimension().identifier().toString(),
                BlockPos.containing(source.getPosition()),
                source.getTextName()
        );
    }

    enum ExpensiveAction {
        SPAWN_ENTITIES(
                20,
                "command.damagenexus.cooldown.spawn_entities"
        ),
        CLEANUP(
                100,
                "command.damagenexus.cooldown.cleanup"
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

    private record EntityCooldownOwner(UUID uuid) {
    }

    private record PositionedCooldownOwner(
            String dimension,
            BlockPos position,
            String name
    ) {
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
