package io.github.naimjeg.damagenexus.client.tooltip;

import io.github.naimjeg.damagenexus.api.client.phrase.RulePhraseRegistry;
import io.github.naimjeg.damagenexus.api.context.DamageMutationResult;
import io.github.naimjeg.damagenexus.api.context.DamageRuleContext;
import io.github.naimjeg.damagenexus.api.display.DisplayText;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import io.github.naimjeg.damagenexus.api.item.template.DamageEntryTemplateReference;
import io.github.naimjeg.damagenexus.api.item.template.DamageItemTemplateReferences;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleDefinition;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleCondition;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleOperation;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleRole;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleStacking;
import io.github.naimjeg.damagenexus.api.rule.affix.*;
import io.github.naimjeg.damagenexus.api.rule.entry.*;
import io.github.naimjeg.damagenexus.builtin.rule.condition.AllOfCondition;
import io.github.naimjeg.damagenexus.builtin.rule.condition.TargetHealthBelowCondition;
import io.github.naimjeg.damagenexus.builtin.rule.condition.TargetMobCategoryIsCondition;
import io.github.naimjeg.damagenexus.builtin.rule.condition.TargetOnFireCondition;
import io.github.naimjeg.damagenexus.builtin.rule.operation.AddBaseDamageOperation;
import io.github.naimjeg.damagenexus.builtin.rule.operation.AddChannelPostMultiplierOperation;
import io.github.naimjeg.damagenexus.client.tooltip.document.*;
import io.github.naimjeg.damagenexus.client.tooltip.narrative.RuleNarrativePlanner;
import io.github.naimjeg.damagenexus.config.TooltipDebugLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.MobCategory;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DamageTooltipRendererTest {
    @Test
    void realEnglishAndChineseCompactSnapshotsUseLanguageSpecificOrder() throws Exception {
        assertEquals(List.of(
                        "Blazing Verdict",
                        "  +4 Fire damage while the target is burning",
                        "  Hold Shift for details"
                ), snapshot("en_us", Locale.US, TooltipDetailLevel.COMPACT));
        assertEquals(List.of(
                        "灼焰裁决",
                        "  目标正在燃烧时，+4 点火焰伤害",
                        "  按住 Shift 查看详情"
                ), snapshot("zh_cn", Locale.SIMPLIFIED_CHINESE, TooltipDetailLevel.COMPACT));
    }

    @Test
    void shiftExpandedSnapshotContainsConditionAndEffectSections() throws Exception {
        List<String> lines = snapshot(
                "zh_cn", Locale.SIMPLIFIED_CHINESE, TooltipDetailLevel.EXPANDED
        );
        assertEquals(List.of(
                "灼焰裁决",
                "  规则",
                "  条件",
                "    目标当前正在燃烧",
                "  效果",
                "    增加 4 点火焰伤害"
        ), lines);
        assertNoLeaks(lines);
    }

    @Test
    void complexRuleShowsBothNestedConditionsAndAllEffects() throws Exception {
        DamageRuleDefinition rule = new DamageRuleDefinition(
                id("complex_rule"), DamageRuleRole.OFFENSIVE,
                DamagePhase.BASE_MODIFICATION, 500,
                List.of(new AllOfCondition(List.of(
                        new TargetOnFireCondition(),
                        new TargetHealthBelowCondition(0.5f)
                ))),
                List.of(
                        new AddBaseDamageOperation(channel("fire"), 4),
                        new AddChannelPostMultiplierOperation(channel("fire"), 0.25f)
                ),
                DamageRuleStacking.STACK, Optional.empty(), Optional.empty()
        );
        try (TooltipTestLanguage ignored = TooltipTestLanguage.install("zh_cn")) {
            List<String> lines = render(
                    document(List.of(entry(rule)), List.of(), List.of(),
                            DamageItemTemplateReferences.EMPTY, TooltipDebugLevel.OFF),
                    TooltipDetailLevel.EXPANDED,
                    TooltipDebugLevel.OFF,
                    Locale.SIMPLIFIED_CHINESE,
                    new ArrayList<>()
            );
            String joined = String.join("\n", lines);
            assertTrue(joined.contains("同时满足以下条件"));
            assertTrue(joined.contains("目标当前正在燃烧"));
            assertTrue(joined.contains("目标生命值低于 50%"));
            assertTrue(joined.contains("增加 4 点火焰伤害"));
            assertTrue(joined.contains("火焰伤害提高 25%"));
            assertNoLeaks(lines);
        }
    }

    @Test
    void fourDebugLevelsAreOrthogonalToShiftAndHeaderAppearsOnce() throws Exception {
        try (TooltipTestLanguage ignored = TooltipTestLanguage.install("en_us")) {
            for (TooltipDetailLevel detail : TooltipDetailLevel.values()) {
                for (TooltipDebugLevel debug : TooltipDebugLevel.values()) {
                    DamageTooltipDocument document = document(
                            List.of(entry(simpleRule())), List.of(), List.of(),
                            DamageItemTemplateReferences.EMPTY, debug
                    );
                    List<String> lines = render(
                            document, detail, debug, Locale.US, new ArrayList<>()
                    );
                    long headers = lines.stream()
                            .filter("[DamageNexus Debug]"::equals).count();
                    assertEquals(debug == TooltipDebugLevel.OFF ? 0 : 1, headers,
                            detail + "/" + debug);
                    String joined = String.join("\n", lines);
                    assertEquals(debug != TooltipDebugLevel.OFF,
                            joined.contains("Sources:"));
                    assertEquals(debug == TooltipDebugLevel.STRUCTURE
                                    || debug == TooltipDebugLevel.FULL,
                            joined.contains("Entry: damagenexus_test:entry"));
                    assertEquals(debug == TooltipDebugLevel.FULL,
                            joined.contains("Phase BASE_MODIFICATION"));
                    if (debug == TooltipDebugLevel.FULL) {
                        assertTrue(joined.contains("Condition type:"));
                        assertTrue(joined.contains("Operation type:"));
                    }
                }
            }
        }
    }

    @Test
    void entryAffixVanillaAndTemplateOnlyDocumentsHaveConsistentDebugHeader() throws Exception {
        try (TooltipTestLanguage ignored = TooltipTestLanguage.install("en_us")) {
            RuleNarrativePlanner narratives = narratives();
            var vanilla = new VanillaTooltipAugmentation(
                    id("sharpness"), Component.literal("Sharpness V"),
                    narratives.plan(List.of(), List.of(
                            new AddBaseDamageOperation(channel("physical"), 2)
                    )), List.of(), List.of("VANILLA_MELEE_ENCHANTMENT"),
                    List.of(), List.of()
            );
            List<DamageTooltipDocument> documents = List.of(
                    document(List.of(entry(simpleRule())), List.of(), List.of(),
                            DamageItemTemplateReferences.EMPTY, TooltipDebugLevel.SUMMARY),
                    document(List.of(), List.of(affix()), List.of(),
                            DamageItemTemplateReferences.EMPTY, TooltipDebugLevel.SUMMARY),
                    document(List.of(), List.of(), List.of(vanilla),
                            DamageItemTemplateReferences.EMPTY, TooltipDebugLevel.SUMMARY),
                    document(List.of(), List.of(), List.of(),
                            new DamageItemTemplateReferences(
                                    List.of(new DamageEntryTemplateReference(id("template"))),
                                    List.of()
                            ), TooltipDebugLevel.SUMMARY)
            );
            for (DamageTooltipDocument document : documents) {
                List<String> lines = render(document, TooltipDetailLevel.COMPACT,
                        TooltipDebugLevel.SUMMARY, Locale.US, new ArrayList<>());
                assertEquals(1, lines.stream()
                        .filter("[DamageNexus Debug]"::equals).count());
            }
        }
    }

    @Test
    void affixHierarchyLabelsEntriesAndEntryHierarchyLabelsRules() throws Exception {
        try (TooltipTestLanguage ignored = TooltipTestLanguage.install("en_us")) {
            List<String> lines = render(
                    document(List.of(), List.of(affix()), List.of(),
                            DamageItemTemplateReferences.EMPTY, TooltipDebugLevel.OFF),
                    TooltipDetailLevel.EXPANDED, TooltipDebugLevel.OFF,
                    Locale.US, new ArrayList<>()
            );
            assertEquals(1, lines.stream().filter(line -> line.trim().equals("Entries")).count());
            assertEquals(1, lines.stream().filter(line -> line.trim().equals("Rules")).count());
        }
    }

    @Test
    void templateKindsUseLocalizedDebugTerminology() throws Exception {
        try (TooltipTestLanguage ignored = TooltipTestLanguage.install("zh_cn")) {
            DamageTooltipDocument document = document(
                    List.of(), List.of(), List.of(),
                    new DamageItemTemplateReferences(
                            List.of(new DamageEntryTemplateReference(id("template"))),
                            List.of()
                    ), TooltipDebugLevel.STRUCTURE
            );
            String output = String.join("\n", render(
                    document, TooltipDetailLevel.COMPACT,
                    TooltipDebugLevel.STRUCTURE,
                    Locale.SIMPLIFIED_CHINESE, new ArrayList<>()
            ));
            assertTrue(output.contains("条目 模板引用"));
            assertFalse(output.contains("ENTRY"));
        }
    }

    @Test
    void vanillaAugmentationDoesNotRepeatNameAndTemplateIdsStayOutOfNormalTooltip() throws Exception {
        try (TooltipTestLanguage ignored = TooltipTestLanguage.install("en_us")) {
            RuleNarrativePlanner narratives = narratives();
            Component anchor = Component.literal("Sharpness V");
            VanillaTooltipAugmentation vanilla = new VanillaTooltipAugmentation(
                    id("sharpness"), anchor,
                    narratives.plan(List.of(), List.of(
                            new AddBaseDamageOperation(channel("physical"), 2)
                    )), List.of(), List.of("VANILLA_MELEE_ENCHANTMENT"),
                    List.of(), List.of()
            );
            DamageTooltipDocument document = document(
                    List.of(), List.of(), List.of(vanilla),
                    new DamageItemTemplateReferences(
                            List.of(new DamageEntryTemplateReference(id("hidden_template"))),
                            List.of()
                    ), TooltipDebugLevel.OFF
            );
            List<Component> initial = new ArrayList<>(List.of(anchor));
            List<String> lines = render(document, TooltipDetailLevel.COMPACT,
                    TooltipDebugLevel.OFF, Locale.US, initial);
            assertEquals(1, lines.stream().filter("Sharpness V"::equals).count());
            assertTrue(lines.stream().noneMatch(line -> line.contains("hidden_template")));
            assertTrue(lines.stream().anyMatch(line -> line.contains("+2 Physical damage")));
        }
    }

    @Test
    void shiftControlsTypedVanillaDetailNotes() throws Exception {
        try (TooltipTestLanguage ignored = TooltipTestLanguage.install("en_us")) {
            RuleNarrativePlanner narratives = narratives();
            Component anchor = Component.literal("Breach IV");
            VanillaTooltipAugmentation vanilla = new VanillaTooltipAugmentation(
                    id("breach"), anchor,
                    narratives.plan(List.of(), List.of(
                            new AddBaseDamageOperation(channel("physical"), 2)
                    )),
                    List.of(VanillaEnchantmentTooltipLines.breachReduction(0.125f)),
                    List.of("VANILLA_MELEE_ENCHANTMENT"), List.of(), List.of()
            );
            DamageTooltipDocument document = document(
                    List.of(), List.of(), List.of(vanilla),
                    DamageItemTemplateReferences.EMPTY, TooltipDebugLevel.OFF
            );
            String compact = String.join("\n", render(
                    document, TooltipDetailLevel.COMPACT, TooltipDebugLevel.OFF,
                    Locale.US, new ArrayList<>(List.of(anchor))
            ));
            String expanded = String.join("\n", render(
                    document, TooltipDetailLevel.EXPANDED, TooltipDebugLevel.OFF,
                    Locale.US, new ArrayList<>(List.of(anchor))
            ));
            assertFalse(compact.contains("armor effectiveness reduction"));
            assertTrue(expanded.contains(
                    "Vanilla armor effectiveness reduction: -12.5%"
            ));
        }
    }

    @Test
    void fullDebugIncludesHiddenSourcesAndNestedConditionTypeIds() throws Exception {
        DamageRuleDefinition nestedRule = new DamageRuleDefinition(
                id("nested_debug"), DamageRuleRole.OFFENSIVE,
                DamagePhase.BASE_MODIFICATION, 500,
                List.of(new AllOfCondition(List.of(
                        new TargetOnFireCondition(),
                        new TargetHealthBelowCondition(0.5f)
                ))),
                List.of(new AddBaseDamageOperation(channel("fire"), 4)),
                DamageRuleStacking.STACK, Optional.empty(), Optional.empty()
        );
        DamageEntryDefinition hidden = new DamageEntryDefinition(
                id("hidden_entry"), DamageEntryDisplay.EMPTY,
                DamageEntrySlot.ITEM, List.of(nestedRule),
                DamageEntryStacking.STACK, Optional.empty()
        );
        try (TooltipTestLanguage ignored = TooltipTestLanguage.install("en_us")) {
            DamageTooltipDocument document = document(
                    List.of(hidden), List.of(), List.of(),
                    DamageItemTemplateReferences.EMPTY, TooltipDebugLevel.FULL
            );
            String off = String.join("\n", render(
                    document, TooltipDetailLevel.COMPACT, TooltipDebugLevel.OFF,
                    Locale.US, new ArrayList<>()
            ));
            assertFalse(off.contains("[DamageNexus Debug]"));

            String full = String.join("\n", render(
                    document, TooltipDetailLevel.COMPACT, TooltipDebugLevel.FULL,
                    Locale.US, new ArrayList<>()
            ));
            assertTrue(full.contains("damagenexus_test:hidden_entry"));
            assertTrue(full.contains("damagenexus:all_of"));
            assertTrue(full.contains("damagenexus:target_on_fire"));
            assertTrue(full.contains("damagenexus:target_health_below"));
        }
    }

    @Test
    void mobCategoryUsesChineseLocalizationInsteadOfEnumName() throws Exception {
        DamageRuleDefinition rule = new DamageRuleDefinition(
                id("mob_category"), DamageRuleRole.OFFENSIVE,
                DamagePhase.BASE_MODIFICATION, 500,
                List.of(new TargetMobCategoryIsCondition(MobCategory.MONSTER)),
                List.of(new AddBaseDamageOperation(channel("fire"), 1)),
                DamageRuleStacking.STACK, Optional.empty(), Optional.empty()
        );
        try (TooltipTestLanguage ignored = TooltipTestLanguage.install("zh_cn")) {
            List<String> lines = render(document(List.of(entry(rule)), List.of(),
                            List.of(), DamageItemTemplateReferences.EMPTY, TooltipDebugLevel.OFF),
                    TooltipDetailLevel.EXPANDED, TooltipDebugLevel.OFF,
                    Locale.SIMPLIFIED_CHINESE, new ArrayList<>());
            String joined = String.join("\n", lines);
            assertTrue(joined.contains("敌对生物"));
            assertFalse(joined.toLowerCase(Locale.ROOT).contains("monster"));
        }
    }

    @Test
    void missingProvidersStayHiddenInCompactUseLocalizedDetailAndExposeIdsOnlyInFullDebug()
            throws Exception {
        Identifier conditionType = Identifier.fromNamespaceAndPath("example", "secret_condition");
        Identifier operationType = Identifier.fromNamespaceAndPath("example", "secret_operation");
        DamageRuleDefinition rule = new DamageRuleDefinition(
                id("unknown_rule"), DamageRuleRole.OFFENSIVE,
                DamagePhase.BASE_MODIFICATION, 500,
                List.of(new UnknownCondition(conditionType)),
                List.of(new UnknownOperation(operationType)),
                DamageRuleStacking.STACK, Optional.empty(), Optional.empty()
        );
        try (TooltipTestLanguage ignored = TooltipTestLanguage.install("en_us")) {
            DamageTooltipDocument compactDocument = document(
                    List.of(entry(rule)), List.of(), List.of(),
                    DamageItemTemplateReferences.EMPTY, TooltipDebugLevel.OFF
            );
            String compact = String.join("\n", render(
                    compactDocument, TooltipDetailLevel.COMPACT,
                    TooltipDebugLevel.OFF, Locale.US, new ArrayList<>()
            ));
            assertFalse(compact.contains(conditionType.toString()));
            assertFalse(compact.contains(operationType.toString()));
            assertFalse(compact.contains("rule_phrase."));

            String detail = String.join("\n", render(
                    compactDocument, TooltipDetailLevel.EXPANDED,
                    TooltipDebugLevel.OFF, Locale.US, new ArrayList<>()
            ));
            assertTrue(detail.contains("Condition cannot be described"));
            assertTrue(detail.contains("Effect cannot be described"));
            assertFalse(detail.contains(conditionType.toString()));

            DamageTooltipDocument fullDocument = document(
                    List.of(entry(rule)), List.of(), List.of(),
                    DamageItemTemplateReferences.EMPTY, TooltipDebugLevel.FULL
            );
            String full = String.join("\n", render(
                    fullDocument, TooltipDetailLevel.COMPACT,
                    TooltipDebugLevel.FULL, Locale.US, new ArrayList<>()
            ));
            assertTrue(full.contains(conditionType.toString()));
            assertTrue(full.contains(operationType.toString()));
            assertEquals(1, occurrences(full, "damagenexus_test:entry"));
        }
    }

    @Test
    void explicitBreakdownKeepsNamelessEntryAndFallbackResolutionMatchesAffix()
            throws Exception {
        DamageEntryDefinition nameless = new DamageEntryDefinition(
                id("nameless"),
                new DamageEntryDisplay(Optional.empty(), List.of(), Optional.empty(), true),
                DamageEntrySlot.ITEM, List.of(simpleRule()),
                DamageEntryStacking.STACK, Optional.empty()
        );
        DamageEntryDefinition fallbackEntry = new DamageEntryDefinition(
                id("fallback_entry"),
                new DamageEntryDisplay(
                        DisplayText.translatableWithFallback("missing.entry", "Fallback Name"),
                        List.of(), Optional.empty(), false
                ),
                DamageEntrySlot.ITEM, List.of(simpleRule()),
                DamageEntryStacking.STACK, Optional.empty()
        );
        DamageAffixDefinition fallbackAffix = new DamageAffixDefinition(
                id("fallback_affix"),
                new DamageAffixDisplay(
                        DisplayText.translatableWithFallback("missing.affix", "Fallback Name"),
                        List.of(), Optional.empty(), false
                ),
                DamageAffixSlot.ITEM, DamageAffixRarity.COMMON,
                List.of(entry(simpleRule())), DamageAffixStacking.STACK,
                Optional.empty()
        );
        try (TooltipTestLanguage ignored = TooltipTestLanguage.install("en_us")) {
            String output = String.join("\n", render(
                    document(List.of(nameless, fallbackEntry), List.of(fallbackAffix),
                            List.of(), DamageItemTemplateReferences.EMPTY, TooltipDebugLevel.OFF),
                    TooltipDetailLevel.COMPACT, TooltipDebugLevel.OFF,
                    Locale.US, new ArrayList<>()
            ));
            assertTrue(output.contains("+4 Fire damage while the target is burning"));
            assertEquals(2, occurrences(output, "Fallback Name"));
        }
    }

    private static List<String> snapshot(
            String localeName,
            Locale locale,
            TooltipDetailLevel detail
    ) throws Exception {
        try (TooltipTestLanguage ignored = TooltipTestLanguage.install(localeName)) {
            List<String> lines = render(
                    document(List.of(entry(simpleRule())), List.of(), List.of(),
                            DamageItemTemplateReferences.EMPTY, TooltipDebugLevel.OFF),
                    detail, TooltipDebugLevel.OFF, locale, new ArrayList<>()
            );
            assertNoLeaks(lines);
            return lines;
        }
    }

    private static DamageTooltipDocument document(
            List<DamageEntryDefinition> entries,
            List<DamageAffixDefinition> affixes,
            List<VanillaTooltipAugmentation> vanilla,
            DamageItemTemplateReferences templates,
            TooltipDebugLevel debug
    ) {
        return new DamageTooltipDocumentPlanner(narratives())
                .plan(entries, affixes, vanilla, templates, debug);
    }

    private static List<String> render(
            DamageTooltipDocument document,
            TooltipDetailLevel detail,
            TooltipDebugLevel debug,
            Locale locale,
            List<Component> tooltip
    ) {
        RuleNarrativePlanner narratives = narratives();
        RulePhraseRegistry registry = registry();
        new DamageTooltipRenderer(
                narratives,
                new RulePhraseRenderer(registry, locale)
        ).render(tooltip, document, new TooltipPresentationPolicy(detail, debug));
        return tooltip.stream().map(Component::getString).toList();
    }

    private static RuleNarrativePlanner narratives() {
        return new RuleNarrativePlanner(registry());
    }

    private static RulePhraseRegistry registry() {
        RulePhraseRegistry registry = new RulePhraseRegistry();
        DamageNexusRulePhraseBootstrap.register(registry);
        registry.freeze();
        return registry;
    }

    private static DamageEntryDefinition entry(DamageRuleDefinition rule) {
        return new DamageEntryDefinition(
                id("entry"),
                new DamageEntryDisplay(
                        DisplayText.translatable("test.damagenexus.snapshot.blazing_verdict"),
                        List.of(), Optional.empty(), true
                ),
                DamageEntrySlot.ITEM,
                List.of(rule),
                DamageEntryStacking.STACK,
                Optional.empty()
        );
    }

    private static DamageAffixDefinition affix() {
        return new DamageAffixDefinition(
                id("affix"),
                new DamageAffixDisplay(
                        DisplayText.literal("Test Affix"),
                        List.of(), Optional.empty(), true
                ),
                DamageAffixSlot.ITEM,
                DamageAffixRarity.RARE,
                List.of(entry(simpleRule())),
                DamageAffixStacking.STACK,
                Optional.empty()
        );
    }

    private static DamageRuleDefinition simpleRule() {
        return new DamageRuleDefinition(
                id("rule"), DamageRuleRole.OFFENSIVE,
                DamagePhase.BASE_MODIFICATION, 500,
                List.of(new TargetOnFireCondition()),
                List.of(new AddBaseDamageOperation(channel("fire"), 4)),
                DamageRuleStacking.STACK, Optional.empty(), Optional.empty()
        );
    }

    private static void assertNoLeaks(List<String> lines) {
        String output = String.join("\n", lines);
        assertFalse(output.contains("%s"), output);
        assertFalse(output.matches("(?s).*rule_(?:phrase|sentence)\\.[a-z0-9_.]+.*"), output);
        assertFalse(output.contains("tooltip.damagenexus"), output);
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        int from = 0;
        while ((from = text.indexOf(needle, from)) >= 0) {
            count++;
            from += needle.length();
        }
        return count;
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("damagenexus_test", path);
    }

    private static Identifier channel(String path) {
        return Identifier.fromNamespaceAndPath("damagenexus", path);
    }

    private record UnknownCondition(Identifier type) implements DamageRuleCondition {
        @Override
        public boolean test(DamageRuleContext ctx) {
            return false;
        }
    }

    private record UnknownOperation(Identifier type) implements DamageRuleOperation {
        @Override
        public DamageMutationResult apply(DamageRuleContext ctx) {
            return DamageMutationResult.NO_OP_ZERO;
        }
    }
}
