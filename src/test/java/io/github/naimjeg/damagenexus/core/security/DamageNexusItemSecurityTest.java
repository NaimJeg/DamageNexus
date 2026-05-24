package io.github.naimjeg.damagenexus.core.security;

import io.github.naimjeg.damagenexus.api.enums.DamageChannel;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import io.github.naimjeg.damagenexus.api.item.DamageNexusItemEntries;
import io.github.naimjeg.damagenexus.api.item.template.DamageEntryTemplateReference;
import io.github.naimjeg.damagenexus.api.item.template.DamageItemTemplateReferences;
import io.github.naimjeg.damagenexus.api.rule.DamageNexusOperations;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleDefinition;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleOperation;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleRole;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleStacking;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryDefinition;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryDisplay;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntrySlot;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryStacking;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import io.github.naimjeg.damagenexus.core.registry.DamageChannelRegistry;
import io.github.naimjeg.damagenexus.core.registry.DamageChannelRegistryTestAccess;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamageNexusItemSecurityTest {

    @BeforeEach
    void resetChannelRegistryAndCache() {
        DamageChannelRegistryTestAccess.reset();
        DamageNexusItemSecurity.clearCaches();
    }

    @AfterEach
    void restoreChannelRegistryAndCache() {
        DamageNexusItemSecurity.clearCaches();
        DamageChannelRegistryTestAccess.reset();
    }

    @Test
    void executionCacheKeyIncludesTemplateAndChannelReadiness() throws Exception {
        Class<?> cacheEntry = java.util.Arrays.stream(
                        DamageNexusItemSecurity.class.getDeclaredClasses())
                .filter(type -> type.getSimpleName().equals("CacheEntry"))
                .findFirst().orElseThrow();
        java.util.Set<String> components = java.util.Arrays.stream(
                        cacheEntry.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName)
                .collect(java.util.stream.Collectors.toSet());
        assertTrue(components.contains("references"));
        assertTrue(components.contains("templateRevision"));
        assertTrue(components.contains("validatedChannelRevision"));
        assertTrue(components.contains("serverAuthoritative"));
        assertTrue(components.contains("currentChannelRevision"));
    }

    @Test
    void cacheMatchChangesForEveryCurrentChannelRevision() throws Exception {
        Class<?> cacheEntry = java.util.Arrays.stream(
                        DamageNexusItemSecurity.class.getDeclaredClasses())
                .filter(type -> type.getSimpleName().equals("CacheEntry"))
                .findFirst().orElseThrow();
        var constructor = cacheEntry.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        Identifier authoredChannel = id("revision_channel");
        Map<Identifier, DamageChannelRegistry.ChannelDefinition> channels =
                Map.of(id("revision_channel_file"),
                        new DamageChannelRegistry.ChannelDefinition(
                                authoredChannel,
                                List.of(),
                                Optional.empty(),
                                true,
                                0));
        DamageChannelRegistryTestAccess.replace(channels);
        long revisionA = DamageChannelRegistry.contentRevision();
        ItemStack stack = allocateWithoutConstructor(ItemStack.class);
        DamageEntryDefinition authored = entry(
                "revision_entry",
                DamageEntrySlot.ITEM,
                rule(
                        "revision_rule",
                        DamagePhase.BASE_MODIFICATION,
                        DamageNexusOperations.addBaseDamage(
                                authoredChannel,
                                1.0f
                        )
                )
        );
        List<DamageEntryDefinition> entries = List.of(authored);
        List<io.github.naimjeg.damagenexus.api.rule.affix
                .DamageAffixDefinition> affixes = List.of();
        DamageItemTemplateReferences references =
                DamageItemTemplateReferences.EMPTY;
        var validated = DamageNexusItemSecurity.validateExecutionDefinitions(
                entries, affixes, "cache_revision_a", revisionA
        );
        assertTrue(validated.authoritative());
        Object entry = constructor.newInstance(
                new java.lang.ref.WeakReference<>(stack),
                entries,
                affixes,
                references,
                11L,
                12L,
                true,
                revisionA,
                validated
        );
        var matches = java.util.Arrays.stream(cacheEntry.getDeclaredMethods())
                .filter(method -> method.getName().equals("matches"))
                .findFirst().orElseThrow();
        matches.setAccessible(true);

        assertTrue((boolean) matches.invoke(
                entry, stack, entries, affixes, references,
                11L, 12L, true, revisionA
        ));

        DamageChannelRegistryTestAccess.replace(channels);
        assertEquals(revisionA, DamageChannelRegistry.contentRevision());
        assertTrue((boolean) matches.invoke(
                entry, stack, entries, affixes, references,
                11L, 12L, true, revisionA
        ), "equal channel content must retain the cache revision");

        DamageChannelRegistryTestAccess.replace(Map.of());
        long revisionB = DamageChannelRegistry.contentRevision();
        assertFalse((boolean) matches.invoke(
                entry, stack, entries, affixes, references,
                11L, 12L, true, revisionB
        ));
        assertFalse(DamageNexusItemSecurity.validateExecutionDefinitions(
                entries, affixes, "cache_revision_b", revisionB
        ).authoritative(), "removed authored channel must fail closed");

        DamageChannelRegistryTestAccess.replace(channels);
        long revisionC = DamageChannelRegistry.contentRevision();
        assertFalse((boolean) matches.invoke(
                entry, stack, entries, affixes, references,
                11L, 12L, true, revisionC
        ));
        assertTrue(DamageNexusItemSecurity.validateExecutionDefinitions(
                entries, affixes, "cache_revision_c", revisionC
        ).authoritative(), "re-added authored channel must revalidate");
    }

    private static <T> T allocateWithoutConstructor(Class<T> type) {
        try {
            Class<?> unsafeType = Class.forName("sun.misc.Unsafe");
            var field = unsafeType.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            Object unsafe = field.get(null);
            var allocate = unsafeType.getMethod(
                    "allocateInstance", Class.class);
            return type.cast(allocate.invoke(unsafe, type));
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(
                    "Unable to allocate an ItemStack identity fixture",
                    exception
            );
        }
    }

    @Test
    void materializedUnknownAuthoredChannelFailsClosed() {
        DamageEntryDefinition unknown = entry(
                "unknown_channel",
                DamageEntrySlot.ITEM,
                rule(
                        "unknown_channel_rule",
                        DamagePhase.BASE_MODIFICATION,
                        DamageNexusOperations.addBaseDamage(
                                id("missing_channel"),
                                4.0f
                        )
                )
        );
        DamageNexusItemSecurity.ValidatedItemRules validated =
                DamageNexusItemSecurity.validateExecutionDefinitions(
                        List.of(unknown),
                        List.of(),
                        "unknown_materialized",
                        io.github.naimjeg.damagenexus.core.registry
                                .DamageChannelRegistry.contentRevision()
                );

        assertFalse(validated.authoritative());
        assertTrue(validated.isEmpty());
    }

    @Test
    void materializedUntypedChannelRemainsLegal() {
        DamageEntryDefinition untyped = entry(
                "untyped_channel",
                DamageEntrySlot.ITEM,
                rule(
                        "untyped_channel_rule",
                        DamagePhase.BASE_MODIFICATION,
                        DamageNexusOperations.addBaseDamage(
                                DamageChannel.UNTYPED_ID,
                                4.0f
                        )
                )
        );
        DamageNexusItemSecurity.ValidatedItemRules validated =
                DamageNexusItemSecurity.validateExecutionDefinitions(
                        List.of(untyped),
                        List.of(),
                        "untyped_materialized",
                        io.github.naimjeg.damagenexus.core.registry
                                .DamageChannelRegistry.contentRevision()
                );

        assertTrue(validated.authoritative());
        assertEquals(List.of(untyped), validated.entries());
    }

    @ParameterizedTest
    @MethodSource("dangerousRules")
    void nonAdministratorCreativeIngressStripsDangerousRules(
            DamageRuleDefinition dangerousRule
    ) {
        DamageNexusItemEntries submitted = bundle(
                entry(
                        "dangerous_" + dangerousRule.id().getPath(),
                        DamageEntrySlot.ITEM,
                        dangerousRule
                )
        );

        assertTrue(DamageNexusItemSecurity
                .containsDangerousOperation(submitted));
        assertEquals(
                DamageNexusItemSecurity.InboundDecision.STRIP_UNTRUSTED,
                DamageNexusItemSecurity.evaluateCreativeInbound(
                        false,
                        submitted
                )
        );
    }

    @Test
    void ordinaryItemWithoutRulesIsUnaffected() {
        assertEquals(
                DamageNexusItemSecurity.InboundDecision
                        .NO_EXECUTABLE_COMPONENTS,
                DamageNexusItemSecurity.evaluateCreativeInbound(
                        false,
                        DamageNexusItemEntries.EMPTY
                )
        );
    }

    @Test
    void templateReferencesAreExecutableIngressButUnknownPayloadFreeIdsAreAdminSafe() {
        DamageItemTemplateReferences references =
                new DamageItemTemplateReferences(
                        List.of(new DamageEntryTemplateReference(
                                id("unknown_template"))),
                        List.of());
        assertEquals(
                DamageNexusItemSecurity.InboundDecision.STRIP_UNTRUSTED,
                DamageNexusItemSecurity.evaluateCreativeInbound(
                        false, DamageNexusItemEntries.EMPTY, references));
        assertEquals(
                DamageNexusItemSecurity.InboundDecision.ALLOW,
                DamageNexusItemSecurity.evaluateCreativeInbound(
                        true, DamageNexusItemEntries.EMPTY, references));
    }

    @Test
    void serverCreatedLegalRuleRemainsExecutable() {
        DamageEntryDefinition legal = entry(
                "server_legal",
                DamageEntrySlot.ITEM,
                rule(
                        "server_legal_rule",
                        DamagePhase.BASE_MODIFICATION,
                        DamageNexusOperations.addBaseDamage(
                                DamageChannel.UNTYPED_ID,
                                4.0f
                        )
                )
        );
        DamageNexusItemSecurity.ValidatedItemRules validated =
                DamageNexusItemSecurity.validateDefinitions(
                        List.of(legal),
                        List.of(),
                        "server_api"
                );

        assertTrue(validated.authoritative());
        assertEquals(List.of(legal), validated.entries());
        assertEquals(
                DamageNexusItemSecurity.InboundDecision.ALLOW,
                DamageNexusItemSecurity.evaluateCreativeInbound(
                        true,
                        bundle(legal)
                )
        );
    }

    @Test
    void structuralOverBudgetRejectsWholeItemWithoutPartialRules() {
        DamageEntryDefinition valid = entry(
                "valid_sibling",
                DamageEntrySlot.ITEM,
                rule(
                        "valid_sibling_rule",
                        DamagePhase.BASE_MODIFICATION,
                        DamageNexusOperations.addBaseDamage(
                                DamageChannel.UNTYPED_ID,
                                1.0f
                        )
                )
        );
        DamageEntryDefinition invalid = new DamageEntryDefinition(
                id("over_budget_entry"),
                DamageEntryDisplay.EMPTY,
                DamageEntrySlot.ITEM,
                java.util.stream.IntStream.range(
                                0,
                                io.github.naimjeg.damagenexus.api.rule
                                        .DamageRuleLimits.MAX_ENTRY_RULES + 1
                        )
                        .mapToObj(index -> rule(
                                "over_budget_rule_" + index,
                                DamagePhase.BASE_MODIFICATION,
                                DamageNexusOperations.addBaseDamage(
                                        DamageChannel.UNTYPED_ID,
                                        1.0f
                                )
                        ))
                        .toList(),
                DamageEntryStacking.STACK,
                Optional.empty()
        );

        DamageNexusItemSecurity.ValidatedItemRules validated =
                DamageNexusItemSecurity.validateDefinitions(
                        List.of(valid, invalid),
                        List.of(),
                        "over_budget"
                );

        assertFalse(validated.authoritative());
        assertTrue(validated.isEmpty());
    }

    private static Stream<DamageRuleDefinition> dangerousRules() {
        return Stream.of(
                rule(
                        "cancel",
                        DamagePhase.FINAL_OVERRIDE,
                        DamageNexusOperations.cancelDamage()
                ),
                rule(
                        "override",
                        DamagePhase.FINAL_OVERRIDE,
                        DamageNexusOperations.overrideFinalDamage(0.0f)
                ),
                rule(
                        "true_damage",
                        DamagePhase.BASE_MODIFICATION,
                        DamageNexusOperations.addTrueDamage(
                                DamageChannel.UNTYPED_ID,
                                10.0f
                        )
                )
        );
    }

    private static DamageNexusItemEntries bundle(
            DamageEntryDefinition entry
    ) {
        return new DamageNexusItemEntries(
                List.of(entry),
                List.of()
        );
    }

    private static DamageEntryDefinition entry(
            String path,
            DamageEntrySlot slot,
            DamageRuleDefinition rule
    ) {
        return new DamageEntryDefinition(
                id(path),
                DamageEntryDisplay.EMPTY,
                slot,
                List.of(rule),
                DamageEntryStacking.STACK,
                Optional.empty()
        );
    }

    private static DamageRuleDefinition rule(
            String path,
            DamagePhase phase,
            DamageRuleOperation operation
    ) {
        return new DamageRuleDefinition(
                id(path),
                DamageRuleRole.OFFENSIVE,
                phase,
                500,
                List.of(),
                List.of(operation),
                DamageRuleStacking.STACK,
                Optional.empty(),
                Optional.empty()
        );
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("test", path);
    }
}
