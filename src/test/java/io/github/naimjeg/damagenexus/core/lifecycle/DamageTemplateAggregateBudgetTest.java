package io.github.naimjeg.damagenexus.core.lifecycle;

import io.github.naimjeg.damagenexus.api.enums.DamageChannel;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import io.github.naimjeg.damagenexus.api.rule.DamageNexusConditions;
import io.github.naimjeg.damagenexus.api.rule.DamageNexusOperations;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleCondition;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleDefinition;
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
import io.github.naimjeg.damagenexus.core.template.DamageTemplateLimits;
import io.github.naimjeg.damagenexus.core.template.DamageTemplateRegistry;
import io.github.naimjeg.damagenexus.core.template.DatapackDamageTemplateReloadListener;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DamageTemplateAggregateBudgetTest {
    private DamageNexusRegistrationAccess access;
    private DamageNexusRegistrationSession session;

    @AfterEach
    void reset() {
        if (session != null) session.close();
        if (access != null) access.close();
        DamageNexusLifecycle.resetForTesting();
    }

    @Test
    void javaEntryCountAtLimitPublishesUnvalidatedSnapshot() {
        begin();
        for (int i = 0; i < DamageTemplateLimits.MAX_ENTRY_TEMPLATES; i++) {
            registerEntry(entry("entry_" + i, List.of(simpleRule("r_" + i))));
        }
        DamageTemplateRegistry.freeze(access);
        assertEquals(DamageTemplateLimits.MAX_ENTRY_TEMPLATES,
                DamageTemplateRegistry.snapshot().entries().size());
        assertFalse(DamageTemplateRegistry.snapshot().serverAuthoritative());
    }

    @Test
    void javaEntryAndAffixCountsOverLimitFailBeforePublication() {
        begin();
        for (int i = 0; i <= DamageTemplateLimits.MAX_ENTRY_TEMPLATES; i++) {
            registerEntry(entry("entry_" + i, List.of(simpleRule("r_" + i))));
        }
        IllegalArgumentException entryFailure = assertThrows(
                IllegalArgumentException.class,
                () -> DamageTemplateRegistry.freeze(access));
        assertTrue(entryFailure.getMessage().contains("category=entry_templates"));
        assertTrue(DamageTemplateRegistry.snapshot().entries().isEmpty());

        reset();
        access = null;
        session = null;
        begin();
        for (int i = 0; i <= DamageTemplateLimits.MAX_AFFIX_TEMPLATES; i++) {
            registerAffix(affix("affix_" + i,
                    entry("nested_" + i, List.of(simpleRule("ar_" + i)))));
        }
        IllegalArgumentException affixFailure = assertThrows(
                IllegalArgumentException.class,
                () -> DamageTemplateRegistry.freeze(access));
        assertTrue(affixFailure.getMessage().contains("category=affix_templates"));
        assertTrue(DamageTemplateRegistry.snapshot().affixes().isEmpty());
    }

    @Test
    void javaRuleAggregateOverLimitFailsWithoutPartialSnapshot() {
        begin();
        for (int entryIndex = 0;
             entryIndex < DamageTemplateLimits.MAX_ENTRY_TEMPLATES;
             entryIndex++) {
            List<DamageRuleDefinition> rules = new ArrayList<>();
            for (int ruleIndex = 0; ruleIndex < 32; ruleIndex++) {
                rules.add(simpleRule("rule_" + entryIndex + "_" + ruleIndex));
            }
            registerEntry(entry("entry_" + entryIndex, rules));
        }
        registerAffix(affix("extra_affix",
                entry("extra_nested", List.of(simpleRule("extra_rule")))));
        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class,
                () -> DamageTemplateRegistry.freeze(access));
        assertTrue(failure.getMessage().contains("category=rules"));
        assertEquals(0L, DamageTemplateRegistry.revision());
        assertTrue(DamageTemplateRegistry.snapshot().entries().isEmpty());
        assertEquals(DamageNexusLifecycleState.REGISTERING,
                DamageNexusLifecycle.state());
    }

    @Test
    void javaConditionNodeAggregateOverLimitFails() {
        begin();
        List<DamageRuleCondition> maxGraph = maxConditionGraph();
        for (int i = 0; i < DamageTemplateLimits.MAX_ENTRY_TEMPLATES; i++) {
            registerEntry(entry("entry_" + i,
                    List.of(rule("rule_" + i, maxGraph,
                            List.of(DamageNexusOperations.addBaseDamage(
                                    DamageChannel.UNTYPED_ID, 1.0f))))));
        }
        registerAffix(affix("extra_affix",
                entry("extra_nested", List.of(rule(
                        "extra_rule",
                        List.of(DamageNexusConditions.always()),
                        List.of(DamageNexusOperations.addBaseDamage(
                                DamageChannel.UNTYPED_ID, 1.0f)))))));
        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class,
                () -> DamageTemplateRegistry.freeze(access));
        assertTrue(failure.getMessage().contains("category=condition_nodes"));
        assertTrue(DamageTemplateRegistry.snapshot().entries().isEmpty());
    }

    @Test
    void javaOperationAggregateOverLimitFails() {
        begin();
        List<DamageRuleOperation> operations = java.util.Collections.nCopies(
                32,
                DamageNexusOperations.addBaseDamage(
                        DamageChannel.UNTYPED_ID, 1.0f));
        for (int entryIndex = 0;
             entryIndex < DamageTemplateLimits.MAX_ENTRY_TEMPLATES;
             entryIndex++) {
            List<DamageRuleDefinition> rules = new ArrayList<>();
            for (int ruleIndex = 0; ruleIndex < 4; ruleIndex++) {
                rules.add(rule("rule_" + entryIndex + "_" + ruleIndex,
                        List.of(), operations));
            }
            registerEntry(entry("entry_" + entryIndex, rules));
        }
        registerAffix(affix("extra_affix",
                entry("extra_nested", List.of(simpleRule("extra_rule")))));
        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class,
                () -> DamageTemplateRegistry.freeze(access));
        assertTrue(failure.getMessage().contains("category=operations"));
        assertTrue(DamageTemplateRegistry.snapshot().entries().isEmpty());
    }

    @Test
    void javaAndDatapackCannotSplitCombinedTemplateCountBudget() {
        begin();
        for (int i = 0; i < DamageTemplateLimits.MAX_ENTRY_TEMPLATES - 1; i++) {
            registerEntry(entry("java_" + i,
                    List.of(simpleRule("java_rule_" + i))));
        }
        DamageTemplateRegistry.freeze(access);
        long revision = DamageTemplateRegistry.revision();
        DamageEntryDefinition first = entry("pack_first",
                List.of(simpleRule("pack_first_rule")));
        DamageEntryDefinition second = entry("pack_second",
                List.of(simpleRule("pack_second_rule")));
        assertFalse(DatapackDamageTemplateReloadListener
                .applyPreparedForTesting(
                        Map.of(first.id(), first, second.id(), second),
                        Map.of()));
        assertEquals(revision, DamageTemplateRegistry.revision());
        assertFalse(DamageTemplateRegistry.snapshot().serverAuthoritative());
    }

    private void begin() {
        access = DamageNexusLifecycle.beginRegistering();
        session = new DamageNexusRegistrationSession(access);
    }

    private void registerEntry(DamageEntryDefinition definition) {
        session.registerEntryTemplate(definition.id(), definition);
    }

    private void registerAffix(DamageAffixDefinition definition) {
        session.registerAffixTemplate(definition.id(), definition);
    }

    private static DamageEntryDefinition entry(
            String path,
            List<DamageRuleDefinition> rules
    ) {
        Identifier id = id(path);
        return new DamageEntryDefinition(
                id, DamageEntryDisplay.EMPTY, DamageEntrySlot.ITEM,
                rules, DamageEntryStacking.STACK, Optional.empty());
    }

    private static DamageAffixDefinition affix(
            String path,
            DamageEntryDefinition entry
    ) {
        Identifier id = id(path);
        return new DamageAffixDefinition(
                id, DamageAffixDisplay.EMPTY, DamageAffixSlot.ITEM,
                DamageAffixRarity.COMMON, List.of(entry),
                DamageAffixStacking.STACK, Optional.empty());
    }

    private static DamageRuleDefinition simpleRule(String path) {
        return rule(path, List.of(), List.of(
                DamageNexusOperations.addBaseDamage(
                        DamageChannel.UNTYPED_ID, 1.0f)));
    }

    private static DamageRuleDefinition rule(
            String path,
            List<DamageRuleCondition> conditions,
            List<DamageRuleOperation> operations
    ) {
        return new DamageRuleDefinition(
                id(path), DamageRuleRole.OFFENSIVE,
                DamagePhase.BASE_MODIFICATION, 500,
                conditions, operations, DamageRuleStacking.STACK,
                Optional.empty(), Optional.empty());
    }

    private static List<DamageRuleCondition> maxConditionGraph() {
        List<DamageRuleCondition> groups = new ArrayList<>();
        for (int group = 0; group < 32; group++) {
            int leaves = group == 31 ? 6 : 7;
            List<DamageRuleCondition> children = new ArrayList<>();
            for (int i = 0; i < leaves; i++) {
                children.add(DamageNexusConditions.always());
            }
            groups.add(DamageNexusConditions.allOf(children));
        }
        // One root + 32 groups + 223 leaves = 256 nodes.
        return List.of(DamageNexusConditions.allOf(groups));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("budgetmod", path);
    }
}
