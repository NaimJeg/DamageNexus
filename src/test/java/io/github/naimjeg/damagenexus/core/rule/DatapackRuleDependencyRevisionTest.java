package io.github.naimjeg.damagenexus.core.rule;

import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import io.github.naimjeg.damagenexus.api.enums.DamageChannel;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleDefinition;
import io.github.naimjeg.damagenexus.api.rule.builder.DamageRuleBuilder;
import io.github.naimjeg.damagenexus.core.registry.DamageChannelRegistry;
import io.github.naimjeg.damagenexus.core.registry.DamageChannelRegistryTestAccess;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DatapackRuleDependencyRevisionTest {
    private static final Identifier CHANNEL = id("channel");

    @BeforeEach
    void setup() {
        DamageChannelRegistryTestAccess.reset();
        DatapackDamageRuleStore.resetForTesting();
    }

    @AfterEach
    void reset() {
        DatapackDamageRuleStore.resetForTesting();
        DamageChannelRegistryTestAccess.reset();
    }

    @Test
    void oldGlobalRulesFailClosedAfterChannelContentChanges() {
        Map<Identifier, DamageChannelRegistry.ChannelDefinition> channels =
                channels(CHANNEL);
        DamageChannelRegistryTestAccess.replace(channels);
        DamageRuleDefinition rule = DamageRuleBuilder.offensive(id("rule"))
                .addBaseDamage(CHANNEL, 1.0f)
                .build();
        assertTrue(DatapackDamageRuleReloadListener.applyPreparedForTesting(
                Map.of(rule.id(), rule)));
        DatapackDamageRuleStore.Snapshot pinned =
                DatapackDamageRuleStore.executionSnapshot();
        assertEquals(List.of(rule), pinned.rules(DamagePhase.BASE_MODIFICATION));

        long revision = DamageChannelRegistry.contentRevision();
        DamageChannelRegistryTestAccess.replace(channels);
        assertEquals(revision, DamageChannelRegistry.contentRevision());
        assertEquals(List.of(rule), DatapackDamageRuleStore.rules());

        long ruleRevision = DatapackDamageRuleStore.snapshot().revision();
        assertFalse(DatapackDamageRuleReloadListener
                .applyPreparedForTesting(null));
        assertEquals(ruleRevision,
                DatapackDamageRuleStore.snapshot().revision());
        assertEquals(List.of(rule), DatapackDamageRuleStore.rules(),
                "a failed reload may retain the old snapshot while its "
                        + "channel dependency is unchanged");

        DamageChannelRegistryTestAccess.replace(Map.of());
        assertEquals(List.of(rule), pinned.rules(),
                "an active transaction keeps its immutable snapshot");
        assertTrue(DatapackDamageRuleStore.rules().isEmpty(),
                "new transactions must not execute stale channel references");
        assertFalse(DatapackDamageRuleStore.executionSnapshot()
                .serverAuthoritative());
    }

    @Test
    void failedGlobalReloadDoesNotMakeStaleRulesCompatibleAgain() {
        DamageChannelRegistryTestAccess.replace(channels(CHANNEL));
        DamageRuleDefinition rule = DamageRuleBuilder.offensive(id("rule"))
                .addBaseDamage(CHANNEL, 1.0f)
                .build();
        assertTrue(DatapackDamageRuleReloadListener.applyPreparedForTesting(
                Map.of(rule.id(), rule)));
        long ruleRevision = DatapackDamageRuleStore.snapshot().revision();

        DamageChannelRegistryTestAccess.replace(Map.of());
        Map<Identifier, DamageRuleDefinition> overBudget =
                new java.util.LinkedHashMap<>();
        for (int i = 0;
             i <= DatapackDamageRuleReloadListener.MAX_DATAPACK_RULES;
             i++) {
            DamageRuleDefinition value = DamageRuleBuilder.offensive(
                            id("over_" + i))
                    .addBaseDamage(DamageChannel.UNTYPED_ID, 1.0f)
                    .build();
            overBudget.put(value.id(), value);
        }
        assertFalse(DatapackDamageRuleReloadListener
                .applyPreparedForTesting(overBudget));
        assertEquals(ruleRevision, DatapackDamageRuleStore.snapshot().revision());
        assertTrue(DatapackDamageRuleStore.rules().isEmpty());
    }

    private static Map<Identifier, DamageChannelRegistry.ChannelDefinition>
    channels(Identifier channel) {
        return Map.of(id("file"),
                new DamageChannelRegistry.ChannelDefinition(
                        channel, List.of(), Optional.empty(), true, 0));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("globalrulemod", path);
    }
}
