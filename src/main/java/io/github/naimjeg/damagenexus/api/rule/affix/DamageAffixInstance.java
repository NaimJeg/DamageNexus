package io.github.naimjeg.damagenexus.api.rule.affix;

import io.github.naimjeg.damagenexus.api.rule.DamageRuleDefinition;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Optional;

/**
 * Experimental authoring-only rolled affix instance.
 *
 * <p>This is the intended future bridge between generated blueprint data and the
 * existing executable DamageRuleDefinition runtime model. The
 * {@code materializedRules} list is not yet mapped to the nested-entry shape
 * required by {@link DamageAffixDefinition}; this type is not executable and
 * has no stable codec or migration contract.</p>
 */
public record DamageAffixInstance(
        Identifier instanceId,
        Identifier blueprintId,
        Optional<Identifier> tierId,
        long rollSeed,
        DamageAffixDisplay display,
        DamageAffixSlot slot,
        DamageAffixRarity rarity,
        DamageAffixStacking stacking,
        Optional<Identifier> stackingGroup,
        List<DamageAffixRolledValue> rolledValues,
        List<DamageRuleDefinition> materializedRules
) {
    public DamageAffixInstance {
        if (instanceId == null) {
            throw new IllegalArgumentException("Damage affix instance id cannot be null");
        }

        if (blueprintId == null) {
            throw new IllegalArgumentException("Damage affix blueprint id cannot be null");
        }

        if (display == null) {
            throw new IllegalArgumentException("Damage affix instance display cannot be null");
        }

        if (slot == null) {
            throw new IllegalArgumentException("Damage affix instance slot cannot be null");
        }

        if (rarity == null) {
            rarity = DamageAffixRarity.COMMON;
        }

        if (stacking == null) {
            stacking = DamageAffixStacking.UNIQUE_AFFIX;
        }

        tierId = tierId == null ? Optional.empty() : tierId;
        stackingGroup = stackingGroup == null ? Optional.empty() : stackingGroup;
        rolledValues = rolledValues == null ? List.of() : List.copyOf(rolledValues);
        materializedRules = materializedRules == null
                ? List.of()
                : List.copyOf(materializedRules);
    }
}

