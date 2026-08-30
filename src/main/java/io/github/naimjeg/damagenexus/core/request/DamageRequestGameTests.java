package io.github.naimjeg.damagenexus.core.request;

import io.github.naimjeg.damagenexus.api.DamageNexusPreMultiplierBuckets;

import io.github.naimjeg.damagenexus.DamageNexus;
import io.github.naimjeg.damagenexus.api.DamageNexusApi;
import io.github.naimjeg.damagenexus.api.DamagePhaseProcessor;
import io.github.naimjeg.damagenexus.api.item.DamageNexusItemApi;
import io.github.naimjeg.damagenexus.api.item.DamageNexusItemEntries;
import io.github.naimjeg.damagenexus.api.item.template.DamageAffixTemplateReference;
import io.github.naimjeg.damagenexus.api.item.template.DamageEntryTemplateReference;
import io.github.naimjeg.damagenexus.api.item.template.DamageItemTemplateReferences;
import io.github.naimjeg.damagenexus.api.item.template.DamageNexusTemplates;
import io.github.naimjeg.damagenexus.api.context.DamageMutationResult;
import io.github.naimjeg.damagenexus.api.context.DamageRuleContext;
import io.github.naimjeg.damagenexus.api.critical.*;
import io.github.naimjeg.damagenexus.api.damage.DamageAttribution;
import io.github.naimjeg.damagenexus.api.damage.DamageAttributionResolution;
import io.github.naimjeg.damagenexus.api.damage.DamageAttributionSource;
import io.github.naimjeg.damagenexus.api.damage.DamageFailureReason;
import io.github.naimjeg.damagenexus.api.damage.DamageInheritancePolicy;
import io.github.naimjeg.damagenexus.api.damage.DamageMetadataKey;
import io.github.naimjeg.damagenexus.api.damage.DamageParentRef;
import io.github.naimjeg.damagenexus.api.damage.DamageRequest;
import io.github.naimjeg.damagenexus.api.damage.DamageRequestKind;
import io.github.naimjeg.damagenexus.api.damage.DamageResult;
import io.github.naimjeg.damagenexus.api.damage.DamageSettlementSnapshot;
import io.github.naimjeg.damagenexus.api.damage.DamageSettlementStatus;
import io.github.naimjeg.damagenexus.api.damage.DamageSourceDescriptor;
import io.github.naimjeg.damagenexus.api.damage.DamageSubmissionStatus;
import io.github.naimjeg.damagenexus.api.damage.DamageTriggerPolicy;
import io.github.naimjeg.damagenexus.api.event.DamageSettledEvent;
import io.github.naimjeg.damagenexus.api.event.DamageSettlementCallback;
import io.github.naimjeg.damagenexus.api.event.DamageNexusRegisterEvent;
import io.github.naimjeg.damagenexus.api.enums.DamageApplicationBucket;
import io.github.naimjeg.damagenexus.api.enums.DamageChannel;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import io.github.naimjeg.damagenexus.api.rule.DamageNexusOperations;
import io.github.naimjeg.damagenexus.api.rule.DamageNexusConditions;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleCondition;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleDefinition;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleOperation;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleProvider;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleRole;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleStacking;
import io.github.naimjeg.damagenexus.api.rule.RuleExecutionContext;
import io.github.naimjeg.damagenexus.api.rule.RuntimeDamageRule;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixDefinition;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixDisplay;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixRarity;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixSlot;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixStacking;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryDefinition;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryDisplay;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntrySlot;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryStacking;
import io.github.naimjeg.damagenexus.api.rule.source.EquippedItemRuleContribution;
import io.github.naimjeg.damagenexus.api.rule.source.EquippedItemRuleSourceCategory;
import io.github.naimjeg.damagenexus.api.rule.source.EquippedItemRuleSourceDirection;
import io.github.naimjeg.damagenexus.api.rule.source.EquippedItemRuleSourceQuery;
import io.github.naimjeg.damagenexus.command.test.TestItemFactory;
import io.github.naimjeg.damagenexus.core.config.DamageNexusSettings;
import io.github.naimjeg.damagenexus.core.gametest.GameTestCodecVerifier;
import io.github.naimjeg.damagenexus.core.gametest.GameTestServerPlayerFactory;
import io.github.naimjeg.damagenexus.core.pipeline.DamageSourcePolicy;
import io.github.naimjeg.damagenexus.bridge.vanilla.VanillaDamageCapture;
import io.github.naimjeg.damagenexus.bridge.vanilla.ProjectileDamageCapture;
import io.github.naimjeg.damagenexus.event.neoforge.VanillaCritHandler;
import io.github.naimjeg.damagenexus.config.DamageNexusConfig;
import io.github.naimjeg.damagenexus.config.DamageNexusConfigValues;
import io.github.naimjeg.damagenexus.config.DeveloperSettings;
import io.github.naimjeg.damagenexus.config.DamageSafetySettings;
import io.github.naimjeg.damagenexus.core.settlement.DamageSettlementCoordinator;
import io.github.naimjeg.damagenexus.core.settlement.DamageSettlementDispatchScope;
import io.github.naimjeg.damagenexus.core.settlement.DamageSettlementMixinStatus;
import io.github.naimjeg.damagenexus.core.settlement.DamageSettlementTracker;
import io.github.naimjeg.damagenexus.core.template.DatapackDamageTemplateReloadListener;
import io.github.naimjeg.damagenexus.core.registry.DamageChannelRegistry;
import io.github.naimjeg.damagenexus.core.rule.DatapackDamageRuleStore;
import io.github.naimjeg.damagenexus.registry.ModDataComponents;
import io.github.naimjeg.damagenexus.registry.PreMultiplierBuckets;
import io.github.naimjeg.damagenexus.registry.ModAttributes;
import io.github.naimjeg.damagenexus.registry.ModDamageProcessors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagLoader;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.neoforge.gametest.GameTestHooks;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/** Launch-only integration coverage for public requests and settlements. */
@EventBusSubscriber(modid = DamageNexus.MODID)
final class DamageRequestGameTests {

    private static final float EPSILON = 0.001f;
    private static final String GAMETEST_RUNTIME_PROPERTY =
            "damagenexus.gametest.runtime";
    private static final ResourceKey<Consumer<GameTestHelper>>
            PUBLIC_DAMAGE_SETTLEMENT_FUNCTION = ResourceKey.create(
            Registries.TEST_FUNCTION,
            id("public_damage_settlement")
    );
    private static final ResourceKey<Consumer<GameTestHelper>>
            REGISTRY_READINESS_FUNCTION = ResourceKey.create(
            Registries.TEST_FUNCTION,
            id("registry_dependency_readiness")
    );
    private static final ResourceKey<Consumer<GameTestHelper>>
            SETTLEMENT_REPOST_SAFETY_FUNCTION = ResourceKey.create(
            Registries.TEST_FUNCTION,
            id("settlement_event_repost_safety")
    );
    private static final DamageMetadataKey<Boolean> TEST_ORIGIN_FLAG =
            DamageMetadataKey.booleanKey(id("gametest_origin_flag"));
    private static final Identifier GAMETEST_ENTRY_TEMPLATE_ID =
            id("gametest_static_entry_template");
    private static final Identifier GAMETEST_AFFIX_TEMPLATE_ID =
            id("gametest_static_affix_template");
    private static final Identifier GAMETEST_DATAPACK_TEMPLATE_ID =
            id("gametest_datapack_entry_template");
    private static final ThreadLocal<SettlementProbe> ACTIVE_PROBE =
            new ThreadLocal<>();

    private DamageRequestGameTests() {
    }

    @SubscribeEvent
    public static void registerTestExtensions(DamageNexusRegisterEvent event) {
        if (!Boolean.getBoolean(GAMETEST_RUNTIME_PROPERTY)) {
            return;
        }
        event.registerPhaseProcessor(new ThrowingMutationProcessor());
        event.registerRuleProvider(new ThrowingMutationProvider());
        event.registerAttributionResolver(
                id("gametest_invalid_candidate_observer"),
                110,
                query -> {
                    SettlementProbe probe = ACTIVE_PROBE.get();
                    if (probe != null
                            && containsEntity(
                            query.candidate(),
                            probe.structurallyInvalidCandidate
                    )) {
                        probe.structurallyInvalidResolverInvocations++;
                    }
                    return Optional.empty();
                }
        );
        event.registerAttributionResolver(
                id("gametest_throwing_proxy_resolver"),
                100,
                query -> {
                    SettlementProbe probe = ACTIVE_PROBE.get();
                    if (probe != null
                            && query.candidate().directEntity()
                            == probe.proxyDirectEntity) {
                        probe.throwingResolverInvocations++;
                        throw new IllegalStateException(
                                "intentional Phase 5 resolver isolation failure"
                        );
                    }
                    return Optional.empty();
                }
        );
        event.registerAttributionResolver(
                id("gametest_invalid_proxy_resolver"),
                90,
                query -> invalidProxyResolution(query, ACTIVE_PROBE.get())
        );
        event.registerAttributionResolver(
                id("gametest_proxy_owner_resolver"),
                50,
                query -> proxyResolution(query, ACTIVE_PROBE.get(), true)
        );
        event.registerAttributionResolver(
                id("gametest_proxy_conflict_resolver"),
                40,
                query -> proxyResolution(query, ACTIVE_PROBE.get(), false)
        );
        event.registerEquippedItemRuleSource(
                id("gametest_external_items_high"),
                50,
                query -> externalItems(query, ACTIVE_PROBE.get(), true)
        );
        event.registerEquippedItemRuleSource(
                id("gametest_external_items_low"),
                10,
                query -> externalItems(query, ACTIVE_PROBE.get(), false)
        );
        event.registerEquippedItemRuleSource(
                id("gametest_external_items_throwing"),
                100,
                query -> {
                    SettlementProbe probe = ACTIVE_PROBE.get();
                    if (probe != null
                            && query.target() == probe.externalThrowingTarget) {
                        probe.externalThrowingInvocations++;
                        throw new IllegalStateException(
                                "intentional Phase 5 external-source isolation failure"
                        );
                    }
                    return List.of();
                }
        );
        event.registerCriticalDecisionProvider(
                id("gametest_critical_throwing"),
                100,
                (context, collector) -> {
                    SettlementProbe probe = ACTIVE_PROBE.get();
                    if (probe != null && context.victim()
                            == probe.criticalThrowingTarget) {
                        probe.criticalThrowingInvocations++;
                        collector.contribute(CriticalDecision.FORCE_CRITICAL);
                        throw new IllegalStateException(
                                "intentional Phase 7 decision-provider isolation failure");
                    }
                }
        );
        event.registerSettlementListener(
                id("gametest_settlement_first_shared_root"),
                200,
                DamageRequestGameTests::firstSharedRootListener
        );
        event.registerSettlementListener(
                id("gametest_settlement_throwing"),
                150,
                callback -> {
                    SettlementProbe probe = ACTIVE_PROBE.get();
                    if (probe != null && callback.snapshot().target()
                            == probe.throwingCallbackTarget) {
                        probe.throwingCallbackInvocations++;
                        throw new IllegalStateException(
                                "intentional registered settlement callback failure"
                        );
                    }
                }
        );
        event.registerSettlementListener(
                id("gametest_settlement_callback"),
                100,
                DamageRequestGameTests::onSettlementCallback
        );
        event.registerSettlementListener(
                id("gametest_settlement_second_shared_root"),
                0,
                DamageRequestGameTests::secondSharedRootListener
        );
        event.registerSettlementListener(
                id("gametest_settlement_cross_kind_proc"),
                -100,
                DamageRequestGameTests::crossKindProcListener
        );
        event.registerSettlementListener(
                id("gametest_settlement_cross_kind_reflection"),
                -200,
                DamageRequestGameTests::crossKindReflectionListener
        );
        event.registerCriticalDecisionProvider(
                id("gametest_critical_force"),
                50,
                (context, collector) -> {
                    SettlementProbe probe = ACTIVE_PROBE.get();
                    if (probe != null && context.victim()
                            == probe.criticalForceTarget) {
                        probe.criticalForceInvocations++;
                        probe.retainedCriticalCollector = collector;
                        collector.contribute(CriticalDecision.FORCE_CRITICAL);
                        collector.contribute(CriticalDecision.FORCE_CRITICAL);
                    }
                }
        );
        event.registerCriticalDecisionProvider(
                id("gametest_critical_suppress"),
                50,
                (context, collector) -> {
                    SettlementProbe probe = ACTIVE_PROBE.get();
                    if (probe != null && context.victim()
                            == probe.criticalSuppressTarget) {
                        probe.criticalSuppressInvocations++;
                        collector.contribute(CriticalDecision.SUPPRESS_CRITICAL);
                    }
                }
        );
        event.registerEntryTemplate(
                GAMETEST_ENTRY_TEMPLATE_ID,
                staticEntryTemplate(GAMETEST_ENTRY_TEMPLATE_ID, 2.0f)
        );
        event.registerAffixTemplate(
                GAMETEST_AFFIX_TEMPLATE_ID,
                staticAffixTemplate(GAMETEST_AFFIX_TEMPLATE_ID, 3.0f)
        );
    }

    private static boolean containsEntity(
            DamageAttribution attribution,
            Entity entity
    ) {
        return entity != null
                && (attribution.directEntity() == entity
                || attribution.logicalAttacker() == entity
                || attribution.effectOwner() == entity
                || attribution.equipmentOwner() == entity);
    }

    private static Optional<DamageAttributionResolution> proxyResolution(
            io.github.naimjeg.damagenexus.api.damage.DamageAttributionQuery query,
            SettlementProbe probe,
            boolean preferred
    ) {
        if (probe == null || probe.proxyDirectEntity == null
                || query.candidate().directEntity()
                != probe.proxyDirectEntity) {
            return Optional.empty();
        }
        if (preferred) {
            probe.preferredResolverInvocations++;
        } else {
            probe.conflictingResolverInvocations++;
        }
        return Optional.of(new DamageAttributionResolution(
                new DamageAttribution(
                        probe.proxyResolvedDirectEntity != null
                                ? probe.proxyResolvedDirectEntity
                                : probe.proxyDirectEntity,
                        probe.attacker,
                        preferred ? probe.attacker : probe.proxyDirectEntity,
                        probe.attacker
                )
        ));
    }

    private static Optional<DamageAttributionResolution> invalidProxyResolution(
            io.github.naimjeg.damagenexus.api.damage.DamageAttributionQuery query,
            SettlementProbe probe
    ) {
        if (probe == null || probe.invalidProxyDirectEntity == null
                || query.candidate().directEntity()
                != probe.invalidProxyDirectEntity) {
            return Optional.empty();
        }
        probe.invalidResolverInvocations++;
        return Optional.of(new DamageAttributionResolution(
                new DamageAttribution(
                        probe.invalidProxyDirectEntity,
                        probe.removedResolverOwner,
                        probe.removedResolverOwner,
                        probe.removedResolverOwner
                )
        ));
    }

    private static List<EquippedItemRuleContribution> externalItems(
            EquippedItemRuleSourceQuery query,
            SettlementProbe probe,
            boolean preferred
    ) {
        if (probe == null || !probe.externalSourcesActive) {
            return List.of();
        }
        if (probe.externalDistinctPhysical && !preferred) {
            return List.of();
        }
        if (preferred) {
            if (query.direction()
                    == EquippedItemRuleSourceDirection.OFFENSIVE) {
                probe.externalOffensiveQueries++;
            } else {
                probe.externalDefensiveQueries++;
            }
        } else if (probe.mutateExternalAfterPreferred
                && query.direction()
                == EquippedItemRuleSourceDirection.OFFENSIVE
                && probe.externalOffensiveStack != null) {
            probe.externalOffensiveStack.remove(
                    ModDataComponents.DAMAGE_ENTRIES.get()
            );
        }

        ItemStack stack = query.direction()
                == EquippedItemRuleSourceDirection.OFFENSIVE
                ? probe.externalOffensiveStack
                : probe.externalDefensiveStack;
        if (stack == null || stack.isEmpty()) {
            return List.of();
        }
        if (probe.externalDistinctPhysical
                && query.direction()
                == EquippedItemRuleSourceDirection.OFFENSIVE) {
            return List.of(
                    EquippedItemRuleContribution.both(
                            stack,
                            id("gametest_external_physical_a"),
                            Identifier.fromNamespaceAndPath("contentmod", "ring_a"),
                            probe.externalCategory,
                            20
                    ),
                    EquippedItemRuleContribution.both(
                            probe.externalSecondStack,
                            id("gametest_external_physical_b"),
                            Identifier.fromNamespaceAndPath("contentmod", "ring_b"),
                            probe.externalCategory,
                            20
                    )
            );
        }
        EquippedItemRuleContribution contribution =
                new EquippedItemRuleContribution(
                        stack,
                        id("gametest_external_physical_source"),
                        Identifier.fromNamespaceAndPath("contentmod", "ring"),
                        probe.externalCategory,
                        preferred ? 20 : 5,
                        probe.externalReadEntries,
                        probe.externalReadAffixes
                );
        if (!preferred) {
            return List.of(contribution);
        }
        return List.of(
                contribution,
                new EquippedItemRuleContribution(
                        stack.copy(),
                        contribution.sourceKey(),
                        Identifier.fromNamespaceAndPath(
                                "contentmod", "duplicate_semantic"
                        ),
                        contribution.category(),
                        contribution.sourcePriority(),
                        contribution.readEntries(),
                        contribution.readAffixes()
                )
        );
    }

    @SubscribeEvent
    public static void registerTestFunction(RegisterEvent event) {
        if (!GameTestHooks.isGametestEnabled()) {
            return;
        }
        event.register(
                Registries.TEST_FUNCTION,
                PUBLIC_DAMAGE_SETTLEMENT_FUNCTION.identifier(),
                () -> DamageRequestGameTests::publicDamageSettlement
        );
        event.register(
                Registries.TEST_FUNCTION,
                REGISTRY_READINESS_FUNCTION.identifier(),
                () -> DamageRequestGameTests::registryDependencyReadiness
        );
        event.register(
                Registries.TEST_FUNCTION,
                SETTLEMENT_REPOST_SAFETY_FUNCTION.identifier(),
                () -> DamageRequestGameTests::settlementEventRepostSafety
        );
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment =
                event.registerEnvironment(
                        id("damage_request_environment"),
                        new TestEnvironmentDefinition.AllOf(List.of())
                );
        Holder<TestEnvironmentDefinition<?>> repostEnvironment =
                event.registerEnvironment(
                        id("settlement_repost_environment"),
                        new TestEnvironmentDefinition.AllOf(List.of())
                );
        TestData<Holder<TestEnvironmentDefinition<?>>> data =
                new TestData<>(
                        environment,
                        Identifier.withDefaultNamespace("empty"),
                        100,
                        0,
                        true,
                        Rotation.NONE
                );
        TestData<Holder<TestEnvironmentDefinition<?>>> repostData =
                new TestData<>(
                        repostEnvironment,
                        Identifier.withDefaultNamespace("empty"),
                        100,
                        0,
                        true,
                        Rotation.NONE
                );

        event.registerTest(
                id("public_damage_settlement"),
                new FunctionGameTestInstance(
                        PUBLIC_DAMAGE_SETTLEMENT_FUNCTION,
                        data
                )
        );
        event.registerTest(
                id("registry_dependency_readiness"),
                new FunctionGameTestInstance(
                        REGISTRY_READINESS_FUNCTION,
                        data
                )
        );
        event.registerTest(
                id("settlement_event_repost_safety"),
                new FunctionGameTestInstance(
                        SETTLEMENT_REPOST_SAFETY_FUNCTION,
                        repostData
                )
        );
    }

    private static void registryDependencyReadiness(GameTestHelper helper) {
        GameTestCodecVerifier.verifyFunctionInstance(helper);
        DamageNexus.LOGGER.info(
                "[DamageNexus] Executing GameTest {}",
                REGISTRY_READINESS_FUNCTION.identifier()
        );
        long channelRevision = DamageChannelRegistry.contentRevision();
        if (!DamageNexusTemplates.serverExecutionReady()
                || DamageNexusTemplates.validatedChannelRevision()
                != channelRevision) {
            throw new AssertionError(
                    "Template snapshot is not bound to the active channel revision");
        }
        DatapackDamageRuleStore.Snapshot rules =
                DatapackDamageRuleStore.executionSnapshot();
        if (!rules.serverAuthoritative()
                || rules.validatedChannelRevision() != channelRevision) {
            throw new AssertionError(
                    "Global-rule snapshot is not bound to the active channel revision");
        }
        if (DamageRequestSubmissionTracker.activeDepthForTests() != 0
                || DamageSettlementTracker.activeDepthForTests() != 0
                || DamageSettlementCoordinator.pendingCountForTests() != 0
                || DamageSettlementDispatchScope.depthForTests() != 0
                || DamageSettlementCoordinator.drainingForTests()
                || DamageTransactionActivity.isActive()) {
            throw new AssertionError(
                    "Registry readiness test observed leaked damage scopes");
        }
        helper.succeed();
    }

    @SubscribeEvent
    public static void onDamageSettled(DamageSettledEvent event) {
        SettlementProbe probe = ACTIVE_PROBE.get();
        if (probe == null) {
            return;
        }

        if (event == probe.manualRepostEvent) {
            if (DamageSettlementDispatchScope.depthForTests() != 0) {
                probe.fail(
                        "Re-posting an official event outside its dispatch scope opened framework delivery state"
                );
            }
            probe.manualRepostObserved = true;
            return;
        }

        DamageSettlementSnapshot snapshot = event.snapshot();
        if (snapshot.target() == probe.observationNativeParentTarget
                && !probe.observationNativeAttempted) {
            probe.observationNativeAttempted = true;
            probe.observationNativeAccepted =
                    probe.observationNativeTarget.hurtServer(
                            probe.levelHelper.getLevel(),
                            probe.attacker.damageSources().mobAttack(
                                    probe.attacker),
                            1.0f
                    );
        }
        if (probe.authorityContractActive
                && snapshot.target() == probe.primaryTarget) {
            probe.officialParentEvent = event;
        }
        if (snapshot.target() == probe.repostParentTarget
                && probe.repostContractActive) {
            probe.repostObserverInvocations++;
            if (!probe.repostInProgress) {
                probe.repostInProgress = true;
                try {
                    NeoForge.EVENT_BUS.post(event);
                } finally {
                    probe.repostInProgress = false;
                }
            } else {
                probe.nestedRepostObserved = true;
                probe.nestedRootResult = DamageNexusApi.submitDamage(request(
                        probe.levelHelper,
                        probe.repostNestedTarget,
                        probe.attacker,
                        DamageRequestKind.PRIMARY,
                        1.0f
                ).build());
                return;
            }
        }
        probe.snapshots.add(snapshot);

        if (snapshot.status() == DamageSettlementStatus.APPLIED) {
            assertSnapshotStateDeltas(snapshot);
        }

        if (DamageTransactionActivity.isActive()
                || DamageRequestSubmissionTracker.hasActiveSubmission()
                || DamageSettlementTracker.hasActiveHurt()) {
            probe.fail(
                    "Settlement listener observed active parent state"
            );
        }
        if (DamageSettlementDispatchScope.depthForTests() != 1) {
            probe.fail("Official settlement event was outside its dispatch scope");
        }

        if (snapshot.target() == probe.throwingListenerTarget) {
            throw new IllegalStateException(
                    "intentional settlement-listener test failure"
            );
        }

    }

    private static void onSettlementCallback(
            DamageSettlementCallback callback
    ) {
        SettlementProbe probe = ACTIVE_PROBE.get();
        if (probe == null) {
            return;
        }
        DamageSettlementSnapshot snapshot = callback.snapshot();
        if (snapshot.target() == probe.throwingCallbackTarget) {
            probe.callbackAfterFailureObserved = true;
        }
        Optional<DamageParentRef> childAuthority = callback.childAuthority();
        if ((snapshot.status() == DamageSettlementStatus.APPLIED)
                != childAuthority.isPresent()) {
            probe.fail("Settlement callback authority did not match APPLIED status");
        }
        if (DamageSettlementDispatchScope.depthForTests() != 1) {
            probe.fail("Settlement callback was outside its dynamic scope");
        }

        if (snapshot.target() == probe.crossCallbackParentTarget) {
            probe.crossCallbackB = callback;
            probe.crossCallbackRefB = childAuthority.orElseThrow();
            probe.crossCallbackStaleAFromB = DamageNexusApi.submitDamage(request(
                    probe.levelHelper,
                    probe.crossCallbackStaleTargetAFromB,
                    probe.attacker,
                    DamageRequestKind.PROC,
                    1.0f
            ).parent(probe.crossCallbackRefA).build());
            probe.crossCallbackLegalB = DamageNexusApi.submitDamage(request(
                    probe.levelHelper,
                    probe.crossCallbackLegalTargetB,
                    probe.attacker,
                    DamageRequestKind.PROC,
                    1.0f
            ).parent(probe.crossCallbackRefB).build());
        }
        if (snapshot.target() == probe.crossCallbackThrowingParentTarget) {
            probe.crossCallbackThrowingB = callback;
            probe.crossCallbackThrowingRefB = childAuthority.orElseThrow();
            probe.crossCallbackThrowingStaleA =
                    DamageNexusApi.submitDamage(request(
                            probe.levelHelper,
                            probe.crossCallbackThrowingStaleTarget,
                            probe.attacker,
                            DamageRequestKind.PROC,
                            1.0f
                    ).parent(probe.crossCallbackThrowingRefA).build());
            probe.crossCallbackThrowingLegalB =
                    DamageNexusApi.submitDamage(request(
                            probe.levelHelper,
                            probe.crossCallbackThrowingLegalTarget,
                            probe.attacker,
                            DamageRequestKind.PROC,
                            1.0f
                    ).parent(probe.crossCallbackThrowingRefB).build());
        }

        if (snapshot.target() == probe.repostParentTarget
                && probe.repostContractActive) {
            probe.repostCallbackInvocations++;
            probe.repostCallback = callback;
            probe.repostAuthority = childAuthority.orElseThrow();
            probe.repostChildResult = DamageNexusApi.submitDamage(request(
                    probe.levelHelper,
                    probe.repostChildTarget,
                    probe.attacker,
                    DamageRequestKind.PROC,
                    1.0f
            ).parent(probe.repostAuthority).build());
        }

        if (snapshot.target() == probe.callbackNativeParentTarget
                && !probe.callbackNativeAttempted) {
            probe.callbackNativeAttempted = true;
            probe.callbackNativeAccepted =
                    probe.callbackNativeTarget.hurtServer(
                            probe.levelHelper.getLevel(),
                            probe.attacker.damageSources().mobAttack(
                                    probe.attacker),
                            1.0f
                    );
            probe.callbackLegalChildResult = DamageNexusApi.submitDamage(
                    request(
                            probe.levelHelper,
                            probe.callbackLegalChildTarget,
                            probe.attacker,
                            DamageRequestKind.PROC,
                            1.0f
                    ).parent(childAuthority.orElseThrow()).build()
            );
        }

        if (probe.unconditionalNativeRootAttemptActive) {
            probe.unconditionalNativeRootAttempts++;
            if (probe.unconditionalNativeRootTarget.hurtServer(
                    probe.levelHelper.getLevel(),
                    probe.attacker.damageSources().mobAttack(probe.attacker),
                    1.0f
            )) {
                probe.unconditionalNativeRootAccepted = true;
            }
        }

        Consumer<DamageSettlementCallback> settlementAction =
                probe.settlementAction;
        if (settlementAction != null) {
            settlementAction.accept(callback);
        }

        if (probe.unfilteredProcLoopActive
                && (probe.unfilteredProcRootId == 0L
                || probe.unfilteredProcRootId
                == snapshot.lineage().rootDamageId())
                && !probe.unfilteredProcTargets.isEmpty()) {
            if (probe.unfilteredProcRootId == 0L) {
                probe.unfilteredProcRootId =
                        snapshot.lineage().rootDamageId();
            }
            LivingEntity target = probe.unfilteredProcTargets.removeFirst();
            DamageResult result = DamageNexusApi.submitDamage(request(
                    probe.levelHelper,
                    target,
                    probe.attacker,
                    DamageRequestKind.PROC,
                    1.0f
            ).parent(childAuthority.orElseThrow()).build());
            probe.unfilteredProcResults.add(result);
        }

        if (snapshot.target() != probe.primaryTarget
                || snapshot.requestKind() != DamageRequestKind.PRIMARY
                || probe.childSubmitted) {
            return;
        }

        if (probe.authorityContractActive) {
            prepareBuilderFixtures(
                    probe,
                    snapshot,
                    childAuthority.orElseThrow()
            );
            probe.officialParentCallback = callback;
            probe.expiredAuthority = childAuthority.orElseThrow();
            probe.rootDuringDispatchResult = DamageNexusApi.submitDamage(
                    request(
                            probe.levelHelper,
                            probe.rootDuringDispatchTarget,
                            probe.attacker,
                            DamageRequestKind.PRIMARY,
                            1.0f
                    ).build()
            );
        }
        probe.childSubmitted = true;
        float parentResolved = snapshot.resolvedDamage();
        float parentHealthAfter = snapshot.healthAfter();

        DamageRequest child = request(
                probe.levelHelper,
                probe.childTarget,
                probe.attacker,
                DamageRequestKind.PROC,
                1.0f
        ).parent(childAuthority.orElseThrow())
                .triggerPolicy(DamageTriggerPolicy.ALL_ALLOWED)
                .build();
        probe.childResult = DamageNexusApi.submitDamage(child);

        if (probe.snapshots.size() != 1) {
            probe.fail(
                    "Child settlement event recursively entered the parent event stack"
            );
        }

        if (probe.childResult.status() == DamageSubmissionStatus.REJECTED
                && probe.childResult.failure().orElseThrow().reason()
                == DamageFailureReason.ACTIVE_TRANSACTION) {
            probe.fail("Child request was rejected as an active transaction");
        }
        if (snapshot.resolvedDamage() != parentResolved
                || snapshot.healthAfter() != parentHealthAfter) {
            probe.fail("Child request mutated the parent snapshot");
        }
    }

    private static void firstSharedRootListener(
            DamageSettlementCallback callback
    ) {
        SettlementProbe probe = ACTIVE_PROBE.get();
        if (probe == null) {
            return;
        }
        if (callback.snapshot().target()
                == probe.crossCallbackThrowingParentTarget) {
            probe.crossCallbackThrowingA = callback;
            probe.crossCallbackThrowingRefA =
                    callback.childAuthority().orElseThrow();
            throw new IllegalStateException(
                    "intentional cross-callback authority cleanup failure"
            );
        }
        if (callback.snapshot().target()
                == probe.crossCallbackParentTarget) {
            probe.crossCallbackA = callback;
            probe.crossCallbackRefA = callback.childAuthority().orElseThrow();
            probe.crossCallbackLegalA = DamageNexusApi.submitDamage(request(
                    probe.levelHelper,
                    probe.crossCallbackLegalTargetA,
                    probe.attacker,
                    DamageRequestKind.PROC,
                    1.0f
            ).parent(probe.crossCallbackRefA).build());
            return;
        }
        if (callback.snapshot().target()
                != probe.sharedBudgetParentTarget) {
            return;
        }
        probe.sharedBudgetResults.add(DamageNexusApi.submitDamage(request(
                probe.levelHelper,
                probe.sharedBudgetFirstTarget,
                probe.attacker,
                DamageRequestKind.PROC,
                1.0f
        ).parent(callback.childAuthority().orElseThrow()).build()));
    }

    private static void secondSharedRootListener(
            DamageSettlementCallback callback
    ) {
        SettlementProbe probe = ACTIVE_PROBE.get();
        if (probe == null) {
            return;
        }
        if (callback.snapshot().target()
                == probe.crossCallbackParentTarget) {
            probe.crossCallbackC = callback;
            probe.crossCallbackRefC = callback.childAuthority().orElseThrow();
            probe.crossCallbackStaleAFromC = DamageNexusApi.submitDamage(request(
                    probe.levelHelper,
                    probe.crossCallbackStaleTargetAFromC,
                    probe.attacker,
                    DamageRequestKind.PROC,
                    1.0f
            ).parent(probe.crossCallbackRefA).build());
            probe.crossCallbackStaleBFromC = DamageNexusApi.submitDamage(request(
                    probe.levelHelper,
                    probe.crossCallbackStaleTargetBFromC,
                    probe.attacker,
                    DamageRequestKind.PROC,
                    1.0f
            ).parent(probe.crossCallbackRefB).build());
            probe.crossCallbackLegalC = DamageNexusApi.submitDamage(request(
                    probe.levelHelper,
                    probe.crossCallbackLegalTargetC,
                    probe.attacker,
                    DamageRequestKind.PROC,
                    1.0f
            ).parent(probe.crossCallbackRefC).build());
            return;
        }
        if (callback.snapshot().target()
                != probe.sharedBudgetParentTarget) {
            return;
        }
        probe.sharedBudgetResults.add(DamageNexusApi.submitDamage(request(
                probe.levelHelper,
                probe.sharedBudgetSecondTarget,
                probe.attacker,
                DamageRequestKind.REFLECTED,
                1.0f
        ).parent(callback.childAuthority().orElseThrow()).build()));
    }

    private static void crossKindProcListener(
            DamageSettlementCallback callback
    ) {
        SettlementProbe probe = ACTIVE_PROBE.get();
        if (probe == null || !probe.crossKindLoopActive) {
            return;
        }

        LivingEntity target;
        if (callback.snapshot().target() == probe.crossKindRootTarget) {
            target = probe.crossKindProcTarget;
        } else if (callback.snapshot().target()
                == probe.crossKindReflectedTarget) {
            target = probe.crossKindRejectedTarget;
        } else {
            return;
        }

        probe.crossKindResults.add(DamageNexusApi.submitDamage(request(
                probe.levelHelper,
                target,
                probe.attacker,
                DamageRequestKind.PROC,
                1.0f
        ).parent(callback.childAuthority().orElseThrow()).build()));
    }

    private static void crossKindReflectionListener(
            DamageSettlementCallback callback
    ) {
        SettlementProbe probe = ACTIVE_PROBE.get();
        if (probe == null
                || !probe.crossKindLoopActive
                || callback.snapshot().target() != probe.crossKindProcTarget) {
            return;
        }

        probe.crossKindResults.add(DamageNexusApi.submitDamage(request(
                probe.levelHelper,
                probe.crossKindReflectedTarget,
                probe.attacker,
                DamageRequestKind.REFLECTED,
                1.0f
        ).parent(callback.childAuthority().orElseThrow()).build()));
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void cancelLateTestDamage(LivingIncomingDamageEvent event) {
        SettlementProbe probe = ACTIVE_PROBE.get();
        if (probe != null && event.getEntity() == probe.lateCancelTarget) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void zeroTestDamageInPre(LivingDamageEvent.Pre event) {
        SettlementProbe probe = ACTIVE_PROBE.get();
        if (probe != null && event.getEntity() == probe.preZeroTarget) {
            event.setNewDamage(0.0f);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void rejectNestedSubmitInLateIncoming(
            LivingIncomingDamageEvent event
    ) {
        SettlementProbe probe = ACTIVE_PROBE.get();
        if (probe != null
                && event.getEntity() == probe.activeIncomingTarget
                && probe.activeIncomingResult == null) {
            assertOnlyHurtScopeActive(probe, "late Incoming");
            probe.activeIncomingResult = DamageNexusApi.submitDamage(request(
                    probe.levelHelper,
                    probe.activeScopeNestedTarget,
                    probe.attacker,
                    DamageRequestKind.PRIMARY,
                    1.0f
            ).build());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void rejectNestedSubmitInPre(LivingDamageEvent.Pre event) {
        SettlementProbe probe = ACTIVE_PROBE.get();
        if (probe != null
                && event.getEntity() == probe.activePreTarget
                && probe.activePreResult == null) {
            assertOnlyHurtScopeActive(probe, "LivingDamageEvent.Pre");
            probe.activePreResult = DamageNexusApi.submitDamage(request(
                    probe.levelHelper,
                    probe.activeScopeNestedTarget,
                    probe.attacker,
                    DamageRequestKind.PRIMARY,
                    1.0f
            ).build());
        }
        if (probe != null
                && event.getEntity() == probe.fifoParentTarget
                && !probe.fifoNestedNativeAttempted) {
            if (DamageSettlementDispatchScope.isActive()) {
                probe.fail("Mutable FIFO fixture unexpectedly ran in settlement dispatch");
            }
            probe.fifoNestedNativeAttempted = true;
            probe.fifoNativeAccepted = probe.fifoNativeTarget.hurtServer(
                    probe.levelHelper.getLevel(),
                    probe.attacker.damageSources().mobAttack(probe.attacker),
                    1.0f
            );
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void rejectNestedSubmitInPost(LivingDamageEvent.Post event) {
        SettlementProbe probe = ACTIVE_PROBE.get();
        if (probe != null
                && event.getEntity() == probe.activePostTarget
                && probe.activePostResult == null) {
            assertOnlyHurtScopeActive(probe, "LivingDamageEvent.Post");
            probe.activePostResult = DamageNexusApi.submitDamage(request(
                    probe.levelHelper,
                    probe.activeScopeNestedTarget,
                    probe.attacker,
                    DamageRequestKind.PRIMARY,
                    1.0f
            ).build());
        }
    }

    private static void assertOnlyHurtScopeActive(
            SettlementProbe probe,
            String phase
    ) {
        if (DamageRequestSubmissionTracker.hasActiveSubmission()
                || DamageTransactionActivity.isActive()
                || !DamageSettlementTracker.hasActiveHurt()) {
            probe.fail(
                    phase + " fixture did not isolate the active-hurt guard"
            );
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void observeResolvedPublicSource(
            LivingIncomingDamageEvent event
    ) {
        SettlementProbe probe = ACTIVE_PROBE.get();
        if (probe != null && event.getEntity() == probe.proxyPublicTarget) {
            probe.observedPublicDirect = event.getSource().getDirectEntity();
            probe.observedPublicLogical = event.getSource().getEntity();
        }
    }

    @SuppressWarnings("removal")
    private static void publicDamageSettlement(GameTestHelper helper) {
        GameTestCodecVerifier.verifyFunctionInstance(helper);
        DamageNexus.LOGGER.info(
                "[DamageNexus] Executing GameTest {}",
                PUBLIC_DAMAGE_SETTLEMENT_FUNCTION.identifier()
        );
        if (!Boolean.getBoolean(GAMETEST_RUNTIME_PROPERTY)) {
            throw new AssertionError(
                    "Damage settlement GameTest requires the test runtime"
            );
        }
        if (!helper.getLevel().getServer().isSameThread()) {
            throw new AssertionError(
                    "Damage settlement GameTest is not on the server thread"
            );
        }
        if (DamageNexusSettings.transactionTrackingEnabled()
                || DamageNexusSettings.compatibilityDiagnosticsEnabled()
                || DamageNexusSettings.fullTraceEnabled()
                || DamageNexusSettings.summaryTraceEnabled()) {
            throw new AssertionError(
                    "Settlement GameTest requires diagnostics to be off"
            );
        }
        if (!DamageSettlementMixinStatus.isApplied()) {
            throw new AssertionError(
                    "LivingEntity settlement mixin marker is absent"
            );
        }

        SettlementProbe probe = new SettlementProbe(helper);
        DamageNexusConfigValues originalConfig = DamageNexusConfig.current();
        boolean asynchronousCompletionScheduled = false;
        ACTIVE_PROBE.set(probe);
        try {
            verifyEventChildRequest(probe);
            verifySuppressedChildRequests(probe);
            verifySameKindLoopSuppression(probe);
            verifyActiveHurtScopeRejection(probe);
            verifySettlementDispatchNativeBoundary(probe);
            verifyUnifiedCompletionFifo(probe);
            verifyUnfilteredProcListenerStops(probe);
            verifyCrossKindListenersTerminate(probe);
            verifyLineageDepthLimit(probe);
            verifyRootDerivationBudget(probe);
            verifyMultipleListenersShareRootBudget(probe);
            verifyNativeDamage(probe);
            verifyNativeProjectileAttribution(probe);
            verifyTrustedProxyAttribution(probe);
            verifyInvalidProxyResolution(probe);
            verifyNotAddedAttributionValidation(probe);
            verifyPhase6OriginConditions(probe);
            verifyPhase7CriticalDecisions(probe);
            verifyPhase7Entrypoints(probe);
            verifyPhase8Attributes(probe);
            verifyResolverAuthoritativeProjectileCategory(probe);
            verifyPhase10StaticTemplates(probe);
            verifyUnregisteredNativeDamage(probe);
            verifyEquipmentOwnerAuthorization(probe);
            verifyExternalItemSources(probe);
            verifyProjectileExternalSourceDeduplication(probe);
            verifyExternalSourceFailureIsolation(probe);
            verifyAbsorptionAndOverkill(probe);
            verifyFrameworkCancellation(probe);
            verifyZeroAndLateCancellation(probe);
            verifyZeroAfterPre(probe);
            verifyTolerantCallbackRollback(probe);
            verifyStrictCallbackCleanup(probe);
            verifyVanillaCooldownRejection(probe);
            verifyRejectedRequestAndListenerFailure(probe);
            verifyCleanup(probe);
            probe.assertNoFailure();
            verifyTickBudgetSameTick(probe, originalConfig);

            helper.runAfterDelay(1, () -> {
                try {
                    verifyTickBudgetReset(probe);
                    verifyCleanup(probe);
                    probe.assertNoFailure();
                    helper.succeed();
                } finally {
                    setConfig(originalConfig);
                    ACTIVE_PROBE.remove();
                }
            });
            asynchronousCompletionScheduled = true;
        } finally {
            if (!asynchronousCompletionScheduled) {
                setConfig(originalConfig);
                ACTIVE_PROBE.remove();
            }
        }
    }

    private static void settlementEventRepostSafety(GameTestHelper helper) {
        GameTestCodecVerifier.verifyFunctionInstance(helper);
        DamageNexus.LOGGER.info(
                "[DamageNexus] Executing GameTest {}",
                SETTLEMENT_REPOST_SAFETY_FUNCTION.identifier()
        );
        if (!Boolean.getBoolean(GAMETEST_RUNTIME_PROPERTY)
                || !helper.getLevel().getServer().isSameThread()) {
            throw new AssertionError(
                    "Settlement re-post safety requires the GameTest server thread"
            );
        }

        SettlementProbe probe = new SettlementProbe(helper);
        ACTIVE_PROBE.set(probe);
        try {
            probe.repostContractActive = true;
            probe.repostParentTarget = probe.spawn(1);
            probe.repostNestedTarget = probe.spawn(3);
            probe.repostChildTarget = probe.spawn(5);
            float nestedHealth = probe.repostNestedTarget.getHealth();
            float childHealth = probe.repostChildTarget.getHealth();
            int admissionsBefore = DamageAdmissionController.currentTickCount(
                    helper.getLevel().getServer());

            DamageResult parent = DamageNexusApi.submitDamage(request(
                    helper,
                    probe.repostParentTarget,
                    probe.attacker,
                    DamageRequestKind.PRIMARY,
                    1.0f
            ).build());
            assertStatus(parent, DamageSubmissionStatus.APPLIED, null);
            assertStatus(
                    Objects.requireNonNull(probe.nestedRootResult),
                    DamageSubmissionStatus.REJECTED,
                    DamageFailureReason.ROOT_REQUEST_DURING_SETTLEMENT
            );
            assertStatus(
                    Objects.requireNonNull(probe.repostChildResult),
                    DamageSubmissionStatus.APPLIED,
                    null
            );

            if (!probe.nestedRepostObserved
                    || probe.repostObserverInvocations != 2
                    || probe.repostCallbackInvocations != 1
                    || probe.snapshots.size() != 2
                    || probe.repostNestedTarget.getHealth() != nestedHealth
                    || probe.repostChildTarget.getHealth() >= childHealth
                    || probe.repostCallback.childAuthority().isPresent()) {
                throw new AssertionError(
                        "Nested settlement event re-post violated the authority contract"
                );
            }

            LivingEntity expiredTarget = probe.spawn(7);
            float expiredHealth = expiredTarget.getHealth();
            DamageResult expired = DamageNexusApi.submitDamage(request(
                    helper,
                    expiredTarget,
                    probe.attacker,
                    DamageRequestKind.PROC,
                    1.0f
            ).parent(probe.repostAuthority).build());
            assertStatus(
                    expired,
                    DamageSubmissionStatus.REJECTED,
                    DamageFailureReason.PARENT_AUTHORITY_INACTIVE
            );
            if (expiredTarget.getHealth() != expiredHealth
                    || parent.lineage().derivedRequestCountInternal() != 1
                    || DamageAdmissionController.currentTickCount(
                    helper.getLevel().getServer()) != admissionsBefore + 2) {
                throw new AssertionError(
                        "Nested/expired authority rejection consumed state or budgets"
                );
            }
            probe.repostContractActive = false;
            verifySameSettlementCallbackAuthorityIsolation(probe);
            verifyCleanup(probe);
            probe.assertNoFailure();
            helper.succeed();
        } finally {
            probe.repostContractActive = false;
            ACTIVE_PROBE.remove();
        }
    }

    private static void verifySameSettlementCallbackAuthorityIsolation(
            SettlementProbe probe
    ) {
        probe.crossCallbackParentTarget = probe.spawn();
        probe.crossCallbackLegalTargetA = probe.spawn();
        probe.crossCallbackStaleTargetAFromB = probe.spawn();
        probe.crossCallbackLegalTargetB = probe.spawn();
        probe.crossCallbackStaleTargetAFromC = probe.spawn();
        probe.crossCallbackStaleTargetBFromC = probe.spawn();
        probe.crossCallbackLegalTargetC = probe.spawn();

        float staleAFromBHealth =
                probe.crossCallbackStaleTargetAFromB.getHealth();
        float staleAFromCHealth =
                probe.crossCallbackStaleTargetAFromC.getHealth();
        float staleBFromCHealth =
                probe.crossCallbackStaleTargetBFromC.getHealth();
        int admissionsBefore = DamageAdmissionController.currentTickCount(
                probe.levelHelper.getLevel().getServer());

        DamageResult parent = DamageNexusApi.submitDamage(request(
                probe.levelHelper,
                probe.crossCallbackParentTarget,
                probe.attacker,
                DamageRequestKind.PRIMARY,
                1.0f
        ).build());
        assertStatus(parent, DamageSubmissionStatus.APPLIED, null);
        assertStatus(
                Objects.requireNonNull(probe.crossCallbackLegalA),
                DamageSubmissionStatus.APPLIED,
                null
        );
        assertStatus(
                Objects.requireNonNull(probe.crossCallbackStaleAFromB),
                DamageSubmissionStatus.REJECTED,
                DamageFailureReason.PARENT_AUTHORITY_INACTIVE
        );
        assertStatus(
                Objects.requireNonNull(probe.crossCallbackLegalB),
                DamageSubmissionStatus.APPLIED,
                null
        );
        assertStatus(
                Objects.requireNonNull(probe.crossCallbackStaleAFromC),
                DamageSubmissionStatus.REJECTED,
                DamageFailureReason.PARENT_AUTHORITY_INACTIVE
        );
        assertStatus(
                Objects.requireNonNull(probe.crossCallbackStaleBFromC),
                DamageSubmissionStatus.REJECTED,
                DamageFailureReason.PARENT_AUTHORITY_INACTIVE
        );
        assertStatus(
                Objects.requireNonNull(probe.crossCallbackLegalC),
                DamageSubmissionStatus.APPLIED,
                null
        );

        if (probe.crossCallbackRefA == probe.crossCallbackRefB
                || probe.crossCallbackRefA == probe.crossCallbackRefC
                || probe.crossCallbackRefB == probe.crossCallbackRefC
                || probe.crossCallbackA.childAuthority().isPresent()
                || probe.crossCallbackB.childAuthority().isPresent()
                || probe.crossCallbackC.childAuthority().isPresent()) {
            throw new AssertionError(
                    "One settlement reused authority across callback invocations"
            );
        }
        if (probe.crossCallbackStaleAFromB.pipelineExecuted()
                || probe.crossCallbackStaleAFromB.settlement().isPresent()
                || probe.crossCallbackStaleAFromC.pipelineExecuted()
                || probe.crossCallbackStaleAFromC.settlement().isPresent()
                || probe.crossCallbackStaleBFromC.pipelineExecuted()
                || probe.crossCallbackStaleBFromC.settlement().isPresent()
                || probe.crossCallbackStaleTargetAFromB.getHealth()
                != staleAFromBHealth
                || probe.crossCallbackStaleTargetAFromC.getHealth()
                != staleAFromCHealth
                || probe.crossCallbackStaleTargetBFromC.getHealth()
                != staleBFromCHealth) {
            throw new AssertionError(
                    "Cross-callback authority rejection entered the pipeline"
            );
        }
        if (parent.lineage().derivedRequestCountInternal() != 3
                || DamageAdmissionController.currentTickCount(
                probe.levelHelper.getLevel().getServer())
                != admissionsBefore + 4) {
            throw new AssertionError(
                    "Cross-callback rejection consumed root or tick budget"
            );
        }
        for (DamageResult child : List.of(
                probe.crossCallbackLegalA,
                probe.crossCallbackLegalB,
                probe.crossCallbackLegalC)) {
            if (child.lineage().rootDamageId()
                    != parent.lineage().rootDamageId()
                    || child.lineage().parentDamageId().orElse(-1L)
                    != parent.lineage().damageId()
                    || child.lineage().recursionDepth() != 1) {
                throw new AssertionError(
                        "Per-callback authority did not share one root lineage"
                );
            }
        }

        probe.crossCallbackThrowingParentTarget = probe.spawn();
        probe.crossCallbackThrowingStaleTarget = probe.spawn();
        probe.crossCallbackThrowingLegalTarget = probe.spawn();
        float staleHealth = probe.crossCallbackThrowingStaleTarget.getHealth();
        admissionsBefore = DamageAdmissionController.currentTickCount(
                probe.levelHelper.getLevel().getServer());

        DamageResult throwingParent = DamageNexusApi.submitDamage(request(
                probe.levelHelper,
                probe.crossCallbackThrowingParentTarget,
                probe.attacker,
                DamageRequestKind.PRIMARY,
                1.0f
        ).build());
        assertStatus(throwingParent, DamageSubmissionStatus.APPLIED, null);
        assertStatus(
                Objects.requireNonNull(probe.crossCallbackThrowingStaleA),
                DamageSubmissionStatus.REJECTED,
                DamageFailureReason.PARENT_AUTHORITY_INACTIVE
        );
        assertStatus(
                Objects.requireNonNull(probe.crossCallbackThrowingLegalB),
                DamageSubmissionStatus.APPLIED,
                null
        );
        if (probe.crossCallbackThrowingRefA
                == probe.crossCallbackThrowingRefB
                || probe.crossCallbackThrowingA.childAuthority().isPresent()
                || probe.crossCallbackThrowingB.childAuthority().isPresent()
                || probe.crossCallbackThrowingStaleA.pipelineExecuted()
                || probe.crossCallbackThrowingStaleA.settlement().isPresent()
                || probe.crossCallbackThrowingStaleTarget.getHealth()
                != staleHealth
                || throwingParent.lineage().derivedRequestCountInternal() != 1
                || DamageAdmissionController.currentTickCount(
                probe.levelHelper.getLevel().getServer())
                != admissionsBefore + 2) {
            throw new AssertionError(
                    "Throwing callback leaked authority into the next callback"
            );
        }
    }

    private static void verifyEventChildRequest(SettlementProbe probe) {
        probe.clear();
        int admissionsBefore = DamageAdmissionController.currentTickCount(
                probe.levelHelper.getLevel().getServer()
        );
        probe.primaryTarget = probe.spawn(1);
        probe.childTarget = probe.spawn(3);
        probe.rootDuringDispatchTarget = probe.spawn();
        probe.authorityContractActive = true;

        boolean unparentedRejected = false;
        try {
            request(
                    probe.levelHelper,
                    probe.childTarget,
                    probe.attacker,
                    DamageRequestKind.PROC,
                    1.0f
            ).build();
        } catch (IllegalArgumentException expected) {
            unparentedRejected = true;
        }
        if (!unparentedRejected) {
            throw new AssertionError(
                    "PROC request was built without a completed parent ref"
            );
        }

        DamageRequest primary = request(
                probe.levelHelper,
                probe.primaryTarget,
                probe.attacker,
                DamageRequestKind.PRIMARY,
                2.0f
        ).actionId(id("gametest_primary_action"))
                .sourceTag(id("gametest_primary_tag"))
                .metadata(TEST_ORIGIN_FLAG, true)
                .build();
        DamageResult primaryResult = DamageNexusApi.submitDamage(primary);

        assertStatus(primaryResult, DamageSubmissionStatus.APPLIED, null);
        assertStatus(
                Objects.requireNonNull(probe.rootDuringDispatchResult),
                DamageSubmissionStatus.REJECTED,
                DamageFailureReason.ROOT_REQUEST_DURING_SETTLEMENT
        );
        if (probe.snapshots.size() != 2 || probe.childResult == null) {
            throw new AssertionError(
                    "Parent and child settlements were not both published"
            );
        }

        DamageSettlementSnapshot parent = probe.snapshots.get(0);
        DamageSettlementSnapshot child = probe.snapshots.get(1);
        assertSame(parent, primaryResult.settlement().orElseThrow());
        assertStatus(
                probe.childResult,
                DamageSubmissionStatus.APPLIED,
                null
        );

        if (child.lineage().damageId() == parent.lineage().damageId()
                || child.lineage().rootDamageId()
                != parent.lineage().rootDamageId()
                || child.lineage().parentDamageId().orElse(-1L)
                != parent.lineage().damageId()
                || child.lineage().recursionDepth()
                != parent.lineage().recursionDepth() + 1) {
            throw new AssertionError("Child settlement lineage is invalid");
        }
        if (probe.officialParentCallback.childAuthority().isPresent()) {
            throw new AssertionError(
                    "Settlement callback authority remained visible after dispatch"
            );
        }
        probe.manualRepostEvent = probe.officialParentEvent;
        try {
            NeoForge.EVENT_BUS.post(probe.manualRepostEvent);
        } finally {
            probe.manualRepostEvent = null;
        }
        if (!probe.manualRepostObserved) {
            throw new AssertionError(
                    "The manual settlement-event replay fixture did not run"
            );
        }
        LivingEntity expiredTarget = probe.spawn();
        float expiredHealth = expiredTarget.getHealth();
        DamageResult expired = DamageNexusApi.submitDamage(request(
                probe.levelHelper,
                expiredTarget,
                probe.attacker,
                DamageRequestKind.PROC,
                1.0f
        ).parent(probe.expiredAuthority).build());
        assertStatus(
                expired,
                DamageSubmissionStatus.REJECTED,
                DamageFailureReason.PARENT_AUTHORITY_INACTIVE
        );
        if (expired.pipelineExecuted()
                || expired.settlement().isPresent()
                || expiredTarget.getHealth() != expiredHealth) {
            throw new AssertionError(
                    "Expired event authority entered the pipeline"
            );
        }

        LivingEntity secondEventTarget = probe.spawn();
        LivingEntity crossEventTarget = probe.spawn();
        float crossEventHealth = crossEventTarget.getHealth();
        DamageResult[] crossEvent = new DamageResult[1];
        DamageParentRef authorityFromFirstEvent = probe.expiredAuthority;
        probe.settlementAction = callback -> {
            if (callback.snapshot().target() != secondEventTarget) {
                return;
            }
            probe.settlementAction = null;
            crossEvent[0] = DamageNexusApi.submitDamage(request(
                    probe.levelHelper,
                    crossEventTarget,
                    probe.attacker,
                    DamageRequestKind.PROC,
                    1.0f
            ).parent(authorityFromFirstEvent).build());
        };
        DamageResult secondEvent = DamageNexusApi.submitDamage(request(
                probe.levelHelper,
                secondEventTarget,
                probe.attacker,
                DamageRequestKind.PRIMARY,
                1.0f
        ).build());
        assertStatus(secondEvent, DamageSubmissionStatus.APPLIED, null);
        assertStatus(
                Objects.requireNonNull(crossEvent[0]),
                DamageSubmissionStatus.REJECTED,
                DamageFailureReason.PARENT_AUTHORITY_INACTIVE
        );
        if (crossEventTarget.getHealth() != crossEventHealth) {
            throw new AssertionError(
                    "Authority from event A was accepted in event B"
            );
        }
        if (DamageAdmissionController.currentTickCount(
                probe.levelHelper.getLevel().getServer()
        ) != admissionsBefore + 3) {
            throw new AssertionError(
                    "Authority/root-dispatch rejection consumed tick budget"
            );
        }
        if (primaryResult.lineage().derivedRequestCountInternal() != 1) {
            throw new AssertionError(
                    "Inactive/cross-event authority consumed root derivation budget"
            );
        }
        probe.authorityContractActive = false;
        if (!child.triggerPolicy().equals(
                DamageTriggerPolicy.PROC_SUPPRESSED
        )) {
            throw new AssertionError(
                    "First PROC did not receive its own downstream suppression"
            );
        }
        if (!probe.triggerPolicyOrderVerified) {
            throw new AssertionError(
                    "Builder trigger policy fixture did not run in the event scope"
            );
        }
        if (!parent.actionId().equals(primary.actionId())
                || !parent.sourceTags().equals(primary.sourceTags())
                || !parent.metadata().get(TEST_ORIGIN_FLAG).orElse(false)
                || parent.logicalAttacker() != probe.attacker
                || parent.equipmentOwner() != probe.attacker) {
            throw new AssertionError(
                    "Request origin was not preserved in the settlement"
            );
        }

        DamageRequest inherited = probe.inheritedRequest;
        if (!inherited.actionId().equals(parent.actionId())
                || !inherited.sourceTags().equals(parent.sourceTags())
                || !inherited.metadata().get(TEST_ORIGIN_FLAG).orElse(false)
                || inherited.logicalAttacker() != null) {
            throw new AssertionError(
                    "Settlement metadata inheritance copied invalid fields"
            );
        }

        DamageRequest inheritedThenExplicit = probe.inheritedThenExplicit;
        DamageRequest explicitThenInherited = probe.explicitThenInherited;
        if (!inheritedThenExplicit.attribution().equals(
                explicitThenInherited.attribution()
        ) || !inheritedThenExplicit.actionId().equals(
                explicitThenInherited.actionId()
        ) || !inheritedThenExplicit.sourceTags().equals(
                explicitThenInherited.sourceTags()
        ) || !inheritedThenExplicit.metadata()
                .get(TEST_ORIGIN_FLAG).equals(Optional.of(false))
                || !explicitThenInherited.metadata()
                .get(TEST_ORIGIN_FLAG).equals(Optional.of(false))
                || inheritedThenExplicit.lineage().damageId()
                == explicitThenInherited.lineage().damageId()
                || inheritedThenExplicit.parentDamageId().orElse(-1L)
                != parent.damageId()
                || explicitThenInherited.parentDamageId().orElse(-1L)
                != parent.damageId()) {
            throw new AssertionError(
                    "Builder inheritance depends on method order"
            );
        }

        boolean immutable = false;
        try {
            parent.resolvedChannelDamage().clear();
        } catch (UnsupportedOperationException expected) {
            immutable = true;
        }
        if (!immutable) {
            throw new AssertionError("Settlement channel map is mutable");
        }
    }

    private static void verifyTriggerPolicyBuildOrder(
            SettlementProbe probe,
            DamageParentRef authority
    ) {
        DamageRequest suppressionFirst = DamageRequest.builder(
                probe.levelHelper.getLevel(),
                probe.spawn(),
                DamageSourceDescriptor.of(DamageTypes.PLAYER_ATTACK),
                1.0f
        ).suppressProcs()
                .kind(DamageRequestKind.PROC)
                .parent(authority)
                .build();
        DamageRequest kindFirst = DamageRequest.builder(
                probe.levelHelper.getLevel(),
                probe.spawn(),
                DamageSourceDescriptor.of(DamageTypes.PLAYER_ATTACK),
                1.0f
        ).kind(DamageRequestKind.PROC)
                .suppressProcs()
                .parent(authority)
                .build();

        if (!suppressionFirst.triggerPolicy().equals(
                kindFirst.triggerPolicy()
        ) || !suppressionFirst.triggerPolicy().equals(
                DamageTriggerPolicy.PROC_SUPPRESSED
        )) {
            throw new AssertionError(
                    "Builder trigger policy depends on setter order"
            );
        }
        probe.triggerPolicyOrderVerified = true;
    }

    private static void prepareBuilderFixtures(
            SettlementProbe probe,
            DamageSettlementSnapshot parent,
            DamageParentRef authority
    ) {
        probe.inheritedRequest = DamageRequest.builder(
                probe.levelHelper.getLevel(),
                probe.spawn(5),
                DamageSourceDescriptor.of(DamageTypes.PLAYER_ATTACK),
                1.0f
        ).kind(DamageRequestKind.CUSTOM)
                .inheritFrom(
                        authority,
                        DamageInheritancePolicy.SOURCE_METADATA
                )
                .build();

        LivingEntity explicitOwner = probe.spawn();
        DamageAttribution explicitAttribution = new DamageAttribution(
                null,
                explicitOwner,
                null,
                explicitOwner
        );
        probe.inheritedThenExplicit = DamageRequest.builder(
                probe.levelHelper.getLevel(),
                probe.spawn(),
                DamageSourceDescriptor.of(DamageTypes.PLAYER_ATTACK),
                1.0f
        ).kind(DamageRequestKind.CUSTOM)
                .inheritFrom(
                        authority,
                        DamageInheritancePolicy
                                .ATTRIBUTION_AND_SOURCE_METADATA
                )
                .attribution(explicitAttribution)
                .actionId(id("gametest_explicit_action"))
                .sourceTag(id("gametest_explicit_tag"))
                .metadata(TEST_ORIGIN_FLAG, false)
                .build();
        probe.explicitThenInherited = DamageRequest.builder(
                probe.levelHelper.getLevel(),
                probe.spawn(),
                DamageSourceDescriptor.of(DamageTypes.PLAYER_ATTACK),
                1.0f
        ).kind(DamageRequestKind.CUSTOM)
                .attribution(explicitAttribution)
                .actionId(id("gametest_explicit_action"))
                .sourceTag(id("gametest_explicit_tag"))
                .metadata(TEST_ORIGIN_FLAG, false)
                .inheritFrom(
                        authority,
                        DamageInheritancePolicy
                                .ATTRIBUTION_AND_SOURCE_METADATA
                )
                .build();
        verifyTriggerPolicyBuildOrder(probe, authority);
        if (probe.inheritedRequest.parentDamageId().orElse(-1L)
                != parent.damageId()) {
            probe.fail("Builder fixture used the wrong event authority");
        }
    }

    private static void verifySuppressedChildRequests(
            SettlementProbe probe
    ) {
        probe.clear();
        LivingEntity parentTarget = probe.spawn();
        LivingEntity target = probe.spawn();
        float healthBefore = target.getHealth();
        float absorptionBefore = target.getAbsorptionAmount();
        DamageResult[] rejectedHolder = new DamageResult[1];
        DamageRequest[] childHolder = new DamageRequest[1];
        probe.settlementAction = callback -> {
            if (callback.snapshot().target() != parentTarget) {
                return;
            }
            probe.settlementAction = null;
            DamageRequest child = request(
                    probe.levelHelper,
                    target,
                    probe.attacker,
                    DamageRequestKind.PROC,
                    1.0f
            ).parent(callback.childAuthority().orElseThrow()).build();
            childHolder[0] = child;
            rejectedHolder[0] = DamageNexusApi.submitDamage(child);
        };
        DamageResult parentResult = DamageNexusApi.submitDamage(request(
                probe.levelHelper,
                parentTarget,
                probe.attacker,
                DamageRequestKind.PRIMARY,
                1.0f
        ).triggerPolicy(DamageTriggerPolicy.NONE_ALLOWED).build());
        assertStatus(parentResult, DamageSubmissionStatus.APPLIED, null);
        DamageResult rejected = Objects.requireNonNull(rejectedHolder[0]);
        probe.snapshots.clear();
        assertPrePipelineRejected(
                probe,
                rejected,
                DamageFailureReason.PROC_SUPPRESSED,
                target,
                healthBefore,
                absorptionBefore
        );

        DamageResult duplicate = DamageNexusApi.submitDamage(
                Objects.requireNonNull(childHolder[0])
        );
        assertStatus(
                duplicate,
                DamageSubmissionStatus.REJECTED,
                DamageFailureReason.DUPLICATE_REQUEST
        );
    }

    private static void verifySameKindLoopSuppression(
            SettlementProbe probe
    ) {
        verifySameKindLoop(
                probe,
                DamageRequestKind.PROC,
                DamageFailureReason.PROC_SUPPRESSED
        );
        verifySameKindLoop(
                probe,
                DamageRequestKind.REFLECTED,
                DamageFailureReason.REFLECTION_SUPPRESSED
        );
        verifySameKindLoop(
                probe,
                DamageRequestKind.THORNS,
                DamageFailureReason.THORNS_SUPPRESSED
        );
    }

    private static void verifyActiveHurtScopeRejection(
            SettlementProbe probe
    ) {
        ItemStack original = probe.attacker.getMainHandItem().copy();
        probe.clear();
        int admissionsBefore = DamageAdmissionController.currentTickCount(
                probe.levelHelper.getLevel().getServer()
        );
        probe.activeScopeNestedTarget = probe.spawn();
        float nestedHealth = probe.activeScopeNestedTarget.getHealth();
        probe.activeIncomingTarget = probe.spawn();
        probe.activePreTarget = probe.spawn();
        probe.activePostTarget = probe.spawn();

        for (LivingEntity target : List.of(
                probe.activeIncomingTarget,
                probe.activePreTarget,
                probe.activePostTarget
        )) {
            boolean parentAccepted = target.hurtServer(
                    probe.levelHelper.getLevel(),
                    probe.attacker.damageSources().mobAttack(probe.attacker),
                    1.0f
            );
            if (!parentAccepted) {
                throw new AssertionError(
                        "Native active-hurt fixture parent was rejected"
                );
            }
        }

        for (DamageResult nested : List.of(
                Objects.requireNonNull(probe.activeIncomingResult),
                Objects.requireNonNull(probe.activePreResult),
                Objects.requireNonNull(probe.activePostResult)
        )) {
            assertStatus(
                    nested,
                    DamageSubmissionStatus.REJECTED,
                    DamageFailureReason.ACTIVE_TRANSACTION
            );
            if (nested.pipelineExecuted() || nested.settlement().isPresent()) {
                throw new AssertionError(
                        "Active hurt-scope rejection entered the pipeline"
                );
            }
        }
        if (probe.activeScopeNestedTarget.getHealth() != nestedHealth) {
            throw new AssertionError(
                    "Active hurt-scope rejection changed its target"
            );
        }
        if (DamageAdmissionController.currentTickCount(
                probe.levelHelper.getLevel().getServer()
        ) != admissionsBefore + 3) {
            throw new AssertionError(
                    "Active hurt-scope rejection consumed tick admission budget"
            );
        }

        try {
            probe.clear();
            probe.activeScopeNestedTarget = probe.spawn();
            nestedHealth = probe.activeScopeNestedTarget.getHealth();
            probe.attacker.setItemSlot(
                    EquipmentSlot.MAINHAND,
                    ruleStack(
                            "gametest_active_transaction_submit",
                            DamagePhase.BASE_MODIFICATION,
                            List.of(new Phase5CountingOperation(
                                    Phase5Counter.ACTIVE_TRANSACTION_SUBMIT
                            ))
                    )
            );
            DamageResult parent = DamageNexusApi.submitDamage(request(
                    probe.levelHelper,
                    probe.spawn(),
                    probe.attacker,
                    DamageRequestKind.PRIMARY,
                    1.0f
            ).build());
            assertStatus(parent, DamageSubmissionStatus.APPLIED, null);
            assertStatus(
                    Objects.requireNonNull(probe.activeTransactionResult),
                    DamageSubmissionStatus.REJECTED,
                    DamageFailureReason.ACTIVE_TRANSACTION
            );
            if (probe.activeScopeNestedTarget.getHealth() != nestedHealth) {
                throw new AssertionError(
                        "Active transaction callback changed nested target"
                );
            }
            if (DamageAdmissionController.currentTickCount(
                    probe.levelHelper.getLevel().getServer()
            ) != admissionsBefore + 4) {
                throw new AssertionError(
                        "Active transaction rejection consumed tick admission budget"
                );
            }
        } finally {
            probe.attacker.setItemSlot(EquipmentSlot.MAINHAND, original);
        }
    }

    private static void verifySettlementDispatchNativeBoundary(
            SettlementProbe probe
    ) {
        probe.clear();
        probe.observationNativeParentTarget = probe.spawn();
        probe.observationNativeTarget = probe.spawn();
        probe.observationNativeTarget.setAbsorptionAmount(2.0f);
        float observationHealth = probe.observationNativeTarget.getHealth();
        float observationAbsorption =
                probe.observationNativeTarget.getAbsorptionAmount();
        int admissionsBefore = DamageAdmissionController.currentTickCount(
                probe.levelHelper.getLevel().getServer());

        DamageResult observationParent = DamageNexusApi.submitDamage(request(
                probe.levelHelper,
                probe.observationNativeParentTarget,
                probe.attacker,
                DamageRequestKind.PRIMARY,
                1.0f
        ).build());
        assertStatus(observationParent, DamageSubmissionStatus.APPLIED, null);
        if (!probe.observationNativeAttempted
                || probe.observationNativeAccepted
                || probe.observationNativeTarget.getHealth()
                != observationHealth
                || probe.observationNativeTarget.getAbsorptionAmount()
                != observationAbsorption
                || probe.snapshots.size() != 1
                || DamageAdmissionController.currentTickCount(
                probe.levelHelper.getLevel().getServer())
                != admissionsBefore + 1
                || observationParent.lineage().derivedRequestCountInternal()
                != 0) {
            throw new AssertionError(
                    "Observation dispatch admitted a direct managed native root"
            );
        }
        verifyCleanup(probe);

        probe.clear();
        probe.callbackNativeParentTarget = probe.spawn();
        probe.callbackNativeTarget = probe.spawn();
        probe.callbackNativeTarget.setAbsorptionAmount(2.0f);
        probe.callbackLegalChildTarget = probe.spawn();
        float callbackHealth = probe.callbackNativeTarget.getHealth();
        float callbackAbsorption =
                probe.callbackNativeTarget.getAbsorptionAmount();
        admissionsBefore = DamageAdmissionController.currentTickCount(
                probe.levelHelper.getLevel().getServer());

        DamageResult callbackParent = DamageNexusApi.submitDamage(request(
                probe.levelHelper,
                probe.callbackNativeParentTarget,
                probe.attacker,
                DamageRequestKind.PRIMARY,
                1.0f
        ).build());
        assertStatus(callbackParent, DamageSubmissionStatus.APPLIED, null);
        assertStatus(
                Objects.requireNonNull(probe.callbackLegalChildResult),
                DamageSubmissionStatus.APPLIED,
                null
        );
        if (!probe.callbackNativeAttempted
                || probe.callbackNativeAccepted
                || probe.callbackNativeTarget.getHealth() != callbackHealth
                || probe.callbackNativeTarget.getAbsorptionAmount()
                != callbackAbsorption
                || probe.snapshots.size() != 2
                || probe.snapshots.get(0).target()
                != probe.callbackNativeParentTarget
                || probe.snapshots.get(1).target()
                != probe.callbackLegalChildTarget
                || callbackParent.lineage().derivedRequestCountInternal() != 1
                || DamageAdmissionController.currentTickCount(
                probe.levelHelper.getLevel().getServer())
                != admissionsBefore + 2) {
            throw new AssertionError(
                    "Callback dispatch native-root guard blocked a legal child or leaked state"
            );
        }
        verifyCleanup(probe);

        probe.clear();
        probe.unconditionalNativeRootTarget = probe.spawn();
        probe.unconditionalNativeRootTarget.setAbsorptionAmount(2.0f);
        LivingEntity loopParentTarget = probe.spawn();
        float loopHealth = probe.unconditionalNativeRootTarget.getHealth();
        float loopAbsorption =
                probe.unconditionalNativeRootTarget.getAbsorptionAmount();
        admissionsBefore = DamageAdmissionController.currentTickCount(
                probe.levelHelper.getLevel().getServer());
        probe.unconditionalNativeRootAttemptActive = true;
        DamageResult loopParent;
        try {
            loopParent = DamageNexusApi.submitDamage(request(
                    probe.levelHelper,
                    loopParentTarget,
                    probe.attacker,
                    DamageRequestKind.PRIMARY,
                    1.0f
            ).build());
        } finally {
            probe.unconditionalNativeRootAttemptActive = false;
        }
        assertStatus(loopParent, DamageSubmissionStatus.APPLIED, null);
        if (probe.unconditionalNativeRootAttempts != 1
                || probe.unconditionalNativeRootAccepted
                || probe.unconditionalNativeRootTarget.getHealth()
                != loopHealth
                || probe.unconditionalNativeRootTarget.getAbsorptionAmount()
                != loopAbsorption
                || probe.snapshots.size() != 1
                || loopParent.lineage().derivedRequestCountInternal() != 0
                || DamageAdmissionController.currentTickCount(
                probe.levelHelper.getLevel().getServer())
                != admissionsBefore + 1) {
            throw new AssertionError(
                    "Unconditional native-root callback produced a rejection settlement loop"
            );
        }
        verifyCleanup(probe);
    }

    private static void verifyUnifiedCompletionFifo(SettlementProbe probe) {
        probe.clear();
        probe.fifoParentTarget = probe.spawn();
        probe.fifoNativeTarget = probe.spawn();
        int admissionsBefore = DamageAdmissionController.currentTickCount(
                probe.levelHelper.getLevel().getServer());

        DamageResult parent = DamageNexusApi.submitDamage(request(
                probe.levelHelper,
                probe.fifoParentTarget,
                probe.attacker,
                DamageRequestKind.PRIMARY,
                1.0f
        ).build());
        assertStatus(parent, DamageSubmissionStatus.APPLIED, null);
        if (!probe.fifoNestedNativeAttempted
                || !probe.fifoNativeAccepted
                || probe.snapshots.size() != 2
                || probe.snapshots.get(0).target() != probe.fifoNativeTarget
                || probe.snapshots.get(1).target() != probe.fifoParentTarget
                || DamageAdmissionController.currentTickCount(
                probe.levelHelper.getLevel().getServer())
                != admissionsBefore + 2) {
            throw new AssertionError(
                    "Nested-native/outer-public completion publication was not strict FIFO"
            );
        }
        verifyCleanup(probe);
    }

    private static void verifySameKindLoop(
            SettlementProbe probe,
            DamageRequestKind kind,
            DamageFailureReason expectedReason
    ) {
        probe.clear();
        LivingEntity rootTarget = probe.spawn();
        LivingEntity firstTarget = probe.spawn();
        LivingEntity rejectedTarget = probe.spawn();
        float healthBefore = rejectedTarget.getHealth();
        float absorptionBefore = rejectedTarget.getAbsorptionAmount();
        DamageResult[] results = new DamageResult[2];
        probe.settlementAction = callback -> {
            if (callback.snapshot().target() == rootTarget) {
                results[0] = DamageNexusApi.submitDamage(request(
                        probe.levelHelper,
                        firstTarget,
                        probe.attacker,
                        kind,
                        1.0f
                ).parent(callback.childAuthority().orElseThrow()).build());
            } else if (callback.snapshot().target() == firstTarget) {
                probe.settlementAction = null;
                results[1] = DamageNexusApi.submitDamage(request(
                        probe.levelHelper,
                        rejectedTarget,
                        probe.attacker,
                        kind,
                        1.0f
                ).parent(callback.childAuthority().orElseThrow()).build());
            }
        };
        DamageResult rootResult = DamageNexusApi.submitDamage(request(
                probe.levelHelper,
                rootTarget,
                probe.attacker,
                DamageRequestKind.PRIMARY,
                1.0f
        ).build());
        assertStatus(rootResult, DamageSubmissionStatus.APPLIED, null);

        DamageResult first = Objects.requireNonNull(results[0]);
        assertStatus(first, DamageSubmissionStatus.APPLIED, null);
        DamageResult rejected = Objects.requireNonNull(results[1]);
        probe.snapshots.clear();
        assertPrePipelineRejected(
                probe,
                rejected,
                expectedReason,
                rejectedTarget,
                healthBefore,
                absorptionBefore
        );
    }

    private static void verifyUnfilteredProcListenerStops(
            SettlementProbe probe
    ) {
        probe.clear();
        LivingEntity firstTarget = probe.spawn();
        LivingEntity secondTarget = probe.spawn();
        float firstBefore = firstTarget.getHealth();
        float secondBefore = secondTarget.getHealth();
        probe.unfilteredProcTargets.addLast(firstTarget);
        probe.unfilteredProcTargets.addLast(secondTarget);
        probe.unfilteredProcLoopActive = true;
        try {
            DamageResult root = DamageNexusApi.submitDamage(request(
                    probe.levelHelper,
                    probe.spawn(),
                    probe.attacker,
                    DamageRequestKind.PRIMARY,
                    1.0f
            ).build());
            assertStatus(root, DamageSubmissionStatus.APPLIED, null);
        } finally {
            probe.unfilteredProcLoopActive = false;
        }

        long applied = probe.unfilteredProcResults.stream()
                .filter(DamageResult::applied)
                .count();
        long suppressed = probe.unfilteredProcResults.stream()
                .filter(result -> result.failure().isPresent()
                        && result.failure().orElseThrow().reason()
                        == DamageFailureReason.PROC_SUPPRESSED)
                .count();
        if (probe.unfilteredProcResults.size() != 2
                || applied != 1
                || suppressed != 1
                || probe.snapshots.size() != 2
                || firstTarget.getHealth() >= firstBefore
                || secondTarget.getHealth() != secondBefore) {
            throw new AssertionError(
                    "Unfiltered PROC listener was not stopped by policy"
            );
        }
        probe.unfilteredProcTargets.clear();
        probe.unfilteredProcResults.clear();
        probe.unfilteredProcRootId = 0L;
        verifyCleanup(probe);
    }

    private static void verifyCrossKindListenersTerminate(
            SettlementProbe probe
    ) {
        probe.clear();
        probe.crossKindRootTarget = probe.spawn();
        probe.crossKindProcTarget = probe.spawn();
        probe.crossKindReflectedTarget = probe.spawn();
        probe.crossKindRejectedTarget = probe.spawn();
        float procBefore = probe.crossKindProcTarget.getHealth();
        float reflectedBefore = probe.crossKindReflectedTarget.getHealth();
        float rejectedBefore = probe.crossKindRejectedTarget.getHealth();
        probe.crossKindLoopActive = true;

        try {
            DamageResult root = DamageNexusApi.submitDamage(request(
                    probe.levelHelper,
                    probe.crossKindRootTarget,
                    probe.attacker,
                    DamageRequestKind.PRIMARY,
                    1.0f
            ).build());
            assertStatus(root, DamageSubmissionStatus.APPLIED, null);

            long applied = probe.crossKindResults.stream()
                    .filter(DamageResult::applied)
                    .count();
            long procSuppressed = probe.crossKindResults.stream()
                    .filter(result -> result.failure().isPresent()
                            && result.failure().orElseThrow().reason()
                            == DamageFailureReason.PROC_SUPPRESSED)
                    .count();
            if (probe.crossKindResults.size() != 3
                    || applied != 2
                    || procSuppressed != 1
                    || probe.snapshots.size() != 3
                    || probe.crossKindProcTarget.getHealth() >= procBefore
                    || probe.crossKindReflectedTarget.getHealth()
                    >= reflectedBefore
                    || probe.crossKindRejectedTarget.getHealth()
                    != rejectedBefore
                    || root.settlement().orElseThrow().lineage()
                    .derivedRequestCountInternal() != 2) {
                throw new AssertionError(
                        "Cross-kind listeners did not terminate monotonically"
                );
            }
            verifyCleanup(probe);
        } finally {
            probe.crossKindLoopActive = false;
            probe.crossKindRootTarget = null;
            probe.crossKindProcTarget = null;
            probe.crossKindReflectedTarget = null;
            probe.crossKindRejectedTarget = null;
            probe.crossKindResults.clear();
        }
    }

    private static void verifyLineageDepthLimit(SettlementProbe probe) {
        DamageNexusConfigValues original = DamageNexusConfig.current();
        setSafety(
                original,
                new DamageSafetySettings(
                        2,
                        64,
                        original.safety()
                                .maxManagedRequestsPerServerTick()
                )
        );
        try {
            probe.clear();
            LivingEntity rootTarget = probe.spawn();
            LivingEntity firstTarget = probe.spawn();
            LivingEntity secondTarget = probe.spawn();
            LivingEntity rejectedTarget = probe.spawn();
            float rejectedBefore = rejectedTarget.getHealth();
            float rejectedAbsorption = rejectedTarget.getAbsorptionAmount();
            DamageResult[] children = new DamageResult[3];
            probe.settlementAction = callback -> {
                LivingEntity nextTarget;
                int index;
                if (callback.snapshot().target() == rootTarget) {
                    nextTarget = firstTarget;
                    index = 0;
                } else if (callback.snapshot().target() == firstTarget) {
                    nextTarget = secondTarget;
                    index = 1;
                } else if (callback.snapshot().target() == secondTarget) {
                    nextTarget = rejectedTarget;
                    index = 2;
                    probe.settlementAction = null;
                } else {
                    return;
                }
                children[index] = DamageNexusApi.submitDamage(request(
                        probe.levelHelper,
                        nextTarget,
                        probe.attacker,
                        DamageRequestKind.DOT,
                        1.0f
                ).parent(callback.childAuthority().orElseThrow()).build());
            };
            DamageResult root = DamageNexusApi.submitDamage(request(
                    probe.levelHelper,
                    rootTarget,
                    probe.attacker,
                    DamageRequestKind.PRIMARY,
                    1.0f
            ).build());
            assertStatus(root, DamageSubmissionStatus.APPLIED, null);
            for (int depth = 1; depth <= 2; depth++) {
                DamageResult child = Objects.requireNonNull(
                        children[depth - 1]
                );
                assertStatus(child, DamageSubmissionStatus.APPLIED, null);
                if (child.settlement().orElseThrow().recursionDepth()
                        != depth) {
                    throw new AssertionError("DOT depth was not explicit");
                }
            }

            DamageResult rejected = Objects.requireNonNull(children[2]);
            probe.snapshots.clear();
            assertPrePipelineRejected(
                    probe,
                    rejected,
                    DamageFailureReason.MAX_RECURSION_DEPTH,
                    rejectedTarget,
                    rejectedBefore,
                    rejectedAbsorption
            );
            if (root.lineage().derivedRequestCountInternal() != 2) {
                throw new AssertionError(
                        "Depth rejection consumed a root budget slot"
                );
            }
        } finally {
            setConfig(original);
        }
    }

    private static void verifyRootDerivationBudget(SettlementProbe probe) {
        DamageNexusConfigValues original = DamageNexusConfig.current();
        setSafety(
                original,
                new DamageSafetySettings(
                        original.safety().maxRecursionDepth(),
                        2,
                        original.safety()
                                .maxManagedRequestsPerServerTick()
                )
        );
        try {
            probe.clear();
            LivingEntity rootTarget = probe.spawn();
            LivingEntity firstTarget = probe.spawn();
            LivingEntity secondTarget = probe.spawn();
            LivingEntity rejectedTarget = probe.spawn();
            float rejectedBefore = rejectedTarget.getHealth();
            float rejectedAbsorption = rejectedTarget.getAbsorptionAmount();
            DamageResult[] children = new DamageResult[3];
            probe.settlementAction = callback -> {
                if (callback.snapshot().target() != rootTarget) {
                    return;
                }
                probe.settlementAction = null;
                DamageParentRef authority =
                        callback.childAuthority().orElseThrow();
                LivingEntity[] targets = {
                        firstTarget, secondTarget, rejectedTarget
                };
                for (int index = 0; index < targets.length; index++) {
                    children[index] = DamageNexusApi.submitDamage(request(
                            probe.levelHelper,
                            targets[index],
                            probe.attacker,
                            DamageRequestKind.CUSTOM,
                            1.0f
                    ).parent(authority).build());
                }
            };
            DamageResult root = DamageNexusApi.submitDamage(request(
                    probe.levelHelper,
                    rootTarget,
                    probe.attacker,
                    DamageRequestKind.PRIMARY,
                    1.0f
            ).build());
            assertStatus(root, DamageSubmissionStatus.APPLIED, null);
            for (int index = 0; index < 2; index++) {
                DamageResult child = Objects.requireNonNull(children[index]);
                assertStatus(
                        child,
                        DamageSubmissionStatus.APPLIED,
                        null
                );
            }

            DamageResult rejected = Objects.requireNonNull(children[2]);
            probe.snapshots.clear();
            assertPrePipelineRejected(
                    probe,
                    rejected,
                    DamageFailureReason.ROOT_DERIVATION_LIMIT,
                    rejectedTarget,
                    rejectedBefore,
                    rejectedAbsorption
            );
            if (root.lineage().derivedRequestCountInternal() != 2) {
                throw new AssertionError(
                        "Root derived count did not remain at its limit"
                );
            }
        } finally {
            setConfig(original);
        }
    }

    private static void verifyMultipleListenersShareRootBudget(
            SettlementProbe probe
    ) {
        DamageNexusConfigValues original = DamageNexusConfig.current();
        setSafety(
                original,
                new DamageSafetySettings(
                        original.safety().maxRecursionDepth(),
                        1,
                        original.safety()
                                .maxManagedRequestsPerServerTick()
                )
        );
        probe.clear();
        probe.sharedBudgetParentTarget = probe.spawn();
        probe.sharedBudgetFirstTarget = probe.spawn();
        probe.sharedBudgetSecondTarget = probe.spawn();
        try {
            DamageResult parent = DamageNexusApi.submitDamage(request(
                    probe.levelHelper,
                    probe.sharedBudgetParentTarget,
                    probe.attacker,
                    DamageRequestKind.PRIMARY,
                    1.0f
            ).build());
            assertStatus(parent, DamageSubmissionStatus.APPLIED, null);

            long applied = probe.sharedBudgetResults.stream()
                    .filter(DamageResult::applied)
                    .count();
            long limited = probe.sharedBudgetResults.stream()
                    .filter(result -> result.failure().isPresent()
                            && result.failure().orElseThrow().reason()
                            == DamageFailureReason.ROOT_DERIVATION_LIMIT)
                    .count();
            if (probe.sharedBudgetResults.size() != 2
                    || applied != 1
                    || limited != 1
                    || parent.settlement().orElseThrow().lineage()
                    .derivedRequestCountInternal() != 1) {
                throw new AssertionError(
                        "Independent listeners did not share the root budget"
                );
            }
            verifyCleanup(probe);
        } finally {
            probe.sharedBudgetParentTarget = null;
            probe.sharedBudgetFirstTarget = null;
            probe.sharedBudgetSecondTarget = null;
            probe.sharedBudgetResults.clear();
            setConfig(original);
        }
    }

    private static void verifyNativeDamage(SettlementProbe probe) {
        probe.clear();
        LivingEntity target = probe.spawn(7);
        float before = target.getHealth();
        DamageSource source = probe.attacker.damageSources()
                .playerAttack(probe.attacker);

        boolean accepted = target.hurtServer(
                probe.levelHelper.getLevel(),
                source,
                2.0f
        );
        if (!accepted || probe.snapshots.size() != 1) {
            throw new AssertionError(
                    "Native managed damage did not publish exactly once"
            );
        }

        DamageSettlementSnapshot snapshot = probe.snapshots.getFirst();
        if (snapshot.requestKind() != DamageRequestKind.PRIMARY
                || snapshot.lineage().hasParent()
                || snapshot.actionId().isPresent()
                || !snapshot.sourceTags().isEmpty()
                || !snapshot.metadata().isEmpty()
                || target.getHealth() >= before) {
            throw new AssertionError("Native damage origin is incorrect");
        }
    }

    private static void verifyNativeProjectileAttribution(
            SettlementProbe probe
    ) {
        probe.clear();
        LivingEntity target = probe.spawn();
        Arrow arrow = probe.levelHelper.spawn(
                EntityType.ARROW,
                new BlockPos(probe.nextSpawnX++, 2, 1)
        );
        arrow.setOwner(probe.attacker);
        DamageSource source = target.damageSources().arrow(
                arrow,
                probe.attacker
        );

        boolean accepted = target.hurtServer(
                probe.levelHelper.getLevel(),
                source,
                2.0f
        );
        if (!accepted || probe.snapshots.size() != 1) {
            throw new AssertionError(
                    "Native projectile damage did not settle exactly once"
            );
        }
        DamageSettlementSnapshot snapshot = probe.snapshots.getFirst();
        if (snapshot.directEntity() != arrow
                || snapshot.logicalAttacker() != probe.attacker
                || snapshot.equipmentOwner() != probe.attacker) {
            throw new AssertionError(
                    "Native projectile equipment owner is not its shooter"
            );
        }
    }

    private static void verifyTrustedProxyAttribution(
            SettlementProbe probe
    ) {
        probe.clear();
        probe.proxyDirectEntity = probe.spawn();
        probe.proxyPublicTarget = probe.spawn();
        probe.throwingResolverInvocations = 0;
        probe.preferredResolverInvocations = 0;
        probe.conflictingResolverInvocations = 0;
        probe.proxyEquipmentRuleInvocations = 0;
        ItemStack original = probe.attacker.getMainHandItem().copy();
        probe.attacker.setItemSlot(
                EquipmentSlot.MAINHAND,
                ruleStack(
                        "gametest_proxy_equipment",
                        DamageRuleRole.OFFENSIVE,
                        DamagePhase.BASE_MODIFICATION,
                        List.of(new Phase5CountingOperation(
                                Phase5Counter.PROXY_EQUIPMENT
                        ))
                )
        );
        try {
            DamageResult result = DamageNexusApi.submitDamage(request(
                    probe.levelHelper,
                    probe.proxyPublicTarget,
                    probe.proxyDirectEntity,
                    DamageRequestKind.PRIMARY,
                    1.0f
            ).directEntity(probe.proxyDirectEntity)
                    .effectOwner(probe.proxyDirectEntity)
                    .equipmentOwner(probe.attacker)
                    .build());
            assertStatus(result, DamageSubmissionStatus.APPLIED, null);
            DamageSettlementSnapshot publicSnapshot = result.settlement()
                    .orElseThrow();
            if (publicSnapshot.directEntity() != probe.proxyDirectEntity
                    || publicSnapshot.logicalAttacker() != probe.attacker
                    || publicSnapshot.effectOwner() != probe.attacker
                    || publicSnapshot.equipmentOwner() != probe.attacker
                    || publicSnapshot.attributionSource()
                    != DamageAttributionSource.REGISTERED_RESOLVER
                    || !publicSnapshot.attributionResolverId().equals(
                    Optional.of(id("gametest_proxy_owner_resolver"))
            ) || probe.observedPublicDirect != probe.proxyDirectEntity
                    || probe.observedPublicLogical != probe.attacker) {
                throw new AssertionError(
                        "Public proxy attribution was not used consistently"
                );
            }
            if (probe.throwingResolverInvocations != 1
                    || probe.preferredResolverInvocations != 1
                    || probe.conflictingResolverInvocations != 1
                    || probe.proxyEquipmentRuleInvocations != 1) {
                throw new AssertionError(
                        "Resolver isolation/conflict or equipment collection was not deterministic"
                );
            }

            probe.clear();
            LivingEntity nativeTarget = probe.spawn();
            DamageSource nativeSource = probe.proxyDirectEntity
                    .damageSources()
                    .mobAttack(probe.proxyDirectEntity);
            boolean accepted = nativeTarget.hurtServer(
                    probe.levelHelper.getLevel(), nativeSource, 1.0f
            );
            if (!accepted || probe.snapshots.size() != 1) {
                throw new AssertionError(
                        "Native proxy damage did not complete its managed pipeline"
                );
            }
            DamageSettlementSnapshot nativeSnapshot = probe.snapshots.getFirst();
            if (nativeSnapshot.logicalAttacker() != probe.attacker
                    || nativeSnapshot.equipmentOwner() != probe.attacker
                    || nativeSnapshot.attributionSource()
                    != DamageAttributionSource.REGISTERED_RESOLVER
                    || nativeSnapshot.pipelineExecuted() == false
                    || probe.proxyEquipmentRuleInvocations != 2) {
                throw new AssertionError(
                        "Native proxy attribution did not enter the full pipeline"
                );
            }
        } finally {
            probe.attacker.setItemSlot(EquipmentSlot.MAINHAND, original);
            probe.proxyPublicTarget = null;
            probe.proxyDirectEntity = null;
            probe.observedPublicDirect = null;
            probe.observedPublicLogical = null;
        }
    }

    private static void verifyInvalidProxyResolution(
            SettlementProbe probe
    ) {
        probe.clear();
        probe.invalidProxyDirectEntity = probe.spawn();
        probe.removedResolverOwner = probe.spawn();
        probe.removedResolverOwner.discard();
        probe.invalidResolverInvocations = 0;
        LivingEntity target = probe.spawn();
        float healthBefore = target.getHealth();
        DamageResult result = DamageNexusApi.submitDamage(request(
                probe.levelHelper,
                target,
                probe.invalidProxyDirectEntity,
                DamageRequestKind.PRIMARY,
                1.0f
        ).directEntity(probe.invalidProxyDirectEntity)
                .effectOwner(probe.invalidProxyDirectEntity)
                .equipmentOwner(probe.attacker)
                .build());
        assertStatus(
                result,
                DamageSubmissionStatus.REJECTED,
                DamageFailureReason.EQUIPMENT_OWNER_UNAUTHORIZED
        );
        if (probe.invalidResolverInvocations != 1
                || target.getHealth() != healthBefore
                || result.pipelineExecuted()
                || result.settlement().isPresent()
                || !probe.snapshots.isEmpty()) {
            throw new AssertionError(
                    "Invalid resolver claim acquired attribution authority"
            );
        }
        probe.invalidProxyDirectEntity = null;
        probe.removedResolverOwner = null;
        verifyCleanup(probe);
    }

    private static void verifyNotAddedAttributionValidation(
            SettlementProbe probe
    ) {
        probe.clear();
        LivingEntity unattached = probe.createUnadded();
        if (unattached.isAddedToLevel() || unattached.isRemoved()) {
            throw new AssertionError(
                    "Unattached entity fixture has invalid lifecycle state"
            );
        }
        probe.structurallyInvalidCandidate = unattached;
        probe.structurallyInvalidResolverInvocations = 0;
        int budgetBefore = DamageAdmissionController.currentTickCount(
                probe.levelHelper.getLevel().getServer()
        );

        assertStructuralRejection(
                probe,
                request(
                        probe.levelHelper,
                        unattached,
                        probe.attacker,
                        DamageRequestKind.PRIMARY,
                        1.0f
                ).build(),
                DamageFailureReason.TARGET_NOT_ADDED,
                unattached
        );
        assertStructuralRejection(
                probe,
                request(
                        probe.levelHelper,
                        probe.spawn(),
                        probe.attacker,
                        DamageRequestKind.PRIMARY,
                        1.0f
                ).attribution(new DamageAttribution(
                        unattached,
                        probe.attacker,
                        probe.attacker,
                        probe.attacker
                )).build(),
                DamageFailureReason.DIRECT_ENTITY_INVALID,
                null
        );
        assertStructuralRejection(
                probe,
                request(
                        probe.levelHelper,
                        probe.spawn(),
                        probe.attacker,
                        DamageRequestKind.PRIMARY,
                        1.0f
                ).attribution(new DamageAttribution(
                        probe.attacker,
                        unattached,
                        probe.attacker,
                        null
                )).build(),
                DamageFailureReason.LOGICAL_ATTACKER_INVALID,
                null
        );
        assertStructuralRejection(
                probe,
                request(
                        probe.levelHelper,
                        probe.spawn(),
                        probe.attacker,
                        DamageRequestKind.PRIMARY,
                        1.0f
                ).attribution(new DamageAttribution(
                        probe.attacker,
                        probe.attacker,
                        unattached,
                        probe.attacker
                )).build(),
                DamageFailureReason.EFFECT_OWNER_INVALID,
                null
        );
        assertStructuralRejection(
                probe,
                request(
                        probe.levelHelper,
                        probe.spawn(),
                        probe.attacker,
                        DamageRequestKind.PRIMARY,
                        1.0f
                ).attribution(new DamageAttribution(
                        probe.attacker,
                        probe.attacker,
                        probe.attacker,
                        unattached
                )).build(),
                DamageFailureReason.EQUIPMENT_OWNER_INVALID,
                null
        );

        int budgetAfter = DamageAdmissionController.currentTickCount(
                probe.levelHelper.getLevel().getServer()
        );
        if (budgetAfter != budgetBefore
                || probe.structurallyInvalidResolverInvocations != 0
                || !probe.snapshots.isEmpty()) {
            throw new AssertionError(
                    "Structurally invalid public attribution reached resolver, admission, or settlement"
            );
        }
        verifyCleanup(probe);

        probe.clear();
        probe.proxyEquipmentRuleInvocations = 0;
        unattached.setItemSlot(
                EquipmentSlot.MAINHAND,
                ruleStack(
                        "gametest_unattached_native_equipment",
                        DamageRuleRole.OFFENSIVE,
                        DamagePhase.BASE_MODIFICATION,
                        List.of(new Phase5CountingOperation(
                                Phase5Counter.PROXY_EQUIPMENT
                        ))
                )
        );
        LivingEntity nativeTarget = probe.spawn();
        float healthBefore = nativeTarget.getHealth();
        boolean accepted = nativeTarget.hurtServer(
                probe.levelHelper.getLevel(),
                unattached.damageSources().mobAttack(unattached),
                1.0f
        );
        if (!accepted || !(nativeTarget.getHealth() < healthBefore)
                || probe.snapshots.size() != 1) {
            throw new AssertionError(
                    "Invalid native attribution incorrectly cancelled vanilla damage"
            );
        }
        DamageSettlementSnapshot snapshot = probe.snapshots.getFirst();
        if (snapshot.directEntity() != null
                || snapshot.logicalAttacker() != null
                || snapshot.effectOwner() != null
                || snapshot.equipmentOwner() != null
                || snapshot.requestKind()
                != DamageRequestKind.ENVIRONMENTAL
                || snapshot.attributionSource()
                != DamageAttributionSource.VANILLA_DEFAULT
                || probe.proxyEquipmentRuleInvocations != 0) {
            throw new AssertionError(
                    "Invalid native attribution was not normalized before rule collection"
            );
        }
        probe.structurallyInvalidCandidate = null;
        verifyCleanup(probe);
    }

    private static void assertStructuralRejection(
            SettlementProbe probe,
            DamageRequest request,
            DamageFailureReason expected,
            LivingEntity explicitTarget
    ) {
        LivingEntity target = explicitTarget == null
                ? request.target()
                : explicitTarget;
        float healthBefore = target.getHealth();
        float absorptionBefore = target.getAbsorptionAmount();
        DamageResult result = DamageNexusApi.submitDamage(request);
        assertStatus(result, DamageSubmissionStatus.REJECTED, expected);
        if (result.pipelineExecuted()
                || result.settlement().isPresent()
                || target.getHealth() != healthBefore
                || target.getAbsorptionAmount() != absorptionBefore) {
            throw new AssertionError(
                    "Structurally invalid public request entered managed damage"
            );
        }
    }

    private static void verifyPhase6OriginConditions(
            SettlementProbe probe
    ) {
        probe.clear();
        probe.phase6ConditionInvocations = 0;
        ItemStack original = probe.attacker.getMainHandItem().copy();
        Identifier inheritedAction = Identifier.fromNamespaceAndPath(
                "examplemod", "request_test"
        );
        Identifier inheritedTag = Identifier.fromNamespaceAndPath(
                "examplemod", "test_damage"
        );
        Identifier overrideAction = Identifier.fromNamespaceAndPath(
                "contentmod", "override_action"
        );
        TagKey<MobEffect> reloadableEffectTag = TagKey.create(
                Registries.MOB_EFFECT,
                Identifier.fromNamespaceAndPath(
                        "contentmod", "gametest_reloadable_effects"
                )
        );
        MobEffectTagBindingScope effectTagScope =
                MobEffectTagBindingScope.open(probe, reloadableEffectTag);
        try {
            probe.attacker.setItemSlot(
                    EquipmentSlot.MAINHAND,
                    ruleStack(
                            "gametest_phase6_primary_conditions",
                            DamageRuleRole.OFFENSIVE,
                            DamagePhase.BASE_MODIFICATION,
                            List.of(DamageNexusConditions.allOf(
                                    DamageNexusConditions.sourceActionIs(
                                            inheritedAction
                                    ),
                                    DamageNexusConditions.sourceTag(
                                            inheritedTag
                                    ),
                                    DamageNexusConditions.requestKindIs(
                                            DamageRequestKind.PRIMARY
                                    ),
                                    DamageNexusConditions.isPrimaryDamage(),
                                    DamageNexusConditions.not(
                                            DamageNexusConditions.isProcDamage()
                                    ),
                                    DamageNexusConditions.not(
                                            DamageNexusConditions.hasParentDamage()
                                    ),
                                    DamageNexusConditions.procAllowed()
                            )),
                            List.of(new Phase5CountingOperation(
                                    Phase5Counter.PHASE6_CONDITION
                            ))
                    )
            );
            DamageResult primary = DamageNexusApi.submitDamage(request(
                    probe.levelHelper,
                    probe.spawn(),
                    probe.attacker,
                    DamageRequestKind.PRIMARY,
                    1.0f
            ).metadata(TEST_ORIGIN_FLAG, true).build());
            assertStatus(primary, DamageSubmissionStatus.APPLIED, null);
            if (probe.phase6ConditionInvocations != 1) {
                throw new AssertionError(
                        "PRIMARY source metadata conditions did not gate a real rule"
                );
            }

            probe.clear();
            LivingEntity nativeTarget = probe.spawn();
            boolean nativeAccepted = nativeTarget.hurtServer(
                    probe.levelHelper.getLevel(),
                    probe.attacker.damageSources().mobAttack(probe.attacker),
                    1.0f
            );
            if (!nativeAccepted || probe.snapshots.size() != 1
                    || probe.phase6ConditionInvocations != 1
                    || probe.snapshots.getFirst().actionId().isPresent()
                    || !probe.snapshots.getFirst().sourceTags().isEmpty()) {
                throw new AssertionError(
                        "Native origin incorrectly acquired public action/tag metadata"
                );
            }

            probe.attacker.setItemSlot(
                    EquipmentSlot.MAINHAND,
                    ruleStack(
                            "gametest_phase6_proc_conditions",
                            DamageRuleRole.OFFENSIVE,
                            DamagePhase.BASE_MODIFICATION,
                            List.of(DamageNexusConditions.allOf(
                                    DamageNexusConditions.sourceActionIs(
                                            inheritedAction
                                    ),
                                    DamageNexusConditions.sourceTag(
                                            inheritedTag
                                    ),
                                    DamageNexusConditions.requestKindIs(
                                            DamageRequestKind.PROC
                                    ),
                                    DamageNexusConditions.isProcDamage(),
                                    DamageNexusConditions.hasParentDamage(),
                                    DamageNexusConditions.not(
                                            DamageNexusConditions.procAllowed()
                                    )
                            )),
                            List.of(new Phase5CountingOperation(
                                    Phase5Counter.PHASE6_CONDITION
                            ))
                    )
            );
            probe.clear();
            LivingEntity procParentTarget = probe.spawn();
            DamageResult[] childHolder = new DamageResult[1];
            probe.settlementAction = callback -> {
                if (callback.snapshot().target() != procParentTarget) {
                    return;
                }
                probe.settlementAction = null;
                childHolder[0] = DamageNexusApi.submitDamage(
                        DamageRequest.builder(
                                        probe.levelHelper.getLevel(),
                                        probe.spawn(),
                                        DamageSourceDescriptor.of(
                                                DamageTypes.PLAYER_ATTACK
                                        ),
                                        1.0f
                                )
                                .logicalAttacker(probe.attacker)
                                .kind(DamageRequestKind.PROC)
                                .inheritFrom(
                                        callback.childAuthority().orElseThrow(),
                                        DamageInheritancePolicy.SOURCE_METADATA
                                )
                                .build()
                );
            };
            DamageResult procParent = DamageNexusApi.submitDamage(request(
                    probe.levelHelper,
                    procParentTarget,
                    probe.attacker,
                    DamageRequestKind.PRIMARY,
                    1.0f
            ).metadata(TEST_ORIGIN_FLAG, true).build());
            assertStatus(procParent, DamageSubmissionStatus.APPLIED, null);
            DamageResult child = Objects.requireNonNull(childHolder[0]);
            assertStatus(child, DamageSubmissionStatus.APPLIED, null);
            DamageSettlementSnapshot childSnapshot = child.settlement()
                    .orElseThrow();
            if (probe.phase6ConditionInvocations != 2
                    || childSnapshot.requestKind() != DamageRequestKind.PROC
                    || !childSnapshot.lineage().hasParent()
                    || childSnapshot.triggerPolicy().procAllowed()
                    || !childSnapshot.actionId().equals(
                    Optional.of(inheritedAction)
            ) || !childSnapshot.sourceTags().contains(inheritedTag)
                    || !childSnapshot.metadata().get(TEST_ORIGIN_FLAG)
                    .equals(Optional.of(true))) {
                throw new AssertionError(
                        "PROC child conditions did not observe inherited final origin"
                );
            }

            probe.attacker.setItemSlot(
                    EquipmentSlot.MAINHAND,
                    ruleStack(
                            "gametest_phase6_override_conditions",
                            DamageRuleRole.OFFENSIVE,
                            DamagePhase.BASE_MODIFICATION,
                            List.of(
                                    DamageNexusConditions.sourceActionIs(
                                            overrideAction
                                    ),
                                    DamageNexusConditions.sourceTag(inheritedTag),
                                    DamageNexusConditions.hasParentDamage()
                            ),
                            List.of(new Phase5CountingOperation(
                                    Phase5Counter.PHASE6_CONDITION
                            ))
                    )
            );
            probe.clear();
            LivingEntity overrideParentTarget = probe.spawn();
            DamageResult[] overrideHolder = new DamageResult[1];
            probe.settlementAction = callback -> {
                if (callback.snapshot().target() != overrideParentTarget) {
                    return;
                }
                probe.settlementAction = null;
                overrideHolder[0] = DamageNexusApi.submitDamage(
                        DamageRequest.builder(
                                        probe.levelHelper.getLevel(),
                                        probe.spawn(),
                                        DamageSourceDescriptor.of(
                                                DamageTypes.PLAYER_ATTACK
                                        ),
                                        1.0f
                                )
                                .logicalAttacker(probe.attacker)
                                .kind(DamageRequestKind.PROC)
                                .inheritFrom(
                                        callback.childAuthority().orElseThrow(),
                                        DamageInheritancePolicy.SOURCE_METADATA
                                )
                                .actionId(overrideAction)
                                .metadata(TEST_ORIGIN_FLAG, false)
                                .build()
                );
            };
            DamageResult overrideParent = DamageNexusApi.submitDamage(request(
                    probe.levelHelper,
                    overrideParentTarget,
                    probe.attacker,
                    DamageRequestKind.PRIMARY,
                    1.0f
            ).metadata(TEST_ORIGIN_FLAG, true).build());
            assertStatus(overrideParent, DamageSubmissionStatus.APPLIED, null);
            DamageResult overridden = Objects.requireNonNull(overrideHolder[0]);
            assertStatus(overridden, DamageSubmissionStatus.APPLIED, null);
            DamageSettlementSnapshot overriddenSnapshot = overridden
                    .settlement().orElseThrow();
            if (probe.phase6ConditionInvocations != 3
                    || !overriddenSnapshot.actionId().equals(
                    Optional.of(overrideAction)
            ) || !overriddenSnapshot.sourceTags().contains(inheritedTag)
                    || !overriddenSnapshot.metadata().get(TEST_ORIGIN_FLAG)
                    .equals(Optional.of(false))) {
                throw new AssertionError(
                        "Explicit child origin values did not override inherited values"
                );
            }

            effectTagScope.bind(List.of(MobEffects.SPEED));
            probe.attacker.addEffect(new MobEffectInstance(
                    MobEffects.SPEED, 200
            ));
            LivingEntity effectTarget = probe.spawn();
            effectTarget.addEffect(new MobEffectInstance(
                    MobEffects.SPEED, 200
            ));
            probe.attacker.setItemSlot(
                    EquipmentSlot.MAINHAND,
                    ruleStack(
                            "gametest_phase6_effect_tag",
                            DamageRuleRole.OFFENSIVE,
                            DamagePhase.BASE_MODIFICATION,
                            List.of(
                                    DamageNexusConditions.attackerEffectTag(
                                            reloadableEffectTag
                                    ),
                                    DamageNexusConditions.targetEffectTag(
                                            reloadableEffectTag
                                    )
                            ),
                            List.of(new Phase5CountingOperation(
                                    Phase5Counter.PHASE6_CONDITION
                            ))
                    )
            );
            probe.clear();
            DamageResult matchingEffects = DamageNexusApi.submitDamage(request(
                    probe.levelHelper,
                    effectTarget,
                    probe.attacker,
                    DamageRequestKind.PRIMARY,
                    1.0f
            ).build());
            assertStatus(
                    matchingEffects,
                    DamageSubmissionStatus.APPLIED,
                    null
            );
            if (probe.phase6ConditionInvocations != 4) {
                throw new AssertionError(
                        "Matching attacker/target MobEffect tags did not execute a real rule"
                );
            }

            probe.clear();
            DamageResult targetMismatch = DamageNexusApi.submitDamage(request(
                    probe.levelHelper,
                    probe.spawn(),
                    probe.attacker,
                    DamageRequestKind.PRIMARY,
                    1.0f
            ).build());
            assertStatus(targetMismatch, DamageSubmissionStatus.APPLIED, null);
            if (probe.phase6ConditionInvocations != 4) {
                throw new AssertionError(
                        "Non-matching target effect unexpectedly passed effect-tag conditions"
                );
            }

            probe.attacker.removeAllEffects();
            LivingEntity attackerMismatchTarget = probe.spawn();
            attackerMismatchTarget.addEffect(new MobEffectInstance(
                    MobEffects.SPEED, 200
            ));
            probe.clear();
            DamageResult attackerMismatch = DamageNexusApi.submitDamage(request(
                    probe.levelHelper,
                    attackerMismatchTarget,
                    probe.attacker,
                    DamageRequestKind.PRIMARY,
                    1.0f
            ).build());
            assertStatus(attackerMismatch, DamageSubmissionStatus.APPLIED, null);
            if (probe.phase6ConditionInvocations != 4) {
                throw new AssertionError(
                        "Non-matching attacker effect unexpectedly passed effect-tag conditions"
                );
            }

            probe.attacker.addEffect(new MobEffectInstance(
                    MobEffects.SPEED, 200
            ));
            effectTagScope.bind(List.of());
            LivingEntity reboundTarget = probe.spawn();
            reboundTarget.addEffect(new MobEffectInstance(
                    MobEffects.SPEED, 200
            ));
            probe.clear();
            DamageResult rebound = DamageNexusApi.submitDamage(request(
                    probe.levelHelper,
                    reboundTarget,
                    probe.attacker,
                    DamageRequestKind.PRIMARY,
                    1.0f
            ).build());
            assertStatus(rebound, DamageSubmissionStatus.APPLIED, null);
            if (probe.phase6ConditionInvocations != 4) {
                throw new AssertionError(
                        "Existing effect-tag condition cached pre-reload members"
                );
            }
        } finally {
            effectTagScope.close();
            probe.attacker.removeAllEffects();
            probe.attacker.setItemSlot(EquipmentSlot.MAINHAND, original);
        }
        verifyCleanup(probe);
    }

    private static void verifyPhase7CriticalDecisions(SettlementProbe probe) {
        var chance = probe.attacker.getAttribute(ModAttributes.CRIT_CHANCE);
        var damage = probe.attacker.getAttribute(
                ModAttributes.CRIT_DAMAGE_ADDITIVE);
        if (chance == null || damage == null) {
            throw new AssertionError("Phase 7 test player lacks critical attributes");
        }
        double originalChance = chance.getBaseValue();
        double originalDamage = damage.getBaseValue();
        ItemStack originalMainhand = probe.attacker.getMainHandItem().copy();
        try {
            chance.setBaseValue(0.0D);
            damage.setBaseValue(0.0D);

            probe.clear();
            DamageSettlementSnapshot defaultSnapshot = DamageNexusApi.submitDamage(
                    request(probe.levelHelper, criticalTarget(probe), probe.attacker,
                            DamageRequestKind.PRIMARY, 2.0f).build()
            ).settlement().orElseThrow();
            assertCriticalDecision(defaultSnapshot, CriticalDecision.DEFAULT,
                    CriticalDecisionOutcome.DEFAULT_NON_CRITICAL,
                    false, false);
            assertClose(defaultSnapshot.resolvedDamage(), 2.0f,
                    "default non-critical formula");

            chance.setBaseValue(1.0D);
            probe.clear();
            DamageSettlementSnapshot chanceSnapshot = DamageNexusApi.submitDamage(
                    request(probe.levelHelper, criticalTarget(probe), probe.attacker,
                            DamageRequestKind.PRIMARY, 2.0f).build()
            ).settlement().orElseThrow();
            assertCriticalDecision(chanceSnapshot, CriticalDecision.DEFAULT,
                    CriticalDecisionOutcome.ATTRIBUTE_CHANCE,
                    true, true);
            assertClose(chanceSnapshot.resolvedDamage(), 3.0f,
                    "attribute critical formula");

            chance.setBaseValue(0.0D);
            probe.clear();
            probe.criticalForceTarget = criticalTarget(probe);
            DamageSettlementSnapshot forced = DamageNexusApi.submitDamage(
                    request(probe.levelHelper, probe.criticalForceTarget,
                            probe.attacker, DamageRequestKind.PRIMARY, 2.0f).build()
            ).settlement().orElseThrow();
            assertCriticalDecision(forced, CriticalDecision.FORCE_CRITICAL,
                    CriticalDecisionOutcome.FORCED, true, false);
            assertClose(forced.resolvedDamage(), 3.0f,
                    "forced critical formula");
            if (probe.criticalForceInvocations != 1
                    || forced.criticalDecision().contributions().size() != 1) {
                throw new AssertionError("FORCE provider was not one-shot/idempotent");
            }
            try {
                probe.retainedCriticalCollector.contribute(
                        CriticalDecision.SUPPRESS_CRITICAL);
                throw new AssertionError("Closed decision collector accepted a late contribution");
            } catch (IllegalStateException expected) {
                // Callback-scoped collector is closed after provider return.
            }

            chance.setBaseValue(1.0D);
            probe.clear();
            probe.criticalSuppressTarget = criticalTarget(probe);
            DamageSettlementSnapshot suppressed = DamageNexusApi.submitDamage(
                    request(probe.levelHelper, probe.criticalSuppressTarget,
                            probe.attacker, DamageRequestKind.PRIMARY, 2.0f).build()
            ).settlement().orElseThrow();
            assertCriticalDecision(suppressed, CriticalDecision.SUPPRESS_CRITICAL,
                    CriticalDecisionOutcome.SUPPRESSED, false, false);
            assertClose(suppressed.resolvedDamage(), 2.0f,
                    "suppressed critical formula");

            probe.clear();
            LivingEntity conflictTarget = probe.spawn();
            probe.criticalForceTarget = conflictTarget;
            probe.criticalSuppressTarget = conflictTarget;
            DamageSettlementSnapshot conflict = DamageNexusApi.submitDamage(
                    request(probe.levelHelper, conflictTarget, probe.attacker,
                            DamageRequestKind.PRIMARY, 2.0f).build()
            ).settlement().orElseThrow();
            assertCriticalDecision(conflict, CriticalDecision.SUPPRESS_CRITICAL,
                    CriticalDecisionOutcome.SUPPRESSED, false, false);

            chance.setBaseValue(0.0D);
            probe.clear();
            probe.criticalThrowingTarget = probe.spawn();
            DamageSettlementSnapshot isolated = DamageNexusApi.submitDamage(
                    request(probe.levelHelper, probe.criticalThrowingTarget,
                            probe.attacker, DamageRequestKind.PRIMARY, 2.0f).build()
            ).settlement().orElseThrow();
            assertCriticalDecision(isolated, CriticalDecision.DEFAULT,
                    CriticalDecisionOutcome.DEFAULT_NON_CRITICAL,
                    false, false);
            if (probe.criticalThrowingInvocations != 1
                    || !isolated.criticalDecision().contributions().isEmpty()) {
                throw new AssertionError("Throwing provider leaked its local FORCE");
            }

            probe.clear();
            probe.phase7CriticalRuleInvocations = 0;
            probe.criticalForceTarget = probe.spawn();
            probe.attacker.setItemSlot(
                    EquipmentSlot.MAINHAND,
                    ruleStack(
                            "gametest_phase7_is_critical",
                            DamageRuleRole.OFFENSIVE,
                            DamagePhase.CRITICAL_HIT,
                            List.of(DamageNexusConditions.critical()),
                            List.of(new Phase5CountingOperation(
                                    Phase5Counter.PHASE7_CRITICAL_RULE))
                    )
            );
            DamageNexusApi.submitDamage(request(
                    probe.levelHelper, probe.criticalForceTarget, probe.attacker,
                    DamageRequestKind.PRIMARY, 2.0f).build());
            if (probe.phase7CriticalRuleInvocations != 1) {
                throw new AssertionError("is_critical rule ran before final decision");
            }
            probe.attacker.setItemSlot(EquipmentSlot.MAINHAND, originalMainhand);

            probe.clear();
            LivingEntity meleeTarget = criticalTarget(probe);
            VanillaCritHandler.onVanillaCriticalHit(new CriticalHitEvent(
                    probe.attacker, meleeTarget, 1.5f, true));
            DamageSettlementSnapshot melee = DamageNexusApi.submitDamage(
                    request(probe.levelHelper, meleeTarget, probe.attacker,
                            DamageRequestKind.PRIMARY, 2.0f).build()
            ).settlement().orElseThrow();
            assertCriticalDecision(melee, CriticalDecision.DEFAULT,
                    CriticalDecisionOutcome.VANILLA_MELEE, true, false);
            assertClose(melee.resolvedDamage(), 3.0f,
                    "vanilla melee critical formula");

            probe.clear();
            LivingEntity suppressedMeleeTarget = probe.spawn();
            probe.criticalSuppressTarget = suppressedMeleeTarget;
            VanillaCritHandler.onVanillaCriticalHit(new CriticalHitEvent(
                    probe.attacker, suppressedMeleeTarget, 1.5f, true));
            DamageSettlementSnapshot suppressedMelee = DamageNexusApi.submitDamage(
                    request(probe.levelHelper, suppressedMeleeTarget, probe.attacker,
                            DamageRequestKind.PRIMARY, 2.0f).build()
            ).settlement().orElseThrow();
            assertCriticalDecision(suppressedMelee,
                    CriticalDecision.SUPPRESS_CRITICAL,
                    CriticalDecisionOutcome.SUPPRESSED, false, false);

            verifyProjectileCriticalDecision(probe, false, false);
            verifyProjectileCriticalDecision(probe, true, false);
            verifyProjectileCriticalDecision(probe, false, true);

            damage.setBaseValue(0.20D);
            chance.setBaseValue(1.0D);
            probe.clear();
            DamageSettlementSnapshot additiveChance = DamageNexusApi.submitDamage(
                    request(probe.levelHelper, criticalTarget(probe), probe.attacker,
                            DamageRequestKind.PRIMARY, 2.0f).build()
            ).settlement().orElseThrow();
            assertClose(additiveChance.resolvedDamage(), 3.4f,
                    "attribute critical additive was not applied exactly once");
            chance.setBaseValue(0.0D);
            probe.clear();
            LivingEntity additiveMeleeTarget = criticalTarget(probe);
            VanillaCritHandler.onVanillaCriticalHit(new CriticalHitEvent(
                    probe.attacker, additiveMeleeTarget, 1.5f, true));
            DamageSettlementSnapshot additiveMelee = DamageNexusApi.submitDamage(
                    request(probe.levelHelper, additiveMeleeTarget, probe.attacker,
                            DamageRequestKind.PRIMARY, 2.0f).build()
            ).settlement().orElseThrow();
            assertClose(additiveMelee.resolvedDamage(), 3.4f,
                    "vanilla melee additive was not applied exactly once");
            verifyProjectileCriticalDecision(probe, false, false, 4.4f);
            verifyProjectileCriticalDecision(probe, false, true, 3.4f);
            damage.setBaseValue(0.0D);

            chance.setBaseValue(0.0D);
            probe.clear();
            probe.primaryTarget = probe.spawn();
            probe.childTarget = probe.spawn();
            probe.criticalForceTarget = probe.primaryTarget;
            DamageResult parentResult = DamageNexusApi.submitDamage(request(
                    probe.levelHelper, probe.primaryTarget, probe.attacker,
                    DamageRequestKind.PRIMARY, 2.0f).build());
            DamageSettlementSnapshot parent = parentResult.settlement().orElseThrow();
            DamageSettlementSnapshot child = probe.childResult
                    .settlement().orElseThrow();
            if (!parent.critical() || child.critical()
                    || child.criticalDecision().outcome()
                    != CriticalDecisionOutcome.DEFAULT_NON_CRITICAL) {
                throw new AssertionError("Child inherited its parent's critical decision");
            }

            probe.clear();
            LivingEntity specialTarget = probe.spawn();
            DamageResult specialDefault = DamageNexusApi.submitDamage(
                    DamageRequest.builder(
                            probe.levelHelper.getLevel(), specialTarget,
                            DamageSourceDescriptor.of(DamageTypes.MACE_SMASH), 2.0f)
                            .logicalAttacker(probe.attacker)
                            .directEntity(probe.attacker)
                            .effectOwner(probe.attacker)
                            .equipmentOwner(probe.attacker)
                            .build());
            assertCriticalDecision(specialDefault.settlement().orElseThrow(),
                    CriticalDecision.DEFAULT, CriticalDecisionOutcome.INELIGIBLE,
                    false, false);
            probe.clear();
            probe.criticalForceTarget = probe.spawn();
            DamageResult specialForced = DamageNexusApi.submitDamage(
                    DamageRequest.builder(
                            probe.levelHelper.getLevel(), probe.criticalForceTarget,
                            DamageSourceDescriptor.of(DamageTypes.MACE_SMASH), 2.0f)
                            .logicalAttacker(probe.attacker)
                            .directEntity(probe.attacker)
                            .effectOwner(probe.attacker)
                            .equipmentOwner(probe.attacker)
                            .build());
            assertCriticalDecision(specialForced.settlement().orElseThrow(),
                    CriticalDecision.FORCE_CRITICAL, CriticalDecisionOutcome.FORCED,
                    true, false);
        } finally {
            chance.setBaseValue(originalChance);
            damage.setBaseValue(originalDamage);
            probe.attacker.setItemSlot(EquipmentSlot.MAINHAND, originalMainhand);
            VanillaCritHandler.clear();
            ProjectileDamageCapture.clear();
            probe.criticalForceTarget = null;
            probe.criticalSuppressTarget = null;
            probe.criticalThrowingTarget = null;
            probe.retainedCriticalCollector = null;
        }
        verifyCleanup(probe);
    }

    private static void verifyProjectileCriticalDecision(
            SettlementProbe probe,
            boolean suppress,
            boolean forceNonCritical
    ) {
        float expected = suppress ? 2.0f : forceNonCritical ? 3.0f : 4.0f;
        verifyProjectileCriticalDecision(probe, suppress, forceNonCritical, expected);
    }

    private static void verifyPhase7Entrypoints(
            SettlementProbe probe
    ) {
        ItemStack original = probe.attacker.getMainHandItem().copy();
        boolean originalGround = probe.attacker.onGround();
        boolean originalSprinting = probe.attacker.isSprinting();
        double originalFallDistance = probe.attacker.fallDistance;
        try (AttributeBaseScope attributes = new AttributeBaseScope()) {
            attributes.set(probe.attacker, ModAttributes.CRIT_CHANCE, 0.0);
            attributes.set(probe.attacker, Attributes.ATTACK_SPEED, 1000.0);
            probe.attacker.setItemSlot(EquipmentSlot.MAINHAND,
                    new ItemStack(Items.IRON_SWORD));

            probe.clear();
            LivingEntity meleeTarget = phase8Target(probe);
            assertPlayerAttackCharged(probe.attacker);
            probe.attacker.setOnGround(false);
            probe.attacker.setSprinting(false);
            probe.attacker.fallDistance = 1.0;
            probe.attacker.attack(meleeTarget);
            if (probe.snapshots.size() != 1
                    || probe.snapshots.getFirst().criticalDecision().outcome()
                    != CriticalDecisionOutcome.VANILLA_MELEE
                    || !probe.snapshots.getFirst().critical()) {
                throw new AssertionError(
                        "Real Player.attack critical entry did not reach the decision engine");
            }

            probe.clear();
            attributes.set(probe.attacker, ModAttributes.CRIT_CHANCE, 1.0);
            LivingEntity projectileTarget = phase8Target(probe);
            Arrow arrow = probe.levelHelper.spawn(
                    EntityType.ARROW,
                    new BlockPos(probe.nextSpawnX++, 2, 1));
            arrow.setOwner(probe.attacker);
            arrow.setBaseDamage(2.0);
            arrow.setCritArrow(true);
            arrow.setDeltaMovement(1.0, 0.0, 0.0);
            invokeRealArrowHit(arrow, projectileTarget);
            if (probe.snapshots.size() != 1) {
                throw new AssertionError(
                        "Real AbstractArrow.onHitEntity entry did not settle once");
            }
            CriticalDecisionSnapshot projectile = probe.snapshots.getFirst()
                    .criticalDecision();
            if (projectile.outcome()
                    != CriticalDecisionOutcome.VANILLA_PROJECTILE
                    || !projectile.critical()
                    || projectile.chanceSampled()) {
                throw new AssertionError(
                        "Real projectile critical entry sampled or resolved incorrectly: "
                                + projectile);
            }
        } finally {
            probe.attacker.setItemSlot(EquipmentSlot.MAINHAND, original);
            probe.attacker.setOnGround(originalGround);
            probe.attacker.setSprinting(originalSprinting);
            probe.attacker.fallDistance = originalFallDistance;
        }
    }

    private static void verifyPhase8Attributes(SettlementProbe probe) {
        ItemStack original = probe.attacker.getMainHandItem().copy();
        try (AttributeBaseScope attributes = new AttributeBaseScope()) {
            zeroPhase8Offense(attributes, probe.attacker);
            probe.attacker.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);

            verifyChannelAttribute(probe, attributes, DamageTypes.ON_FIRE,
                    ModAttributes.FIRE_DAMAGE_ADDITIVE);
            verifyChannelAttribute(probe, attributes, DamageTypes.FREEZE,
                    ModAttributes.COLD_DAMAGE_ADDITIVE);
            verifyChannelAttribute(probe, attributes, DamageTypes.LIGHTNING_BOLT,
                    ModAttributes.LIGHTNING_DAMAGE_ADDITIVE);
            verifyChannelAttribute(probe, attributes, DamageTypes.MAGIC,
                    ModAttributes.MAGIC_DAMAGE_ADDITIVE);
            verifyChannelAttribute(probe, attributes, NeoForgeMod.POISON_DAMAGE,
                    ModAttributes.POISON_DAMAGE_ADDITIVE);
            verifyChannelAttribute(probe, attributes, DamageTypes.WITHER,
                    ModAttributes.WITHER_DAMAGE_ADDITIVE);
            verifyChannelAttribute(probe, attributes, DamageTypes.MACE_SMASH,
                    ModAttributes.KINETIC_DAMAGE_ADDITIVE);

            attributes.set(probe.attacker,
                    ModAttributes.POISON_DAMAGE_ADDITIVE, 0.25);
            probe.attacker.setItemSlot(EquipmentSlot.MAINHAND, ruleStack(
                    "gametest_phase8_poison_conversion",
                    DamagePhase.TYPE_SCALING,
                    List.of(DamageNexusOperations.convertDamage(
                            DamageChannel.PHYSICAL_ID,
                            DamageChannel.POISON_ID,
                            1.0f))));
            assertResolved(submitPhase8(probe, DamageTypes.PLAYER_ATTACK,
                    probe.attacker, null, 4.0f), 5.0f,
                    "poison channel attribute");
            attributes.set(probe.attacker,
                    ModAttributes.POISON_DAMAGE_ADDITIVE, 0.0);
            probe.attacker.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);

            attributes.set(probe.attacker,
                    ModAttributes.FIRE_DAMAGE_ADDITIVE, 0.5);
            assertResolved(submitPhase8(probe, DamageTypes.FREEZE,
                    probe.attacker, null, 4.0f), 4.0f,
                    "channel attribute isolation");
            attributes.set(probe.attacker,
                    ModAttributes.FIRE_DAMAGE_ADDITIVE, 0.0);

            attributes.set(probe.attacker,
                    ModAttributes.MELEE_DAMAGE_ADDITIVE, 0.2);
            assertResolved(submitPhase8(probe, DamageTypes.PLAYER_ATTACK,
                    probe.attacker, null, 4.0f), 4.8f,
                    "public melee category attribute");
            probe.clear();
            LivingEntity nativeMeleeTarget = phase8Target(probe);
            if (!nativeMeleeTarget.hurtServer(
                    probe.levelHelper.getLevel(),
                    nativeMeleeTarget.damageSources().playerAttack(probe.attacker),
                    4.0f) || probe.snapshots.size() != 1) {
                throw new AssertionError("Native melee category did not settle");
            }
            assertResolved(probe.snapshots.getFirst(), 4.8f,
                    "native melee category attribute");
            attributes.set(probe.attacker,
                    ModAttributes.MELEE_DAMAGE_ADDITIVE, 0.0);

            attributes.set(probe.attacker,
                    ModAttributes.PROJECTILE_DAMAGE_ADDITIVE, 0.3);
            attributes.set(probe.attacker,
                    ModAttributes.MELEE_DAMAGE_ADDITIVE, 0.2);
            Arrow classifiedArrow = phase8Arrow(probe);
            assertResolved(submitPhase8(probe, DamageTypes.ARROW,
                    probe.attacker, classifiedArrow, 4.0f), 5.2f,
                    "projectile category precedence");
            attributes.set(probe.attacker,
                    ModAttributes.PROJECTILE_DAMAGE_ADDITIVE, 0.0);
            attributes.set(probe.attacker,
                    ModAttributes.MELEE_DAMAGE_ADDITIVE, 0.0);

            assertResolved(submitPhase8(probe, DamageTypes.ON_FIRE,
                    null, null, 4.0f), 4.0f,
                    "environment category exclusion");

            attributes.set(probe.attacker,
                    ModAttributes.FIRE_DAMAGE_ADDITIVE, 0.25);
            probe.attacker.setItemSlot(EquipmentSlot.MAINHAND, ruleStack(
                    "gametest_phase8_convert_fire",
                    DamagePhase.TYPE_SCALING,
                    List.of(DamageNexusOperations.convertDamage(
                            DamageChannel.PHYSICAL_ID,
                            DamageChannel.FIRE_ID, 1.0f))));
            assertResolved(submitPhase8(probe, DamageTypes.PLAYER_ATTACK,
                    probe.attacker, null, 4.0f), 5.0f,
                    "converted target channel attribute");

            attributes.set(probe.attacker,
                    ModAttributes.FIRE_DAMAGE_ADDITIVE, 0.0);
            attributes.set(probe.attacker,
                    ModAttributes.LIGHTNING_DAMAGE_ADDITIVE, 0.2);
            probe.attacker.setItemSlot(EquipmentSlot.MAINHAND, ruleStack(
                    "gametest_phase8_gain_lightning",
                    DamagePhase.TYPE_SCALING,
                    List.of(DamageNexusOperations.gainExtraDamage(
                            DamageChannel.PHYSICAL_ID,
                            DamageChannel.LIGHTNING_ID, 0.5f))));
            assertResolved(submitPhase8(probe, DamageTypes.PLAYER_ATTACK,
                    probe.attacker, null, 4.0f), 6.4f,
                    "gained target channel attribute");

            attributes.set(probe.attacker,
                    ModAttributes.LIGHTNING_DAMAGE_ADDITIVE, 0.0);
            attributes.set(probe.attacker,
                    ModAttributes.FIRE_DAMAGE_ADDITIVE, 0.25);
            probe.attacker.setItemSlot(EquipmentSlot.MAINHAND, ruleStack(
                    "gametest_phase8_same_bucket",
                    DamagePhase.TYPE_SCALING,
                    List.of(
                            DamageNexusOperations.convertDamage(
                                    DamageChannel.PHYSICAL_ID,
                                    DamageChannel.FIRE_ID, 1.0f),
                            DamageNexusOperations.addChannelPreMultiplier(
                                    DamageChannel.FIRE_ID,
                                    DamageNexusPreMultiplierBuckets.FIRE_DAMAGE,
                                    0.10f))));
            assertResolved(submitPhase8(probe, DamageTypes.PLAYER_ATTACK,
                    probe.attacker, null, 4.0f), 5.4f,
                    "same-bucket additive stacking");

            attributes.set(probe.attacker,
                    ModAttributes.MELEE_DAMAGE_ADDITIVE, 0.20);
            assertResolved(submitPhase8(probe, DamageTypes.PLAYER_ATTACK,
                    probe.attacker, null, 4.0f), 6.48f,
                    "channel and category bucket multiplication");

            probe.clear();
            LivingEntity attributeParentTarget = phase8Target(probe);
            LivingEntity attributeChildTarget = phase8Target(probe);
            DamageResult[] attributeChild = new DamageResult[1];
            probe.settlementAction = callback -> {
                if (callback.snapshot().target() != attributeParentTarget) {
                    return;
                }
                probe.settlementAction = null;
                attributeChild[0] = DamageNexusApi.submitDamage(
                        DamageRequest.builder(
                                        probe.levelHelper.getLevel(),
                                        attributeChildTarget,
                                        DamageSourceDescriptor.of(
                                                DamageTypes.PLAYER_ATTACK
                                        ),
                                        4.0f
                                )
                                .logicalAttacker(probe.attacker)
                                .kind(DamageRequestKind.DOT)
                                .parent(callback.childAuthority().orElseThrow())
                                .build()
                );
            };
            DamageResult parent = DamageNexusApi.submitDamage(
                    DamageRequest.builder(
                                    probe.levelHelper.getLevel(),
                                    attributeParentTarget,
                                    DamageSourceDescriptor.of(
                                            DamageTypes.PLAYER_ATTACK
                                    ),
                                    4.0f
                            )
                            .logicalAttacker(probe.attacker)
                            .kind(DamageRequestKind.PRIMARY)
                            .build()
            );
            assertResolved(parent.settlement().orElseThrow(), 6.48f,
                    "parent attribute handoff");
            DamageResult child = Objects.requireNonNull(attributeChild[0]);
            assertResolved(child.settlement().orElseThrow(), 6.48f,
                    "child independent attribute handoff");

            attributes.set(probe.attacker,
                    ModAttributes.MELEE_DAMAGE_ADDITIVE, 0.0);
            attributes.set(probe.attacker,
                    ModAttributes.FIRE_DAMAGE_ADDITIVE, 1.0);
            probe.attacker.setItemSlot(EquipmentSlot.MAINHAND, ruleStack(
                    "gametest_phase8_true_damage",
                    DamagePhase.BASE_MODIFICATION,
                    List.of(DamageNexusOperations.addTrueDamage(
                            DamageChannel.FIRE_ID, 4.0f))));
            assertResolved(submitPhase8(probe, DamageTypes.ON_FIRE,
                    probe.attacker, null, 1.0f), 6.0f,
                    "true damage bypasses attribute pre-multipliers");

            zeroPhase8Offense(attributes, probe.attacker);
            probe.attacker.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            verifyPhase8Resistance(probe, attributes);

            attributes.set(probe.attacker,
                    ModAttributes.VULNERABLE_DAMAGE_ADDITIVE, 2.0);
            LivingEntity reservedTarget = phase8Target(probe);
            attributes.set(reservedTarget, ModAttributes.DODGE_CHANCE, 1.0);
            attributes.set(reservedTarget, ModAttributes.HEALING_RECEIVED, 0.0);
            assertResolved(submitPhase8(probe, reservedTarget,
                    DamageTypes.PLAYER_ATTACK, probe.attacker, null, 4.0f),
                    4.0f, "reserved attributes remain unconsumed");
        } finally {
            probe.attacker.setItemSlot(EquipmentSlot.MAINHAND, original);
        }
    }

    private static void verifyChannelAttribute(
            SettlementProbe probe,
            AttributeBaseScope attributes,
            ResourceKey<DamageType> damageType,
            Holder<Attribute> attribute
    ) {
        attributes.set(probe.attacker, attribute, 0.25);
        assertResolved(submitPhase8(probe, damageType,
                probe.attacker, null, 4.0f), 5.0f,
                "channel attribute " + attribute.getKey().identifier());
        attributes.set(probe.attacker, attribute, 0.0);
    }

    private static void verifyPhase8Resistance(
            SettlementProbe probe,
            AttributeBaseScope attributes
    ) {
        LivingEntity melee = phase8Target(probe);
        attributes.set(melee, ModAttributes.RESISTANCE_PHYSICAL, 25.0);
        attributes.set(melee, ModAttributes.RESISTANCE_MELEE, 25.0);
        assertResolved(submitPhase8(probe, melee, DamageTypes.PLAYER_ATTACK,
                probe.attacker, null, 6.0f), 3.0f,
                "channel plus melee resistance uses one formula");

        LivingEntity projectile = phase8Target(probe);
        attributes.set(projectile, ModAttributes.RESISTANCE_PROJECTILE, 50.0);
        assertResolved(submitPhase8(probe, projectile, DamageTypes.ARROW,
                probe.attacker, phase8Arrow(probe), 4.0f), 2.0f,
                "projectile category resistance");

        LivingEntity temporary = phase8Target(probe);
        attributes.set(temporary, ModAttributes.RESISTANCE_PHYSICAL, 25.0);
        attributes.set(temporary, ModAttributes.RESISTANCE_MELEE, 25.0);
        temporary.setItemSlot(EquipmentSlot.MAINHAND, ruleStack(
                "gametest_phase8_temp_physical_resistance",
                DamageRuleRole.DEFENSIVE,
                DamagePhase.MITIGATION_SETUP,
                List.of(DamageNexusOperations.addTemporaryResistance(
                        DamageChannel.PHYSICAL_ID, 25.0f))));
        assertResolved(submitPhase8(probe, temporary,
                DamageTypes.PLAYER_ATTACK, probe.attacker, null, 5.0f),
                2.0f, "temporary channel category resistance composition");

        probe.attacker.setItemSlot(EquipmentSlot.MAINHAND, ruleStack(
                "gametest_phase8_multichannel",
                DamagePhase.TYPE_SCALING,
                List.of(DamageNexusOperations.convertDamage(
                        DamageChannel.PHYSICAL_ID,
                        DamageChannel.FIRE_ID, 0.5f))));
        LivingEntity multi = phase8Target(probe);
        attributes.set(multi, ModAttributes.RESISTANCE_PHYSICAL, 25.0);
        attributes.set(multi, ModAttributes.RESISTANCE_FIRE, 75.0);
        attributes.set(multi, ModAttributes.RESISTANCE_MELEE, 25.0);
        assertResolved(submitPhase8(probe, multi,
                DamageTypes.PLAYER_ATTACK, probe.attacker, null, 4.0f),
                1.6666667f, "per-channel plus shared category resistance");
        probe.attacker.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);

        LivingEntity channelVulnerability = phase8Target(probe);
        channelVulnerability.setItemSlot(EquipmentSlot.MAINHAND, ruleStack(
                "gametest_channel_vulnerability",
                DamageRuleRole.DEFENSIVE,
                DamagePhase.MITIGATION_SETUP,
                List.of(DamageNexusOperations.addChannelMitigation(
                        DamageChannel.PHYSICAL_ID, -2.0f))));
        assertResolved(submitPhase8(probe, channelVulnerability,
                DamageTypes.PLAYER_ATTACK, probe.attacker, null, 4.0f),
                12.0f, "channel mitigation vulnerability below negative one");

        LivingEntity channelFullReduction = phase8Target(probe);
        channelFullReduction.setItemSlot(EquipmentSlot.MAINHAND, ruleStack(
                "gametest_channel_full_reduction",
                DamageRuleRole.DEFENSIVE,
                DamagePhase.MITIGATION_SETUP,
                List.of(DamageNexusOperations.addChannelMitigation(
                        DamageChannel.PHYSICAL_ID, 2.0f))));
        assertPhase8ZeroDamage(probe, channelFullReduction,
                DamageTypes.PLAYER_ATTACK, probe.attacker, null, 4.0f,
                "channel mitigation positive upper bound");

        LivingEntity globalVulnerability = phase8Target(probe);
        globalVulnerability.setItemSlot(EquipmentSlot.MAINHAND, ruleStack(
                "gametest_global_vulnerability",
                DamageRuleRole.DEFENSIVE,
                DamagePhase.MITIGATION_SETUP,
                List.of(DamageNexusOperations.addGlobalMitigation(-2.0f))));
        assertResolved(submitPhase8(probe, globalVulnerability,
                DamageTypes.PLAYER_ATTACK, probe.attacker, null, 4.0f),
                12.0f, "global mitigation vulnerability below negative one");

        LivingEntity globalFullReduction = phase8Target(probe);
        globalFullReduction.setItemSlot(EquipmentSlot.MAINHAND, ruleStack(
                "gametest_global_full_reduction",
                DamageRuleRole.DEFENSIVE,
                DamagePhase.MITIGATION_SETUP,
                List.of(DamageNexusOperations.addGlobalMitigation(2.0f))));
        assertPhase8ZeroDamage(probe, globalFullReduction,
                DamageTypes.PLAYER_ATTACK, probe.attacker, null, 4.0f,
                "global mitigation positive upper bound");

        float resistanceK = Math.max(
                0.0001f,
                DamageNexusConfig.current().formulas().resistanceKValue()
        );
        for (float rating : List.of(0.0f, -25.0f, -50.0f, -100.0f, -200.0f)) {
            LivingEntity negative = phase8Target(probe);
            attributes.set(negative, ModAttributes.RESISTANCE_MELEE, rating);
            float reduction = rating / resistanceK;
            assertResolved(submitPhase8(probe, negative,
                    DamageTypes.PLAYER_ATTACK, probe.attacker, null, 4.0f),
                    4.0f * (1.0f - reduction),
                    "negative category resistance " + rating);
        }

        verifyConvertGainVulnerability(probe, attributes, resistanceK);

        LivingEntity bypass = phase8Target(probe);
        attributes.set(bypass, ModAttributes.RESISTANCE_MELEE, 1000.0);
        probe.clear();
        float before = bypass.getHealth();
        if (!bypass.hurtServer(probe.levelHelper.getLevel(),
                bypass.damageSources().fellOutOfWorld(), 4.0f)
                || !close(before - bypass.getHealth(), 4.0f)
                || !probe.snapshots.isEmpty()) {
            throw new AssertionError(
                    "BYPASSES_RESISTANCE hard-vanilla source changed semantics");
        }
    }

    private static void verifyConvertGainVulnerability(
            SettlementProbe probe,
            AttributeBaseScope attributes,
            float resistanceK
    ) {
        ItemStack original = probe.attacker.getMainHandItem().copy();
        try {
            probe.attacker.setItemSlot(
                    EquipmentSlot.MAINHAND,
                    TestItemFactory.convertGainOpsItem()
            );

            for (float rating : List.of(0.0f, -50.0f, -100.0f, -200.0f)) {
                LivingEntity target = phase8Target(probe);
                attributes.set(
                        target,
                        ModAttributes.RESISTANCE_LIGHTNING,
                        rating
                );
                float lightningMultiplier = 1.0f - rating / resistanceK;
                assertResolved(submitPhase8(probe, target,
                        DamageTypes.PLAYER_ATTACK, probe.attacker, null, 4.0f),
                        4.0f + lightningMultiplier,
                        "convert/gain lightning vulnerability " + rating);
            }

            for (float rating : List.of(0.0f, -50.0f, -100.0f, -200.0f)) {
                LivingEntity target = phase8Target(probe);
                attributes.set(
                        target,
                        ModAttributes.RESISTANCE_LIGHTNING,
                        rating
                );
                VanillaCritHandler.onVanillaCriticalHit(new CriticalHitEvent(
                        probe.attacker, target, 1.5f, true));
                float lightningMultiplier = 1.0f - rating / resistanceK;
                assertResolved(submitPhase8(probe, target,
                        DamageTypes.PLAYER_ATTACK, probe.attacker, null, 4.0f),
                        6.0f + lightningMultiplier,
                        "critical convert/gain lightning vulnerability " + rating);
            }
        } finally {
            VanillaCritHandler.clear();
            probe.attacker.setItemSlot(EquipmentSlot.MAINHAND, original);
        }
    }

    private static void verifyResolverAuthoritativeProjectileCategory(
            SettlementProbe probe
    ) {
        try (AttributeBaseScope attributes = new AttributeBaseScope()) {
            zeroPhase8Offense(attributes, probe.attacker);
            attributes.set(probe.attacker,
                    ModAttributes.PROJECTILE_DAMAGE_ADDITIVE, 0.25);
            attributes.set(probe.attacker,
                    ModAttributes.MELEE_DAMAGE_ADDITIVE, 0.50);
            probe.proxyDirectEntity = probe.spawn();
            probe.proxyResolvedDirectEntity = phase8Arrow(probe);
            LivingEntity target = phase8Target(probe);
            attributes.set(target, ModAttributes.RESISTANCE_PROJECTILE, 50.0);
            probe.clear();
            if (!target.hurtServer(probe.levelHelper.getLevel(),
                    target.damageSources().mobAttack(probe.proxyDirectEntity),
                    4.0f) || probe.snapshots.size() != 1) {
                throw new AssertionError(
                        "Resolver-authoritative projectile did not settle once");
            }
            DamageSettlementSnapshot snapshot = probe.snapshots.getFirst();
            assertResolved(snapshot, 2.5f,
                    "resolver final direct projectile classification");
            if (snapshot.directEntity() != probe.proxyResolvedDirectEntity) {
                throw new AssertionError(
                        "Settlement did not preserve authoritative projectile direct entity");
            }
        } finally {
            probe.proxyResolvedDirectEntity = null;
            probe.proxyDirectEntity = null;
        }
    }

    private static void verifyPhase10StaticTemplates(SettlementProbe probe) {
        if (!io.github.naimjeg.damagenexus.api.item.template
                .DamageNexusTemplates.serverExecutionReady()) {
            throw new AssertionError(
                    "Server template registry was not channel-validated before GameTest execution");
        }
        ItemStack original = probe.attacker.getMainHandItem().copy();
        boolean previousSources = probe.externalSourcesActive;
        ItemStack previousExternal = probe.externalOffensiveStack;
        ItemStack previousDefensive = probe.externalDefensiveStack;
        try (AttributeBaseScope attributes = new AttributeBaseScope()) {
            zeroPhase8Offense(attributes, probe.attacker);

            ItemStack apiContract = templateReferenceStack(
                    List.of(new DamageEntryTemplateReference(
                            id("gametest_api_contract_unresolved"))),
                    List.of());
            if (!DamageNexusItemApi.hasAny(apiContract)
                    || !DamageNexusItemApi.get(apiContract).isEmpty()
                    || !DamageNexusItemApi.getMaterializedEntries(
                    apiContract).isEmpty()
                    || !DamageNexusItemApi.getResolvedMaterializedEntries(
                    apiContract).isEmpty()) {
                throw new AssertionError(
                        "Item API confused template references with materialized definitions");
            }
            DamageItemTemplateReferences apiReferences =
                    DamageNexusItemApi.getTemplateReferences(apiContract);
            if (!DamageNexusItemApi.set(
                    apiContract, DamageNexusItemEntries.EMPTY)
                    || !apiReferences.equals(DamageNexusItemApi
                    .getTemplateReferences(apiContract))) {
                throw new AssertionError(
                        "Materialized item set() changed template references");
            }
            if (!DamageNexusItemApi.clear(apiContract)
                    || DamageNexusItemApi.hasAny(apiContract)) {
                throw new AssertionError(
                        "Item clear() did not clear all executable components");
            }

            Identifier missingChannel = id("gametest_missing_authored_channel");
            ItemStack unknownMaterialized = new ItemStack(Items.STONE);
            unknownMaterialized.set(
                    ModDataComponents.DAMAGE_ENTRIES.get(),
                    List.of(staticEntryTemplateChannel(
                            id("gametest_unknown_materialized"),
                            missingChannel,
                            9.0f
                    ))
            );
            probe.attacker.setItemSlot(
                    EquipmentSlot.MAINHAND,
                    unknownMaterialized
            );
            assertResolved(submitPhase8(
                    probe,
                    DamageTypes.PLAYER_ATTACK,
                    probe.attacker,
                    null,
                    4.0f
            ), 4.0f, "unknown materialized channel fails closed");
            if (io.github.naimjeg.damagenexus.core.security
                    .DamageNexusItemSecurity.evaluateCreativeInbound(
                            true,
                            new DamageNexusItemEntries(
                                    List.of(staticEntryTemplateChannel(
                                            id("gametest_admin_unknown"),
                                            missingChannel,
                                            9.0f
                                    )),
                                    List.of()
                            )
                    ) != io.github.naimjeg.damagenexus.core.security
                    .DamageNexusItemSecurity.InboundDecision.STRIP_INVALID) {
                throw new AssertionError(
                        "Administrator ingress accepted an unknown authored channel"
                );
            }

            ItemStack knownMaterialized = new ItemStack(Items.STONE);
            knownMaterialized.set(
                    ModDataComponents.DAMAGE_ENTRIES.get(),
                    List.of(staticEntryTemplateChannel(
                            id("gametest_known_materialized"),
                            DamageChannel.FIRE_ID,
                            2.0f
                    ))
            );
            probe.attacker.setItemSlot(
                    EquipmentSlot.MAINHAND,
                    knownMaterialized
            );
            assertResolved(submitPhase8(
                    probe,
                    DamageTypes.PLAYER_ATTACK,
                    probe.attacker,
                    null,
                    4.0f
            ), 6.0f, "known materialized channel remains executable");

            ItemStack nestedUnknown = new ItemStack(Items.STONE);
            DamageEntryDefinition invalidNested =
                    staticEntryTemplateChannel(
                            id("gametest_nested_unknown_entry"),
                            missingChannel,
                            9.0f
                    );
            nestedUnknown.set(
                    ModDataComponents.DAMAGE_AFFIXES.get(),
                    List.of(new DamageAffixDefinition(
                            id("gametest_nested_unknown_affix"),
                            DamageAffixDisplay.EMPTY,
                            DamageAffixSlot.ITEM,
                            DamageAffixRarity.COMMON,
                            List.of(invalidNested),
                            DamageAffixStacking.STACK,
                            Optional.empty()
                    ))
            );
            probe.attacker.setItemSlot(
                    EquipmentSlot.MAINHAND,
                    nestedUnknown
            );
            assertResolved(submitPhase8(
                    probe,
                    DamageTypes.PLAYER_ATTACK,
                    probe.attacker,
                    null,
                    4.0f
            ), 4.0f, "affix nested unknown channel fails closed");

            ItemStack javaEntry = templateReferenceStack(
                    List.of(new DamageEntryTemplateReference(
                            GAMETEST_ENTRY_TEMPLATE_ID)),
                    List.of());
            probe.attacker.setItemSlot(EquipmentSlot.MAINHAND, javaEntry);
            assertResolved(submitPhase8(probe, DamageTypes.PLAYER_ATTACK,
                    probe.attacker, null, 4.0f), 6.0f,
                    "Java entry template reference");

            ItemStack javaAffix = templateReferenceStack(
                    List.of(),
                    List.of(new DamageAffixTemplateReference(
                            GAMETEST_AFFIX_TEMPLATE_ID)));
            probe.attacker.setItemSlot(EquipmentSlot.MAINHAND, javaAffix);
            assertResolved(submitPhase8(probe, DamageTypes.PLAYER_ATTACK,
                    probe.attacker, null, 4.0f), 7.0f,
                    "Java affix template nested entry");

            ItemStack merged = templateReferenceStack(
                    List.of(new DamageEntryTemplateReference(
                            GAMETEST_ENTRY_TEMPLATE_ID)),
                    List.of());
            merged.set(ModDataComponents.DAMAGE_ENTRIES.get(), List.of(
                    staticEntryTemplate(id("gametest_materialized_before_template"),
                            1.0f)));
            probe.attacker.setItemSlot(EquipmentSlot.MAINHAND, merged);
            assertResolved(submitPhase8(probe, DamageTypes.PLAYER_ATTACK,
                    probe.attacker, null, 4.0f), 7.0f,
                    "materialized definitions before references");

            ItemStack duplicate = templateReferenceStack(
                    List.of(
                            new DamageEntryTemplateReference(
                                    GAMETEST_ENTRY_TEMPLATE_ID),
                            new DamageEntryTemplateReference(
                                    GAMETEST_ENTRY_TEMPLATE_ID)),
                    List.of());
            probe.attacker.setItemSlot(EquipmentSlot.MAINHAND, duplicate);
            assertResolved(submitPhase8(probe, DamageTypes.PLAYER_ATTACK,
                    probe.attacker, null, 4.0f), 8.0f,
                    "duplicate template references retain stacking semantics");

            ItemStack unresolved = templateReferenceStack(
                    List.of(new DamageEntryTemplateReference(
                            id("gametest_missing_template"))),
                    List.of());
            probe.attacker.setItemSlot(EquipmentSlot.MAINHAND, unresolved);
            assertResolved(submitPhase8(probe, DamageTypes.PLAYER_ATTACK,
                    probe.attacker, null, 4.0f), 4.0f,
                    "unresolved template reference is inert");

            probe.attacker.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            probe.externalSourcesActive = true;
            probe.externalOffensiveStack = javaEntry;
            probe.externalDefensiveStack = ItemStack.EMPTY;
            probe.externalCategory = EquippedItemRuleSourceCategory.ITEM;
            probe.mutateExternalAfterPreferred = false;
            probe.externalDistinctPhysical = false;
            assertResolved(submitPhase8(probe, DamageTypes.PLAYER_ATTACK,
                    probe.attacker, null, 4.0f), 6.0f,
                    "external equipped source template reference");
            probe.externalSourcesActive = false;

            DamageEntryDefinition initial = staticEntryTemplate(
                    GAMETEST_DATAPACK_TEMPLATE_ID, 1.0f);
            if (!DatapackDamageTemplateReloadListener.applyPreparedForTesting(
                    Map.of(initial.id(), initial), Map.of())) {
                throw new AssertionError(
                        "GameTest datapack template snapshot was rejected");
            }
            ItemStack reloadable = templateReferenceStack(
                    List.of(new DamageEntryTemplateReference(
                            GAMETEST_DATAPACK_TEMPLATE_ID)),
                    List.of());
            probe.attacker.setItemSlot(EquipmentSlot.MAINHAND, reloadable);
            assertResolved(submitPhase8(probe, DamageTypes.PLAYER_ATTACK,
                    probe.attacker, null, 4.0f), 5.0f,
                    "datapack static template reference");

            DamageEntryDefinition reloaded = staticEntryTemplate(
                    GAMETEST_DATAPACK_TEMPLATE_ID, 4.0f);
            if (!DatapackDamageTemplateReloadListener.applyPreparedForTesting(
                    Map.of(reloaded.id(), reloaded), Map.of())) {
                throw new AssertionError(
                        "GameTest template replacement was rejected");
            }
            assertResolved(submitPhase8(probe, DamageTypes.PLAYER_ATTACK,
                    probe.attacker, null, 4.0f), 8.0f,
                    "same ItemStack observes successful template reload");

            long revisionBeforeFailure =
                    io.github.naimjeg.damagenexus.api.item.template
                            .DamageNexusTemplates.revision();
            DamageEntryDefinition mismatched = staticEntryTemplate(
                    id("gametest_mismatched_definition"), 20.0f);
            if (DatapackDamageTemplateReloadListener.applyPreparedForTesting(
                    Map.of(GAMETEST_DATAPACK_TEMPLATE_ID, mismatched),
                    Map.of())) {
                throw new AssertionError(
                        "Mismatched template reload unexpectedly published");
            }
            if (io.github.naimjeg.damagenexus.api.item.template
                    .DamageNexusTemplates.revision() != revisionBeforeFailure) {
                throw new AssertionError(
                        "Failed template reload advanced the revision");
            }
            assertResolved(submitPhase8(probe, DamageTypes.PLAYER_ATTACK,
                    probe.attacker, null, 4.0f), 8.0f,
                    "failed reload retains prior template definition");
        } finally {
            DatapackDamageTemplateReloadListener.applyPreparedForTesting(
                    Map.of(), Map.of());
            probe.externalSourcesActive = previousSources;
            probe.externalOffensiveStack = previousExternal;
            probe.externalDefensiveStack = previousDefensive;
            probe.mutateExternalAfterPreferred = false;
            probe.externalDistinctPhysical = false;
            probe.attacker.setItemSlot(EquipmentSlot.MAINHAND, original);
        }
        verifyCleanup(probe);
    }

    private static ItemStack templateReferenceStack(
            List<DamageEntryTemplateReference> entries,
            List<DamageAffixTemplateReference> affixes
    ) {
        ItemStack stack = new ItemStack(Items.STONE);
        if (!DamageNexusItemApi.setTemplateReferences(
                stack, new DamageItemTemplateReferences(entries, affixes))) {
            throw new AssertionError("Unable to attach template references");
        }
        return stack;
    }

    private static DamageEntryDefinition staticEntryTemplate(
            Identifier templateId,
            float amount
    ) {
        return new DamageEntryDefinition(
                templateId,
                DamageEntryDisplay.EMPTY,
                DamageEntrySlot.ITEM,
                List.of(new DamageRuleDefinition(
                        Identifier.fromNamespaceAndPath(
                                templateId.getNamespace(),
                                templateId.getPath() + "_rule"),
                        DamageRuleRole.OFFENSIVE,
                        DamagePhase.BASE_MODIFICATION,
                        500,
                        List.of(),
                        List.of(DamageNexusOperations.addBaseDamage(
                                DamageChannel.UNTYPED_ID, amount)),
                        DamageRuleStacking.STACK,
                        Optional.empty(),
                        Optional.empty())),
                DamageEntryStacking.STACK,
                Optional.empty());
    }

    private static DamageEntryDefinition staticEntryTemplateChannel(
            Identifier templateId,
            Identifier channelId,
            float amount
    ) {
        return new DamageEntryDefinition(
                templateId,
                DamageEntryDisplay.EMPTY,
                DamageEntrySlot.ITEM,
                List.of(new DamageRuleDefinition(
                        Identifier.fromNamespaceAndPath(
                                templateId.getNamespace(),
                                templateId.getPath() + "_rule"
                        ),
                        DamageRuleRole.OFFENSIVE,
                        DamagePhase.BASE_MODIFICATION,
                        500,
                        List.of(),
                        List.of(DamageNexusOperations.addBaseDamage(
                                channelId,
                                amount
                        )),
                        DamageRuleStacking.STACK,
                        Optional.empty(),
                        Optional.empty()
                )),
                DamageEntryStacking.STACK,
                Optional.empty()
        );
    }

    private static DamageAffixDefinition staticAffixTemplate(
            Identifier templateId,
            float amount
    ) {
        Identifier nestedId = Identifier.fromNamespaceAndPath(
                templateId.getNamespace(), templateId.getPath() + "_entry");
        return new DamageAffixDefinition(
                templateId,
                DamageAffixDisplay.EMPTY,
                DamageAffixSlot.ITEM,
                DamageAffixRarity.COMMON,
                List.of(staticEntryTemplate(nestedId, amount)),
                DamageAffixStacking.STACK,
                Optional.empty());
    }

    private static void zeroPhase8Offense(
            AttributeBaseScope attributes,
            LivingEntity attacker
    ) {
        for (Holder<Attribute> attribute : List.of(
                ModAttributes.CRIT_CHANCE,
                ModAttributes.CRIT_DAMAGE_ADDITIVE,
                ModAttributes.FIRE_DAMAGE_ADDITIVE,
                ModAttributes.COLD_DAMAGE_ADDITIVE,
                ModAttributes.LIGHTNING_DAMAGE_ADDITIVE,
                ModAttributes.MAGIC_DAMAGE_ADDITIVE,
                ModAttributes.WITHER_DAMAGE_ADDITIVE,
                ModAttributes.POISON_DAMAGE_ADDITIVE,
                ModAttributes.KINETIC_DAMAGE_ADDITIVE,
                ModAttributes.MELEE_DAMAGE_ADDITIVE,
                ModAttributes.PROJECTILE_DAMAGE_ADDITIVE)) {
            attributes.set(attacker, attribute, 0.0);
        }
    }

    private static DamageSettlementSnapshot submitPhase8(
            SettlementProbe probe,
            ResourceKey<DamageType> type,
            LivingEntity attacker,
            Entity direct,
            float amount
    ) {
        return submitPhase8(probe, phase8Target(probe), type,
                attacker, direct, amount);
    }

    private static DamageSettlementSnapshot submitPhase8(
            SettlementProbe probe,
            LivingEntity target,
            ResourceKey<DamageType> type,
            LivingEntity attacker,
            Entity direct,
            float amount
    ) {
        return submitPhase8Result(probe, target, type, attacker, direct,
                DamageRequestKind.PRIMARY, amount)
                .settlement().orElseThrow();
    }

    private static void assertPhase8ZeroDamage(
            SettlementProbe probe,
            LivingEntity target,
            ResourceKey<DamageType> type,
            LivingEntity attacker,
            Entity direct,
            float amount,
            String scenario
    ) {
        probe.clear();
        DamageRequest.Builder builder = DamageRequest.builder(
                probe.levelHelper.getLevel(), target,
                DamageSourceDescriptor.of(type), amount
        );
        if (attacker != null) {
            builder.logicalAttacker(attacker);
        }
        if (direct != null) {
            builder.directEntity(direct);
        }
        DamageResult result = DamageNexusApi.submitDamage(builder.build());
        assertStatus(
                result,
                DamageSubmissionStatus.NOT_APPLIED,
                DamageFailureReason.ZERO_DAMAGE
        );
        if (probe.snapshots.size() != 1
                || probe.snapshots.getFirst().resolvedDamage() != 0.0f) {
            throw new AssertionError(
                    scenario + " did not reduce managed damage to zero"
            );
        }
    }

    private static DamageResult submitPhase8Result(
            SettlementProbe probe,
            ResourceKey<DamageType> type,
            LivingEntity attacker,
            Entity direct,
            DamageRequestKind kind,
            float amount
    ) {
        return submitPhase8Result(probe, phase8Target(probe), type,
                attacker, direct, kind, amount);
    }

    private static DamageResult submitPhase8Result(
            SettlementProbe probe,
            LivingEntity target,
            ResourceKey<DamageType> type,
            LivingEntity attacker,
            Entity direct,
            DamageRequestKind kind,
            float amount
    ) {
        probe.clear();
        DamageRequest.Builder builder = DamageRequest.builder(
                probe.levelHelper.getLevel(), target,
                DamageSourceDescriptor.of(type), amount).kind(kind);
        if (attacker != null) {
            builder.logicalAttacker(attacker);
        }
        if (direct != null) {
            builder.directEntity(direct);
        }
        DamageResult result = DamageNexusApi.submitDamage(builder.build());
        if (result.status() != DamageSubmissionStatus.APPLIED) {
            throw new AssertionError("Phase 8 request expected APPLIED but got "
                    + result.status() + " failure=" + result.failure());
        }
        if (probe.snapshots.size() != 1) {
            throw new AssertionError(
                    "Phase 8 request did not publish exactly one settlement");
        }
        return result;
    }

    private static LivingEntity phase8Target(SettlementProbe probe) {
        LivingEntity target = probe.spawn();
        target.setAbsorptionAmount(0.0f);
        AttributeInstance armor = target.getAttribute(Attributes.ARMOR);
        if (armor != null) {
            armor.setBaseValue(0.0);
        }
        return target;
    }

    private static Arrow phase8Arrow(SettlementProbe probe) {
        Arrow arrow = probe.levelHelper.spawn(EntityType.ARROW,
                new BlockPos(probe.nextSpawnX++, 2, 1));
        arrow.setOwner(probe.attacker);
        return arrow;
    }

    private static void assertResolved(
            DamageSettlementSnapshot snapshot,
            float expected,
            String scenario
    ) {
        if (!close(snapshot.resolvedDamage(), expected)) {
            throw new AssertionError(scenario + " expected=" + expected
                    + " actual=" + snapshot.resolvedDamage());
        }
    }

    private static void assertPlayerAttackCharged(ServerPlayer player) {
        if (player.getAttackStrengthScale(0.5f) < 0.999f) {
            throw new AssertionError("Unable to charge real player attack entry");
        }
    }

    private static void invokeRealArrowHit(
            AbstractArrow arrow,
            LivingEntity target
    ) {
        try {
            Method method = AbstractArrow.class.getDeclaredMethod(
                    "onHitEntity", EntityHitResult.class);
            method.setAccessible(true);
            method.invoke(arrow, new EntityHitResult(target));
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(
                    "Unable to invoke real projectile hit entry", exception);
        }
    }

    private static void verifyProjectileCriticalDecision(
            SettlementProbe probe,
            boolean suppress,
            boolean forceNonCritical,
            float expectedResolved
    ) {
        probe.clear();
        LivingEntity target = criticalTarget(probe);
        Arrow arrow = probe.levelHelper.spawn(
                EntityType.ARROW, new BlockPos(probe.nextSpawnX++, 2, 1));
        arrow.setOwner(probe.attacker);
        DamageSource source = target.damageSources().arrow(arrow, probe.attacker);
        if (suppress) probe.criticalSuppressTarget = target;
        if (forceNonCritical) probe.criticalForceTarget = target;
        boolean capturedCritical = !forceNonCritical;
        int post = capturedCritical ? 4 : 2;
        ProjectileDamageCapture.capture(
                arrow, target, source, ItemStack.EMPTY,
                2.0f, 2.0f, 2, post, capturedCritical);
        try {
            if (!target.hurtServer(probe.levelHelper.getLevel(), source, post)) {
                throw new AssertionError("Projectile critical fixture was rejected");
            }
        } finally {
            ProjectileDamageCapture.clear();
        }
        DamageSettlementSnapshot snapshot = probe.snapshots.getFirst();
        if (suppress) {
            assertCriticalDecision(snapshot, CriticalDecision.SUPPRESS_CRITICAL,
                    CriticalDecisionOutcome.SUPPRESSED, false, false);
        } else if (forceNonCritical) {
            assertCriticalDecision(snapshot, CriticalDecision.FORCE_CRITICAL,
                    CriticalDecisionOutcome.FORCED, true, false);
        } else {
            assertCriticalDecision(snapshot, CriticalDecision.DEFAULT,
                    CriticalDecisionOutcome.VANILLA_PROJECTILE, true, false);
        }
        assertClose(snapshot.resolvedDamage(), expectedResolved,
                "projectile critical formula");
        probe.criticalForceTarget = null;
        probe.criticalSuppressTarget = null;
    }

    private static LivingEntity criticalTarget(SettlementProbe probe) {
        LivingEntity target = probe.spawn();
        var armor = target.getAttribute(Attributes.ARMOR);
        var toughness = target.getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (armor != null) armor.setBaseValue(0.0D);
        if (toughness != null) toughness.setBaseValue(0.0D);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            target.setItemSlot(slot, ItemStack.EMPTY);
        }
        return target;
    }

    private static void assertClose(float actual, float expected, String label) {
        if (Math.abs(actual - expected) > 0.02f) {
            throw new AssertionError(label + ": expected=" + expected
                    + " actual=" + actual);
        }
    }

    private static void assertCriticalDecision(
            DamageSettlementSnapshot snapshot,
            CriticalDecision decision,
            CriticalDecisionOutcome outcome,
            boolean critical,
            boolean sampled
    ) {
        CriticalDecisionSnapshot criticalSnapshot = snapshot.criticalDecision();
        if (!criticalSnapshot.frozen()
                || criticalSnapshot.effectiveDecision() != decision
                || criticalSnapshot.outcome() != outcome
                || criticalSnapshot.critical() != critical
                || snapshot.critical() != critical
                || criticalSnapshot.chanceSampled() != sampled
                || criticalSnapshot.effectApplied() != critical) {
            throw new AssertionError("Unexpected critical snapshot: " + criticalSnapshot);
        }
        try {
            criticalSnapshot.contributions().clear();
            throw new AssertionError("Critical contribution snapshot is mutable");
        } catch (UnsupportedOperationException expected) {
            // Expected immutable public view.
        }
    }

    private static final class MobEffectTagBindingScope implements AutoCloseable {
        private final MappedRegistry<MobEffect> registry;
        private final TagKey<MobEffect> testTag;
        private final Map<TagKey<MobEffect>, List<Holder<MobEffect>>> original;
        private final Map<TagKey<MobEffect>, List<Holder<MobEffect>>> baseline;
        private final Map.Entry<TagKey<MobEffect>, List<Holder<MobEffect>>> sentinel;
        private boolean closed;

        private MobEffectTagBindingScope(
                MappedRegistry<MobEffect> registry,
                TagKey<MobEffect> testTag
        ) {
            this.registry = registry;
            this.testTag = testTag;
            this.original = copyBindings(registry);
            Map.Entry<TagKey<MobEffect>, List<Holder<MobEffect>>> existing =
                    original.entrySet().stream().findFirst().orElse(null);
            if (existing == null) {
                TagKey<MobEffect> fixtureSentinel = TagKey.create(
                        Registries.MOB_EFFECT,
                        id("gametest_effect_tag_sentinel"));
                List<Holder<MobEffect>> members = List.of(MobEffects.SPEED);
                Map<TagKey<MobEffect>, List<Holder<MobEffect>>> withSentinel =
                        new LinkedHashMap<>(original);
                withSentinel.put(fixtureSentinel, members);
                this.baseline = Map.copyOf(withSentinel);
                this.sentinel = Map.entry(fixtureSentinel, members);
                apply(this.baseline);
            } else {
                this.baseline = original;
                this.sentinel = existing;
            }
        }

        @SuppressWarnings("unchecked")
        static MobEffectTagBindingScope open(
                SettlementProbe probe,
                TagKey<MobEffect> testTag
        ) {
            Registry<MobEffect> registry = probe.levelHelper.getLevel()
                    .registryAccess().lookupOrThrow(Registries.MOB_EFFECT);
            if (!(registry instanceof MappedRegistry<?> raw)) {
                throw new AssertionError("GameTest MobEffect registry is not reloadable");
            }
            return new MobEffectTagBindingScope(
                    (MappedRegistry<MobEffect>) raw, testTag);
        }

        void bind(List<Holder<MobEffect>> values) {
            if (closed) throw new IllegalStateException("MobEffect tag scope is closed");
            Map<TagKey<MobEffect>, List<Holder<MobEffect>>> merged =
                    new LinkedHashMap<>(baseline);
            merged.put(testTag, List.copyOf(values));
            apply(merged);
        }

        @Override
        public void close() {
            if (closed) return;
            try {
                List<Holder<MobEffect>> preserved = copyBindings(registry)
                        .get(sentinel.getKey());
                if (!sentinel.getValue().equals(preserved)) {
                    throw new AssertionError("MobEffect sentinel tag was changed");
                }
                apply(original);
                if (!original.equals(copyBindings(registry))) {
                    throw new AssertionError(
                            "Complete MobEffect tag binding was not restored");
                }
            } finally {
                closed = true;
            }
        }

        private void apply(Map<TagKey<MobEffect>, List<Holder<MobEffect>>> bindings) {
            registry.prepareTagReload(new TagLoader.LoadResult<>(
                    registry.key(), bindings)).apply();
        }

        private static Map<TagKey<MobEffect>, List<Holder<MobEffect>>> copyBindings(
                Registry<MobEffect> registry
        ) {
            Map<TagKey<MobEffect>, List<Holder<MobEffect>>> copy =
                    new LinkedHashMap<>();
            registry.listTags().forEach(named ->
                    copy.put(named.key(), named.stream().toList()));
            return Map.copyOf(copy);
        }
    }

    private static void verifyExternalItemSources(SettlementProbe probe) {
        probe.clear();
        probe.externalSourcesActive = true;
        probe.externalOffensiveQueries = 0;
        probe.externalDefensiveQueries = 0;
        probe.externalOffensiveRuleInvocations = 0;
        probe.externalDefensiveRuleInvocations = 0;
        probe.externalCategory = EquippedItemRuleSourceCategory.ITEM;
        probe.externalOffensiveStack = ruleStack(
                "gametest_external_offensive",
                DamageRuleRole.OFFENSIVE,
                DamagePhase.BASE_MODIFICATION,
                List.of(new Phase5CountingOperation(
                        Phase5Counter.EXTERNAL_OFFENSIVE
                ))
        );
        probe.externalDefensiveStack = ruleStack(
                "gametest_external_defensive",
                DamageRuleRole.DEFENSIVE,
                DamagePhase.BASE_MODIFICATION,
                List.of(new Phase5CountingOperation(
                        Phase5Counter.EXTERNAL_DEFENSIVE
                ))
        );
        probe.mutateExternalAfterPreferred = true;
        try {
            DamageResult result = DamageNexusApi.submitDamage(request(
                    probe.levelHelper,
                    probe.spawn(),
                    probe.attacker,
                    DamageRequestKind.PRIMARY,
                    1.0f
            ).build());
            assertStatus(result, DamageSubmissionStatus.APPLIED, null);
            if (probe.externalOffensiveQueries != 1
                    || probe.externalDefensiveQueries != 1
                    || probe.externalOffensiveRuleInvocations != 1
                    || probe.externalDefensiveRuleInvocations != 1) {
                throw new AssertionError(
                        "External item sources were re-enumerated, duplicated, or not snapshotted"
                );
            }

            verifyExternalReadModes(probe);

            probe.clear();
            LivingEntity self = probe.spawn();
            ItemStack shared = ruleStack(
                    "gametest_external_self",
                    DamageRuleRole.ANY,
                    DamagePhase.BASE_MODIFICATION,
                    List.of(new Phase5CountingOperation(
                            Phase5Counter.EXTERNAL_SELF
                    ))
            );
            probe.externalSelfRuleInvocations = 0;
            probe.externalOffensiveStack = shared;
            probe.externalDefensiveStack = shared;
            probe.mutateExternalAfterPreferred = false;
            DamageResult selfResult = DamageNexusApi.submitDamage(request(
                    probe.levelHelper,
                    self,
                    self,
                    DamageRequestKind.PRIMARY,
                    1.0f
            ).equipmentOwner(self).build());
            if (selfResult.status() != DamageSubmissionStatus.APPLIED
                    && selfResult.status() != DamageSubmissionStatus.NOT_APPLIED) {
                throw new AssertionError("Self damage did not settle");
            }
            if (probe.externalSelfRuleInvocations != 2) {
                throw new AssertionError(
                        "Self-damage offensive and defensive sources were incorrectly deduplicated"
                );
            }

            probe.clear();
            probe.externalDistinctPhysical = true;
            probe.externalOffensiveRuleInvocations = 0;
            probe.externalOffensiveStack = ruleStack(
                    "gametest_external_distinct",
                    DamageRuleRole.OFFENSIVE,
                    DamagePhase.BASE_MODIFICATION,
                    List.of(new Phase5CountingOperation(
                            Phase5Counter.EXTERNAL_OFFENSIVE
                    ))
            );
            probe.externalSecondStack = probe.externalOffensiveStack.copy();
            probe.externalDefensiveStack = ItemStack.EMPTY;
            DamageResult distinct = DamageNexusApi.submitDamage(request(
                    probe.levelHelper,
                    probe.spawn(),
                    probe.attacker,
                    DamageRequestKind.PRIMARY,
                    1.0f
            ).build());
            assertStatus(distinct, DamageSubmissionStatus.APPLIED, null);
            if (probe.externalOffensiveRuleInvocations != 2) {
                throw new AssertionError(
                        "Distinct physical sources with equal stacks were merged"
                );
            }
        } finally {
            probe.externalSourcesActive = false;
            probe.externalOffensiveStack = ItemStack.EMPTY;
            probe.externalDefensiveStack = ItemStack.EMPTY;
            probe.mutateExternalAfterPreferred = false;
            probe.externalDistinctPhysical = false;
            probe.externalSecondStack = ItemStack.EMPTY;
            probe.externalReadEntries = true;
            probe.externalReadAffixes = true;
        }
        verifyCleanup(probe);
    }

    private static void verifyExternalReadModes(SettlementProbe probe) {
        probe.externalOffensiveStack = entryAndAffixRuleStack();
        probe.externalDefensiveStack = ItemStack.EMPTY;
        probe.mutateExternalAfterPreferred = false;

        assertExternalReadMode(probe, true, false, 1, 0);
        assertExternalReadMode(probe, false, true, 0, 1);
        assertExternalReadMode(probe, true, true, 1, 1);
    }

    private static void assertExternalReadMode(
            SettlementProbe probe,
            boolean readEntries,
            boolean readAffixes,
            int expectedEntries,
            int expectedAffixes
    ) {
        probe.clear();
        probe.externalReadEntries = readEntries;
        probe.externalReadAffixes = readAffixes;
        probe.externalEntryModeInvocations = 0;
        probe.externalAffixModeInvocations = 0;
        DamageResult result = DamageNexusApi.submitDamage(request(
                probe.levelHelper,
                probe.spawn(),
                probe.attacker,
                DamageRequestKind.PRIMARY,
                1.0f
        ).build());
        assertStatus(result, DamageSubmissionStatus.APPLIED, null);
        if (probe.externalEntryModeInvocations != expectedEntries
                || probe.externalAffixModeInvocations != expectedAffixes) {
            throw new AssertionError(
                    "External entry/affix read mode was not honored: entries="
                            + probe.externalEntryModeInvocations
                            + " affixes=" + probe.externalAffixModeInvocations
            );
        }
    }

    private static void verifyExternalSourceFailureIsolation(
            SettlementProbe probe
    ) {
        probe.clear();
        probe.externalThrowingTarget = probe.spawn();
        probe.externalThrowingInvocations = 0;
        DamageResult result = DamageNexusApi.submitDamage(request(
                probe.levelHelper,
                probe.externalThrowingTarget,
                probe.attacker,
                DamageRequestKind.PRIMARY,
                1.0f
        ).build());
        assertStatus(result, DamageSubmissionStatus.APPLIED, null);
        if (probe.externalThrowingInvocations != 2
                || probe.snapshots.size() != 1) {
            throw new AssertionError(
                    "External item source exception was not isolated per direction"
            );
        }
        probe.externalThrowingTarget = null;
        verifyCleanup(probe);
    }

    private static void verifyProjectileExternalSourceDeduplication(
            SettlementProbe probe
    ) {
        probe.clear();
        probe.externalSourcesActive = true;
        probe.externalDefensiveStack = ItemStack.EMPTY;
        probe.externalProjectileRuleInvocations = 0;
        probe.capturedProjectileRuleInvocations = 0;
        ItemStack capturedWeapon = ruleStack(
                "gametest_captured_projectile",
                DamageRuleRole.OFFENSIVE,
                DamagePhase.BASE_MODIFICATION,
                List.of(new Phase5CountingOperation(
                        Phase5Counter.CAPTURED_PROJECTILE
                ))
        );
        probe.externalOffensiveStack = ruleStack(
                "gametest_external_projectile",
                DamageRuleRole.OFFENSIVE,
                DamagePhase.BASE_MODIFICATION,
                List.of(new Phase5CountingOperation(
                        Phase5Counter.EXTERNAL_PROJECTILE
                ))
        );
        try {
            probe.externalCategory = EquippedItemRuleSourceCategory.PROJECTILE;
            submitCapturedProjectile(probe, capturedWeapon);
            if (probe.capturedProjectileRuleInvocations != 1
                    || probe.externalProjectileRuleInvocations != 0) {
                throw new AssertionError(
                        "External projectile source duplicated the captured weapon"
                );
            }

            probe.clear();
            probe.externalCategory = EquippedItemRuleSourceCategory.ITEM;
            submitCapturedProjectile(probe, capturedWeapon);
            if (probe.capturedProjectileRuleInvocations != 2
                    || probe.externalProjectileRuleInvocations != 1) {
                throw new AssertionError(
                        "Generic external equipment did not apply to projectile damage"
                );
            }
        } finally {
            probe.externalSourcesActive = false;
            probe.externalOffensiveStack = ItemStack.EMPTY;
            probe.externalDefensiveStack = ItemStack.EMPTY;
            probe.externalCategory = EquippedItemRuleSourceCategory.ITEM;
        }
        verifyCleanup(probe);
    }

    private static void submitCapturedProjectile(
            SettlementProbe probe,
            ItemStack capturedWeapon
    ) {
        LivingEntity target = probe.spawn();
        Arrow arrow = probe.levelHelper.spawn(
                EntityType.ARROW,
                new BlockPos(probe.nextSpawnX++, 2, 1)
        );
        arrow.setOwner(probe.attacker);
        DamageSource source = target.damageSources().arrow(
                arrow,
                probe.attacker
        );
        VanillaDamageCapture.captureModifyDamage(
                probe.levelHelper.getLevel(),
                capturedWeapon,
                target,
                source,
                1.0f,
                1.0f
        );
        boolean accepted = target.hurtServer(
                probe.levelHelper.getLevel(),
                source,
                1.0f
        );
        if (!accepted || probe.snapshots.size() != 1) {
            throw new AssertionError(
                    "Captured projectile source did not settle exactly once"
            );
        }
    }

    private static void verifyEquipmentOwnerAuthorization(
            SettlementProbe probe
    ) {
        ItemStack originalAttackerStack =
                probe.attacker.getMainHandItem().copy();
        LivingEntity unrelatedOwner = probe.spawn();
        ItemStack originalUnrelatedStack =
                unrelatedOwner.getMainHandItem().copy();
        LivingEntity targetOwner = probe.spawn();
        ItemStack originalTargetStack = targetOwner.getMainHandItem().copy();
        unrelatedOwner.setItemSlot(
                EquipmentSlot.MAINHAND,
                ruleStack(
                        "gametest_forged_equipment_owner",
                        DamagePhase.BASE_MODIFICATION,
                        List.of(new CountingEquipmentOperation())
                )
        );

        try {
            probe.equipmentRuleInvocations = 0;

            probe.clear();
            DamageResult authorized = DamageNexusApi.submitDamage(request(
                    probe.levelHelper,
                    probe.spawn(),
                    probe.attacker,
                    DamageRequestKind.PRIMARY,
                    1.0f
            ).equipmentOwner(probe.attacker).build());
            assertStatus(authorized, DamageSubmissionStatus.APPLIED, null);
            if (probe.snapshots.size() != 1
                    || probe.snapshots.getFirst().equipmentOwner()
                    != probe.attacker) {
                throw new AssertionError(
                        "Logical attacker was not accepted as equipment owner"
                );
            }

            probe.clear();
            DamageResult noOwner = DamageNexusApi.submitDamage(request(
                    probe.levelHelper,
                    probe.spawn(),
                    probe.attacker,
                    DamageRequestKind.PRIMARY,
                    1.0f
            ).equipmentOwner(null).build());
            assertStatus(noOwner, DamageSubmissionStatus.APPLIED, null);
            if (probe.snapshots.size() != 1
                    || probe.snapshots.getFirst().equipmentOwner() != null) {
                throw new AssertionError(
                        "Explicit null equipment owner was not preserved"
                );
            }

            probe.clear();
            DamageRequest roleOnlyOwner = request(
                    probe.levelHelper,
                    probe.spawn(),
                    probe.attacker,
                    DamageRequestKind.PRIMARY,
                    1.0f
            ).logicalAttacker(null)
                    .directEntity(unrelatedOwner)
                    .effectOwner(unrelatedOwner)
                    .build();
            if (roleOnlyOwner.equipmentOwner() != null) {
                throw new AssertionError(
                        "Direct/effect roles inferred an equipment owner"
                );
            }
            DamageResult roleOnlyResult = DamageNexusApi.submitDamage(
                    roleOnlyOwner
            );
            assertStatus(
                    roleOnlyResult,
                    DamageSubmissionStatus.APPLIED,
                    null
            );
            if (probe.snapshots.size() != 1
                    || probe.snapshots.getFirst().equipmentOwner() != null) {
                throw new AssertionError(
                        "Role-only owner changed during submission"
                );
            }

            assertUnauthorizedEquipmentOwner(
                    probe,
                    request(
                            probe.levelHelper,
                            probe.spawn(),
                            probe.attacker,
                            DamageRequestKind.PRIMARY,
                            1.0f
                    ).equipmentOwner(unrelatedOwner).build()
            );
            assertUnauthorizedEquipmentOwner(
                    probe,
                    request(
                            probe.levelHelper,
                            probe.spawn(),
                            probe.attacker,
                            DamageRequestKind.PRIMARY,
                            1.0f
                    ).directEntity(unrelatedOwner)
                            .equipmentOwner(unrelatedOwner)
                            .build()
            );
            assertUnauthorizedEquipmentOwner(
                    probe,
                    request(
                            probe.levelHelper,
                            probe.spawn(),
                            probe.attacker,
                            DamageRequestKind.PRIMARY,
                            1.0f
                    ).effectOwner(unrelatedOwner)
                            .equipmentOwner(unrelatedOwner)
                            .build()
            );

            targetOwner.setItemSlot(
                    EquipmentSlot.MAINHAND,
                    unrelatedOwner.getMainHandItem().copy()
            );
            assertUnauthorizedEquipmentOwner(
                    probe,
                    request(
                            probe.levelHelper,
                            targetOwner,
                            probe.attacker,
                            DamageRequestKind.PRIMARY,
                            1.0f
                    ).equipmentOwner(targetOwner).build()
            );

            if (probe.equipmentRuleInvocations != 0) {
                throw new AssertionError(
                        "Rejected equipment-owner rules were collected"
                );
            }
        } finally {
            probe.attacker.setItemSlot(
                    EquipmentSlot.MAINHAND,
                    originalAttackerStack
            );
            unrelatedOwner.setItemSlot(
                    EquipmentSlot.MAINHAND,
                    originalUnrelatedStack
            );
            targetOwner.setItemSlot(
                    EquipmentSlot.MAINHAND,
                    originalTargetStack
            );
        }
    }

    private static void assertUnauthorizedEquipmentOwner(
            SettlementProbe probe,
            DamageRequest request
    ) {
        probe.clear();
        DamageResult result = DamageNexusApi.submitDamage(request);
        assertStatus(
                result,
                DamageSubmissionStatus.REJECTED,
                DamageFailureReason.EQUIPMENT_OWNER_UNAUTHORIZED
        );
        if (result.pipelineExecuted()
                || result.settlement().isPresent()
                || !probe.snapshots.isEmpty()) {
            throw new AssertionError(
                    "Unauthorized equipment owner entered the pipeline"
            );
        }
    }

    private static void verifyUnregisteredNativeDamage(
            SettlementProbe probe
    ) {
        probe.clear();
        LivingEntity target = probe.spawn();
        DamageSource source = new DamageSource(Holder.direct(
                new DamageType("gametest_direct_holder", 0.0f)
        ));
        float before = target.getHealth();

        if (DamageSourcePolicy.shouldManage(source)
                || DamageSourceDescriptor.tryFrom(source).isPresent()) {
            throw new AssertionError(
                    "Unregistered source acquired managed authority or an ID"
            );
        }

        VanillaDamageCapture.captureModifyDamage(
                probe.levelHelper.getLevel(),
                ItemStack.EMPTY,
                target,
                source,
                2.0f,
                2.0f
        );
        boolean accepted = target.hurtServer(
                probe.levelHelper.getLevel(),
                source,
                2.0f
        );

        if (!accepted || target.getHealth() >= before) {
            throw new AssertionError(
                    "Unregistered native source did not continue through vanilla"
            );
        }
        if (!probe.snapshots.isEmpty()) {
            throw new AssertionError(
                    "Unregistered native source published a fabricated settlement"
            );
        }
        if (VanillaDamageCapture.consumeOffensiveSnapshot(
                source,
                target,
                2.0f
        ) != null) {
            throw new AssertionError(
                    "Unregistered native source leaked vanilla capture state"
            );
        }
        verifyCleanup(probe);
    }

    private static void verifyAbsorptionAndOverkill(SettlementProbe probe) {
        LivingEntity unabsorbed = probe.spawn();
        DamageSettlementSnapshot normal = submitAndSingleSnapshot(
                probe,
                unabsorbed,
                2.0f
        );
        if (normal.healthDamage() <= 0.0f
                || normal.absorptionDamage() != 0.0f) {
            throw new AssertionError(
                    "Unabsorbed damage did not reduce only health"
            );
        }

        LivingEntity absorbed = probe.spawn(9);
        absorbed.getAttribute(Attributes.MAX_ABSORPTION)
                .setBaseValue(20.0);
        absorbed.setAbsorptionAmount(5.0f);
        DamageSettlementSnapshot full = submitAndSingleSnapshot(
                probe,
                absorbed,
                2.0f
        );
        assertAmounts(
                full,
                full.resolvedDamage(),
                full.resolvedDamage(),
                0.0f,
                full.resolvedDamage()
        );

        LivingEntity splitTarget = probe.spawn(11);
        splitTarget.getAttribute(Attributes.MAX_ABSORPTION)
                .setBaseValue(20.0);
        splitTarget.setAbsorptionAmount(1.0f);
        DamageSettlementSnapshot split = submitAndSingleSnapshot(
                probe,
                splitTarget,
                3.0f
        );
        assertAmounts(
                split,
                split.resolvedDamage(),
                split.resolvedDamage(),
                split.resolvedDamage() - 1.0f,
                1.0f
        );
        if (close(split.healthDamage(), split.absorptionDamage())
                || close(split.healthDamage(), split.appliedDamage())) {
            throw new AssertionError(
                    "Resolved/applied/health/absorption were conflated"
            );
        }

        LivingEntity overkillTarget = probe.spawn(13);
        overkillTarget.setHealth(2.0f);
        DamageSettlementSnapshot overkill = submitAndSingleSnapshot(
                probe,
                overkillTarget,
                10.0f
        );
        assertAmounts(
                overkill,
                overkill.resolvedDamage(),
                overkill.resolvedDamage(),
                2.0f,
                0.0f
        );
    }

    private static void verifyZeroAndLateCancellation(SettlementProbe probe) {
        LivingEntity zeroTarget = probe.spawn(15);
        DamageSettlementSnapshot zero = submitAndSingleSnapshot(
                probe,
                zeroTarget,
                0.0f
        );
        if (zero.status() != DamageSettlementStatus.NOT_APPLIED
                || zero.reason().orElseThrow()
                != DamageFailureReason.ZERO_DAMAGE
                || zero.appliedDamage() != 0.0f
                || zero.healthDamage() != 0.0f
                || zero.absorptionDamage() != 0.0f) {
            throw new AssertionError("Zero settlement semantics are invalid");
        }

        probe.clear();
        probe.lateCancelTarget = probe.spawn(17);
        DamageResult late = DamageNexusApi.submitDamage(request(
                probe.levelHelper,
                probe.lateCancelTarget,
                probe.attacker,
                DamageRequestKind.PRIMARY,
                2.0f
        ).build());
        if (probe.snapshots.size() != 1) {
            throw new AssertionError(
                    "Late-cancelled managed damage did not publish once"
            );
        }
        DamageSettlementSnapshot snapshot = probe.snapshots.getFirst();
        assertStatus(
                late,
                DamageSubmissionStatus.NOT_APPLIED,
                DamageFailureReason.LATE_INCOMING_CANCELLATION
        );
        if (!snapshot.cancelled()
                || snapshot.appliedDamage() != 0.0f
                || snapshot.healthDamage() != 0.0f
                || snapshot.absorptionDamage() != 0.0f) {
            throw new AssertionError(
                    "Late cancellation fabricated application values"
            );
        }
        probe.lateCancelTarget = null;
    }

    private static void verifyZeroAfterPre(SettlementProbe probe) {
        probe.clear();
        probe.preZeroTarget = probe.spawn();
        DamageResult result;
        try {
            result = DamageNexusApi.submitDamage(request(
                    probe.levelHelper,
                    probe.preZeroTarget,
                    probe.attacker,
                    DamageRequestKind.PRIMARY,
                    2.0f
            ).build());
        } finally {
            probe.preZeroTarget = null;
        }

        assertStatus(
                result,
                DamageSubmissionStatus.NOT_APPLIED,
                DamageFailureReason.ZERO_AFTER_PRE
        );
        if (!result.pipelineExecuted() || !result.vanillaAccepted()
                || result.cancelled() || probe.snapshots.size() != 1) {
            throw new AssertionError(
                    "Pre-zero result lifecycle semantics are invalid"
            );
        }
        DamageSettlementSnapshot snapshot = probe.snapshots.getFirst();
        if (snapshot.reason().orElseThrow()
                != DamageFailureReason.ZERO_AFTER_PRE
                || snapshot.resolvedDamage() <= 0.0f
                || snapshot.appliedDamage() != 0.0f
                || snapshot.healthDamage() != 0.0f
                || snapshot.absorptionDamage() != 0.0f
                || snapshot.cancelled()
                || !snapshot.pipelineExecuted()) {
            throw new AssertionError(
                    "Pre-zero settlement values are invalid"
            );
        }
    }

    private static void verifyFrameworkCancellation(SettlementProbe probe) {
        ItemStack original = probe.attacker.getMainHandItem().copy();
        ItemStack cancellationSource = new ItemStack(Items.STONE);
        DamageRuleDefinition rule = new DamageRuleDefinition(
                id("gametest_cancel_rule"),
                DamageRuleRole.OFFENSIVE,
                DamagePhase.FINAL_OVERRIDE,
                500,
                List.of(),
                List.of(DamageNexusOperations.cancelDamage(
                        "gametest/settlement_cancel"
                )),
                DamageRuleStacking.STACK,
                Optional.empty(),
                Optional.empty()
        );
        DamageEntryDefinition entry = new DamageEntryDefinition(
                id("gametest_cancel_entry"),
                DamageEntryDisplay.EMPTY,
                DamageEntrySlot.ITEM,
                List.of(rule),
                DamageEntryStacking.STACK,
                Optional.empty()
        );
        cancellationSource.set(
                ModDataComponents.DAMAGE_ENTRIES.get(),
                List.of(entry)
        );
        probe.attacker.setItemSlot(
                EquipmentSlot.MAINHAND,
                cancellationSource
        );

        try {
            LivingEntity target = probe.spawn();
            probe.clear();
            DamageResult result = DamageNexusApi.submitDamage(request(
                    probe.levelHelper,
                    target,
                    probe.attacker,
                    DamageRequestKind.PRIMARY,
                    2.0f
            ).build());
            assertStatus(
                    result,
                    DamageSubmissionStatus.NOT_APPLIED,
                    DamageFailureReason.CANCELLED
            );
            if (probe.snapshots.size() != 1) {
                throw new AssertionError(
                        "Cancelled damage did not publish exactly once"
                );
            }
            DamageSettlementSnapshot snapshot = probe.snapshots.getFirst();
            if (snapshot.status() != DamageSettlementStatus.NOT_APPLIED
                    || snapshot.reason().orElseThrow()
                    != DamageFailureReason.CANCELLED
                    || !snapshot.cancelled()
                    || snapshot.appliedDamage() != 0.0f
                    || snapshot.healthDamage() != 0.0f
                    || snapshot.absorptionDamage() != 0.0f) {
                throw new AssertionError(
                        "DamageNexus cancellation settlement is invalid"
                );
            }
        } finally {
            probe.attacker.setItemSlot(EquipmentSlot.MAINHAND, original);
        }
    }

    private static void verifyRejectedRequestAndListenerFailure(
            SettlementProbe probe
    ) {
        probe.clear();
        LivingEntity target = probe.spawn(19);
        DamageRequest unknownType = DamageRequest.builder(
                probe.levelHelper.getLevel(),
                target,
                DamageSourceDescriptor.of(ResourceKey.create(
                        Registries.DAMAGE_TYPE,
                        Identifier.fromNamespaceAndPath(
                                "examplemod",
                                "missing_damage_type"
                        )
                )),
                1.0f
        ).logicalAttacker(probe.attacker).build();
        DamageResult rejected = DamageNexusApi.submitDamage(unknownType);
        assertStatus(
                rejected,
                DamageSubmissionStatus.REJECTED,
                DamageFailureReason.UNKNOWN_DAMAGE_TYPE
        );
        if (!probe.snapshots.isEmpty() || rejected.settlement().isPresent()) {
            throw new AssertionError(
                    "Pre-pipeline rejection published a settlement"
            );
        }

        probe.clear();
        LivingEntity duplicateTarget = probe.spawn();
        DamageRequest singleUse = request(
                probe.levelHelper,
                duplicateTarget,
                probe.attacker,
                DamageRequestKind.PRIMARY,
                1.0f
        ).build();
        DamageResult firstSubmission = DamageNexusApi.submitDamage(singleUse);
        int eventsAfterFirstSubmission = probe.snapshots.size();
        DamageResult duplicate = DamageNexusApi.submitDamage(singleUse);
        assertStatus(firstSubmission, DamageSubmissionStatus.APPLIED, null);
        assertStatus(
                duplicate,
                DamageSubmissionStatus.REJECTED,
                DamageFailureReason.DUPLICATE_REQUEST
        );
        if (eventsAfterFirstSubmission != 1
                || probe.snapshots.size() != eventsAfterFirstSubmission
                || duplicate.settlement().isPresent()) {
            throw new AssertionError(
                    "Duplicate request entered or observed the pipeline"
            );
        }

        probe.clear();
        LivingEntity parentTarget = probe.spawn(19);
        probe.throwingListenerTarget = probe.spawn(21);
        probe.throwingCallbackTarget = probe.throwingListenerTarget;
        probe.listenerFailureQueuedTarget = probe.spawn();
        float throwingBefore = probe.throwingListenerTarget.getHealth();
        float queuedBefore = probe.listenerFailureQueuedTarget.getHealth();
        DamageResult[] queuedChildren = new DamageResult[2];
        probe.settlementAction = callback -> {
            if (callback.snapshot().target() != parentTarget) {
                return;
            }
            probe.settlementAction = null;
            DamageParentRef authority = callback.childAuthority()
                    .orElseThrow();
            queuedChildren[0] = DamageNexusApi.submitDamage(request(
                    probe.levelHelper,
                    probe.throwingListenerTarget,
                    probe.attacker,
                    DamageRequestKind.PROC,
                    1.0f
            ).parent(authority).build());
            queuedChildren[1] = DamageNexusApi.submitDamage(request(
                    probe.levelHelper,
                    probe.listenerFailureQueuedTarget,
                    probe.attacker,
                    DamageRequestKind.DOT,
                    1.0f
            ).parent(authority).build());
        };
        DamageResult committed = DamageNexusApi.submitDamage(request(
                probe.levelHelper,
                parentTarget,
                probe.attacker,
                DamageRequestKind.PRIMARY,
                1.0f
        ).build());
        assertStatus(committed, DamageSubmissionStatus.APPLIED, null);
        assertStatus(queuedChildren[0], DamageSubmissionStatus.APPLIED, null);
        assertStatus(queuedChildren[1], DamageSubmissionStatus.APPLIED, null);
        if (probe.snapshots.size() != 3
                || committed.settlement().isEmpty()
                || probe.throwingListenerTarget.getHealth()
                >= throwingBefore
                || probe.listenerFailureQueuedTarget.getHealth()
                >= queuedBefore
                || probe.throwingCallbackInvocations != 1
                || !probe.callbackAfterFailureObserved
                || DamageSettlementCoordinator.pendingCountForTests() != 0
                || DamageSettlementCoordinator.drainingForTests()) {
            throw new AssertionError(
                    "Listener failure invalidated damage or stalled the FIFO"
            );
        }
        probe.throwingListenerTarget = null;
        probe.throwingCallbackTarget = null;

        LivingEntity laterTarget = probe.spawn();
        DamageResult later = DamageNexusApi.submitDamage(request(
                probe.levelHelper,
                laterTarget,
                probe.attacker,
                DamageRequestKind.PRIMARY,
                1.0f
        ).build());
        assertStatus(later, DamageSubmissionStatus.APPLIED, null);
        if (probe.snapshots.size() != 4) {
            throw new AssertionError(
                    "A later independent settlement was not published after listener failure"
            );
        }
    }

    private static void verifyVanillaCooldownRejection(
            SettlementProbe probe
    ) {
        LivingEntity target = probe.spawn();
        submitAndSingleSnapshot(probe, target, 2.0f);

        probe.clear();
        DamageResult rejected = DamageNexusApi.submitDamage(request(
                probe.levelHelper,
                target,
                probe.attacker,
                DamageRequestKind.PRIMARY,
                1.0f
        ).build());
        assertStatus(
                rejected,
                DamageSubmissionStatus.NOT_APPLIED,
                DamageFailureReason.VANILLA_REJECTED
        );
        if (probe.snapshots.size() != 1) {
            throw new AssertionError(
                    "Cooldown rejection did not publish exactly once"
            );
        }
        DamageSettlementSnapshot snapshot = probe.snapshots.getFirst();
        if (snapshot.appliedDamage() != 0.0f
                || snapshot.healthDamage() != 0.0f
                || snapshot.absorptionDamage() != 0.0f
                || snapshot.cancelled()) {
            throw new AssertionError(
                    "Cooldown rejection fabricated applied damage"
            );
        }
    }

    private static void verifyTolerantCallbackRollback(
            SettlementProbe probe
    ) {
        ItemStack original = probe.attacker.getMainHandItem().copy();
        try {
            probe.providerFailureInvocations = 0;
            probe.processorFailureInvocations = 0;
            probe.attacker.setItemSlot(
                    EquipmentSlot.MAINHAND,
                    ruleStack(
                            "gametest_prior_provider_rule",
                            DamagePhase.BASE_MODIFICATION,
                            List.of(DamageNexusOperations.addBaseDamage(
                                    io.github.naimjeg.damagenexus.api.enums
                                            .DamageChannel.UNTYPED_ID,
                                    1.0f
                            ))
                    )
            );
            DamageSettlementSnapshot providerControl =
                    submitAndSingleSnapshot(probe, probe.spawn(), 2.0f);

            probe.providerRollbackTarget = probe.spawn();
            DamageSettlementSnapshot providerFailure;
            try {
                providerFailure = submitAndSingleSnapshot(
                        probe,
                        probe.providerRollbackTarget,
                        2.0f
                );
            } finally {
                probe.providerRollbackTarget = null;
            }
            if (!close(
                    providerControl.resolvedDamage(),
                    providerFailure.resolvedDamage()
            )) {
                throw new AssertionError(
                        "Failed provider changed prior rules or damage state"
                );
            }
            if (probe.providerFailureInvocations == 0) {
                throw new AssertionError(
                        "GameTest provider fault injection did not execute"
                );
            }

            probe.attacker.setItemSlot(
                    EquipmentSlot.MAINHAND,
                    ItemStack.EMPTY
            );
            DamageSettlementSnapshot processorControl =
                    submitAndSingleSnapshot(probe, probe.spawn(), 2.0f);
            probe.processorRollbackTarget = probe.spawn();
            DamageSettlementSnapshot processorFailure;
            try {
                processorFailure = submitAndSingleSnapshot(
                        probe,
                        probe.processorRollbackTarget,
                        2.0f
                );
            } finally {
                probe.processorRollbackTarget = null;
            }
            if (!close(
                    processorControl.resolvedDamage(),
                    processorFailure.resolvedDamage()
            )) {
                throw new AssertionError(
                        "Failed processor left partially applied multipliers"
                );
            }
            if (probe.processorFailureInvocations == 0) {
                throw new AssertionError(
                        "GameTest processor fault injection did not execute"
                );
            }

            probe.attacker.setItemSlot(
                    EquipmentSlot.MAINHAND,
                    ruleStack(
                            "gametest_operation_rollback",
                            DamagePhase.FINAL_OVERRIDE,
                            List.of(
                                    new ThrowingFinalOperation(),
                                    DamageNexusOperations
                                            .overrideFinalDamage(3.0f)
                            )
                    )
            );
            DamageSettlementSnapshot operationFailure =
                    submitAndSingleSnapshot(probe, probe.spawn(), 2.0f);
            if (!close(operationFailure.resolvedDamage(), 3.0f)
                    || operationFailure.cancelled()) {
                throw new AssertionError(
                        "Failed rule operation was not fully rolled back"
                );
            }
        } finally {
            probe.providerRollbackTarget = null;
            probe.processorRollbackTarget = null;
            probe.attacker.setItemSlot(EquipmentSlot.MAINHAND, original);
        }
    }

    private static void verifyStrictCallbackCleanup(
            SettlementProbe probe
    ) {
        DamageNexusConfigValues originalConfig = DamageNexusConfig.current();
        ItemStack originalStack = probe.attacker.getMainHandItem().copy();
        try {
            setStrictModes(originalConfig, true, false);
            probe.clear();
            probe.criticalThrowingTarget = probe.spawn();
            assertSubmissionThrows(probe, probe.criticalThrowingTarget);
            probe.criticalThrowingTarget = null;
            if (!probe.snapshots.isEmpty()) {
                throw new AssertionError(
                        "Strict critical provider failure published a settlement"
                );
            }
            verifyCleanup(probe);

            probe.clear();
            probe.processorRollbackTarget = probe.spawn();
            assertSubmissionThrows(probe, probe.processorRollbackTarget);
            probe.processorRollbackTarget = null;
            if (!probe.snapshots.isEmpty()) {
                throw new AssertionError(
                        "Strict processor failure published a settlement"
                );
            }
            verifyCleanup(probe);

            setStrictModes(originalConfig, false, true);
            probe.attacker.setItemSlot(
                    EquipmentSlot.MAINHAND,
                    ruleStack(
                            "gametest_strict_operation",
                            DamagePhase.FINAL_OVERRIDE,
                            List.of(new ThrowingFinalOperation())
                    )
            );
            probe.clear();
            assertSubmissionThrows(probe, probe.spawn());
            if (!probe.snapshots.isEmpty()) {
                throw new AssertionError(
                        "Strict rule failure published a settlement"
                );
            }
            verifyCleanup(probe);
        } finally {
            probe.criticalThrowingTarget = null;
            probe.processorRollbackTarget = null;
            probe.attacker.setItemSlot(EquipmentSlot.MAINHAND, originalStack);
            setConfig(originalConfig);
        }
    }

    private static void assertSubmissionThrows(
            SettlementProbe probe,
            LivingEntity target
    ) {
        boolean threw = false;
        try {
            DamageNexusApi.submitDamage(request(
                    probe.levelHelper,
                    target,
                    probe.attacker,
                    DamageRequestKind.PRIMARY,
                    2.0f
            ).build());
        } catch (RuntimeException expected) {
            threw = true;
        }
        if (!threw) {
            throw new AssertionError("Strict callback failure did not escape");
        }
    }

    private static ItemStack ruleStack(
            String path,
            DamagePhase phase,
            List<DamageRuleOperation> operations
    ) {
        return ruleStack(
                path,
                DamageRuleRole.OFFENSIVE,
                phase,
                operations
        );
    }

    private static ItemStack ruleStack(
            String path,
            DamageRuleRole role,
            DamagePhase phase,
            List<DamageRuleOperation> operations
    ) {
        return ruleStack(
                path,
                role,
                phase,
                List.of(),
                operations
        );
    }

    private static ItemStack ruleStack(
            String path,
            DamageRuleRole role,
            DamagePhase phase,
            List<DamageRuleCondition> conditions,
            List<DamageRuleOperation> operations
    ) {
        DamageRuleDefinition rule = new DamageRuleDefinition(
                id(path + "_rule"),
                role,
                phase,
                500,
                conditions,
                operations,
                DamageRuleStacking.STACK,
                Optional.empty(),
                Optional.empty()
        );
        DamageEntryDefinition entry = new DamageEntryDefinition(
                id(path + "_entry"),
                DamageEntryDisplay.EMPTY,
                DamageEntrySlot.ITEM,
                List.of(rule),
                DamageEntryStacking.STACK,
                Optional.empty()
        );
        ItemStack stack = new ItemStack(Items.STONE);
        stack.set(ModDataComponents.DAMAGE_ENTRIES.get(), List.of(entry));
        return stack;
    }

    private static ItemStack entryAndAffixRuleStack() {
        ItemStack stack = ruleStack(
                "gametest_external_mode_entry",
                DamageRuleRole.OFFENSIVE,
                DamagePhase.BASE_MODIFICATION,
                List.of(new Phase5CountingOperation(
                        Phase5Counter.EXTERNAL_ENTRY_MODE
                ))
        );
        DamageRuleDefinition affixRule = new DamageRuleDefinition(
                id("gametest_external_mode_affix_rule"),
                DamageRuleRole.OFFENSIVE,
                DamagePhase.BASE_MODIFICATION,
                500,
                List.of(),
                List.of(new Phase5CountingOperation(
                        Phase5Counter.EXTERNAL_AFFIX_MODE
                )),
                DamageRuleStacking.STACK,
                Optional.empty(),
                Optional.empty()
        );
        DamageEntryDefinition nestedEntry = new DamageEntryDefinition(
                id("gametest_external_mode_affix_entry"),
                DamageEntryDisplay.EMPTY,
                DamageEntrySlot.ITEM,
                List.of(affixRule),
                DamageEntryStacking.STACK,
                Optional.empty()
        );
        DamageAffixDefinition affix = new DamageAffixDefinition(
                id("gametest_external_mode_affix"),
                DamageAffixDisplay.EMPTY,
                DamageAffixSlot.ITEM,
                DamageAffixRarity.COMMON,
                List.of(nestedEntry),
                DamageAffixStacking.STACK,
                Optional.empty()
        );
        stack.set(ModDataComponents.DAMAGE_AFFIXES.get(), List.of(affix));
        return stack;
    }

    private static void verifyTickBudgetSameTick(
            SettlementProbe probe,
            DamageNexusConfigValues originalConfig
    ) {
        int baseline = DamageAdmissionController.currentTickCount(
                probe.levelHelper.getLevel().getServer()
        );
        int limit = baseline + 2;
        setSafety(
                originalConfig,
                new DamageSafetySettings(
                        originalConfig.safety().maxRecursionDepth(),
                        originalConfig.safety().maxDerivedRequestsPerRoot(),
                        limit
                )
        );
        probe.tickBudgetTestTick = probe.levelHelper.getLevel()
                .getServer()
                .getTickCount();
        probe.tickBudgetLimit = limit;

        probe.clear();
        for (int index = 1; index <= 2; index++) {
            DamageResult accepted = DamageNexusApi.submitDamage(request(
                    probe.levelHelper,
                    probe.spawn(),
                    probe.attacker,
                    DamageRequestKind.PRIMARY,
                    1.0f
            ).build());
            assertStatus(accepted, DamageSubmissionStatus.APPLIED, null);
            int count = DamageAdmissionController.currentTickCount(
                    probe.levelHelper.getLevel().getServer()
            );
            if (count != baseline + index) {
                throw new AssertionError(
                        "Public request was counted more than once"
                );
            }
        }

        probe.clear();
        LivingEntity rejectedTarget = probe.spawn();
        float healthBefore = rejectedTarget.getHealth();
        float absorptionBefore = rejectedTarget.getAbsorptionAmount();
        DamageRequest rejectedRequest = request(
                probe.levelHelper,
                rejectedTarget,
                probe.attacker,
                DamageRequestKind.PRIMARY,
                1.0f
        ).build();
        DamageResult rejected = DamageNexusApi.submitDamage(rejectedRequest);
        assertPrePipelineRejected(
                probe,
                rejected,
                DamageFailureReason.SERVER_TICK_BUDGET_EXHAUSTED,
                rejectedTarget,
                healthBefore,
                absorptionBefore
        );
        assertStatus(
                DamageNexusApi.submitDamage(rejectedRequest),
                DamageSubmissionStatus.REJECTED,
                DamageFailureReason.DUPLICATE_REQUEST
        );

        probe.clear();
        LivingEntity nativeTarget = probe.spawn();
        float nativeHealth = nativeTarget.getHealth();
        float nativeAbsorption = nativeTarget.getAbsorptionAmount();
        DamageSource source = probe.attacker.damageSources()
                .playerAttack(probe.attacker);
        boolean vanillaAccepted = nativeTarget.hurtServer(
                probe.levelHelper.getLevel(),
                source,
                1.0f
        );
        if (vanillaAccepted || probe.snapshots.size() != 1) {
            throw new AssertionError(
                    "Native tick-budget rejection did not settle exactly once"
            );
        }
        DamageSettlementSnapshot snapshot = probe.snapshots.getFirst();
        if (snapshot.status() != DamageSettlementStatus.NOT_APPLIED
                || snapshot.reason().orElse(null)
                != DamageFailureReason.SERVER_TICK_BUDGET_EXHAUSTED
                || snapshot.pipelineExecuted()
                || snapshot.resolvedDamage() != 0.0f
                || snapshot.appliedDamage() != 0.0f
                || snapshot.healthDamage() != 0.0f
                || snapshot.absorptionDamage() != 0.0f
                || snapshot.healthBefore() != snapshot.healthAfter()
                || snapshot.absorptionBefore()
                != snapshot.absorptionAfter()
                || nativeTarget.getHealth() != nativeHealth
                || nativeTarget.getAbsorptionAmount() != nativeAbsorption) {
            throw new AssertionError(
                    "Native tick-budget settlement semantics are invalid"
            );
        }

        probe.tickResetTarget = probe.spawn();
        verifyCleanup(probe);
    }

    private static void verifyTickBudgetReset(SettlementProbe probe) {
        int currentTick = probe.levelHelper.getLevel()
                .getServer()
                .getTickCount();
        if (currentTick <= probe.tickBudgetTestTick) {
            throw new AssertionError("Tick budget test did not advance a tick");
        }

        probe.clear();
        DamageResult result = DamageNexusApi.submitDamage(request(
                probe.levelHelper,
                probe.tickResetTarget,
                probe.attacker,
                DamageRequestKind.PRIMARY,
                1.0f
        ).build());
        assertStatus(result, DamageSubmissionStatus.APPLIED, null);
        if (probe.snapshots.size() != 1
                || DamageAdmissionController.currentTickCount(
                probe.levelHelper.getLevel().getServer()
        ) > probe.tickBudgetLimit) {
            throw new AssertionError(
                    "Server tick budget did not reset on the next tick"
            );
        }
    }

    private static void assertPrePipelineRejected(
            SettlementProbe probe,
            DamageResult result,
            DamageFailureReason reason,
            LivingEntity target,
            float healthBefore,
            float absorptionBefore
    ) {
        assertStatus(result, DamageSubmissionStatus.REJECTED, reason);
        if (result.pipelineExecuted()
                || result.settlement().isPresent()
                || !probe.snapshots.isEmpty()
                || target.getHealth() != healthBefore
                || target.getAbsorptionAmount() != absorptionBefore) {
            throw new AssertionError(
                    "Rejected admission changed state or entered the pipeline"
            );
        }
        verifyCleanup(probe);
    }

    private static void setSafety(
            DamageNexusConfigValues base,
            DamageSafetySettings safety
    ) {
        setConfig(new DamageNexusConfigValues(
                base.developer(),
                base.diagnostics(),
                base.tooltips(),
                base.formulas(),
                base.vanillaCompatibility(),
                safety
        ));
    }

    private static void setStrictModes(
            DamageNexusConfigValues base,
            boolean strictProcessors,
            boolean strictRules
    ) {
        setConfig(new DamageNexusConfigValues(
                new DeveloperSettings(
                        base.developer().testCommandsEnabled(),
                        strictProcessors,
                        strictRules
                ),
                base.diagnostics(),
                base.tooltips(),
                base.formulas(),
                base.vanillaCompatibility(),
                base.safety()
        ));
    }

    private static void setConfig(DamageNexusConfigValues values) {
        try {
            Field field = DamageNexusConfig.class.getDeclaredField("CURRENT");
            field.setAccessible(true);
            field.set(null, values);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Unable to set GameTest config", exception);
        }
    }

    private static void verifyCleanup(SettlementProbe probe) {
        if (DamageRequestSubmissionTracker.activeDepthForTests() != 0
                || DamageSettlementTracker.activeDepthForTests() != 0
                || DamageSettlementCoordinator.pendingCountForTests() != 0
                || DamageSettlementDispatchScope.depthForTests() != 0
                || DamageSettlementCoordinator.drainingForTests()
                || DamageTransactionActivity.isActive()) {
            throw new AssertionError(
                    "Damage lifecycle state leaked after settlement"
            );
        }
    }

    private static DamageSettlementSnapshot submitAndSingleSnapshot(
            SettlementProbe probe,
            LivingEntity target,
            float amount
    ) {
        probe.clear();
        DamageResult result = DamageNexusApi.submitDamage(request(
                probe.levelHelper,
                target,
                probe.attacker,
                DamageRequestKind.PRIMARY,
                amount
        ).build());
        if (probe.snapshots.size() != 1) {
            throw new AssertionError("Expected exactly one settlement event");
        }
        if (amount > 0.0f) {
            assertStatus(result, DamageSubmissionStatus.APPLIED, null);
        } else {
            assertStatus(
                    result,
                    DamageSubmissionStatus.NOT_APPLIED,
                    DamageFailureReason.ZERO_DAMAGE
            );
        }
        return probe.snapshots.getFirst();
    }

    private static DamageRequest.Builder request(
            GameTestHelper helper,
            LivingEntity target,
            LivingEntity attacker,
            DamageRequestKind kind,
            float amount
    ) {
        return DamageRequest.builder(
                helper.getLevel(),
                target,
                DamageSourceDescriptor.of(DamageTypes.PLAYER_ATTACK),
                amount
        ).logicalAttacker(attacker)
                .kind(kind)
                .actionId(Identifier.fromNamespaceAndPath(
                        "examplemod",
                        "request_test"
                ))
                .sourceTag(Identifier.fromNamespaceAndPath(
                        "examplemod",
                        "test_damage"
                ));
    }

    private static void assertAmounts(
            DamageSettlementSnapshot snapshot,
            float resolved,
            float applied,
            float health,
            float absorption
    ) {
        if (!close(snapshot.resolvedDamage(), resolved)
                || !close(snapshot.appliedDamage(), applied)
                || !close(snapshot.healthDamage(), health)
                || !close(snapshot.absorptionDamage(), absorption)) {
            throw new AssertionError(
                    "Unexpected settlement amounts: resolved="
                            + snapshot.resolvedDamage()
                            + " applied="
                            + snapshot.appliedDamage()
                            + " health="
                            + snapshot.healthDamage()
                            + " absorption="
                            + snapshot.absorptionDamage()
            );
        }
    }

    private static void assertStatus(
            DamageResult result,
            DamageSubmissionStatus status,
            DamageFailureReason reason
    ) {
        if (result.status() != status) {
            throw new AssertionError(
                    "Expected status " + status + " but got " + result.status()
            );
        }
        if (status == DamageSubmissionStatus.APPLIED
                || status == DamageSubmissionStatus.NOT_APPLIED) {
            if (result.settlement().isEmpty()
                    || !result.pipelineExecuted()
                    || result.failure().stream().anyMatch(failure ->
                    failure.reason()
                            == DamageFailureReason.PIPELINE_NOT_OBSERVED)) {
                throw new AssertionError(
                        "Managed result has no observed settlement pipeline"
                );
            }
        }
        if (reason == null) {
            if (result.failure().isPresent()) {
                throw new AssertionError(
                        "Successful result unexpectedly has a failure"
                );
            }
        } else if (result.failure().isEmpty()
                || result.failure().orElseThrow().reason() != reason) {
            throw new AssertionError(
                    "Expected failure " + reason + " but got "
                            + result.failure()
            );
        }
    }

    private static void assertSame(Object expected, Object actual) {
        if (expected != actual) {
            throw new AssertionError("Expected identical snapshot instance");
        }
    }

    private static boolean close(float first, float second) {
        return Math.abs(first - second) <= EPSILON;
    }

    private static void assertSnapshotStateDeltas(
            DamageSettlementSnapshot snapshot
    ) {
        float expectedHealth = Math.max(
                0.0f,
                snapshot.healthBefore() - snapshot.healthAfter()
        );
        float expectedAbsorption = Math.max(
                0.0f,
                snapshot.absorptionBefore() - snapshot.absorptionAfter()
        );
        if (!close(snapshot.healthDamage(), expectedHealth)
                || !close(
                        snapshot.absorptionDamage(),
                        expectedAbsorption
                )) {
            throw new AssertionError(
                    "Applied settlement does not match entity state deltas"
            );
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(DamageNexus.MODID, path);
    }

    private static final class ThrowingMutationProcessor
            implements DamagePhaseProcessor {

        @Override
        public void apply(DamageRuleContext ctx) {
            SettlementProbe probe = ACTIVE_PROBE.get();
            if (probe == null || ctx.victim()
                    != probe.processorRollbackTarget) {
                return;
            }

            probe.processorFailureInvocations++;

            ctx.tryAddChannelPreMultiplier(
                    ctx.getInitialChannel(),
                    PreMultiplierBuckets.genericDamage(),
                    2.0f,
                    "gametest/processor_channel_pre"
            );
            ctx.tryAddApplicationPreMultiplier(
                    DamageApplicationBucket.DN_RULE_BASE,
                    PreMultiplierBuckets.genericDamage(),
                    3.0f,
                    "gametest/processor_application_pre"
            );
            ctx.tryAddGlobalPreMultiplier(
                    PreMultiplierBuckets.genericDamage(),
                    4.0f,
                    "gametest/processor_global_pre"
            );
            ctx.tryAddChannelPostMultiplier(
                    ctx.getInitialChannel(),
                    0.5f,
                    "gametest/processor_channel_post"
            );
            throw new IllegalStateException(
                    "intentional processor rollback test"
            );
        }

        @Override
        public DamagePhase phase() {
            return DamagePhase.TYPE_SCALING;
        }

        @Override
        public int getPriority() {
            return PRIORITY_OVERRIDE;
        }
    }

    private static final class ThrowingMutationProvider
            implements DamageRuleProvider {

        @Override
        public boolean supportsPhase(DamagePhase phase) {
            return phase == DamagePhase.BASE_MODIFICATION;
        }

        @Override
        public void collect(
                DamageRuleContext ctx,
                DamagePhase phase,
                List<RuntimeDamageRule> out
        ) {
            SettlementProbe probe = ACTIVE_PROBE.get();
            if (probe == null || ctx.victim()
                    != probe.providerRollbackTarget) {
                return;
            }

            probe.providerFailureInvocations++;

            ctx.tryAddBaseDamage(
                    ctx.getInitialChannel(),
                    40.0f,
                    "gametest/provider_partial_mutation"
            );
            RuntimeDamageRule partial = new RuntimeDamageRule(
                    new DamageRuleDefinition(
                            id("gametest_provider_partial_rule"),
                            DamageRuleRole.OFFENSIVE,
                            phase,
                            900,
                            List.of(),
                            List.of(DamageNexusOperations.addBaseDamage(
                                    DamageChannel.UNTYPED_ID,
                                    100.0f
                            )),
                            DamageRuleStacking.STACK,
                            Optional.empty(),
                            Optional.empty()
                    ),
                    RuleExecutionContext.javaApiRule(
                            DamageRuleRole.OFFENSIVE
                    )
            );
            out.add(partial);
            out.clear();
            out.add(partial);
            throw new IllegalStateException(
                    "intentional provider rollback test"
            );
        }
    }

    private static final class ThrowingFinalOperation
            implements DamageRuleOperation {

        @Override
        public Identifier type() {
            return id("gametest_throwing_final_operation");
        }

        @Override
        public DamageMutationResult apply(DamageRuleContext ctx) {
            ctx.tryOverrideFinalDamage(
                    99.0f,
                    "gametest/operation_partial_override"
            );
            ctx.tryCancelDamage("gametest/operation_partial_cancel");
            throw new IllegalStateException(
                    "intentional rule operation rollback test"
            );
        }

        @Override
        public Set<DamagePhase> supportedPhases() {
            return Set.of(DamagePhase.FINAL_OVERRIDE);
        }
    }

    private static final class CountingEquipmentOperation
            implements DamageRuleOperation {

        @Override
        public Identifier type() {
            return id("gametest_counting_equipment_operation");
        }

        @Override
        public DamageMutationResult apply(DamageRuleContext ctx) {
            SettlementProbe probe = ACTIVE_PROBE.get();
            if (probe != null) {
                probe.equipmentRuleInvocations++;
            }
            return ctx.tryAddBaseDamage(
                    ctx.getInitialChannel(),
                    100.0f,
                    "gametest/forged_equipment_rule"
            );
        }

        @Override
        public Set<DamagePhase> supportedPhases() {
            return Set.of(DamagePhase.BASE_MODIFICATION);
        }
    }

    private enum Phase5Counter {
        PROXY_EQUIPMENT,
        EXTERNAL_OFFENSIVE,
        EXTERNAL_DEFENSIVE,
        EXTERNAL_SELF,
        CAPTURED_PROJECTILE,
        EXTERNAL_PROJECTILE,
        EXTERNAL_ENTRY_MODE,
        EXTERNAL_AFFIX_MODE,
        PHASE6_CONDITION,
        PHASE7_CRITICAL_RULE,
        ACTIVE_TRANSACTION_SUBMIT
    }

    private record Phase5CountingOperation(Phase5Counter counter)
            implements DamageRuleOperation {

        @Override
        public Identifier type() {
            return id("gametest_phase5_counting_operation_"
                    + counter.name().toLowerCase());
        }

        @Override
        public DamageMutationResult apply(DamageRuleContext ctx) {
            SettlementProbe probe = ACTIVE_PROBE.get();
            if (probe != null) {
                switch (counter) {
                    case PROXY_EQUIPMENT ->
                            probe.proxyEquipmentRuleInvocations++;
                    case EXTERNAL_OFFENSIVE ->
                            probe.externalOffensiveRuleInvocations++;
                    case EXTERNAL_DEFENSIVE ->
                            probe.externalDefensiveRuleInvocations++;
                    case EXTERNAL_SELF ->
                            probe.externalSelfRuleInvocations++;
                    case CAPTURED_PROJECTILE ->
                            probe.capturedProjectileRuleInvocations++;
                    case EXTERNAL_PROJECTILE ->
                            probe.externalProjectileRuleInvocations++;
                    case EXTERNAL_ENTRY_MODE ->
                            probe.externalEntryModeInvocations++;
                    case EXTERNAL_AFFIX_MODE ->
                            probe.externalAffixModeInvocations++;
                    case PHASE6_CONDITION ->
                            probe.phase6ConditionInvocations++;
                    case PHASE7_CRITICAL_RULE ->
                            probe.phase7CriticalRuleInvocations++;
                    case ACTIVE_TRANSACTION_SUBMIT -> {
                        if (probe.activeTransactionResult == null) {
                            probe.activeTransactionResult =
                                    DamageNexusApi.submitDamage(request(
                                            probe.levelHelper,
                                            probe.activeScopeNestedTarget,
                                            probe.attacker,
                                            DamageRequestKind.PRIMARY,
                                            1.0f
                                    ).build());
                        }
                    }
                }
            }
            return DamageMutationResult.APPLIED;
        }

        @Override
        public Set<DamagePhase> supportedPhases() {
            return counter == Phase5Counter.PHASE7_CRITICAL_RULE
                    ? Set.of(DamagePhase.CRITICAL_HIT)
                    : Set.of(DamagePhase.BASE_MODIFICATION);
        }
    }

    private static final class AttributeBaseScope implements AutoCloseable {
        private final java.util.IdentityHashMap<AttributeInstance, Double>
                originals = new java.util.IdentityHashMap<>();
        private boolean closed;

        void set(
                LivingEntity entity,
                Holder<Attribute> attribute,
                double value
        ) {
            if (closed) {
                throw new IllegalStateException("Attribute scope is closed");
            }
            AttributeInstance instance = entity.getAttribute(attribute);
            if (instance == null) {
                throw new AssertionError(
                        "Missing test attribute " + attribute.getKey().identifier());
            }
            originals.putIfAbsent(instance, instance.getBaseValue());
            instance.setBaseValue(value);
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            originals.forEach(AttributeInstance::setBaseValue);
            originals.clear();
            closed = true;
        }
    }

    private static final class SettlementProbe {

        private final GameTestHelper levelHelper;
        private final ServerPlayer attacker;
        private final List<DamageSettlementSnapshot> snapshots =
                new ArrayList<>();
        private LivingEntity primaryTarget;
        private LivingEntity childTarget;
        private LivingEntity lateCancelTarget;
        private LivingEntity preZeroTarget;
        private LivingEntity throwingListenerTarget;
        private LivingEntity listenerFailureQueuedTarget;
        private LivingEntity throwingCallbackTarget;
        private int throwingCallbackInvocations;
        private boolean callbackAfterFailureObserved;
        private LivingEntity providerRollbackTarget;
        private LivingEntity processorRollbackTarget;
        private int providerFailureInvocations;
        private int processorFailureInvocations;
        private int equipmentRuleInvocations;
        private LivingEntity proxyDirectEntity;
        private Entity proxyResolvedDirectEntity;
        private LivingEntity invalidProxyDirectEntity;
        private LivingEntity removedResolverOwner;
        private LivingEntity proxyPublicTarget;
        private Entity observedPublicDirect;
        private Entity observedPublicLogical;
        private int throwingResolverInvocations;
        private int preferredResolverInvocations;
        private int conflictingResolverInvocations;
        private int invalidResolverInvocations;
        private LivingEntity structurallyInvalidCandidate;
        private int structurallyInvalidResolverInvocations;
        private int proxyEquipmentRuleInvocations;
        private boolean externalSourcesActive;
        private boolean mutateExternalAfterPreferred;
        private boolean externalDistinctPhysical;
        private ItemStack externalOffensiveStack = ItemStack.EMPTY;
        private ItemStack externalDefensiveStack = ItemStack.EMPTY;
        private ItemStack externalSecondStack = ItemStack.EMPTY;
        private EquippedItemRuleSourceCategory externalCategory =
                EquippedItemRuleSourceCategory.ITEM;
        private boolean externalReadEntries = true;
        private boolean externalReadAffixes = true;
        private int externalOffensiveQueries;
        private int externalDefensiveQueries;
        private int externalOffensiveRuleInvocations;
        private int externalDefensiveRuleInvocations;
        private int externalSelfRuleInvocations;
        private int externalEntryModeInvocations;
        private int externalAffixModeInvocations;
        private int phase6ConditionInvocations;
        private LivingEntity criticalForceTarget;
        private LivingEntity criticalSuppressTarget;
        private LivingEntity criticalThrowingTarget;
        private int criticalForceInvocations;
        private int criticalSuppressInvocations;
        private int criticalThrowingInvocations;
        private int phase7CriticalRuleInvocations;
        private CriticalDecisionCollector retainedCriticalCollector;
        private int capturedProjectileRuleInvocations;
        private int externalProjectileRuleInvocations;
        private LivingEntity externalThrowingTarget;
        private int externalThrowingInvocations;
        private LivingEntity sharedBudgetParentTarget;
        private LivingEntity sharedBudgetFirstTarget;
        private LivingEntity sharedBudgetSecondTarget;
        private final List<DamageResult> sharedBudgetResults =
                new ArrayList<>();
        private final Deque<LivingEntity> unfilteredProcTargets =
                new ArrayDeque<>();
        private final List<DamageResult> unfilteredProcResults =
                new ArrayList<>();
        private boolean unfilteredProcLoopActive;
        private long unfilteredProcRootId;
        private LivingEntity crossKindRootTarget;
        private LivingEntity crossKindProcTarget;
        private LivingEntity crossKindReflectedTarget;
        private LivingEntity crossKindRejectedTarget;
        private final List<DamageResult> crossKindResults =
                new ArrayList<>();
        private boolean crossKindLoopActive;
        private LivingEntity tickResetTarget;
        private int tickBudgetTestTick;
        private int tickBudgetLimit;
        private boolean childSubmitted;
        private DamageResult childResult;
        private Consumer<DamageSettlementCallback> settlementAction;
        private DamageSettledEvent officialParentEvent;
        private DamageSettlementCallback officialParentCallback;
        private DamageSettledEvent manualRepostEvent;
        private boolean manualRepostObserved;
        private DamageParentRef expiredAuthority;
        private LivingEntity rootDuringDispatchTarget;
        private DamageResult rootDuringDispatchResult;
        private boolean repostContractActive;
        private boolean repostInProgress;
        private boolean nestedRepostObserved;
        private int repostObserverInvocations;
        private int repostCallbackInvocations;
        private LivingEntity repostParentTarget;
        private LivingEntity repostNestedTarget;
        private LivingEntity repostChildTarget;
        private DamageResult nestedRootResult;
        private DamageResult repostChildResult;
        private DamageParentRef repostAuthority;
        private DamageSettlementCallback repostCallback;
        private LivingEntity crossCallbackParentTarget;
        private LivingEntity crossCallbackLegalTargetA;
        private LivingEntity crossCallbackStaleTargetAFromB;
        private LivingEntity crossCallbackLegalTargetB;
        private LivingEntity crossCallbackStaleTargetAFromC;
        private LivingEntity crossCallbackStaleTargetBFromC;
        private LivingEntity crossCallbackLegalTargetC;
        private DamageSettlementCallback crossCallbackA;
        private DamageSettlementCallback crossCallbackB;
        private DamageSettlementCallback crossCallbackC;
        private DamageParentRef crossCallbackRefA;
        private DamageParentRef crossCallbackRefB;
        private DamageParentRef crossCallbackRefC;
        private DamageResult crossCallbackLegalA;
        private DamageResult crossCallbackStaleAFromB;
        private DamageResult crossCallbackLegalB;
        private DamageResult crossCallbackStaleAFromC;
        private DamageResult crossCallbackStaleBFromC;
        private DamageResult crossCallbackLegalC;
        private LivingEntity crossCallbackThrowingParentTarget;
        private LivingEntity crossCallbackThrowingStaleTarget;
        private LivingEntity crossCallbackThrowingLegalTarget;
        private DamageSettlementCallback crossCallbackThrowingA;
        private DamageSettlementCallback crossCallbackThrowingB;
        private DamageParentRef crossCallbackThrowingRefA;
        private DamageParentRef crossCallbackThrowingRefB;
        private DamageResult crossCallbackThrowingStaleA;
        private DamageResult crossCallbackThrowingLegalB;
        private boolean authorityContractActive;
        private LivingEntity activeIncomingTarget;
        private LivingEntity activePreTarget;
        private LivingEntity activePostTarget;
        private LivingEntity activeScopeNestedTarget;
        private DamageResult activeIncomingResult;
        private DamageResult activePreResult;
        private DamageResult activePostResult;
        private DamageResult activeTransactionResult;
        private LivingEntity observationNativeParentTarget;
        private LivingEntity observationNativeTarget;
        private boolean observationNativeAttempted;
        private boolean observationNativeAccepted;
        private LivingEntity callbackNativeParentTarget;
        private LivingEntity callbackNativeTarget;
        private LivingEntity callbackLegalChildTarget;
        private boolean callbackNativeAttempted;
        private boolean callbackNativeAccepted;
        private DamageResult callbackLegalChildResult;
        private boolean unconditionalNativeRootAttemptActive;
        private LivingEntity unconditionalNativeRootTarget;
        private int unconditionalNativeRootAttempts;
        private boolean unconditionalNativeRootAccepted;
        private LivingEntity fifoParentTarget;
        private LivingEntity fifoNativeTarget;
        private boolean fifoNestedNativeAttempted;
        private boolean fifoNativeAccepted;
        private DamageRequest inheritedRequest;
        private DamageRequest inheritedThenExplicit;
        private DamageRequest explicitThenInherited;
        private boolean triggerPolicyOrderVerified;
        private String failure;
        private int nextSpawnX = 23;

        private SettlementProbe(GameTestHelper helper) {
            this.levelHelper = helper;
            this.attacker = GameTestServerPlayerFactory.create(helper);
        }

        private LivingEntity spawn(int x) {
            return levelHelper.spawnWithNoFreeWill(
                    EntityType.ZOMBIE,
                    new BlockPos(x, 2, 1)
            );
        }

        private LivingEntity spawn() {
            LivingEntity entity = spawn(nextSpawnX);
            nextSpawnX += 2;
            return entity;
        }

        private LivingEntity createUnadded() {
            LivingEntity entity = EntityType.ZOMBIE.create(
                    levelHelper.getLevel(),
                    EntitySpawnReason.COMMAND
            );
            if (entity == null) {
                throw new AssertionError(
                        "Unable to create unattached entity fixture"
                );
            }
            return entity;
        }

        private void clear() {
            snapshots.clear();
            primaryTarget = null;
            childTarget = null;
            childSubmitted = false;
            childResult = null;
            settlementAction = null;
            officialParentEvent = null;
            officialParentCallback = null;
            manualRepostEvent = null;
            manualRepostObserved = false;
            expiredAuthority = null;
            rootDuringDispatchTarget = null;
            rootDuringDispatchResult = null;
            repostContractActive = false;
            repostInProgress = false;
            nestedRepostObserved = false;
            repostObserverInvocations = 0;
            repostCallbackInvocations = 0;
            repostParentTarget = null;
            repostNestedTarget = null;
            repostChildTarget = null;
            nestedRootResult = null;
            repostChildResult = null;
            repostAuthority = null;
            repostCallback = null;
            crossCallbackParentTarget = null;
            crossCallbackLegalTargetA = null;
            crossCallbackStaleTargetAFromB = null;
            crossCallbackLegalTargetB = null;
            crossCallbackStaleTargetAFromC = null;
            crossCallbackStaleTargetBFromC = null;
            crossCallbackLegalTargetC = null;
            crossCallbackA = null;
            crossCallbackB = null;
            crossCallbackC = null;
            crossCallbackRefA = null;
            crossCallbackRefB = null;
            crossCallbackRefC = null;
            crossCallbackLegalA = null;
            crossCallbackStaleAFromB = null;
            crossCallbackLegalB = null;
            crossCallbackStaleAFromC = null;
            crossCallbackStaleBFromC = null;
            crossCallbackLegalC = null;
            crossCallbackThrowingParentTarget = null;
            crossCallbackThrowingStaleTarget = null;
            crossCallbackThrowingLegalTarget = null;
            crossCallbackThrowingA = null;
            crossCallbackThrowingB = null;
            crossCallbackThrowingRefA = null;
            crossCallbackThrowingRefB = null;
            crossCallbackThrowingStaleA = null;
            crossCallbackThrowingLegalB = null;
            throwingListenerTarget = null;
            listenerFailureQueuedTarget = null;
            throwingCallbackTarget = null;
            throwingCallbackInvocations = 0;
            callbackAfterFailureObserved = false;
            authorityContractActive = false;
            activeIncomingTarget = null;
            activePreTarget = null;
            activePostTarget = null;
            activeScopeNestedTarget = null;
            activeIncomingResult = null;
            activePreResult = null;
            activePostResult = null;
            activeTransactionResult = null;
            observationNativeParentTarget = null;
            observationNativeTarget = null;
            observationNativeAttempted = false;
            observationNativeAccepted = false;
            callbackNativeParentTarget = null;
            callbackNativeTarget = null;
            callbackLegalChildTarget = null;
            callbackNativeAttempted = false;
            callbackNativeAccepted = false;
            callbackLegalChildResult = null;
            unconditionalNativeRootAttemptActive = false;
            unconditionalNativeRootTarget = null;
            unconditionalNativeRootAttempts = 0;
            unconditionalNativeRootAccepted = false;
            fifoParentTarget = null;
            fifoNativeTarget = null;
            fifoNestedNativeAttempted = false;
            fifoNativeAccepted = false;
            inheritedRequest = null;
            inheritedThenExplicit = null;
            explicitThenInherited = null;
            triggerPolicyOrderVerified = false;
        }

        private void fail(String message) {
            if (failure == null) {
                failure = message;
            }
        }

        private void assertNoFailure() {
            if (failure != null) {
                throw new AssertionError(failure);
            }
        }
    }
}
