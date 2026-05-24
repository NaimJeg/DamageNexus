package io.github.naimjeg.damagenexus.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import io.github.naimjeg.damagenexus.DamageNexus;
import io.github.naimjeg.damagenexus.core.gametest.GameTestCodecVerifier;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHooks;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.List;
import java.util.function.Consumer;

/**
 * Launch-only security checks. RegisterGameTestsEvent is only fired when
 * NeoForge's GameTest facility is enabled, so no test is added in production.
 */
@EventBusSubscriber(modid = DamageNexus.MODID)
final class DamageNexusSecurityGameTests {

    private static final ResourceKey<Consumer<GameTestHelper>>
            SECURITY_BOUNDARIES_FUNCTION = ResourceKey.create(
            Registries.TEST_FUNCTION,
            id("security_boundaries")
    );

    private DamageNexusSecurityGameTests() {
    }

    @SubscribeEvent
    public static void registerTestFunction(RegisterEvent event) {
        if (!GameTestHooks.isGametestEnabled()) {
            return;
        }
        event.register(
                Registries.TEST_FUNCTION,
                SECURITY_BOUNDARIES_FUNCTION.identifier(),
                () -> DamageNexusSecurityGameTests::securityBoundaries
        );
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment =
                event.registerEnvironment(
                        id("security_environment"),
                        new TestEnvironmentDefinition.AllOf(List.of())
                );
        TestData<Holder<TestEnvironmentDefinition<?>>> data =
                new TestData<>(
                        environment,
                        Identifier.withDefaultNamespace("empty"),
                        20,
                        0,
                        true,
                        Rotation.NONE
                );

        event.registerTest(
                id("security_boundaries"),
                new FunctionGameTestInstance(
                        SECURITY_BOUNDARIES_FUNCTION,
                        data
                )
        );
    }

    private static void securityBoundaries(GameTestHelper helper) {
        GameTestCodecVerifier.verifyFunctionInstance(helper);
        DamageNexus.LOGGER.info(
                "[DamageNexus] Executing GameTest {}",
                SECURITY_BOUNDARIES_FUNCTION.identifier()
        );
        verifyCommandPermissions();
        helper.succeed();
    }

    private static void verifyCommandPermissions() {
        var root = Commands.literal("damagenexus");

        DamageTestCommands.register(root);
        DamageItemCommands.register(root);
        DamageDamageCommands.register(root);
        DamageBypassCommands.register(root);
        DamageMobCommands.register(root);
        DamageEffectCommands.register(root);
        DamageAttributeCommands.register(root);
        DamageCleanupCommands.register(root);

        CommandDispatcher<CommandSourceStack> dispatcher =
                new CommandDispatcher<>();
        CommandNode<CommandSourceStack> built =
                dispatcher.register(root);
        DamageCommandSecurity.verifyTree(built);

        CommandSourceStack ordinary =
                Commands.createCompilationContext(
                        PermissionSet.NO_PERMISSIONS
                );
        CommandSourceStack administrator =
                Commands.createCompilationContext(
                        PermissionSet.ALL_PERMISSIONS
                );

        for (CommandNode<CommandSourceStack> child
                : built.getChildren()) {
            String branch = child.getName();

            if (child.getRequirement().test(ordinary)
                    || !child.getRequirement().test(administrator)) {
                throw new AssertionError(
                        "Invalid admin permission on branch " + branch
                );
            }

            verifyParse(
                    dispatcher,
                    ordinary,
                    branch,
                    false
            );
            verifyParse(
                    dispatcher,
                    administrator,
                    branch,
                    true
            );
        }

        var unclassified = Commands.literal("root")
                .then(Commands.literal("future_branch"));
        boolean rejected = false;

        try {
            DamageCommandSecurity.verifyTree(unclassified.build());
        } catch (IllegalStateException expected) {
            rejected = true;
        }

        if (!rejected) {
            throw new AssertionError(
                    "An unprotected command branch was accepted"
            );
        }

        var protectedFuture = Commands.literal("root")
                .then(Commands.literal("future_branch")
                        .requires(DamageCommandSecurity
                                .adminPermission()));
        DamageCommandSecurity.verifyTree(protectedFuture.build());
    }

    private static void verifyParse(
            CommandDispatcher<CommandSourceStack> dispatcher,
            CommandSourceStack source,
            String path,
            boolean expectedToConsume
    ) {
        var result = dispatcher.parse(
                "damagenexus " + path,
                source
        );
        boolean consumed = !result.getReader().canRead();

        if (consumed != expectedToConsume) {
            throw new AssertionError(
                    "Unexpected command visibility for /damagenexus "
                            + path
                            + ": consumed="
                            + consumed
                            + " expected="
                            + expectedToConsume
            );
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(DamageNexus.MODID, path);
    }
}
