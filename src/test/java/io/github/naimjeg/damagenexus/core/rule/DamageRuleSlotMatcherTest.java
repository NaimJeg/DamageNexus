package io.github.naimjeg.damagenexus.core.rule;

import io.github.naimjeg.damagenexus.api.enums.DamageChannel;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleDefinition;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleProviderType;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleRole;
import io.github.naimjeg.damagenexus.api.rule.RuleExecutionContext;
import io.github.naimjeg.damagenexus.api.rule.RuleSourceLocation;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixDefinition;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixDisplay;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixRarity;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixSlot;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixStacking;
import io.github.naimjeg.damagenexus.api.rule.affix.RuntimeDamageAffix;
import io.github.naimjeg.damagenexus.api.rule.builder.DamageRuleBuilder;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryDefinition;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryDisplay;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntrySlot;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryStacking;
import io.github.naimjeg.damagenexus.api.rule.entry.RuntimeDamageEntry;
import io.github.naimjeg.damagenexus.api.rule.source.EquippedItemRuleSourceCategory;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamageRuleSlotMatcherTest {

    @ParameterizedTest
    @MethodSource("supportedSlotCases")
    void entryAndAffixSlotsUseProviderAndLocation(
            DamageEntrySlot entrySlot,
            DamageAffixSlot affixSlot,
            DamageRuleProviderType providerType,
            RuleSourceLocation location,
            boolean expected
    ) {
        RuleExecutionContext context = context(providerType, location);

        assertEquals(
                expected,
                DamageRuleSlotMatcher.matches(entrySlot, context)
        );
        assertEquals(
                expected,
                DamageRuleSlotMatcher.matches(affixSlot, context)
        );

        DamageEntryDefinition entry = entry(
                "parameterized_" + entrySlot.name().toLowerCase(),
                entrySlot,
                DamageEntryStacking.STACK,
                Optional.empty()
        );
        DamageAffixDefinition affix = affix(
                "parameterized_" + affixSlot.name().toLowerCase(),
                affixSlot,
                entry,
                DamageAffixStacking.STACK,
                Optional.empty()
        );

        assertEquals(
                expected,
                !new RuntimeDamageEntry(entry, context)
                        .expandRules()
                        .isEmpty()
        );
        assertEquals(
                expected,
                !new RuntimeDamageAffix(affix, context)
                        .expandRules()
                        .isEmpty()
        );
    }

    static Stream<Arguments> supportedSlotCases() {
        return Stream.of(
                arguments(
                        DamageEntrySlot.WEAPON,
                        DamageAffixSlot.WEAPON,
                        DamageRuleProviderType.ITEM_EQUIPMENT,
                        RuleSourceLocation.VICTIM_HEAD,
                        false
                ),
                arguments(
                        DamageEntrySlot.ARMOR,
                        DamageAffixSlot.ARMOR,
                        DamageRuleProviderType.ITEM_EQUIPMENT,
                        RuleSourceLocation.ATTACKER_MAINHAND,
                        false
                ),
                arguments(
                        DamageEntrySlot.ARMOR,
                        DamageAffixSlot.ARMOR,
                        DamageRuleProviderType.ITEM_EQUIPMENT,
                        RuleSourceLocation.ATTACKER_OFFHAND,
                        false
                ),
                arguments(
                        DamageEntrySlot.WEAPON,
                        DamageAffixSlot.WEAPON,
                        DamageRuleProviderType.ITEM_EQUIPMENT,
                        RuleSourceLocation.ATTACKER_MAINHAND,
                        true
                ),
                arguments(
                        DamageEntrySlot.WEAPON,
                        DamageAffixSlot.WEAPON,
                        DamageRuleProviderType.ITEM_EQUIPMENT,
                        RuleSourceLocation.VICTIM_OFFHAND,
                        true
                ),
                arguments(
                        DamageEntrySlot.WEAPON,
                        DamageAffixSlot.WEAPON,
                        DamageRuleProviderType.PROJECTILE_SOURCE,
                        RuleSourceLocation.PROJECTILE,
                        true
                ),
                arguments(
                        DamageEntrySlot.PROJECTILE,
                        DamageAffixSlot.PROJECTILE,
                        DamageRuleProviderType.PROJECTILE_SOURCE,
                        RuleSourceLocation.PROJECTILE,
                        true
                ),
                arguments(
                        DamageEntrySlot.PROJECTILE,
                        DamageAffixSlot.PROJECTILE,
                        DamageRuleProviderType.ITEM_EQUIPMENT,
                        RuleSourceLocation.ATTACKER_MAINHAND,
                        false
                ),
                arguments(
                        DamageEntrySlot.ITEM,
                        DamageAffixSlot.ITEM,
                        DamageRuleProviderType.ITEM_EQUIPMENT,
                        RuleSourceLocation.VICTIM_CHEST,
                        true
                ),
                arguments(
                        DamageEntrySlot.ITEM,
                        DamageAffixSlot.ITEM,
                        DamageRuleProviderType.PROJECTILE_SOURCE,
                        RuleSourceLocation.PROJECTILE,
                        true
                )
        );
    }

    @Test
    void contradictoryProviderAndLocationNeverMatchBuiltInSlots() {
        RuleExecutionContext contradictory = context(
                DamageRuleProviderType.ITEM_EQUIPMENT,
                RuleSourceLocation.PROJECTILE
        );

        assertFalse(contradictory.hasConsistentBuiltInSource());
        assertFalse(DamageRuleSlotMatcher.matches(
                DamageEntrySlot.ITEM,
                contradictory
        ));
        assertFalse(DamageRuleSlotMatcher.matches(
                DamageEntrySlot.PROJECTILE,
                contradictory
        ));
    }

    @ParameterizedTest
    @MethodSource("externalCategoryCases")
    void externalCategoriesMatchOnlyTheirDocumentedGenericSlots(
            EquippedItemRuleSourceCategory category,
            DamageEntrySlot slot,
            boolean expected
    ) {
        RuleExecutionContext context = RuleExecutionContext.externalItemSource(
                DamageRuleRole.OFFENSIVE,
                null,
                ItemStack.EMPTY,
                Identifier.fromNamespaceAndPath("slotmod", "provider"),
                Identifier.fromNamespaceAndPath("slotmod", "physical_source"),
                Identifier.fromNamespaceAndPath("contentmod", "ring"),
                category
        );
        assertTrue(context.hasConsistentBuiltInSource());
        assertEquals(expected, context.matches(slot));
        assertEquals(RuleSourceLocation.EXTERNAL, context.sourceLocation());
    }

    static Stream<Arguments> externalCategoryCases() {
        return Stream.of(
                Arguments.of(EquippedItemRuleSourceCategory.ITEM, DamageEntrySlot.ITEM, true),
                Arguments.of(EquippedItemRuleSourceCategory.ITEM, DamageEntrySlot.WEAPON, false),
                Arguments.of(EquippedItemRuleSourceCategory.WEAPON, DamageEntrySlot.ITEM, true),
                Arguments.of(EquippedItemRuleSourceCategory.WEAPON, DamageEntrySlot.WEAPON, true),
                Arguments.of(EquippedItemRuleSourceCategory.WEAPON, DamageEntrySlot.ARMOR, false),
                Arguments.of(EquippedItemRuleSourceCategory.ARMOR, DamageEntrySlot.ITEM, true),
                Arguments.of(EquippedItemRuleSourceCategory.ARMOR, DamageEntrySlot.ARMOR, true),
                Arguments.of(EquippedItemRuleSourceCategory.PROJECTILE, DamageEntrySlot.ITEM, true),
                Arguments.of(EquippedItemRuleSourceCategory.PROJECTILE, DamageEntrySlot.WEAPON, true),
                Arguments.of(EquippedItemRuleSourceCategory.PROJECTILE, DamageEntrySlot.PROJECTILE, true),
                Arguments.of(EquippedItemRuleSourceCategory.PROJECTILE, DamageEntrySlot.ARMOR, false)
        );
    }

    @Test
    void runtimeEntryAndAffixEnforceBothSlotLevels() {
        RuleExecutionContext helmet = context(
                DamageRuleProviderType.ITEM_EQUIPMENT,
                RuleSourceLocation.VICTIM_HEAD
        );
        DamageEntryDefinition weaponEntry = entry(
                "weapon_entry",
                DamageEntrySlot.WEAPON,
                DamageEntryStacking.STACK,
                Optional.empty()
        );
        DamageAffixDefinition outerMatches = affix(
                "helmet_affix",
                DamageAffixSlot.ARMOR,
                weaponEntry,
                DamageAffixStacking.STACK,
                Optional.empty()
        );

        assertTrue(new RuntimeDamageEntry(
                weaponEntry,
                helmet
        ).expandRules().isEmpty());
        assertTrue(new RuntimeDamageAffix(
                outerMatches,
                helmet
        ).expandRules().isEmpty());
        assertTrue(StackDamageEntryCollector.resolveApplicableAffixes(
                List.of(outerMatches),
                helmet
        ).isEmpty());
    }

    @Test
    void inapplicableDefinitionsCannotReplaceApplicableStackingWinner() {
        Identifier group =
                Identifier.fromNamespaceAndPath("test", "shared_group");
        DamageEntryDefinition applicable = entry(
                "applicable_weapon",
                DamageEntrySlot.WEAPON,
                DamageEntryStacking.REPLACE,
                Optional.of(group)
        );
        DamageEntryDefinition laterButInapplicable = entry(
                "inapplicable_armor",
                DamageEntrySlot.ARMOR,
                DamageEntryStacking.REPLACE,
                Optional.of(group)
        );

        List<DamageEntryDefinition> selected =
                StackDamageEntryCollector.resolveApplicableEntries(
                        List.of(applicable, laterButInapplicable),
                        context(
                                DamageRuleProviderType.ITEM_EQUIPMENT,
                                RuleSourceLocation.ATTACKER_MAINHAND
                        )
                );

        assertEquals(List.of(applicable), selected);
    }

    @Test
    void affixWithNoApplicableNestedEntryCannotReplaceWinner() {
        Identifier group =
                Identifier.fromNamespaceAndPath("test", "affix_group");
        DamageAffixDefinition applicable = affix(
                "applicable_affix",
                DamageAffixSlot.WEAPON,
                entry(
                        "applicable_nested",
                        DamageEntrySlot.WEAPON,
                        DamageEntryStacking.STACK,
                        Optional.empty()
                ),
                DamageAffixStacking.REPLACE,
                Optional.of(group)
        );
        DamageAffixDefinition laterButNestedMismatch = affix(
                "nested_mismatch_affix",
                DamageAffixSlot.WEAPON,
                entry(
                        "armor_nested",
                        DamageEntrySlot.ARMOR,
                        DamageEntryStacking.STACK,
                        Optional.empty()
                ),
                DamageAffixStacking.REPLACE,
                Optional.of(group)
        );

        List<DamageAffixDefinition> selected =
                StackDamageEntryCollector.resolveApplicableAffixes(
                        List.of(applicable, laterButNestedMismatch),
                        context(
                                DamageRuleProviderType.ITEM_EQUIPMENT,
                                RuleSourceLocation.ATTACKER_MAINHAND
                        )
                );

        assertEquals(List.of(applicable), selected);
    }

    private static Arguments arguments(
            DamageEntrySlot entrySlot,
            DamageAffixSlot affixSlot,
            DamageRuleProviderType providerType,
            RuleSourceLocation location,
            boolean expected
    ) {
        return Arguments.of(
                entrySlot,
                affixSlot,
                providerType,
                location,
                expected
        );
    }

    private static RuleExecutionContext context(
            DamageRuleProviderType providerType,
            RuleSourceLocation location
    ) {
        return new RuleExecutionContext(
                providerType,
                location,
                DamageRuleRole.OFFENSIVE,
                null,
                ItemStack.EMPTY,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private static DamageEntryDefinition entry(
            String path,
            DamageEntrySlot slot,
            DamageEntryStacking stacking,
            Optional<Identifier> stackingGroup
    ) {
        return new DamageEntryDefinition(
                Identifier.fromNamespaceAndPath("test", path),
                DamageEntryDisplay.EMPTY,
                slot,
                List.of(rule(path + "_rule")),
                stacking,
                stackingGroup
        );
    }


    private static DamageAffixDefinition affix(
            String path,
            DamageAffixSlot slot,
            DamageEntryDefinition entry,
            DamageAffixStacking stacking,
            Optional<Identifier> stackingGroup
    ) {
        return new DamageAffixDefinition(
                Identifier.fromNamespaceAndPath("test", path),
                DamageAffixDisplay.EMPTY,
                slot,
                DamageAffixRarity.COMMON,
                List.of(entry),
                stacking,
                stackingGroup
        );
    }

    private static DamageRuleDefinition rule(String path) {
        return DamageRuleBuilder
                .offensive(Identifier.fromNamespaceAndPath("test", path))
                .addBaseDamage(DamageChannel.UNTYPED_ID, 1.0f)
                .build();
    }
}
