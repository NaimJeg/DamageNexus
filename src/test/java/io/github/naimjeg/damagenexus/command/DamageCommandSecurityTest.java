package io.github.naimjeg.damagenexus.command;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamageCommandSecurityTest {

    @Test
    void administrativeBranchesUseSharedPermissionPredicate()
            throws IOException {
        for (String commandClass : List.of(
                "DamageTestCommands",
                "DamageItemCommands",
                "DamageProjectileItemCommands",
                "DamageDamageCommands",
                "DamageBypassCommands",
                "DamageMobCommands",
                "DamageEffectCommands",
                "DamageAttributeCommands",
                "DamageCleanupCommands"
        )) {
            assertTrue(
                    source(commandClass).contains(
                            ".requires("
                                    + "DamageCommandSecurity"
                                    + ".adminPermission())"
                    ),
                    commandClass + " must use the shared admin predicate"
            );
        }

        String security = source("DamageCommandSecurity");

        assertTrue(security.contains(
                "Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)"
        ));
        assertFalse(security.contains("ADMIN_BRANCHES"));
        assertFalse(security.contains("PUBLIC_BRANCHES"));
        assertFalse(source("DamageNexusCommands").contains("debug_forward"));
        assertFalse(Files.exists(Path.of(
                "src", "main", "java", "io", "github", "naimjeg",
                "damagenexus", "command", "DamageDebugForwardCommands.java"
        )));
    }

    @Test
    void expensiveCommandCooldownIsSourceScopedAndHandlesTickReset() {
        Object firstServer = new Object();
        Object secondServer = new Object();
        Object firstSource = "first-source";
        Object secondSource = "second-source";

        assertTrue(DamageCommandSecurity.tryAcquire(
                firstServer,
                firstSource,
                DamageCommandSecurity.ExpensiveAction.SPAWN_ENTITIES,
                100
        ));
        assertFalse(DamageCommandSecurity.tryAcquire(
                firstServer,
                firstSource,
                DamageCommandSecurity.ExpensiveAction.SPAWN_ENTITIES,
                119
        ));
        assertTrue(DamageCommandSecurity.tryAcquire(
                firstServer,
                secondSource,
                DamageCommandSecurity.ExpensiveAction.SPAWN_ENTITIES,
                119
        ));
        assertTrue(DamageCommandSecurity.tryAcquire(
                firstServer,
                firstSource,
                DamageCommandSecurity.ExpensiveAction.SPAWN_ENTITIES,
                120
        ));
        assertTrue(DamageCommandSecurity.tryAcquire(
                secondServer,
                firstSource,
                DamageCommandSecurity.ExpensiveAction.SPAWN_ENTITIES,
                100
        ));
        assertTrue(DamageCommandSecurity.tryAcquire(
                firstServer,
                firstSource,
                DamageCommandSecurity.ExpensiveAction.SPAWN_ENTITIES,
                1
        ));

        assertTrue(DamageCommandSecurity.tryAcquire(
                firstServer,
                firstSource,
                DamageCommandSecurity.ExpensiveAction.CLEANUP,
                200
        ));
        assertFalse(DamageCommandSecurity.tryAcquire(
                firstServer,
                firstSource,
                DamageCommandSecurity.ExpensiveAction.CLEANUP,
                299
        ));
        assertTrue(DamageCommandSecurity.tryAcquire(
                firstServer,
                firstSource,
                DamageCommandSecurity.ExpensiveAction.CLEANUP,
                300
        ));
    }

    private static String source(String className) throws IOException {
        return Files.readString(Path.of(
                "src",
                "main",
                "java",
                "io",
                "github",
                "naimjeg",
                "damagenexus",
                "command",
                className + ".java"
        ));
    }
}
