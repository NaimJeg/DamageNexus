package io.github.naimjeg.damagenexus.client.tooltip.document;

import io.github.naimjeg.damagenexus.api.client.phrase.RulePhraseRegistry;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import io.github.naimjeg.damagenexus.api.item.template.DamageItemTemplateReferences;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleDefinition;
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
import io.github.naimjeg.damagenexus.client.tooltip.narrative.RuleNarrativePlanner;
import io.github.naimjeg.damagenexus.config.TooltipDebugLevel;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DamageTooltipDocumentPlannerOrderingTest {

    @Test
    void rulesFollowPhasePriorityAndIdentifierInsteadOfAuthoredOrder() {
        DamageRuleDefinition r1 = rule("r1", DamagePhase.BASE_MODIFICATION, 100);
        DamageRuleDefinition r2 = rule("r2", DamagePhase.BASE_MODIFICATION, 900);
        DamageRuleDefinition r3 = rule("r3", DamagePhase.TYPE_SCALING, 1000);
        DamageRuleDefinition r4 = rule("r4", DamagePhase.CRITICAL_HIT, 500);

        DamageTooltipDocument document = plan(
                List.of(entry("entry", r4, r1, r3, r2)),
                List.of()
        );

        assertEquals(
                List.of(id("r2"), id("r1"), id("r3"), id("r4")),
                document.standaloneEntries().getFirst().rules().stream()
                        .map(RuleTooltipView::id)
                        .toList()
        );
    }

    @Test
    void samePhaseHigherPriorityDisplaysBeforeLowerPriority() {
        DamageTooltipDocument document = plan(
                List.of(entry(
                        "golden_sword",
                        rule("fire_convert", DamagePhase.TYPE_SCALING, 400),
                        rule("lightning_gain", DamagePhase.TYPE_SCALING, 401)
                )),
                List.of()
        );

        assertEquals(
                List.of(id("lightning_gain"), id("fire_convert")),
                document.standaloneEntries().getFirst().rules().stream()
                        .map(RuleTooltipView::id)
                        .toList()
        );
    }

    @Test
    void entriesWithinAnAffixUseTheirEarliestRuleAsTheDisplayKey() {
        DamageEntryDefinition type = entry(
                "entry_type",
                rule("type", DamagePhase.TYPE_SCALING, 900)
        );
        DamageEntryDefinition baseLow = entry(
                "entry_base_low",
                rule("base_low", DamagePhase.BASE_MODIFICATION, 100)
        );
        DamageEntryDefinition baseHigh = entry(
                "entry_base_high",
                rule("base_high", DamagePhase.BASE_MODIFICATION, 800)
        );

        DamageTooltipDocument document = plan(
                List.of(),
                List.of(affix("affix", type, baseLow, baseHigh))
        );

        assertEquals(
                List.of(id("entry_base_high"), id("entry_base_low"), id("entry_type")),
                document.affixes().getFirst().entries().stream()
                        .map(EntryTooltipView::id)
                        .toList()
        );
    }

    @Test
    void affixesUseTheirEarliestContainedRuleAsTheDisplayKey() {
        DamageAffixDefinition type = affix(
                "affix_type",
                entry("entry_type", rule("type", DamagePhase.TYPE_SCALING, 1000))
        );
        DamageAffixDefinition baseLow = affix(
                "affix_base_low",
                entry("entry_base_low", rule("base_low", DamagePhase.BASE_MODIFICATION, 100))
        );
        DamageAffixDefinition baseHigh = affix(
                "affix_base_high",
                entry("entry_base_high", rule("base_high", DamagePhase.BASE_MODIFICATION, 900))
        );

        DamageTooltipDocument document = plan(
                List.of(),
                List.of(type, baseLow, baseHigh)
        );

        assertEquals(
                List.of(id("affix_base_high"), id("affix_base_low"), id("affix_type")),
                document.affixes().stream().map(AffixTooltipView::id).toList()
        );
    }

    @Test
    void entriesAndAffixesUseTheirFullSortedRuleSequenceAfterTheFirstRule() {
        DamageRuleDefinition sharedBase = rule(
                "shared_base",
                DamagePhase.BASE_MODIFICATION,
                100
        );
        DamageEntryDefinition entryLow = entry(
                "entry_a_low_second_rule",
                sharedBase,
                rule("entry_low_type", DamagePhase.TYPE_SCALING, 400)
        );
        DamageEntryDefinition entryHigh = entry(
                "entry_z_high_second_rule",
                sharedBase,
                rule("entry_high_type", DamagePhase.TYPE_SCALING, 401)
        );

        DamageTooltipDocument entriesDocument = plan(
                List.of(entryLow, entryHigh),
                List.of()
        );
        assertEquals(
                List.of(id("entry_z_high_second_rule"), id("entry_a_low_second_rule")),
                entriesDocument.standaloneEntries().stream()
                        .map(EntryTooltipView::id)
                        .toList()
        );

        DamageTooltipDocument affixesDocument = plan(
                List.of(),
                List.of(
                        affix("affix_a_low_second_rule", entryLow),
                        affix("affix_z_high_second_rule", entryHigh)
                )
        );
        assertEquals(
                List.of(id("affix_z_high_second_rule"), id("affix_a_low_second_rule")),
                affixesDocument.affixes().stream()
                        .map(AffixTooltipView::id)
                        .toList()
        );
    }

    @Test
    void equalExecutionKeysHaveStableIdentifierTieBreaks() {
        DamageRuleDefinition ruleZ = rule("rule_z", DamagePhase.BASE_MODIFICATION, 500);
        DamageRuleDefinition ruleA = rule("rule_a", DamagePhase.BASE_MODIFICATION, 500);
        DamageRuleDefinition shared = rule("shared", DamagePhase.TYPE_SCALING, 400);

        for (int iteration = 0; iteration < 5; iteration++) {
            DamageTooltipDocument document = plan(
                    List.of(
                            entry("entry_z", shared),
                            entry("entry_a", shared),
                            entry("rule_tie", ruleZ, ruleA)
                    ),
                    List.of(
                            affix("affix_z", entry("nested_z", shared)),
                            affix("affix_a", entry("nested_a", shared))
                    )
            );

            assertEquals(
                    List.of(id("rule_tie"), id("entry_a"), id("entry_z")),
                    document.standaloneEntries().stream()
                            .map(EntryTooltipView::id)
                            .toList()
            );
            assertEquals(
                    List.of(id("rule_a"), id("rule_z")),
                    document.standaloneEntries().getFirst().rules().stream()
                            .map(RuleTooltipView::id)
                            .toList()
            );
            assertEquals(
                    List.of(id("affix_a"), id("affix_z")),
                    document.affixes().stream().map(AffixTooltipView::id).toList()
            );
        }
    }

    private static DamageTooltipDocument plan(
            List<DamageEntryDefinition> entries,
            List<DamageAffixDefinition> affixes
    ) {
        return new DamageTooltipDocumentPlanner(
                new RuleNarrativePlanner(new RulePhraseRegistry())
        ).plan(
                entries,
                affixes,
                List.of(),
                DamageItemTemplateReferences.EMPTY,
                TooltipDebugLevel.OFF
        );
    }

    private static DamageRuleDefinition rule(
            String path,
            DamagePhase phase,
            int priority
    ) {
        return new DamageRuleDefinition(
                id(path),
                DamageRuleRole.OFFENSIVE,
                phase,
                priority,
                List.of(),
                List.of(),
                DamageRuleStacking.STACK,
                Optional.empty(),
                Optional.empty()
        );
    }

    private static DamageEntryDefinition entry(
            String path,
            DamageRuleDefinition... rules
    ) {
        return new DamageEntryDefinition(
                id(path),
                DamageEntryDisplay.EMPTY,
                DamageEntrySlot.ITEM,
                List.of(rules),
                DamageEntryStacking.STACK,
                Optional.empty()
        );
    }

    private static DamageAffixDefinition affix(
            String path,
            DamageEntryDefinition... entries
    ) {
        return new DamageAffixDefinition(
                id(path),
                DamageAffixDisplay.EMPTY,
                DamageAffixSlot.ITEM,
                DamageAffixRarity.COMMON,
                List.of(entries),
                DamageAffixStacking.STACK,
                Optional.empty()
        );
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("damagenexus_test", path);
    }
}
