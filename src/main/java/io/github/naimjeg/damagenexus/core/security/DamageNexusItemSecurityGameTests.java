package io.github.naimjeg.damagenexus.core.security;

import io.github.naimjeg.damagenexus.DamageNexus;
import io.github.naimjeg.damagenexus.api.display.DisplayText;
import io.github.naimjeg.damagenexus.api.enums.DamageChannel;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import io.github.naimjeg.damagenexus.api.event.DamageNexusRegisterEvent;
import io.github.naimjeg.damagenexus.api.item.template.DamageEntryTemplateReference;
import io.github.naimjeg.damagenexus.api.item.template.DamageItemTemplateReferences;
import io.github.naimjeg.damagenexus.api.rule.DamageNexusOperations;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleCondition;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleDefinition;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleLimits;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleOperation;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleRole;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleStacking;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixDefinition;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixDisplay;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixRarity;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixSlot;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixStacking;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryDefinition;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryDisplay;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntrySlot;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryStacking;
import io.github.naimjeg.damagenexus.builtin.rule.condition.AlwaysCondition;
import io.github.naimjeg.damagenexus.builtin.rule.condition.AllOfCondition;
import io.github.naimjeg.damagenexus.builtin.rule.condition.NotCondition;
import io.github.naimjeg.damagenexus.core.gametest.GameTestCodecVerifier;
import io.github.naimjeg.damagenexus.core.gametest.GameTestServerPlayerFactory;
import io.github.naimjeg.damagenexus.core.registry.DamageChannelRegistry;
import io.github.naimjeg.damagenexus.registry.ModDataComponents;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHooks;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@EventBusSubscriber(modid = DamageNexus.MODID)
final class DamageNexusItemSecurityGameTests {

    private static final String GAMETEST_RUNTIME_PROPERTY =
            "damagenexus.gametest.runtime";
    private static final Identifier SECURITY_TEMPLATE_ID =
            id("gametest_security_dangerous_template");

    private static final ResourceKey<Consumer<GameTestHelper>>
            ITEM_SECURITY_SERVICE_FUNCTION = functionKey(
            "creative_item_security_service"
    );
    private static final ResourceKey<Consumer<GameTestHelper>>
            CREATIVE_PACKET_INGRESS_FUNCTION = functionKey(
            "creative_packet_ingress_security"
    );

    private DamageNexusItemSecurityGameTests() {
    }

    @SubscribeEvent
    public static void registerTemplate(DamageNexusRegisterEvent event) {
        if (Boolean.getBoolean(GAMETEST_RUNTIME_PROPERTY)) {
            event.registerEntryTemplate(
                    SECURITY_TEMPLATE_ID,
                    new DamageEntryDefinition(
                            SECURITY_TEMPLATE_ID,
                            DamageEntryDisplay.EMPTY,
                            DamageEntrySlot.ITEM,
                            List.of(rule(
                                    "gametest_security_template_rule",
                                    List.of(),
                                    List.of(DamageNexusOperations
                                            .cancelDamage()),
                                    DamagePhase.FINAL_OVERRIDE)),
                            DamageEntryStacking.STACK,
                            Optional.empty()));
        }
    }

    @SubscribeEvent
    public static void registerTestFunctions(RegisterEvent event) {
        if (!GameTestHooks.isGametestEnabled()) {
            return;
        }
        event.register(
                Registries.TEST_FUNCTION,
                ITEM_SECURITY_SERVICE_FUNCTION.identifier(),
                () -> DamageNexusItemSecurityGameTests
                        ::creativeItemSecurityService
        );
        event.register(
                Registries.TEST_FUNCTION,
                CREATIVE_PACKET_INGRESS_FUNCTION.identifier(),
                () -> DamageNexusItemSecurityGameTests
                        ::creativePacketIngressSecurity
        );
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment =
                event.registerEnvironment(
                        id("item_security_environment"),
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
                id("creative_item_security_service"),
                new FunctionGameTestInstance(
                        ITEM_SECURITY_SERVICE_FUNCTION,
                        data
                )
        );
        event.registerTest(
                id("creative_packet_ingress_security"),
                new FunctionGameTestInstance(
                        CREATIVE_PACKET_INGRESS_FUNCTION,
                        data
                )
        );
    }

    private static void creativeItemSecurityService(GameTestHelper helper) {
        GameTestCodecVerifier.verifyFunctionInstance(helper);
        DamageNexus.LOGGER.info(
                "[DamageNexus] Executing GameTest {}",
                ITEM_SECURITY_SERVICE_FUNCTION.identifier()
        );
        verifyNonAdministratorEntries();
        verifyNonAdministratorAffixes();
        verifyTemplateReferenceIngress();
        verifyAdministratorLegalRule();
        verifyAdministratorInvalidRules();
        verifyCachedGraphCannotFollowCallerMutation();
        verifyActualItemStackCacheRevisionIdentity();
        helper.succeed();
    }

    private static void verifyActualItemStackCacheRevisionIdentity() {
        try {
            Class<?> cacheEntry = java.util.Arrays.stream(
                            DamageNexusItemSecurity.class.getDeclaredClasses())
                    .filter(type -> type.getSimpleName().equals("CacheEntry"))
                    .findFirst().orElseThrow();
            var constructor = cacheEntry.getDeclaredConstructors()[0];
            constructor.setAccessible(true);
            var matches = java.util.Arrays.stream(
                            cacheEntry.getDeclaredMethods())
                    .filter(method -> method.getName().equals("matches"))
                    .findFirst().orElseThrow();
            matches.setAccessible(true);

            ItemStack stack = new ItemStack(Items.STICK);
            List<DamageEntryDefinition> entries = List.of();
            List<DamageAffixDefinition> affixes = List.of();
            DamageItemTemplateReferences references =
                    DamageItemTemplateReferences.EMPTY;
            long revision = DamageChannelRegistry.contentRevision();
            Object cached = constructor.newInstance(
                    new java.lang.ref.WeakReference<>(stack),
                    entries,
                    affixes,
                    references,
                    11L,
                    12L,
                    true,
                    revision,
                    DamageNexusItemSecurity.validateDefinitions(
                            entries, affixes, "gametest_cache_revision")
            );

            if (!(boolean) matches.invoke(
                    cached, stack, entries, affixes, references,
                    11L, 12L, true, revision)
                    || (boolean) matches.invoke(
                    cached, stack, entries, affixes, references,
                    11L, 12L, true, revision + 1L)
                    || (boolean) matches.invoke(
                    cached, stack, entries, affixes, references,
                    11L, 12L, true, revision + 2L)) {
                throw new AssertionError(
                        "The same ItemStack reused a cache entry across channel revisions"
                );
            }
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(
                    "Unable to inspect execution-cache revision identity",
                    exception
            );
        }
    }

    /**
     * Integration coverage for the production packet handler. This deliberately
     * does not call either sanitizeCreativeInbound overload: packet.handle
     * dispatches to the mixin-transformed ServerGamePacketListenerImpl.
     */
    private static void creativePacketIngressSecurity(
            GameTestHelper helper
    ) {
        GameTestCodecVerifier.verifyFunctionInstance(helper);
        DamageNexus.LOGGER.info(
                "[DamageNexus] Executing GameTest {}",
                CREATIVE_PACKET_INGRESS_FUNCTION.identifier()
        );
        ServerPlayer player = GameTestServerPlayerFactory.create(helper);

        try {
            verifyRealCreativePacketIngress(helper, player);
            DamageNexus.LOGGER.info(
                    "[DamageNexus] GameTest verified real "
                            + "ServerGamePacketListenerImpl#"
                            + "handleSetCreativeModeSlot ingress on "
                            + "the server thread."
            );
            helper.succeed();
        } finally {
            helper.getLevel().getServer().getPlayerList()
                    .deop(player.nameAndId());
        }
    }

    private static void verifyRealCreativePacketIngress(
            GameTestHelper helper,
            ServerPlayer player
    ) {
        if (!helper.getLevel().getServer().isSameThread()) {
            throw new AssertionError(
                    "Creative packet integration test is not on the server thread"
            );
        }

        player.getAbilities().instabuild = true;
        setAdministrator(player, false);
        assertPermission(player, false);

        assertHandlerStrips(player, stackWithEntries("packet_entry"));
        assertHandlerStrips(player, stackWithAffixes("packet_affix"));
        assertHandlerStrips(player, stackWithTemplateReference(
                SECURITY_TEMPLATE_ID));

        ItemStack both = stackWithEntries("packet_both_entry");
        both.set(
                ModDataComponents.DAMAGE_AFFIXES.get(),
                List.of(affix(
                        "packet_both_affix",
                        entry("packet_both_nested", validRule("packet_both_rule"))
                ))
        );
        assertHandlerStrips(player, both);

        setAdministrator(player, true);
        assertPermission(player, true);
        assertHandlerPreserves(player, stackWithEntries("packet_admin_legal"));
        assertHandlerPreservesTemplateReference(
                player,
                stackWithTemplateReference(SECURITY_TEMPLATE_ID));
        assertHandlerPreservesTemplateReference(
                player,
                stackWithTemplateReference(id("unknown_template_reference")));

        ItemStack mixedOverflow = stackWithEntries("packet_mixed_entry");
        mixedOverflow.set(
                ModDataComponents.DAMAGE_TEMPLATE_REFERENCES.get(),
                new DamageItemTemplateReferences(
                        java.util.stream.IntStream.range(
                                        0,
                                        DamageItemTemplateReferences
                                                .MAX_ENTRY_REFERENCES)
                                .mapToObj(index ->
                                        new DamageEntryTemplateReference(
                                                SECURITY_TEMPLATE_ID))
                                .toList(),
                        List.of()));
        assertHandlerStrips(player, mixedOverflow);

        assertHandlerStrips(player, stackWithAggregateRuleOverflow());
        assertHandlerStrips(player, stackWithOverwideConditions());
        assertHandlerStrips(player, stackWithOverdeepCondition());
        assertHandlerStrips(player, stackWithInvalidNumber(Float.NaN));
        assertHandlerStrips(
                player,
                stackWithInvalidNumber(Float.POSITIVE_INFINITY)
        );
        assertHandlerStrips(player, stackWithIllegalOperation());
        assertHandlerStrips(player, stackWithValidAndInvalidRules());

        verifyVanillaBehaviorWithoutExecutableComponents(player);
        verifyNonCreativePlayerCannotWriteSlot(player);
    }

    private static void assertHandlerStrips(
            ServerPlayer player,
            ItemStack submitted
    ) {
        ServerboundSetCreativeModeSlotPacket packet =
                new ServerboundSetCreativeModeSlotPacket(36, submitted);

        packet.handle(player.connection);

        ItemStack written = player.inventoryMenu.getSlot(36).getItem();

        if (written != packet.itemStack()) {
            throw new AssertionError(
                    "Vanilla did not write the sanitized packet ItemStack instance"
            );
        }

        assertDamageComponentsRemovedAndOrdinaryPreserved(written);
    }

    private static void assertHandlerPreserves(
            ServerPlayer player,
            ItemStack submitted
    ) {
        ServerboundSetCreativeModeSlotPacket packet =
                new ServerboundSetCreativeModeSlotPacket(36, submitted);

        packet.handle(player.connection);

        ItemStack written = player.inventoryMenu.getSlot(36).getItem();

        if (written != packet.itemStack()
                || !written.has(ModDataComponents.DAMAGE_ENTRIES.get())
                || !ordinaryComponentsPreserved(written)) {
            throw new AssertionError(
                    "Legal administrator packet was not preserved"
            );
        }
    }

    private static void assertHandlerPreservesTemplateReference(
            ServerPlayer player,
            ItemStack submitted
    ) {
        new ServerboundSetCreativeModeSlotPacket(36, submitted)
                .handle(player.connection);
        ItemStack written = player.inventoryMenu.getSlot(36).getItem();
        if (written != submitted
                || !written.has(ModDataComponents
                .DAMAGE_TEMPLATE_REFERENCES.get())
                || !ordinaryComponentsPreserved(written)) {
            throw new AssertionError(
                    "Legal administrator template reference was not preserved");
        }
    }

    private static void verifyVanillaBehaviorWithoutExecutableComponents(
            ServerPlayer player
    ) {
        ItemStack ordinary = stackWithOrdinaryComponents();
        ServerboundSetCreativeModeSlotPacket packet =
                new ServerboundSetCreativeModeSlotPacket(36, ordinary);

        packet.handle(player.connection);

        if (player.inventoryMenu.getSlot(36).getItem() != ordinary
                || !ordinaryComponentsPreserved(ordinary)) {
            throw new AssertionError(
                    "Rule-free creative packet no longer follows vanilla behavior"
            );
        }
    }

    private static void verifyNonCreativePlayerCannotWriteSlot(
            ServerPlayer player
    ) {
        ItemStack previous = new ItemStack(Items.DIRT);
        player.inventoryMenu.getSlot(36).set(previous);
        player.getAbilities().instabuild = false;
        ItemStack submitted = stackWithOrdinaryComponents();

        new ServerboundSetCreativeModeSlotPacket(36, submitted)
                .handle(player.connection);

        if (player.inventoryMenu.getSlot(36).getItem() != previous) {
            throw new AssertionError(
                    "Non-creative player unexpectedly wrote a creative slot"
            );
        }

        player.getAbilities().instabuild = true;
    }

    private static void setAdministrator(
            ServerPlayer player,
            boolean administrator
    ) {
        var playerList = player.level().getServer().getPlayerList();

        if (administrator) {
            playerList.op(
                    player.nameAndId(),
                    Optional.of(LevelBasedPermissionSet.GAMEMASTER),
                    Optional.empty()
            );
        } else {
            playerList.deop(player.nameAndId());
        }
    }

    private static void assertPermission(
            ServerPlayer player,
            boolean expected
    ) {
        boolean actual = player.permissions().hasPermission(
                Permissions.COMMANDS_GAMEMASTER
        );

        if (actual != expected) {
            throw new AssertionError(
                    "Unexpected server-side administrator permission: " + actual
            );
        }
    }

    private static void verifyNonAdministratorEntries() {
        ItemStack stack = stackWithOrdinaryComponent();
        stack.set(
                ModDataComponents.DAMAGE_ENTRIES.get(),
                List.of(entry("untrusted_entry", validRule("entry_rule")))
        );

        assertDecision(
                DamageNexusItemSecurity.InboundDecision.STRIP_UNTRUSTED,
                DamageNexusItemSecurity.sanitizeCreativeInbound(
                        false,
                        stack
                )
        );
        assertDamageComponentsRemovedAndOrdinaryPreserved(stack);
    }

    private static void verifyNonAdministratorAffixes() {
        ItemStack stack = stackWithOrdinaryComponent();
        stack.set(
                ModDataComponents.DAMAGE_AFFIXES.get(),
                List.of(affix(
                        "untrusted_affix",
                        entry("nested", validRule("nested_rule"))
                ))
        );

        assertDecision(
                DamageNexusItemSecurity.InboundDecision.STRIP_UNTRUSTED,
                DamageNexusItemSecurity.sanitizeCreativeInbound(
                        false,
                        stack
                )
        );
        assertDamageComponentsRemovedAndOrdinaryPreserved(stack);
    }

    private static void verifyTemplateReferenceIngress() {
        ItemStack untrusted = stackWithTemplateReference(SECURITY_TEMPLATE_ID);
        assertDecision(
                DamageNexusItemSecurity.InboundDecision.STRIP_UNTRUSTED,
                DamageNexusItemSecurity.sanitizeCreativeInbound(
                        false, untrusted));
        assertDamageComponentsRemovedAndOrdinaryPreserved(untrusted);

        ItemStack administrator = stackWithTemplateReference(
                SECURITY_TEMPLATE_ID);
        assertDecision(
                DamageNexusItemSecurity.InboundDecision.ALLOW,
                DamageNexusItemSecurity.sanitizeCreativeInbound(
                        true, administrator));
        if (!administrator.has(ModDataComponents
                .DAMAGE_TEMPLATE_REFERENCES.get())) {
            throw new AssertionError(
                    "Administrator template reference was removed");
        }

        ItemStack unresolved = stackWithTemplateReference(
                id("gametest_unknown_reference"));
        assertDecision(
                DamageNexusItemSecurity.InboundDecision.ALLOW,
                DamageNexusItemSecurity.sanitizeCreativeInbound(
                        true, unresolved));
    }

    private static void verifyAdministratorLegalRule() {
        ItemStack stack = stackWithOrdinaryComponent();
        stack.set(
                ModDataComponents.DAMAGE_ENTRIES.get(),
                List.of(entry("legal", validRule("legal_rule")))
        );

        assertDecision(
                DamageNexusItemSecurity.InboundDecision.ALLOW,
                DamageNexusItemSecurity.sanitizeCreativeInbound(
                        true,
                        stack
                )
        );

        if (!stack.has(ModDataComponents.DAMAGE_ENTRIES.get())
                || !ordinaryComponentPreserved(stack)) {
            throw new AssertionError(
                    "Legal administrator item was modified"
            );
        }
    }

    private static void verifyAdministratorInvalidRules() {
        List<DamageRuleDefinition> tooMany = new ArrayList<>();

        for (int index = 0;
                index <= DamageRuleLimits.MAX_ENTRY_RULES;
                index++) {
            tooMany.add(validRule("over_" + index));
        }

        assertAdministratorInvalid(entryWithRules(
                "over_limit",
                tooMany
        ));

        DamageRuleCondition condition = new AlwaysCondition();

        for (int depth = 1;
                depth < DamageRuleLimits.MAX_CONDITION_DEPTH + 1;
                depth++) {
            condition = new NotCondition(condition);
        }

        assertAdministratorInvalid(entry(
                "over_depth",
                rule(
                        "over_depth_rule",
                        List.of(condition),
                        List.of(DamageNexusOperations.addBaseDamage(
                                DamageChannel.UNTYPED_ID,
                                1.0f
                        ))
                )
        ));
        assertAdministratorInvalid(entry(
                "nan",
                rule(
                        "nan_rule",
                        List.of(),
                        List.of(DamageNexusOperations.addBaseDamage(
                                DamageChannel.UNTYPED_ID,
                                Float.NaN
                        ))
                )
        ));
    }

    private static void verifyCachedGraphCannotFollowCallerMutation() {
        List<DamageRuleCondition> nestedChildren = new ArrayList<>();
        nestedChildren.add(new AlwaysCondition());
        List<DamageRuleCondition> sourceConditions =
                new ArrayList<>();
        sourceConditions.add(new AllOfCondition(nestedChildren));
        DamageRuleDefinition rule = rule(
                "immutable_cached_rule",
                sourceConditions,
                List.of(DamageNexusOperations.addBaseDamage(
                        DamageChannel.UNTYPED_ID,
                        1.0f
                ))
        );
        List<DamageRuleDefinition> sourceRules =
                new ArrayList<>(List.of(rule));
        DamageEntryDefinition entry = entryWithRules(
                "immutable_cached_entry",
                sourceRules
        );
        List<DamageEntryDefinition> sourceEntries =
                new ArrayList<>(List.of(entry));
        ItemStack stack = stackWithOrdinaryComponent();
        stack.set(
                ModDataComponents.DAMAGE_ENTRIES.get(),
                sourceEntries
        );

        var first = DamageNexusItemSecurity.validateForExecution(
                stack,
                "gametest/cache"
        );
        sourceConditions.clear();
        nestedChildren.clear();
        nestedChildren.add(new NotCondition(new AlwaysCondition()));
        sourceRules.clear();
        sourceEntries.clear();
        var second = DamageNexusItemSecurity.validateForExecution(
                stack,
                "gametest/cache_after_mutation"
        );

        if (!first.authoritative()
                || first.entries().size() != 1
                || second.entries().size() != 1
                || second.entries().getFirst().rules().getFirst()
                .conditions().size() != 1
                || !(second.entries().getFirst().rules().getFirst()
                .conditions().getFirst() instanceof AllOfCondition allOf)
                || allOf.conditions().size() != 1
                || !(allOf.conditions().getFirst()
                instanceof AlwaysCondition)) {
            throw new AssertionError(
                    "Cached executable graph followed caller mutation"
            );
        }
    }

    private static void assertAdministratorInvalid(
            DamageEntryDefinition entry
    ) {
        ItemStack stack = stackWithOrdinaryComponent();
        stack.set(
                ModDataComponents.DAMAGE_ENTRIES.get(),
                List.of(entry)
        );

        assertDecision(
                DamageNexusItemSecurity.InboundDecision.STRIP_INVALID,
                DamageNexusItemSecurity.sanitizeCreativeInbound(
                        true,
                        stack
                )
        );
        assertDamageComponentsRemovedAndOrdinaryPreserved(stack);
    }

    private static ItemStack stackWithEntries(String path) {
        ItemStack stack = stackWithOrdinaryComponents();
        stack.set(
                ModDataComponents.DAMAGE_ENTRIES.get(),
                List.of(entry(path, validRule(path + "_rule")))
        );
        return stack;
    }

    private static ItemStack stackWithAffixes(String path) {
        ItemStack stack = stackWithOrdinaryComponents();
        stack.set(
                ModDataComponents.DAMAGE_AFFIXES.get(),
                List.of(affix(
                        path,
                        entry(path + "_entry", validRule(path + "_rule"))
                ))
        );
        return stack;
    }

    private static ItemStack stackWithTemplateReference(Identifier id) {
        ItemStack stack = stackWithOrdinaryComponents();
        stack.set(
                ModDataComponents.DAMAGE_TEMPLATE_REFERENCES.get(),
                new DamageItemTemplateReferences(
                        List.of(new DamageEntryTemplateReference(id)),
                        List.of()));
        return stack;
    }

    private static ItemStack stackWithAggregateRuleOverflow() {
        List<DamageEntryDefinition> entries = new ArrayList<>();

        for (int entryIndex = 0; entryIndex < 5; entryIndex++) {
            List<DamageRuleDefinition> rules = new ArrayList<>();

            for (int ruleIndex = 0;
                    ruleIndex < DamageRuleLimits.MAX_ENTRY_RULES;
                    ruleIndex++) {
                rules.add(validRule(
                        "packet_aggregate_"
                                + entryIndex
                                + "_"
                                + ruleIndex
                ));
            }

            entries.add(entryWithRules(
                    "packet_aggregate_entry_" + entryIndex,
                    rules
            ));
        }

        ItemStack stack = stackWithOrdinaryComponents();
        stack.set(ModDataComponents.DAMAGE_ENTRIES.get(), entries);
        return stack;
    }

    private static ItemStack stackWithOverwideConditions() {
        return stackWithRule(
                "packet_overwide",
                rule(
                        "packet_overwide_rule",
                        java.util.Collections.nCopies(
                                DamageRuleLimits.MAX_RULE_CONDITIONS + 1,
                                new AlwaysCondition()
                        ),
                        List.of(DamageNexusOperations.addBaseDamage(
                                DamageChannel.UNTYPED_ID,
                                1.0f
                        ))
                )
        );
    }

    private static ItemStack stackWithOverdeepCondition() {
        DamageRuleCondition condition = new AlwaysCondition();

        for (int depth = 1;
                depth < DamageRuleLimits.MAX_CONDITION_DEPTH + 1;
                depth++) {
            condition = new NotCondition(condition);
        }

        return stackWithRule(
                "packet_overdeep",
                rule(
                        "packet_overdeep_rule",
                        List.of(condition),
                        List.of(DamageNexusOperations.addBaseDamage(
                                DamageChannel.UNTYPED_ID,
                                1.0f
                        ))
                )
        );
    }

    private static ItemStack stackWithInvalidNumber(float value) {
        return stackWithRule(
                "packet_invalid_number_" + Float.floatToRawIntBits(value),
                rule(
                        "packet_invalid_number_rule_"
                                + Float.floatToRawIntBits(value),
                        List.of(),
                        List.of(DamageNexusOperations.addBaseDamage(
                                DamageChannel.UNTYPED_ID,
                                value
                        ))
                )
        );
    }

    private static ItemStack stackWithIllegalOperation() {
        return stackWithRule(
                "packet_illegal_operation",
                rule(
                        "packet_illegal_operation_rule",
                        List.of(),
                        List.of(DamageNexusOperations.cancelDamage())
                )
        );
    }

    private static ItemStack stackWithValidAndInvalidRules() {
        ItemStack stack = stackWithOrdinaryComponents();
        stack.set(
                ModDataComponents.DAMAGE_ENTRIES.get(),
                List.of(
                        entry("packet_partial_valid", validRule(
                                "packet_partial_valid_rule"
                        )),
                        entry(
                                "packet_partial_invalid",
                                rule(
                                        "packet_partial_invalid_rule",
                                        List.of(),
                                        List.of(DamageNexusOperations
                                                .cancelDamage())
                                )
                        )
                )
        );
        return stack;
    }

    private static ItemStack stackWithRule(
            String path,
            DamageRuleDefinition rule
    ) {
        ItemStack stack = stackWithOrdinaryComponents();
        stack.set(
                ModDataComponents.DAMAGE_ENTRIES.get(),
                List.of(entry(path, rule))
        );
        return stack;
    }

    private static ItemStack stackWithOrdinaryComponents() {
        return stackWithOrdinaryComponent();
    }

    private static ItemStack stackWithOrdinaryComponent() {
        ItemStack stack = new ItemStack(Items.STONE);
        stack.set(
                DataComponents.CUSTOM_NAME,
                Component.literal("ordinary component")
        );
        CompoundTag thirdPartyData = new CompoundTag();
        thirdPartyData.putString("third_party_component", "preserved");
        stack.set(
                DataComponents.CUSTOM_DATA,
                CustomData.of(thirdPartyData)
        );
        return stack;
    }

    private static boolean ordinaryComponentPreserved(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);

        return Component.literal("ordinary component").equals(
                stack.get(DataComponents.CUSTOM_NAME)
        )
                && customData != null
                && customData.contains("third_party_component");
    }

    private static boolean ordinaryComponentsPreserved(ItemStack stack) {
        return ordinaryComponentPreserved(stack);
    }

    private static void
    assertDamageComponentsRemovedAndOrdinaryPreserved(ItemStack stack) {
        if (stack.has(ModDataComponents.DAMAGE_ENTRIES.get())
                || stack.has(ModDataComponents.DAMAGE_AFFIXES.get())
                || stack.has(ModDataComponents.DAMAGE_TEMPLATE_REFERENCES.get())
                || !ordinaryComponentPreserved(stack)) {
            throw new AssertionError(
                    "Sanitization removed unrelated components or kept "
                            + "executable DamageNexus data"
            );
        }
    }

    private static void assertDecision(
            DamageNexusItemSecurity.InboundDecision expected,
            DamageNexusItemSecurity.InboundDecision actual
    ) {
        if (expected != actual) {
            throw new AssertionError(
                    "Expected " + expected + " but got " + actual
            );
        }
    }

    private static DamageEntryDefinition entry(
            String path,
            DamageRuleDefinition rule
    ) {
        return entryWithRules(path, List.of(rule));
    }

    private static DamageEntryDefinition entryWithRules(
            String path,
            List<DamageRuleDefinition> rules
    ) {
        return new DamageEntryDefinition(
                id(path),
                DamageEntryDisplay.EMPTY,
                DamageEntrySlot.ITEM,
                rules,
                DamageEntryStacking.STACK,
                Optional.empty()
        );
    }

    private static DamageAffixDefinition affix(
            String path,
            DamageEntryDefinition entry
    ) {
        return new DamageAffixDefinition(
                id(path),
                DamageAffixDisplay.EMPTY,
                DamageAffixSlot.ITEM,
                DamageAffixRarity.COMMON,
                List.of(entry),
                DamageAffixStacking.STACK,
                Optional.empty()
        );
    }

    private static DamageRuleDefinition validRule(String path) {
        return rule(
                path,
                List.of(),
                List.of(DamageNexusOperations.addBaseDamage(
                        DamageChannel.UNTYPED_ID,
                        1.0f
                ))
        );
    }

    private static DamageRuleDefinition rule(
            String path,
            List<DamageRuleCondition> conditions,
            List<? extends DamageRuleOperation> operations
    ) {
        return rule(path, conditions, operations,
                DamagePhase.BASE_MODIFICATION);
    }

    private static DamageRuleDefinition rule(
            String path,
            List<DamageRuleCondition> conditions,
            List<? extends DamageRuleOperation> operations,
            DamagePhase phase
    ) {
        return new DamageRuleDefinition(
                id(path),
                DamageRuleRole.OFFENSIVE,
                phase,
                500,
                conditions,
                List.copyOf(operations),
                DamageRuleStacking.STACK,
                Optional.empty(),
                Optional.empty()
        );
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(
                DamageNexus.MODID,
                path
        );
    }

    private static ResourceKey<Consumer<GameTestHelper>> functionKey(
            String path
    ) {
        return ResourceKey.create(Registries.TEST_FUNCTION, id(path));
    }
}
