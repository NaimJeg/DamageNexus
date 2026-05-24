package io.github.naimjeg.damagenexus.core.lifecycle;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.naimjeg.damagenexus.api.enums.DamageChannel;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import io.github.naimjeg.damagenexus.api.rule.DamageNexusOperations;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleDefinition;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleLimits;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleRole;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleStacking;
import io.github.naimjeg.damagenexus.api.rule.affix.*;
import io.github.naimjeg.damagenexus.api.rule.builder.DamageRuleBuilder;
import io.github.naimjeg.damagenexus.api.rule.entry.*;
import io.github.naimjeg.damagenexus.core.template.DamageTemplateRegistry;
import io.github.naimjeg.damagenexus.core.template.DamageTemplateSnapshot;
import io.github.naimjeg.damagenexus.core.template.DatapackDamageTemplateReloadListener;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DatapackDamageTemplateReloadTest {
    private DamageNexusRegistrationAccess access;

    @BeforeEach
    void setup() {
        access = DamageNexusLifecycle.beginRegistering();
        DamageTemplateRegistry.freeze(access);
    }

    @AfterEach
    void reset() {
        access.close();
        DamageNexusLifecycle.resetForTesting();
    }

    @Test
    void realJsonDecodesAndPublishesBothTypedNamespacesAtomically() {
        DamageEntryDefinition entry = entry("examplemod", "entry", 2.0f);
        DamageAffixDefinition affix = affix("examplemod", "affix", 3.0f);
        DamageEntryDefinition decodedEntry = DamageEntryDefinition.CODEC.parse(
                JsonOps.INSTANCE,
                JsonParser.parseString(DamageEntryDefinition.CODEC
                        .encodeStart(JsonOps.INSTANCE, entry)
                        .getOrThrow().toString())).getOrThrow();
        DamageAffixDefinition decodedAffix = DamageAffixDefinition.CODEC.parse(
                JsonOps.INSTANCE,
                JsonParser.parseString(DamageAffixDefinition.CODEC
                        .encodeStart(JsonOps.INSTANCE, affix)
                        .getOrThrow().toString())).getOrThrow();

        assertTrue(DatapackDamageTemplateReloadListener
                .applyPreparedForTesting(
                        Map.of(entry.id(), decodedEntry),
                        Map.of(affix.id(), decodedAffix)));
        assertEquals(1L, DamageTemplateRegistry.revision());
        assertEquals(Optional.of(entry), DamageTemplateRegistry.entry(entry.id()));
        assertEquals(Optional.of(affix), DamageTemplateRegistry.affix(affix.id()));
    }

    @Test
    void failedReloadKeepsPreviousSnapshotAndRevision() {
        DamageEntryDefinition first = entry("examplemod", "entry", 1.0f);
        assertTrue(DatapackDamageTemplateReloadListener
                .applyPreparedForTesting(Map.of(first.id(), first), Map.of()));
        DamageTemplateSnapshot previous = DamageTemplateRegistry.snapshot();

        DamageEntryDefinition mismatched =
                entry("examplemod", "definition_id", 9.0f);
        assertFalse(DatapackDamageTemplateReloadListener
                .applyPreparedForTesting(
                        Map.of(id("examplemod", "file_id"), mismatched),
                        Map.of()));
        assertSame(previous, DamageTemplateRegistry.snapshot());
        assertEquals(1L, DamageTemplateRegistry.revision());
        assertEquals(Optional.of(first), DamageTemplateRegistry.entry(first.id()));
    }

    @Test
    void successfulReloadReplacesDefinitionAndAdvancesRevision() {
        DamageEntryDefinition first = entry("examplemod", "entry", 1.0f);
        DamageEntryDefinition second = entry("examplemod", "entry", 4.0f);
        assertTrue(DatapackDamageTemplateReloadListener
                .applyPreparedForTesting(Map.of(first.id(), first), Map.of()));
        assertTrue(DatapackDamageTemplateReloadListener
                .applyPreparedForTesting(Map.of(second.id(), second), Map.of()));
        assertEquals(2L, DamageTemplateRegistry.revision());
        assertEquals(Optional.of(second), DamageTemplateRegistry.entry(second.id()));
    }

    @Test
    void javaConflictAndInvalidNestedDefinitionRejectWholeReload() {
        access.close();
        DamageNexusLifecycle.resetForTesting();
        access = DamageNexusLifecycle.beginRegistering();
        DamageEntryDefinition javaEntry = entry("javamod", "owned", 1.0f);
        DamageTemplateRegistry.registerEntry(access, javaEntry.id(), javaEntry);
        DamageTemplateRegistry.freeze(access);
        DamageTemplateSnapshot initial = DamageTemplateRegistry.snapshot();

        assertFalse(DatapackDamageTemplateReloadListener
                .applyPreparedForTesting(
                        Map.of(javaEntry.id(), javaEntry), Map.of()));
        assertSame(initial, DamageTemplateRegistry.snapshot());

        Identifier badAffixId = id("contentmod", "bad_nested");
        DamageRuleDefinition wrongPhase = new DamageRuleDefinition(
                id("contentmod", "bad_nested_rule"),
                DamageRuleRole.OFFENSIVE,
                DamagePhase.BASE_MODIFICATION,
                500,
                List.of(),
                List.of(DamageNexusOperations.overrideFinalDamage(3.0f)),
                DamageRuleStacking.STACK,
                Optional.empty(),
                Optional.empty());
        DamageEntryDefinition invalidEntry = new DamageEntryDefinition(
                id("contentmod", "bad_nested_entry"),
                DamageEntryDisplay.EMPTY,
                DamageEntrySlot.ITEM,
                List.of(wrongPhase),
                DamageEntryStacking.STACK,
                Optional.empty());
        DamageAffixDefinition invalid = new DamageAffixDefinition(
                badAffixId,
                DamageAffixDisplay.EMPTY,
                DamageAffixSlot.ITEM,
                DamageAffixRarity.COMMON,
                List.of(invalidEntry),
                DamageAffixStacking.STACK,
                Optional.empty());
        assertFalse(DatapackDamageTemplateReloadListener
                .applyPreparedForTesting(
                        Map.of(), Map.of(badAffixId, invalid)));
        assertSame(initial, DamageTemplateRegistry.snapshot());
    }

    @Test
    void aggregateBudgetRejectsAtomicallyWithoutOverflowOrPartialPublish() {
        Map<Identifier, DamageAffixDefinition> affixes =
                new LinkedHashMap<>();
        int rulesPerAffix = 128;
        int affixCount =
                DatapackDamageTemplateReloadListener.MAX_TEMPLATE_RULES
                        / rulesPerAffix + 1;
        for (int affixIndex = 0; affixIndex < affixCount; affixIndex++) {
            Identifier affixId = id("contentmod", "aggregate_" + affixIndex);
            List<DamageEntryDefinition> nested = new java.util.ArrayList<>();
            for (int entryIndex = 0; entryIndex < 4; entryIndex++) {
                Identifier entryId = id("contentmod", "aggregate_"
                        + affixIndex + "_entry_" + entryIndex);
                List<DamageRuleDefinition> rules = new java.util.ArrayList<>();
                for (int ruleIndex = 0;
                     ruleIndex < DamageRuleLimits.MAX_ENTRY_RULES;
                     ruleIndex++) {
                    rules.add(rule(entryId, ruleIndex));
                }
                nested.add(new DamageEntryDefinition(
                        entryId, DamageEntryDisplay.EMPTY,
                        DamageEntrySlot.ITEM, rules,
                        DamageEntryStacking.STACK, Optional.empty()));
            }
            affixes.put(affixId, new DamageAffixDefinition(
                    affixId, DamageAffixDisplay.EMPTY, DamageAffixSlot.ITEM,
                    DamageAffixRarity.COMMON, nested,
                    DamageAffixStacking.STACK, Optional.empty()));
        }
        DamageTemplateSnapshot before = DamageTemplateRegistry.snapshot();
        assertFalse(DatapackDamageTemplateReloadListener
                .applyPreparedForTesting(Map.of(), affixes));
        assertSame(before, DamageTemplateRegistry.snapshot());
        assertEquals(0L, DamageTemplateRegistry.revision());
    }

    @Test
    void unknownOperationAndConditionReturnCodecErrors() {
        Identifier validId = id("contentmod", "codec");
        DamageEntryDefinition valid = new DamageEntryDefinition(
                validId, DamageEntryDisplay.EMPTY, DamageEntrySlot.ITEM,
                List.of(DamageRuleBuilder.offensive(
                                id("contentmod", "codec_rule"))
                        .always()
                        .addBaseDamage(DamageChannel.UNTYPED_ID, 1.0f)
                        .build()),
                DamageEntryStacking.STACK, Optional.empty());
        String json = DamageEntryDefinition.CODEC
                .encodeStart(JsonOps.INSTANCE, valid)
                .getOrThrow().toString();
        String unknownOperation = json.replace(
                "damagenexus:add_base_damage",
                "contentmod:missing_operation");
        IllegalArgumentException operationError = assertThrows(
                IllegalArgumentException.class,
                () -> DamageEntryDefinition.CODEC.parse(
                        JsonOps.INSTANCE,
                        JsonParser.parseString(unknownOperation)));
        assertTrue(operationError.getMessage().contains(
                "contentmod:missing_operation"));

        String withUnknownCondition = json.replace(
                "damagenexus:always",
                "contentmod:missing_condition");
        IllegalArgumentException conditionError = assertThrows(
                IllegalArgumentException.class,
                () -> DamageEntryDefinition.CODEC.parse(
                        JsonOps.INSTANCE,
                        JsonParser.parseString(withUnknownCondition)));
        assertTrue(conditionError.getMessage().contains(
                "contentmod:missing_condition"));
    }

    private static DamageRuleDefinition rule(
            Identifier entryId,
            int index
    ) {
        return new DamageRuleDefinition(
                Identifier.fromNamespaceAndPath(
                        entryId.getNamespace(),
                        entryId.getPath() + "_rule_" + index),
                DamageRuleRole.OFFENSIVE,
                DamagePhase.BASE_MODIFICATION,
                500,
                List.of(),
                List.of(DamageNexusOperations.addBaseDamage(
                        DamageChannel.UNTYPED_ID, 1.0f)),
                DamageRuleStacking.STACK,
                Optional.empty(),
                Optional.empty());
    }

    static DamageEntryDefinition entry(
            String namespace,
            String path,
            float amount
    ) {
        Identifier id = id(namespace, path);
        return new DamageEntryDefinition(
                id, DamageEntryDisplay.EMPTY, DamageEntrySlot.ITEM,
                List.of(DamageRuleBuilder.offensive(
                                id(namespace, path + "_rule"))
                        .addBaseDamage(DamageChannel.UNTYPED_ID, amount)
                        .build()), DamageEntryStacking.STACK, Optional.empty());
    }

    static DamageAffixDefinition affix(
            String namespace,
            String path,
            float amount
    ) {
        Identifier id = id(namespace, path);
        return new DamageAffixDefinition(
                id, DamageAffixDisplay.EMPTY, DamageAffixSlot.ITEM,
                DamageAffixRarity.COMMON,
                List.of(entry(namespace, path + "_nested", amount)),
                DamageAffixStacking.STACK, Optional.empty());
    }

    static Identifier id(String namespace, String path) {
        return Identifier.fromNamespaceAndPath(namespace, path);
    }
}
