package io.github.naimjeg.damagenexus.api.rule;

import io.github.naimjeg.damagenexus.api.rule.source.EquippedItemRuleContribution;
import io.github.naimjeg.damagenexus.api.rule.source.EquippedItemRuleSourceCategory;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EquippedItemRuleContributionTest {

    @Test
    void entriesAffixesAndBothModesRemainDistinct() {
        EquippedItemRuleContribution entries = contribution(true, false);
        EquippedItemRuleContribution affixes = contribution(false, true);
        EquippedItemRuleContribution both = contribution(true, true);

        assertTrue(entries.readEntries());
        assertFalse(entries.readAffixes());
        assertFalse(affixes.readEntries());
        assertTrue(affixes.readAffixes());
        assertTrue(both.readEntries() && both.readAffixes());
        assertThrows(
                IllegalArgumentException.class,
                () -> contribution(false, false)
        );
    }

    @Test
    void sourcePriorityHasBoundedRange() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new EquippedItemRuleContribution(
                        ItemStack.EMPTY,
                        id("source"),
                        id("slot"),
                        EquippedItemRuleSourceCategory.ITEM,
                        EquippedItemRuleContribution.MAX_PRIORITY + 1,
                        true,
                        true
                )
        );
    }

    private static EquippedItemRuleContribution contribution(
            boolean entries,
            boolean affixes
    ) {
        return new EquippedItemRuleContribution(
                ItemStack.EMPTY,
                id("source"),
                id("slot"),
                EquippedItemRuleSourceCategory.ITEM,
                0,
                entries,
                affixes
        );
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("testmod", path);
    }
}
