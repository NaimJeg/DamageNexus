package io.github.naimjeg.damagenexus.core.rule;

import io.github.naimjeg.damagenexus.api.rule.RuleExecutionContext;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixSlot;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntrySlot;

/**
 * Matches declared entry/affix slots against the runtime rule source.
 */
final class DamageRuleSlotMatcher {

    private DamageRuleSlotMatcher() {
    }

    static boolean matches(
            DamageEntrySlot slot,
            RuleExecutionContext executionContext
    ) {
        if (slot == null || executionContext == null) {
            return false;
        }

        return executionContext.matches(slot);
    }

    static boolean matches(
            DamageAffixSlot slot,
            RuleExecutionContext executionContext
    ) {
        if (slot == null || executionContext == null) {
            return false;
        }

        return executionContext.matches(slot);
    }
}
