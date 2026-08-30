package io.github.naimjeg.damagenexus.entity;

import io.github.naimjeg.damagenexus.DamageNexus;
import io.github.naimjeg.damagenexus.api.event.DamageSettledEvent;
import io.github.naimjeg.damagenexus.block.DamageDummyBlock;
import io.github.naimjeg.damagenexus.block.entity.DamageDummyBlockEntity;
import io.github.naimjeg.damagenexus.core.gametest.GameTestCodecVerifier;
import io.github.naimjeg.damagenexus.menu.DamageDummyMenu;
import io.github.naimjeg.damagenexus.registry.ModAttributes;
import io.github.naimjeg.damagenexus.registry.ModBlockEntityTypes;
import io.github.naimjeg.damagenexus.registry.ModBlocks;
import io.github.naimjeg.damagenexus.registry.ModEntityTypes;
import io.github.naimjeg.damagenexus.registry.ModItems;
import io.github.naimjeg.damagenexus.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHooks;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Launch-only integration coverage for the damage dummy.
 *
 * <p>Each independent behavior runs as its own GameTest on a fresh entity:
 * registration/coverage, editing, damage, persistence, and the GameTest-only
 * sentinel attribute. Lethal damage therefore never contaminates editing or
 * persistence checks.</p>
 */
@EventBusSubscriber(modid = DamageNexus.MODID)
final class DamageDummyGameTests {

    private static final ResourceKey<Consumer<GameTestHelper>>
            ATTRIBUTES_FUNCTION = ResourceKey.create(
            Registries.TEST_FUNCTION,
            id("damage_dummy_attributes")
    );
    private static final ResourceKey<Consumer<GameTestHelper>>
            EDITING_FUNCTION = ResourceKey.create(
            Registries.TEST_FUNCTION,
            id("damage_dummy_editing")
    );
    private static final ResourceKey<Consumer<GameTestHelper>>
            DAMAGE_FUNCTION = ResourceKey.create(
            Registries.TEST_FUNCTION,
            id("damage_dummy_damage")
    );
    private static final ResourceKey<Consumer<GameTestHelper>>
            PERSISTENCE_FUNCTION = ResourceKey.create(
            Registries.TEST_FUNCTION,
            id("damage_dummy_persistence")
    );
    private static final ResourceKey<Consumer<GameTestHelper>>
            SENTINEL_FUNCTION = ResourceKey.create(
            Registries.TEST_FUNCTION,
            id("damage_dummy_sentinel")
    );
    private static final ResourceKey<Consumer<GameTestHelper>>
            NAME_TRANSLATION_FUNCTION = ResourceKey.create(
            Registries.TEST_FUNCTION,
            id("damage_dummy_name_translation")
    );
    private static final ResourceKey<Consumer<GameTestHelper>>
            BLOCK_REGISTRATION_FUNCTION = ResourceKey.create(
            Registries.TEST_FUNCTION,
            id("damage_dummy_block_registration")
    );
    private static final ResourceKey<Consumer<GameTestHelper>>
            BLOCK_ANCHOR_FUNCTION = ResourceKey.create(
            Registries.TEST_FUNCTION,
            id("damage_dummy_block_anchor")
    );
    private static final ResourceKey<Consumer<GameTestHelper>>
            FRESH_PLACEMENT_FUNCTION = ResourceKey.create(
            Registries.TEST_FUNCTION,
            id("damage_dummy_fresh_placement")
    );
    private static final ResourceKey<Consumer<GameTestHelper>>
            BLOCK_SINGLE_FUNCTION = ResourceKey.create(
            Registries.TEST_FUNCTION,
            id("damage_dummy_block_single")
    );
    private static final ResourceKey<Consumer<GameTestHelper>>
            RELOAD_RECONCILE_FUNCTION = ResourceKey.create(
            Registries.TEST_FUNCTION,
            id("damage_dummy_block_reload_reconcile")
    );
    private static final ResourceKey<Consumer<GameTestHelper>>
            EXTERNAL_DISCARD_FUNCTION = ResourceKey.create(
            Registries.TEST_FUNCTION,
            id("damage_dummy_external_discard")
    );
    private static final ResourceKey<Consumer<GameTestHelper>>
            KILL_FUNCTION = ResourceKey.create(
            Registries.TEST_FUNCTION,
            id("damage_dummy_kill")
    );
    private static final ResourceKey<Consumer<GameTestHelper>>
            BLOCK_DUPLICATE_REPAIR_FUNCTION = ResourceKey.create(
            Registries.TEST_FUNCTION,
            id("damage_dummy_block_duplicate_repair")
    );
    private static final ResourceKey<Consumer<GameTestHelper>>
            BLOCK_BREAK_FUNCTION = ResourceKey.create(
            Registries.TEST_FUNCTION,
            id("damage_dummy_block_break")
    );
    private static final ResourceKey<Consumer<GameTestHelper>>
            SELF_CLEANUP_FUNCTION = ResourceKey.create(
            Registries.TEST_FUNCTION,
            id("damage_dummy_self_cleanup")
    );
    private static final ResourceKey<Consumer<GameTestHelper>>
            STALE_UUID_REMOVAL_FUNCTION = ResourceKey.create(
            Registries.TEST_FUNCTION,
            id("damage_dummy_stale_uuid_removal")
    );
    private static final ResourceKey<Consumer<GameTestHelper>>
            DESTROY_DUPLICATES_FUNCTION = ResourceKey.create(
            Registries.TEST_FUNCTION,
            id("damage_dummy_destroy_duplicates")
    );
    private static final ResourceKey<Consumer<GameTestHelper>>
            BOUND_REPLACEMENT_FUNCTION = ResourceKey.create(
            Registries.TEST_FUNCTION,
            id("damage_dummy_bound_replacement_rejected")
    );
    private static final ResourceKey<Consumer<GameTestHelper>>
            STANDALONE_UNAFFECTED_FUNCTION = ResourceKey.create(
            Registries.TEST_FUNCTION,
            id("damage_dummy_standalone_unaffected")
    );
    private static final ResourceKey<Consumer<GameTestHelper>>
            ANCHORED_DAMAGE_FUNCTION = ResourceKey.create(
            Registries.TEST_FUNCTION,
            id("damage_dummy_anchored_damage")
    );
    private static final ResourceKey<Consumer<GameTestHelper>>
            ANCHORED_LETHAL_FUNCTION = ResourceKey.create(
            Registries.TEST_FUNCTION,
            id("damage_dummy_anchored_lethal")
    );
    private static final ResourceKey<Consumer<GameTestHelper>>
            ATTRIBUTE_RETENTION_FUNCTION = ResourceKey.create(
            Registries.TEST_FUNCTION,
            id("damage_dummy_attribute_retention")
    );
    private static final ResourceKey<Consumer<GameTestHelper>>
            STANDALONE_DEATH_FUNCTION = ResourceKey.create(
            Registries.TEST_FUNCTION,
            id("damage_dummy_standalone_death")
    );
    private static final ResourceKey<Consumer<GameTestHelper>>
            LINK_PERSISTENCE_FUNCTION = ResourceKey.create(
            Registries.TEST_FUNCTION,
            id("damage_dummy_link_persistence")
    );
    private static final ResourceKey<Consumer<GameTestHelper>>
            BLOCK_CLEARANCE_FUNCTION = ResourceKey.create(
            Registries.TEST_FUNCTION,
            id("damage_dummy_block_clearance")
    );
    private static final ResourceKey<Consumer<GameTestHelper>>
            MENU_CONTRACT_FUNCTION = ResourceKey.create(
            Registries.TEST_FUNCTION,
            id("damage_dummy_menu_contract")
    );

    /**
     * Settlement observation capture for the anchored-dummy pipeline
     * regression: armed only around the direct hurt call in one test, and
     * filtered by the exact target UUID, so concurrent damage tests cannot
     * pollute it. DamageSettledEvent is posted synchronously on the server
     * thread by the settlement coordinator before hurtServer returns.
     */
    private static UUID settlementCaptureTarget;
    private static boolean settlementCaptureObserved;
    private static float settlementCaptureHealthDamage;

    private static final ResourceKey<Attribute> MAX_HEALTH_KEY =
            ResourceKey.create(
                    Registries.ATTRIBUTE,
                    Identifier.withDefaultNamespace("max_health")
            );
    private static final ResourceKey<Attribute> CRIT_CHANCE_KEY =
            ResourceKey.create(
                    Registries.ATTRIBUTE,
                    ModAttributes.CRIT_CHANCE.getId()
            );

    private DamageDummyGameTests() {
    }

    @SubscribeEvent
    public static void registerTestFunctions(RegisterEvent event) {
        if (!GameTestHooks.isGametestEnabled()) {
            return;
        }
        event.register(
                Registries.TEST_FUNCTION,
                ATTRIBUTES_FUNCTION.identifier(),
                () -> DamageDummyGameTests::damageDummyAttributes
        );
        event.register(
                Registries.TEST_FUNCTION,
                EDITING_FUNCTION.identifier(),
                () -> DamageDummyGameTests::damageDummyEditing
        );
        event.register(
                Registries.TEST_FUNCTION,
                DAMAGE_FUNCTION.identifier(),
                () -> DamageDummyGameTests::damageDummyDamage
        );
        event.register(
                Registries.TEST_FUNCTION,
                PERSISTENCE_FUNCTION.identifier(),
                () -> DamageDummyGameTests::damageDummyPersistence
        );
        event.register(
                Registries.TEST_FUNCTION,
                SENTINEL_FUNCTION.identifier(),
                () -> DamageDummyGameTests::damageDummySentinel
        );
        event.register(
                Registries.TEST_FUNCTION,
                NAME_TRANSLATION_FUNCTION.identifier(),
                () -> DamageDummyGameTests::damageDummyNameTranslation
        );
        event.register(
                Registries.TEST_FUNCTION,
                BLOCK_REGISTRATION_FUNCTION.identifier(),
                () -> DamageDummyGameTests::damageDummyBlockRegistration
        );
        event.register(
                Registries.TEST_FUNCTION,
                BLOCK_ANCHOR_FUNCTION.identifier(),
                () -> DamageDummyGameTests::damageDummyBlockAnchor
        );
        event.register(
                Registries.TEST_FUNCTION,
                FRESH_PLACEMENT_FUNCTION.identifier(),
                () -> DamageDummyGameTests::damageDummyFreshPlacement
        );
        event.register(
                Registries.TEST_FUNCTION,
                BLOCK_SINGLE_FUNCTION.identifier(),
                () -> DamageDummyGameTests::damageDummyBlockSingle
        );
        event.register(
                Registries.TEST_FUNCTION,
                RELOAD_RECONCILE_FUNCTION.identifier(),
                () -> DamageDummyGameTests::damageDummyBlockReloadReconcile
        );
        event.register(
                Registries.TEST_FUNCTION,
                EXTERNAL_DISCARD_FUNCTION.identifier(),
                () -> DamageDummyGameTests::damageDummyExternalDiscard
        );
        event.register(
                Registries.TEST_FUNCTION,
                KILL_FUNCTION.identifier(),
                () -> DamageDummyGameTests::damageDummyKill
        );
        event.register(
                Registries.TEST_FUNCTION,
                BLOCK_DUPLICATE_REPAIR_FUNCTION.identifier(),
                () -> DamageDummyGameTests::damageDummyBlockDuplicateRepair
        );
        event.register(
                Registries.TEST_FUNCTION,
                BLOCK_BREAK_FUNCTION.identifier(),
                () -> DamageDummyGameTests::damageDummyBlockBreak
        );
        event.register(
                Registries.TEST_FUNCTION,
                SELF_CLEANUP_FUNCTION.identifier(),
                () -> DamageDummyGameTests::damageDummySelfCleanup
        );
        event.register(
                Registries.TEST_FUNCTION,
                STALE_UUID_REMOVAL_FUNCTION.identifier(),
                () -> DamageDummyGameTests::damageDummyStaleUuidRemoval
        );
        event.register(
                Registries.TEST_FUNCTION,
                DESTROY_DUPLICATES_FUNCTION.identifier(),
                () -> DamageDummyGameTests::damageDummyDestroyDuplicates
        );
        event.register(
                Registries.TEST_FUNCTION,
                BOUND_REPLACEMENT_FUNCTION.identifier(),
                () -> DamageDummyGameTests::damageDummyBoundReplacementRejected
        );
        event.register(
                Registries.TEST_FUNCTION,
                STANDALONE_UNAFFECTED_FUNCTION.identifier(),
                () -> DamageDummyGameTests::damageDummyStandaloneUnaffected
        );
        event.register(
                Registries.TEST_FUNCTION,
                ANCHORED_DAMAGE_FUNCTION.identifier(),
                () -> DamageDummyGameTests::damageDummyAnchoredDamage
        );
        event.register(
                Registries.TEST_FUNCTION,
                ANCHORED_LETHAL_FUNCTION.identifier(),
                () -> DamageDummyGameTests::damageDummyAnchoredLethal
        );
        event.register(
                Registries.TEST_FUNCTION,
                ATTRIBUTE_RETENTION_FUNCTION.identifier(),
                () -> DamageDummyGameTests::damageDummyAttributeRetention
        );
        event.register(
                Registries.TEST_FUNCTION,
                STANDALONE_DEATH_FUNCTION.identifier(),
                () -> DamageDummyGameTests::damageDummyStandaloneDeath
        );
        event.register(
                Registries.TEST_FUNCTION,
                LINK_PERSISTENCE_FUNCTION.identifier(),
                () -> DamageDummyGameTests::damageDummyLinkPersistence
        );
        event.register(
                Registries.TEST_FUNCTION,
                BLOCK_CLEARANCE_FUNCTION.identifier(),
                () -> DamageDummyGameTests::damageDummyBlockClearance
        );
        event.register(
                Registries.TEST_FUNCTION,
                MENU_CONTRACT_FUNCTION.identifier(),
                () -> DamageDummyGameTests::damageDummyMenuContract
        );
    }

    @SubscribeEvent
    public static void onDamageSettled(DamageSettledEvent event) {
        if (settlementCaptureTarget == null) {
            return;
        }
        if (event.snapshot().target() != null
                && event.snapshot().target().getUUID()
                .equals(settlementCaptureTarget)) {
            settlementCaptureObserved = true;
            settlementCaptureHealthDamage =
                    event.snapshot().healthDamage();
        }
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment =
                event.registerEnvironment(
                        id("damage_dummy_environment"),
                        new TestEnvironmentDefinition.AllOf(List.of())
                );
        registerTest(
                event,
                environment,
                "damage_dummy_attributes",
                ATTRIBUTES_FUNCTION
        );
        registerTest(
                event,
                environment,
                "damage_dummy_editing",
                EDITING_FUNCTION
        );
        registerTest(
                event,
                environment,
                "damage_dummy_damage",
                DAMAGE_FUNCTION
        );
        registerTest(
                event,
                environment,
                "damage_dummy_persistence",
                PERSISTENCE_FUNCTION
        );
        registerTest(
                event,
                environment,
                "damage_dummy_sentinel",
                SENTINEL_FUNCTION
        );
        registerTest(
                event,
                environment,
                "damage_dummy_name_translation",
                NAME_TRANSLATION_FUNCTION
        );
        registerBlockTest(
                event,
                environment,
                "damage_dummy_block_registration",
                BLOCK_REGISTRATION_FUNCTION
        );
        registerBlockTest(
                event,
                environment,
                "damage_dummy_block_anchor",
                BLOCK_ANCHOR_FUNCTION
        );
        registerBlockTest(
                event,
                environment,
                "damage_dummy_fresh_placement",
                FRESH_PLACEMENT_FUNCTION
        );
        registerBlockTest(
                event,
                environment,
                "damage_dummy_block_single",
                BLOCK_SINGLE_FUNCTION
        );
        registerBlockTest(
                event,
                environment,
                "damage_dummy_block_reload_reconcile",
                RELOAD_RECONCILE_FUNCTION
        );
        registerBlockTest(
                event,
                environment,
                "damage_dummy_external_discard",
                EXTERNAL_DISCARD_FUNCTION
        );
        registerBlockTest(
                event,
                environment,
                "damage_dummy_kill",
                KILL_FUNCTION
        );
        registerBlockTest(
                event,
                environment,
                "damage_dummy_block_duplicate_repair",
                BLOCK_DUPLICATE_REPAIR_FUNCTION
        );
        registerBlockTest(
                event,
                environment,
                "damage_dummy_block_break",
                BLOCK_BREAK_FUNCTION
        );
        registerBlockTest(
                event,
                environment,
                "damage_dummy_self_cleanup",
                SELF_CLEANUP_FUNCTION
        );
        registerBlockTest(
                event,
                environment,
                "damage_dummy_stale_uuid_removal",
                STALE_UUID_REMOVAL_FUNCTION
        );
        registerBlockTest(
                event,
                environment,
                "damage_dummy_destroy_duplicates",
                DESTROY_DUPLICATES_FUNCTION
        );
        registerBlockTest(
                event,
                environment,
                "damage_dummy_bound_replacement_rejected",
                BOUND_REPLACEMENT_FUNCTION
        );
        registerBlockTest(
                event,
                environment,
                "damage_dummy_standalone_unaffected",
                STANDALONE_UNAFFECTED_FUNCTION
        );
        registerBlockTest(
                event,
                environment,
                "damage_dummy_anchored_damage",
                ANCHORED_DAMAGE_FUNCTION
        );
        registerBlockTest(
                event,
                environment,
                "damage_dummy_anchored_lethal",
                ANCHORED_LETHAL_FUNCTION
        );
        registerBlockTest(
                event,
                environment,
                "damage_dummy_attribute_retention",
                ATTRIBUTE_RETENTION_FUNCTION
        );
        registerBlockTest(
                event,
                environment,
                "damage_dummy_standalone_death",
                STANDALONE_DEATH_FUNCTION
        );
        registerBlockTest(
                event,
                environment,
                "damage_dummy_link_persistence",
                LINK_PERSISTENCE_FUNCTION
        );
        registerBlockTest(
                event,
                environment,
                "damage_dummy_block_clearance",
                BLOCK_CLEARANCE_FUNCTION
        );
        registerBlockTest(
                event,
                environment,
                "damage_dummy_menu_contract",
                MENU_CONTRACT_FUNCTION
        );
    }

    private static void registerTest(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition<?>> environment,
            String path,
            ResourceKey<Consumer<GameTestHelper>> function
    ) {
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
                id(path),
                new FunctionGameTestInstance(function, data)
        );
    }

    /**
     * Block/anchor tests are asynchronous (they wait for the block entity
     * reconciliation ticker), so they get a much larger timeout than the
     * synchronous entity tests.
     */
    private static void registerBlockTest(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition<?>> environment,
            String path,
            ResourceKey<Consumer<GameTestHelper>> function
    ) {
        TestData<Holder<TestEnvironmentDefinition<?>>> data =
                new TestData<>(
                        environment,
                        Identifier.withDefaultNamespace("empty"),
                        400,
                        0,
                        true,
                        Rotation.NONE
                );
        event.registerTest(
                id(path),
                new FunctionGameTestInstance(function, data)
        );
    }

    private static DamageDummyEntity spawnDummy(
            GameTestHelper helper,
            BlockPos pos
    ) {
        DamageDummyEntity dummy = helper.spawn(
                ModEntityTypes.DAMAGE_DUMMY.get(),
                pos
        );
        if (dummy == null || !dummy.isAlive()) {
            throw new AssertionError(
                    "damagenexus:damage_dummy did not spawn as a living entity"
            );
        }
        return dummy;
    }

    private static void damageDummyAttributes(GameTestHelper helper) {
        GameTestCodecVerifier.verifyFunctionInstance(helper);
        DamageDummyEntity dummy = spawnDummy(helper, new BlockPos(1, 2, 1));
        verifyRegistration();
        verifyStaticLivingEntity(dummy);
        verifyCoreAttributes(dummy);
        verifyUniversalAttributeCoverage(dummy);
        verifyEnumeration(dummy);
        helper.succeed();
    }

    private static void damageDummyEditing(GameTestHelper helper) {
        GameTestCodecVerifier.verifyFunctionInstance(helper);
        DamageDummyEntity dummy = spawnDummy(helper, new BlockPos(1, 2, 1));
        verifyEditing(dummy);
        helper.succeed();
    }

    private static void damageDummyDamage(GameTestHelper helper) {
        GameTestCodecVerifier.verifyFunctionInstance(helper);
        DamageDummyEntity dummy = spawnDummy(helper, new BlockPos(1, 2, 1));
        verifyDamage(helper, dummy);
        helper.succeed();
    }

    private static void damageDummyPersistence(GameTestHelper helper) {
        GameTestCodecVerifier.verifyFunctionInstance(helper);
        DamageDummyEntity dummy = spawnDummy(helper, new BlockPos(1, 2, 1));
        verifyPersistence(helper, dummy);
        helper.succeed();
    }

    private static void damageDummySentinel(GameTestHelper helper) {
        GameTestCodecVerifier.verifyFunctionInstance(helper);
        DamageDummyEntity dummy = spawnDummy(helper, new BlockPos(1, 2, 1));
        verifySentinel(dummy);
        helper.succeed();
    }

    /**
     * Discoverability contract: the registered EntityType derives the
     * standard translation key ({@code entity.damagenexus.damage_dummy}) and
     * an unnamed dummy resolves that key through vanilla Entity name
     * semantics, with no custom name assigned. Localized display text itself
     * is client/resource-pack state and is intentionally not asserted here.
     */
    private static void damageDummyNameTranslation(GameTestHelper helper) {
        GameTestCodecVerifier.verifyFunctionInstance(helper);
        if (!require(helper,
                "entity.damagenexus.damage_dummy".equals(
                        ModEntityTypes.DAMAGE_DUMMY.get().getDescriptionId()),
                "EntityType description id is not "
                        + "entity.damagenexus.damage_dummy")) {
            return;
        }

        DamageDummyEntity dummy = spawnDummy(helper, new BlockPos(1, 2, 1));
        if (!require(helper,
                !dummy.hasCustomName() && dummy.getCustomName() == null,
                "spawned dummy must not carry a custom name")) {
            return;
        }
        if (!require(helper,
                containsTranslatableKey(
                        dummy.getName(),
                        "entity.damagenexus.damage_dummy"),
                "dummy.getName() does not resolve the EntityType "
                        + "translation key")) {
            return;
        }
        if (!require(helper,
                containsTranslatableKey(
                        dummy.getDisplayName(),
                        "entity.damagenexus.damage_dummy"),
                "dummy.getDisplayName() does not resolve the EntityType "
                        + "translation key")) {
            return;
        }
        helper.succeed();
    }

    private static boolean containsTranslatableKey(
            Component component,
            String key
    ) {
        if (component.getContents() instanceof TranslatableContents contents
                && key.equals(contents.getKey())) {
            return true;
        }
        for (Component sibling : component.getSiblings()) {
            if (containsTranslatableKey(sibling, key)) {
                return true;
            }
        }
        return false;
    }

    private static void verifyRegistration() {
        Optional<Holder.Reference<EntityType<?>>> registered =
                BuiltInRegistries.ENTITY_TYPE.get(id("damage_dummy"));
        if (registered.isEmpty()) {
            throw new AssertionError(
                    "damagenexus:damage_dummy is not in the entity registry"
            );
        }
        EntityType<?> type = registered.get().value();
        if (!type.canSummon()) {
            throw new AssertionError(
                    "damagenexus:damage_dummy must be summonable"
            );
        }
        if (!DefaultAttributes.hasSupplier(type)) {
            throw new AssertionError(
                    "damagenexus:damage_dummy has no attribute supplier"
            );
        }
    }

    /**
     * A/B: the dummy is a plain static LivingEntity. It must not be a Mob
     * (no goal selector, navigation, targeting, or brain-driven AI) and must
     * never be affected by gravity.
     */
    private static void verifyStaticLivingEntity(DamageDummyEntity dummy) {
        if (!(dummy instanceof LivingEntity)) {
            throw new AssertionError(
                    "damage dummy is not a LivingEntity"
            );
        }
        // DamageDummyEntity is a direct LivingEntity; it cannot be a Mob, so
        // no goal selector / navigation / targeting surface exists at all.
        // The default brain must also carry no memories, sensors, or
        // behaviors, i.e. there is no AI to run.
        if (!dummy.getBrain().isBrainDead()) {
            throw new AssertionError(
                    "damage dummy must not have any AI brain content"
            );
        }
        if (!dummy.isNoGravity()) {
            throw new AssertionError(
                    "damage dummy should spawn with gravity disabled"
            );
        }
    }

    private static void verifyCoreAttributes(DamageDummyEntity dummy) {
        AttributeInstance maxHealth = dummy.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) {
            throw new AssertionError(
                    "damage dummy is missing MAX_HEALTH"
            );
        }
        if (maxHealth.getBaseValue() != 20.0D) {
            throw new AssertionError(
                    "damage dummy MAX_HEALTH base is not the vanilla default"
            );
        }
        if (dummy.getMaxHealth() != 20.0F) {
            throw new AssertionError(
                    "damage dummy max health is inconsistent after spawn"
            );
        }
        if (dummy.getHealth() != dummy.getMaxHealth()) {
            throw new AssertionError(
                    "damage dummy did not spawn with full health"
            );
        }
    }

    /**
     * Universal coverage: every registered attribute must be attached. This
     * includes the GameTest-only sentinel, which the dummy implementation
     * never names, proving attachment is registry-driven rather than
     * allowlisted. Representative vanilla and DamageNexus attributes are
     * checked explicitly for readable failure messages, but no version-
     * sensitive assumption about the Mob baseline is made.
     */
    private static void verifyUniversalAttributeCoverage(
            DamageDummyEntity dummy
    ) {
        List<Holder.Reference<Attribute>> registered =
                BuiltInRegistries.ATTRIBUTE.listElements().toList();
        for (Holder.Reference<Attribute> holder : registered) {
            if (dummy.getAttribute(holder) == null) {
                throw new AssertionError(
                        "damage dummy is missing registered attribute "
                                + holder.key()
                );
            }
        }

        // Representative DamageNexus attributes, reached through the same
        // generic mechanism (no manual listing in the entity).
        requireAttribute(dummy, ModAttributes.RESISTANCE_FIRE, "resistance_fire");
        requireAttribute(dummy, ModAttributes.CRIT_CHANCE, "crit_chance");
    }

    private static void requireAttribute(
            LivingEntity entity,
            Holder<Attribute> attribute,
            String name
    ) {
        if (entity.getAttribute(attribute) == null) {
            throw new AssertionError(
                    "damage dummy is missing attribute " + name
            );
        }
    }

    private static void verifyEnumeration(DamageDummyEntity dummy) {
        int registeredCount =
                (int) BuiltInRegistries.ATTRIBUTE.listElements().count();
        List<DamageDummyAttributes.AvailableAttribute> available =
                DamageDummyAttributes.availableAttributes(dummy);
        if (available.size() != registeredCount) {
            throw new AssertionError(
                    "enumerated attributes (" + available.size()
                            + ") != registered attributes ("
                            + registeredCount + ")"
            );
        }

        List<Identifier> ids = available.stream()
                .map(DamageDummyAttributes.AvailableAttribute::id)
                .toList();
        List<Identifier> sorted = ids.stream()
                .sorted(Identifier::compareNamespaced)
                .toList();
        if (!ids.equals(sorted)) {
            throw new AssertionError(
                    "available attributes are not sorted by registry id"
            );
        }
        if (new HashSet<>(ids).size() != ids.size()) {
            throw new AssertionError(
                    "available attributes contain duplicates"
            );
        }

        if (DamageDummyAttributes.find(dummy, MAX_HEALTH_KEY).isEmpty()) {
            throw new AssertionError(
                    "catalog find() did not resolve MAX_HEALTH"
            );
        }
        if (!DamageDummyAttributes.has(dummy, MAX_HEALTH_KEY)) {
            throw new AssertionError(
                    "catalog has() rejected an attached MAX_HEALTH"
            );
        }
    }

    private static void verifyEditing(DamageDummyEntity dummy) {
        // MAX_HEALTH base edit must update the effective value through the
        // real AttributeInstance.
        if (!DamageDummyAttributes.setBaseValue(dummy, MAX_HEALTH_KEY, 40.0D)) {
            throw new AssertionError("setBaseValue failed for MAX_HEALTH");
        }
        double base = DamageDummyAttributes.getBaseValue(
                dummy,
                MAX_HEALTH_KEY
        ).orElseThrow();
        if (base != 40.0D) {
            throw new AssertionError(
                    "setBaseValue/getBaseValue round-trip failed: " + base
            );
        }
        if (dummy.getMaxHealth() != 40.0F) {
            throw new AssertionError(
                    "MAX_HEALTH edit did not change max health"
            );
        }

        // Normal Attribute validation must still apply: RangedAttribute clamps
        // the effective value to its declared maximum (1024 for MAX_HEALTH).
        if (!DamageDummyAttributes.setBaseValue(
                dummy,
                MAX_HEALTH_KEY,
                5000.0D
        )) {
            throw new AssertionError("setBaseValue failed for MAX_HEALTH");
        }
        if (dummy.getMaxHealth() != 1024.0F) {
            throw new AssertionError(
                    "out-of-range base value bypassed attribute validation"
            );
        }

        if (!DamageDummyAttributes.resetBaseValue(dummy, MAX_HEALTH_KEY)) {
            throw new AssertionError("resetBaseValue failed for MAX_HEALTH");
        }
        if (dummy.getMaxHealth() != 20.0F) {
            throw new AssertionError(
                    "resetBaseValue did not restore the default"
            );
        }

        // A DamageNexus attribute uses the exact same generic edit/reset path.
        if (!DamageDummyAttributes.setBaseValue(dummy, CRIT_CHANCE_KEY, 0.5D)) {
            throw new AssertionError("setBaseValue failed for CRIT_CHANCE");
        }
        AttributeInstance critChance =
                dummy.getAttribute(ModAttributes.CRIT_CHANCE);
        if (critChance == null || critChance.getBaseValue() != 0.5D) {
            throw new AssertionError(
                    "CRIT_CHANCE base edit did not stick"
            );
        }
        if (critChance.getValue() != 0.5D) {
            throw new AssertionError(
                    "CRIT_CHANCE effective value did not update"
            );
        }
        if (!DamageDummyAttributes.resetBaseValue(dummy, CRIT_CHANCE_KEY)) {
            throw new AssertionError("resetBaseValue failed for CRIT_CHANCE");
        }
        if (dummy.getAttribute(ModAttributes.CRIT_CHANCE)
                .getBaseValue() != 0.0D) {
            throw new AssertionError(
                    "CRIT_CHANCE reset did not restore the default"
            );
        }
    }

    private static void verifyDamage(
            GameTestHelper helper,
            DamageDummyEntity dummy
    ) {
        LivingEntity attacker = helper.spawnWithNoFreeWill(
                EntityType.ZOMBIE,
                new BlockPos(1, 2, 3)
        );
        DamageSource source = attacker.damageSources().mobAttack(attacker);
        float before = dummy.getHealth();

        if (!dummy.hurtServer(helper.getLevel(), source, 5.0F)) {
            throw new AssertionError(
                    "damage dummy rejected ordinary damage"
            );
        }
        float after = dummy.getHealth();
        if (after <= 0.0F || before - after < 4.5F) {
            throw new AssertionError(
                    "damage dummy did not take ordinary damage: "
                            + before + " -> " + after
            );
        }

        if (!dummy.hurtServer(helper.getLevel(), source, 1000.0F)) {
            throw new AssertionError(
                    "damage dummy rejected lethal damage"
            );
        }
        if (!dummy.isDeadOrDying()) {
            throw new AssertionError(
                    "damage dummy did not die from lethal damage"
            );
        }
    }

    /**
     * Persistence uses a fresh, still-living dummy: edit one vanilla and one
     * DamageNexus/test attribute, then round-trip through the normal vanilla
     * entity serialization path ({@code restoreFrom}) and verify base values
     * survive. No custom NBT is written.
     */
    private static void verifyPersistence(
            GameTestHelper helper,
            DamageDummyEntity source
    ) {
        if (!source.isAlive()) {
            throw new AssertionError(
                    "persistence source dummy is not alive"
            );
        }
        if (!DamageDummyAttributes.setBaseValue(source, MAX_HEALTH_KEY, 40.0D)) {
            throw new AssertionError(
                    "setBaseValue failed before persistence check"
            );
        }
        if (!DamageDummyAttributes.setBaseValue(
                source,
                DamageDummyTestAttribute.KEY,
                12.0D
        )) {
            throw new AssertionError(
                    "sentinel setBaseValue failed before persistence check"
            );
        }

        DamageDummyEntity reloaded = ModEntityTypes.DAMAGE_DUMMY.get().create(
                helper.getLevel(),
                EntitySpawnReason.COMMAND
        );
        if (reloaded == null) {
            throw new AssertionError(
                    "unable to create a fresh damage dummy"
            );
        }
        // Normal vanilla entity serialization: saveWithoutId + load through
        // the exact ValueOutput/ValueInput round trip used by chunk saving.
        reloaded.restoreFrom(source);

        AttributeInstance reloadedMaxHealth =
                reloaded.getAttribute(Attributes.MAX_HEALTH);
        if (reloadedMaxHealth == null
                || reloadedMaxHealth.getBaseValue() != 40.0D) {
            throw new AssertionError(
                    "MAX_HEALTH base value did not survive serialization"
            );
        }
        if (reloaded.getMaxHealth() != 40.0F) {
            throw new AssertionError(
                    "max health did not survive serialization"
            );
        }

        Holder.Reference<Attribute> sentinel =
                DamageDummyTestAttribute.holder().orElseThrow(() ->
                        new AssertionError(
                                "sentinel attribute missing from registry"
                        )
                );
        AttributeInstance reloadedSentinel = reloaded.getAttribute(sentinel);
        if (reloadedSentinel == null
                || reloadedSentinel.getBaseValue() != 12.0D) {
            throw new AssertionError(
                    "sentinel base value did not survive serialization"
            );
        }
        if (reloadedSentinel.getValue() != 12.0D) {
            throw new AssertionError(
                    "sentinel effective value did not survive serialization"
            );
        }
    }

    /**
     * The sentinel is the primary regression proof for universal attribute
     * support: the dummy implementation never names it, yet the registry-
     * driven lifecycle attaches it with its declared default, the catalog
     * exposes it, and DamageNexus edits/resets it through the real
     * AttributeInstance APIs.
     */
    private static void verifySentinel(DamageDummyEntity dummy) {
        Holder.Reference<Attribute> sentinel =
                DamageDummyTestAttribute.holder().orElseThrow(() ->
                        new AssertionError(
                                "damagenexus:damage_dummy_test_attribute "
                                        + "missing from ATTRIBUTE registry"
                        )
                );

        AttributeInstance instance = dummy.getAttribute(sentinel);
        if (instance == null) {
            throw new AssertionError(
                    "sentinel attribute is not attached to the dummy"
            );
        }
        if (instance.getBaseValue() != DamageDummyTestAttribute.DEFAULT_BASE) {
            throw new AssertionError(
                    "sentinel base value does not match its declared default: "
                            + instance.getBaseValue()
            );
        }
        if (instance.getValue() != DamageDummyTestAttribute.DEFAULT_BASE) {
            throw new AssertionError(
                    "sentinel effective value does not match its default: "
                            + instance.getValue()
            );
        }

        List<DamageDummyAttributes.AvailableAttribute> available =
                DamageDummyAttributes.availableAttributes(dummy);
        if (available.stream()
                .noneMatch(entry ->
                        entry.id().equals(DamageDummyTestAttribute.ID))) {
            throw new AssertionError(
                    "sentinel attribute missing from the attribute catalog"
            );
        }

        if (!DamageDummyAttributes.setBaseValue(
                dummy,
                DamageDummyTestAttribute.KEY,
                12.0D
        )) {
            throw new AssertionError(
                    "setBaseValue failed for the sentinel attribute"
            );
        }
        if (dummy.getAttribute(sentinel).getBaseValue() != 12.0D) {
            throw new AssertionError(
                    "sentinel base edit did not stick"
            );
        }
        if (dummy.getAttribute(sentinel).getValue() != 12.0D) {
            throw new AssertionError(
                    "sentinel effective value did not update"
            );
        }

        if (!DamageDummyAttributes.resetBaseValue(
                dummy,
                DamageDummyTestAttribute.KEY
        )) {
            throw new AssertionError(
                    "resetBaseValue failed for the sentinel attribute"
            );
        }
        if (dummy.getAttribute(sentinel).getBaseValue()
                != DamageDummyTestAttribute.DEFAULT_BASE) {
            throw new AssertionError(
                "sentinel reset did not restore the declared default"
            );
        }
    }

    // ------------------------------------------------------------------
    // Block / anchored-dummy tests
    // ------------------------------------------------------------------

    private static BlockPos blockTestPos() {
        return new BlockPos(1, 2, 1);
    }

    /**
     * All anchored dummies currently in the level for the pedestal at the
     * relative position. Removed entities are filtered out so a mid-tick
     * discard does not produce a stale hit.
     */
    private static List<DamageDummyEntity> anchoredDummies(
            GameTestHelper helper,
            BlockPos relativePos
    ) {
        BlockPos absolute = helper.absolutePos(relativePos);
        AABB search = new AABB(absolute).inflate(2.0);
        return helper.getLevel().getEntitiesOfClass(
                DamageDummyEntity.class,
                search,
                dummy -> dummy.isAnchoredAt(absolute) && !dummy.isRemoved()
        );
    }

    /**
     * Every non-removed {@link DamageDummyEntity} inside the same local
     * pedestal search AABB, regardless of anchor state. Used only for
     * lifecycle assertions where a leaked standalone entity would matter:
     * {@code anchoredDummies} cannot prove a dummy did not simply clear its
     * anchor and stay alive.
     */
    private static List<DamageDummyEntity> nearbyDamageDummies(
            GameTestHelper helper,
            BlockPos relativePos
    ) {
        BlockPos absolute = helper.absolutePos(relativePos);
        AABB search = new AABB(absolute).inflate(2.0);
        return helper.getLevel().getEntitiesOfClass(
                DamageDummyEntity.class,
                search,
                dummy -> !dummy.isRemoved()
        );
    }

    private static boolean require(
            GameTestHelper helper,
            boolean condition,
            String message
    ) {
        if (!condition) {
            helper.fail(message);
            return false;
        }
        return true;
    }

    /**
     * Polls {@code condition} every server tick until it holds or the attempt
     * budget is exhausted, then runs {@code onSuccess}. GameTest callbacks run
     * after level/entity ticking in the same server tick, so a one-tick poll
     * observes post-tick state; the bounded retry tolerates chunk entity
     * ticking lag under load without weakening the assertion.
     */
    private static void pollUntil(
            GameTestHelper helper,
            int attempts,
            BooleanSupplier condition,
            String failureMessage,
            Runnable onSuccess
    ) {
        if (condition.getAsBoolean()) {
            onSuccess.run();
            return;
        }
        if (attempts <= 0) {
            helper.fail(failureMessage);
            return;
        }
        helper.runAfterDelay(1, () -> pollUntil(
                helper,
                attempts - 1,
                condition,
                failureMessage,
                onSuccess
        ));
    }

    private static LivingEntity spawnAttacker(GameTestHelper helper, BlockPos pos) {
        LivingEntity attacker = helper.spawnWithNoFreeWill(
                EntityType.ZOMBIE,
                pos
        );
        if (attacker == null) {
            throw new AssertionError("unable to spawn zombie attacker");
        }
        return attacker;
    }

    /**
     * A + L: block/item/block-entity registration and the physical
     * low-profile plate collision/selection shape.
     */
    private static void damageDummyBlockRegistration(GameTestHelper helper) {
        GameTestCodecVerifier.verifyFunctionInstance(helper);

        Optional<Holder.Reference<Block>> blockHolder =
                BuiltInRegistries.BLOCK.get(id("damage_dummy"));
        if (blockHolder.isEmpty()
                || !(blockHolder.get().value() instanceof DamageDummyBlock block)) {
            throw new AssertionError(
                    "damagenexus:damage_dummy is not a registered DamageDummyBlock"
            );
        }

        Optional<Holder.Reference<Item>> itemHolder =
                BuiltInRegistries.ITEM.get(id("damage_dummy"));
        if (itemHolder.isEmpty()
                || !(itemHolder.get().value() instanceof BlockItem blockItem)
                || blockItem.getBlock() != block) {
            throw new AssertionError(
                    "damagenexus:damage_dummy has no matching BlockItem"
            );
        }

        if (BuiltInRegistries.BLOCK_ENTITY_TYPE
                .get(id("damage_dummy"))
                .isEmpty()) {
            throw new AssertionError(
                    "damagenexus:damage_dummy block entity type is not registered"
            );
        }
        if (!ModBlockEntityTypes.DAMAGE_DUMMY.get()
                .isValid(block.defaultBlockState())) {
            throw new AssertionError(
                    "block entity type does not accept the pedestal block"
            );
        }

        if (block.defaultBlockState().getPistonPushReaction()
                != PushReaction.BLOCK) {
            throw new AssertionError(
                    "pedestal must be non-piston-movable"
            );
        }

        BlockPos absolute = helper.absolutePos(blockTestPos());
        VoxelShape shape = block.defaultBlockState()
                .getShape(helper.getLevel(), absolute);
        VoxelShape collision = block.defaultBlockState()
                .getCollisionShape(helper.getLevel(), absolute);
        if (shape.isEmpty()
                || Math.abs(shape.bounds().getYsize()
                - DamageDummyBlock.BASE_HEIGHT) > 1.0E-6) {
            throw new AssertionError(
                    "pedestal selection shape is not the low-profile "
                            + "plate height"
            );
        }
        if (collision.isEmpty()
                || Math.abs(collision.bounds().getYsize()
                - DamageDummyBlock.BASE_HEIGHT) > 1.0E-6) {
            throw new AssertionError(
                    "pedestal collision shape is not the low-profile "
                            + "plate height"
            );
        }
        helper.succeed();
    }

    /**
     * B: placement creates exactly one anchored dummy standing directly on
     * the low-profile plate at the single authoritative anchored position
     * (centered X/Z, {@code DamageDummyBlock.BASE_HEIGHT} above the block).
     */
    private static void damageDummyBlockAnchor(GameTestHelper helper) {
        GameTestCodecVerifier.verifyFunctionInstance(helper);
        BlockPos pos = blockTestPos();
        BlockPos absolute = helper.absolutePos(pos);
        helper.setBlock(pos, ModBlocks.DAMAGE_DUMMY.get());
        helper.runAfterDelay(60, () -> {
            List<DamageDummyEntity> dummies = anchoredDummies(helper, pos);
            if (!require(helper, dummies.size() == 1,
                    "expected exactly one anchored dummy, found "
                            + dummies.size())) {
                return;
            }
            DamageDummyEntity dummy = dummies.get(0);
            if (!require(helper, dummy.isAnchoredAt(absolute),
                    "dummy is not anchored at the pedestal")) {
                return;
            }
            // C: the authoritative anchored position comes from the single
            // shared method; its Y must be exactly the plate base height.
            Vec3 expected = DamageDummyEntity.getAnchoredPosition(absolute);
            if (!require(helper,
                    expected.y() == absolute.getY()
                            + DamageDummyBlock.BASE_HEIGHT,
                    "anchored position Y does not use the plate base height: "
                            + expected.y() + " != " + absolute.getY()
                            + " + " + DamageDummyBlock.BASE_HEIGHT)) {
                return;
            }
            boolean atFeet = Math.abs(dummy.getX() - expected.x()) <= 0.01
                    && Math.abs(dummy.getY() - expected.y()) <= 0.01
                    && Math.abs(dummy.getZ() - expected.z()) <= 0.01;
            if (!require(helper, atFeet,
                    "dummy feet " + dummy.position()
                            + " != expected " + expected)) {
                return;
            }
            helper.succeed();
        });
    }

    /**
     * Fresh-placement regression: a normal {@link BlockItem} placement must
     * establish the anchored dummy synchronously as part of the placement
     * action, never 20/40 ticks later through periodic reconciliation. The
     * real item placement path is exercised through
     * {@code ModItems.DAMAGE_DUMMY.get().place(...)} with a mock player,
     * which runs {@link BlockItem#place} and therefore
     * {@link DamageDummyBlock#setPlacedBy}; a plain {@code helper.setBlock}
     * would only exercise the direct-world-mutation reconciliation path and
     * would not prove the placement callback contract.
     *
     * <p>The first existence assertions run in the same server tick as the
     * placement call (the helper itself is fully synchronous, and the entity
     * is added to the level before {@code setPlacedBy} returns). After at
     * least two full reconciliation intervals the test re-asserts exactly one
     * anchored dummy with the same UUID, proving the periodic reconcile did
     * not add a duplicate after fresh initialization.</p>
     */
    private static void damageDummyFreshPlacement(GameTestHelper helper) {
        GameTestCodecVerifier.verifyFunctionInstance(helper);
        BlockPos pos = blockTestPos();
        BlockPos absolute = helper.absolutePos(pos);

        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        BlockPlaceContext context = new BlockPlaceContext(
                player,
                InteractionHand.MAIN_HAND,
                new ItemStack(ModItems.DAMAGE_DUMMY.get()),
                new BlockHitResult(
                        Vec3.atCenterOf(absolute),
                        Direction.DOWN,
                        absolute,
                        true
                )
        );
        InteractionResult placement = ModItems.DAMAGE_DUMMY.get().place(context);
        if (!require(helper, placement.consumesAction(),
                "BlockItem placement of the damage dummy was not consumed")) {
            return;
        }

        if (!require(helper,
                helper.getBlockState(pos).is(ModBlocks.DAMAGE_DUMMY.get()),
                "BlockItem placement did not place the pedestal")) {
            return;
        }
        DamageDummyBlockEntity blockEntity = helper.getBlockEntity(
                pos,
                DamageDummyBlockEntity.class
        );
        if (!require(helper, blockEntity != null,
                "BlockItem placement did not create the block entity")) {
            return;
        }

        UUID linked = blockEntity.linkedDummyUuid();
        List<DamageDummyEntity> dummies = anchoredDummies(helper, pos);
        if (!require(helper, dummies.size() == 1,
                "fresh placement did not create exactly one anchored dummy "
                        + "immediately, found " + dummies.size())) {
            return;
        }
        DamageDummyEntity dummy = dummies.get(0);
        if (!require(helper,
                linked != null && linked.equals(dummy.getUUID()),
                "linked UUID is not populated immediately after fresh "
                        + "placement")) {
            return;
        }
        if (!require(helper, !dummy.isRemoved() && dummy.isAlive(),
                "freshly placed anchored dummy is removed or not alive")) {
            return;
        }
        Vec3 expected = DamageDummyEntity.getAnchoredPosition(absolute);
        boolean atFeet = Math.abs(dummy.getX() - expected.x()) <= 0.01
                && Math.abs(dummy.getY() - expected.y()) <= 0.01
                && Math.abs(dummy.getZ() - expected.z()) <= 0.01;
        if (!require(helper, atFeet,
                "freshly placed dummy feet " + dummy.position()
                        + " != expected " + expected)) {
            return;
        }

        // Zero-duplicate follow-up: wait beyond at least two normal
        // reconciliation intervals (2 * 20 ticks), then prove the first
        // periodic reconciliations did not spawn a second entity.
        helper.runAfterDelay(50, () -> {
            List<DamageDummyEntity> later = anchoredDummies(helper, pos);
            if (!require(helper, later.size() == 1,
                    "periodic reconciliation created a duplicate after "
                            + "fresh placement")) {
                return;
            }
            if (!require(helper,
                    later.get(0).getUUID().equals(dummy.getUUID()),
                    "periodic reconciliation replaced the freshly placed "
                            + "dummy UUID")) {
                return;
            }
            helper.succeed();
        });
    }

    /** C: multiple reconciliation cycles never spawn a second dummy. */
    private static void damageDummyBlockSingle(GameTestHelper helper) {
        GameTestCodecVerifier.verifyFunctionInstance(helper);
        BlockPos pos = blockTestPos();
        helper.setBlock(pos, ModBlocks.DAMAGE_DUMMY.get());
        helper.runAfterDelay(60, () -> {
            if (!require(helper, anchoredDummies(helper, pos).size() == 1,
                    "expected one dummy after first reconciliation")) {
                return;
            }
            helper.runAfterDelay(60, () -> {
                if (!require(helper, anchoredDummies(helper, pos).size() == 1,
                        "second reconciliation spawned a duplicate dummy")) {
                    return;
                }
                helper.succeed();
            });
        });
    }

    /**
     * D: reload reconciliation. After a simulated reload where the linked
     * entity has not loaded yet, the first reconciliation must not spawn a
     * replacement or destroy the pedestal. A later reconciliation treats the
     * still-missing persisted identity as a broken logical pair and destroys
     * the pedestal without resurrection.
     *
     * <p>The reconciliation phase is driven deterministically through
     * {@link DamageDummyBlockEntity#reconcileNow} instead of waiting for the
     * 20-tick ticker, so the "first" and "later" reconciliations are explicit
     * and never depend on tick timing.</p>
     */
    private static void damageDummyBlockReloadReconcile(GameTestHelper helper) {
        GameTestCodecVerifier.verifyFunctionInstance(helper);
        BlockPos pos = blockTestPos();
        BlockPos absolute = helper.absolutePos(pos);
        helper.setBlock(pos, ModBlocks.DAMAGE_DUMMY.get());
        helper.runAfterDelay(60, () -> {
            List<DamageDummyEntity> dummies = anchoredDummies(helper, pos);
            if (!require(helper, dummies.size() == 1,
                    "expected one anchored dummy before simulated reload")) {
                return;
            }
            DamageDummyEntity original = dummies.get(0);
            UUID originalUuid = original.getUUID();
            DamageDummyBlockEntity blockEntity = helper.getBlockEntity(
                    pos,
                    DamageDummyBlockEntity.class
            );
            if (!require(helper,
                    blockEntity != null
                            && originalUuid.equals(
                            blockEntity.linkedDummyUuid()),
                    "block entity link missing before simulated reload")) {
                return;
            }

            // Simulate the chunk/entity load race: the linked entity has not
            // loaded yet (removed from the level), while the ownership state
            // (linked UUID) is already known to a fresh block entity that has
            // never reconciled.
            original.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
            DamageDummyBlockEntity reloaded = DamageDummyBlockEntity
                    .createUnreconciled(
                    absolute,
                    helper.getLevel().getBlockState(absolute),
                    originalUuid
            );
            if (!require(helper, originalUuid.equals(reloaded.linkedDummyUuid()),
                    "fresh block entity did not retain the linked UUID")) {
                return;
            }

            // First reconciliation: the stored UUID is unresolved, no local
            // entity exists, and initial sync is not complete. It must only
            // record completion, never spawn a duplicate.
            reloaded.reconcileNow(helper.getLevel());
            if (!require(helper, anchoredDummies(helper, pos).isEmpty(),
                    "first reconciliation spawned a dummy during the "
                            + "unresolved ownership window")) {
                return;
            }
            if (!require(helper,
                    helper.getBlockState(pos).is(ModBlocks.DAMAGE_DUMMY.get()),
                    "first unresolved reconciliation destroyed the pedestal")) {
                return;
            }
            if (!require(helper,
                    originalUuid.equals(reloaded.linkedDummyUuid()),
                    "first unresolved reconciliation cleared the bound UUID")) {
                return;
            }

            // Later reconciliation: still unresolved, so the persisted bound
            // identity terminates the logical pair. No replacement is legal.
            reloaded.reconcileNow(helper.getLevel());
            if (!require(helper,
                    !helper.getBlockState(pos).is(ModBlocks.DAMAGE_DUMMY.get()),
                    "later unresolved reconciliation kept the broken pedestal")) {
                return;
            }
            if (!require(helper, anchoredDummies(helper, pos).isEmpty(),
                    "bound-missing reconciliation spawned a replacement")) {
                return;
            }
            helper.succeed();
        });
    }

    /** External discard immediately terminates both halves of the pair. */
    private static void damageDummyExternalDiscard(GameTestHelper helper) {
        GameTestCodecVerifier.verifyFunctionInstance(helper);
        BlockPos pos = blockTestPos();
        helper.setBlock(pos, ModBlocks.DAMAGE_DUMMY.get());
        helper.runAfterDelay(60, () -> {
            List<DamageDummyEntity> dummies = anchoredDummies(helper, pos);
            if (!require(helper, dummies.size() == 1,
                    "expected one anchored dummy before external discard")) {
                return;
            }
            DamageDummyEntity removed = dummies.get(0);
            removed.discard();
            pollUntil(
                    helper,
                    10,
                    () -> removed.isRemoved()
                            && !helper.getBlockState(pos)
                            .is(ModBlocks.DAMAGE_DUMMY.get())
                            && anchoredDummies(helper, pos).isEmpty(),
                    "external discard did not terminate the pedestal pair",
                    helper::succeed
            );
        });
    }

    /** LivingEntity.kill uses generic-kill damage, then KILLED removal. */
    private static void damageDummyKill(GameTestHelper helper) {
        GameTestCodecVerifier.verifyFunctionInstance(helper);
        BlockPos pos = blockTestPos();
        helper.setBlock(pos, ModBlocks.DAMAGE_DUMMY.get());
        helper.runAfterDelay(60, () -> {
            List<DamageDummyEntity> dummies = anchoredDummies(helper, pos);
            if (!require(helper, dummies.size() == 1,
                    "expected one anchored dummy before kill")) {
                return;
            }
            DamageDummyEntity killed = dummies.get(0);
            killed.kill(helper.getLevel());
            if (!require(helper, killed.isDeadOrDying(),
                    "generic-kill damage was incorrectly protected")) {
                return;
            }
            pollUntil(
                    helper,
                    35,
                    () -> killed.isRemoved()
                            && !helper.getBlockState(pos)
                            .is(ModBlocks.DAMAGE_DUMMY.get())
                            && anchoredDummies(helper, pos).isEmpty(),
                    "kill did not terminate the pedestal pair",
                    helper::succeed
            );
        });
    }

    /** E: duplicate anchored dummies are reduced to exactly one. */
    private static void damageDummyBlockDuplicateRepair(GameTestHelper helper) {
        GameTestCodecVerifier.verifyFunctionInstance(helper);
        BlockPos pos = blockTestPos();
        BlockPos absolute = helper.absolutePos(pos);
        helper.setBlock(pos, ModBlocks.DAMAGE_DUMMY.get());
        helper.runAfterDelay(60, () -> {
            if (!require(helper, anchoredDummies(helper, pos).size() == 1,
                    "expected one anchored dummy before duplicate test")) {
                return;
            }
            DamageDummyEntity bound = anchoredDummies(helper, pos).get(0);
            UUID boundUuid = bound.getUUID();
            DamageDummyEntity duplicate = spawnDummy(helper, pos);
            duplicate.bindToAnchor(absolute, 0.0F);
            if (!require(helper, anchoredDummies(helper, pos).size() == 2,
                    "duplicate dummy did not register at the same anchor")) {
                return;
            }
            helper.runAfterDelay(60, () -> {
                List<DamageDummyEntity> survivors =
                        anchoredDummies(helper, pos);
                if (!require(helper, survivors.size() == 1,
                        "duplicate repair did not reduce to one dummy")) {
                    return;
                }
                if (!require(helper,
                        helper.getBlockState(pos).is(ModBlocks.DAMAGE_DUMMY.get()),
                        "duplicate cleanup destroyed the pedestal")) {
                    return;
                }
                DamageDummyBlockEntity blockEntity = helper.getBlockEntity(
                        pos,
                        DamageDummyBlockEntity.class
                );
                if (!require(helper,
                        survivors.get(0) == bound
                                && !bound.isRemoved()
                                && duplicate.isRemoved()
                                && blockEntity != null
                                && boundUuid.equals(blockEntity.linkedDummyUuid()),
                        "duplicate cleanup did not preserve the bound keeper")) {
                    return;
                }
                if (!require(helper, survivors.get(0).isAnchoredAt(absolute),
                        "surviving dummy lost its anchor")) {
                    return;
                }
                helper.succeed();
            });
        });
    }

    /**
     * F: breaking the pedestal removes the owned entity. The oracle is the
     * original entity reference itself (removed from the world, then
     * unresolvable by UUID), not merely a lost anchor: {@code anchoredDummies}
     * alone would falsely pass if the entity cleared its anchor and stayed
     * alive as a standalone dummy.
     */
    private static void damageDummyBlockBreak(GameTestHelper helper) {
        GameTestCodecVerifier.verifyFunctionInstance(helper);
        BlockPos pos = blockTestPos();
        helper.setBlock(pos, ModBlocks.DAMAGE_DUMMY.get());
        helper.runAfterDelay(60, () -> {
            List<DamageDummyEntity> dummies = anchoredDummies(helper, pos);
            if (!require(helper, dummies.size() == 1,
                    "expected one anchored dummy before block break")) {
                return;
            }
            DamageDummyEntity original = dummies.get(0);
            UUID originalUuid = original.getUUID();
            helper.destroyBlock(pos);
            helper.runAfterDelay(10, () -> {
                pollUntil(
                        helper,
                        20,
                        original::isRemoved,
                        "owned dummy was not removed after pedestal destruction",
                        () -> {
                            if (!require(helper,
                                    helper.getLevel().getEntity(originalUuid) == null,
                                    "owned dummy UUID still resolves after "
                                            + "pedestal destruction")) {
                                return;
                            }
                            if (!require(helper,
                                    nearbyDamageDummies(helper, pos).isEmpty(),
                                    "owned dummy survived pedestal destruction "
                                            + "as a leaked standalone entity")) {
                                return;
                            }
                            helper.assertBlockNotPresent(
                                    ModBlocks.DAMAGE_DUMMY.get(),
                                    pos
                            );
                            helper.succeed();
                        }
                );
            });
        });
    }

    /**
     * Entity-side fail-closed regression: the anchored dummy must discard
     * itself when its pedestal disappears, independently of the block entity
     * cleanup path. The pedestal is replaced with air using
     * {@link Block#UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS}, which removes the
     * block entity without running {@code preRemoveSideEffects}; only the
     * entity's own self-validation can remove it afterwards.
     */
    private static void damageDummySelfCleanup(GameTestHelper helper) {
        GameTestCodecVerifier.verifyFunctionInstance(helper);
        BlockPos pos = blockTestPos();
        BlockPos absolute = helper.absolutePos(pos);
        helper.setBlock(pos, ModBlocks.DAMAGE_DUMMY.get());
        helper.runAfterDelay(60, () -> {
            List<DamageDummyEntity> dummies = anchoredDummies(helper, pos);
            if (!require(helper, dummies.size() == 1,
                    "expected one anchored dummy before self-cleanup test")) {
                return;
            }
            DamageDummyEntity dummy = dummies.get(0);
            UUID uuid = dummy.getUUID();
            helper.getLevel().setBlock(
                    absolute,
                    Blocks.AIR.defaultBlockState(),
                    Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS
            );
            helper.runAfterDelay(1, () -> pollUntil(
                    helper,
                    20,
                    dummy::isRemoved,
                    "anchored orphan did not discard itself after its "
                            + "pedestal was replaced",
                    () -> {
                        if (!require(helper,
                                helper.getLevel().getEntity(uuid) == null,
                                "self-discarded dummy UUID still resolves")) {
                            return;
                        }
                        if (!require(helper,
                                nearbyDamageDummies(helper, pos).isEmpty(),
                                "entity self-cleanup left a leaked dummy")) {
                            return;
                        }
                        helper.succeed();
                    }
            ));
        });
    }

    /**
     * Cleanup safety net regression: pedestal destruction must not depend on
     * the persisted linked UUID. The stored link is nulled through the
     * dedicated test-support hook, then the pedestal is destroyed before any
     * reconcile cycle can re-adopt the dummy; the local safety scan must still
     * remove the real anchored dummy.
     */
    private static void damageDummyStaleUuidRemoval(GameTestHelper helper) {
        GameTestCodecVerifier.verifyFunctionInstance(helper);
        BlockPos pos = blockTestPos();
        helper.setBlock(pos, ModBlocks.DAMAGE_DUMMY.get());
        helper.runAfterDelay(60, () -> {
            List<DamageDummyEntity> dummies = anchoredDummies(helper, pos);
            if (!require(helper, dummies.size() == 1,
                    "expected one anchored dummy before stale UUID test")) {
                return;
            }
            DamageDummyEntity dummy = dummies.get(0);
            UUID uuid = dummy.getUUID();
            DamageDummyBlockEntity blockEntity = helper.getBlockEntity(
                    pos,
                    DamageDummyBlockEntity.class
            );
            if (!require(helper, blockEntity != null,
                    "missing block entity before stale UUID test")) {
                return;
            }
            blockEntity.testOverrideLinkedUuid(null);
            helper.destroyBlock(pos);
            helper.runAfterDelay(10, () -> pollUntil(
                    helper,
                    20,
                    dummy::isRemoved,
                    "dummy survived pedestal destruction with a null "
                            + "linked UUID",
                    () -> {
                        if (!require(helper,
                                helper.getLevel().getEntity(uuid) == null,
                                "dummy UUID still resolves after stale-link "
                                        + "destruction")) {
                            return;
                        }
                        if (!require(helper,
                                nearbyDamageDummies(helper, pos).isEmpty(),
                                "stale-link cleanup left a leaked dummy")) {
                            return;
                        }
                        helper.succeed();
                    }
            ));
        });
    }

    /**
     * Duplicate destruction regression: when the pedestal is destroyed while
     * two dummies are anchored to it, both must be removed. The pedestal is
     * destroyed in the same callback that adds the duplicate, so no reconcile
     * cycle can converge the pair before cleanup must remove all of them.
     */
    private static void damageDummyDestroyDuplicates(GameTestHelper helper) {
        GameTestCodecVerifier.verifyFunctionInstance(helper);
        BlockPos pos = blockTestPos();
        BlockPos absolute = helper.absolutePos(pos);
        helper.setBlock(pos, ModBlocks.DAMAGE_DUMMY.get());
        helper.runAfterDelay(60, () -> {
            List<DamageDummyEntity> initial = anchoredDummies(helper, pos);
            if (!require(helper, initial.size() == 1,
                    "expected one anchored dummy before duplicate "
                            + "destruction")) {
                return;
            }
            DamageDummyEntity keeper = initial.get(0);
            UUID keeperUuid = keeper.getUUID();
            DamageDummyEntity duplicate = spawnDummy(helper, pos);
            duplicate.bindToAnchor(absolute, 0.0F);
            UUID duplicateUuid = duplicate.getUUID();
            helper.destroyBlock(pos);
            helper.runAfterDelay(10, () -> pollUntil(
                    helper,
                    20,
                    () -> keeper.isRemoved() && duplicate.isRemoved(),
                    "pedestal destruction did not remove every anchored "
                            + "duplicate",
                    () -> {
                        if (!require(helper,
                                helper.getLevel().getEntity(keeperUuid) == null,
                                "keeper UUID still resolves after duplicate "
                                        + "destruction")) {
                            return;
                        }
                        if (!require(helper,
                                helper.getLevel().getEntity(duplicateUuid) == null,
                                "duplicate UUID still resolves after duplicate "
                                        + "destruction")) {
                            return;
                        }
                        if (!require(helper,
                                nearbyDamageDummies(helper, pos).isEmpty(),
                                "duplicate destruction leaked dummies")) {
                            return;
                        }
                        helper.succeed();
                    }
            ));
        });
    }

    /** A duplicate cannot replace an already-established bound UUID. */
    private static void damageDummyBoundReplacementRejected(
            GameTestHelper helper
    ) {
        GameTestCodecVerifier.verifyFunctionInstance(helper);
        BlockPos pos = blockTestPos();
        BlockPos absolute = helper.absolutePos(pos);
        helper.setBlock(pos, ModBlocks.DAMAGE_DUMMY.get());
        helper.runAfterDelay(60, () -> {
            List<DamageDummyEntity> initial = anchoredDummies(helper, pos);
            if (!require(helper, initial.size() == 1,
                    "expected one bound dummy before replacement test")) {
                return;
            }
            DamageDummyEntity bound = initial.get(0);
            DamageDummyEntity replacement = spawnDummy(helper, pos);
            replacement.bindToAnchor(absolute, 0.0F);
            if (!require(helper, anchoredDummies(helper, pos).size() == 2,
                    "replacement candidate was not anchored")) {
                return;
            }

            bound.discard();
            pollUntil(
                    helper,
                    10,
                    () -> bound.isRemoved()
                            && replacement.isRemoved()
                            && !helper.getBlockState(pos)
                            .is(ModBlocks.DAMAGE_DUMMY.get())
                            && anchoredDummies(helper, pos).isEmpty(),
                    "bound removal silently adopted the replacement candidate",
                    helper::succeed
            );
        });
    }

    /**
     * Standalone regression: a {@code /summon}-style dummy without an anchor
     * must be completely unaffected by the destruction of a nearby pedestal,
     * even though it sits inside the pedestal's local search AABB. Cleanup
     * predicates must stay scoped to {@code isAnchoredAt(anchorPos)}.
     */
    private static void damageDummyStandaloneUnaffected(GameTestHelper helper) {
        GameTestCodecVerifier.verifyFunctionInstance(helper);
        BlockPos pos = blockTestPos();
        helper.setBlock(pos, ModBlocks.DAMAGE_DUMMY.get());
        DamageDummyEntity standalone = spawnDummy(
                helper,
                pos.offset(1, 0, 1)
        );
        UUID standaloneUuid = standalone.getUUID();
        helper.runAfterDelay(60, () -> {
            List<DamageDummyEntity> anchored = anchoredDummies(helper, pos);
            if (!require(helper, anchored.size() == 1,
                    "expected one anchored dummy for standalone test")) {
                return;
            }
            DamageDummyEntity anchoredDummy = anchored.get(0);
            if (!require(helper,
                    !standalone.isAnchored() && !standalone.isRemoved(),
                    "standalone dummy was anchored/removed before "
                            + "destruction")) {
                return;
            }
            helper.destroyBlock(pos);
            helper.runAfterDelay(10, () -> pollUntil(
                    helper,
                    20,
                    anchoredDummy::isRemoved,
                    "anchored dummy was not removed while the standalone "
                            + "remained",
                    () -> {
                        if (!require(helper,
                                !standalone.isRemoved() && standalone.isAlive(),
                                "pedestal cleanup removed the nearby "
                                        + "standalone dummy")) {
                            return;
                        }
                        if (!require(helper, !standalone.isAnchored(),
                                "standalone dummy gained an anchor")) {
                            return;
                        }
                        if (!require(helper,
                                nearbyDamageDummies(helper, pos).stream()
                                        .allMatch(dummy -> dummy.getUUID()
                                                .equals(standaloneUuid)),
                                "pedestal cleanup left unexpected dummies "
                                        + "nearby")) {
                            return;
                        }
                        helper.succeed();
                    }
            ));
        });
    }

    /** G: anchored nonlethal hit applies, then heals to max next tick. */
    private static void damageDummyAnchoredDamage(GameTestHelper helper) {
        GameTestCodecVerifier.verifyFunctionInstance(helper);
        BlockPos pos = blockTestPos();
        helper.setBlock(pos, ModBlocks.DAMAGE_DUMMY.get());
        helper.runAfterDelay(60, () -> {
            List<DamageDummyEntity> dummies = anchoredDummies(helper, pos);
            if (!require(helper, dummies.size() == 1,
                    "expected one anchored dummy for damage test")) {
                return;
            }
            DamageDummyEntity dummy = dummies.get(0);
            LivingEntity attacker = spawnAttacker(
                    helper,
                    new BlockPos(1, 2, 3)
            );
            DamageSource source = attacker.damageSources().mobAttack(attacker);
            float before = dummy.getHealth();
            // Pipeline regression: the anchored dummy must still produce a
            // normal DamageNexus settlement for the hit (immortality is
            // implemented after settlement, never by rejecting damage).
            settlementCaptureTarget = dummy.getUUID();
            settlementCaptureObserved = false;
            settlementCaptureHealthDamage = 0.0F;
            if (!require(helper, dummy.hurtServer(helper.getLevel(), source, 5.0F),
                    "anchored dummy rejected nonlethal damage")) {
                return;
            }
            boolean settled = settlementCaptureObserved;
            float settledHealthDamage = settlementCaptureHealthDamage;
            settlementCaptureTarget = null;
            if (!require(helper, settled,
                    "DamageNexus did not publish a settlement for the "
                            + "anchored dummy hit")) {
                return;
            }
            if (!require(helper, settledHealthDamage > 0.0F,
                    "DamageNexus settlement observed no health damage on "
                            + "the anchored dummy")) {
                return;
            }
            float after = dummy.getHealth();
            if (!require(helper, after > 0.0F && before - after >= 4.5F,
                    "anchored dummy did not take nonlethal damage: "
                            + before + " -> " + after)) {
                return;
            }
            if (!require(helper, dummy.isAlive() && dummy.isAnchoredAt(
                    helper.absolutePos(pos)),
                    "anchored dummy no longer alive/anchor after nonlethal hit")) {
                return;
            }
            helper.runAfterDelay(2, () -> pollUntil(
                    helper,
                    6,
                    () -> dummy.isAlive()
                            && !dummy.isRemoved()
                            && dummy.getHealth() == dummy.getMaxHealth(),
                    "anchored dummy did not heal to max after nonlethal damage",
                    helper::succeed
            ));
        });
    }

    /**
     * H: a clearly lethal hit on the anchored dummy is processed normally by
     * DamageNexus and makes the dummy immortal only after the damage lands.
     * Immediately after the attack the entity must exist with an unchanged
     * UUID and no permanent death state; one server tick later health must be
     * back at the recorded maximum.
     */
    private static void damageDummyAnchoredLethal(GameTestHelper helper) {
        GameTestCodecVerifier.verifyFunctionInstance(helper);
        BlockPos pos = blockTestPos();
        BlockPos absolute = helper.absolutePos(pos);
        helper.setBlock(pos, ModBlocks.DAMAGE_DUMMY.get());
        helper.runAfterDelay(60, () -> {
            List<DamageDummyEntity> dummies = anchoredDummies(helper, pos);
            if (!require(helper, dummies.size() == 1,
                    "expected one anchored dummy for lethal test")) {
                return;
            }
            DamageDummyEntity dummy = dummies.get(0);
            UUID uuid = dummy.getUUID();
            float maxHealth = dummy.getMaxHealth();
            LivingEntity attacker = spawnAttacker(
                    helper,
                    new BlockPos(1, 2, 3)
            );
            DamageSource source = attacker.damageSources().mobAttack(attacker);
            // Prove the hit goes through the normal DamageNexus settlement
            // path rather than being bypassed or cancelled.
            settlementCaptureTarget = dummy.getUUID();
            settlementCaptureObserved = false;
            settlementCaptureHealthDamage = 0.0F;
            boolean accepted = dummy.hurtServer(
                    helper.getLevel(),
                    source,
                    100.0F
            );
            boolean settled = settlementCaptureObserved;
            float settledHealthDamage = settlementCaptureHealthDamage;
            settlementCaptureTarget = null;
            if (!require(helper, accepted,
                    "anchored dummy rejected lethal damage")) {
                return;
            }
            if (!require(helper, settled && settledHealthDamage > 0.0F,
                    "DamageNexus did not settle the lethal anchored hit")) {
                return;
            }
            // Immediately after the attack: same identity, no removal, no
            // permanent death state, still a valid living target.
            if (!require(helper, !dummy.isRemoved(),
                    "anchored dummy was removed by lethal damage")) {
                return;
            }
            if (!require(helper, dummy.isAlive(),
                    "anchored dummy is not alive after lethal damage")) {
                return;
            }
            if (!require(helper, dummy.getUUID().equals(uuid),
                    "lethal damage replaced the anchored dummy UUID")) {
                return;
            }
            if (!require(helper, dummy.isAnchoredAt(absolute),
                    "lethal damage detached the anchored dummy")) {
                return;
            }
            // The pending restore must return health to the recorded maximum
            // on the next server tick. Poll briefly to tolerate chunk entity
            // ticking lag under load.
            helper.runAfterDelay(1, () -> pollUntil(
                    helper,
                    6,
                    () -> !dummy.isRemoved()
                            && dummy.isAlive()
                            && dummy.getUUID().equals(uuid)
                            && dummy.getHealth() == maxHealth,
                    "anchored dummy did not survive with health restored "
                            + "to the recorded max",
                    helper::succeed
            ));
        });
    }

    /** I: attribute edits survive a lethal hit on the same entity. */
    private static void damageDummyAttributeRetention(GameTestHelper helper) {
        GameTestCodecVerifier.verifyFunctionInstance(helper);
        BlockPos pos = blockTestPos();
        BlockPos absolute = helper.absolutePos(pos);
        helper.setBlock(pos, ModBlocks.DAMAGE_DUMMY.get());
        helper.runAfterDelay(60, () -> {
            List<DamageDummyEntity> dummies = anchoredDummies(helper, pos);
            if (!require(helper, dummies.size() == 1,
                    "expected one anchored dummy for attribute retention")) {
                return;
            }
            DamageDummyEntity dummy = dummies.get(0);
            if (!require(helper,
                    DamageDummyAttributes.setBaseValue(
                            dummy, MAX_HEALTH_KEY, 40.0D)
                            && DamageDummyAttributes.setBaseValue(
                            dummy, CRIT_CHANCE_KEY, 0.5D),
                    "unable to edit attributes before lethal hit")) {
                return;
            }
            UUID uuid = dummy.getUUID();
            LivingEntity attacker = spawnAttacker(
                    helper,
                    new BlockPos(1, 2, 3)
            );
            DamageSource source = attacker.damageSources().mobAttack(attacker);
            if (!require(helper,
                    dummy.hurtServer(helper.getLevel(), source, 200.0F),
                    "anchored dummy rejected lethal damage")) {
                return;
            }
            if (!require(helper, dummy.isAlive()
                            && dummy.getUUID().equals(uuid)
                            && dummy.isAnchoredAt(absolute),
                    "lethal damage broke the anchored dummy identity")) {
                return;
            }
            helper.runAfterDelay(2, () -> pollUntil(
                    helper,
                    6,
                    () -> dummy.isAlive()
                            && !dummy.isRemoved()
                            && dummy.getUUID().equals(uuid)
                            && dummy.getHealth() == 40.0F,
                    "anchored dummy did not heal to the edited max health",
                    () -> {
                        if (!require(helper,
                                DamageDummyAttributes.getBaseValue(
                                        dummy, MAX_HEALTH_KEY)
                                        .orElse(-1.0D) == 40.0D,
                                "MAX_HEALTH base did not survive the lethal "
                                        + "hit")) {
                            return;
                        }
                        if (!require(helper,
                                DamageDummyAttributes.getBaseValue(
                                        dummy, CRIT_CHANCE_KEY)
                                        .orElse(-1.0D) == 0.5D,
                                "CRIT_CHANCE base did not survive the lethal "
                                        + "hit")) {
                            return;
                        }
                        helper.succeed();
                    }
            ));
        });
    }

    /**
     * J: a standalone (unanchored) dummy is a static target and still dies
     * normally. First verify it does not drift (no gravity, no movement, no
     * AI), then verify lethal damage kills and removes it.
     */
    private static void damageDummyStandaloneDeath(GameTestHelper helper) {
        GameTestCodecVerifier.verifyFunctionInstance(helper);
        DamageDummyEntity dummy = spawnDummy(helper, new BlockPos(1, 2, 1));
        if (!require(helper, !dummy.isAnchored(),
                "manually summoned dummy unexpectedly anchored")) {
            return;
        }
        Vec3 spawnPosition = dummy.position();
        helper.runAfterDelay(20, () -> {
            if (!require(helper, dummy.isAlive() && !dummy.isRemoved(),
                    "standalone dummy disappeared while idle")) {
                return;
            }
            if (!require(helper,
                    dummy.position().distanceToSqr(spawnPosition) <= 0.0001,
                    "standalone dummy moved without gravity/AI: "
                            + spawnPosition + " -> " + dummy.position())) {
                return;
            }
            LivingEntity attacker = spawnAttacker(
                    helper,
                    new BlockPos(1, 2, 3)
            );
            DamageSource source = attacker.damageSources().mobAttack(attacker);
            if (!require(helper,
                    dummy.hurtServer(helper.getLevel(), source, 1000.0F),
                    "standalone dummy rejected lethal damage")) {
                return;
            }
            if (!require(helper, dummy.isDeadOrDying(),
                    "standalone dummy did not die from lethal damage")) {
                return;
            }
            helper.runAfterDelay(25, () -> pollUntil(
                    helper,
                    60,
                    dummy::isRemoved,
                    "standalone dummy was not removed after death",
                    helper::succeed
            ));
        });
    }

    /**
     * K: BlockEntity linked UUID and entity anchor BlockPos survive normal
     * serialization round-trips.
     */
    private static void damageDummyLinkPersistence(GameTestHelper helper) {
        GameTestCodecVerifier.verifyFunctionInstance(helper);
        BlockPos pos = blockTestPos();
        BlockPos absolute = helper.absolutePos(pos);
        helper.setBlock(pos, ModBlocks.DAMAGE_DUMMY.get());
        helper.runAfterDelay(60, () -> {
            List<DamageDummyEntity> dummies = anchoredDummies(helper, pos);
            if (!require(helper, dummies.size() == 1,
                    "expected one anchored dummy for persistence test")) {
                return;
            }
            DamageDummyEntity dummy = dummies.get(0);
            DamageDummyBlockEntity blockEntity = helper.getBlockEntity(
                    pos,
                    DamageDummyBlockEntity.class
            );
            if (!require(helper,
                    blockEntity != null
                            && dummy.getUUID().equals(
                            blockEntity.linkedDummyUuid()),
                    "block entity does not store the linked UUID")) {
                return;
            }

            CompoundTag tag = blockEntity.saveWithFullMetadata(
                    helper.getLevel().registryAccess()
            );
            BlockEntity reloaded = BlockEntity.loadStatic(
                    absolute,
                    helper.getLevel().getBlockState(absolute),
                    tag,
                    helper.getLevel().registryAccess()
            );
            if (!require(helper,
                    reloaded instanceof DamageDummyBlockEntity reloadedBe
                            && dummy.getUUID().equals(
                            reloadedBe.linkedDummyUuid()),
                    "linked UUID did not survive block entity serialization")) {
                return;
            }

            DamageDummyEntity fresh = ModEntityTypes.DAMAGE_DUMMY.get()
                    .create(helper.getLevel(), EntitySpawnReason.COMMAND);
            if (!require(helper, fresh != null,
                    "unable to create fresh dummy for persistence test")) {
                return;
            }
            fresh.restoreFrom(dummy);
            if (!require(helper, fresh.isAnchoredAt(absolute),
                    "anchor BlockPos did not survive entity serialization")) {
                return;
            }
            helper.succeed();
        });
    }

    /** M: placement and reconciliation respect vertical clearance. */
    private static void damageDummyBlockClearance(GameTestHelper helper) {
        GameTestCodecVerifier.verifyFunctionInstance(helper);
        BlockPos pos = blockTestPos();
        // The dummy stands on the 1/16 plate (feet at Y + BASE_HEIGHT, body
        // 1.8 tall, top at Y + 1.8625), so a ceiling one block above the plate
        // still intersects the entity volume.
        BlockPos ceiling = pos.offset(0, 1, 0);
        BlockPos absolute = helper.absolutePos(pos);
        helper.setBlock(ceiling, Blocks.STONE);
        helper.setBlock(pos, ModBlocks.DAMAGE_DUMMY.get());

        BlockPlaceContext context = new BlockPlaceContext(
                helper.getLevel(),
                null,
                InteractionHand.MAIN_HAND,
                ItemStack.EMPTY,
                new BlockHitResult(
                        Vec3.atCenterOf(absolute),
                        Direction.UP,
                        absolute,
                        false
                )
        );
        if (!require(helper,
                ModBlocks.DAMAGE_DUMMY.get().getStateForPlacement(context) == null,
                "placement was allowed under insufficient headroom")) {
            return;
        }

        helper.runAfterDelay(70, () -> {
            if (!require(helper, anchoredDummies(helper, pos).isEmpty(),
                    "dummy spawned into obstructed space")) {
                return;
            }
            helper.setBlock(ceiling, Blocks.AIR);
            helper.runAfterDelay(70, () -> {
                if (!require(helper,
                        anchoredDummies(helper, pos).size() == 1,
                        "dummy did not spawn after clearance was restored")) {
                    return;
                }
                helper.succeed();
            });
        });
    }

    /**
     * N: server-side menu contract. The block entity is a {@link MenuProvider}
     * whose menu knows its anchor position, uses the registered menu type,
     * contains zero slots, stays valid while the pedestal exists and the
     * player is in range, and becomes invalid once the pedestal is removed.
     * The block's empty-hand interaction must consume the action so the held
     * item/placement pipeline never continues.
     */
    private static void damageDummyMenuContract(GameTestHelper helper) {
        GameTestCodecVerifier.verifyFunctionInstance(helper);
        BlockPos pos = blockTestPos();
        BlockPos absolute = helper.absolutePos(pos);
        helper.setBlock(pos, ModBlocks.DAMAGE_DUMMY.get());
        helper.runAfterDelay(20, () -> {
            DamageDummyBlockEntity blockEntity = helper.getBlockEntity(
                    pos,
                    DamageDummyBlockEntity.class
            );
            if (!require(helper, blockEntity instanceof MenuProvider,
                    "block entity is not a MenuProvider")) {
                return;
            }

            Player player = helper.makeMockPlayer(GameType.CREATIVE);
            player.setPos(Vec3.atCenterOf(absolute));

            // Right-click contract: the empty-hand block interaction consumes
            // the action, so nothing continues into item placement/use.
            InteractionResult interaction = helper.getLevel()
                    .getBlockState(absolute)
                    .useWithoutItem(
                            helper.getLevel(),
                            player,
                            new BlockHitResult(
                                    Vec3.atCenterOf(absolute),
                                    Direction.UP,
                                    absolute,
                                    false
                            )
                    );
            if (!require(helper, interaction.consumesAction(),
                    "pedestal right-click does not consume the interaction")) {
                return;
            }

            AbstractContainerMenu menu = ((MenuProvider) blockEntity)
                    .createMenu(0, player.getInventory(), player);
            if (!require(helper, menu instanceof DamageDummyMenu,
                    "block entity did not create a DamageDummyMenu")) {
                return;
            }
            DamageDummyMenu dummyMenu = (DamageDummyMenu) menu;
            if (!require(helper,
                    dummyMenu.anchorPos().equals(absolute),
                    "menu anchorPos does not match the pedestal position")) {
                return;
            }
            if (!require(helper,
                    dummyMenu.getType() == ModMenuTypes.DAMAGE_DUMMY.get(),
                    "menu type is not damagenexus:damage_dummy")) {
                return;
            }
            if (!require(helper, dummyMenu.slots.isEmpty(),
                    "damage dummy menu must not contain any slots")) {
                return;
            }
            if (!require(helper, dummyMenu.stillValid(player),
                    "menu must be valid while the pedestal exists")) {
                return;
            }

            helper.destroyBlock(pos);
            if (!require(helper, !dummyMenu.stillValid(player),
                    "menu must become invalid after the pedestal is removed")) {
                return;
            }
            helper.succeed();
        });
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(DamageNexus.MODID, path);
    }
}
